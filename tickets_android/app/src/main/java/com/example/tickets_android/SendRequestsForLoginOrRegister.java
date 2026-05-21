package com.example.tickets_android;

import static android.content.Context.MODE_PRIVATE;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
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
                        Log.d("API_SUCCESS", "Respuesta recibida: " + response.toString());
                        try {
                            // Guardar Token
                            String receivedToken = response.getString("token");
                            SharedPreferences preferences = context.getSharedPreferences("SESSIONS_APP_PREFS", MODE_PRIVATE);
                            SharedPreferences.Editor editor = preferences.edit();
                            editor.putString("VALID_TOKEN", receivedToken);
                            editor.apply();

                            // Intentar obtener datos del usuario de la respuesta
                            // Fallback al requestBody si no están en la respuesta
                            String nombreRecibido = response.optString("nombreUsuario", 
                                    response.optString("username", 
                                            requestBody.optString("username", "Usuario")));
                            
                            String empresaRecibida = response.optString("empresa", 
                                    requestBody.optString("empresa", ""));

                            // Si el servidor devuelve un objeto "user", buscamos dentro
                            if (response.has("user")) {
                                JSONObject userObj = response.getJSONObject("user");
                                if (nombreRecibido.equals("Usuario")) {
                                    nombreRecibido = userObj.optString("username", nombreRecibido);
                                }
                                if (empresaRecibida.isEmpty()) {
                                    empresaRecibida = userObj.optString("empresa", empresaRecibida);
                                }
                            }

                            SharedPreferences userPrefs = context.getSharedPreferences("sesion_usuario", MODE_PRIVATE);
                            SharedPreferences.Editor userEditor = userPrefs.edit();
                            userEditor.putString("key_nombre", nombreRecibido);
                            userEditor.putString("key_empresa", empresaRecibida);
                            userEditor.apply();

                            Toast.makeText(context, onResponse, Toast.LENGTH_SHORT).show();

                            // Navegar a la siguiente pantalla
                            Intent myIntent = new Intent(context, MainActivity.class);
                            myIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            context.startActivity(myIntent);

                        } catch (JSONException e) {
                            Log.e("API_ERROR", "Error al parsear JSON exitoso", e);
                            Toast.makeText(context, "Error en los datos recibidos del servidor", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (error.networkResponse != null) {
                            int statusCode = error.networkResponse.statusCode;
                            String data = new String(error.networkResponse.data);

                            Log.e("API_ERROR", "Código de estado: " + statusCode);
                            Log.e("API_ERROR", "Respuesta del servidor: " + data);

                            try {
                                JSONObject jsonObject = new JSONObject(data);
                                String errorMsg = jsonObject.optString("error", "Error en la solicitud");
                                Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show();
                            } catch (JSONException e) {
                                Toast.makeText(context, "Error " + statusCode + ": Servidor no disponible", Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(context, "No se pudo conectar con el servidor. Revisa tu conexión.", Toast.LENGTH_SHORT).show();
                        }
                    }
                }
        );
        queue.add(request);
    }
}
