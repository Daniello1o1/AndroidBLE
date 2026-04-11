package com.example.sensoresv2.presentation

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class SensorMessageSender(
    private val context: Context
) {
    companion object {
        const val SENSOR_PATH = "/sensor_data"
    }

    suspend fun sendSensorData(
        accX: Float,
        accY: Float,
        accZ: Float,
        gyroX: Float,
        gyroY: Float,
        gyroZ: Float,
        pitch: Float,
        roll: Float,
        yaw: Float
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val nodeClient = Wearable.getNodeClient(context)
            val messageClient = Wearable.getMessageClient(context)

            val connectedNodes = nodeClient.connectedNodes.await()
            Log.d("WEAR_SEND", "Nodos conectados: " + connectedNodes.size)

            if (connectedNodes.isEmpty()) {
                Log.d("WEAR_SEND", "No hay nodos conectados")
                return@withContext false
            }

            val payload = buildString {
                append("{")
                append("\"accX\":$accX,")
                append("\"accY\":$accY,")
                append("\"accZ\":$accZ,")
                append("\"gyroX\":$gyroX,")
                append("\"gyroY\":$gyroY,")
                append("\"gyroZ\":$gyroZ,")
                append("\"pitch\":$pitch,")
                append("\"roll\":$roll,")
                append("\"yaw\":$yaw")
                append("}")
            }.toByteArray(Charsets.UTF_8)

            connectedNodes.forEach { node ->
                Log.d("WEAR_SEND", "Enviando a nodo: " + node.id)
                messageClient.sendMessage(
                    node.id,
                    SENSOR_PATH,
                    payload
                ).await()
                Log.d("WEAR_SEND", "Mensaje enviado")
            }

            true
        } catch (e: Exception) {
            Log.e("WEAR_SEND", "Error al enviar mensaje", e)
            false
        }
    }
}