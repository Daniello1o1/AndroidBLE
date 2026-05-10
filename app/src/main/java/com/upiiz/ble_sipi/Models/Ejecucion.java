package com.upiiz.ble_sipi.Models;


import java.io.Serializable;
import java.util.Map;

public class Ejecucion implements Serializable {

    public String id;
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

    public Ejecucion() {}
}
