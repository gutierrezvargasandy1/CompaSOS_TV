package com.example.compasos_tv.Screan


import android.view.KeyEvent

/**
 * Puente entre MainActivity.dispatchKeyEvent (nivel Android View, se
 * ejecuta ANTES de que el sistema decida qué View tiene el foco) y
 * TvReproductorScreen (nivel Compose).
 *
 * Necesario porque dentro de un WebView el foco nativo de Android y el
 * foco de Compose no siempre se sincronizan: si depend�amos de
 * Modifier.onKeyEvent + focusRequester, en algunos casos ninguna vista
 * termina con el foco real y el control remoto deja de responder por
 * completo. Interceptando aquí nos aseguramos de recibir la tecla sin
 * importar qué vista interna del árbol tenga o no el foco.
 */
object ManejadorTeclasReproductor {
    var actual: ((KeyEvent) -> Boolean)? = null
}