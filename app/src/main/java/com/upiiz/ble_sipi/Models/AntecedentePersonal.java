package com.upiiz.ble_sipi.Models;

import java.io.Serializable;

public class AntecedentePersonal implements Serializable {
    public String id;
    public String pacienteId;
    public String enfermedad;
    public String descripcion;
    public String diagnosticadoPor;
    public String fechaDiagnostico;

    public AntecedentePersonal() {}
}