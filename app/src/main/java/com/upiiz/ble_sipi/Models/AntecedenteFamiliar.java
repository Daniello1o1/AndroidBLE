package com.upiiz.ble_sipi.Models;

import java.io.Serializable;

public class AntecedenteFamiliar implements Serializable {
    public String id;
    public String pacienteId;
    public String parentesco;
    public String enfermedad;
    public String descripcion;
    public boolean siguePresente;

    public AntecedenteFamiliar() {}
}