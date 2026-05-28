package com.example.tickets_android;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.android.volley.Request;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class DatosUsuarioActivity extends AppCompatActivity {
    private Usuario usuarioActual;

    private EditText etUsername;
    private EditText etNombre;
    private EditText etEmpresa;
    private EditText etEmail;
    private EditText etPassword;
    private Button btnGuardar;

    private String url = Conexiones.PERFIL_USUARIO;
    private String onResponse = "Datos actualizados correctamente";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);

        SharedPreferences sharedPref = getSharedPreferences("ajustes_tema", Context.MODE_PRIVATE);
        boolean isModoOscuro = sharedPref.getBoolean("modo_oscuro_activado", false);

        if (isModoOscuro) {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_datos_usuario);

        // Inicializar todas las vistas
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etUsername = findViewById(R.id.etUsername);
        etNombre = findViewById(R.id.etNombre);
        etEmpresa = findViewById(R.id.etEmpresa);
        btnGuardar = findViewById(R.id.btnGuardarCambios);

        // Cargar datos iniciales desde la BD
        obtenerDatos();

        if (btnGuardar != null) {
            btnGuardar.setOnClickListener(v -> mostrarConfirmacionEditar());
        }
    }

    private void mostrarConfirmacionEditar() {
        new AlertDialog.Builder(this)
                .setTitle("Editar perfil")
                .setMessage("¿Estás seguro de que quieres editar los datos de tu perfil?")
                .setPositiveButton("Editar", (dialog, which) -> editarDatos())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void editarDatos() {
        // Recopilamos los datos actuales de los campos de texto
        JSONObject requestBody = new JSONObject();
        try {
            // Usamos "contacto" y "empresa" que son los nombres de tu modelo en Django
            requestBody.put("username", etUsername.getText().toString().trim());
            requestBody.put("nombre", etNombre.getText().toString().trim());
            requestBody.put("empresa", etEmpresa.getText().toString().trim());
            requestBody.put("correo", etEmail.getText().toString().trim());
            requestBody.put("password", etPassword.getText().toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        // Enviamos la petición POST para actualizar el perfil
        JsonObjectRequestWithCustomAuth request = new JsonObjectRequestWithCustomAuth(
                Request.Method.PUT,
                url,
                requestBody,
                response -> {
                    Toast.makeText(this, onResponse, Toast.LENGTH_SHORT).show();
                    obtenerDatos(); // Refrescar UI
                },
                error -> {
                    String errorMsg = "Error al actualizar";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try {
                            // Convertimos la respuesta del servidor en texto para ver el error real
                            String body = new String(error.networkResponse.data, "UTF-8");
                            Log.e("API_ERROR", "Detalle del error 400: " + body);
                            errorMsg = body; // El Toast mostrará el JSON de error de Django
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    Toast.makeText(this, "Fallo: " + errorMsg, Toast.LENGTH_LONG).show();
                },
                this
        );
        Volley.newRequestQueue(this).add(request);
    }
    private void obtenerDatos() {
        JsonArrayRequestWithCustomAuth request = new JsonArrayRequestWithCustomAuth(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        if (response.length() > 0) {
                            JSONObject userJson = response.getJSONObject(0);
                            usuarioActual = new Usuario(userJson);
                            actualizarInterfazUsuario();
                        }
                    } catch (JSONException e) {
                        Log.e("ERROR", "Error al parsear el array de usuario");
                    }
                },
                error -> intentarCargarComoObjeto(url),
                this
        );
        Volley.newRequestQueue(this).add(request);
    }

    private void intentarCargarComoObjeto(String url) {
        JsonObjectRequestWithCustomAuth request = new JsonObjectRequestWithCustomAuth(
                Request.Method.GET, url, null,
                response -> {
                    usuarioActual = new Usuario(response);
                    actualizarInterfazUsuario();
                },
                error -> {
                    Log.e("API", "Error total al cargar perfil: " + error.toString());
                },
                this
        );
        Volley.newRequestQueue(this).add(request);
    }

    private void actualizarInterfazUsuario() {
        if (usuarioActual != null) {
            if (etUsername != null) etUsername.setText(usuarioActual.getUsername());
            if (etEmail != null) etEmail.setText(usuarioActual.getCorreo());
            if (etNombre != null) etNombre.setText(usuarioActual.getNombre());
            if (etEmpresa != null) etEmpresa.setText(usuarioActual.getEmpresa());
            // Ahora cargamos la contraseña real obtenida de la base de datos
            if (etPassword != null) etPassword.setText("");
        }
    }
}