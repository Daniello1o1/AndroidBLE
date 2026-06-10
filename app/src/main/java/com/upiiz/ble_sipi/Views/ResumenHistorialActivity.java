package com.upiiz.ble_sipi.Views;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.upiiz.ble_sipi.Models.Ejecucion;
import com.upiiz.ble_sipi.Models.MuestraDato;
import com.upiiz.ble_sipi.Models.Prueba;
import com.upiiz.ble_sipi.R;
import com.upiiz.ble_sipi.Tools.EMGFrequencyAnalyzer;
import com.upiiz.ble_sipi.Tools.MusculoAnalyzer;
import com.upiiz.ble_sipi.Tools.ReporteGenerator;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ResumenHistorialActivity extends AppCompatActivity {

    private Prueba prueba;
    private Ejecucion ejecucion;
    private boolean tieneCSVLocal;

    // Resultados calculados desde CSV local
    private Map<String, float[]> metricasPorFase;
    private Map<String, MusculoAnalyzer.ResultadoAnalisis> analisisPorFase;
    private MusculoAnalyzer.ResultadoAnalisis analisisGlobal;
    private float[] metricasGlobales;
    private List<String> fases = new ArrayList<>();

    private static final int SAMPLE_RATE = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resumen_prueba);

        prueba    = (Prueba)    getIntent().getSerializableExtra("prueba");
        ejecucion = (Ejecucion) getIntent().getSerializableExtra("ejecucion");

        tieneCSVLocal = ReporteGenerator.existeCSVLocal(
                this, prueba.id, ejecucion.id);

        configurarHeader();

        if (tieneCSVLocal) {
            calcularDesdeCSV();
            configurarTabs();
        } else {
            descargarCSVSiDisponible();
            calcularDesdeFirestore();
            configurarTabs();
        }


        MaterialButton btnPDF = findViewById(R.id.btnExportarPDF);
        if (btnPDF != null) btnPDF.setOnClickListener(v -> exportarPDFHistorial());
    }

    // ===== CALCULAR =====

    private void calcularDesdeCSV() {
        List<MuestraDato> muestras = ReporteGenerator.leerMuestrasCompletas(
                this, prueba.id, ejecucion.id);
        if (muestras == null || muestras.isEmpty()) {
            calcularDesdeFirestore();
            return;
        }

        metricasPorFase = new LinkedHashMap<>();
        analisisPorFase = new LinkedHashMap<>();

        Map<String, List<MuestraDato>> muestrasPorFase =
                ReporteGenerator.agruparPorFase(muestras);
        EMGFrequencyAnalyzer analyzer = new EMGFrequencyAnalyzer(1024, SAMPLE_RATE);

        for (Map.Entry<String, List<MuestraDato>> entry : muestrasPorFase.entrySet()) {
            String fase             = entry.getKey();
            List<MuestraDato> datos = entry.getValue();
            fases.add(fase);

            List<Float> emg    = new ArrayList<>();
            List<Float> dynamo = new ArrayList<>();
            List<Float> gx     = new ArrayList<>();
            List<Float> gy     = new ArrayList<>();
            List<Float> gz     = new ArrayList<>();
            List<Float> pitch  = new ArrayList<>();
            List<Float> roll   = new ArrayList<>();
            List<Float> yaw    = new ArrayList<>();

            for (MuestraDato m : datos) {
                emg.add(m.emg);    dynamo.add(m.dinamometro);
                gx.add(m.gyroX);   gy.add(m.gyroY);   gz.add(m.gyroZ);
                pitch.add(m.pitch); roll.add(m.roll);   yaw.add(m.yaw);
            }

            double[] mags = emg.size() >= 1024
                    ? analyzer.computeMagnitudes(emg, 0) : null;

            MusculoAnalyzer.ResultadoAnalisis r =
                    MusculoAnalyzer.analizar(emg, dynamo, gx, gy, gz,
                            pitch, roll, yaw, mags);

            analisisPorFase.put(fase, r);
            metricasPorFase.put(fase, new float[]{
                    r.mav, r.wl, r.orderV, r.dynMav,
                    r.rms, r.frecuenciaMediana, r.fuerzaMaxima,
                    calcularROMMax(r), r.danielsEstimado});
        }

        // Global
        List<Float> emgTotal = new ArrayList<>();
        List<Float> dynTotal = new ArrayList<>();
        List<Float> gxT = new ArrayList<>(), gyT = new ArrayList<>(), gzT = new ArrayList<>();
        List<Float> pT  = new ArrayList<>(), rT  = new ArrayList<>(), yT  = new ArrayList<>();

        for (MuestraDato m : muestras) {
            emgTotal.add(m.emg);    dynTotal.add(m.dinamometro);
            gxT.add(m.gyroX);       gyT.add(m.gyroY);      gzT.add(m.gyroZ);
            pT.add(m.pitch);        rT.add(m.roll);         yT.add(m.yaw);
        }

        double[] magsG = emgTotal.size() >= 1024
                ? analyzer.computeMagnitudes(emgTotal, 0) : null;
        analisisGlobal = MusculoAnalyzer.analizar(
                emgTotal, dynTotal, gxT, gyT, gzT, pT, rT, yT, magsG);
        metricasGlobales = new float[]{
                analisisGlobal.mav, analisisGlobal.wl,
                analisisGlobal.orderV, analisisGlobal.dynMav,
                analisisGlobal.rms, analisisGlobal.frecuenciaMediana,
                analisisGlobal.fuerzaMaxima,
                calcularROMMax(analisisGlobal), analisisGlobal.danielsEstimado};
    }

    private void calcularDesdeFirestore() {
        // Sin CSV — usar métricas de Firestore para mostrar lo que hay
        fases.clear();
        metricasPorFase  = new LinkedHashMap<>();
        analisisPorFase  = new LinkedHashMap<>();

        if (ejecucion.metricasPorFase != null) {
            for (Map.Entry<String, Map<String, Float>> entry
                    : ejecucion.metricasPorFase.entrySet()) {
                String fase          = entry.getKey();
                Map<String, Float> m = entry.getValue();
                fases.add(fase);

                // Crear ResultadoAnalisis parcial desde Firestore
                MusculoAnalyzer.ResultadoAnalisis r =
                        new MusculoAnalyzer.ResultadoAnalisis();
                r.rms              = getFloat(m, "emgRMS");
                r.mav              = getFloat(m, "emgMAV");
                r.wl               = getFloat(m, "emgWL");
                r.frecuenciaMediana = getFloat(m, "emgFrecMediana");
                r.indiceFatigaEMG  = getFloat(m, "emgIndiceFatiga");
                r.fuerzaMaxima     = getFloat(m, "dynFuerzaMax");
                r.tiempoHastaPico  = getFloat(m, "dynTiempoPico");
                r.rfd              = getFloat(m, "dynRFD");
                r.impulso          = getFloat(m, "dynImpulso");
                r.romPitch         = getFloat(m, "romPitch");
                r.romRoll          = getFloat(m, "romRoll");
                r.romYaw           = getFloat(m, "romYaw");
                r.velocidadAngularMaxima   = getFloat(m, "omegaMax");
                r.velocidadAngularPromedio = getFloat(m, "omegaProm");
                r.indiceFatigaMecanica     = getFloat(m, "fatigaMecanica");
                r.eficienciaMuscular  = getFloat(m, "eficMuscular");
                r.eficienciaMovimiento = getFloat(m, "eficMovimiento");
                r.onsetEMGFuerza      = getFloat(m, "onsetFuerza");
                r.onsetEMGMovimiento  = getFloat(m, "onsetMovimiento");
                r.danielsEstimado     = (int) getFloat(m, "danielsEstimado");
                r.dynMav = r.mav;
                r.orderV = Float.NaN;

                analisisPorFase.put(fase, r);
                metricasPorFase.put(fase, new float[]{
                        r.mav, r.wl, r.orderV, r.dynMav,
                        r.rms, r.frecuenciaMediana, r.fuerzaMaxima,
                        calcularROMMax(r), r.danielsEstimado});
            }
        }

        // Global desde Firestore
        analisisGlobal = new MusculoAnalyzer.ResultadoAnalisis();
        analisisGlobal.rms               = ejecucion.rms;
        analisisGlobal.mav               = ejecucion.mav;
        analisisGlobal.wl                = ejecucion.wl;
        analisisGlobal.frecuenciaMediana = ejecucion.frecuenciaMediana;
        analisisGlobal.indiceFatigaEMG   = ejecucion.indiceFatigaEMG;
        analisisGlobal.fuerzaMaxima      = ejecucion.fuerzaMaxima;
        analisisGlobal.tiempoHastaPico   = ejecucion.tiempoHastaPico;
        analisisGlobal.rfd               = ejecucion.rfd;
        analisisGlobal.impulso           = ejecucion.impulso;
        analisisGlobal.romPitch          = ejecucion.romPitch;
        analisisGlobal.romRoll           = ejecucion.romRoll;
        analisisGlobal.romYaw            = ejecucion.romYaw;
        analisisGlobal.velocidadAngularMaxima   = ejecucion.velocidadAngularMaxima;
        analisisGlobal.velocidadAngularPromedio = ejecucion.velocidadAngularPromedio;
        analisisGlobal.indiceFatigaMecanica     = ejecucion.indiceFatigaMecanica;
        analisisGlobal.eficienciaMuscular  = ejecucion.eficienciaMuscular;
        analisisGlobal.eficienciaMovimiento = ejecucion.eficienciaMovimiento;
        analisisGlobal.onsetEMGFuerza      = ejecucion.onsetEMGFuerza;
        analisisGlobal.onsetEMGMovimiento  = ejecucion.onsetEMGMovimiento;
        analisisGlobal.danielsEstimado     = ejecucion.danielsEstimado;
        analisisGlobal.danielsAsignado     = ejecucion.danielsAsignado;
        analisisGlobal.dynMav              = ejecucion.dynMAVTotal;

        metricasGlobales = new float[]{
                analisisGlobal.mav, analisisGlobal.wl,
                Float.NaN, analisisGlobal.dynMav,
                analisisGlobal.rms, analisisGlobal.frecuenciaMediana,
                analisisGlobal.fuerzaMaxima,
                calcularROMMax(analisisGlobal), analisisGlobal.danielsEstimado};
    }

    // ===== TABS =====

    private void configurarTabs() {
        ViewPager2 viewPager = findViewById(R.id.viewPager);
        TabLayout tabLayout  = findViewById(R.id.tabLayout);

        viewPager.setAdapter(new FragmentStateAdapter(this) {
            @Override
            public int getItemCount() {
                return 1 + fases.size();
            }

            @Override
            public Fragment createFragment(int position) {
                if (position == 0) {
                    return ResumenGeneralFragment.newInstance(
                            analisisGlobal,
                            metricasGlobales,
                            grado -> {
                                // Desde historial no se actualiza Daniels
                                Toast.makeText(ResumenHistorialActivity.this,
                                        "No se puede actualizar Daniels desde el historial",
                                        Toast.LENGTH_SHORT).show();
                            });
                } else {
                    String fase = fases.get(position - 1);
                    List<Float> emgFase = obtenerEMGFase(fase);
                    return ResumenFaseFragment.newInstance(
                            metricasPorFase.get(fase),
                            analisisPorFase.get(fase),
                            emgFase);
                }
            }
        });

        new TabLayoutMediator(tabLayout, viewPager, (tab, position) ->
                tab.setText(position == 0 ? "General" : fases.get(position - 1))
        ).attach();
    }

    // ===== HELPERS =====

    private List<Float> obtenerEMGFase(String fase) {
        if (!tieneCSVLocal) return null;
        Map<String, List<Float>> emgPorFase = ReporteGenerator.leerEMGPorFase(
                this, prueba.id, ejecucion.id);
        return emgPorFase != null ? emgPorFase.get(fase) : null;
    }

    private float calcularROMMax(MusculoAnalyzer.ResultadoAnalisis r) {
        float max = Float.NaN;
        if (!Float.isNaN(r.romPitch)) max = r.romPitch;
        if (!Float.isNaN(r.romRoll) && (Float.isNaN(max) || r.romRoll > max)) max = r.romRoll;
        if (!Float.isNaN(r.romYaw)  && (Float.isNaN(max) || r.romYaw  > max)) max = r.romYaw;
        return max;
    }

    private float getFloat(Map<String, ?> map, String key) {
        Object val = map.get(key);
        if (val instanceof Double) return ((Double) val).floatValue();
        if (val instanceof Float)  return (Float) val;
        return Float.NaN;
    }

    private void configurarHeader() {
        ((TextView) findViewById(R.id.tvNombrePrueba)).setText(prueba.nombre);

        String fecha = ejecucion.fechaEjecucion != 0
                ? new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                .format(new java.util.Date(ejecucion.fechaEjecucion))
                : "";

        String origen = tieneCSVLocal ? "  ·  FFT disponible" : "  ·  Sin datos locales";

        ((TextView) findViewById(R.id.tvInfoPrueba)).setText(
                fecha + "  ·  " + ejecucion.duracionReal + "s" +
                        "  ·  " + ejecucion.totalMuestras + " muestras" + origen);
    }

    private void exportarPDFHistorial() {
        try {
            File archivo = ReporteGenerator.exportarPDFHistorial(
                    this, prueba, ejecucion);
            Uri uri = FileProvider.getUriForFile(
                    this, getPackageName() + ".provider", archivo);
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("application/pdf");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent, "Compartir PDF"));
        } catch (Exception e) {
            Toast.makeText(this, "Error al generar PDF: " + e.getMessage(),
                    Toast.LENGTH_LONG).show();
        }
    }

    private void descargarCSVSiDisponible() {
        Toast.makeText(this, "Descargando datos...", Toast.LENGTH_SHORT).show();
        ReporteGenerator.descargarCSVDeStorage(this, prueba.id, ejecucion.id,
                new ReporteGenerator.OnDescargaListener() {
                    @Override
                    public void onExito(File archivo) {
                        tieneCSVLocal = true;
                        runOnUiThread(() -> {
                            configurarHeader();
                            calcularDesdeCSV();
                            configurarTabs();
                            Toast.makeText(ResumenHistorialActivity.this,
                                    "Datos descargados — FFT disponible",
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                    @Override
                    public void onError(Exception e) {
                        android.util.Log.w("STORAGE",
                                "CSV no disponible: " + e.getMessage());
                    }
                    @Override
                    public void onProgreso(int porcentaje) {}
                });
    }
}