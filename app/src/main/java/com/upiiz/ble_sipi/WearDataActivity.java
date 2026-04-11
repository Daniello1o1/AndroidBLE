package com.upiiz.ble_sipi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.json.JSONException;
import org.json.JSONObject;

public class WearDataActivity extends AppCompatActivity {

    private TextView tvStatus;
    private TextView tvAccX, tvAccY, tvAccZ;
    private TextView tvGyroX, tvGyroY, tvGyroZ;
    private TextView tvPitch, tvRoll, tvYaw;

    private final BroadcastReceiver sensorReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (DataLayerListenerService.ACTION_SENSOR_DATA.equals(intent.getAction())) {
                String data = intent.getStringExtra(DataLayerListenerService.EXTRA_SENSOR_DATA);
                if (data != null) {
                    tvStatus.setText("Datos recibidos en tiempo real");
                    updateSensorViews(data);
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wear_data);

        tvStatus = findViewById(R.id.tvStatus);

        tvAccX = findViewById(R.id.tvAccX);
        tvAccY = findViewById(R.id.tvAccY);
        tvAccZ = findViewById(R.id.tvAccZ);

        tvGyroX = findViewById(R.id.tvGyroX);
        tvGyroY = findViewById(R.id.tvGyroY);
        tvGyroZ = findViewById(R.id.tvGyroZ);

        tvPitch = findViewById(R.id.tvPitch);
        tvRoll = findViewById(R.id.tvRoll);
        tvYaw = findViewById(R.id.tvYaw);
    }

    @Override
    protected void onStart() {
        super.onStart();

        IntentFilter filter = new IntentFilter(DataLayerListenerService.ACTION_SENSOR_DATA);
        ContextCompat.registerReceiver(
                this,
                sensorReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
        );
    }

    @Override
    protected void onStop() {
        super.onStop();
        unregisterReceiver(sensorReceiver);
    }

    private void updateSensorViews(String data) {
        try {
            JSONObject json = new JSONObject(data);

            tvAccX.setText("X: " + format(json.optDouble("accX")));
            tvAccY.setText("Y: " + format(json.optDouble("accY")));
            tvAccZ.setText("Z: " + format(json.optDouble("accZ")));

            tvGyroX.setText("X: " + format(json.optDouble("gyroX")));
            tvGyroY.setText("Y: " + format(json.optDouble("gyroY")));
            tvGyroZ.setText("Z: " + format(json.optDouble("gyroZ")));

            tvPitch.setText("Pitch: " + format(json.optDouble("pitch")) + "°");
            tvRoll.setText("Roll: " + format(json.optDouble("roll")) + "°");
            tvYaw.setText("Yaw: " + format(json.optDouble("yaw")) + "°");

        } catch (JSONException e) {
            tvStatus.setText("Error al leer los datos");
        }
    }

    private String format(double value) {
        return String.format("%.2f", value);
    }
}