package com.upiiz.ble_sipi.Views;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.upiiz.ble_sipi.Models.Ejecucion;
import com.upiiz.ble_sipi.Models.MuestraDato;
import com.upiiz.ble_sipi.Models.Prueba;
import com.upiiz.ble_sipi.R;
import com.upiiz.ble_sipi.Repository.PruebaRepository;
import com.upiiz.ble_sipi.Tools.EMGFrequencyAnalyzer;
import com.upiiz.ble_sipi.Tools.MuestrasCache;
import com.upiiz.ble_sipi.Tools.MusculoAnalyzer;
import com.upiiz.ble_sipi.Tools.ReporteGenerator;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ResumenPruebaActivity extends AppCompatActivity {

    private Prueba config;
    private ArrayList<MuestraDato> muestras;
    private Map<String, List<MuestraDato>> muestrasPorFase;
    private Map<String, float[]> metricasPorFase;
    private Map<String, MusculoAnalyzer.ResultadoAnalisis> analisisPorFase;
    private MusculoAnalyzer.ResultadoAnalisis analisisGlobal;
    private float[] metricasGlobales;

    private PruebaRepository repository;
    private boolean yaGuardado = false;
    private String ejecucionIdGuardado = null;
    private int danielsAsignadoFinal = -1;

    private static final int SAMPLE_RATE = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resumen_prueba);

        config   = (Prueba) getIntent().getSerializableExtra("config");
        muestras = new ArrayList<>(MuestrasCache.obtener());
        MuestrasCache.limpiar();

        repository = new PruebaRepository();

        ((TextView) findViewById(R.id.tvNombrePrueba)).setText(config.nombre);
        ((TextView) findViewById(R.id.tvInfoPrueba)).setText(
                config.duracionTotalSegundos + "s   ·   " + muestras.size() + " muestras");

        calcularTodo();
        configurarTabs();
        guardarEjecucionEnFirestore();

        findViewById(R.id.btnExportarPDF).setOnClickListener(v -> exportarPDF());
    }

    // ================= CALCULAR TODO =================

    private void calcularTodo() {
        muestrasPorFase = ReporteGenerator.agruparPorFase(muestras);
        metricasPorFase = new LinkedHashMap<>();
        analisisPorFase = new LinkedHashMap<>();

        EMGFrequencyAnalyzer analyzer = new EMGFrequencyAnalyzer(1024, SAMPLE_RATE);

        for (Map.Entry<String, List<MuestraDato>> entry : muestrasPorFase.entrySet()) {
            String fase             = entry.getKey();
            List<MuestraDato> datos = entry.getValue();

            List<Float> emg    = new ArrayList<>();
            List<Float> dynamo = new ArrayList<>();
            for (MuestraDato m : datos) {
                emg.add(m.emg);
                dynamo.add(m.dinamometro);
            }

            // Un solo análisis completo por fase
            double[] mags = emg.size() >= 1024
                    ? analyzer.computeMagnitudes(emg, 0) : null;
            MusculoAnalyzer.ResultadoAnalisis resultado =
                    MusculoAnalyzer.analizar(emg, dynamo, mags);

            analisisPorFase.put(fase, resultado);

            // Métricas básicas tomadas del mismo resultado
            metricasPorFase.put(fase, new float[]{
                    resultado.mav,
                    resultado.wl,
                    resultado.orderV,
                    resultado.dynMav
            });
        }

        // Global
        List<Float> emgTotal    = new ArrayList<>();
        List<Float> dynamoTotal = new ArrayList<>();
        for (MuestraDato m : muestras) {
            emgTotal.add(m.emg);
            dynamoTotal.add(m.dinamometro);
        }

        double[] magsGlobal = emgTotal.size() >= 1024
                ? analyzer.computeMagnitudes(emgTotal, 0) : null;
        analisisGlobal = MusculoAnalyzer.analizar(emgTotal, dynamoTotal, magsGlobal);

        metricasGlobales = new float[]{
                analisisGlobal.mav,
                analisisGlobal.wl,
                analisisGlobal.orderV,
                analisisGlobal.dynMav
        };
    }

    // ================= TABS =================

    private void configurarTabs() {
        List<String> fases = new ArrayList<>(muestrasPorFase.keySet());

        ViewPager2 viewPager = findViewById(R.id.viewPager);
        TabLayout tabLayout  = findViewById(R.id.tabLayout);

        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @Override
            public int getItemCount() {
                return 1 + fases.size(); // General + una por fase
            }

            @Override
            public Fragment createFragment(int position) {
                if (position == 0) {
                    return ResumenGeneralFragment.newInstance(
                            analisisGlobal,
                            metricasGlobales,
                            grado -> {
                                danielsAsignadoFinal = grado;
                                actualizarDanielsEnFirestore(grado);
                            });
                } else {
                    String fase = fases.get(position - 1);
                    List<Float> emgFase = new ArrayList<>();
                    List<MuestraDato> datos = muestrasPorFase.get(fase);
                    if (datos != null) {
                        for (MuestraDato m : datos) emgFase.add(m.emg);
                    }
                    return ResumenFaseFragment.newInstance(
                            metricasPorFase.get(fase),
                            analisisPorFase.get(fase),
                            emgFase);
                }
            }
        });

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) -> {
            tab.setText(position == 0 ? "General" : fases.get(position - 1));
        }).attach();
    }

    // ================= GUARDAR FIRESTORE =================

    private void guardarEjecucionEnFirestore() {
        if (yaGuardado) return;
        yaGuardado = true;

        List<Float> emgTotal    = new ArrayList<>();
        List<Float> dynamoTotal = new ArrayList<>();
        for (MuestraDato m : muestras) {
            emgTotal.add(m.emg);
            dynamoTotal.add(m.dinamometro);
        }

        Map<String, Map<String, Float>> metricasFaseFirestore = new LinkedHashMap<>();
        for (Map.Entry<String, float[]> entry : metricasPorFase.entrySet()) {
            float[] vals = entry.getValue();
            Map<String, Float> faseMapa = new HashMap<>();
            faseMapa.put("emgMAV",    vals[0]);
            faseMapa.put("emgWL",     vals[1]);
            faseMapa.put("emgOrderV", vals[2]);
            faseMapa.put("dynMAV",    vals[3]);
            metricasFaseFirestore.put(entry.getKey(), faseMapa);
        }

        Ejecucion ejecucion = new Ejecucion();
        ejecucion.duracionReal    = config.duracionTotalSegundos;
        ejecucion.pacienteId      = config.pacienteId;
        ejecucion.totalMuestras   = muestras.size();
        ejecucion.emgMAVTotal     = metricasGlobales[0];
        ejecucion.emgWLTotal      = metricasGlobales[1];
        ejecucion.emgOrderVTotal  = metricasGlobales[2];
        ejecucion.dynMAVTotal     = metricasGlobales[3];
        ejecucion.metricasPorFase = metricasFaseFirestore;

        // Análisis global
        if (analisisGlobal != null) {
            ejecucion.rms                = analisisGlobal.rms;
            ejecucion.var                = analisisGlobal.var;
            ejecucion.zc                 = analisisGlobal.zc;
            ejecucion.ssc                = analisisGlobal.ssc;
            ejecucion.frecuenciaMediana  = analisisGlobal.frecuenciaMediana;
            ejecucion.frecuenciaMedia    = analisisGlobal.frecuenciaMedia;
            ejecucion.potenciaTotal      = analisisGlobal.potenciaTotal;
            ejecucion.ratioBandas        = analisisGlobal.ratioBandas;
            ejecucion.indiceFatiga       = analisisGlobal.indiceFatiga;
            ejecucion.tasaDecaimientoRMS = analisisGlobal.tasaDecaimientoRMS;
            ejecucion.fuerzaMaxima       = analisisGlobal.fuerzaMaxima;
            ejecucion.fuerzaMinima       = analisisGlobal.fuerzaMinima;
            ejecucion.tiempoHastaPico    = analisisGlobal.tiempoHastaPico;
            ejecucion.rfd                = analisisGlobal.rfd;
            ejecucion.impulso            = analisisGlobal.impulso;
            ejecucion.coeficienteVariacion = analisisGlobal.coeficienteVariacion;
            ejecucion.eficienciaMusular  = analisisGlobal.eficienciaMusular;
            ejecucion.onsetMusular       = analisisGlobal.onsetMusular;
            ejecucion.danielsEstimado    = analisisGlobal.danielsEstimado;
            ejecucion.danielsAsignado    = -1;
        }

        repository.guardarEjecucion(config.id, ejecucion,
                new PruebaRepository.Callback<String>() {
                    @Override
                    public void onSuccess(String id) {
                        ejecucionIdGuardado = id;
                        ejecucion.id = id;
                        for (MuestraDato m : muestras) m.ejecucionId = id;

                        try {
                            ReporteGenerator.guardarCSVLocal(
                                    ResumenPruebaActivity.this,
                                    config.id, id, muestras);

                            // Subir a Firebase Storage en background
                            ReporteGenerator.subirCSVAStorage(
                                    ResumenPruebaActivity.this,
                                    config.id,
                                    id,
                                    new ReporteGenerator.OnSubidaListener() {
                                        @Override
                                        public void onExito() {
                                            android.util.Log.d("STORAGE", "CSV subido exitosamente");
                                        }
                                        @Override
                                        public void onError(Exception e) {
                                            android.util.Log.e("STORAGE",
                                                    "Error subiendo CSV: " + e.getMessage());
                                            // No mostrar error al usuario — el CSV local sigue disponible
                                        }
                                        @Override
                                        public void onProgreso(int porcentaje) {
                                            android.util.Log.d("STORAGE", "Subida: " + porcentaje + "%");
                                        }
                                    });

                        } catch (Exception e) {
                            android.util.Log.e("RESUMEN", "Error CSV local: " + e.getMessage());
                        }
                    }
                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(ResumenPruebaActivity.this,
                                "No se pudo guardar en Firestore",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void actualizarDanielsEnFirestore(int grado) {
        if (ejecucionIdGuardado == null) return;
        Map<String, Object> update = new HashMap<>();
        update.put("danielsAsignado", grado);
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("pruebas").document(config.id)
                .collection("ejecuciones").document(ejecucionIdGuardado)
                .update(update)
                .addOnSuccessListener(v -> Toast.makeText(this,
                        "Grado Daniels " + grado + " guardado", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e -> Toast.makeText(this,
                        "Error al guardar Daniels", Toast.LENGTH_SHORT).show());
    }

    // ================= EXPORTAR =================

    private void exportarPDF() {
        try {
            File archivo = ReporteGenerator.exportarPDF(
                    this, config, muestras, metricasPorFase);
            compartirArchivo(archivo, "application/pdf");
        } catch (Exception e) {
            Toast.makeText(this, "Error PDF: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void compartirArchivo(File archivo, String mimeType) {
        Uri uri = FileProvider.getUriForFile(
                this, getPackageName() + ".provider", archivo);
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Compartir reporte"));
    }
}