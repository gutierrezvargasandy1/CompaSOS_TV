package com.example.compasos_tv.data.entitys

import androidx.room.Entity

/**
 * Entidad Room que representa un video de seguridad/prevención cacheado
 * localmente a partir de resultados de YouTube Data API v3 (ver
 * `VideosSeguridadRepository`). Se persiste en la tabla `videos_tv`.
 *
 * Usa clave compuesta (id + categoria): el mismo video de YouTube puede
 * aparecer en más de una categoría de `CategoriaVideo` sin pisarse entre sí
 * al hacer un `REPLACE`.
 *
 * @property id                identificador del video de YouTube (11 caracteres).
 * @property categoria         clave de la categoría a la que pertenece (ver `CategoriaVideo`).
 * @property titulo            título del video.
 * @property canal              nombre del canal de YouTube que lo publicó.
 * @property thumbnailUrl      URL de la miniatura a mostrar en la grilla.
 * @property fechaPublicacion  fecha de publicación original en YouTube (puede ser null).
 * @property fechaCache        timestamp (epoch ms) de cuándo se guardó este registro,
 *                             usado para saber si el caché ya expiró.
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
