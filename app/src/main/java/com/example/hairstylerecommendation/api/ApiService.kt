package com.example.hairstylerecommendation.api


import com.example.hairstylerecommendation.models.User

import retrofit2.http.GET

interface ApiService {
    @GET("users")
    fun getUsers(): retrofit2.Call<List<User>>
}