/* While this template provides a good starting point for using Wear Compose, you can always
 * take a look at https://github.com/android/wear-os-samples/tree/main/ComposeStarter to find the
 * most up to date changes to the libraries and their usages.
 */

package com.example.sensoresv2.presentation

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import kotlin.math.PI
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import android.view.WindowManager
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() { //Pantalla Principal

    private lateinit var sensorManager: SensorManager //Late init = inicializa posteriormente en el onCreate. Variable encargada de administrar los sensores
    private var accelerometer: Sensor? = null //Variable encargada del acelerómetro, puede ser null si el reloj no cuenta con este sensor
    private var gyroscope: Sensor? = null //Lo mismo de arriba, pero en el giroscopio
    private var rotationVector: Sensor? = null //Igual, pero para el sensor de rotación

    override fun onCreate(savedInstanceState: Bundle?) { //Punto de inicio de la actividad (lo que se muestra en el reloj)
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager //Obtiene acceso a todos los senores y los adapta para el formato correcto
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) //Pide acceso al acelerómetro si existe
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE) //Igual xd
        rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR) //Igual xddd

        setContent { //La UI de la app, equivalente a setContentView()
            MaterialTheme { //Se aplica el estilo visual, colores, tipografía y eso
                SensorScreen( //Pantalla personalizada
                    sensorManager = sensorManager,
                    accelerometer = accelerometer,
                    gyroscope = gyroscope,
                    rotationVector = rotationVector
                )
            }
        }
    }
}

@Composable //Cada vez que los datos cambien, se actualizan en la pantalla
fun SensorScreen(
    sensorManager: SensorManager,
    accelerometer: Sensor?,
    gyroscope: Sensor?,
    rotationVector: Sensor?
) {
    var accX by remember { mutableStateOf(0f) } //Datos x, y, z, del acelerómetro
    var accY by remember { mutableStateOf(0f) }
    var accZ by remember { mutableStateOf(0f) }

    var gyroX by remember { mutableStateOf(0f) } //Datos x, y, z, del giroscopio
    var gyroY by remember { mutableStateOf(0f) }
    var gyroZ by remember { mutableStateOf(0f) }

    var pitch by remember { mutableStateOf(0f) } //Datos x, y, z, de la posición
    var roll by remember { mutableStateOf(0f) }
    var yaw by remember { mutableStateOf(0f) }

    var hasRotationVector by remember { mutableStateOf(rotationVector != null) } //Si el reloj cuenta con orientación, se muestra
    val context = LocalContext.current
    val sender = remember { SensorMessageSender(context) }
    val scope = rememberCoroutineScope()


    DisposableEffect(sensorManager, accelerometer, gyroscope, rotationVector) { //Esta parte se encarga de que los sensores solo estén muestreando cuando el programa está en pantalla
        val rotationMatrix = FloatArray(9)
        val orientationAngles = FloatArray(3)

        val listener = object : SensorEventListener {

            override fun onSensorChanged(event: SensorEvent) {
                val timestampMs = event.timestamp / 1_000_000L

                when (event.sensor.type) {
                    Sensor.TYPE_ACCELEROMETER -> {
                        accX = event.values[0]
                        accY = event.values[1]
                        accZ = event.values[2]

                        scope.launch {
                            sender.sendSensorData(
                                timestampMs = timestampMs,
                                accX = accX, accY = accY, accZ = accZ,
                                gyroX = gyroX, gyroY = gyroY, gyroZ = gyroZ,
                                pitch = pitch, roll = roll, yaw = yaw
                            )
                        }
                    }

                    Sensor.TYPE_GYROSCOPE -> {
                        gyroX = event.values[0]
                        gyroY = event.values[1]
                        gyroZ = event.values[2]
                    }

                    Sensor.TYPE_ROTATION_VECTOR -> {
                        SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                        SensorManager.getOrientation(rotationMatrix, orientationAngles)
                        yaw   = radiansToDegrees(orientationAngles[0])
                        pitch = radiansToDegrees(orientationAngles[1])
                        roll  = radiansToDegrees(orientationAngles[2])
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        accelerometer?.let { //Solo lo registra si el sensor existe
            sensorManager.registerListener( //Empieza a escuchar este sensor, y se le da un delay para tener una frecuencia de datos moderada
                listener,
                it,
                SensorManager.SENSOR_DELAY_FASTEST
            )
        }

        gyroscope?.let { //Solo lo registra si el sensor existe
            sensorManager.registerListener( //Empieza a escuchar este sensor, y se le da un delay para tener una frecuencia de datos moderada
                listener,
                it,
                SensorManager.SENSOR_DELAY_FASTEST
            )
        }

        rotationVector?.let { //Solo lo registra si el sensor existe
            sensorManager.registerListener( //Empieza a escuchar este sensor, y se le da un delay para tener una frecuencia de datos moderada
                listener,
                it,
                SensorManager.SENSOR_DELAY_FASTEST
            )
        }

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    SensorScreenContent(
        accX = accX,
        accY = accY,
        accZ = accZ,
        gyroX = gyroX,
        gyroY = gyroY,
        gyroZ = gyroZ,
        pitch = pitch,
        roll = roll,
        yaw = yaw,
        hasRotationVector = hasRotationVector
    )
}

@Composable
fun SensorScreenContent(
    accX: Float,
    accY: Float,
    accZ: Float,
    gyroX: Float,
    gyroY: Float,
    gyroZ: Float,
    pitch: Float,
    roll: Float,
    yaw: Float,
    hasRotationVector: Boolean
) {
    val scrollState = rememberScrollState()

    Column( //UI
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(8.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("ACC") //Aquí llegan los datos del acelerómetro
        Text("X ${"%.1f".format(accX)}")
        Text("Y ${"%.1f".format(accY)}")
        Text("Z ${"%.1f".format(accZ)}")

        Text("")

        Text("GYR") //Aquí llegan los datos del giroscopio
        Text("X ${"%.1f".format(gyroX)}")
        Text("Y ${"%.1f".format(gyroY)}")
        Text("Z ${"%.1f".format(gyroZ)}")

        Text("")

        if (hasRotationVector) {
            Text("ORI") //Aquí llegan los datos del sensor de rotación
            Text("Pitch ${"%.1f".format(pitch)}°")
            Text("Roll ${"%.1f".format(roll)}°")
            Text("Yaw ${"%.1f".format(yaw)}°")
        } else {
            Text("Sin rotation vector")
        }
    }
}

@Preview(showBackground = true) //Preview de los datos
@Composable
fun SensorScreenPreview() {
    MaterialTheme {
        SensorScreenContent(
            accX = 1.2f,
            accY = -0.8f,
            accZ = 9.7f,
            gyroX = 0.4f,
            gyroY = -0.2f,
            gyroZ = 1.1f,
            pitch = 15.0f,
            roll = -8.0f,
            yaw = 42.0f,
            hasRotationVector = true
        )
    }
}

fun radiansToDegrees(radians: Float): Float { //Convierte los radianes a grados
    return (radians * 180f / PI.toFloat())
}