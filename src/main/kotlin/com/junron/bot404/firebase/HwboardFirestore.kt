package com.junron.bot404.firebase

import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.firestore.Firestore
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import com.junron.bot404.config
import com.junron.bot404.model.Homework
import com.junron.bot404.util.indentedJson
import com.junron.bot404.util.isFuture
import com.junron.bot404.util.toDate
import kotlinx.serialization.builtins.list
import java.io.File

object HwboardFirestore {
    private val db: Firestore
    var hwboardConfig: HwboardConfig
        private set
    private var homework: List<Homework>

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
            }
    }

    fun syncData() {
        val data = indentedJson.parse(
            Homework.serializer().list,
            File("./homework.json").readText()
        )
        db.batch().apply {
            val homeworkCollection =
                db.collection("hwboard").document(config.hwboardName)
                    .collection("homework")
            data.forEach {
                val homeworkDocument = homeworkCollection.document(it.id)
                set(homeworkDocument, it)
            }
        }.commit().get()
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
}
