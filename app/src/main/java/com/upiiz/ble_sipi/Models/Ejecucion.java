package com.upiiz.ble_sipi.Models;


import java.io.Serializable;
import java.util.Map;

public class Ejecucion implements Serializable {

    public String id;
    public String pacienteId;
    public String pruebaId;
    public long fechaEjecucion;
    public int duracionReal;
    public int totalMuestras;

    // Métricas totales
    public float emgMAVTotal;
    public float emgWLTotal;
    public float emgOrderVTotal;
    public float dynMAVTotal;

    // Métricas por fase — clave: nombre de fase
    // valor: mapa con "emgMAV", "emgWL", "emgOrderV", "dynMAV"
    public Map<String, Map<String, Float>> metricasPorFase;

    // Metricas GOD

    // EMG
    public float rms;
    public float mav;
    public float wl;
    public float frecuenciaMediana;
    public float indiceFatigaEMG;

    // Dinamómetro
    public float fuerzaMaxima;
    public float tiempoHastaPico;
    public float rfd;
    public float impulso;

    // IMU
    public float romPitch;
    public float romRoll;
    public float romYaw;
    public float velocidadAngularMaxima;
    public float velocidadAngularPromedio;
    public float indiceFatigaMecanica;

    // Fusión
    public float eficienciaMuscular;
    public float eficienciaMovimiento;
    public float onsetEMGFuerza;
    public float onsetEMGMovimiento;

    // Daniels
    public int danielsEstimado;
    public int danielsAsignado;


    public Ejecucion() {}
}
