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
        if (analisis == null) return;

        // EMG
        setText(v, R.id.tvRMS,           "%.4f",    analisis.rms);
        setText(v, R.id.tvMAV,           "%.4f",    analisis.mav);
        setText(v, R.id.tvWL,            "%.4f",    analisis.wl);
        setText(v, R.id.tvFrecMediana,   "%.1f Hz", analisis.frecuenciaMediana);
        setText(v, R.id.tvIndiceFatiga,  "%.4f",    analisis.indiceFatigaEMG);

        // Dinamómetro
        setText(v, R.id.tvFuerzaMax,     "%.4f V",  analisis.fuerzaMaxima);
        setText(v, R.id.tvTiempoPico,    "%.0f ms", analisis.tiempoHastaPico);
        setText(v, R.id.tvRFD,          "%.2f V/s", analisis.rfd);
        setText(v, R.id.tvImpulso,       "%.4f",    analisis.impulso);

        // IMU
        setText(v, R.id.tvRomPitch,      "%.1f°",   analisis.romPitch);
        setText(v, R.id.tvRomRoll,       "%.1f°",   analisis.romRoll);
        setText(v, R.id.tvRomYaw,        "%.1f°",   analisis.romYaw);
        setText(v, R.id.tvOmegaMax,      "%.1f °/s", analisis.velocidadAngularMaxima);
        setText(v, R.id.tvOmegaProm,     "%.1f °/s", analisis.velocidadAngularPromedio);
        setText(v, R.id.tvFatigaMec,     "%.4f",    analisis.indiceFatigaMecanica);

        // Fusión
        setText(v, R.id.tvEficMuscular,  "%.4f",    analisis.eficienciaMuscular);
        setText(v, R.id.tvEficMov,       "%.4f",    analisis.eficienciaMovimiento);
        setText(v, R.id.tvOnsetFuerza,   "%.0f ms", analisis.onsetEMGFuerza);
        setText(v, R.id.tvOnsetMov,      "%.0f ms", analisis.onsetEMGMovimiento);

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