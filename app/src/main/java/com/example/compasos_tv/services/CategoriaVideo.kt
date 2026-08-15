package com.example.compasos_tv.services

/**
 * Cada categoría define una búsqueda curada en YouTube.
 * Ajusta consultaBusqueda para afinar los resultados, o agrega
 * "&channelId=UCxxxx" en VideosSeguridadRepository para restringir
 * a un canal oficial (Protección Civil, Cruz Roja, etc.).
 */
data class CategoriaVideo(
    val clave: String,
    val titulo: String,
    val consultaBusqueda: String
)

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