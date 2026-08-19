package com.example.compasos_tv.services

/**
 * Representa una categoría del catálogo de videos de prevención/seguridad
 * mostrado en `TvVideosScreen`.
 *
 * Cada categoría define una búsqueda curada en YouTube.
 * Ajusta [consultaBusqueda] para afinar los resultados, o agrega
 * "&channelId=UCxxxx" en `VideosSeguridadRepository` para restringir
 * a un canal oficial (Protección Civil, Cruz Roja, etc.).
 *
 * @property clave             identificador corto usado como clave en Room y en la UI.
 * @property titulo             texto mostrado en el chip de categoría.
 * @property consultaBusqueda  texto que se envía como parámetro `q` a la YouTube Data API v3.
 */
data class CategoriaVideo(
    val clave: String,
    val titulo: String,
    val consultaBusqueda: String
)

/**
 * Catálogo fijo de todas las categorías de video disponibles en la app.
 * Se usa como fuente única de verdad tanto para los chips de filtro como
 * para las búsquedas en `VideosSeguridadRepository`.
 */
object CategoriasVideoSeguridad {
    val TODAS = listOf(
        CategoriaVideo(
            clave = "prevencion",
            titulo = "Prevención de secuestro",
            consultaBusqueda = "prevención de secuestro consejos de seguridad familiar"
        ),
        CategoriaVideo(
            clave = "que_hacer",
            titulo = "Qué hacer en una emergencia",
            consultaBusqueda = "qué hacer en caso de secuestro cómo actuar seguridad"
        ),
        CategoriaVideo(
            clave = "ninos",
            titulo = "Seguridad para niños",
            consultaBusqueda = "seguridad infantil prevención secuestro niños consejos para padres"
        ),
        CategoriaVideo(
            clave = "entorno",
            titulo = "Rutas y entornos seguros",
            consultaBusqueda = "cómo identificar rutas seguras seguridad personal en la calle"
        ),
        CategoriaVideo(
            clave = "autoproteccion",
            titulo = "Señales de alerta",
            consultaBusqueda = "señales de alerta autoprotección seguridad personal consejos"
        )
    )
}
