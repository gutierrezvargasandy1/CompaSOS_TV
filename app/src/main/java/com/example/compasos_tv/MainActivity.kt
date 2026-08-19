package com.example.compasos_tv

import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.compasos_tv.Navigation.TvNavigation
import com.example.compasos_tv.Screan.ManejadorTeclasReproductor
import com.example.compasos_tv.services.TvMqttService

/**
 * Actividad única de la app (patrón single-activity + Jetpack Compose).
 *
 * Responsabilidades:
 *  1. Arrancar el servicio en primer plano [TvMqttService], que mantiene la
 *     conexión MQTT viva y escucha los datos que llegan desde el teléfono
 *     (ubicaciones, alertas, notificaciones, confirmación de vinculación).
 *  2. Montar el árbol de Compose a través de [TvNavigation], que controla
 *     la navegación entre pantallas de la TV.
 *  3. Redirigir los eventos del control remoto hacia la pantalla del
 *     reproductor de video cuando corresponde (ver [dispatchKeyEvent]).
 */
class MainActivity : ComponentActivity() {

    /**
     * Punto de entrada de la Activity. Se ejecuta una sola vez al crearse.
     *
     * @param savedInstanceState estado previo de la Activity (no se usa aquí,
     *        la app no restaura estado propio de la Activity).
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TvNavigation()
        }
    }

    /**
     * Intercepta TODOS los eventos de teclado/control remoto de la Activity
     * antes de que el sistema decida qué View tiene el foco.
     *
     * Es necesario porque, dentro de un reproductor embebido (WebView), el
     * foco nativo de Android y el foco de Compose no siempre se sincronizan.
     * Si delegáramos únicamente en Modifier.onKeyEvent + focusRequester,
     * ninguna vista interna terminaría con el foco real y el control remoto
     * dejaría de responder. Por eso [ManejadorTeclasReproductor] expone un
     * callback global que esta función invoca primero.
     *
     * @param event evento de tecla recibido del sistema/control remoto.
     * @return true si el evento fue consumido (por el reproductor o por la
     *         implementación por defecto de la Activity).
     */
    @Suppress("RestrictedApi")
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val manejado = ManejadorTeclasReproductor.actual?.invoke(event) ?: false
        return manejado || super.dispatchKeyEvent(event)
    }
}
