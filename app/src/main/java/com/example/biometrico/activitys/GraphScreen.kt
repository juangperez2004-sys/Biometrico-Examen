package com.example.biometrico.activitys

import android.graphics.Color as AndroidColor
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.biometrico.components.AppSubtitle
import com.example.biometrico.components.AppTitle
import com.example.biometrico.components.FilledTonalBoton
import com.example.biometrico.network.ApiService
import com.example.biometrico.network.Entrenamiento
import com.example.biometrico.ui.theme.*
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.*
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import kotlinx.coroutines.launch


// las dos metricas que se pueden ver en la grafica
enum class MetricaVista { KILOMETROS, MINUTOS }


// convierte fecha ISO a formato dd/MM para mostrar en la grafica
// ej: "2024-05-12T..." -> "12/05"
fun formatearFecha(fechaIso: String): String {
    return try {
        val parte = fechaIso.take(10)
        val partes = parte.split("-")
        if (partes.size >= 3) "${partes[2]}/${partes[1]}" else fechaIso.take(5)
    } catch (_: Exception) {
        fechaIso.take(5)
    }
}


@Composable
fun GraphScreen(onVolver: () -> Unit = {}) {

    val scope = rememberCoroutineScope()
    var entrenamientos by remember { mutableStateOf<List<Entrenamiento>>(emptyList()) }
    var cargando by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf("") }
    var metricaVista by remember { mutableStateOf(MetricaVista.KILOMETROS) }

    // cargo los entrenamientos al entrar a la pantalla
    // los reverso para que aparezcan del mas viejo al mas nuevo en la grafica
    LaunchedEffect(Unit) {
        val resultado = ApiService.obtenerEntrenamientos()
        if (resultado.isSuccess) {
            entrenamientos = resultado.getOrDefault(emptyList()).reversed()
        } else {
            error = resultado.exceptionOrNull()?.message ?: "Error al cargar"
        }
        cargando = false
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

            AppTitle(text = "Mis Gráficas")
            Spacer(modifier = Modifier.height(6.dp))
            AppSubtitle(text = "Progreso real desde MongoDB Atlas")

            Spacer(modifier = Modifier.height(24.dp))

            // muestro algo distinto dependiendo del estado
            when {

                // todavia esperando respuesta del servidor
                cargando -> {
                    Spacer(modifier = Modifier.height(80.dp))
                    CircularProgressIndicator(color = SunsetOrange, strokeWidth = 3.dp)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Cargando datos...", color = SunsetWhite70, fontSize = 14.sp)
                }

                // hubo un error al conectar
                error.isNotEmpty() -> {
                    EstadoVacio(
                        icono = Icons.Default.WifiOff,
                        titulo = "Sin conexión al servidor",
                        subtitulo = "Verifica tu red\n$error",
                        color = SunsetMagenta
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    FilledTonalBoton(
                        onClick = {
                            scope.launch {
                                cargando = true
                                error = ""
                                val resultado = ApiService.obtenerEntrenamientos()
                                entrenamientos = if (resultado.isSuccess)
                                    resultado.getOrDefault(emptyList()).reversed()
                                else {
                                    error = resultado.exceptionOrNull()?.message ?: "Error"
                                    emptyList()
                                }
                                cargando = false
                            }
                        },
                        text = "Reintentar",
                        icon = Icons.Default.Refresh,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // conecto bien pero no hay nada guardado todavia
                entrenamientos.isEmpty() -> {
                    EstadoVacioAnimado()
                }

                // hay datos, muestro todo
                else -> {
                    ResumenCard(entrenamientos = entrenamientos)

                    Spacer(modifier = Modifier.height(24.dp))

                    // tabs para cambiar entre km y minutos
                    SelectorMetrica(
                        seleccionada = metricaVista,
                        onSeleccionar = { metricaVista = it }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    val tituloGrafica = when (metricaVista) {
                        MetricaVista.KILOMETROS -> "Kilómetros por sesión"
                        MetricaVista.MINUTOS -> "Minutos por sesión"
                    }

                    Text(
                        tituloGrafica,
                        color = SunsetWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // contenedor de la grafica
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(SunsetCard)
                            .padding(8.dp)
                    ) {
                        when (metricaVista) {
                            MetricaVista.KILOMETROS -> GraficaBarras(entrenamientos = entrenamientos)
                            MetricaVista.MINUTOS -> GraficaLineas(entrenamientos = entrenamientos)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // uso .last() porque la lista ya esta en orden cronologico
                    UltimoEntrenamientoCard(entrenamiento = entrenamientos.last())

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

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


// tabs para seleccionar que metrica ver en la grafica
@Composable
fun SelectorMetrica(seleccionada: MetricaVista, onSeleccionar: (MetricaVista) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(SunsetCard)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        MetricaTab(
            texto = "Kilómetros",
            seleccionado = seleccionada == MetricaVista.KILOMETROS,
            modifier = Modifier.weight(1f),
            onClick = { onSeleccionar(MetricaVista.KILOMETROS) }
        )
        MetricaTab(
            texto = "Minutos",
            seleccionado = seleccionada == MetricaVista.MINUTOS,
            modifier = Modifier.weight(1f),
            onClick = { onSeleccionar(MetricaVista.MINUTOS) }
        )
    }
}


// boton individual de cada tab, cambia de color si esta seleccionado
@Composable
fun MetricaTab(texto: String, seleccionado: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (seleccionado) SunsetOrange
            else androidx.compose.ui.graphics.Color.Transparent
        ),
        shape = RoundedCornerShape(10.dp),
        elevation = ButtonDefaults.buttonElevation(0.dp)
    ) {
        Text(
            texto,
            color = if (seleccionado) SunsetWhite else SunsetWhite70,
            fontWeight = if (seleccionado) FontWeight.Bold else FontWeight.Normal,
            fontSize = 13.sp
        )
    }
}


// grafica de barras para mostrar los kilometros de cada sesion
@Composable
fun GraficaBarras(entrenamientos: List<Entrenamiento>) {
    val naranja = SunsetOrange.toArgb()
    val blanco = SunsetWhite.toArgb()
    val blancoSuave = SunsetWhite70.toArgb()

    val etiquetas = entrenamientos.map { formatearFecha(it.fecha) }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            BarChart(context).apply {
                setBackgroundColor(AndroidColor.TRANSPARENT)
                description.isEnabled = false
                legend.isEnabled = false
                setTouchEnabled(true)
                setPinchZoom(false)
                setDrawGridBackground(false)
                setDrawBarShadow(false)
                setDrawValueAboveBar(true)
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    textColor = blanco
                    textSize = 10f
                    granularity = 1f
                    labelRotationAngle = -30f
                }
                axisLeft.apply {
                    textColor = blancoSuave
                    textSize = 10f
                    setDrawGridLines(true)
                    gridColor = AndroidColor.argb(40, 255, 255, 255)
                    axisMinimum = 0f
                }
                axisRight.isEnabled = false
                extraBottomOffset = 10f
            }
        },
        update = { chart ->
            val entradas = entrenamientos.mapIndexed { i, e ->
                BarEntry(i.toFloat(), e.kilometros.toFloat())
            }
            val dataSet = BarDataSet(entradas, "Kilómetros").apply {
                color = naranja
                valueTextColor = blanco
                valueTextSize = 11f
                valueFormatter = object : ValueFormatter() {
                    override fun getBarLabel(barEntry: BarEntry) =
                        "%.1f km".format(barEntry.y)
                }
            }
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(etiquetas)
            chart.data = BarData(dataSet).apply { barWidth = 0.6f }
            chart.animateY(900)
            chart.invalidate()
        }
    )
}


// grafica de linea para mostrar los minutos de cada sesion
@Composable
fun GraficaLineas(entrenamientos: List<Entrenamiento>) {
    val magenta = SunsetMagenta.toArgb()
    val blanco = SunsetWhite.toArgb()
    val blancoSuave = SunsetWhite70.toArgb()

    val etiquetas = entrenamientos.map { formatearFecha(it.fecha) }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { context ->
            LineChart(context).apply {
                setBackgroundColor(AndroidColor.TRANSPARENT)
                description.isEnabled = false
                legend.isEnabled = false
                setTouchEnabled(true)
                setPinchZoom(false)
                setDrawGridBackground(false)
                xAxis.apply {
                    position = XAxis.XAxisPosition.BOTTOM
                    setDrawGridLines(false)
                    textColor = blanco
                    textSize = 10f
                    granularity = 1f
                    labelRotationAngle = -30f
                }
                axisLeft.apply {
                    textColor = blancoSuave
                    textSize = 10f
                    setDrawGridLines(true)
                    gridColor = AndroidColor.argb(40, 255, 255, 255)
                    axisMinimum = 0f
                }
                axisRight.isEnabled = false
                extraBottomOffset = 10f
            }
        },
        update = { chart ->
            val entradas = entrenamientos.mapIndexed { i, e ->
                Entry(i.toFloat(), e.minutos.toFloat())
            }
            val dataSet = LineDataSet(entradas, "Minutos").apply {
                color = magenta
                valueTextColor = blanco
                valueTextSize = 11f
                lineWidth = 2.5f
                circleRadius = 5f
                setCircleColor(magenta)
                circleHoleColor = AndroidColor.TRANSPARENT
                setDrawFilled(true)
                fillColor = magenta
                fillAlpha = 50
                mode = LineDataSet.Mode.CUBIC_BEZIER
                valueFormatter = object : ValueFormatter() {
                    override fun getPointLabel(entry: Entry) =
                        "%.0f min".format(entry.y)
                }
            }
            chart.xAxis.valueFormatter = IndexAxisValueFormatter(etiquetas)
            chart.data = LineData(dataSet)
            chart.animateY(900)
            chart.invalidate()
        }
    )
}


// pantalla que se muestra cuando no hay entrenamientos aun
// el icono parpadea para llamar la atencion
@Composable
fun EstadoVacioAnimado() {
    val infiniteTransition = rememberInfiniteTransition(label = "empty")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ), label = "emptyAlpha"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp)
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(SunsetCard),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.BarChart,
                contentDescription = null,
                tint = SunsetOrange.copy(alpha = alpha),
                modifier = Modifier.size(64.dp)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            "Aún no hay entrenamientos",
            color = SunsetWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "Registra tu primera sesión\nusando el micrófono",
            color = SunsetWhite70,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // ejemplos de lo que puede decir el usuario
        listOf(
            "\"Corrí 5 km en 30 minutos\"",
            "\"Hice 10 kilómetros en 45 min\"",
            "\"Cuarenta minutos, 8 km\""
        ).forEach { tip ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = SunsetCard),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    tip,
                    modifier = Modifier.padding(12.dp),
                    color = SunsetWhite70,
                    fontSize = 13.sp
                )
            }
        }
    }
}


