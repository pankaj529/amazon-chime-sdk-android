package com.amazon.chime.sdkdemo

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.amazon.chime.sdk.utils.logger.ConsoleLogger
import com.amazon.chime.sdk.utils.logger.LogLevel
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MeetingHomeActivity : AppCompatActivity() {
    private val logger = ConsoleLogger(LogLevel.INFO)
    private val uiScope = CoroutineScope(Dispatchers.Main)
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO

    private val WEBRTC_PERMISSION_REQUEST_CODE = 1
    private val MEETING_REGION = "us-east-1"
    private val TAG = "MeetingHomeActivity"

    private val WEBRTC_PERM = arrayOf(
        Manifest.permission.MODIFY_AUDIO_SETTINGS,
        Manifest.permission.RECORD_AUDIO
    )

    private var meetingEditText: EditText? = null
    private var nameEditText: EditText? = null
    private var authenticationProgressBar: ProgressBar? = null
    private var meetingID: String? = null
    private var yourName: String? = null

    companion object {
        const val MEETING_RESPONSE_KEY = "MEETING_RESPONSE"
        const val MEETING_ID_KEY = "MEETING_ID"
        const val NAME_KEY = "NAME"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_meeting_home)
        meetingEditText = findViewById(R.id.editMeetingId)
        nameEditText = findViewById(R.id.editName)
        authenticationProgressBar = findViewById(R.id.progressAuthentication)

        findViewById<Button>(R.id.buttonContinue)?.setOnClickListener { joinMeeting() }
    }

    private fun joinMeeting() {
        meetingID = meetingEditText?.text.toString().trim().replace("\\s+".toRegex(), "+")
        yourName = nameEditText?.text.toString().trim().replace("\\s+".toRegex(), "+")

        if (meetingID.isNullOrBlank()) {
            Toast.makeText(this, getString(R.string.meeting_id_invalid), Toast.LENGTH_LONG).show()
        } else if (yourName.isNullOrBlank()) {
            Toast.makeText(this, getString(R.string.name_invalid), Toast.LENGTH_LONG).show()
        } else {
            if (hasPermissionsAlready()) {
                authenticate(getString(R.string.test_url), meetingID, yourName)
            } else {
                ActivityCompat.requestPermissions(this, WEBRTC_PERM, WEBRTC_PERMISSION_REQUEST_CODE)
            }
        }
    }

    private fun hasPermissionsAlready(): Boolean {
        return WEBRTC_PERM.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissionsList: Array<String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            WEBRTC_PERMISSION_REQUEST_CODE -> {
                val isMissingPermission: Boolean =
                    grantResults.isEmpty() || grantResults.any { PackageManager.PERMISSION_GRANTED != it }

                if (isMissingPermission) {
                    Toast.makeText(this, getString(R.string.permission_error), Toast.LENGTH_LONG)
                        .show()
                    return
                }
                authenticate(getString(R.string.test_url), meetingID, yourName)
            }
        }
    }

    private fun authenticate(
        meetingUrl: String,
        meetingId: String?,
        attendeeName: String?
    ) =
        uiScope.launch {
            authenticationProgressBar?.visibility = View.VISIBLE
            logger.info(TAG, "Joining meeting. URL: $meetingUrl")

            val meetingResponseJson: String? = joinMeeting(meetingUrl, meetingId, attendeeName)

            authenticationProgressBar?.visibility = View.INVISIBLE

            if (meetingResponseJson == null) {
                Toast.makeText(
                    applicationContext,
                    "There was an error joining the meeting. Please try again or use a different meeting ID",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                val intent = Intent(applicationContext, InMeetingActivity::class.java)
                intent.putExtra(MEETING_RESPONSE_KEY, meetingResponseJson)
                intent.putExtra(MEETING_ID_KEY, meetingId)
                intent.putExtra(NAME_KEY, attendeeName)
                startActivity(intent)
            }
        }

    private suspend fun joinMeeting(
        meetingUrl: String,
        meetingId: String?,
        attendeeName: String?
    ): String? {
        return withContext(ioDispatcher) {
            val serverUrl =
                URL("${meetingUrl}join?title=$meetingId&name=$attendeeName&region=$MEETING_REGION")

            try {
                val response = StringBuffer()
                with(serverUrl.openConnection() as HttpURLConnection) {
                    requestMethod = "POST"
                    doInput = true
                    doOutput = true

                    BufferedReader(InputStreamReader(inputStream)).use {
                        var inputLine = it.readLine()
                        while (inputLine != null) {
                            response.append(inputLine)
                            inputLine = it.readLine()
                        }
                        it.close()
                    }

                    if (responseCode == 200) {
                        response.toString()
                    } else {
                        logger.error(TAG, "Unable to join meeting. Response code: $responseCode")
                        null
                    }
                }
            } catch (exception: Exception) {
                logger.error(TAG, "There was an exception while joining the meeting: $exception")
                null
            }
        }
    }
}
