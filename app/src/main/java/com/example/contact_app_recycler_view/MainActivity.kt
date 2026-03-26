package com.example.contact_app_recycler_view

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.ContactsContract
import android.view.LayoutInflater
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity(), ContactAdapter.OnContactActionListener {

    private lateinit var etName: EditText
    private lateinit var etPhone: EditText
    private lateinit var btnSave: Button
    private lateinit var btnLoadContacts: Button
    private lateinit var recyclerViewContacts: RecyclerView
    private lateinit var etSearch: EditText
    private lateinit var btnSort: Button
    private lateinit var contactAdapter: ContactAdapter
    private val contactList = mutableListOf<Contact>()
    private var filteredList = mutableListOf<Contact>()

    // Permission launcher
    private val contactPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                loadContactsAsync()
            } else {
                Toast.makeText(this, "Contacts permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // UI Components
        etName = findViewById(R.id.etName)
        etPhone = findViewById(R.id.etPhone)
        btnSave = findViewById(R.id.btnSave)
        btnLoadContacts = findViewById(R.id.btnLoadContacts)
        recyclerViewContacts = findViewById(R.id.recyclerViewContacts)

        // Add search EditText dynamically
        etSearch = EditText(this).apply {
            hint = "Search by name or phone"
        }
        (findViewById<LinearLayout>(R.id.main)).addView(etSearch, 2)

        // Sort Button
        btnSort = Button(this).apply { text = "Sort A-Z" }
        (findViewById<LinearLayout>(R.id.main)).addView(btnSort, 3)

        // RecyclerView setup
        contactAdapter = ContactAdapter(filteredList, this)
        recyclerViewContacts.layoutManager = LinearLayoutManager(this)
        recyclerViewContacts.adapter = contactAdapter

        // Save contact
        btnSave.setOnClickListener { saveContact() }

        // Load contacts from phone
        btnLoadContacts.setOnClickListener { checkPermissionAndLoadContacts() }

        // Search listener
        etSearch.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterContacts(s.toString())
            }
            override fun afterTextChanged(s: android.text.Editable?) {}
        })

        // Sort listener
        btnSort.setOnClickListener {
            contactList.sortBy { it.name.lowercase() }
            filterContacts(etSearch.text.toString())
        }
    }

    private fun saveContact() {
        val name = etName.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        if (!validateInputs(name, phone, etName, etPhone)) return

        val newContact = Contact(name, phone)
        contactList.add(newContact)
        filterContacts(etSearch.text.toString())

        Toast.makeText(this, "Contact saved successfully", Toast.LENGTH_SHORT).show()
        etName.text.clear()
        etPhone.text.clear()
    }

    private fun validateInputs(name: String, phone: String, nameInput: EditText, phoneInput: EditText): Boolean {
        var valid = true
        if (name.isEmpty()) { nameInput.error = "Name is required"; valid = false }
        if (phone.isEmpty()) { phoneInput.error = "Phone number required"; valid = false }
        else if (phone.length < 10 || !phone.all { it.isDigit() || it == '+' }) {
            phoneInput.error = "Enter valid phone number"; valid = false
        }
        return valid
    }

    private fun checkPermissionAndLoadContacts() {
        when {
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED -> {
                loadContactsAsync()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS) -> {
                AlertDialog.Builder(this)
                    .setTitle("Permission Required")
                    .setMessage("App needs permission to read contacts.")
                    .setPositiveButton("Grant") { _, _ ->
                        contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
                    }
                    .setNegativeButton("Deny", null)
                    .show()
            }
            else -> contactPermissionLauncher.launch(Manifest.permission.READ_CONTACTS)
        }
    }

    // Async loading with Coroutines
    private fun loadContactsAsync() {
        Toast.makeText(this, "Loading contacts...", Toast.LENGTH_SHORT).show()
        CoroutineScope(Dispatchers.Main).launch {
            val loadedContacts = withContext(Dispatchers.IO) { loadContactsFromPhone() }
            if (loadedContacts.isEmpty()) {
                Toast.makeText(this@MainActivity, "No contacts found", Toast.LENGTH_SHORT).show()
                return@launch
            }
            contactList.clear()
            contactList.addAll(loadedContacts)
            filterContacts(etSearch.text.toString())
            Toast.makeText(this@MainActivity, "${loadedContacts.size} contacts loaded", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadContactsFromPhone(): MutableList<Contact> {
        val contacts = mutableListOf<Contact>()
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.PHOTO_URI
        )
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection, null, null,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
        )
        cursor?.use {
            val nameIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val phoneIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
            val photoIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_URI)
            while (it.moveToNext()) {
                val name = it.getString(nameIndex) ?: ""
                val phone = it.getString(phoneIndex) ?: ""
                val photoUri = it.getString(photoIndex)
                if (name.isNotBlank() && phone.isNotBlank()) {
                    contacts.add(Contact(name, phone, photoUri))
                }
            }
        }
        return contacts
    }

    // Filter contacts for search
    private fun filterContacts(query: String) {
        filteredList.clear()
        if (query.isEmpty()) filteredList.addAll(contactList)
        else filteredList.addAll(contactList.filter {
            it.name.contains(query, true) || it.phone.contains(query)
        })
        contactAdapter.notifyDataSetChanged()
    }

    // RecyclerView listeners
    override fun onItemClick(position: Int) {
        val contact = filteredList[position]
        Toast.makeText(this, "Contact: ${contact.name}\nPhone: ${contact.phone}", Toast.LENGTH_SHORT).show()
    }

    override fun onEditClick(position: Int) {
        showEditDialog(position)
    }

    override fun onDeleteClick(position: Int) {
        showDeleteDialog(position)
    }

    private fun showDeleteDialog(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Delete Contact")
            .setMessage("Are you sure you want to delete this contact?")
            .setPositiveButton("Yes") { _, _ ->
                val contact = filteredList[position]
                contactList.remove(contact)
                filterContacts(etSearch.text.toString())
                Toast.makeText(this, "Contact deleted", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun showEditDialog(position: Int) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.activity_dialog_edit_item, null)
        val etEditName = dialogView.findViewById<EditText>(R.id.etEditName)
        val etEditPhone = dialogView.findViewById<EditText>(R.id.etEditPhone)

        val contact = filteredList[position]
        etEditName.setText(contact.name)
        etEditPhone.setText(contact.phone)

        val dialog = AlertDialog.Builder(this)
            .setTitle("Edit Contact")
            .setView(dialogView)
            .setPositiveButton("Update", null)
            .setNegativeButton("Cancel", null)
            .create()

        dialog.show()
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val updatedName = etEditName.text.toString().trim()
            val updatedPhone = etEditPhone.text.toString().trim()
            if (validateInputs(updatedName, updatedPhone, etEditName, etEditPhone)) {
                contact.name = updatedName
                contact.phone = updatedPhone
                filterContacts(etSearch.text.toString())
                Toast.makeText(this, "Contact updated", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
        }
    }
}