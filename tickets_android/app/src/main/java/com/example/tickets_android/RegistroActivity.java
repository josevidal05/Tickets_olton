package com.example.tickets_android;

import android.app.AppComponentFactory;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import org.json.JSONException;
import org.json.JSONObject;

public class RegistroActivity extends AppCompatActivity {

    private EditText et_usuario;
    private EditText et_nombre;
    private EditText et_empresa;
    private EditText et_correo;
    private EditText et_contrasena;
    private EditText et_confirmar_contrasena;
    private Button bt_registrarse;
    private Button bt_inicioSesion;
    private Context context = this;
    private String url = Conexiones.REGISTRO_URL;
    private String onResponse = "Usuario creado correctamente";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Inicializamos el Splash Screen
        SplashScreen.installSplashScreen(this);

        SharedPreferences sharedPref = getSharedPreferences("ajustes_tema", Context.MODE_PRIVATE);
        boolean isModoOscuro = sharedPref.getBoolean("modo_oscuro_activado", false);

        if (isModoOscuro) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registro);

        et_usuario = findViewById(R.id.nombre_usuario);
        et_nombre = findViewById(R.id.nombre);
        et_empresa = findViewById(R.id.empresa);
        et_correo = findViewById(R.id.correo);
        et_contrasena = findViewById(R.id.contrasena);
        et_confirmar_contrasena = findViewById(R.id.confirmar_contrasena);

        bt_registrarse = findViewById(R.id.btRegistro);
        bt_inicioSesion = findViewById(R.id.inicio_sesion);

        bt_registrarse.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String contrasena = et_contrasena.getText().toString();
                String confirmarContrasena = et_confirmar_contrasena.getText().toString();

                if (contrasena.isEmpty()) {
                    et_contrasena.setError("La contraseña no puede estar vacía");
                    return;
                }

                if (!contrasena.equals(confirmarContrasena)) {
                    et_confirmar_contrasena.setError("Las contraseñas no coinciden");
                    Toast.makeText(context, "Las contraseñas no coinciden", Toast.LENGTH_LONG).show();
                    return;
                }

                JSONObject requestBody = new JSONObject();
                try{
                    requestBody.put("username", et_usuario.getText().toString());
                    requestBody.put("nombre", et_nombre.getText().toString());
                    requestBody.put("empresa", et_empresa.getText().toString());
                    requestBody.put("email", et_correo.getText().toString());
                    requestBody.put("password", contrasena);
                    requestBody.put("confirm_password", confirmarContrasena);

                    SendRequestsForLoginOrRegister sendRequestsForLoginOrRegister = new SendRequestsForLoginOrRegister();
                    sendRequestsForLoginOrRegister.sendPostRequest(context, url, requestBody, onResponse);

                }catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        bt_inicioSesion.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent=new Intent(context, LoginActivity.class);
                startActivity(myIntent);
            }
        });


    }









}
