package com.example.contact_app_recycler_view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

import android.net.Uri
import android.widget.ImageView

class ContactAdapter(
    private val contactList: MutableList<Contact>,
    private val listener: OnContactActionListener
) : RecyclerView.Adapter<ContactAdapter.ContactViewHolder>() {

    interface OnContactActionListener {
        fun onItemClick(position: Int)
        fun onEditClick(position: Int)
        fun onDeleteClick(position: Int)
    }

    class ContactViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvContactName: TextView = itemView.findViewById(R.id.tvContactName)
        val tvContactPhone: TextView = itemView.findViewById(R.id.tvContactPhone)
        val btnEdit: Button = itemView.findViewById(R.id.btnEdit)
        val btnDelete: Button = itemView.findViewById(R.id.btnDelete)
        val imgProfile: ImageView = itemView.findViewById(R.id.imgProfile)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ContactViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.activity_item_contact, parent, false)
        return ContactViewHolder(view)
    }

    override fun onBindViewHolder(holder: ContactViewHolder, position: Int) {
        val contact = contactList[position]

        holder.tvContactName.text = contact.name
        holder.tvContactPhone.text = contact.phone

        if (contact.imageUri != null) {
            holder.imgProfile.setImageURI(Uri.parse(contact.imageUri))
        } else {
            holder.imgProfile.setImageResource(R.mipmap.ic_launcher)
        }

        holder.itemView.setOnClickListener { listener.onItemClick(position) }
        holder.btnEdit.setOnClickListener { listener.onEditClick(position) }
        holder.btnDelete.setOnClickListener { listener.onDeleteClick(position) }
    }

    override fun getItemCount() = contactList.size
}