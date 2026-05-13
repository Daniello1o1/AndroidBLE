package com.upiiz.ble_sipi.Views;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.upiiz.ble_sipi.Adapters.PacienteAdapter;
import com.upiiz.ble_sipi.Models.Paciente;
import com.upiiz.ble_sipi.R;
import com.upiiz.ble_sipi.Repository.PacienteRepository;

import java.util.List;

public class ListaPacientesActivity extends AppCompatActivity {

    private PacienteAdapter adapter;
    private PacienteRepository repository;
    private ProgressBar progressBar;
    private View layoutVacio;
    private TextInputEditText etBuscar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_lista_pacientes);

        repository = new PacienteRepository();

        bindViews();
        configurarRecycler();
        configurarBuscador();

        findViewById(R.id.btnNuevoPaciente).setOnClickListener(v ->
                startActivity(new Intent(this, RegistrarPacienteActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        cargarPacientes();
    }

    private void bindViews() {
        progressBar = findViewById(R.id.progressBar);
        layoutVacio = findViewById(R.id.layoutVacio);
        etBuscar    = findViewById(R.id.etBuscar);
    }

    private void configurarRecycler() {
        RecyclerView recycler = findViewById(R.id.recyclerPacientes);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PacienteAdapter(paciente -> {
            Intent intent = new Intent(this, DetallePacienteActivity.class);
            intent.putExtra("paciente", paciente);
            startActivity(intent);
        });
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

    private void cargarPacientes() {
        progressBar.setVisibility(View.VISIBLE);
        layoutVacio.setVisibility(View.GONE);

        repository.obtenerPacientes(new PacienteRepository.Callback<List<Paciente>>() {
            @Override
            public void onSuccess(List<Paciente> pacientes) {
                progressBar.setVisibility(View.GONE);
                adapter.setPacientes(pacientes);
                actualizarEstadoVacio();
            }

            @Override
            public void onError(Exception e) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(ListaPacientesActivity.this,
                        "Error al cargar pacientes", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void actualizarEstadoVacio() {
        layoutVacio.setVisibility(
                adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }
}