package com.example.compasos_tv.data.entitys

import androidx.room.Entity

/**
 * Video de seguridad/prevención cacheado desde YouTube Data API v3.
 * Clave compuesta (id + categoria): el mismo video puede aparecer
 * en más de una categoría sin pisarse en el REPLACE.
 */
@Entity(tableName = "videos_tv", primaryKeys = ["id", "categoria"])
data class VideoTvEntity(
    val id: String,                 // ID del video de YouTube (11 caracteres)
    val categoria: String,          // clave de la categoría (ver CategoriaVideo)
    val titulo: String,
    val canal: String,
    val thumbnailUrl: String,
    val fechaPublicacion: String? = null,
    val fechaCache: Long = System.currentTimeMillis()
)