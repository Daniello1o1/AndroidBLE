package com.upiiz.ble_sipi.Views;

import static com.upiiz.ble_sipi.Tools.ReporteGenerator.exportarCSV;
import static com.upiiz.ble_sipi.Tools.ReporteGenerator.exportarPDF;

import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.button.MaterialButton;
import com.upiiz.ble_sipi.Models.Ejecucion;
import com.upiiz.ble_sipi.Models.MuestraDato;
import com.upiiz.ble_sipi.Models.Prueba;
import com.upiiz.ble_sipi.R;
import com.upiiz.ble_sipi.Repository.PruebaRepository;
import com.upiiz.ble_sipi.Tools.EMGFrequencyAnalyzer;
import com.upiiz.ble_sipi.Tools.MuestrasCache;
import com.upiiz.ble_sipi.Tools.ReporteGenerator;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ResumenPruebaActivity extends AppCompatActivity {
    private Prueba config;
    private PruebaRepository repository;
    private boolean yaGuardado = false; // evitar doble guardado
    private ArrayList<MuestraDato> muestras;
    private Map<String, List<MuestraDato>> muestrasPorFase;
    private Map<String, float[]> metricasPorFase; // [MAV, WL, OrderV, DynMAV]

    private LinearLayout containerFases;
    private MaterialButton btnExportarPDF, btnExportarCSV;

    private static final int SAMPLE_RATE = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_resumen_prueba);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        config   = (Prueba) getIntent().getSerializableExtra("config");
        muestras = new ArrayList<>(MuestrasCache.obtener());
        MuestrasCache.limpiar(); // limpiar después de leer

        containerFases = findViewById(R.id.containerFases);
        btnExportarPDF = findViewById(R.id.btnExportarPDF);
        btnExportarCSV = findViewById(R.id.btnExportarCSV);

        ((TextView) findViewById(R.id.tvNombrePrueba)).setText(config.nombre);
        ((TextView) findViewById(R.id.tvInfoPrueba)).setText(
                config.duracionTotalSegundos + "s   ·   " + muestras.size() + " muestras");

        calcularMetricas();
        construirTarjetasPorFase();
        repository = new PruebaRepository();
        guardarEjecucionEnFirestore();

        btnExportarCSV.setOnClickListener(v -> exportarCSV());
        btnExportarPDF.setOnClickListener(v -> exportarPDF());
    }
    // ================= CALCULAR =================

    private void calcularMetricas() {
        muestrasPorFase = ReporteGenerator.agruparPorFase(muestras);
        metricasPorFase = new LinkedHashMap<>();

        for (Map.Entry<String, List<MuestraDato>> entry : muestrasPorFase.entrySet()) {
            List<MuestraDato> datos = entry.getValue();

            List<Float> emg    = new ArrayList<>();
            List<Float> dynamo = new ArrayList<>();
            for (MuestraDato m : datos) {
                emg.add(m.emg);
                dynamo.add(m.dinamometro);
            }

            float mav     = MAV(emg);
            float wl      = WL(emg);
            float orderV  = OrderV(emg);
            float dynMav  = MAV(dynamo);

            metricasPorFase.put(entry.getKey(), new float[]{mav, wl, orderV, dynMav});
        }
    }

    // ================= TARJETAS =================

    private void construirTarjetasPorFase() {
        EMGFrequencyAnalyzer analyzer = new EMGFrequencyAnalyzer(1024, SAMPLE_RATE);

        for (Map.Entry<String, List<MuestraDato>> entry : muestrasPorFase.entrySet()) {
            String fase          = entry.getKey();
            List<MuestraDato> datos = entry.getValue();
            float[] metricas     = metricasPorFase.get(fase);

            View tarjeta = LayoutInflater.from(this)
                    .inflate(R.layout.item_resumen_fase, containerFases, false);

            ((TextView) tarjeta.findViewById(R.id.tvNombreFase)).setText(fase);
            ((TextView) tarjeta.findViewById(R.id.tvMAV))
                    .setText(String.format(Locale.US, "%.4f", metricas[0]));
            ((TextView) tarjeta.findViewById(R.id.tvWL))
                    .setText(String.format(Locale.US, "%.4f", metricas[1]));
            ((TextView) tarjeta.findViewById(R.id.tvOrderV))
                    .setText(String.format(Locale.US, "%.4f", metricas[2]));
            ((TextView) tarjeta.findViewById(R.id.tvDynMAV))
                    .setText(String.format(Locale.US, "%.4f", metricas[3]));

            // Ocultar métricas ESP32 si no se usó
            if (!config.necesitaESP32()) {
                tarjeta.findViewById(R.id.tvLabelEMG).setVisibility(View.GONE);
                tarjeta.findViewById(R.id.layoutMetricasEMG).setVisibility(View.GONE);
                tarjeta.findViewById(R.id.tvLabelFFT).setVisibility(View.GONE);
                tarjeta.findViewById(R.id.chartFFT).setVisibility(View.GONE);
            } else {
                // Graficar FFT de esta fase
                List<Float> emgFase = new ArrayList<>();
                for (MuestraDato m : datos) emgFase.add(m.emg);
                android.util.Log.d("FFT_DEBUG", "Fase: " + fase + " | Muestras: " + emgFase.size());
                if (emgFase.size() >= 1024) {
                    graficarFFT(tarjeta, analyzer, emgFase);
                } else {
                    tarjeta.findViewById(R.id.tvLabelFFT).setVisibility(View.GONE);
                    tarjeta.findViewById(R.id.chartFFT).setVisibility(View.GONE);
                }
            }

            containerFases.addView(tarjeta);
        }
    }

    private void graficarFFT(View tarjeta, EMGFrequencyAnalyzer analyzer, List<Float> emgFase) {
        LineChart chart = tarjeta.findViewById(R.id.chartFFT);
        double[] magnitudes = analyzer.computeMagnitudes(emgFase, 0);
        android.util.Log.d("FFT_DEBUG", "magnitudes: " +
                (magnitudes == null ? "NULL" : magnitudes.length));

        if (magnitudes == null) return;

        ArrayList<Entry> entries = new ArrayList<>();
        int maxFreqIndex = Math.min(magnitudes.length, 250);
        for (int i = 0; i < maxFreqIndex; i++) {
            entries.add(new Entry(i, (float) magnitudes[i]));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Espectro");
        dataSet.setColor(Color.rgb(255, 87, 34));
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setLineWidth(1.5f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.rgb(255, 87, 34));
        dataSet.setFillAlpha(80);

        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return String.format("%.0f", value * (1000f / 1024f));
            }
        });

        YAxis yAxis = chart.getAxisLeft();
        yAxis.setAxisMinimum(0f);
        chart.getAxisRight().setEnabled(false);
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setData(new LineData(dataSet));
        chart.invalidate();
    }

    // ================= MÉTRICAS =================

    private float MAV(List<Float> muestras) {
        if (muestras.isEmpty()) return 0f;
        float suma = 0;
        for (float v : muestras) suma += Math.abs(v);
        return suma / muestras.size();
    }

    private float WL(List<Float> muestras) {
        if (muestras.size() < 2) return 0f;
        float suma = 0;
        for (int i = 0; i < muestras.size() - 1; i++) {
            suma += Math.abs(muestras.get(i + 1) - muestras.get(i));
        }
        return suma / muestras.size();
    }

    private float OrderV(List<Float> muestras) {
        if (muestras.isEmpty()) return 0f;
        float suma = 0;
        for (float v : muestras) suma += v * v;
        return (float) Math.sqrt(suma / muestras.size());
    }

    // ================= EXPORTAR =================

    private void exportarCSV() {
        try {
            File archivo = ReporteGenerator.exportarCSV(this, config, muestras);
            compartirArchivo(archivo, "text/csv");
            Toast.makeText(this, "CSV generado: " + archivo.getName(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error al generar CSV: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void exportarPDF() {
        try {
            File archivo = ReporteGenerator.exportarPDF(this, config, muestras, metricasPorFase);
            compartirArchivo(archivo, "application/pdf");
            Toast.makeText(this, "PDF generado: " + archivo.getName(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error al generar PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void compartirArchivo(File archivo, String mimeType) {
        Uri uri = FileProvider.getUriForFile(
                this,
                getPackageName() + ".provider",
                archivo
        );
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, "Compartir reporte"));
    }
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
        ejecucion.pacienteId = config.pacienteId; // viene del config
        ejecucion.totalMuestras   = muestras.size();
        ejecucion.emgMAVTotal     = MAV(emgTotal);
        ejecucion.emgWLTotal      = WL(emgTotal);
        ejecucion.emgOrderVTotal  = OrderV(emgTotal);
        ejecucion.dynMAVTotal     = MAV(dynamoTotal);
        ejecucion.metricasPorFase = metricasFaseFirestore;

        repository.guardarEjecucion(config.id, ejecucion,
                new PruebaRepository.Callback<String>() {
                    @Override
                    public void onSuccess(String id) {
                        ejecucion.id = id;

                        // Guardar CSV local con el ID de ejecución
                        try {
                            ReporteGenerator.guardarCSVLocal(
                                    ResumenPruebaActivity.this,
                                    config.id,
                                    id,
                                    muestras);
                            android.util.Log.d("RESUMEN", "CSV local guardado: "
                                    + config.id + "_" + id + ".csv");
                        } catch (Exception e) {
                            android.util.Log.e("RESUMEN",
                                    "Error guardando CSV local: " + e.getMessage());
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        Toast.makeText(ResumenPruebaActivity.this,
                                "No se pudo guardar en Firestore", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}