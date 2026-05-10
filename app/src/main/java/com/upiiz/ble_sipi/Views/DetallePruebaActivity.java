package com.upiiz.ble_sipi.Views;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
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
import com.upiiz.ble_sipi.Repository.PruebaRepository;

public class DetallePruebaActivity extends AppCompatActivity {

    private Prueba prueba;
    private PruebaRepository repository;
    private boolean modoEdicion = false;

    // Campos
    private TextInputEditText etNombre, etDuracion;
    private SwitchMaterial switchFases;
    private LinearLayout layoutFases, containerFases;
    private TextView tvAdvertenciaFases;
    private CheckBox cbEMG, cbDinamometro, cbAcelerometro, cbGiroscopio, cbOrientacion;

    // Botones
    private MaterialButton btnEditar, btnGuardarCambios, btnBorrar;
    private MaterialButton btnEjecutar, btnHistorial, btnAgregarFase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_detalle_prueba);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        prueba     = (Prueba) getIntent().getSerializableExtra("prueba");
        repository = new PruebaRepository();

        bindViews();
        cargarDatos();
        configurarBotones();
    }
    private void bindViews() {
        etNombre            = findViewById(R.id.etNombre);
        etDuracion          = findViewById(R.id.etDuracion);
        switchFases         = findViewById(R.id.switchFases);
        layoutFases         = findViewById(R.id.layoutFases);
        containerFases      = findViewById(R.id.containerFases);
        tvAdvertenciaFases  = findViewById(R.id.tvAdvertenciaFases);
        cbEMG               = findViewById(R.id.cbEMG);
        cbDinamometro       = findViewById(R.id.cbDinamometro);
        cbAcelerometro      = findViewById(R.id.cbAcelerometro);
        cbGiroscopio        = findViewById(R.id.cbGiroscopio);
        cbOrientacion       = findViewById(R.id.cbOrientacion);
        btnEditar           = findViewById(R.id.btnEditar);
        btnGuardarCambios   = findViewById(R.id.btnGuardarCambios);
        btnBorrar           = findViewById(R.id.btnBorrar);
        btnEjecutar         = findViewById(R.id.btnEjecutar);
        btnHistorial        = findViewById(R.id.btnHistorial);
        btnAgregarFase      = findViewById(R.id.btnAgregarFase);
    }

    // ================= CARGAR DATOS =================

    private void cargarDatos() {
        etNombre.setText(prueba.nombre);
        etDuracion.setText(String.valueOf(prueba.duracionTotalSegundos));
        switchFases.setChecked(prueba.tieneIntervalos);

        cbEMG.setChecked(prueba.usarEMG);
        cbDinamometro.setChecked(prueba.usarDinamometro);
        cbAcelerometro.setChecked(prueba.usarAcelerometro);
        cbGiroscopio.setChecked(prueba.usarGiroscopio);
        cbOrientacion.setChecked(prueba.usarOrientacion);

        if (prueba.tieneIntervalos && !prueba.fases.isEmpty()) {
            layoutFases.setVisibility(View.VISIBLE);
            containerFases.removeAllViews();
            for (FasePrueba f : prueba.fases) {
                agregarFilaFase(f.nombre, f.duracionSegundos);
            }
        }
    }

    // ================= BOTONES =================

    private void configurarBotones() {

        switchFases.setOnCheckedChangeListener((btn, checked) -> {
            if (!modoEdicion) return;
            layoutFases.setVisibility(checked ? View.VISIBLE : View.GONE);
            if (checked && containerFases.getChildCount() == 0) agregarFilaFase("", 0);
        });

        btnAgregarFase.setOnClickListener(v -> agregarFilaFase("", 0));

        btnEditar.setOnClickListener(v -> activarModoEdicion());

        btnGuardarCambios.setOnClickListener(v -> guardarCambios());

        btnBorrar.setOnClickListener(v -> confirmarBorrado());

        btnEjecutar.setOnClickListener(v -> {
            Intent intent = new Intent(this, ConectarDispositivosActivity.class);
            intent.putExtra("config", prueba);
            startActivity(intent);
        });

        btnHistorial.setOnClickListener(v -> {
            Intent intent = new Intent(this, HistorialActivity.class);
            intent.putExtra("prueba", prueba);
            startActivity(intent);
        });
    }

    // ================= MODO EDICIÓN =================

    private void activarModoEdicion() {
        modoEdicion = true;

        // Habilitar campos
        etNombre.setEnabled(true);
        etDuracion.setEnabled(true);
        switchFases.setEnabled(true);
        cbEMG.setEnabled(true);
        cbDinamometro.setEnabled(true);
        cbAcelerometro.setEnabled(true);
        cbGiroscopio.setEnabled(true);
        cbOrientacion.setEnabled(true);
        btnAgregarFase.setVisibility(
                switchFases.isChecked() ? View.VISIBLE : View.GONE);

        // Habilitar botón eliminar en filas de fase
        for (int i = 0; i < containerFases.getChildCount(); i++) {
            containerFases.getChildAt(i)
                    .findViewById(R.id.btnEliminarFase)
                    .setVisibility(View.VISIBLE);
        }

        btnEditar.setVisibility(View.GONE);
        btnGuardarCambios.setVisibility(View.VISIBLE);
    }

    // ================= GUARDAR =================

    private void guardarCambios() {
        String nombre  = etNombre.getText() != null
                ? etNombre.getText().toString().trim() : "";
        String durStr  = etDuracion.getText() != null
                ? etDuracion.getText().toString() : "";

        if (nombre.isEmpty()) {
            etNombre.setError("Escribe un nombre");
            return;
        }
        if (durStr.isEmpty()) {
            etDuracion.setError("Escribe la duración");
            return;
        }

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

        // Actualizar objeto
        prueba.nombre                = nombre;
        prueba.duracionTotalSegundos = Integer.parseInt(durStr);
        prueba.tieneIntervalos       = switchFases.isChecked();
        prueba.usarEMG               = cbEMG.isChecked();
        prueba.usarDinamometro       = cbDinamometro.isChecked();
        prueba.usarAcelerometro      = cbAcelerometro.isChecked();
        prueba.usarGiroscopio        = cbGiroscopio.isChecked();
        prueba.usarOrientacion       = cbOrientacion.isChecked();

        prueba.fases.clear();
        if (switchFases.isChecked()) {
            for (int i = 0; i < containerFases.getChildCount(); i++) {
                View fila   = containerFases.getChildAt(i);
                EditText etN = fila.findViewById(R.id.etNombreFase);
                EditText etD = fila.findViewById(R.id.etDuracionFase);
                String nom  = etN.getText() != null
                        ? etN.getText().toString().trim() : "Fase " + (i + 1);
                String dur  = etD.getText() != null
                        ? etD.getText().toString() : "0";
                prueba.fases.add(new FasePrueba(nom, Integer.parseInt(dur)));
            }
        }

        btnGuardarCambios.setEnabled(false);
        btnGuardarCambios.setText("Guardando...");

        repository.actualizarPrueba(prueba, new PruebaRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                desactivarModoEdicion();
                btnGuardarCambios.setEnabled(true);
                btnGuardarCambios.setText("Guardar cambios");
                Toast.makeText(DetallePruebaActivity.this,
                        "Prueba actualizada", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(Exception e) {
                btnGuardarCambios.setEnabled(true);
                btnGuardarCambios.setText("Guardar cambios");
                Toast.makeText(DetallePruebaActivity.this,
                        "Error al guardar", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void desactivarModoEdicion() {
        modoEdicion = false;

        etNombre.setEnabled(false);
        etDuracion.setEnabled(false);
        switchFases.setEnabled(false);
        cbEMG.setEnabled(false);
        cbDinamometro.setEnabled(false);
        cbAcelerometro.setEnabled(false);
        cbGiroscopio.setEnabled(false);
        cbOrientacion.setEnabled(false);
        btnAgregarFase.setVisibility(View.GONE);

        for (int i = 0; i < containerFases.getChildCount(); i++) {
            containerFases.getChildAt(i)
                    .findViewById(R.id.btnEliminarFase)
                    .setVisibility(View.GONE);
        }

        btnEditar.setVisibility(View.VISIBLE);
        btnGuardarCambios.setVisibility(View.GONE);
        tvAdvertenciaFases.setText("");
    }

    // ================= BORRAR =================

    private void confirmarBorrado() {
        new AlertDialog.Builder(this)
                .setTitle("Borrar prueba")
                .setMessage("¿Estás seguro? Esta acción no se puede deshacer.")
                .setPositiveButton("Borrar", (dialog, which) -> borrarPrueba())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void borrarPrueba() {
        btnBorrar.setEnabled(false);

        repository.borrarPrueba(prueba.id, new PruebaRepository.Callback<Void>() {
            @Override
            public void onSuccess(Void result) {
                Toast.makeText(DetallePruebaActivity.this,
                        "Prueba eliminada", Toast.LENGTH_SHORT).show();
                finish(); // Regresa a ListaPruebasActivity
            }

            @Override
            public void onError(Exception e) {
                btnBorrar.setEnabled(true);
                Toast.makeText(DetallePruebaActivity.this,
                        "Error al borrar", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // ================= FASES =================

    private void agregarFilaFase(String nombre, int duracion) {
        View fila = LayoutInflater.from(this)
                .inflate(R.layout.item_fase, containerFases, false);

        EditText etN = fila.findViewById(R.id.etNombreFase);
        EditText etD = fila.findViewById(R.id.etDuracionFase);

        etN.setText(nombre);
        if (duracion > 0) etD.setText(String.valueOf(duracion));

        // En modo ver los campos están deshabilitados
        etN.setEnabled(modoEdicion);
        etD.setEnabled(modoEdicion);

        View btnEliminar = fila.findViewById(R.id.btnEliminarFase);
        btnEliminar.setVisibility(modoEdicion ? View.VISIBLE : View.GONE);
        btnEliminar.setOnClickListener(v -> {
            containerFases.removeView(fila);
            verificarDuraciones();
        });

        etD.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) { verificarDuraciones(); }
        });

        containerFases.addView(fila);
        layoutFases.setVisibility(View.VISIBLE);
    }

    private void verificarDuraciones() {
        String durStr = etDuracion.getText() != null
                ? etDuracion.getText().toString() : "";
        if (durStr.isEmpty()) return;

        int durTotal = Integer.parseInt(durStr);
        int sumFases = getSumaFases();

        tvAdvertenciaFases.setText(sumFases != durTotal
                ? "Las fases suman " + sumFases + "s de " + durTotal + "s"
                : "");
    }

    private int getSumaFases() {
        int suma = 0;
        for (int i = 0; i < containerFases.getChildCount(); i++) {
            EditText etD = containerFases.getChildAt(i)
                    .findViewById(R.id.etDuracionFase);
            String val = etD.getText() != null ? etD.getText().toString() : "";
            if (!val.isEmpty()) suma += Integer.parseInt(val);
        }
        return suma;
    }
}