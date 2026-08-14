package com.example.compasos_tv.config

import android.util.Log
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence

class MqttManager {

    private var client: MqttClient? = null

    val estaConectado: Boolean get() = client?.isConnected == true

    fun conectar() {
        if (estaConectado) return
        val clientId = "compasos_tv_${System.currentTimeMillis()}"
        client = MqttClient(MqttConfig.BROKER_URL, clientId, MemoryPersistence())
        client!!.connect(MqttConnectOptions().apply {
            isCleanSession       = true
            connectionTimeout    = MqttConfig.TIMEOUT_CONEXION
            keepAliveInterval    = MqttConfig.KEEP_ALIVE
            isAutomaticReconnect = true
        })
        Log.d("TvMqtt", "Conectado al broker")
    }

    fun publicar(topic: String, payload: String) {
        val c = client ?: throw IllegalStateException("MQTT no conectado")
        c.publish(topic, MqttMessage(payload.toByteArray()).apply { qos = MqttConfig.QOS })
        Log.d("TvMqtt", "► [$topic]: $payload")
    }

    fun suscribir(topic: String, onMensaje: (String, String) -> Unit) {
        val c = client ?: throw IllegalStateException("MQTT no conectado")
        c.subscribe(topic, MqttConfig.QOS) { t, msg ->
            val payload = String(msg.payload, Charsets.UTF_8)
            Log.d("TvMqtt", "◄ [$t]: $payload")
            onMensaje(t, payload)
        }
    }

    fun desconectar() {
        try { client?.takeIf { it.isConnected }?.disconnect() } catch (_: Exception) {}
        client = null
    }
}