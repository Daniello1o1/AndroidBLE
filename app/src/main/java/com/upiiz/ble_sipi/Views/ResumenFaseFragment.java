package com.upiiz.ble_sipi.Views;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.upiiz.ble_sipi.R;
import com.upiiz.ble_sipi.Tools.EMGFrequencyAnalyzer;
import com.upiiz.ble_sipi.Tools.MusculoAnalyzer;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ResumenFaseFragment extends Fragment {

    private float[] metricasBasicas;
    private MusculoAnalyzer.ResultadoAnalisis analisis;
    private List<Float> emgFase;

    public static ResumenFaseFragment newInstance(
            float[] metricasBasicas,
            MusculoAnalyzer.ResultadoAnalisis analisis,
            List<Float> emgFase) {
        ResumenFaseFragment f = new ResumenFaseFragment();
        f.metricasBasicas = metricasBasicas;
        f.analisis        = analisis;
        f.emgFase         = emgFase;
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_resumen_fase, container, false);
        llenarDatos(view);
        return view;
    }

    private void llenarDatos(View v) {
        // Métricas básicas
        if (metricasBasicas != null) {
            setText(v, R.id.tvMAV,    "%.4f", metricasBasicas[0]);
            setText(v, R.id.tvWL,     "%.4f", metricasBasicas[1]);
            setText(v, R.id.tvOrderV, "%.4f", metricasBasicas[2]);
            setText(v, R.id.tvDynMAV, "%.4f", metricasBasicas[3]);
        }

        if (analisis != null) {
            setText(v, R.id.tvRMS,         "%.4f",   analisis.rms);
            setText(v, R.id.tvFrecMediana, "%.1f Hz", analisis.frecuenciaMediana);
            setText(v, R.id.tvRFD,         "%.2f",   analisis.rfd);
            setText(v, R.id.tvFuerzaMax,   "%.4f V", analisis.fuerzaMaxima);
            setText(v, R.id.tvIndiceFatiga,"%.4f",   analisis.indiceFatiga);
            setText(v, R.id.tvEficiencia,  "%.4f",   analisis.eficienciaMusular);
            setText(v, R.id.tvCV,          "%.1f%%", analisis.coeficienteVariacion);

            TextView tvDaniels = v.findViewById(R.id.tvDaniels);
            if (tvDaniels != null)
                tvDaniels.setText(String.valueOf(analisis.danielsEstimado));
        }

        // FFT
        graficarFFT(v);
    }

    private void graficarFFT(View v) {
        LineChart chart   = v.findViewById(R.id.chartFFT);
        TextView tvSinFFT = v.findViewById(R.id.tvSinFFT);

        if (emgFase == null || emgFase.size() < 1024) {
            chart.setVisibility(View.GONE);
            if (tvSinFFT != null) tvSinFFT.setVisibility(View.VISIBLE);
            return;
        }

        EMGFrequencyAnalyzer analyzer = new EMGFrequencyAnalyzer(1024, 1000);
        double[] magnitudes = analyzer.computeMagnitudes(emgFase, 0);
        if (magnitudes == null) return;

        ArrayList<Entry> entries = new ArrayList<>();
        int maxFreqIndex = Math.min(magnitudes.length, 250);
        for (int i = 0; i < maxFreqIndex; i++) {
            entries.add(new Entry(i * (1000f / 1024f), (float) magnitudes[i]));
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
                return String.format("%.0f Hz", value);
            }
        });

        YAxis yAxis = chart.getAxisLeft();
        yAxis.setAxisMinimum(0f);
        chart.getAxisRight().setEnabled(false);
        chart.getDescription().setEnabled(false);
        chart.setTouchEnabled(true);
        chart.setDragEnabled(true);
        chart.setPinchZoom(true);
        chart.setData(new LineData(dataSet));
        chart.invalidate();
    }

    private void setText(View v, int id, String fmt, float val) {
        TextView tv = v.findViewById(id);
        if (tv == null) return;
        if (Float.isNaN(val)) {
            tv.setText("—");          // guión para dato ausente
            tv.setTextColor(getResources().getColor(R.color.text_hint, null));
        } else {
            tv.setText(String.format(Locale.US, fmt, val));
            tv.setTextColor(getResources().getColor(R.color.on_background, null));
        }
    }
}
