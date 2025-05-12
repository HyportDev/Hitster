package com.example.hitster.data

class AccessTokenProvider {
    private var accessToken: String? = null

    fun setAccessToken(token: String) {
        accessToken = token
    }

    fun getAccessToken(): String? {
        return accessToken
    }
}