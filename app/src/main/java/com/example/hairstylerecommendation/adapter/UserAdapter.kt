package com.example.hairstylerecommendation.adapter


import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.hairstylerecommendation.R
import com.example.hairstylerecommendation.models.User


class UserAdapter(private val users: List<User>) : RecyclerView.Adapter<UserAdapter.UserViewHolder>() {
    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.tvName)
        val email: TextView = itemView.findViewById(R.id.tvEmail)
        val phone: TextView = itemView.findViewById(R.id.tvPhone)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.name.text = user.name
        holder.email.text = user.email
        holder.phone.text = user.phone
    }

    override fun getItemCount() = users.size
}
