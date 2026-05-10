package com.upiiz.ble_sipi.Views;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
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
import com.upiiz.ble_sipi.Models.Prueba;
import com.upiiz.ble_sipi.R;

import org.json.JSONObject;

import java.util.ArrayList;

public class VerificarSenalActivity extends AppCompatActivity {

    private Prueba config;

    // BLE
    private BLEService bleService;
    private boolean isBound = false;

    // Watch
    private BroadcastReceiver watchReceiver;

    // Gráfica
    private LineChart lineChart;
    private float xChart = 0;
    private final ArrayList<Float> pendingEmg    = new ArrayList<>();
    private final ArrayList<Float> pendingDynamo = new ArrayList<>();
    private final Handler plotHandler = new Handler();
    private static final int PLOT_INTERVAL_MS = 50;

    // UI
    private View cardWatch;
    private TextView tvAcc, tvGyro, tvOri;
    private MaterialButton btnIniciarPrueba, btnRegresar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_verificar_senal);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        config = (Prueba) getIntent().getSerializableExtra("config");

        bindViews();
        configurarChart();
        configurarUI();
        conectarESP32();
        registrarWatchReceiver();
        iniciarPlotLoop();
    }
    // ================= UI =================

    private void bindViews() {
        lineChart       = findViewById(R.id.chart);
        cardWatch       = findViewById(R.id.cardWatch);
        tvAcc           = findViewById(R.id.tvAcc);
        tvGyro          = findViewById(R.id.tvGyro);
        tvOri           = findViewById(R.id.tvOri);
        btnIniciarPrueba = findViewById(R.id.btnIniciarPrueba);
        btnRegresar     = findViewById(R.id.btnRegresar);

        btnIniciarPrueba.setOnClickListener(v -> irAPrueba());
        btnRegresar.setOnClickListener(v -> {
            finish(); // Regresa a la pantalla 3
        });
    }

    private void configurarUI() {
        // Mostrar card del Watch solo si se seleccionó
        if (config.necesitaWatch()) cardWatch.setVisibility(View.VISIBLE);

        // Ocultar gráfica si no usa ESP32
        if (!config.necesitaESP32()) {
            findViewById(R.id.tvLabelESP32).setVisibility(View.GONE);
            lineChart.setVisibility(View.GONE);
        }
    }

    // ================= ESP32 =================

    private void conectarESP32() {
        if (!config.necesitaESP32()) return;

        // El servicio ya está corriendo desde la pantalla 3
        // Solo nos enlazamos para recibir los datos
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
                synchronized (pendingEmg) {
                    // Solo graficar los sensores seleccionados
                    pendingEmg.add(config.usarEMG ? emg : 0f);
                    pendingDynamo.add(config.usarDinamometro ? dynamo : 0f);
                }
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

                    float accX  = (float) json.getDouble("accX");
                    float accY  = (float) json.getDouble("accY");
                    float accZ  = (float) json.getDouble("accZ");
                    float gyroX = (float) json.getDouble("gyroX");
                    float gyroY = (float) json.getDouble("gyroY");
                    float gyroZ = (float) json.getDouble("gyroZ");
                    float pitch = (float) json.getDouble("pitch");
                    float roll  = (float) json.getDouble("roll");
                    float yaw   = (float) json.getDouble("yaw");

                    runOnUiThread(() -> {
                        if (config.usarAcelerometro)
                            tvAcc.setText(String.format("%.1f  %.1f  %.1f", accX, accY, accZ));
                        if (config.usarGiroscopio)
                            tvGyro.setText(String.format("%.1f  %.1f  %.1f", gyroX, gyroY, gyroZ));
                        if (config.usarOrientacion)
                            tvOri.setText(String.format("%.1f  %.1f  %.1f", pitch, roll, yaw));
                    });

                } catch (Exception e) {
                    android.util.Log.e("VERIFICAR", "Error parseando Watch: " + e.getMessage());
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

                for (int i = 0; i < emgCopy.size(); i++) {
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

    // ================= NAVEGAR =================

    private void irAPrueba() {
        Intent intent = new Intent(this, EjecutarPruebaActivity.class);
        intent.putExtra("config", config);
        startActivity(intent);
    }

    // ================= CICLO DE VIDA =================

    @Override
    protected void onDestroy() {
        super.onDestroy();
        plotHandler.removeCallbacksAndMessages(null);
        if (isBound) {
            unbindService(bleConnection);
            isBound = false;
        }
        if (watchReceiver != null) unregisterReceiver(watchReceiver);
    }
}