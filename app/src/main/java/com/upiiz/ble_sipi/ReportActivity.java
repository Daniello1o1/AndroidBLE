package com.upiiz.ble_sipi;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
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

import java.util.ArrayList;
import java.util.List;

public class ReportActivity extends AppCompatActivity {
    float[] arrayEMG;
    float[] arrayDyn;
    private List<Float> muestrasEMG;
    private List<Float> muestrasDyn;
    TextView tvEmgMAV, tvEmgWl, tvEmgOrderV, tvDynMAV;
    private EMGFrequencyAnalyzer analyzer;
    Button btnBack;
    private LineChart chartEspectro;
    private int sampleRate = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_report);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        arrayEMG = getIntent().getFloatArrayExtra("emgMuestras");
        arrayDyn = getIntent().getFloatArrayExtra("dynMuestras");


        if (arrayEMG == null || arrayEMG.length < 1024) {
            Toast.makeText(this, "Datos insuficientes para FFT (mínimo 1024 muestras)",
                    Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        muestrasEMG = new ArrayList<>();
        muestrasDyn = new ArrayList<>();
        for (float valor : arrayEMG) {
            muestrasEMG.add(valor);
        }
        for (float valor : arrayDyn) {
            muestrasDyn.add(valor);
        }

        tvEmgMAV = findViewById(R.id.tvEmgMAV);
        tvEmgWl = findViewById(R.id.tvEmgWl);
        tvEmgOrderV = findViewById(R.id.tvEmgOrderV);
        tvDynMAV = findViewById(R.id.tvDynMAV);

        chartEspectro = findViewById(R.id.chart);

        tvEmgMAV.setText("MAV: "+MAV(muestrasEMG));
        tvEmgWl.setText("WL: "+WL(muestrasEMG));
        tvEmgOrderV.setText("Order V: "+orderV(muestrasEMG));
        tvDynMAV.setText("MAV: "+MAV(muestrasDyn));

        btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v->{finish();});

        analyzer = new EMGFrequencyAnalyzer(1024, sampleRate);
        configurarGrafico();
        graficarEspectro(muestrasEMG, 0);
    }

    public float MAV(List<Float> muestras){
        float suma = 0;
        for(int i=0;i<muestras.size();i++){
            suma += muestras.get(i);
        }
        return suma / muestras.size();
    }
    public float orderV(List<Float> muestras){
        float suma = 0;
        for(int i=0;i<muestras.size();i++){
            suma += (float) Math.pow(muestras.get(i),2);
        }
        return (float) Math.sqrt(suma / muestras.size());
    }

    public float WL(List<Float> muestras){
        float suma = 0;
        for(int i=0;i<muestras.size()-1;i++){
            suma += Math.abs(muestras.get(i+1) - muestras.get(i));
        }
        return suma/muestras.size();
    }

    private void configurarGrafico() {
        chartEspectro.setTouchEnabled(true);
        chartEspectro.setPinchZoom(true);
        chartEspectro.setDragEnabled(true);
        chartEspectro.getDescription().setEnabled(false);

        XAxis xAxis = chartEspectro.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(Color.BLACK);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                float resolucion = 1000f / 1024f; // ~0.98 Hz
                return String.format("%.0f", value * resolucion);
            }
        });

        YAxis yAxisLeft = chartEspectro.getAxisLeft();
        yAxisLeft.setAxisMinimum(0f);
        yAxisLeft.setTextColor(Color.BLACK);

        chartEspectro.getAxisRight().setEnabled(false);
    }
    private void graficarEspectro(List<Float> muestras, int startIndex) {
        double[] magnitudes = analyzer.computeMagnitudes(muestras, startIndex);
        if (magnitudes == null) return;

        ArrayList<Entry> entries = new ArrayList<>();
        int maxFreqIndex = Math.min(magnitudes.length, 250); // hasta ~245 Hz

        for (int i = 0; i < maxFreqIndex; i++) {
            entries.add(new Entry(i, (float) magnitudes[i]));
        }

        LineDataSet dataSet = new LineDataSet(entries, "Espectro EMG");
        dataSet.setColor(Color.rgb(255, 87, 34));
        dataSet.setDrawCircles(false);
        dataSet.setDrawValues(false);
        dataSet.setLineWidth(1.5f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.rgb(255, 87, 34));
        dataSet.setFillAlpha(80);

        LineData lineData = new LineData(dataSet);
        chartEspectro.setData(lineData);
        chartEspectro.invalidate();
    }
}