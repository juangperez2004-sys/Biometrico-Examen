package com.example.biometrico.activitys

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.biometrico.components.AppSubtitle
import com.example.biometrico.components.AppTitle
import com.example.biometrico.components.FilledTonalBoton
import com.example.biometrico.network.ApiService
import com.example.biometrico.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.*


// los posibles estados de la pantalla de voz
enum class VoiceEstado { IDLE, ESCUCHANDO, CONFIRMANDO, GUARDANDO, EXITO, ERROR }


// diccionario para convertir numeros escritos en letras a digitos
// ej: "cinco" -> "5", "veinte" -> "20"
private val numerosLetras = mapOf(
    "cero" to "0", "uno" to "1", "una" to "1", "dos" to "2",
    "tres" to "3", "cuatro" to "4", "cinco" to "5", "seis" to "6",
    "siete" to "7", "ocho" to "8", "nueve" to "9", "diez" to "10",
    "once" to "11", "doce" to "12", "trece" to "13", "catorce" to "14",
    "quince" to "15", "dieciséis" to "16", "diecisiete" to "17",
    "dieciocho" to "18", "diecinueve" to "19", "veinte" to "20",
    "veintiuno" to "21", "veintidós" to "22", "veintitrés" to "23",
    "veinticuatro" to "24", "veinticinco" to "25", "treinta" to "30",
    "cuarenta" to "40", "cincuenta" to "50", "sesenta" to "60",
    "setenta" to "70", "ochenta" to "80", "noventa" to "90",
    "cien" to "100", "ciento" to "100"
)


// convierte el texto reconocido a minusculas y reemplaza palabras numericas
// tambien maneja compuestos como "treinta y dos" -> "32"
fun normalizarTexto(texto: String): String {
    var t = texto.lowercase()

    val compuestoRegex = Regex(
        """(veinte|treinta|cuarenta|cincuenta|sesenta|setenta|ochenta|noventa)\s+y\s+""" +
                """(uno|dos|tres|cuatro|cinco|seis|siete|ocho|nueve|una)"""
    )

    t = compuestoRegex.replace(t) { mr ->
        val decena = numerosLetras[mr.groupValues[1]]?.toIntOrNull() ?: 0
        val unidad = numerosLetras[mr.groupValues[2]]?.toIntOrNull() ?: 0
        (decena + unidad).toString()
    }

    numerosLetras.forEach { (palabra, numero) ->
        t = t.replace(Regex("""\b$palabra\b"""), numero)
    }

    return t
}


// busca kilometros y minutos dentro del texto que dijo el usuario
// regresa un par (km, min) como strings, vacios si no encuentra algo
fun extraerMetricas(texto: String): Pair<String, String> {
    val t = normalizarTexto(texto)

    val kmRegex = Regex("""(\d+(?:[.,]\d+)?)\s*(?:km|kilómetros?|kilometros?|kms?)""")
    val minRegex = Regex(
        """(\d+(?:[.,]\d+)?)\s*(?:min(?:utos?)?)|""" +
                """(?:tardé|tarde|duré|dure|en)\s+(\d+(?:[.,]\d+)?)(?:\s+min(?:utos?)?)?"""
    )

    val km = kmRegex.find(t)?.groupValues?.get(1)?.replace(",", ".") ?: ""

    var min = ""
    val minMatch = minRegex.find(t)
    if (minMatch != null) {
        min = (minMatch.groupValues[1].ifEmpty { minMatch.groupValues[2] }).replace(",", ".")
    }

    return Pair(km, min)
}


// hace vibrar el telefono con un patron personalizado
// funciona tanto en android nuevo como en versiones viejas
fun vibrar(context: Context, patron: LongArray = longArrayOf(0, 80, 60, 80)) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as android.os.VibratorManager
        vm.defaultVibrator
    } else {
        @Suppress("DEPRECATION")
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        vibrator.vibrate(VibrationEffect.createWaveform(patron, -1))
    } else {
        @Suppress("DEPRECATION")
        vibrator.vibrate(patron, -1)
    }
}


