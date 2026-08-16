package com.example.compasos_tv.config

import android.util.Log
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.concurrent.ConcurrentHashMap

/**
 * EL BUG MÁS IMPORTANTE QUE ESTO ARREGLA:
 *
 * TvMqttService creaba un MqttManager y VinculacionTvRepository creaba OTRO.
 * Eran dos conexiones distintas al broker, con suscripciones separadas: la
 * respuesta de vinculación llegaba a una mientras el servicio escuchaba en la
 * otra. Ahora hay UNA sola instancia compartida: MqttManager.instancia
 *
 * Además:
 *  - Re-suscripción automática al reconectar. Con isAutomaticReconnect +
 *    cleanSession=true, al parpadear el WiFi Paho reconecta pero el broker ya
 *    olvidó tus suscripciones: la pantalla se queda congelada "conectada".
 *  - retained y Last Will.
 */
class MqttManager private constructor() {

    companion object {
        private const val TAG = "TvMqtt"

        /** Instancia única para toda la app de TV. Úsala siempre. */
        val instancia: MqttManager by lazy { MqttManager() }
    }

    private var client: MqttClient? = null
    private val suscripciones = ConcurrentHashMap<String, (String, String) -> Unit>()
    private var onConexion: ((reconectado: Boolean) -> Unit)? = null

    val estaConectado: Boolean
        get() = client?.isConnected == true

    fun alConectar(bloque: (reconectado: Boolean) -> Unit) { onConexion = bloque }

    /** Llamar siempre desde Dispatchers.IO */
    @Synchronized
    @JvmOverloads
    fun conectar(
        clientId: String = "compasos_tv_${System.currentTimeMillis()}",
        lwtTopic: String? = null,
        lwtPayload: String? = null
    ) {
        if (estaConectado) return

        val c = MqttClient(MqttConfig.BROKER_URL, clientId, MemoryPersistence())

        c.setCallback(object : MqttCallbackExtended {
            override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                Log.d(TAG, if (reconnect) "🔄 Reconectado" else "✅ Conectado a $serverURI")
                // connectComplete corre en el hilo interno de Paho y subscribe()
                // es bloqueante: si lo llamo aquí directo puedo trabar ese hilo.
                if (reconnect) Thread { reaplicarSuscripciones() }.start()
                onConexion?.invoke(reconnect)
            }
            override fun connectionLost(cause: Throwable?) {
                Log.w(TAG, "⚠️ Conexión perdida: ${cause?.message}")
            }
            override fun messageArrived(topic: String?, message: MqttMessage?) {}
            override fun deliveryComplete(token: IMqttDeliveryToken?) {}
        })

        c.connect(MqttConnectOptions().apply {
            isCleanSession       = true
            connectionTimeout    = MqttConfig.TIMEOUT_CONEXION
            keepAliveInterval    = MqttConfig.KEEP_ALIVE
            isAutomaticReconnect = true
            if (lwtTopic != null && lwtPayload != null) {
                setWill(lwtTopic, lwtPayload.toByteArray(Charsets.UTF_8), 1, true)
            }
        })

        client = c
        Log.d(TAG, "Conectado al broker como $clientId")
    }

    private fun reaplicarSuscripciones() {
        val c = client ?: return
        suscripciones.forEach { (topic, cb) ->
            try {
                c.subscribe(topic, MqttConfig.QOS) { t, msg ->
                    cb(t, String(msg.payload, Charsets.UTF_8))
                }
                Log.d(TAG, "↻ Re-suscrito a: $topic")
            } catch (e: Exception) {
                Log.e(TAG, "Error re-suscribiendo a $topic: ${e.message}")
            }
        }
    }

    @JvmOverloads
    fun publicar(
        topic: String,
        payload: String,
        qos: Int = MqttConfig.QOS,
        retained: Boolean = false
    ) {
        val c = client ?: throw IllegalStateException("MQTT no conectado")
        c.publish(topic, MqttMessage(payload.toByteArray(Charsets.UTF_8)).apply {
            this.qos = qos
            this.isRetained = retained
        })
        Log.d(TAG, "► [$topic]${if (retained) "(retained)" else ""}: $payload")
    }

    @JvmOverloads
    fun publicarSeguro(
        topic: String, payload: String,
        qos: Int = MqttConfig.QOS, retained: Boolean = false
    ): Boolean = try {
        publicar(topic, payload, qos, retained); true
    } catch (e: Exception) {
        Log.e(TAG, "No se pudo publicar en $topic: ${e.message}"); false
    }

    fun suscribir(topic: String, onMensaje: (String, String) -> Unit) {
        val c = client ?: throw IllegalStateException("MQTT no conectado")
        suscripciones[topic] = onMensaje
        c.subscribe(topic, MqttConfig.QOS) { t, msg ->
            val payload = String(msg.payload, Charsets.UTF_8)
            Log.d(TAG, "◄ [$t]: $payload")
            onMensaje(t, payload)
        }
        Log.d(TAG, "Suscrito a: $topic")
    }

    fun desuscribir(topic: String) {
        try {
            suscripciones.remove(topic)
            client?.unsubscribe(topic)
        } catch (e: Exception) {
            Log.e(TAG, "Error al desuscribir de $topic: ${e.message}")
        }
    }

    fun desconectar() {
        try { client?.takeIf { it.isConnected }?.disconnect() } catch (_: Exception) {}
        suscripciones.clear()
        client = null
    }
}