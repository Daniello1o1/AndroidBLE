package com.upiiz.ble_sipi.Models;

import java.io.Serializable;

public class MuestraDato implements Serializable {
    public long timestampMs;
    public String fase;

    // Contexto
    public String pruebaId;
    public String pruebaNombre;
    public String ejecucionId;
    public String pacienteId;
    public String pacienteNombre;
    public int    pacienteEdad;
    public String pacienteSexo;

    // ESP32 — NaN si no se usó
    public float emg         = Float.NaN;
    public float dinamometro = Float.NaN;

    // Watch — NaN si no se usó
    public float accX  = Float.NaN;
    public float accY  = Float.NaN;
    public float accZ  = Float.NaN;
    public float gyroX = Float.NaN;
    public float gyroY = Float.NaN;
    public float gyroZ = Float.NaN;
    public float pitch = Float.NaN;
    public float roll  = Float.NaN;
    public float yaw   = Float.NaN;

    public MuestraDato(long timestampMs, String fase) {
        this.timestampMs = timestampMs;
        this.fase = fase;
    }
}