package ru.journal.schedulejournal.auth

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import ru.journal.schedulejournal.Header
import ru.journal.schedulejournal.RequestBodyItem
import ru.journal.schedulejournal.Requests
import java.io.File

@Serializable
data class AuthData(val login: String?, val password: String?, val accessToken: String?): java.io.Serializable

fun saveAuthData(context: Context, authData: AuthData, filename: String = "auth_data.json") {
    val jsonString = Json.encodeToString(AuthData.serializer(), authData)
    val file = File(context.filesDir, filename)
    try {
        file.writeText(jsonString)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

fun loadAuthData(context: Context, filename: String = "auth_data.json"): AuthData {
    val file = File(context.filesDir, filename)
    return try {
        val jsonString = file.readText()
        Json.decodeFromString(AuthData.serializer(), jsonString)
    } catch (e: Exception) {
        AuthData(null, null, null)
    }
}

suspend fun login(context: Context, userLogin: String, userPassword: String): Boolean {
    val APPLICATION_KEY = "6a56a5df2667e65aab73ce76d1dd737f7d1faef9c52e8b8c55ac75f565d8e8a6"
    val URL = "https://msapi.top-academy.ru/api/v2/auth/login"

    val headers: List<Header> = listOf(
        Header("Content-Type", "application/json"),
        Header("Accept", "application/json, text/plain, */*"),
        Header("Origin", "https://journal.top-academy.ru"),
        Header("Referer", "https://journal.top-academy.ru")
    )

    val body: List<RequestBodyItem> = listOf(
        RequestBodyItem("application_key", APPLICATION_KEY),
        RequestBodyItem("id_city", null),
        RequestBodyItem("password", userPassword),
        RequestBodyItem("username", userLogin)
    )

    return withContext(Dispatchers.IO) {
        val request: Requests = Requests()

        val response: String = request.postRequest(URL, headers, body).toString()
        if (response != "null") {
            val accessToken: String? = Json.parseToJsonElement(response).jsonObject["access_token"]?.jsonPrimitive?.content
            val newAuthData = AuthData(userLogin, userPassword, accessToken)
            saveAuthData(context, newAuthData)
            return@withContext true
        } else {
            return@withContext false
        }
    }
}