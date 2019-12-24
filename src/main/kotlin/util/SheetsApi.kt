package util

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.model.ValueRange
import kotlinx.serialization.UnstableDefault
import java.io.File
import com.google.api.services.drive.model.File as DriveFile


@UnstableDefault
object SheetsApi {
  private val scopes = listOf(DriveScopes.DRIVE)
  private val secrets = File("secrets/bot-creds.json")
  private val jsonFactory = JacksonFactory.getDefaultInstance()!!
  private const val appName = "404 bot"

  private fun getCredentials() =
      GoogleCredential.fromStream(secrets.inputStream())
          .createScoped(scopes)

  fun exportPoll(name: String, voteOptions: List<VoteOption>, callback: (String) -> Unit) {
    val credential = getCredentials()
    val transport = GoogleNetHttpTransport.newTrustedTransport()
    val sheetsService = Sheets.Builder(transport, jsonFactory, credential)
        .setApplicationName(appName)
        .build()!!
    val driveService = Drive.Builder(transport, jsonFactory, credential)
        .setApplicationName(appName)
        .build()!!

    val outputVotes = mutableListOf("Votes") + ClassLists.load()
        .map { student ->
          val vote = voteOptions.firstOrNull { voteOption ->
            student in voteOption.students
          }
          vote?.emoji?.name ?: "Not Voted"
        }

    val copy = with(driveService.files().copy(
        //  Original spreadsheet
        "1SJBgU5CWyKNst6VmtvtJdTi0ggO1gAphplLqc5Hpy7w",
        DriveFile()
            //  Generated directory
            .setParents(listOf("1kxhHGEn4NzM42xQ0vDfhclse8cRqqPAs"))
            .setName("Poll $name")

    )) {
      fields = "webViewLink,id"
      this
    }.execute()

    callback(copy.webViewLink)

    val range = with(ValueRange()) {
      range = "Sheet1!E1:E23"
      majorDimension = "COLUMNS"
      setValues(listOf(
          outputVotes.toMutableList()
      ))
      this
    }
    sheetsService.spreadsheets().values()
        .update(copy.id, "Sheet1!E1:E23", range)
        .setValueInputOption("RAW")
        .execute()
  }
}

