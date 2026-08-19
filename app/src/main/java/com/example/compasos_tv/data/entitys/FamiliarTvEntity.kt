package com.example.compasos_tv.data.entitys

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entidad Room que representa a un integrante de la familia vinculada, junto
 * con su última ubicación conocida. Se persiste en la tabla `familiares_tv`.
 *
 * El teléfono actualiza estos datos periódicamente vía MQTT (ver
 * `TvMqttService.procesarSesion` / `procesarUbicacion`), y se guarda usando
 * `FamiliarTvDao.guardarConservandoUbicacion` para no perder la última
 * posición conocida cuando llega un snapshot sin coordenadas.
 *
 * @property usuarioId            identificador único del familiar (clave primaria).
 * @property nombre               nombre de pila del familiar.
 * @property apellido             apellido del familiar (opcional).
 * @property latitud              última latitud reportada, o null si nunca reportó.
 * @property longitud             última longitud reportada, o null si nunca reportó.
 * @property ultimaUbicacionFecha fecha/hora del último reporte de ubicación.
 * @property enLinea              true si el familiar se considera actualmente conectado.
 */
@Entity(tableName = "familiares_tv")
data class FamiliarTvEntity(
    @PrimaryKey
    val usuarioId:           String,
    val nombre:              String,
    val apellido:            String?  = null,
    val latitud:             Double?  = null,
    val longitud:            Double?  = null,
    val ultimaUbicacionFecha: String? = null,
    val enLinea:             Boolean  = false
)
