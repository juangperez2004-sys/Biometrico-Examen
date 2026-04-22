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


// los datos que muestra la pantalla de inicio
data class ResumenHome(
    val totalEntrenamientos: Int,
    val kmTotal: Double,
    val minutosTotal: Double,
    val pacePromedio: Double
)

// como se ve cada entrenamiento cuando lo traigo del servidor
data class Entrenamiento(
    val id: String,
    val texto: String,
    val kilometros: Double,
    val minutos: Double,
    val fecha: String
)


object ApiService {

    private const val BASE_URL = "https://biometrico-examen.onrender.com"


    // funcion auxiliar para no repetir el mismo codigo de conexion en cada llamada
    // hace un GET y devuelve el texto de la respuesta
    private fun get(ruta: String, timeout: Int = 5000): String {
        val conn = URL("$BASE_URL$ruta").openConnection() as HttpURLConnection
        conn.requestMethod = "GET"
        conn.connectTimeout = timeout
        conn.readTimeout = timeout

        val respuesta = BufferedReader(InputStreamReader(conn.inputStream)).readText()
        conn.disconnect()
        return respuesta
    }


    // checa si el servidor esta encendido, regresa true o false
    suspend fun verificarConexion(): Boolean = withContext(Dispatchers.IO) {
        try {
            val conn = URL("$BASE_URL/ping").openConnection() as HttpURLConnection
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


    // manda un entrenamiento nuevo al servidor
    suspend fun guardarEntrenamiento(
        texto: String,
        kilometros: Double,
        minutos: Double
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val conn = URL("$BASE_URL/entrenamientos").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.doOutput = true

            // armo el json con los datos del entrenamiento
            val body = JSONObject().apply {
                put("texto", texto)
                put("kilometros", kilometros)
                put("minutos", minutos)
                put("fecha", java.time.Instant.now().toString())
            }.toString()

            OutputStreamWriter(conn.outputStream).use { it.write(body) }

            val code = conn.responseCode
            conn.disconnect()

            // 201 significa que se creo bien en el servidor
            if (code == 201) Result.success("ok") else Result.failure(Exception("Error $code"))

        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    // trae la lista de entrenamientos y la convierte a objetos Entrenamiento
    suspend fun obtenerEntrenamientos(): Result<List<Entrenamiento>> = withContext(Dispatchers.IO) {
        try {
            val texto = get("/entrenamientos")
            val array = JSONArray(texto)

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


    // trae el resumen general: km totales, pace promedio, etc
    suspend fun obtenerResumen(): Result<ResumenHome> = withContext(Dispatchers.IO) {
        try {
            val texto = get("/resumen")
            val json = JSONObject(texto)

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


    // trae cuantos dias seguidos lleva entrenando
    suspend fun obtenerRacha(): Result<Int> = withContext(Dispatchers.IO) {
        try {
            val texto = get("/racha")
            val json = JSONObject(texto)

            Result.success(json.getInt("racha"))

        } catch (e: Exception) {
            Result.failure(e)
        }
    }

}