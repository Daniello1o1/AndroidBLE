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

    // Análisis completo
    public float rms;
    public float var;
    public int   zc;
    public int   ssc;
    public float frecuenciaMediana;
    public float frecuenciaMedia;
    public float potenciaTotal;
    public float ratioBandas;
    public float indiceFatiga;
    public float tasaDecaimientoRMS;
    public float fuerzaMaxima;
    public float fuerzaMinima;
    public float tiempoHastaPico;
    public float rfd;
    public float impulso;
    public float coeficienteVariacion;
    public float eficienciaMusular;
    public float onsetMusular;
    public int   danielsEstimado;
    public int   danielsAsignado; // asignado por el médico

    public Ejecucion() {}
}
