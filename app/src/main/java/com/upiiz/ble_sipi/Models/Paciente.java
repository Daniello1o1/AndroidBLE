package com.upiiz.ble_sipi.Models;

import java.io.Serializable;

public class Paciente implements Serializable {
    public String id;
    public String nombre;
    public String apellidos;
    public String fechaNacimiento; // "dd/MM/yyyy"
    public int edad;
    public String sexo;            // "Masculino", "Femenino", "Otro"
    public float peso;
    public float talla;
    public String observaciones;
    public long creadoEn;

    public Paciente() {}

    public String getNombreCompleto() {
        return nombre + " " + apellidos;
    }
}