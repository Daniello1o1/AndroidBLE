package com.upiiz.ble_sipi.Models;

import java.io.Serializable;

public class PerfilClinico implements Serializable {
    public String id;
    public String pacienteId;

    public boolean fuma;
    public boolean tomaAlcohol;
    public boolean consumeDrogas;
    public boolean tieneAntecedentesEnfermedades;
    public boolean sarcopenia;
    public boolean debilidadMuscularCronica;
    public boolean deficitVitaminaB12;
    public boolean deficitVitaminaD;
    public boolean participaRehabilitacion;
    public boolean alimentacionSaludable;
    public String  tipoEjercicio;
    public boolean disartria;
    public String  tipDisartria;
    public String  observaciones;

    public PerfilClinico() {}
}