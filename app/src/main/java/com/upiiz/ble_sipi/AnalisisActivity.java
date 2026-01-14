package com.upiiz.ble_sipi;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.List;

public class AnalisisActivity extends AppCompatActivity {
    private static final long TIEMPO_PRUEBA = 30 * 1000;
    private static final long TIEMPO_DESCANSO = 3 * 60 * 1000;

    private CountDownTimer timer;

    private int repeticiones = 0;
    private static final int TOTAL_REP = 10;

    Button btnIniciar;

    TextView txtEstado, txtTiempo;
    private float sumaOrderV = 0;
    private float sumaMAV = 0;
    private float sumaWL = 0;


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
        txtEstado = findViewById(R.id.txtEstado);
        txtTiempo = findViewById(R.id.txtTiempo);
        btnIniciar = findViewById(R.id.btnIniciar);

        btnIniciar.setOnClickListener(v -> iniciarPrueba());
    }
    private void iniciarPrueba() {
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

                if (mensajeFin.contains("Descanso")) {
                    iniciarDescanso();
                } else {
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
    public float WLRT(float anterior, float nueva){
        sumaWL += Math.abs(nueva - anterior);
        return sumaWL;
    }
}