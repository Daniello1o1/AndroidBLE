package com.upiiz.ble_sipi.Models;

import java.io.Serializable;

public class Medicamento implements Serializable {
    public String id;
    public String pacienteId;
    public String nombre;
    public String tipoTratamiento;
    public String dosis;
    public String frecuencia;
    public String motivo;
    public String fechaInicio;
    public String fechaFin;
    public String observaciones;

    public Medicamento() {}
}