package com.junron.bot404.firebase

import com.google.auth.oauth2.ServiceAccountCredentials
import com.google.cloud.firestore.Firestore
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.cloud.FirestoreClient
import com.junron.bot404.config
import com.junron.bot404.model.Homework
import com.junron.bot404.util.indentedJson
import kotlinx.serialization.builtins.list
import java.io.File

object HwboardFirestore {
    private val db: Firestore

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
    }

    fun getConfig() =
        db.collection("hwboard").document(config.hwboardName).get().get()
            .toObject(HwboardConfig::class.java)!!
            .copy(name = config.hwboardName)

    fun syncData() {
        val data = indentedJson.parse(
            Homework.serializer().list,
            File("./homework.json").readText()
        )
        val updateTime = db.batch().apply {
            val homeworkCollection =
                db.collection("hwboard").document(config.hwboardName)
                    .collection("homework")
            data.forEach {
                val homeworkDocument = homeworkCollection.document(it.id)
                set(homeworkDocument, it)
            }
        }.commit().get().first().updateTime
        println(updateTime)
    }
}
