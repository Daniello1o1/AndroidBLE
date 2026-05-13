package com.upiiz.ble_sipi.Models;

import java.io.Serializable;

public class LesionPrevia implements Serializable {
    public String id;
    public String pacienteId;
    public String tipoLesion;
    public String zonaAfectada;
    public String fechaLesion;
    public boolean secuela;
    public String descripcion;

    public LesionPrevia() {}
}