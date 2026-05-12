package com.example.jadmusic;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

public class SendRequestsForLoginOrRegister {

    public void sendPostRequest(Context context, String url, JSONObject requestBody, String onResponse) {

        RequestQueue queue = Volley.newRequestQueue(context);

        JsonObjectRequest request = new JsonObjectRequest(
                Request.Method.POST,
                url,
                requestBody,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        Toast.makeText(context, onResponse, Toast.LENGTH_SHORT).show();

                        try {

                            String receivedToken = response.getString("token");

                            SharedPreferences preferences = context.getSharedPreferences("SESSIONS_APP_PREFS", MODE_PRIVATE);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putString("VALID_TOKEN", receivedToken);
                            editor.apply();

                            String nombreRecibido = response.optString("nombreUsuario", "Usuario");
                            String emailRecibido = response.optString("email", "sin_correo@jadmusic.com");

                            SharedPreferences userPrefs = context.getSharedPreferences("sesion_usuario", MODE_PRIVATE);
                            SharedPreferences.Editor userEditor = userPrefs.edit();
                            userEditor.putString("key_nombre", nombreRecibido);
                            userEditor.putString("key_email", emailRecibido);
                            userEditor.apply();

                        } catch (JSONException e) {
                            e.printStackTrace();
                            Toast.makeText(context, "Error al leer datos del servidor", Toast.LENGTH_SHORT).show();
                        }

                        Intent myIntent = new Intent(context, MainActivity.class);
                        myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        context.startActivity(myIntent);
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (error.networkResponse != null) {
                            String jsonResponse = new String(error.networkResponse.data);
                            try {
                                JSONObject jsonObject = new JSONObject(jsonResponse);
                                String errorMsg = jsonObject.optString("error", "Error en la solicitud");
                                Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show();

                            } catch (JSONException e) {
                                e.printStackTrace();
                                Toast.makeText(context, "Error al procesar la respuesta", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(context, "Fallo de conexión con el servidor", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
        queue.add(request);
    }
}