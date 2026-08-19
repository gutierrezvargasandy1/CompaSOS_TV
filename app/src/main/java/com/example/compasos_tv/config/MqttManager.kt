package com.example.compasos_tv.config

import android.util.Log
import org.eclipse.paho.client.mqttv3.*
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.util.concurrent.ConcurrentHashMap

/**
 * Envoltorio (wrapper) sobre el cliente MQTT de Paho que administra UNA sola
 * conexión al broker para toda la app de TV, con reconexión automática y
 * re-suscripción a los topics ya registrados.
 *
 * EL BUG MÁS IMPORTANTE QUE ESTO ARREGLA:
 *
 * `TvMqttService` creaba un `MqttManager` y `VinculacionTvRepository` creaba
 * OTRO. Eran dos conexiones distintas al broker, con suscripciones
 * separadas: la respuesta de vinculación llegaba a una mientras el servicio
 * escuchaba en la otra. Ahora hay UNA sola instancia compartida, accesible
 * mediante [instancia] (patrón singleton).
 *
 * Además:
 *  - Re-suscripción automática al reconectar. Con `isAutomaticReconnect` +
 *    `cleanSession = true`, al parpadear el WiFi Paho reconecta pero el
 *    broker ya olvidó tus suscripciones: la pantalla se queda congelada
 *    "conectada" sin recibir nada. [reaplicarSuscripciones] soluciona esto.
 *  - Soporte de mensajes retained y de Last Will and Testament (LWT).
 *
 * El constructor es privado: la única forma de obtener una instancia es a
 * través de [instancia].
 */
class MqttManager private constructor() {

    companion object {
        private const val TAG = "TvMqtt"

        /** Instancia única para toda la app de TV. Úsala siempre. */
        val instancia: MqttManager by lazy { MqttManager() }
    }

    /** Cliente Paho actualmente conectado, o null si no hay conexión activa. */
    private var client: MqttClient? = null

    /** Registro de topic → callback, usado para re-suscribir tras reconectar. */
    private val suscripciones = ConcurrentHashMap<String, (String, String) -> Unit>()

    /** Callback opcional invocado cada vez que se completa una conexión. */
    private var onConexion: ((reconectado: Boolean) -> Unit)? = null

    /** true si el cliente MQTT está actualmente conectado al broker. */
    val estaConectado: Boolean
        get() = client?.isConnected == true

    /**
     * Registra un callback que se dispara cada vez que se conecta (o
     * reconecta) con el broker.
     *
     * @param bloque recibe `true` si fue una reconexión, `false` si fue la
     *        primera conexión.
     */
    fun alConectar(bloque: (reconectado: Boolean) -> Unit) { onConexion = bloque }

    /**
     * Abre la conexión con el broker MQTT configurado en [MqttConfig].
     * No hace nada si ya había una conexión activa.
     *
     * Llamar siempre desde Dispatchers.IO (es una operación bloqueante).
     *
     * @param clientId   identificador único del cliente ante el broker.
     * @param lwtTopic   topic del Last Will (mensaje que el broker publica
     *                   automáticamente si esta TV se desconecta abruptamente).
     * @param lwtPayload contenido del mensaje de Last Will.
     */
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

    /**
     * Vuelve a suscribirse a todos los topics registrados en [suscripciones].
     * Se invoca automáticamente tras una reconexión, porque `cleanSession =
     * true` hace que el broker olvide las suscripciones anteriores.
     */
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

    /**
     * Publica un mensaje en [topic]. Lanza excepción si no hay conexión
     * activa (usar [publicarSeguro] si no se quiere manejar la excepción).
     *
     * @param topic    topic destino.
     * @param payload  contenido del mensaje (texto plano, normalmente JSON).
     * @param qos      calidad de servicio (por defecto [MqttConfig.QOS]).
     * @param retained si true, el broker conserva este mensaje como el
     *                 "último valor conocido" del topic para nuevos suscriptores.
     */
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

    /**
     * Igual que [publicar], pero atrapa cualquier excepción y la reporta
     * como resultado booleano en vez de propagarla. Útil quando el caller
     * no puede permitirse un crash si el broker no está disponible.
     *
     * @return true si el mensaje se publicó correctamente, false si falló.
     */
    @JvmOverloads
    fun publicarSeguro(
        topic: String, payload: String,
        qos: Int = MqttConfig.QOS, retained: Boolean = false
    ): Boolean = try {
        publicar(topic, payload, qos, retained); true
    } catch (e: Exception) {
        Log.e(TAG, "No se pudo publicar en $topic: ${e.message}"); false
    }

    /**
     * Se suscribe a [topic] y registra [onMensaje] para que se re-aplique
     * automáticamente si la conexión se cae y se restablece.
     *
     * @param onMensaje callback invocado con (topic, payload) por cada
     *        mensaje recibido en ese topic.
     */
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

    /** Cancela la suscripción a [topic] y lo quita del registro de re-suscripción. */
    fun desuscribir(topic: String) {
        try {
            suscripciones.remove(topic)
            client?.unsubscribe(topic)
        } catch (e: Exception) {
            Log.e(TAG, "Error al desuscribir de $topic: ${e.message}")
        }
    }

    /** Cierra la conexión con el broker y limpia todas las suscripciones registradas. */
    fun desconectar() {
        try { client?.takeIf { it.isConnected }?.disconnect() } catch (_: Exception) {}
        suscripciones.clear()
        client = null
    }
}
