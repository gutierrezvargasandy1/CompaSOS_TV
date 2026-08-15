package com.example.compasos_tv.config


object MqttConfig {
    const val BROKER_URL       = "tcp://192.168.1.5:1883"
    const val TIMEOUT_CONEXION = 10
    const val KEEP_ALIVE       = 60
    const val QOS              = 1

    // Topics TV ← teléfono
    const val TOPIC_TV          = "compasos/tv"          // compasos/tv/{tvId}/sesion|alerta|ubicacion
    const val TOPIC_VINCULACION = "compasos/vinculacion"  // compasos/vinculacion/tv/{tvId}/...
}