package com.upiiz.ble_sipi;

import android.Manifest;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.IBinder;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AnalisisActivity extends AppCompatActivity {
    private static final long TIEMPO_PRUEBA = 30 * 1000;
    private static final long TIEMPO_DESCANSO = 3 * 60 * 1000;

    private CountDownTimer timer;

    private int repeticiones = 0;
    private static final int TOTAL_REP = 10;

    private List<Float> muestrasEMG;
    private List<Float> muestrasDyn;

    Button btnIniciar, btnBack;

    TextView txtEstado, txtTiempo;
    private float sumaOrderV = 0;
    private float sumaMAV = 0;
    private float sumaWL = 0;

    private View time_circle;

    // BLE
    BLEService bleService;
    boolean isBound = false;

    private ServiceConnection connection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

            BLEService.LocalBinder binder = (BLEService.LocalBinder) service;
            bleService = binder.getService();
            isBound = true;

            // usar el mismo buffer de muestras del service
            muestrasEMG = bleService.emgSamples;
            muestrasDyn = bleService.dynamoSamples;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_analisis);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Intent intent = new Intent(this, BLEService.class);
        bindService(intent, connection, BIND_AUTO_CREATE);

        muestrasEMG = new ArrayList<>();
        muestrasDyn = new ArrayList<>();
        txtEstado = findViewById(R.id.txtEstado);
        txtTiempo = findViewById(R.id.txtTiempo);
        btnIniciar = findViewById(R.id.btnIniciar);
        time_circle = findViewById(R.id.time_circle);

        btnBack = findViewById(R.id.btnBack);


        btnIniciar.setOnClickListener(v -> iniciarPrueba());
        btnBack.setOnClickListener(v->finish());
    }
    private void iniciarPrueba() {
        if (muestrasEMG != null)
            muestrasEMG.clear();
        if (muestrasDyn != null)
            muestrasDyn.clear();
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(
                time_circle,
                PropertyValuesHolder.ofFloat("scaleX", 1f, 2f, 1f),
                PropertyValuesHolder.ofFloat("scaleY", 1f, 2f, 1f)
        );

        animator.setDuration(3000);
        animator.setRepeatCount(9);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.start();

        btnIniciar.setVisibility(View.GONE);
        txtEstado.setText("Realiza 10 repeticiones de flexión-extensión");
        iniciarConteo(TIEMPO_PRUEBA, "Tiempo terminado. Descanso de 3 minutos...");
    }
    private void iniciarConteo(long tiempoMS, String mensajeFin) {



        timer = new CountDownTimer(tiempoMS, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                long sec = millisUntilFinished / 1000;
                txtTiempo.setText(sec + " s");
            }

            @Override
            public void onFinish() {
                txtTiempo.setText("0s");
                txtEstado.setText(mensajeFin);

                AlertDialog.Builder builder = new AlertDialog.Builder(AnalisisActivity.this);
                builder.setTitle("Resultado");
                builder.setMessage("MAV: " + MAV(muestrasEMG) + "\n" +
                                    "WL: " + WL(muestrasEMG) + "\n" +
                                    "Order V: " + orderV(muestrasEMG) + "\n" +
                                    "Dynamo MAV: " + MAV(muestrasDyn) + "\n"
                        );

                builder.setPositiveButton("OK", null);

                AlertDialog dialog = builder.create();
                dialog.show();

                if (mensajeFin.contains("Descanso")) {
                    //iniciarDescanso();
                //} else {
                    terminarTodo();
                }
            }
        };

        timer.start();
    }

    private void iniciarDescanso() {
        iniciarConteo(TIEMPO_DESCANSO, "Descanso terminado. Prueba completa.");
    }

    private void terminarTodo() {
        btnIniciar.setVisibility(View.VISIBLE);
        btnIniciar.setText("Reiniciar prueba");
        txtEstado.setText("¡Listo! Puedes repetir la prueba.");
        Log.d("numMuestras", String.valueOf(muestrasEMG.size()));
    }
    public float MAV(List<Float> muestras){
        float suma = 0;
        for(int i=0;i<muestras.size();i++){
            suma += muestras.get(i);
        }
        return suma / muestras.size();
    }
    public float orderV(List<Float> muestras){
        float suma = 0;
        for(int i=0;i<muestras.size();i++){
            suma += (float) Math.pow(muestras.get(i),2);
        }
        return (float) Math.sqrt(suma / muestras.size());
    }

    public float WL(List<Float> muestras){
        float suma = 0;
        for(int i=0;i<muestras.size()-1;i++){
            suma += Math.abs(muestras.get(i+1) - muestras.get(i));
        }
        return suma;
    }
}