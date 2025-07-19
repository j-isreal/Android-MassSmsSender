// android-masssmssender
// Copyright © 2025 Jacob "Isreal" Eiler and Isreal Consulting, LLC.
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU Affero General Public License as published
// by the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU Affero General Public License for more details.
//
// You should have received a copy of the GNU Affero General Public License
// along with this program.  If not, see <https://www.gnu.org/licenses/>.
//
// Created by Jacob "Isreal" Eiler and Isreal Consulting, LLC.
// https://www.icllc.cc
//

package com.example.masssmssender

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import android.telephony.SmsManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*

class MainActivity : AppCompatActivity() {

    companion object {
        private const val PERMISSION_REQUEST_READ_CONTACTS = 100
        private const val PERMISSION_REQUEST_READ_STORAGE = 101
        private const val PERMISSION_REQUEST_SEND_SMS = 102
        private const val PICK_CONTACT_REQUEST = 1
    }

    private lateinit var contactsContainer: LinearLayout
    private lateinit var messageEditText: EditText
    private lateinit var statusLogTextView: TextView
    private val selectedNumbers = LinkedHashSet<String>() // keep order

    private val csvPickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val csvUri = result.data?.data
            csvUri?.let { importContactsFromCsv(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val scrollView = ScrollView(this)
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(20, 20, 20, 20)
        }

        val pickContactsBtn = Button(this).apply {
            text = "Pick Contact"
            setOnClickListener {
                if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this@MainActivity, arrayOf(Manifest.permission.READ_CONTACTS), PERMISSION_REQUEST_READ_CONTACTS)
                } else {
                    openContactPicker()
                }
            }
        }

        val importCsvBtn = Button(this).apply {
            text = "Import CSV"
            setOnClickListener {
                openCsvPicker() // no permission check needed
            }
        }


        messageEditText = EditText(this).apply {
            hint = "Enter message here"
            minLines = 3
            maxLines = 6
        }

        val sendButton = Button(this).apply {
            text = "Send SMS"
            setOnClickListener { sendSmsToSelected() }
        }

        contactsContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }

        statusLogTextView = TextView(this).apply {
            text = "Status Log:\n"
            setPadding(0, 20, 0, 0)
        }

        val resetButton = Button(this).apply {
            text = "Reset"
            setOnClickListener {
                selectedNumbers.clear()
                contactsContainer.removeAllViews()
                statusLogTextView.text = "Status Log:\n"
                messageEditText.setText("")
                Toast.makeText(this@MainActivity, "App reset", Toast.LENGTH_SHORT).show()
            }
        }

        val exitButton = Button(this).apply {
            text = "Exit"
            setOnClickListener {
                finishAffinity() // Closes all activities and exits
            }
        }

        mainLayout.apply {
            addView(pickContactsBtn)
            addView(importCsvBtn)
            addView(messageEditText)
            addView(sendButton)
            addView(TextView(this@MainActivity).apply { text = "Selected Contacts:" })
            addView(contactsContainer)
            addView(statusLogTextView)
            addView(resetButton) // Add Reset here
            addView(exitButton)  // Add Exit here
        }

        scrollView.addView(mainLayout)
        setContentView(scrollView)
    }

    private fun openContactPicker() {
        val intent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
        startActivityForResult(intent, PICK_CONTACT_REQUEST)
    }

    private fun openCsvPicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "text/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        csvPickerLauncher.launch(intent)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICK_CONTACT_REQUEST && resultCode == RESULT_OK && data != null) {
            data.data?.let { uri ->
                loadContactFromUri(uri)
            }
        }
    }

    private fun loadContactFromUri(contactUri: Uri) {
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.TYPE
        )
        try {
            contentResolver.query(contactUri, projection, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val name = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME))
                    val number = cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER))
                    val type = cursor.getInt(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.TYPE))

                    if (type == ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE ||
                        type == ContactsContract.CommonDataKinds.Phone.TYPE_WORK_MOBILE ||
                        type == ContactsContract.CommonDataKinds.Phone.TYPE_CUSTOM) {
                        addContact(name, number)
                    } else {
                        toastOnUiThread("Selected contact is not a mobile number.")
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            toastOnUiThread("Failed to load contact")
        }
    }

    private fun addContact(name: String, number: String) {
        val normalizedNumber = number.replace(Regex("[\\s\\-()]"), "")
        if (selectedNumbers.add(normalizedNumber)) {
            runOnUiThread {
                val tv = TextView(this).apply { text = "$name : $normalizedNumber" }
                contactsContainer.addView(tv)
                appendStatus("Added contact: $name ($normalizedNumber)")
            }
        } else {
            appendStatus("Contact $name ($normalizedNumber) already selected")
        }
    }

    private fun importContactsFromCsv(csvUri: Uri) {
        try {
            contentResolver.openInputStream(csvUri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    var importedCount = 0
                    while (true) {
                        val line = reader.readLine() ?: break
                        if (line.isNotBlank()) {
                            val number = line.trim()
                            val name = "Contact ${selectedNumbers.size + 1}"
                            addContact(name, number)
                            importedCount++
                        }
                    }
                    toastOnUiThread("Imported $importedCount phone numbers from CSV.")
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            toastOnUiThread("Failed to import CSV")
        }
    }

    private fun sendSmsToSelected() {
        val message = messageEditText.text.toString().trim()
        if (message.isEmpty()) {
            toastOnUiThread("Please enter a message before sending.")
            return
        }
        if (selectedNumbers.isEmpty()) {
            toastOnUiThread("No contacts selected.")
            return
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.SEND_SMS), PERMISSION_REQUEST_SEND_SMS)
            return
        }

        val smsManager = SmsManager.getDefault()
        for (number in selectedNumbers) {
            try {
                smsManager.sendTextMessage(number, null, message, null, null)
                appendStatus("Sent SMS to $number")
            } catch (e: Exception) {
                e.printStackTrace()
                appendStatus("Failed to send SMS to $number: ${e.message}")
            }
        }
        // Clear contacts and prompt to save log
        selectedNumbers.clear()
        contactsContainer.removeAllViews()
        promptToSaveStatusLog()

    }

    private fun appendStatus(text: String) {
        runOnUiThread {
            statusLogTextView.append("$text\n")
        }
    }

    private fun promptToSaveStatusLog() {
        val dialog = AlertDialog.Builder(this)
            .setTitle("Save Results")
            .setMessage("Do you want to save the SMS log to a text file?")
            .setPositiveButton("Save") { _, _ -> saveLogToFile() }
            .setNegativeButton("Cancel", null)
            .create()
        dialog.show()
    }

    private fun saveLogToFile() {
        try {
            val fileName = "sms_log_${System.currentTimeMillis()}.txt"
            val content = statusLogTextView.text.toString()

            val resolver = contentResolver
            val contentValues = android.content.ContentValues().apply {
                put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "text/plain")
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Download/")
            }

            val uri = resolver.insert(android.provider.MediaStore.Files.getContentUri("external"), contentValues)

            uri?.let {
                resolver.openOutputStream(it)?.use { outputStream ->
                    outputStream.write(content.toByteArray())
                    outputStream.flush()
                }
                toastOnUiThread("Saved to Downloads/$fileName")
            } ?: toastOnUiThread("Failed to create file.")
        } catch (e: Exception) {
            e.printStackTrace()
            toastOnUiThread("Failed to save file: ${e.message}")
        }
    }

    private fun toastOnUiThread(text: String) {
        runOnUiThread {
            Toast.makeText(this, text, Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_READ_CONTACTS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openContactPicker()
                } else {
                    toastOnUiThread("Contacts permission denied")
                }
            }
            PERMISSION_REQUEST_READ_STORAGE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    openCsvPicker()
                } else {
                    toastOnUiThread("Storage permission denied")
                }
            }
            PERMISSION_REQUEST_SEND_SMS -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    sendSmsToSelected()
                } else {
                    toastOnUiThread("Send SMS permission denied")
                }
            }
        }
    }
}
