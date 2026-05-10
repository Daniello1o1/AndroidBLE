package com.upiiz.ble_sipi.Views;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.upiiz.ble_sipi.BLE.BLEService;
import com.upiiz.ble_sipi.BLE.DataLayerListenerService;
import com.upiiz.ble_sipi.Models.Prueba;
import com.upiiz.ble_sipi.R;

public class ConectarDispositivosActivity extends AppCompatActivity {

    // Config de la prueba
    private Prueba config;

    // BLE — ESP32
    private BLEService bleService;
    private boolean isBound = false;
    private boolean esp32Conectado = false;

    // Watch
    private boolean watchConectado = false;
    private BroadcastReceiver watchReceiver;

    // Timeout — si en 15s no conecta, muestra error
    private final Handler timeoutHandler = new Handler();
    private static final int TIMEOUT_MS = 15000;

    // UI
    private View cardESP32, cardWatch;
    private View indicadorESP32, indicadorWatch;
    private TextView tvEstadoESP32, tvEstadoWatch;
    private MaterialButton btnReintentar, btnIniciarPrueba;
    private static final int REQUEST_ENABLE_BT = 1;

    private void verificarBluetooth() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        if (bluetoothAdapter == null) {
            new AlertDialog.Builder(this)
                    .setTitle("Bluetooth no disponible")
                    .setMessage("Este dispositivo no soporta Bluetooth.")
                    .setPositiveButton("Aceptar", (d, w) -> finish())
                    .setCancelable(false)
                    .show();
            return;
        }

