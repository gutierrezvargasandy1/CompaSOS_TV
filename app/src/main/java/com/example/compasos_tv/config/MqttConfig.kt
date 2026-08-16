package com.example.compasos_tv.config

object MqttConfig {

    /**
     * ⚠️ TIENE QUE SER LA MISMA IP QUE EN EL TELÉFONO.
     * Tenías 192.168.1.5 aquí y 192.168.1.102 en el móvil: los dos logean
     * "✅ Conectado" y nunca se ven porque son brokers distintos.
     */
    const val BROKER_URL       = "tcp://192.168.1.102:1883"

    const val TIMEOUT_CONEXION = 10
    const val KEEP_ALIVE       = 60
    const val QOS              = 1

    const val TOPIC_TV          = "compasos/tv"
    const val TOPIC_VINCULACION = "compasos/vinculacion"

    /**
     * UNA sola suscripción con wildcard en vez de tres sueltas. Menos
     * round-trips al reconectar, y si el teléfono agrega un subtopic nuevo
     * (p. ej. /notificacion) le llega sin tocar nada de este lado.
     */
    fun topicTodoDeEstaTv(tvId: String) = "$TOPIC_TV/$tvId/#"

    fun topicSolicitud(codigo: String) = "$TOPIC_VINCULACION/tv/$codigo/solicitud"
    fun topicRespuesta(codigo: String) = "$TOPIC_VINCULACION/tv/$codigo/respuesta"

    /** Presencia de la propia TV (es su Last Will). */
    fun topicEstadoTv(tvId: String) = "$TOPIC_TV/$tvId/estado"

    /** Si el teléfono no da señales en este tiempo, la UI lo marca desconectado. */
    const val TIMEOUT_TELEFONO_MS = 90_000L
}