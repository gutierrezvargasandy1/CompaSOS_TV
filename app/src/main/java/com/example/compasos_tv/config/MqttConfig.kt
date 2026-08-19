package com.example.compasos_tv.config

/**
 * Constantes y helpers de topics MQTT usados por toda la app de TV.
 *
 * Centraliza en un solo lugar la URL del broker y la forma en que se arman
 * los nombres de los topics, para no tener Strings mágicos repetidos en
 * [MqttManager], `TvMqttService.kt` y `VinculacionTvRepository.kt`.
 */
object MqttConfig {
    const val BROKER_URL       = "tcp://192.168.1.102:1883"

    /** Tiempo máximo (segundos) para establecer la conexión con el broker. */
    const val TIMEOUT_CONEXION = 10

    /** Intervalo (segundos) de keep-alive del cliente MQTT. */
    const val KEEP_ALIVE       = 60

    /** Calidad de servicio (QoS) usada por defecto en publicaciones/suscripciones. */
    const val QOS              = 1

    /** Prefijo raíz de todos los topics propios de una TV. */
    const val TOPIC_TV          = "compasos/tv"

    /** Prefijo raíz de los topics del flujo de vinculación TV ↔ teléfono. */
    const val TOPIC_VINCULACION = "compasos/vinculacion"

    /**
     * UNA sola suscripción con wildcard en vez de tres sueltas. Menos
     * round-trips al reconectar, y si el teléfono agrega un subtopic nuevo
     * (p. ej. /notificacion) le llega sin tocar nada de este lado.
     *
     * @param tvId identificador único de esta TV (ver `TvMqttService.obtenerTvId`).
     * @return topic con wildcard, p. ej. "compasos/tv/tv_ABC123/#".
     */
    fun topicTodoDeEstaTv(tvId: String) = "$TOPIC_TV/$tvId/#"

    /** Topic donde el teléfono publica la SOLICITUD de vinculación con [codigo]. */
    fun topicSolicitud(codigo: String) = "$TOPIC_VINCULACION/tv/$codigo/solicitud"

    /** Topic donde el teléfono publica la RESPUESTA/confirmación de vinculación. */
    fun topicRespuesta(codigo: String) = "$TOPIC_VINCULACION/tv/$codigo/respuesta"

    /** Presencia de la propia TV (es su Last Will). */
    fun topicEstadoTv(tvId: String) = "$TOPIC_TV/$tvId/estado"

    /** Si el teléfono no da señales en este tiempo, la UI lo marca desconectado. */
    const val TIMEOUT_TELEFONO_MS = 90_000L
}
