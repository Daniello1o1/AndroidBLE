package com.upiiz.ble_sipi.Tools;

import com.upiiz.ble_sipi.Models.MuestraDato;
import java.util.ArrayList;
import java.util.List;

public class MuestrasCache {
    private static List<MuestraDato> muestras = new ArrayList<>();

    public static void guardar(List<MuestraDato> datos) {
        muestras = new ArrayList<>(datos);
    }

    public static List<MuestraDato> obtener() {
        return muestras;
    }

    public static void limpiar() {
        muestras.clear();
    }
}