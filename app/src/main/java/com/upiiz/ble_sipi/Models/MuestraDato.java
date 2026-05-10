package com.upiiz.ble_sipi.Models;

import java.io.Serializable;

public class MuestraDato implements Serializable {
    public long timestampMs;
    public String fase;

    // ESP32
    public float emg;
    public float dinamometro;

    // Watch
    public float accX, accY, accZ;
    public float gyroX, gyroY, gyroZ;
    public float pitch, roll, yaw;

    public MuestraDato(long timestampMs, String fase) {
        this.timestampMs = timestampMs;
        this.fase = fase;
    }
}
