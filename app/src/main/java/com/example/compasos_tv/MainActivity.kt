package com.example.compasos_tv

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.compasos_tv.Navigation.TvNavigation
import com.example.compasos_tv.Screan.ManejadorTeclasReproductor

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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