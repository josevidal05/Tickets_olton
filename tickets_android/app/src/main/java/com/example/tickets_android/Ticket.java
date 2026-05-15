package com.example.tickets_android;

import org.json.JSONException;
import org.json.JSONObject;

public class Ticket {
    private int id;
    private String contacto;
    private String empresa;
    private String tipo_dispositivo;
    private String id_dispositivo;
    private String observaciones;
    private String archivo;
    private String portes;
    private String transporte;
    private String fecha;

    public Ticket(JSONObject jsonObject) {
        try {
            this.id = jsonObject.getInt("id");
            this.contacto = jsonObject.optString("contacto", "");
            this.empresa = jsonObject.optString("empresa", "");
            this.tipo_dispositivo = jsonObject.optString("tipo_dispositivo", "");
            this.id_dispositivo = jsonObject.optString("id_dispositivo", "");
            this.observaciones = jsonObject.optString("observaciones", "");
            this.archivo = jsonObject.optString("archivo", "");
            this.portes = jsonObject.optString("portes", "");
            this.transporte = jsonObject.optString("empresa_transporte", "");
            this.fecha = jsonObject.optString("fecha_creacion", "");
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public int getId() { return id; }
    public String getContacto() { return contacto; }
    public String getEmpresa() { return empresa; }
    public String getTipo_dispositivo() { return tipo_dispositivo; }
    public String getId_dispositivo() { return id_dispositivo; }
    public String getObservaciones() { return observaciones; }
    public String getArchivo() { return archivo; }
    public String getPortes() { return portes; }
    public String getTransporte() { return transporte; }
    public String getFecha() { return fecha; }
}
