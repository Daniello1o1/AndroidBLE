package com.upiiz.ble_sipi.Models;

import java.io.Serializable;

public class Usuario implements Serializable {
    public String uid;
    public String nombre;
    public String apellidos;
    public String email;
    public String institucion;
    public String rol;
    public long creadoEn;

    public Usuario() {}

    public String getNombreCompleto() {
        return nombre + " " + apellidos;
    }
}