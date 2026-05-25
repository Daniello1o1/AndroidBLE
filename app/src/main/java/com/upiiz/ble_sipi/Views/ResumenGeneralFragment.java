package com.upiiz.ble_sipi.Views;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.upiiz.ble_sipi.R;
import com.upiiz.ble_sipi.Tools.MusculoAnalyzer;

import java.util.Locale;

public class ResumenGeneralFragment extends Fragment {

    private MusculoAnalyzer.ResultadoAnalisis analisis;
    private float[] metricasBasicas; // [MAV, WL, OrderV, DynMAV]
    private OnDanielsAsignadoListener danielsListener;

    public interface OnDanielsAsignadoListener {
        void onDanielsAsignado(int grado);
    }

    public static ResumenGeneralFragment newInstance(
            MusculoAnalyzer.ResultadoAnalisis analisis,
            float[] metricasBasicas,
            OnDanielsAsignadoListener listener) {
        ResumenGeneralFragment f = new ResumenGeneralFragment();
        f.analisis       = analisis;
        f.metricasBasicas = metricasBasicas;
        f.danielsListener = listener;
        return f;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_resumen_general, container, false);
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

        if (analisis == null) return;

        // Análisis muscular
        setText(v, R.id.tvRMS,          "%.4f", analisis.rms);
        setText(v, R.id.tvVar,          "%.4f", analisis.var);
        setTextInt(v, R.id.tvZC,        analisis.zc);
        setTextInt(v, R.id.tvSSC,       analisis.ssc);
        setText(v, R.id.tvFrecMediana,  "%.1f Hz", analisis.frecuenciaMediana);
        setText(v, R.id.tvFrecMedia,    "%.1f Hz", analisis.frecuenciaMedia);
        setText(v, R.id.tvPotencia,     "%.2f", analisis.potenciaTotal);
        setText(v, R.id.tvRatioBandas,  "%.4f", analisis.ratioBandas);
        setText(v, R.id.tvFuerzaMax,    "%.4f V", analisis.fuerzaMaxima);
        setText(v, R.id.tvRFD,          "%.2f V/s", analisis.rfd);
        setText(v, R.id.tvTiempoPico,   "%.0f ms", analisis.tiempoHastaPico);
        setText(v, R.id.tvImpulso,      "%.4f", analisis.impulso);
        setText(v, R.id.tvEficiencia,   "%.4f", analisis.eficienciaMusular);
        setText(v, R.id.tvOnset,        "%.0f ms", analisis.onsetMusular);
        setText(v, R.id.tvIndiceFatiga, "%.4f", analisis.indiceFatiga);
        setText(v, R.id.tvCV,           "%.1f%%", analisis.coeficienteVariacion);

        // Daniels
        TextView tvDaniels = v.findViewById(R.id.tvDanielsEstimado);
        if (tvDaniels != null)
            tvDaniels.setText(String.valueOf(analisis.danielsEstimado));

        // Botón guardar Daniels
        MaterialButton btnGuardar = v.findViewById(R.id.btnGuardarDaniels);
        TextInputEditText etDaniels = v.findViewById(R.id.etDanielsAsignado);

        if (btnGuardar != null && etDaniels != null) {
            btnGuardar.setOnClickListener(btn -> {
                String val = etDaniels.getText() != null
                        ? etDaniels.getText().toString() : "";
                if (val.isEmpty()) return;
                int grado = Integer.parseInt(val);
                if (grado < 0 || grado > 5) {
                    etDaniels.setError("0-5");
                    return;
                }
                if (danielsListener != null) danielsListener.onDanielsAsignado(grado);
            });
        }
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

    private void setTextInt(View v, int id, int val) {
        TextView tv = v.findViewById(id);
        if (tv != null) tv.setText(String.valueOf(val));
    }
}