package com.example.compasos_tv.Navigation

/**
 * Define cada pantalla navegable de la app de TV como una constante tipada,
 * en vez de usar Strings sueltos repartidos por todo el código.
 *
 * Cada objeto representa una ruta del [androidx.navigation.NavHost] (ver
 * [TvNavigation]) junto con el título que se muestra en el menú lateral
 * (ver `TvMainScreen.kt`).
 *
 * @property route  identificador único de la ruta usado por Navigation Compose.
 * @property titulo texto visible en la barra de navegación de la TV.
 */
sealed class TvScreen(val route: String, val titulo: String) {

    /** Pantalla inicial: captura del código para vincular la TV con el teléfono. */
    object Vinculacion      : TvScreen("tv_vinculacion",        "Vincular")

    /** Pantalla principal con el mapa y la lista de familiares. */
    object Dashboard        : TvScreen("tv_dashboard",          "Inicio")

    /** Bandeja de alertas de emergencia y notificaciones recibidas. */
    object AlertasRecibidas : TvScreen("tv_alertas_recibidas",  "Alertas")

    /** Listado de familiares vinculados con su estado de conexión. */
    object FamiliaEnLinea   : TvScreen("tv_familia",            "Familia")

    /** Configuración del dispositivo y opción de desvincular la TV. */
    object Configuracion    : TvScreen("tv_configuracion",      "Configuración")

    /** Catálogo de videos de prevención/seguridad. */
    object Videos : TvScreen("tv_videos", "videos")

}
