package com.upiiz.ble_sipi.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.upiiz.ble_sipi.R;
import com.upiiz.ble_sipi.Models.Ejecucion;
import com.upiiz.ble_sipi.Models.Prueba;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class EjecucionAdapter extends RecyclerView.Adapter<EjecucionAdapter.ViewHolder> {

    public interface OnEjecucionClickListener {
        void onEjecucionClick(Ejecucion ejecucion);
    }

    private final List<Ejecucion> lista = new ArrayList<>();
    private final Prueba prueba;
    private final OnEjecucionClickListener listener;
    private final SimpleDateFormat sdf =
            new SimpleDateFormat("dd/MM/yyyy  HH:mm", Locale.getDefault());

    public EjecucionAdapter(Prueba prueba, OnEjecucionClickListener listener) {
        this.prueba   = prueba;
        this.listener = listener;
    }

    public void setEjecuciones(List<Ejecucion> ejecuciones) {
        lista.clear();
        lista.addAll(ejecuciones);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_ejecucion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Ejecucion e = lista.get(position);

        // Fecha
        if (e.fechaEjecucion != 0) {
            holder.tvFecha.setText(sdf.format(new java.util.Date(e.fechaEjecucion)));
        }

        // Duración
        holder.tvDuracion.setText(e.duracionReal + "s  ·  " +
                e.totalMuestras + " muestras");

        // Métricas EMG — ocultar si no se usó
        if (prueba.necesitaESP32()) {
            holder.tvLabelEMG.setVisibility(View.VISIBLE);
            holder.layoutEMG.setVisibility(View.VISIBLE);
            holder.tvMAV.setText(String.format(Locale.US, "%.4f", e.emgMAVTotal));
            holder.tvWL.setText(String.format(Locale.US, "%.4f", e.emgWLTotal));
            holder.tvOrderV.setText(String.format(Locale.US, "%.4f", e.emgOrderVTotal));
        } else {
            holder.tvLabelEMG.setVisibility(View.GONE);
            holder.layoutEMG.setVisibility(View.GONE);
        }

        // Dinamómetro — ocultar si no se usó
        if (prueba.usarDinamometro) {
            holder.tvLabelDyn.setVisibility(View.VISIBLE);
            holder.tvDynMAV.setVisibility(View.VISIBLE);
            holder.tvDynMAV.setText(String.format(Locale.US, "%.4f", e.dynMAVTotal));
        } else {
            holder.tvLabelDyn.setVisibility(View.GONE);
            holder.tvDynMAV.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onEjecucionClick(e));
    }

    @Override
    public int getItemCount() { return lista.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvFecha, tvDuracion;
        TextView tvLabelEMG, tvMAV, tvWL, tvOrderV;
        TextView tvLabelDyn, tvDynMAV;
        LinearLayout layoutEMG;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFecha     = itemView.findViewById(R.id.tvFecha);
            tvDuracion  = itemView.findViewById(R.id.tvDuracion);
            tvLabelEMG  = itemView.findViewById(R.id.tvLabelEMG);
            layoutEMG   = itemView.findViewById(R.id.layoutEMG);
            tvMAV       = itemView.findViewById(R.id.tvMAV);
            tvWL        = itemView.findViewById(R.id.tvWL);
            tvOrderV    = itemView.findViewById(R.id.tvOrderV);
            tvLabelDyn  = itemView.findViewById(R.id.tvLabelDyn);
            tvDynMAV    = itemView.findViewById(R.id.tvDynMAV);
        }
    }
}
