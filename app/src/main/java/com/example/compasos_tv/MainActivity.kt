package com.example.compasos_tv

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.compasos_tv.Navigation.TvNavigation
import com.example.compasos_tv.Screan.ManejadorTeclasReproductor
import com.example.compasos_tv.services.TvMqttService

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ⚠️ ESTA LÍNEA ERA LA QUE FALTABA.
        // TvMqttService estaba declarado en el AndroidManifest pero nadie lo
        // arrancaba nunca: por eso la TV no se suscribía a ningún topic y no
        // llegaba absolutamente nada del teléfono, aunque el broker estuviera
        // bien y la vinculación se hubiera hecho.
        TvMqttService.iniciar(applicationContext)

        setContent {
            TvNavigation()
        }
    }

    @Suppress("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val manejado = ManejadorTeclasReproductor.actual?.invoke(event) ?: false
        return manejado || super.dispatchKeyEvent(event)
    }
}