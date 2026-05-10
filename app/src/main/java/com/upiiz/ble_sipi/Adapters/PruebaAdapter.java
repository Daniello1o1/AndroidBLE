package com.upiiz.ble_sipi.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.upiiz.ble_sipi.R;
import com.upiiz.ble_sipi.Models.Prueba;

import java.util.ArrayList;
import java.util.List;

public class PruebaAdapter extends RecyclerView.Adapter<PruebaAdapter.ViewHolder> {

    public interface OnPruebaClickListener {
        void onPruebaClick(Prueba prueba);
    }

    private final List<Prueba> listaOriginal = new ArrayList<>();
    private final List<Prueba> listaFiltrada = new ArrayList<>();
    private final OnPruebaClickListener listener;

    public PruebaAdapter(OnPruebaClickListener listener) {
        this.listener = listener;
    }

    public void setPruebas(List<Prueba> pruebas) {
        listaOriginal.clear();
        listaOriginal.addAll(pruebas);
        listaFiltrada.clear();
        listaFiltrada.addAll(pruebas);
        notifyDataSetChanged();
    }

    public void filtrar(String query) {
        listaFiltrada.clear();
        if (query == null || query.trim().isEmpty()) {
            listaFiltrada.addAll(listaOriginal);
        } else {
            String q = query.toLowerCase().trim();
            for (Prueba p : listaOriginal) {
                if (p.nombre != null && p.nombre.toLowerCase().contains(q)) {
                    listaFiltrada.add(p);
                }
            }
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_prueba, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Prueba prueba = listaFiltrada.get(position);

        holder.tvNombre.setText(prueba.nombre);

        // Info: duración + sensores activos
        List<String> sensores = new ArrayList<>();
        if (prueba.usarEMG)          sensores.add("EMG");
        if (prueba.usarDinamometro)  sensores.add("Dinamómetro");
        if (prueba.usarAcelerometro) sensores.add("Acelerómetro");
        if (prueba.usarGiroscopio)   sensores.add("Giroscopio");
        if (prueba.usarOrientacion)  sensores.add("Orientación");

        holder.tvInfo.setText(prueba.duracionTotalSegundos + "s  ·  " +
                String.join(", ", sensores));

        // Fases
        if (prueba.tieneIntervalos && !prueba.fases.isEmpty()) {
            holder.tvFases.setText(prueba.fases.size() + " fases");
            holder.tvFases.setVisibility(View.VISIBLE);
        } else {
            holder.tvFases.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> listener.onPruebaClick(prueba));
    }

    @Override
    public int getItemCount() {
        return listaFiltrada.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvNombre, tvInfo, tvFases;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvNombre = itemView.findViewById(R.id.tvNombre);
            tvInfo   = itemView.findViewById(R.id.tvInfo);
            tvFases  = itemView.findViewById(R.id.tvFases);
        }
    }
}
