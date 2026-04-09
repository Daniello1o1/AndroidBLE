package com.upiiz.ble_sipi;

import android.Manifest;
import android.app.Service;
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
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.UUID;

public class BLEService extends Service {

    public static final String DEVICE_NAME = "MicroC";

    static final String SERVICE_UUID = "4fafc201-1fb5-459e-8fcc-c5c9c331914b";
    static final String CHARACTERISTIC_UUID = "beb5483e-36e1-4688-b7f5-ea07361b26a8";

    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner bleScanner;
    private BluetoothGatt bluetoothGatt;
    private OnConnectedListener onConnectedListener;

    public ArrayList<Float> emgSamples = new ArrayList<>();
    public ArrayList<Float> dynamoSamples = new ArrayList<>();

    private int totalMuestrasRecibidas = 0;
    private long lastTime = 0;
    private int packetCount = 0;
    private long lastPacketLog = 0;

    public interface DataListener{
        void onDataReceived(float emg, float dynamo);
    }
    public interface OnConnectedListener {
        void onConnected();
    }

    public void setOnConnectedListener(OnConnectedListener listener) {
        this.onConnectedListener = listener;
    }

    private DataListener listener;

    public void setListener(DataListener listener){
        this.listener = listener;
    }

    private final IBinder binder = new LocalBinder();
    private int lastExpectedIndex = -1;

    public class LocalBinder extends Binder {
        BLEService getService(){
            return BLEService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bleScanner = bluetoothAdapter.getBluetoothLeScanner();
        return binder;
    }

    // ================= SCAN =================

    public void startScan(){
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }
        bleScanner.startScan(scanCallback);
    }

    private final ScanCallback scanCallback = new ScanCallback(){
        @Override
        public void onScanResult(int callbackType, ScanResult result) {

            BluetoothDevice device = result.getDevice();

            if (ActivityCompat.checkSelfPermission(BLEService.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            if(device.getName() != null && device.getName().equals(DEVICE_NAME)){

                bleScanner.stopScan(this);

                connect(device);
            }
        }
    };

    // ================= CONNECT =================

    private void connect(BluetoothDevice device){

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
        bluetoothGatt = device.connectGatt(
                this,
                false,
                gattCallback,
                BluetoothDevice.TRANSPORT_LE
        );
    }

    // ================= CALLBACK =================

    private final BluetoothGattCallback gattCallback = new BluetoothGattCallback(){

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            if(newState == BluetoothGatt.STATE_CONNECTED){
                if (ActivityCompat.checkSelfPermission(BLEService.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    // here to request the missing permissions, and then overriding
                    //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                    //                                          int[] grantResults)
                    // to handle the case where the user grants the permission. See the documentation
                    // for ActivityCompat#requestPermissions for more details.
                    return;
                }
                if (onConnectedListener != null) {
                    onConnectedListener.onConnected();
                }
                gatt.discoverServices();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            BluetoothGattService service =
                    gatt.getService(UUID.fromString(SERVICE_UUID));

            if(service == null) return;

            BluetoothGattCharacteristic chara =
                    service.getCharacteristic(UUID.fromString(CHARACTERISTIC_UUID));

            if(chara == null) return;

            if (ActivityCompat.checkSelfPermission(BLEService.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                //    ActivityCompat#requestPermissions
                // here to request the missing permissions, and then overriding
                //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                //                                          int[] grantResults)
                // to handle the case where the user grants the permission. See the documentation
                // for ActivityCompat#requestPermissions for more details.
                return;
            }
            gatt.setCharacteristicNotification(chara, true);

            for(BluetoothGattDescriptor d : chara.getDescriptors()){
                d.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                gatt.writeDescriptor(d);
            }

            gatt.requestConnectionPriority(BluetoothGatt.CONNECTION_PRIORITY_HIGH);
            gatt.requestMtu(256);
        }
        @Override
        public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
            Log.d("BLE_MTU", "MTU negociado: " + mtu + ", status: " + status);
        }
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            byte[] data = characteristic.getValue();
            if (data == null || data.length < 4) return;

            // Leer timestamp (primeros 4 bytes, big-endian)
            int firstIndex = ((data[0] & 0xFF) << 24) |
                    ((data[1] & 0xFF) << 16) |
                    ((data[2] & 0xFF) << 8)  |
                    (data[3] & 0xFF);

            // Calcular cuántas muestras vienen en este paquete
            int samples = (data.length - 4) / 4;  // cada muestra ocupa 4 bytes (2 EMG + 2 DIN)
            if (samples == 0) return;

            // Detectar pérdidas
            if (lastExpectedIndex != -1 && firstIndex != lastExpectedIndex) {
                int lost = firstIndex - lastExpectedIndex;
            }
            // Actualizar el próximo índice esperado
            lastExpectedIndex = firstIndex + samples;

            // Procesar cada muestra
            for (int i = 0; i < samples; i++) {
                int offset = 4 + i * 4;
                int rawEMG = ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
                int rawDynamo = ((data[offset + 2] & 0xFF) << 8) | (data[offset + 3] & 0xFF);

                float emgVolt = (rawEMG * 3.3f) / 4095.0f;
                float dynamoVolt = (rawDynamo * 3.3f) / 4095.0f;

                emgSamples.add(emgVolt);
                dynamoSamples.add(dynamoVolt);

                if (listener != null) {
                    listener.onDataReceived(emgVolt, dynamoVolt);
                }
            }


// dentro del bucle for de cada muestra:
            packetCount++;
            long now = System.currentTimeMillis();
            if (now - lastPacketLog >= 1000) {
                Log.d("BLE_STATS", "Paquetes/s: " + packetCount + ", Muestras/s: " + (packetCount * samples));
                packetCount = 0;
                lastPacketLog = now;
            }
        }
    };
}