// pantalla generica de error o vacio, recibe el icono y colores como parametros
@Composable
fun EstadoVacio(
    icono: androidx.compose.ui.graphics.vector.ImageVector,
    titulo: String,
    subtitulo: String,
    color: androidx.compose.ui.graphics.Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(vertical = 32.dp)
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icono, null, tint = color, modifier = Modifier.size(40.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(titulo, color = SunsetWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Spacer(modifier = Modifier.height(6.dp))
        Text(subtitulo, color = SunsetWhite70, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}


// card que muestra los datos del entrenamiento mas reciente
@Composable
fun UltimoEntrenamientoCard(entrenamiento: Entrenamiento) {
    val km = entrenamiento.kilometros
    val min = entrenamiento.minutos
    val pace = if (km > 0) "%.1f min/km".format(min / km) else "—"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SunsetOrange.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.FitnessCenter,
                    null,
                    tint = SunsetOrange,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "Último entrenamiento",
                    color = SunsetOrange,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    formatearFecha(entrenamiento.fecha),
                    color = SunsetWhite40,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ResumenItem("%.1f km".format(km), "Distancia")
                ResumenItem("%.0f min".format(min), "Tiempo")
                ResumenItem(pace, "Pace")
            }
        }
    }
}


// card con los totales de todos los entrenamientos juntos
@Composable
fun ResumenCard(entrenamientos: List<Entrenamiento>) {
    val totalKm = entrenamientos.sumOf { it.kilometros }
    val totalMin = entrenamientos.sumOf { it.minutos }
    val totalSesiones = entrenamientos.size
    val pacePromedio = if (totalKm > 0) "%.1f".format(totalMin / totalKm) else "—"

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(SunsetCard)
            .padding(20.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                "Resumen total",
                color = SunsetWhite70,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ResumenItem("$totalSesiones", "Sesiones")
                ResumenItem("%.1f".format(totalKm), "Km totales")
                ResumenItem("%.0f".format(totalMin), "Minutos")
                ResumenItem("$pacePromedio", "Pace prom.")
            }
        }
    }
}


// componente pequeño de valor + etiqueta, se reutiliza en varias cards
@Composable
fun ResumenItem(valor: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(valor, color = SunsetWhite, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Text(label, color = SunsetWhite70, fontSize = 10.sp, textAlign = TextAlign.Center)
    }
}