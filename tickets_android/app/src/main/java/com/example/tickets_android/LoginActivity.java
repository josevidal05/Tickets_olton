package com.example.tickets_android;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import org.json.JSONException;
import org.json.JSONObject;

public class LoginActivity extends AppCompatActivity {

    private EditText editTextUsuario;
    private EditText editTextContrasena;
    private Button registerButton;
    private Button sessionButton;
    private Context context = this;
    
    // Ahora usamos la constante de Conexiones
    private String url = Conexiones.LOGIN_URL;
    private String onResponse = "Incio de sesión correcto";

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
        setContentView(R.layout.activity_login);

        editTextUsuario = findViewById(R.id.inisesusuario);
        editTextContrasena = findViewById(R.id.inisescontrasena);
        registerButton = findViewById(R.id.regboton);
        sessionButton = findViewById(R.id.sesboton);

        sessionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JSONObject requestBody = new JSONObject();
                try {
                    requestBody.put("username", editTextUsuario.getText().toString());
                    requestBody.put("password", editTextContrasena.getText().toString());

                    SendRequestsForLoginOrRegister sendRequestsForLoginOrRegister = new SendRequestsForLoginOrRegister();
                    sendRequestsForLoginOrRegister.sendPostRequest(context, url, requestBody, onResponse);
                } catch (JSONException e) {
                    throw new RuntimeException(e);
                }
            }
        });
        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent=new Intent(context, RegistroActivity.class);
                startActivity(myIntent);
            }
        });
    }
}
