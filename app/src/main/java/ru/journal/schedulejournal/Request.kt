package ru.journal.schedulejournal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response

data class Header(
    val name: String,
    val value: String
)

data class RequestBodyItem(
    val key: String,
    val value: String?
)

class Requests (
    private val client: OkHttpClient = OkHttpClient()
){
    suspend fun getRequest(url: String, headers: List<Header>): String? = withContext(
        Dispatchers.IO) {
        val requestBuilder: Request.Builder = Request.Builder().url(url)

        headers.forEach { header ->
            requestBuilder.header(header.name, header.value)
        }

        val request: Request = requestBuilder.build()

        try {
            val response: Response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                throw Exception("Ошибка в GET запросе: ${response.code} - ${response.body?.string()}")
                return@withContext null
            }
            response.body?.string()
        } catch (e: Exception) {
            e.printStackTrace()
            println("Ошибка при отправке GET запроса: ${e.message}")
            null
        }
    }

    suspend fun postRequest(url: String, headers: List<Header>, body: List<RequestBodyItem>): String? = withContext(
        Dispatchers.IO) {
        val requestBuilder: Request.Builder = Request.Builder().url(url)
        val mediaType = "application/json; charset=utf-8".toMediaType()

        headers.forEach { header ->
            requestBuilder.header(header.name, header.value)
        }

        requestBuilder.post(Json.encodeToString(body.associate{it.key to it.value}).toRequestBody(mediaType))

        val request: Request = requestBuilder.build()

        try {
            val response: Response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                throw Exception("Ошибка в POST запросе: ${response.code} - ${response.body?.string()}")
                return@withContext null
            }
            response.body?.string()
        } catch (e: Exception) {
            e.printStackTrace()
            println("Ошибка при отправке POST запроса: ${e.message}")
            null
        }
    }
}