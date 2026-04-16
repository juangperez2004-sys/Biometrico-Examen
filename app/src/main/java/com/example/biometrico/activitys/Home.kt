package com.example.biometrico.activitys

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.biometrico.components.AppSubtitle
import com.example.biometrico.components.AppTitle
import com.example.biometrico.components.FilledTonalBoton
import com.example.biometrico.components.SunsetCard
import com.example.biometrico.network.ApiService
import com.example.biometrico.network.ResumenHome
import com.example.biometrico.ui.theme.*

@Composable
fun Home(
    onIrAVoz: () -> Unit = {},
    onIrAGrafica: () -> Unit = {},
    onCerrarSesion: () -> Unit = {}
) {
    var resumen by remember { mutableStateOf<ResumenHome?>(null) }
    var racha by remember { mutableStateOf(0) }
    var cargando by remember { mutableStateOf(true) }
    var errorConexion by remember { mutableStateOf(false) }
    var mostrarDialogoCerrar by remember { mutableStateOf(false) }

    // ── Cargar datos directo, sin ping previo ────────────────
    LaunchedEffect(Unit) {
        errorConexion = false
        val resumenResult = ApiService.obtenerResumen()
        val rachaResult   = ApiService.obtenerRacha()

        if (resumenResult.isSuccess) {
            resumen = resumenResult.getOrNull()
        } else {
            errorConexion = true
        }

        if (rachaResult.isSuccess) {
            racha = rachaResult.getOrNull() ?: 0
        }

        cargando = false
    }

    // ── Diálogo confirmación cerrar sesión ───────────────────
    if (mostrarDialogoCerrar) {
        AlertDialog(
            onDismissRequest = { mostrarDialogoCerrar = false },
            containerColor = SunsetCard,
            title = {
                Text("Cerrar sesión", color = SunsetWhite, fontWeight = FontWeight.Bold)
            },
            text = {
                Text("¿Confirmas que quieres cerrar sesión?", color = SunsetWhite70)
            },
            confirmButton = {
                TextButton(onClick = {
                    mostrarDialogoCerrar = false
                    onCerrarSesion()
                }) {
                    Text("Cerrar sesión", color = SunsetMagenta, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostrarDialogoCerrar = false }) {
                    Text("Cancelar", color = SunsetWhite70)
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SunsetBackground)
    ) {
        // Fondo decorativo
        Box(
            modifier = Modifier
                .size(280.dp)
                .align(Alignment.TopEnd)
                .offset(x = 80.dp, y = (-80).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(SunsetMagenta.copy(0.12f), SunsetBackground.copy(0f))
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // ── Botón cerrar sesión ──────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(onClick = { mostrarDialogoCerrar = true }) {
                    Icon(
                        imageVector = Icons.Default.Logout,
                        contentDescription = "Cerrar sesión",
                        tint = SunsetWhite70,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            // ── Alerta error de conexión ─────────────────────
            if (errorConexion && !cargando) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = SunsetMagenta.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.WifiOff,
                            contentDescription = null,
                            tint = SunsetMagenta,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "No se pudo conectar al servidor.",
                            color = SunsetMagenta,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // ── Avatar ───────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(SunsetOrange, SunsetMagenta))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = SunsetWhite,
                    modifier = Modifier.size(44.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            AppTitle(text = "Bienvenido")
            Spacer(modifier = Modifier.height(6.dp))
            AppSubtitle(text = "Autenticación exitosa · Sesión activa")

            Spacer(modifier = Modifier.height(32.dp))

            // ── Card sesión verificada ───────────────────────
            SunsetCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(SunsetOrange.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = SunsetOrange,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column {
                        Text(
                            "Sesión verificada",
                            fontWeight = FontWeight.SemiBold,
                            color = SunsetWhite,
                            fontSize = 14.sp
                        )
                        Text(
                            "Huella dactilar autenticada",
                            color = SunsetWhite70,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Cards métricas ───────────────────────────────
            if (cargando) {
                // Skeleton mientras carga
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    repeat(3) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(80.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(SunsetCard)
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.FitnessCenter,
                        label = "Entrenos",
                        value = "${resumen?.totalEntrenamientos ?: 0}"
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.DirectionsRun,
                        label = "Km totales",
                        value = "%.1f".format(resumen?.kmTotal ?: 0.0)
                    )
                    MetricCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Default.LocalFireDepartment,
                        label = "Racha",
                        value = "${racha}d"
                    )
                }

                // ── Card pace promedio (solo si hay datos) ───
                val pace = resumen?.pacePromedio ?: 0.0
                if (!errorConexion && resumen != null && pace > 0) {
                    Spacer(modifier = Modifier.height(12.dp))
                    SunsetCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(SunsetOrange.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Speed,
                                        contentDescription = null,
                                        tint = SunsetOrange,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Column {
                                    Text(
                                        "Pace promedio",
                                        color = SunsetWhite70,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        "%.1f min/km".format(pace),
                                        color = SunsetWhite,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                            Text("histórico", color = SunsetWhite70, fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            FilledTonalBoton(
                onClick = onIrAVoz,
                text = "Registrar entrenamiento",
                icon = Icons.Default.Mic,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            FilledTonalBoton(
                onClick = onIrAGrafica,
                text = "Ver mis gráficas",
                icon = Icons.Default.BarChart,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun MetricCard(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String
) {
    val infiniteTransition = rememberInfiniteTransition(label = "metric")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "metricScale"
    )

    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(SunsetCard)
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = SunsetOrange,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(value, fontWeight = FontWeight.Bold, color = SunsetWhite, fontSize = 18.sp)
            Text(label, color = SunsetWhite70, fontSize = 10.sp)
        }
    }
}