package com.upiiz.ble_sipi.Views;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.upiiz.ble_sipi.Models.FasePrueba;
import com.upiiz.ble_sipi.Models.Prueba;
import com.upiiz.ble_sipi.R;

import java.util.ArrayList;

public class ConfigPruebaActivity extends AppCompatActivity {
    private TextInputEditText etNombrePrueba, etDuracionTotal;
    private SwitchMaterial switchFases;
    private LinearLayout layoutFases, containerFases;
    private MaterialButton btnAgregarFase, btnSiguiente;
    private TextView tvAdvertenciaFases;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_config_prueba);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etNombrePrueba      = findViewById(R.id.etNombrePrueba);
        etDuracionTotal     = findViewById(R.id.etDuracionTotal);
        switchFases         = findViewById(R.id.switchFases);
        layoutFases         = findViewById(R.id.layoutFases);
        containerFases      = findViewById(R.id.containerFases);
        btnAgregarFase      = findViewById(R.id.btnAgregarFase);
        btnSiguiente        = findViewById(R.id.btnSiguiente);
        tvAdvertenciaFases  = findViewById(R.id.tvAdvertenciaFases);

        // Mostrar/ocultar sección de fases
        switchFases.setOnCheckedChangeListener((btn, checked) -> {
            layoutFases.setVisibility(checked ? View.VISIBLE : View.GONE);
            if (checked && containerFases.getChildCount() == 0) {
                agregarFila(); // Agrega una fila vacía al activar
            }
        });

        btnAgregarFase.setOnClickListener(v -> agregarFila());

        btnSiguiente.setOnClickListener(v -> siguiente());

    }
    private void agregarFila() {
        View fila = LayoutInflater.from(this)
                .inflate(R.layout.item_fase, containerFases, false);

        // Escuchar cambios para recalcular advertencia
        EditText etDur = fila.findViewById(R.id.etDuracionFase);
        etDur.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) { verificarDuraciones(); }
        });

        // Botón eliminar fila
        fila.findViewById(R.id.btnEliminarFase).setOnClickListener(v -> {
            containerFases.removeView(fila);
            verificarDuraciones();
        });

        containerFases.addView(fila);
    }

    private void verificarDuraciones() {
        String durStr = etDuracionTotal.getText() != null
                ? etDuracionTotal.getText().toString() : "";

        if (durStr.isEmpty()) {
            tvAdvertenciaFases.setText("");
            return;
        }

        int durTotal = Integer.parseInt(durStr);
        int sumFases = getSumaFases();

        if (sumFases != durTotal) {
            tvAdvertenciaFases.setText(
                    "Las fases suman " + sumFases + "s, pero la duración total es " + durTotal + "s"
            );
        } else {
            tvAdvertenciaFases.setText("");
        }
    }

    private int getSumaFases() {
        int suma = 0;
        for (int i = 0; i < containerFases.getChildCount(); i++) {
            View fila = containerFases.getChildAt(i);
            EditText etDur = fila.findViewById(R.id.etDuracionFase);
            String val = etDur.getText() != null ? etDur.getText().toString() : "";
            if (!val.isEmpty()) suma += Integer.parseInt(val);
        }
        return suma;
    }

    private void siguiente() {
        // Validar nombre
        String nombre = etNombrePrueba.getText() != null
                ? etNombrePrueba.getText().toString().trim() : "";
        if (nombre.isEmpty()) {
            etNombrePrueba.setError("Escribe un nombre para la prueba");
            return;
        }

        // Validar duración
        String durStr = etDuracionTotal.getText() != null
                ? etDuracionTotal.getText().toString() : "";
        if (durStr.isEmpty()) {
            etDuracionTotal.setError("Escribe la duración total");
            return;
        }

        // Validar fases si el switch está activo
        if (switchFases.isChecked()) {
            if (containerFases.getChildCount() == 0) {
                tvAdvertenciaFases.setText("Agrega al menos una fase");
                return;
            }
            if (getSumaFases() != Integer.parseInt(durStr)) {
                tvAdvertenciaFases.setText("Las fases deben sumar exactamente la duración total");
                return;
            }
        }

        // Construir el objeto PruebaConfig
        Prueba config = new Prueba();
        config.nombre = nombre;
        config.duracionTotalSegundos = Integer.parseInt(durStr);
        config.tieneIntervalos = switchFases.isChecked();

        if (switchFases.isChecked()) {
            for (int i = 0; i < containerFases.getChildCount(); i++) {
                View fila = containerFases.getChildAt(i);
                EditText etNom = fila.findViewById(R.id.etNombreFase);
                EditText etDur = fila.findViewById(R.id.etDuracionFase);

                String nomFase = etNom.getText() != null
                        ? etNom.getText().toString().trim() : "";
                String durFase = etDur.getText() != null
                        ? etDur.getText().toString() : "";

                if (nomFase.isEmpty()) nomFase = "Fase " + (i + 1);

                config.fases.add(new FasePrueba(nomFase, Integer.parseInt(durFase)));
            }
        }

        // Pasar a la pantalla 2
        Intent intent = new Intent(this, SeleccionarSensoresActivity.class);
        intent.putExtra("config", config);

        startActivity(intent);
    }
}
