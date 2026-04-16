package com.example.biometrico

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.example.biometrico.activitys.GraphScreen
import com.example.biometrico.activitys.Home
import com.example.biometrico.activitys.LoginActivity
import com.example.biometrico.activitys.VoiceScreen
import com.example.biometrico.ui.theme.BiometricoTheme

// ── Pantallas disponibles ────────────────────────────────────
enum class Pantalla { LOGIN, HOME, VOZ, GRAFICA }

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BiometricoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var pantallaActual by remember { mutableStateOf(Pantalla.LOGIN) }

                    when (pantallaActual) {
                        Pantalla.LOGIN -> LoginActivity(
                            onAutenticacionExitosa = {
                                pantallaActual = Pantalla.HOME
                            }
                        )
                        Pantalla.HOME -> Home(
                            onIrAVoz = { pantallaActual = Pantalla.VOZ },
                            onIrAGrafica = { pantallaActual = Pantalla.GRAFICA },
                            onCerrarSesion = { pantallaActual = Pantalla.LOGIN }
                        )
                        Pantalla.VOZ -> VoiceScreen(
                            onVolver = { pantallaActual = Pantalla.HOME }
                        )
                        Pantalla.GRAFICA -> GraphScreen(
                            onVolver = { pantallaActual = Pantalla.HOME }
                        )
                    }
                }
            }
        }
    }
}