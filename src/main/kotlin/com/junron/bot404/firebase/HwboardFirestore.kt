package com.junron.bot404.firebase

import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.firestore.Firestore
import com.google.cloud.firestore.SetOptions
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import com.junron.bot404.Config.Companion.config
import com.junron.bot404.model.Homework
import com.junron.bot404.model.Tag
import com.junron.bot404.util.isFuture
import com.junron.bot404.util.toDate
import com.junron.bot404.util.uuid
import java.io.File
import java.util.*
import kotlin.concurrent.schedule

object HwboardFirestore {
    private val db: Firestore
    var hwboardConfig: HwboardConfig
        private set
    private var timer: TimerTask? = null
    private var homework: List<Homework>
    private val callbacks = mutableListOf<(List<Homework>) -> Unit>()

    init {
        val serviceAccount = File("secrets/service-account.json")
        val credentials =
            ServiceAccountCredentials.fromStream(serviceAccount.inputStream())
        val options = FirebaseOptions.builder()
            .setCredentials(credentials)
            .setDatabaseUrl("https://${config.projectId}.firebaseio.com")
            .setProjectId(config.projectId)
            .build()
        val app = FirebaseApp.initializeApp(options)
        db = FirestoreClient.getFirestore(app)
        hwboardConfig =
            db.collection("hwboard").document(config.hwboardName).get().get()
                .toObject(HwboardConfig::class.java)!!
                .copy(name = config.hwboardName)
        db.collection("hwboard").document(config.hwboardName)
            .addSnapshotListener { value, _ ->
                value ?: return@addSnapshotListener
                hwboardConfig = value.toObject(HwboardConfig::class.java)!!
                    .copy(name = config.hwboardName)
            }
        this.homework = db.collection("hwboard")
            .document(config.hwboardName)
            .collection("homework")
            .get().get().documents.map {
                it.toObject(Homework::class.java)
            }
        db.collection("hwboard")
            .document(config.hwboardName)
            .collection("homework")
            .addSnapshotListener { value, _ ->
                value ?: return@addSnapshotListener
                homework = value.documents.map {
                    it.toObject(Homework::class.java)
                }
                setTimer()
                callbacks.forEach { it(homework) }
            }
    }

    private fun setTimer() {
        timer?.cancel()
        homework.filter { it.dueDate.toDate().isFuture() }
            .minBy { it.dueDate.toDate().time - Date().time }
            ?.let {
                val due = it.dueDate.toDate()
                timer = Timer(uuid(), false).schedule(due) {
                    setTimer()
                    callbacks.forEach { it(homework) }
                }
            }
    }

    fun getTags() = hwboardConfig.tags

    fun getHomework(current: Boolean = true) =
        if (current) homework.filter {
            !it.deleted && it.dueDate.toDate().isFuture()
        }
        else homework

    fun updateHomework(homework: Homework) {
        db.collection("hwboard")
            .document(config.hwboardName)
            .collection("homework")
            .document(homework.id)
            .set(homework)
    }

    fun deleteHomework(homework: Homework) {
        updateHomework(homework.copy(deleted = true))
    }

    fun addListener(listener: (List<Homework>) -> Unit) {
        callbacks += listener
    }

    private fun addTag(tag: Tag) {
        db.collection("hwboard")
            .document(config.hwboardName)
            .set(
                mapOf("tags" to (hwboardConfig.tags + tag).map { it.toMap() }),
                SetOptions.merge()
            )
    }
}
