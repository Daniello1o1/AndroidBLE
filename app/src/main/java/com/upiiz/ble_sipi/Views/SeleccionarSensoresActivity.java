package com.upiiz.ble_sipi.Views;

import android.content.Intent;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.upiiz.ble_sipi.Models.Prueba;
import com.upiiz.ble_sipi.R;
import com.upiiz.ble_sipi.Repository.PruebaRepository;

import java.util.ArrayList;

public class SeleccionarSensoresActivity extends AppCompatActivity {

    private CheckBox cbEMG, cbDinamometro;
    private CheckBox cbAcelerometro, cbGiroscopio, cbOrientacion;
    private TextView tvAdvertencia;
    private MaterialButton btnSiguiente;
    private Prueba config;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_seleccionar_sensores);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Reconstruir el PruebaConfig que viene de la pantalla 1
        config = new Prueba();
        config = (Prueba) getIntent().getSerializableExtra("config");


        // Bindear vistas
        cbEMG          = findViewById(R.id.cbEMG);
        cbDinamometro  = findViewById(R.id.cbDinamometro);
        cbAcelerometro = findViewById(R.id.cbAcelerometro);
        cbGiroscopio   = findViewById(R.id.cbGiroscopio);
        cbOrientacion  = findViewById(R.id.cbOrientacion);
        tvAdvertencia  = findViewById(R.id.tvAdvertenciaSensores);
        btnSiguiente   = findViewById(R.id.btnSiguiente);

        btnSiguiente.setOnClickListener(v -> siguiente());
    }
    private void siguiente() {

        // Validar que al menos un sensor esté seleccionado
        boolean alguno = cbEMG.isChecked() || cbDinamometro.isChecked()
                || cbAcelerometro.isChecked() || cbGiroscopio.isChecked()
                || cbOrientacion.isChecked();

        if (!alguno) {
            tvAdvertencia.setText("Selecciona al menos un sensor");
            return;
        }

        tvAdvertencia.setText("");

        // Guardar selección en el config
        config.usarEMG          = cbEMG.isChecked();
        config.usarDinamometro  = cbDinamometro.isChecked();
        config.usarAcelerometro = cbAcelerometro.isChecked();
        config.usarGiroscopio   = cbGiroscopio.isChecked();
        config.usarOrientacion  = cbOrientacion.isChecked();

        // Deshabilitar botón mientras guarda
        btnSiguiente.setEnabled(false);
        btnSiguiente.setText("Guardando...");

        // Guardar en Firestore antes de continuar
        PruebaRepository repository = new PruebaRepository();
        repository.crearPrueba(config, new PruebaRepository.Callback<String>() {

            @Override
            public void onSuccess(String id) {
                // Guardar el ID que nos dio Firestore en el objeto
                config.id = id;

                // Navegar a pantalla 3
                Intent intent = new Intent(
                        SeleccionarSensoresActivity.this,
                        ConectarDispositivosActivity.class);
                intent.putExtra("config", config);
                startActivity(intent);

                btnSiguiente.setEnabled(true);
                btnSiguiente.setText("Siguiente");
            }

            @Override
            public void onError(Exception e) {
                btnSiguiente.setEnabled(true);
                btnSiguiente.setText("Siguiente");
                Toast.makeText(SeleccionarSensoresActivity.this,
                        "Error al guardar la prueba", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private ArrayList<String> getNombresFases() {
        ArrayList<String> nombres = new ArrayList<>();
        for (com.upiiz.ble_sipi.Models.FasePrueba f : config.fases) nombres.add(f.nombre);
        return nombres;
    }

    private ArrayList<Integer> getDuracionesFases() {
        ArrayList<Integer> duraciones = new ArrayList<>();
        for (com.upiiz.ble_sipi.Models.FasePrueba f : config.fases) duraciones.add(f.duracionSegundos);
        return duraciones;
    }
}