        if (!bluetoothAdapter.isEnabled()) {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) return;

            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            iniciarConexiones();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == RESULT_OK) {
                iniciarConexiones(); // Bluetooth activado, continuar
            } else {
                Toast.makeText(this,
                        "Se necesita Bluetooth para conectar los sensores",
                        Toast.LENGTH_LONG).show();
                finish(); // Regresar si rechaza
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_conectar_dispositivos);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        config = (Prueba) getIntent().getSerializableExtra("config");



        bindViews();
        mostrarTarjetasSegunConfig();
        verificarBluetooth();

        btnReintentar.setOnClickListener(v -> reintentar());
        btnIniciarPrueba.setOnClickListener(v -> irAPrueba());
    }
    private void bindViews() {
        cardESP32       = findViewById(R.id.cardESP32);
        cardWatch       = findViewById(R.id.cardWatch);
        indicadorESP32  = findViewById(R.id.indicadorESP32);
        indicadorWatch  = findViewById(R.id.indicadorWatch);
        tvEstadoESP32   = findViewById(R.id.tvEstadoESP32);
        tvEstadoWatch   = findViewById(R.id.tvEstadoWatch);
        btnReintentar   = findViewById(R.id.btnReintentar);
        btnIniciarPrueba = findViewById(R.id.btnIniciarPrueba);
    }

    // Muestra solo las tarjetas que necesita la prueba
    private void mostrarTarjetasSegunConfig() {
        if (config.necesitaESP32()) cardESP32.setVisibility(View.VISIBLE);
        if (config.necesitaWatch())  cardWatch.setVisibility(View.VISIBLE);
    }

    private void iniciarConexiones() {
        btnReintentar.setVisibility(View.GONE);

        if (config.necesitaESP32()) {
            setEstadoESP32("Buscando...", false, false);
            iniciarBLEService();
            // Timeout para ESP32
            timeoutHandler.postDelayed(() -> {
                if (!esp32Conectado) {
                    setEstadoESP32("No encontrado", false, true);
                    btnReintentar.setVisibility(View.VISIBLE);
                }
            }, TIMEOUT_MS);
        }

        if (config.necesitaWatch()) {
            setEstadoWatch("Esperando datos...", false, false);
            registrarWatchReceiver();
            // Timeout para Watch
            timeoutHandler.postDelayed(() -> {
                if (!watchConectado) {
                    setEstadoWatch("Sin respuesta del reloj", false, true);
                    btnReintentar.setVisibility(View.VISIBLE);
                }
            }, TIMEOUT_MS);
        }
    }

    // ================= ESP32 =================

    private void iniciarBLEService() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) return;

        Intent intent = new Intent(this, BLEService.class);
        bindService(intent, bleConnection, BIND_AUTO_CREATE);
    }

    private final ServiceConnection bleConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BLEService.LocalBinder binder = (BLEService.LocalBinder) service;
            bleService = binder.getService();
            isBound = true;

            bleService.setOnConnectedListener(() -> {
                esp32Conectado = true;
                timeoutHandler.removeCallbacksAndMessages(null);
                setEstadoESP32("Conectado", true, false);
                verificarTodoConectado();
            });

            bleService.startScan();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            esp32Conectado = false;
            setEstadoESP32("Desconectado", false, true);
        }
    };

    // ================= WATCH =================

    private void registrarWatchReceiver() {
        watchReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                // Llegó el primer mensaje del Watch — ya está comunicado
                if (!watchConectado) {
                    watchConectado = true;
                    timeoutHandler.removeCallbacksAndMessages(null);
                    setEstadoWatch("Conectado", true, false);
                    verificarTodoConectado();
                }
            }
        };
        IntentFilter filter = new IntentFilter(DataLayerListenerService.ACTION_SENSOR_DATA);
        ContextCompat.registerReceiver(
                this,
                watchReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
        );
    }

    // ================= ESTADO UI =================

    private void setEstadoESP32(String texto, boolean ok, boolean error) {
        runOnUiThread(() -> {
            tvEstadoESP32.setText(texto);
            if (ok)    indicadorESP32.setBackgroundResource(R.drawable.circle_green);
            else if (error) indicadorESP32.setBackgroundResource(R.drawable.circle_red);
            else       indicadorESP32.setBackgroundResource(R.drawable.circle_gray);
        });
    }

    private void setEstadoWatch(String texto, boolean ok, boolean error) {
        runOnUiThread(() -> {
            tvEstadoWatch.setText(texto);
            if (ok)    indicadorWatch.setBackgroundResource(R.drawable.circle_green);
            else if (error) indicadorWatch.setBackgroundResource(R.drawable.circle_red);
            else       indicadorWatch.setBackgroundResource(R.drawable.circle_gray);
        });
    }

    // Habilita el botón solo cuando todo lo requerido está conectado
    private void verificarTodoConectado() {
        boolean esp32OK = !config.necesitaESP32() || esp32Conectado;
        boolean watchOK = !config.necesitaWatch()  || watchConectado;

        runOnUiThread(() -> {
            btnIniciarPrueba.setEnabled(esp32OK && watchOK);
        });
    }

    // ================= REINTENTAR =================

    private void reintentar() {
        esp32Conectado  = false;
        watchConectado  = false;
        btnIniciarPrueba.setEnabled(false);

        if (isBound) {
            unbindService(bleConnection);
            isBound = false;
        }
        if (watchReceiver != null) {
            unregisterReceiver(watchReceiver);
            watchReceiver = null;
        }

        // Resetear indicadores
        if (config.necesitaESP32()) setEstadoESP32("Buscando...", false, false);
        if (config.necesitaWatch())  setEstadoWatch("Esperando datos...", false, false);

        iniciarConexiones();
    }

    // ================= IR A PRUEBA =================

    private void irAPrueba() {
        Intent intent = new Intent(this, VerificarSenalActivity.class);
        intent.putExtra("config", config);
        startActivity(intent);
    }

    // ================= CICLO DE VIDA =================

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timeoutHandler.removeCallbacksAndMessages(null);
        if (isBound) {
            unbindService(bleConnection);
            isBound = false;
        }
        if (watchReceiver != null) {
            unregisterReceiver(watchReceiver);
        }
    }
}