@Composable
fun VoiceScreen(onVolver: () -> Unit = {}) {

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var estado by remember { mutableStateOf(VoiceEstado.IDLE) }
    var textoReconocido by remember { mutableStateOf("") }
    var textoParcial by remember { mutableStateOf("") }
    var kilometros by remember { mutableStateOf("") }
    var minutos by remember { mutableStateOf("") }
    var mensajeError by remember { mutableStateOf("") }
    var rmsLevel by remember { mutableStateOf(0f) }
    var timeoutJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    // revisa si ya tenemos permiso de microfono
    fun tienePermiso() = ContextCompat.checkSelfPermission(
        context, Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    // pide el permiso al usuario si no lo tiene
    fun pedirPermiso() {
        ActivityCompat.requestPermissions(
            context as Activity,
            arrayOf(Manifest.permission.RECORD_AUDIO),
            1001
        )
    }

    val speechRecognizer = remember { SpeechRecognizer.createSpeechRecognizer(context) }

    // listener que maneja todos los eventos del reconocedor de voz
    val recognitionListener = remember {
        object : RecognitionListener {

            override fun onReadyForSpeech(p0: Bundle?) {
                vibrar(context, longArrayOf(0, 60))
                // si en 5 segundos no habla, cancelo solo
                timeoutJob = scope.launch {
                    delay(5000)
                    if (estado == VoiceEstado.ESCUCHANDO) {
                        speechRecognizer.stopListening()
                        rmsLevel = 0f
                        estado = VoiceEstado.IDLE
                    }
                }
            }

            override fun onBeginningOfSpeech() {
                // ya empezo a hablar, cancelo el timeout
                timeoutJob?.cancel()
            }

            override fun onRmsChanged(rmsdB: Float) {
                // nivel de volumen del micro, lo uso para la animacion de ondas
                rmsLevel = (rmsdB / 10f).coerceIn(0f, 1f)
            }

            override fun onBufferReceived(p0: ByteArray?) {}

            override fun onEndOfSpeech() {
                textoParcial = ""
                rmsLevel = 0f
            }

            override fun onPartialResults(partialResults: Bundle?) {
                // texto en tiempo real mientras habla
                val parcial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!parcial.isNullOrEmpty()) {
                    textoParcial = parcial[0]
                    timeoutJob?.cancel()
                }
            }

            override fun onResults(results: Bundle?) {
                timeoutJob?.cancel()
                rmsLevel = 0f

                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    textoReconocido = matches[0]
                    textoParcial = ""
                    val (km, min) = extraerMetricas(textoReconocido)
                    kilometros = km
                    minutos = min
                    estado = VoiceEstado.CONFIRMANDO
                } else {
                    mensajeError = "No se detectó texto. Intenta de nuevo."
                    estado = VoiceEstado.ERROR
                }
            }

            override fun onError(error: Int) {
                timeoutJob?.cancel()
                textoParcial = ""
                rmsLevel = 0f

                mensajeError = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "No se entendió. Intenta de nuevo."
                    SpeechRecognizer.ERROR_NETWORK -> "Sin conexión de red."
                    SpeechRecognizer.ERROR_AUDIO -> "Error de micrófono."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No se detectó voz. Intenta de nuevo."
                    else -> "Error al reconocer voz ($error)."
                }
                estado = VoiceEstado.ERROR
            }

            override fun onEvent(p0: Int, p1: Bundle?) {}
        }
    }

    // conecto el listener y destruyo el recognizer cuando salgo de la pantalla
    DisposableEffect(Unit) {
        speechRecognizer.setRecognitionListener(recognitionListener)
        onDispose {
            timeoutJob?.cancel()
            speechRecognizer.destroy()
        }
    }

    // arranca el reconocimiento de voz en español
    fun iniciarEscucha() {
        textoParcial = ""
        rmsLevel = 0f
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-MX")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Ej: Corrí 5 km en 30 minutos")
        }
        estado = VoiceEstado.ESCUCHANDO
        speechRecognizer.startListening(intent)
    }

    // manda el entrenamiento al servidor y regresa al home si sale bien
    fun guardarEntrenamiento() {
        scope.launch {
            estado = VoiceEstado.GUARDANDO
            val resultado = ApiService.guardarEntrenamiento(
                texto = textoReconocido,
                kilometros = kilometros.toDoubleOrNull() ?: 0.0,
                minutos = minutos.toDoubleOrNull() ?: 0.0
            )
            if (resultado.isSuccess) {
                vibrar(context, longArrayOf(0, 80, 60, 80, 60, 150))
                estado = VoiceEstado.EXITO
                delay(3000)
                onVolver()
            } else {
                mensajeError = resultado.exceptionOrNull()?.message ?: "Error desconocido"
                estado = VoiceEstado.ERROR
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SunsetBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            AppTitle(text = "Captura por Voz")
            Spacer(modifier = Modifier.height(6.dp))
            AppSubtitle(text = "Di: \"Corrí 5 km en 30 minutos\"")
            Spacer(modifier = Modifier.height(40.dp))

            // muestro el boton de microfono segun el estado actual
            when (estado) {
                VoiceEstado.IDLE, VoiceEstado.ERROR -> {
                    MicBoton(activo = false, rmsLevel = 0f, onClick = {
                        if (tienePermiso()) iniciarEscucha() else pedirPermiso()
                    })
                }
                VoiceEstado.ESCUCHANDO -> {
                    MicBoton(activo = true, rmsLevel = rmsLevel, onClick = {
                        // si toca el boton mientras escucha, cancela
                        timeoutJob?.cancel()
                        speechRecognizer.stopListening()
                        textoParcial = ""
                        rmsLevel = 0f
                        estado = VoiceEstado.IDLE
                    })
                }
                else -> MicBoton(activo = false, rmsLevel = 0f, onClick = {})
            }

            Spacer(modifier = Modifier.height(32.dp))

            // cada estado muestra algo diferente debajo del microfono
            when (estado) {

                VoiceEstado.IDLE ->
                    AppSubtitle(text = "Presiona el micrófono y habla")

                VoiceEstado.ESCUCHANDO -> {
                    Text(
                        "Escuchando... (5s sin voz cancela)",
                        color = SunsetOrange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                    // muestro lo que va reconociendo en tiempo real
                    if (textoParcial.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(SunsetCard)
                                .padding(16.dp)
                        ) {
                            Text(
                                textoParcial,
                                color = SunsetWhite,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                VoiceEstado.CONFIRMANDO -> {
                    PantallaConfirmacion(
                        texto = textoReconocido,
                        kilometros = kilometros,
                        minutos = minutos,
                        onKilometrosChange = { kilometros = it },
                        onMinutosChange = { minutos = it },
                        onConfirmar = { guardarEntrenamiento() },
                        onReintentar = {
                            textoReconocido = ""
                            kilometros = ""
                            minutos = ""
                            estado = VoiceEstado.IDLE
                        }
                    )
                }

                VoiceEstado.GUARDANDO -> {
                    CircularProgressIndicator(color = SunsetOrange)
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Guardando datos...", color = SunsetWhite70, fontSize = 14.sp)
                }

                VoiceEstado.EXITO -> {
                    PantallaExito(
                        kilometros = kilometros,
                        minutos = minutos,
                        onNuevo = {
                            textoReconocido = ""
                            kilometros = ""
                            minutos = ""
                            estado = VoiceEstado.IDLE
                        }
                    )
                }

                VoiceEstado.ERROR -> {
                    Text(
                        mensajeError,
                        color = SunsetMagenta,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    FilledTonalBoton(
                        onClick = { estado = VoiceEstado.IDLE },
                        text = "Intentar de nuevo",
                        icon = Icons.Default.Refresh,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            FilledTonalBoton(
                onClick = onVolver,
                text = "Volver al inicio",
                icon = Icons.Default.ArrowBack,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}


// boton circular del microfono con animacion de ondas cuando esta activo
@Composable
fun MicBoton(activo: Boolean, rmsLevel: Float, onClick: () -> Unit) {

    val infiniteTransition = rememberInfiniteTransition(label = "mic")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (activo) 1.12f else 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "micScale"
    )

    // tres ondas con velocidades distintas para que se vea organico
    val wave1 by infiniteTransition.animateFloat(
        initialValue = 0.6f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(600, easing = EaseInOut), RepeatMode.Reverse),
        label = "w1"
    )
    val wave2 by infiniteTransition.animateFloat(
        initialValue = 0.7f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(800, easing = EaseInOut), RepeatMode.Reverse),
        label = "w2"
    )
    val wave3 by infiniteTransition.animateFloat(
        initialValue = 0.8f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1000, easing = EaseInOut), RepeatMode.Reverse),
        label = "w3"
    )

    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(220.dp)) {
        if (activo) {
            val waveAlpha = 0.15f + rmsLevel * 0.2f
            OndaCircular(size = 220.dp, alpha = waveAlpha * wave3, color = SunsetOrange)
            OndaCircular(size = 190.dp, alpha = waveAlpha * wave2 + 0.05f, color = SunsetOrange)
            OndaCircular(size = 165.dp, alpha = waveAlpha * wave1 + 0.08f, color = SunsetMagenta)
        }

        Box(
            modifier = Modifier
                .size(160.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        if (activo) listOf(SunsetOrange, SunsetMagenta)
                        else listOf(SunsetCard, SunsetSurface)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            IconButton(onClick = onClick, modifier = Modifier.fillMaxSize()) {
                Icon(
                    imageVector = if (activo) Icons.Default.MicOff else Icons.Default.Mic,
                    contentDescription = null,
                    tint = SunsetWhite,
                    modifier = Modifier.size(72.dp)
                )
            }
        }
    }
}


// circulo semitransparente que forma las ondas del microfono
@Composable
fun OndaCircular(size: Dp, alpha: Float, color: Color) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(color.copy(alpha = alpha.coerceIn(0f, 1f)))
    )
}


// pantalla que muestra lo que entendio el sistema y deja editar los datos antes de guardar
@Composable
fun PantallaConfirmacion(
    texto: String,
    kilometros: String,
    minutos: String,
    onKilometrosChange: (String) -> Unit,
    onMinutosChange: (String) -> Unit,
    onConfirmar: () -> Unit,
    onReintentar: () -> Unit
) {
    val ambosDetectados = kilometros.isNotEmpty() && minutos.isNotEmpty()

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // si detecto los dos datos muestro el resumen, si no aviso lo que falta
        if (ambosDetectados) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SunsetOrange.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Métricas detectadas",
                        color = SunsetOrange,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        MetricaPreview(valor = "$kilometros km", label = "Distancia", icon = Icons.Default.DirectionsRun)
                        MetricaPreview(valor = "$minutos min", label = "Tiempo", icon = Icons.Default.Timer)
                        val km = kilometros.toDoubleOrNull() ?: 0.0
                        val min = minutos.toDoubleOrNull() ?: 0.0
                        val pace = if (km > 0) "%.1f min/km".format(min / km) else "—"
                        MetricaPreview(valor = pace, label = "Pace", icon = Icons.Default.Speed)
                    }
                }
            }
        } else {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SunsetMagenta.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Info, null, tint = SunsetMagenta, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "No detecté ${if (kilometros.isEmpty()) "km" else ""}${if (kilometros.isEmpty() && minutos.isEmpty()) " ni " else ""}${if (minutos.isEmpty()) "minutos" else ""}. Completa los datos.",
                        color = SunsetMagenta,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // muestro el texto original que dijo el usuario
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(SunsetCard)
                .padding(14.dp)
        ) {
            Column {
                Text("Texto reconocido:", color = SunsetWhite70, fontSize = 11.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(texto, color = SunsetWhite, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // campos para corregir los datos si el reconocimiento no fue exacto
        OutlinedTextField(
            value = kilometros,
            onValueChange = onKilometrosChange,
            label = { Text("Kilómetros", color = SunsetWhite70) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SunsetOrange,
                unfocusedBorderColor = SunsetWhite40,
                focusedTextColor = SunsetWhite,
                unfocusedTextColor = SunsetWhite
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = minutos,
            onValueChange = onMinutosChange,
            label = { Text("Minutos", color = SunsetWhite70) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = SunsetOrange,
                unfocusedBorderColor = SunsetWhite40,
                focusedTextColor = SunsetWhite,
                unfocusedTextColor = SunsetWhite
            )
        )

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onConfirmar,
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp),
            colors = ButtonDefaults.buttonColors(containerColor = SunsetOrange),
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.CloudUpload, null, tint = SunsetWhite, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                "Guardar entrenamiento",
                color = SunsetWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        FilledTonalBoton(
            onClick = onReintentar,
            text = "Reintentar voz",
            icon = Icons.Default.Refresh,
            modifier = Modifier.fillMaxWidth()
        )
    }
}


// iconito con valor y etiqueta, se reutiliza en confirmacion y en exito
@Composable
fun MetricaPreview(
    valor: String,
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = SunsetOrange, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(valor, color = SunsetWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        Text(label, color = SunsetWhite70, fontSize = 10.sp)
    }
}


// pantalla de confirmacion cuando el guardado fue exitoso
// cuenta regresiva de 3 segundos y regresa al home solo
@Composable
fun PantallaExito(kilometros: String, minutos: String, onNuevo: () -> Unit) {

    var segundos by remember { mutableStateOf(3) }

    LaunchedEffect(Unit) {
        while (segundos > 0) {
            delay(1000)
            segundos--
        }
    }

    val km = kilometros.toDoubleOrNull() ?: 0.0
    val min = minutos.toDoubleOrNull() ?: 0.0
    val pace = if (km > 0) "%.1f min/km".format(min / km) else "—"

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(SunsetOrange.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.CloudDone, null, tint = SunsetOrange, modifier = Modifier.size(56.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Datos guardados correctamente",
            color = SunsetWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SunsetCard),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricaPreview("$kilometros km", "Distancia", Icons.Default.DirectionsRun)
                MetricaPreview("$minutos min", "Tiempo", Icons.Default.Timer)
                MetricaPreview(pace, "Pace", Icons.Default.Speed)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text("Volviendo al inicio en $segundos s...", color = SunsetWhite70, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(12.dp))

        FilledTonalBoton(
            onClick = onNuevo,
            text = "Nuevo entrenamiento",
            icon = Icons.Default.Add,
            modifier = Modifier.fillMaxWidth()
        )
    }
}