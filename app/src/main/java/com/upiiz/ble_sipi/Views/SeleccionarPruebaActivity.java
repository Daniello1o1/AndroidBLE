package com.upiiz.ble_sipi.Views;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.upiiz.ble_sipi.Adapters.PruebaAdapter;
import com.upiiz.ble_sipi.Models.Paciente;
import com.upiiz.ble_sipi.Models.Prueba;
import com.upiiz.ble_sipi.R;
import com.upiiz.ble_sipi.Repository.PruebaRepository;

import java.util.List;

public class SeleccionarPruebaActivity extends AppCompatActivity {

    private Paciente paciente;
    private PruebaAdapter adapter;
    private PruebaRepository repository;
    private ProgressBar progressBar;
    private View layoutVacio;
    private TextInputEditText etBuscar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_seleccionar_prueba);

        paciente   = (Paciente) getIntent().getSerializableExtra("paciente");
        repository = new PruebaRepository();

        bindViews();
        configurarRecycler();
        configurarBuscador();
        cargarPruebas();
    }

    private void bindViews() {
        progressBar = findViewById(R.id.progressBar);
        layoutVacio = findViewById(R.id.layoutVacio);
        etBuscar    = findViewById(R.id.etBuscar);

        // Mostrar nombre del paciente en el header
        ((TextView) findViewById(R.id.tvNombrePaciente))
                .setText("Paciente: " + paciente.getNombreCompleto());
    }

    private void configurarRecycler() {
        RecyclerView recycler = findViewById(R.id.recyclerPruebas);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        adapter = new PruebaAdapter(prueba -> seleccionarPrueba(prueba));
        recycler.setAdapter(adapter);
    }

    private void configurarBuscador() {
        etBuscar.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
            public void afterTextChanged(Editable s) {
                adapter.filtrar(s.toString());
                actualizarEstadoVacio();
            }
        });
    }

    private void cargarPruebas() {
        progressBar.setVisibility(View.VISIBLE);
        layoutVacio.setVisibility(View.GONE);

        repository.obtenerPruebas(new PruebaRepository.Callback<List<Prueba>>() {
            @Override
            public void onSuccess(List<Prueba> pruebas) {
                progressBar.setVisibility(View.GONE);
                adapter.setPruebas(pruebas);
                actualizarEstadoVacio();
            }

            @Override
            public void onError(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(SeleccionarPruebaActivity.this,
                        "Error al cargar pruebas", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void seleccionarPrueba(Prueba prueba) {
        // Asignar el paciente a esta ejecución
        prueba.pacienteId = paciente.id;

        // Ir directo a conectar dispositivos
        Intent intent = new Intent(this, ConectarDispositivosActivity.class);
        intent.putExtra("config", prueba);
        intent.putExtra("paciente", paciente);
        startActivity(intent);
    }

    private void actualizarEstadoVacio() {
        layoutVacio.setVisibility(
                adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }
}