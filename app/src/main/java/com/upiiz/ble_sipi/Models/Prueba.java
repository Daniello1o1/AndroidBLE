package com.upiiz.ble_sipi.Models;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Prueba implements Serializable {

    public String id;                       // ID del documento en Firestore
    public String nombre;
    public String pacienteId;
    public int duracionTotalSegundos;
    public boolean tieneIntervalos;
    public List<FasePrueba> fases;
    public long creadoEn;

    // Sensores
    public boolean usarEMG;
    public boolean usarDinamometro;
    public boolean usarAcelerometro;
    public boolean usarGiroscopio;
    public boolean usarOrientacion;

    public Prueba() {
        fases = new ArrayList<>();
    }

    public int getDuracionDesdeFases() {
        int total = 0;
        for (FasePrueba f : fases) total += f.duracionSegundos;
        return total;
    }

    public boolean necesitaESP32() {
        return usarEMG || usarDinamometro;
    }

    public boolean necesitaWatch() {
        return usarAcelerometro || usarGiroscopio || usarOrientacion;
    }
}