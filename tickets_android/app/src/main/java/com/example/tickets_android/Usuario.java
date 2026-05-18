package com.example.tickets_android;


import org.json.JSONException;
import org.json.JSONObject;


public class Usuario {

    private int id;
    private String username;
    private String empresa;
    private String nombre;
    private String correo;

    public Usuario(JSONObject jsonObject) {

        try {
            this.id = jsonObject.getInt("id");
            this.username = jsonObject.optString("contacto", "");
            this.empresa = jsonObject.optString("empresa", "");
            this.nombre = jsonObject.optString("nombre", "");
            this.correo = jsonObject.optString("correo", "");
        }catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getEmpresa() { return empresa; }
    public String getNombre() { return nombre; }
    public String getCorreo() { return correo; }


}
