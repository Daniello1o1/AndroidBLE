package com.upiiz.ble_sipi;

import android.Manifest;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothProfile;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import java.util.UUID;

public class BLEManager {


    // Mantiene referencia al dispositivo BLE conectado
    public static BluetoothDevice device = null;

    // Mantiene la conexión activa entre Activities
    public static BluetoothGatt gatt = null;

    // Característica usada para notificaciones
    public static BluetoothGattCharacteristic characteristic = null;
}