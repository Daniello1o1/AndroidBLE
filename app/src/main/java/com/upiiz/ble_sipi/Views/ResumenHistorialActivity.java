package com.upiiz.ble_sipi.Views;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.button.MaterialButton;
import com.upiiz.ble_sipi.R;
import com.upiiz.ble_sipi.Tools.EMGFrequencyAnalyzer;
import com.upiiz.ble_sipi.Tools.ReporteGenerator;
import com.upiiz.ble_sipi.Models.Ejecucion;
import com.upiiz.ble_sipi.Models.Prueba;

import android.graphics.Color;
import android.widget.Toast;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ResumenHistorialActivity extends AppCompatActivity {

    private Prueba prueba;
    private Ejecucion ejecucion;
    private boolean tieneCSVLocal;

    private static final int SAMPLE_RATE = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resumen_prueba);

        prueba    = (Prueba)    getIntent().getSerializableExtra("prueba");
        ejecucion = (Ejecucion) getIntent().getSerializableExtra("ejecucion");

        // Verificar si existe CSV local
        tieneCSVLocal = ReporteGenerator.existeCSVLocal(
                this, prueba.id, ejecucion.id);

        if (!tieneCSVLocal) {
            // Intentar descargar de Storage
            descargarCSVSiDisponible();
        }

        configurarHeader();
        construirTarjetasPorFase();

        // Ocultar exportar CSV/PDF — no aplica desde historial
        MaterialButton btnPDF = findViewById(R.id.btnExportarPDF);
        btnPDF.setOnClickListener(v -> exportarPDFHistorial());
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

    private void construirTarjetasPorFase() {
        LinearLayout container = findViewById(R.id.containerFases);

        // Cargar EMG por fase desde CSV si existe
        Map<String, List<Float>> emgPorFase = null;
        if (tieneCSVLocal) {
            emgPorFase = ReporteGenerator.leerEMGPorFase(
                    this, prueba.id, ejecucion.id);
        }

        // Tarjeta de totales
        agregarTarjeta(container, "Total general",
                ejecucion.emgMAVTotal,
                ejecucion.emgWLTotal,
                ejecucion.emgOrderVTotal,
                ejecucion.dynMAVTotal,
                emgPorFase != null ? obtenerTodosEMG(emgPorFase) : null);

        // Tarjetas por fase
        if (ejecucion.metricasPorFase != null) {
            for (Map.Entry<String, Map<String, Float>> entry
                    : ejecucion.metricasPorFase.entrySet()) {

                String fase          = entry.getKey();
                Map<String, Float> m = entry.getValue();

                List<Float> emgFase = (emgPorFase != null)
                        ? emgPorFase.get(fase) : null;

                agregarTarjeta(container, fase,
                        getFloat(m, "emgMAV"),
                        getFloat(m, "emgWL"),
                        getFloat(m, "emgOrderV"),
                        getFloat(m, "dynMAV"),
                        emgFase);
            }
        }
    }

    // Exportar

    private void exportarPDFHistorial() {
        try {
            File archivo = ReporteGenerator.exportarPDFHistorial(
                    this, prueba, ejecucion);  // sin emgPorFase

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

    private void agregarTarjeta(LinearLayout container,
                                String titulo,
                                float emgMAV, float emgWL,
                                float emgOrderV, float dynMAV,
                                List<Float> emgParaFFT) {

        View tarjeta = LayoutInflater.from(this)
                .inflate(R.layout.item_resumen_fase, container, false);

        ((TextView) tarjeta.findViewById(R.id.tvNombreFase)).setText(titulo);

        if (prueba.necesitaESP32()) {
            ((TextView) tarjeta.findViewById(R.id.tvMAV))
                    .setText(String.format(Locale.US, "%.4f", emgMAV));
            ((TextView) tarjeta.findViewById(R.id.tvWL))
                    .setText(String.format(Locale.US, "%.4f", emgWL));
            ((TextView) tarjeta.findViewById(R.id.tvOrderV))
                    .setText(String.format(Locale.US, "%.4f", emgOrderV));
            ((TextView) tarjeta.findViewById(R.id.tvDynMAV))
                    .setText(String.format(Locale.US, "%.4f", dynMAV));
        } else {
            tarjeta.findViewById(R.id.tvLabelEMG).setVisibility(View.GONE);
            tarjeta.findViewById(R.id.layoutMetricasEMG).setVisibility(View.GONE);
        }

        // FFT — solo si hay datos locales y suficientes muestras
        if (emgParaFFT != null && emgParaFFT.size() >= 1024) {
            graficarFFT(tarjeta, emgParaFFT);
        } else {
            tarjeta.findViewById(R.id.tvLabelFFT).setVisibility(View.GONE);
            tarjeta.findViewById(R.id.chartFFT).setVisibility(View.GONE);
        }

        container.addView(tarjeta);
    }

    private void graficarFFT(View tarjeta, List<Float> emgFase) {
        EMGFrequencyAnalyzer analyzer = new EMGFrequencyAnalyzer(1024, SAMPLE_RATE);
        double[] magnitudes = analyzer.computeMagnitudes(emgFase, 0);
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

        LineChart chart = tarjeta.findViewById(R.id.chartFFT);
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

    // Combina todas las fases en una sola lista para el total
    private List<Float> obtenerTodosEMG(Map<String, List<Float>> emgPorFase) {
        List<Float> todos = new ArrayList<>();
        for (List<Float> fase : emgPorFase.values()) todos.addAll(fase);
        return todos;
    }

    private float getFloat(Map<String, ?> map, String key) {
        Object val = map.get(key);
        if (val instanceof Double) return ((Double) val).floatValue();
        if (val instanceof Float)  return (Float) val;
        return 0f;
    }
    private void descargarCSVSiDisponible() {
        // Mostrar indicador de descarga
        Toast.makeText(this, "Descargando datos...", Toast.LENGTH_SHORT).show();

        ReporteGenerator.descargarCSVDeStorage(
                this,
                prueba.id,
                ejecucion.id,
                new ReporteGenerator.OnDescargaListener() {
                    @Override
                    public void onExito(File archivo) {
                        tieneCSVLocal = true;
                        runOnUiThread(() -> {
                            // Actualizar subtítulo para indicar que FFT está disponible
                            configurarHeader();
                            Toast.makeText(ResumenHistorialActivity.this,
                                    "Datos descargados — FFT disponible",
                                    Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        android.util.Log.w("STORAGE",
                                "CSV no disponible en Storage: " + e.getMessage());
                        // Silencioso — simplemente no habrá FFT
                    }

                    @Override
                    public void onProgreso(int porcentaje) {
                        android.util.Log.d("STORAGE", "Descarga: " + porcentaje + "%");
                    }
                });
    }
}