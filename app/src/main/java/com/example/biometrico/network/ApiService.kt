package com.example.biometrico.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

data class ResumenHome(
    val totalEntrenamientos: Int,
    val kmTotal: Double,
    val minutosTotal: Double,
    val pacePromedio: Double
)

data class Entrenamiento(
    val id: String,
    val texto: String,
    val kilometros: Double,
    val minutos: Double,
    val fecha: String
)

object ApiService {

    private const val BASE_URL = "https://biometrico-examen.onrender.com"

    suspend fun verificarConexion(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/ping")
            val conn = url.openConnection() as HttpURLConnection
            conn.connectTimeout = 3000
            conn.readTimeout = 3000
            conn.requestMethod = "GET"
            val code = conn.responseCode
            conn.disconnect()
            code == 200
        } catch (e: Exception) {
            false
        }
    }

    suspend fun guardarEntrenamiento(
        texto: String,
        kilometros: Double,
        minutos: Double
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/entrenamientos")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            val body = JSONObject().apply {
                put("texto", texto)
                put("kilometros", kilometros)
                put("minutos", minutos)
                put("fecha", java.time.Instant.now().toString())
            }.toString()

            OutputStreamWriter(conn.outputStream).use { it.write(body) }

            val code = conn.responseCode
            conn.disconnect()
            if (code == 201) Result.success("ok") else Result.failure(Exception("Error $code"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun obtenerEntrenamientos(): Result<List<Entrenamiento>> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/entrenamientos")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 5000

            val text = BufferedReader(InputStreamReader(conn.inputStream)).readText()
            conn.disconnect()

            val array = JSONArray(text)
            val lista = mutableListOf<Entrenamiento>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                lista.add(
                    Entrenamiento(
                        id = obj.optString("_id", ""),
                        texto = obj.optString("texto", ""),
                        kilometros = obj.optDouble("kilometros", 0.0),
                        minutos = obj.optDouble("minutos", 0.0),
                        fecha = obj.optString("fecha", "")
                    )
                )
            }
            Result.success(lista)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun obtenerResumen(): Result<ResumenHome> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/resumen")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 5000

            val text = BufferedReader(InputStreamReader(conn.inputStream)).readText()
            conn.disconnect()

            val json = JSONObject(text)
            Result.success(
                ResumenHome(
                    totalEntrenamientos = json.getInt("totalEntrenamientos"),
                    kmTotal = json.getDouble("kmTotal"),
                    minutosTotal = json.getDouble("minTotal"),
                    pacePromedio = json.getDouble("pacePromedio")
                )
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun obtenerRacha(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val url = URL("$BASE_URL/racha")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 5000

            val text = BufferedReader(InputStreamReader(conn.inputStream)).readText()
            conn.disconnect()

            val json = JSONObject(text)
            Result.success(json.getInt("racha"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}