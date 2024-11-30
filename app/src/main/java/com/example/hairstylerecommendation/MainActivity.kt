package com.example.hairstylerecommendation

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.hairstylerecommendation.adapter.UserAdapter
import com.example.hairstylerecommendation.api.RetrofitInstance
import com.example.hairstylerecommendation.models.User
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        val btnNavigate: Button = findViewById(R.id.btnNavigate)
        btnNavigate.setOnClickListener {
            navigateToSecondActivity()
        }

        RetrofitInstance.api.getUsers().enqueue(object : Callback<List<User>> {
            override fun onResponse(call: Call<List<User>>, response: Response<List<User>>) {
                if (response.isSuccessful) {
                    val users = response.body() ?: emptyList()
                    recyclerView.adapter = UserAdapter(users)
                } else {
                    Toast.makeText(this@MainActivity, "Error: ${response.message()}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onFailure(call: Call<List<User>>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Failed: ${t.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun navigateToSecondActivity() {
        val intent = Intent(this, SecondActivity::class.java)
        startActivity(intent)
    }

}
