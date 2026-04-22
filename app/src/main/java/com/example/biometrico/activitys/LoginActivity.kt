package com.example.biometrico.activitys

import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.biometrico.components.ActionsButton
import com.example.biometrico.components.AppSubtitle
import com.example.biometrico.components.AppTitle
import com.example.biometrico.components.ImprimirIcono
import com.example.biometrico.ui.theme.*


@Composable
fun LoginActivity(onAutenticacionExitosa: () -> Unit = {}) {

    val context = LocalContext.current
    val activity = context as? FragmentActivity

    // cuento los intentos fallidos para bloquear despues de 3
    var intentosFallidos by remember { mutableStateOf(0) }
    var bloqueado by remember { mutableStateOf(false) }

    // reviso si el telefono tiene sensor biometrico
    val tieneBiometrico = remember {
        val bm = BiometricManager.from(context)
        bm.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK) ==
                BiometricManager.BIOMETRIC_SUCCESS
    }

    // animacion de fondo que respira
    val infiniteTransition = rememberInfiniteTransition(label = "bg")
    val bgScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = EaseInOut),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bgScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SunsetBackground)
    ) {

        // circulo decorativo arriba a la izquierda
        Box(
            modifier = Modifier
                .size(300.dp)
                .scale(bgScale)
                .offset(x = (-60).dp, y = (-60).dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            SunsetOrange.copy(alpha = 0.15f),
                            SunsetBackground.copy(alpha = 0f)
                        )
                    )
                )
        )

        // circulo decorativo abajo a la derecha
        Box(
            modifier = Modifier
                .size(250.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 60.dp, y = 60.dp)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            SunsetMagenta.copy(alpha = 0.15f),
                            SunsetBackground.copy(alpha = 0f)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // etiqueta pequeña arriba
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(SunsetOrange.copy(0.2f), SunsetMagenta.copy(0.2f))
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "BITÁCORA DEPORTIVA",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SunsetOrange,
                    letterSpacing = 2.sp
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // si no hay sensor muestro icono de advertencia, si hay muestro la huella
            if (!tieneBiometrico) {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(SunsetMagenta.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Warning,
                        null,
                        tint = SunsetMagenta,
                        modifier = Modifier.size(56.dp)
                    )
                }
            } else {
                // el icono se pone rosa si ya fallo 3 veces
                ImprimirIcono(
                    icono = Icons.Default.Fingerprint,
                    size = 96.dp,
                    tint = if (intentosFallidos >= 3) SunsetMagenta else SunsetOrange
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            AppTitle(text = "Bienvenido de vuelta")

            Spacer(modifier = Modifier.height(12.dp))

            // si no hay sensor aviso al usuario, si hay le digo que use su huella
            if (!tieneBiometrico) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = SunsetMagenta.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning, null,
                            tint = SunsetMagenta,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Este dispositivo no tiene sensor biométrico disponible.",
                            color = SunsetMagenta,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                AppSubtitle(
                    text = "Verifica tu identidad con\ntu huella dactilar para continuar"
                )
            }

            // aviso de intentos fallidos, solo aparece entre el 1 y el 2
            if (intentosFallidos in 1..2) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = SunsetOrange.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text(
                        "⚠️ Intento fallido ${intentosFallidos}/3. Asegúrate de usar el dedo registrado.",
                        modifier = Modifier.padding(10.dp),
                        color = SunsetOrange,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // mensaje de bloqueo cuando ya llego a 3 intentos
            if (bloqueado) {
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = SunsetMagenta.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Lock, null,
                            tint = SunsetMagenta,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "3 intentos fallidos. Presiona el botón para intentar de nuevo.",
                            color = SunsetMagenta,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))

            ActionsButton(
                onClick = {
                    if (!tieneBiometrico) {
                        Toast.makeText(
                            context,
                            "Este dispositivo no tiene sensor biométrico",
                            Toast.LENGTH_LONG
                        ).show()
                        return@ActionsButton
                    }

                    if (activity != null) {
                        bloqueado = false // reseteo el bloqueo cada vez que intenta
                        lanzarBiometricoConContador(
                            activity = activity,
                            onExito = { onAutenticacionExitosa() },
                            onFallo = {
                                intentosFallidos++
                                if (intentosFallidos >= 3) {
                                    bloqueado = true
                                    Toast.makeText(
                                        context,
                                        "3 intentos fallidos. La app sigue activa.",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }
                            },
                            onError = { msg ->
                                if (msg.isNotEmpty()) {
                                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    } else {
                        Toast.makeText(context, "No se pudo obtener la actividad", Toast.LENGTH_SHORT).show()
                    }
                },
                text = if (bloqueado) "Reintentar biométrico" else "Iniciar con Biométrico",
                icon = if (bloqueado) Icons.Default.Lock else Icons.Default.Fingerprint,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Tus datos están protegidos y cifrados",
                fontSize = 12.sp,
                color = SunsetWhite40,
                textAlign = TextAlign.Center
            )
        }
    }
}


// lanza el prompt biometrico y maneja los 3 casos: exito, fallo y error del sistema
fun lanzarBiometricoConContador(
    activity: FragmentActivity,
    onExito: () -> Unit,
    onFallo: () -> Unit,
    onError: (String) -> Unit
) {
    val executor = ContextCompat.getMainExecutor(activity)

    val biometricPrompt = BiometricPrompt(
        activity, executor,
        object : BiometricPrompt.AuthenticationCallback() {

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onExito()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                // huella leida pero no coincide con ninguna registrada
                onFallo()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)

                // si el usuario cancela no muestro nada, solo para errores reales
                val msg = when (errorCode) {
                    BiometricPrompt.ERROR_CANCELED,
                    BiometricPrompt.ERROR_USER_CANCELED,
                    BiometricPrompt.ERROR_NEGATIVE_BUTTON -> ""
                    BiometricPrompt.ERROR_HW_NOT_PRESENT,
                    BiometricPrompt.ERROR_HW_UNAVAILABLE -> "Sensor biométrico no disponible"
                    BiometricPrompt.ERROR_NO_BIOMETRICS -> "No hay huellas registradas en este dispositivo"
                    BiometricPrompt.ERROR_LOCKOUT -> "Demasiados intentos. Espera un momento."
                    BiometricPrompt.ERROR_LOCKOUT_PERMANENT -> "Sensor bloqueado. Desbloquea con PIN."
                    else -> "Error biométrico: $errString"
                }
                onError(msg)
            }
        }
    )

    val promptInfo = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Autenticación Biométrica")
        .setSubtitle("Usa tu huella para iniciar sesión")
        .setNegativeButtonText("Cancelar")
        .build()

    biometricPrompt.authenticate(promptInfo)
}


// esta funcion queda para que no truene nada que ya la usaba antes
fun lanzarBiometrico(
    activity: FragmentActivity,
    autenticacionExitosa: () -> Unit
) {
    lanzarBiometricoConContador(
        activity = activity,
        onExito = autenticacionExitosa,
        onFallo = {},
        onError = {}
    )
}