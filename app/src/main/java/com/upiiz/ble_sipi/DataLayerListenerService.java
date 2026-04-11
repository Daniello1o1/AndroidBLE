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

            Intent intent = new Intent(ACTION_SENSOR_DATA);
            intent.setPackage(getPackageName());
            intent.putExtra(EXTRA_SENSOR_DATA, data);
            sendBroadcast(intent);

            Log.d("PHONE_WEAR", "Broadcast enviado");
        }
    }
}