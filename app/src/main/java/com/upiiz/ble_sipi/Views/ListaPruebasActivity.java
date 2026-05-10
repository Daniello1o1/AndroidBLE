package com.upiiz.ble_sipi.Views;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.upiiz.ble_sipi.Adapters.PruebaAdapter;
import com.upiiz.ble_sipi.Models.Prueba;
import com.upiiz.ble_sipi.R;
import com.upiiz.ble_sipi.Repository.PruebaRepository;

import java.util.List;

public class ListaPruebasActivity extends AppCompatActivity {

    private PruebaAdapter adapter;
    private PruebaRepository repository;
    private ProgressBar progressBar;
    private View layoutVacio;
    private TextInputEditText etBuscar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_lista_pruebas);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        repository = new PruebaRepository();

        bindViews();
        configurarRecycler();
        configurarBuscador();

        findViewById(R.id.btnNuevaPrueba).setOnClickListener(v ->
                startActivity(new Intent(this, ConfigPruebaActivity.class)));
    }
    @Override
    protected void onResume() {
        super.onResume();
        // Recargar cada vez que regresamos a esta pantalla
        cargarPruebas();
    }
    private void bindViews() {
        progressBar  = findViewById(R.id.progressBar);
        layoutVacio  = findViewById(R.id.layoutVacio);
        etBuscar     = findViewById(R.id.etBuscar);
    }

    private void configurarRecycler() {
        RecyclerView recycler = findViewById(R.id.recyclerPruebas);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PruebaAdapter(prueba -> {
            Intent intent = new Intent(this, DetallePruebaActivity.class);
            intent.putExtra("prueba", prueba);
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
                Toast.makeText(ListaPruebasActivity.this,
                        "Error al cargar pruebas", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void actualizarEstadoVacio() {
        layoutVacio.setVisibility(
                adapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
    }
}