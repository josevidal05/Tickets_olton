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
            this.id = jsonObject.optInt("id", 0);
            // Intentamos coger 'contacto', si no 'username'
            this.username = jsonObject.optString("contacto", jsonObject.optString("username", "Usuario"));
            this.empresa = jsonObject.optString("empresa", "Empresa no cargada");
            this.nombre = jsonObject.optString("nombre", "");
            this.correo = jsonObject.optString("correo", "");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public int getId() { return id; }
    public String getUsername() { return username; }
    public String getEmpresa() { return empresa; }
    public String getNombre() { return nombre; }
    public String getCorreo() { return correo; }


}
