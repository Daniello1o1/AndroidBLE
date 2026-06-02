package com.upiiz.ble_sipi.Views;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.button.MaterialButton;
import com.upiiz.ble_sipi.BLE.BLEService;
import com.upiiz.ble_sipi.BLE.DataLayerListenerService;
import com.upiiz.ble_sipi.Models.FasePrueba;
import com.upiiz.ble_sipi.Models.MuestraDato;
import com.upiiz.ble_sipi.Models.Paciente;
import com.upiiz.ble_sipi.Models.Prueba;
import com.upiiz.ble_sipi.R;
import com.upiiz.ble_sipi.Tools.MuestrasCache;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class EjecutarPruebaActivity extends AppCompatActivity {

    // Config
    private Prueba config;

    // Datos recolectados
    private final List<MuestraDato> muestras = new ArrayList<>();

    // Estado de fases
    private int faseActualIndex = 0;
    private int segundosFaseRestantes = 0;
    private int segundosTotalRestantes = 0;

    // Último dato del Watch
    private float lastAccX, lastAccY, lastAccZ;
    private float lastGyroX, lastGyroY, lastGyroZ;
    private float lastPitch, lastRoll, lastYaw;
    private long lastWatchTimestamp = 0;

    // Handlers
    private final Handler timerHandler = new Handler();
    private final Handler plotHandler  = new Handler();

    // Buffer de gráfica
    private final ArrayList<Float> pendingEmg    = new ArrayList<>();
    private final ArrayList<Float> pendingDynamo = new ArrayList<>();
    private float xChart = 0;
    private static final int PLOT_INTERVAL_MS = 50;

    // BLE
    private BLEService bleService;
    private boolean isBound = false;

    // Watch receiver
    private BroadcastReceiver watchReceiver;

    // Sonido
    private ToneGenerator toneGenerator;

    // UI
    private TextView tvNombrePrueba, tvFaseActual, tvNumeroFase;
    private TextView tvTiempoFase, tvTiempoTotal;
    private TextView tvAcc, tvGyro, tvOri;
    private View cardWatch;
    private LineChart lineChart;
    private MaterialButton btnDetener;

    //Hilo

    private final java.util.concurrent.BlockingQueue<float[]> colaRaw =
            new java.util.concurrent.LinkedBlockingQueue<>();

    private Thread hiloGuardado;
    private volatile boolean corriendo = true;
    private Paciente paciente;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_ejecutar_prueba);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        config = (Prueba) getIntent().getSerializableExtra("config");
        toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 80);
        paciente = (Paciente) getIntent().getSerializableExtra("paciente");

        bindViews();
        configurarChart();
        configurarUI();
        conectarESP32();
        registrarWatchReceiver();
        iniciarTimer();
        iniciarPlotLoop();
        iniciarHiloGuardado();

        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {

                        new AlertDialog.Builder(EjecutarPruebaActivity.this)
                                .setTitle("¿Salir de la prueba?")
                                .setMessage("Se perderán todos los datos recopilados hasta ahora.")
                                .setPositiveButton("Sí, salir", (d, w) -> {

                                    timerHandler.removeCallbacksAndMessages(null);
                                    plotHandler.removeCallbacksAndMessages(null);

                                    corriendo = false;

                                    if (hiloGuardado != null) {
                                        hiloGuardado.interrupt();
                                    }

                                    MuestrasCache.limpiar();
                                    finish();
                                })
                                .setNegativeButton("Continuar prueba", null)
                                .show();
                    }
                });
    }

    private void bindViews() {
        tvNombrePrueba = findViewById(R.id.tvNombrePrueba);
        tvFaseActual   = findViewById(R.id.tvFaseActual);
        tvNumeroFase   = findViewById(R.id.tvNumeroFase);
        tvTiempoFase   = findViewById(R.id.tvTiempoFase);
        tvTiempoTotal  = findViewById(R.id.tvTiempoTotal);
        tvAcc          = findViewById(R.id.tvAcc);
        tvGyro         = findViewById(R.id.tvGyro);
        tvOri          = findViewById(R.id.tvOri);
        cardWatch      = findViewById(R.id.cardWatch);
        lineChart      = findViewById(R.id.chart);
        btnDetener     = findViewById(R.id.btnDetener);

        btnDetener.setOnClickListener(v -> detenerPrueba());
    }

    private void configurarUI() {
        tvNombrePrueba.setText(config.nombre);
        segundosTotalRestantes = config.duracionTotalSegundos;

        if (config.necesitaWatch()) cardWatch.setVisibility(View.VISIBLE);

        actualizarUIFase();
    }

    private void actualizarUIFase() {
        if (config.tieneIntervalos && !config.fases.isEmpty()) {
            FasePrueba fase = config.fases.get(faseActualIndex);
            tvFaseActual.setText(fase.nombre);
            tvNumeroFase.setText("Fase " + (faseActualIndex + 1) + " de " + config.fases.size());
            segundosFaseRestantes = fase.duracionSegundos;
        } else {
            tvFaseActual.setText("Prueba en curso");
            tvNumeroFase.setText("");
            segundosFaseRestantes = config.duracionTotalSegundos;
        }
    }

    private String getFaseActualNombre() {
        if (config.tieneIntervalos && !config.fases.isEmpty()) {
            return config.fases.get(faseActualIndex).nombre;
        }
        return "General";
    }

    // ================= TIMER =================

    private void iniciarTimer() {
        timerHandler.post(timerRunnable);
    }

    private final Runnable timerRunnable = new Runnable() {
        @Override
        public void run() {
            if (segundosTotalRestantes <= 0) {
                detenerPrueba();
                return;
            }

            // Actualizar contadores
            segundosTotalRestantes--;
            segundosFaseRestantes--;

            // Actualizar UI
            tvTiempoTotal.setText(formatTiempo(segundosTotalRestantes));
            tvTiempoFase.setText(formatTiempo(segundosFaseRestantes));

            // Cambiar de fase si se acabó el tiempo de la fase actual
            if (config.tieneIntervalos
                    && segundosFaseRestantes <= 0
                    && faseActualIndex < config.fases.size() - 1) {

                faseActualIndex++;
                actualizarUIFase();
                sonarCambioDeFase();
            }

            timerHandler.postDelayed(this, 1000);
        }
    };

    private String formatTiempo(int segundos) {
        int min = segundos / 60;
        int sec = segundos % 60;
        return String.format("%02d:%02d", min, sec);
    }

    private void sonarCambioDeFase() {
        toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP, 400);
    }

    // ================= ESP32 =================

    private void conectarESP32() {
        if (!config.necesitaESP32()) return;

        Intent intent = new Intent(this, BLEService.class);
        bindService(intent, bleConnection, BIND_AUTO_CREATE);
    }

    private final ServiceConnection bleConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BLEService.LocalBinder binder = (BLEService.LocalBinder) service;
            bleService = binder.getService();
            isBound = true;

            bleService.setListener((emg, dynamo) -> {
                // Solo encolar — rapidísimo, no bloquea nada
                colaRaw.offer(new float[]{emg, dynamo});
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    // ================= WATCH =================

    private void registrarWatchReceiver() {
        if (!config.necesitaWatch()) return;

        watchReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String data = intent.getStringExtra(DataLayerListenerService.EXTRA_SENSOR_DATA);
                if (data == null) return;

                try {
                    JSONObject json = new JSONObject(data);
                    lastWatchTimestamp = json.getLong("timestamp");
                    lastAccX  = (float) json.getDouble("accX");
                    lastAccY  = (float) json.getDouble("accY");
                    lastAccZ  = (float) json.getDouble("accZ");
                    lastGyroX = (float) json.getDouble("gyroX");
                    lastGyroY = (float) json.getDouble("gyroY");
                    lastGyroZ = (float) json.getDouble("gyroZ");
                    lastPitch = (float) json.getDouble("pitch");
                    lastRoll  = (float) json.getDouble("roll");
                    lastYaw   = (float) json.getDouble("yaw");

                    // Si la prueba no usa ESP32, guardar muestra desde el Watch
                    if (!config.necesitaESP32()) {
                        MuestraDato m = new MuestraDato(lastWatchTimestamp, getFaseActualNombre());
                        m.accX = lastAccX; m.accY = lastAccY; m.accZ = lastAccZ;
                        m.gyroX = lastGyroX; m.gyroY = lastGyroY; m.gyroZ = lastGyroZ;
                        m.pitch = lastPitch; m.roll = lastRoll; m.yaw = lastYaw;
                        synchronized (muestras) { muestras.add(m); }
                    }

                    // Actualizar UI del Watch
                    runOnUiThread(() -> {
                        if (config.usarAcelerometro)
                            tvAcc.setText(String.format("%.1f  %.1f  %.1f", lastAccX, lastAccY, lastAccZ));
                        if (config.usarGiroscopio)
                            tvGyro.setText(String.format("%.1f  %.1f  %.1f", lastGyroX, lastGyroY, lastGyroZ));
                        if (config.usarOrientacion)
                            tvOri.setText(String.format("%.1f  %.1f  %.1f", lastPitch, lastRoll, lastYaw));
                    });

                } catch (Exception e) {
                    android.util.Log.e("PRUEBA", "Error parseando Watch: " + e.getMessage());
                }
            }
        };

        ContextCompat.registerReceiver(
                this,
                watchReceiver,
                new IntentFilter(DataLayerListenerService.ACTION_SENSOR_DATA),
                ContextCompat.RECEIVER_NOT_EXPORTED
        );
    }

    // ================= GRÁFICA =================

    private void configurarChart() {
        LineDataSet emgSet = new LineDataSet(new ArrayList<>(), "EMG");
        emgSet.setColor(Color.RED);
        emgSet.setDrawCircles(false);
        emgSet.setValueTextSize(0);
        emgSet.setLineWidth(1.2f);

        LineDataSet dynamoSet = new LineDataSet(new ArrayList<>(), "Dinamómetro");
        dynamoSet.setColor(Color.BLUE);
        dynamoSet.setDrawCircles(false);
        dynamoSet.setValueTextSize(0);
        dynamoSet.setLineWidth(1.2f);

        LineData lineData = new LineData();
        lineData.addDataSet(emgSet);
        lineData.addDataSet(dynamoSet);
        lineChart.setData(lineData);

        lineChart.setTouchEnabled(false);
        lineChart.setPinchZoom(false);
        lineChart.getDescription().setEnabled(false);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawLabels(false);

        YAxis yAxis = lineChart.getAxisLeft();
        yAxis.setAxisMinimum(0f);
        yAxis.setAxisMaximum(3.5f);
        yAxis.setLabelCount(8);

        lineChart.getAxisRight().setEnabled(false);
    }

    private void iniciarPlotLoop() {
        if (!config.necesitaESP32()) return;

        plotHandler.post(new Runnable() {
            @Override
            public void run() {
                ArrayList<Float> emgCopy, dynamoCopy;
                synchronized (pendingEmg) {
                    if (pendingEmg.isEmpty()) {
                        plotHandler.postDelayed(this, PLOT_INTERVAL_MS);
                        return;
                    }
                    emgCopy    = new ArrayList<>(pendingEmg);
                    dynamoCopy = new ArrayList<>(pendingDynamo);
                    pendingEmg.clear();
                    pendingDynamo.clear();
                }

                // Tomar solo los últimos 100 puntos si hay demasiados
                // En lugar de los primeros — así la gráfica siempre muestra lo más reciente
                int total = emgCopy.size();
                int inicio = Math.max(0, total - 100);
                for (int i = inicio; i < total; i++) {
                    agregarPuntoChart(emgCopy.get(i), dynamoCopy.get(i));
                }

                plotHandler.postDelayed(this, PLOT_INTERVAL_MS);
            }
        });
    }

    private void agregarPuntoChart(float emg, float dynamo) {
        LineData data = lineChart.getData();
        LineDataSet emgSet    = (LineDataSet) data.getDataSetByIndex(0);
        LineDataSet dynamoSet = (LineDataSet) data.getDataSetByIndex(1);

        emgSet.addEntry(new Entry(xChart, emg));
        dynamoSet.addEntry(new Entry(xChart, dynamo));
        xChart++;

        if (emgSet.getEntryCount() > 500) {
            emgSet.removeFirst();
            dynamoSet.removeFirst();
        }

        data.notifyDataChanged();
        lineChart.notifyDataSetChanged();
        lineChart.moveViewToX(xChart);
        lineChart.invalidate();
    }

    // ================= DETENER =================

    private void detenerPrueba() {
        timerHandler.removeCallbacksAndMessages(null);
        plotHandler.removeCallbacksAndMessages(null);

        // Detener hilo de guardado
        corriendo = false;
        if (hiloGuardado != null) hiloGuardado.interrupt();

        // Esperar a que el hilo termine de procesar lo que queda en la cola
        try {
            if (hiloGuardado != null) hiloGuardado.join(500); // esperar máx 500ms
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        ArrayList<MuestraDato> listaMuestras;
        synchronized (muestras) {
            listaMuestras = new ArrayList<>(muestras);
        }

        MuestrasCache.guardar(listaMuestras);

        Intent intent = new Intent(this, ResumenPruebaActivity.class);
        intent.putExtra("config", config);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        timerHandler.removeCallbacksAndMessages(null);
        plotHandler.removeCallbacksAndMessages(null);

        // Detener hilo de guardado si aún corre
        corriendo = false;
        if (hiloGuardado != null) hiloGuardado.interrupt();

        if (isBound) {
            unbindService(bleConnection);
            isBound = false;
        }
        if (watchReceiver != null) unregisterReceiver(watchReceiver);
        if (toneGenerator != null) toneGenerator.release();
    }

    private void iniciarHiloGuardado() {
        hiloGuardado = new Thread(() -> {
            while (corriendo) {
                try {
                    // Espera hasta que llegue un dato — no consume CPU
                    float[] muestra = colaRaw.poll(100,
                            java.util.concurrent.TimeUnit.MILLISECONDS);
                    if (muestra == null) continue;

                    float emg    = muestra[0];
                    float dynamo = muestra[1];

                    // Guardar muestra — en hilo secundario, no bloquea UI
                    MuestraDato m = new MuestraDato(
                            System.currentTimeMillis(), getFaseActualNombre());

                    // Contexto
                    m.pruebaId       = config.id;
                    m.pruebaNombre   = config.nombre;
                    m.pacienteId     = config.pacienteId;
                    m.pacienteNombre = paciente != null ? paciente.getNombreCompleto() : "";
                    m.pacienteEdad   = paciente != null ? paciente.edad : 0;
                    m.pacienteSexo   = paciente != null ? paciente.sexo : "";

                    // ESP32 — solo si se usa
                    if (config.usarEMG)         m.emg         = emg;
                    if (config.usarDinamometro) m.dinamometro = dynamo;

                    // Watch — solo si se usa, con los últimos valores recibidos
                    if (config.usarAcelerometro) {
                        m.accX = lastAccX;
                        m.accY = lastAccY;
                        m.accZ = lastAccZ;
                    }
                    if (config.usarGiroscopio) {
                        m.gyroX = lastGyroX;
                        m.gyroY = lastGyroY;
                        m.gyroZ = lastGyroZ;
                    }
                    if (config.usarOrientacion) {
                        m.pitch = lastPitch;
                        m.roll  = lastRoll;
                        m.yaw   = lastYaw;
                    }

                    synchronized (muestras) { muestras.add(m); }

                    // Buffer para gráfica — solo agregar, no dibujar
                    synchronized (pendingEmg) {
                        pendingEmg.add(emg);
                        pendingDynamo.add(dynamo);
                    }

                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        });
        hiloGuardado.setName("hilo-guardado");
        hiloGuardado.start();
    }
}