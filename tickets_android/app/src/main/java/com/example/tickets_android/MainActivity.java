package com.example.tickets_android;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;
import java.nio.charset.StandardCharsets;

public class MainActivity extends AppCompatActivity {

    // IMPORTANTE: Añadimos /crear/ al final para que vaya a la función correcta de Django
    private String url = "http://192.168.0.151:8000/tickets_android/";
    private Button btnEnviar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnEnviar = findViewById(R.id.btnEnviarTicket);
        btnEnviar.setOnClickListener(v -> enviarTicket());
    }

    private void enviarTicket() {
        String n = ((EditText) findViewById(R.id.etNombre)).getText().toString().trim();
        String e = ((EditText) findViewById(R.id.etNombreEmpresa)).getText().toString().trim();
        String t = ((Spinner) findViewById(R.id.spinnerTipoDispositivo)).getSelectedItem().toString();
        String id = ((EditText) findViewById(R.id.etIdDispositivo)).getText().toString().trim();
        String d = ((EditText) findViewById(R.id.etDuda)).getText().toString().trim();

        if (n.isEmpty() || e.isEmpty() || id.isEmpty() || d.isEmpty()) {
            Toast.makeText(this, "Rellena todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        btnEnviar.setEnabled(false);
        btnEnviar.setBackgroundColor(Color.parseColor("#4CAF50"));
        btnEnviar.setText("Enviando...");

        try {
            JSONObject json = new JSONObject();
            json.put("nombre", n);
            json.put("nombre_empresa", e);
            json.put("tipo_dispositivo", t);
            json.put("id_dispositivo", id);
            json.put("duda", d);

            JsonObjectRequest request = new JsonObjectRequest(Request.Method.POST, url, json,
                    response -> {
                        Toast.makeText(this, "¡TICKET GUARDADO EN DJANGO!", Toast.LENGTH_LONG).show();
                        resetearBoton("#3B59FE", "Enviar Ticket");
                        limpiarCampos();
                    },
                    error -> {
                        btnEnviar.setEnabled(true);
                        btnEnviar.setBackgroundColor(Color.parseColor("#F44336"));
                        btnEnviar.setText("Reintentar");

                        if (error.networkResponse != null) {
                            String body = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                            Log.e("DJANGO_ERROR", body);
                            Toast.makeText(this, "Error " + error.networkResponse.statusCode + ": " + body, Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "Error de red: ¿Está el servidor encendido?", Toast.LENGTH_LONG).show();
                        }
                    }
            );

            request.setRetryPolicy(new DefaultRetryPolicy(20000, 0, 1f));
            Volley.newRequestQueue(this).add(request);

        } catch (JSONException err) { 
            err.printStackTrace();
        }
    }

    private void resetearBoton(String color, String texto) {
        btnEnviar.setEnabled(true);
        btnEnviar.setBackgroundColor(Color.parseColor(color));
        btnEnviar.setText(texto);
    }

    private void limpiarCampos() {
        ((EditText) findViewById(R.id.etNombre)).setText("");
        ((EditText) findViewById(R.id.etNombreEmpresa)).setText("");
        ((EditText) findViewById(R.id.etIdDispositivo)).setText("");
        ((EditText) findViewById(R.id.etDuda)).setText("");
    }
}
