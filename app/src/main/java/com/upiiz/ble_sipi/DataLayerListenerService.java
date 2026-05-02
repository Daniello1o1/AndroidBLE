package com.upiiz.ble_sipi;

import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;

import java.nio.charset.StandardCharsets;

public class DataLayerListenerService extends WearableListenerService {

    public static final String ACTION_SENSOR_DATA = "com.upiiz.ble_sipi.ACTION_SENSOR_DATA";
    public static final String EXTRA_SENSOR_DATA = "extra_sensor_data";
    public static final String SENSOR_PATH = "/sensor_data";

    @Override
    public void onMessageReceived(@NonNull MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);

        Log.d("PHONE_WEAR", "Mensaje recibido. Path: " + messageEvent.getPath());

        if (SENSOR_PATH.equals(messageEvent.getPath())) {
            String data = new String(messageEvent.getData(), StandardCharsets.UTF_8);
            Log.d("PHONE_WEAR", "Data recibida: " + data);

            try {
                org.json.JSONObject json = new org.json.JSONObject(data);

                long  timestampMs = json.getLong("timestamp");
                float accX        = (float) json.getDouble("accX");
                float accY        = (float) json.getDouble("accY");
                float accZ        = (float) json.getDouble("accZ");
                float gyroX       = (float) json.getDouble("gyroX");
                float gyroY       = (float) json.getDouble("gyroY");
                float gyroZ       = (float) json.getDouble("gyroZ");
                float pitch       = (float) json.getDouble("pitch");
                float roll        = (float) json.getDouble("roll");
                float yaw         = (float) json.getDouble("yaw");

                Log.d("PHONE_WEAR", "Timestamp: " + timestampMs + "ms" +
                        " | Acc: " + accX + ", " + accY + ", " + accZ +
                        " | Gyro: " + gyroX + ", " + gyroY + ", " + gyroZ +
                        " | Pitch: " + pitch + " Roll: " + roll + " Yaw: " + yaw);

            } catch (org.json.JSONException e) {
                Log.e("PHONE_WEAR", "Error parseando JSON: " + e.getMessage());
            }

            Intent intent = new Intent(ACTION_SENSOR_DATA);
            intent.setPackage(getPackageName());
            intent.putExtra(EXTRA_SENSOR_DATA, data);
            sendBroadcast(intent);
        }
    }
}