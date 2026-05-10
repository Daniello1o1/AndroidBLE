package com.upiiz.ble_sipi.Views;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.upiiz.ble_sipi.Adapters.EjecucionAdapter;
import com.upiiz.ble_sipi.R;
import com.upiiz.ble_sipi.Repository.PruebaRepository;
import com.upiiz.ble_sipi.Models.Ejecucion;
import com.upiiz.ble_sipi.Models.Prueba;

import java.util.List;

public class HistorialActivity extends AppCompatActivity {

    private Prueba prueba;
    private EjecucionAdapter adapter;
    private PruebaRepository repository;
    private ProgressBar progressBar;
    private View layoutVacio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_historial);

        prueba     = (Prueba) getIntent().getSerializableExtra("prueba");
        repository = new PruebaRepository();

        bindViews();
        configurarRecycler();
        cargarEjecuciones();
    }

    private void bindViews() {
        progressBar = findViewById(R.id.progressBar);
        layoutVacio = findViewById(R.id.layoutVacio);

        ((TextView) findViewById(R.id.tvTitulo)).setText(prueba.nombre);
        ((TextView) findViewById(R.id.tvSubtitulo)).setText(
                "Historial de ejecuciones");
    }

    private void configurarRecycler() {
        RecyclerView recycler = findViewById(R.id.recyclerEjecuciones);
        recycler.setLayoutManager(new LinearLayoutManager(this));

        adapter = new EjecucionAdapter(prueba, ejecucion -> {
            Intent intent = new Intent(this, ResumenHistorialActivity.class);
            intent.putExtra("prueba",    prueba);
            intent.putExtra("ejecucion", ejecucion);
            startActivity(intent);
        });

        recycler.setAdapter(adapter);
    }

    private void cargarEjecuciones() {
        progressBar.setVisibility(View.VISIBLE);
        layoutVacio.setVisibility(View.GONE);

        repository.obtenerEjecuciones(prueba.id,
                new PruebaRepository.Callback<List<Ejecucion>>() {
                    @Override
                    public void onSuccess(List<Ejecucion> ejecuciones) {
                        progressBar.setVisibility(View.GONE);
                        adapter.setEjecuciones(ejecuciones);
                        layoutVacio.setVisibility(
                                ejecuciones.isEmpty() ? View.VISIBLE : View.GONE);
                    }

                    @Override
                    public void onError(Exception e) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(HistorialActivity.this,
                                "Error al cargar historial", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}