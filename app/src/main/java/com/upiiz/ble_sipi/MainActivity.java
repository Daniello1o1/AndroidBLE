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
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
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

    // === GRAFICA ===
    private LineChart lineChart;
    private float x = 0;

    // === BLE UUIDs ===
    private static final String SERVICE_UUID = "4fafc201-1fb5-459e-8fcc-c5c9c331914b";
    private static final String CHARACTERISTIC_UUID = "beb5483e-36e1-4688-b7f5-ea07361b26a8";
    private static final String DEVICE_NAME = "MicroC";

    // === BLE ===
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bleScanner;
    private BluetoothGatt bluetoothGatt;

    // === UI ===
    private TextView tvData;
    private Button btnConnect;

    private final int REQUEST_ENABLE_BT = 1;

    Handler uiHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // == UI ==
        lineChart = findViewById(R.id.chart);
        tvData = findViewById(R.id.tvData);
        btnConnect = findViewById(R.id.btnConnect);


        // === CONFIGURAR GRAFICA ===
        LineDataSet dataSet = new LineDataSet(new ArrayList<>(), "EMG ENV");
        dataSet.setColor(Color.BLUE);
        dataSet.setDrawCircles(false);
        dataSet.setLineWidth(1.2f);

        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);

        lineChart.setTouchEnabled(true);
        lineChart.setPinchZoom(true);

        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);

        YAxis yAxis = lineChart.getAxisLeft();
        yAxis.setAxisMinimum(0f);
        yAxis.setAxisMaximum(3.5f); // Voltaje ESP32 (ADC 12 bits)
        yAxis.setLabelCount(12);
        yAxis.setTextColor(Color.WHITE);

        lineChart.getAxisRight().setEnabled(false);

        // === BLE ===
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bleScanner = bluetoothAdapter.getBluetoothLeScanner();

        // === BOTONES ===
        btnConnect.setOnClickListener(v -> startScan());

    }

    private void startScan() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this, new String[]{
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
            }, REQUEST_ENABLE_BT);

            return;
        }

        Toast.makeText(this, "Buscando ESP32...", Toast.LENGTH_SHORT).show();
        bleScanner.startScan(scanCallback);
    }

    private final ScanCallback scanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {

            BluetoothDevice device = result.getDevice();

            // Verificar permisos
            if (ActivityCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
                return;

            // Coincide el nombre
            if (device.getName() != null && device.getName().equals(DEVICE_NAME)) {

                Toast.makeText(MainActivity.this,
                        "ESP32 encontrado: " + DEVICE_NAME, Toast.LENGTH_SHORT).show();

                bleScanner.stopScan(this);

                connectToDevice(device);
            }
        }
    };

    private void connectToDevice(BluetoothDevice device) {

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
            return;

        bluetoothGatt = device.connectGatt(
                MainActivity.this,
                false,
                gattCallback,
                BluetoothDevice.TRANSPORT_LE
        );
    }

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            if (newState == BluetoothGatt.STATE_CONNECTED) {

                uiHandler.post(() -> tvData.setText("Conectado. Descubriendo servicios..."));

                if (ActivityCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
                    return;

                gatt.discoverServices();
            }
            else{
                uiHandler.post(() -> tvData.setText("Desconectado"));
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            BluetoothGattService service =
                    gatt.getService(UUID.fromString(SERVICE_UUID));

            if (service == null) return;

            BluetoothGattCharacteristic chara =
                    service.getCharacteristic(UUID.fromString(CHARACTERISTIC_UUID));

            if (chara == null) return;

            // Activar notificaciones
            if (ActivityCompat.checkSelfPermission(MainActivity.this,
                    Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
                return;

            gatt.setCharacteristicNotification(chara, true);

            for (BluetoothGattDescriptor d : chara.getDescriptors()) {
                d.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(d);
            }

            uiHandler.post(() -> tvData.setText("Notificaciones activadas"));
        }
        
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {

            byte[] data = characteristic.getValue();

            if (data.length >= 2) {

                int raw = ((data[1] & 0xFF) << 8) | (data[0] & 0xFF);

                float volt = (raw * 3.3f) / 4095.0f;

                uiHandler.post(() -> plotSample(volt));
            }
        }
    };

    private void plotSample(float sample) {

        LineDataSet dataSet =
                (LineDataSet) lineChart.getData().getDataSetByIndex(0);

        dataSet.addEntry(new Entry(x, sample));
        x++;

        if (dataSet.getEntryCount() > 300)
            dataSet.removeFirst();

        lineChart.getData().notifyDataChanged();
        lineChart.notifyDataSetChanged();
        lineChart.moveViewToX(x);
        lineChart.invalidate();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == REQUEST_ENABLE_BT)
            startScan();
    }
}
