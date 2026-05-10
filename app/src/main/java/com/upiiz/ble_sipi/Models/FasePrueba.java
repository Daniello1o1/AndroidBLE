package com.upiiz.ble_sipi.Models;
import java.io.Serializable;

public class FasePrueba implements Serializable{
    public String nombre;
    public int duracionSegundos;


    public FasePrueba(String nombre, int duracionSegundos) {
        this.nombre = nombre;
        this.duracionSegundos = duracionSegundos;
    }
}