package com.upiiz.ble_sipi;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.LineData;

import java.util.ArrayList;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    // CHART
    private LineChart lineChart;
    private float x = 0;
    //BLE
    BLEService bleService;
    boolean isBound = false;
    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

    // UI
    private TextView tvData;
    private Button btnConnect;
    private Button btnStart;
    private Button btnWearOS;
    Handler uiHandler = new Handler();

    private Handler plotHandler = new Handler();
    private Runnable plotRunnable;
    private ArrayList<Float> pendingEmg = new ArrayList<>();
    private ArrayList<Float> pendingDynamo = new ArrayList<>();
    private static final int PLOT_INTERVAL_MS = 50; // actualizar gráfico cada 50 ms (20 fps)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        plotRunnable = new Runnable() {
            @Override
            public void run() {
                // Copiar los pendientes y limpiar
                ArrayList<Float> emgCopy, dynamoCopy;
                synchronized (pendingEmg) {
                    if (pendingEmg.isEmpty()) {
                        plotHandler.postDelayed(this, PLOT_INTERVAL_MS);
                        return;
                    }
                    emgCopy = new ArrayList<>(pendingEmg);
                    dynamoCopy = new ArrayList<>(pendingDynamo);
                    pendingEmg.clear();
                    pendingDynamo.clear();
                }
                // Dibujar todos los puntos acumulados
                for (int i = 0; i < emgCopy.size(); i++) {
                    plotSamples(emgCopy.get(i), dynamoCopy.get(i));
                }
                plotHandler.postDelayed(this, PLOT_INTERVAL_MS);
            }
        };
        plotHandler.post(plotRunnable);



        if (bluetoothAdapter == null) {
            Toast.makeText(this, "El dispositivo no soporta Bluetooth", Toast.LENGTH_SHORT).show();
        } else {
            if (!bluetoothAdapter.isEnabled()) {
                Toast.makeText(this, "Activa el Bluetooth", Toast.LENGTH_SHORT).show();

                Intent intentBlue = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                startActivity(intentBlue);
            }
        }

        Intent intent = new Intent(this, BLEService.class);
        bindService(intent, connection, BIND_AUTO_CREATE);

        lineChart = findViewById(R.id.chart);
        tvData = findViewById(R.id.tvData);
        btnConnect = findViewById(R.id.btnConnect);
        btnStart = findViewById(R.id.btnStart);
        btnStart.setVisibility(View.GONE);
        //Reloj
        btnWearOS = findViewById(R.id.btnWearOS);

        LineDataSet emgSet = new LineDataSet(new ArrayList<>(), "EMG ENV");
        emgSet.setColor(Color.RED);
        emgSet.setDrawCircles(false);
        emgSet.setValueTextSize(0);
        emgSet.setLineWidth(1.2f);

        LineDataSet dynamoSet = new LineDataSet(new ArrayList<>(), "DYNAMO");
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
        yAxis.setAxisMaximum(3.5f); // Voltaje ESP32 (ADC 12 bits)
        yAxis.setLabelCount(12);
        yAxis.setTextColor(Color.BLACK);

        lineChart.getAxisRight().setEnabled(false);


        // Botones
        btnConnect.setOnClickListener(v -> {
            if (bluetoothAdapter == null) {
                Toast.makeText(this, "El dispositivo no soporta Bluetooth", Toast.LENGTH_SHORT).show();
            } else {
                if (!bluetoothAdapter.isEnabled()) {
                    Toast.makeText(this, "Activa el Bluetooth", Toast.LENGTH_SHORT).show();

                    Intent intentBlue = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    startActivity(intentBlue);
                }
                else{
                    if(isBound){
                        bleService.startScan();
                    }
                }
            }
        });
        btnStart.setOnClickListener(v -> {
            startActivity(new Intent(this, AnalisisActivity.class));
        });
        //Reloj
        btnWearOS.setOnClickListener(v -> {
            startActivity(new Intent(this, WearDataActivity.class));
        });
    }
    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            BLEService.LocalBinder binder = (BLEService.LocalBinder) service;
            bleService = binder.getService();
            isBound = true;

            bleService.setListener((emg, dynamo) -> {
                synchronized (pendingEmg) {
                    pendingEmg.add(emg);
                    pendingDynamo.add(dynamo);
                }
            });

            bleService.setOnConnectedListener(() -> {
                runOnUiThread(() -> btnStart.setVisibility(View.VISIBLE));
            });

        }
        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
            btnStart.setVisibility(View.GONE);
            synchronized (pendingEmg) {
                pendingEmg.clear();
                pendingDynamo.clear();
            }
        }

    };
    private void plotSamples(float emg, float dynamo) {

        LineData data = lineChart.getData();

        LineDataSet emgSet =
                (LineDataSet) data.getDataSetByIndex(0);

        LineDataSet dynamoSet =
                (LineDataSet) data.getDataSetByIndex(1);

        emgSet.addEntry(new Entry(x, emg));
        dynamoSet.addEntry(new Entry(x, dynamo));

        x++;


        if (emgSet.getEntryCount() > 500) {
            emgSet.removeFirst();
            dynamoSet.removeFirst();
        }

        data.notifyDataChanged();
        lineChart.notifyDataSetChanged();
        lineChart.moveViewToX(x);
        lineChart.invalidate();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        plotHandler.removeCallbacks(plotRunnable);
        if (isBound) {
            unbindService(connection);
            isBound = false;
        }
    }
}
