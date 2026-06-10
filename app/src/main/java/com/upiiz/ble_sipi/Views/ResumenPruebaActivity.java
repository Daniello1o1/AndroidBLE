package com.upiiz.ble_sipi.Views;


import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
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
        findViewById(R.id.btnFinalizarResumen).setOnClickListener(v -> {
            irAlHistorial();
        });

        getOnBackPressedDispatcher().addCallback(this,
                new OnBackPressedCallback(true) {
                    @Override
                    public void handleOnBackPressed() {
                        // No hacer nada
                        // Forzar al usuario a usar el botón
                    }
                });
    }

    private void irAlHistorial() {
        Intent intent = new Intent(this, HistorialActivity.class);
        intent.putExtra("prueba", config);
        // Limpiar el stack — quitar ConectarDispositivos, VerificarSenal, EjecutarPrueba
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        startActivity(intent);
        finish();
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
            List<Float> gx     = new ArrayList<>();
            List<Float> gy     = new ArrayList<>();
            List<Float> gz     = new ArrayList<>();
            List<Float> pitchL = new ArrayList<>();
            List<Float> rollL  = new ArrayList<>();
            List<Float> yawL   = new ArrayList<>();

            for (MuestraDato m : datos) {
                emg.add(m.emg);
                dynamo.add(m.dinamometro);
                gx.add(m.gyroX);
                gy.add(m.gyroY);
                gz.add(m.gyroZ);
                pitchL.add(m.pitch);
                rollL.add(m.roll);
                yawL.add(m.yaw);
            }

            double[] mags = emg.size() >= 1024
                    ? analyzer.computeMagnitudes(emg, 0) : null;

            MusculoAnalyzer.ResultadoAnalisis resultado =
                    MusculoAnalyzer.analizar(emg, dynamo, gx, gy, gz,
                            pitchL, rollL, yawL, mags);

            analisisPorFase.put(fase, resultado);
            float romMax = Math.max(
                    Float.isNaN(resultado.romPitch) ? 0 : resultado.romPitch,
                    Math.max(
                            Float.isNaN(resultado.romRoll) ? 0 : resultado.romRoll,
                            Float.isNaN(resultado.romYaw)  ? 0 : resultado.romYaw));

            metricasPorFase.put(fase, new float[]{
                    resultado.mav,
                    resultado.wl,
                    resultado.orderV,
                    resultado.dynMav,
                    resultado.rms,
                    resultado.frecuenciaMediana,
                    resultado.fuerzaMaxima,
                    romMax,
                    resultado.danielsEstimado});
        }

        // Global
        List<Float> emgTotal    = new ArrayList<>();
        List<Float> dynamoTotal = new ArrayList<>();
        List<Float> gxTotal     = new ArrayList<>();
        List<Float> gyTotal     = new ArrayList<>();
        List<Float> gzTotal     = new ArrayList<>();
        List<Float> pitchLTotal = new ArrayList<>();
        List<Float> rollLTotal  = new ArrayList<>();
        List<Float> yawLTotal   = new ArrayList<>();
        for (MuestraDato m : muestras) {
            emgTotal.add(m.emg);
            dynamoTotal.add(m.dinamometro);
            gxTotal.add(m.gyroX);
            gyTotal.add(m.gyroY);
            gzTotal.add(m.gyroZ);
            pitchLTotal.add(m.pitch);
            rollLTotal.add(m.roll);
            yawLTotal.add(m.yaw);
        }

        double[] magsGlobal = emgTotal.size() >= 1024
                ? analyzer.computeMagnitudes(emgTotal, 0) : null;
        analisisGlobal = MusculoAnalyzer.analizar(emgTotal, dynamoTotal, gxTotal, gyTotal, gzTotal, pitchLTotal, rollLTotal, yawLTotal, magsGlobal);

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
                                if (ejecucionIdGuardado != null) {
                                    // Ya tenemos el ID — guardar ahora
                                    actualizarDanielsEnFirestore(grado);
                                }
                                // Si no hay ID aún, se guardará en onSuccess de guardarEjecucionEnFirestore
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
        for (Map.Entry<String, MusculoAnalyzer.ResultadoAnalisis> entry
                : analisisPorFase.entrySet()) {
            String fase = entry.getKey();
            MusculoAnalyzer.ResultadoAnalisis a = entry.getValue();

            Map<String, Float> faseMapa = new HashMap<>();
            // EMG
            faseMapa.put("emgRMS",          a.rms);
            faseMapa.put("emgMAV",          a.mav);
            faseMapa.put("emgWL",           a.wl);
            faseMapa.put("emgFrecMediana",  a.frecuenciaMediana);
            faseMapa.put("emgIndiceFatiga", a.indiceFatigaEMG);
            // Dinamómetro
            faseMapa.put("dynFuerzaMax",    a.fuerzaMaxima);
            faseMapa.put("dynTiempoPico",   a.tiempoHastaPico);
            faseMapa.put("dynRFD",          a.rfd);
            faseMapa.put("dynImpulso",      a.impulso);
            // IMU
            faseMapa.put("romPitch",        a.romPitch);
            faseMapa.put("romRoll",         a.romRoll);
            faseMapa.put("romYaw",          a.romYaw);
            faseMapa.put("omegaMax",        a.velocidadAngularMaxima);
            faseMapa.put("omegaProm",       a.velocidadAngularPromedio);
            faseMapa.put("fatigaMecanica",  a.indiceFatigaMecanica);
            // Fusión
            faseMapa.put("eficMuscular",    a.eficienciaMuscular);
            faseMapa.put("eficMovimiento",  a.eficienciaMovimiento);
            faseMapa.put("onsetFuerza",     a.onsetEMGFuerza);
            faseMapa.put("onsetMovimiento", a.onsetEMGMovimiento);
            faseMapa.put("danielsEstimado", (float) a.danielsEstimado);

            metricasFaseFirestore.put(fase, faseMapa);
        }

        Ejecucion ejecucion = new Ejecucion();
        ejecucion.duracionReal    = config.duracionTotalSegundos;
        ejecucion.pacienteId      = config.pacienteId;
        ejecucion.totalMuestras   = muestras.size();
        ejecucion.fechaEjecucion  = System.currentTimeMillis();
        ejecucion.emgMAVTotal     = metricasGlobales[0];
        ejecucion.emgWLTotal      = metricasGlobales[1];
        ejecucion.emgOrderVTotal  = metricasGlobales[2];
        ejecucion.dynMAVTotal     = metricasGlobales[3];
        ejecucion.metricasPorFase = metricasFaseFirestore;

        // Análisis global
        if (analisisGlobal != null) {
            ejecucion.rms                    = analisisGlobal.rms;
            ejecucion.mav                    = analisisGlobal.mav;
            ejecucion.wl                     = analisisGlobal.wl;
            ejecucion.frecuenciaMediana      = analisisGlobal.frecuenciaMediana;
            ejecucion.indiceFatigaEMG        = analisisGlobal.indiceFatigaEMG;
            ejecucion.fuerzaMaxima           = analisisGlobal.fuerzaMaxima;
            ejecucion.tiempoHastaPico        = analisisGlobal.tiempoHastaPico;
            ejecucion.rfd                    = analisisGlobal.rfd;
            ejecucion.impulso                = analisisGlobal.impulso;
            ejecucion.romPitch               = analisisGlobal.romPitch;
            ejecucion.romRoll                = analisisGlobal.romRoll;
            ejecucion.romYaw                 = analisisGlobal.romYaw;
            ejecucion.velocidadAngularMaxima   = analisisGlobal.velocidadAngularMaxima;
            ejecucion.velocidadAngularPromedio = analisisGlobal.velocidadAngularPromedio;
            ejecucion.indiceFatigaMecanica   = analisisGlobal.indiceFatigaMecanica;
            ejecucion.eficienciaMuscular     = analisisGlobal.eficienciaMuscular;
            ejecucion.eficienciaMovimiento   = analisisGlobal.eficienciaMovimiento;
            ejecucion.onsetEMGFuerza         = analisisGlobal.onsetEMGFuerza;
            ejecucion.onsetEMGMovimiento     = analisisGlobal.onsetEMGMovimiento;
            ejecucion.danielsEstimado        = analisisGlobal.danielsEstimado;
            ejecucion.danielsAsignado        = -1;
            ejecucion.emgMAVTotal            = analisisGlobal.mav;
            ejecucion.emgWLTotal             = analisisGlobal.wl;
            ejecucion.emgOrderVTotal         = analisisGlobal.orderV;
            ejecucion.dynMAVTotal            = analisisGlobal.dynMav;
        }

        repository.guardarEjecucion(config.id, ejecucion,
                new PruebaRepository.Callback<String>() {
                    @Override
                    public void onSuccess(String id) {
                        ejecucionIdGuardado = id;
                        ejecucion.id = id;
                        for (MuestraDato m : muestras) m.ejecucionId = id;

                        // Si el médico ya asignó Daniels antes de que terminara de guardar
                        if (danielsAsignadoFinal >= 0) {
                            actualizarDanielsEnFirestore(danielsAsignadoFinal);
                        }

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