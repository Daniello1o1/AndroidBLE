package com.upiiz.ble_sipi.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.upiiz.ble_sipi.Models.Paciente;
import com.upiiz.ble_sipi.R;

import java.util.ArrayList;
import java.util.List;

public class PacienteAdapter extends RecyclerView.Adapter<PacienteAdapter.ViewHolder> {

    public interface OnPacienteClickListener {
        void onPacienteClick(Paciente paciente);
    }

    private final List<Paciente> listaOriginal = new ArrayList<>();
    private final List<Paciente> listaFiltrada = new ArrayList<>();
    private final OnPacienteClickListener listener;

    public PacienteAdapter(OnPacienteClickListener listener) {
        this.listener = listener;
    }

    public void setPacientes(List<Paciente> pacientes) {
        listaOriginal.clear();
        listaOriginal.addAll(pacientes);
        listaFiltrada.clear();
        listaFiltrada.addAll(pacientes);
        notifyDataSetChanged();
    }

    public void filtrar(String query) {
        listaFiltrada.clear();
        if (query == null || query.trim().isEmpty()) {
            listaFiltrada.addAll(listaOriginal);
        } else {
            String q = query.toLowerCase().trim();
            for (Paciente p : listaOriginal) {
                if ((p.nombre != null && p.nombre.toLowerCase().contains(q)) ||
                        (p.apellidos != null && p.apellidos.toLowerCase().contains(q))) {
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
                .inflate(R.layout.item_paciente, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Paciente p = listaFiltrada.get(position);

        // Inicial del nombre
        String inicial = (p.nombre != null && !p.nombre.isEmpty())
                ? String.valueOf(p.nombre.charAt(0)).toUpperCase() : "?";
        holder.tvInicial.setText(inicial);

        holder.tvNombre.setText(p.getNombreCompleto());
        holder.tvInfo.setText(p.edad + " años  ·  " + (p.sexo != null ? p.sexo : ""));

        holder.itemView.setOnClickListener(v -> listener.onPacienteClick(p));
    }

    @Override
    public int getItemCount() { return listaFiltrada.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvInicial, tvNombre, tvInfo;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInicial = itemView.findViewById(R.id.tvInicial);
            tvNombre  = itemView.findViewById(R.id.tvNombre);
            tvInfo    = itemView.findViewById(R.id.tvInfo);
        }
    }
}