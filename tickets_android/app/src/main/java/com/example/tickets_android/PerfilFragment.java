package com.example.tickets_android;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.toolbox.Volley;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.json.JSONException;
import org.json.JSONObject;

public class PerfilFragment extends Fragment {

    private Usuario usuarioActual;
    private TextView tvLogout;
    private TextView tvNombreUsuario;
    private TextView tvPerfilCorreo;
    private TextView tvMisDatos;
    private TextView tvMisTickets;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        obtenerDatosUsuario();
    }

    private void obtenerDatosUsuario() {
        String urlPerfil = Conexiones.PERFIL_USUARIO;

        JsonArrayRequestWithCustomAuth request = new JsonArrayRequestWithCustomAuth(
                Request.Method.GET,
                urlPerfil,
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
                error -> intentarCargarComoObjeto(urlPerfil),
                requireContext()
        );
        Volley.newRequestQueue(requireContext()).add(request);
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
                requireContext()
        );
        Volley.newRequestQueue(requireContext()).add(request);
    }

    private void actualizarInterfazUsuario() {
        if (getView() != null && usuarioActual != null) {
            // Corregido: Usar TextView en lugar de EditText
            if (tvNombreUsuario != null) tvNombreUsuario.setText(usuarioActual.getNombre());
            if (tvPerfilCorreo != null) tvPerfilCorreo.setText(usuarioActual.getCorreo());
        }
    }

    private void cerrarSesion() {
        // 1. Llamar al endpoint de logout para invalidar el token en el servidor
        String logoutUrl = Conexiones.LOGOUT;
        JsonObjectRequestWithCustomAuth logoutRequest = new JsonObjectRequestWithCustomAuth(
                com.android.volley.Request.Method.POST, logoutUrl, null,
                response -> {}, // ignoramos la respuesta
                error -> {},    // ignoramos el error (de todas formas limpiamos sesión local)
                requireContext()
        );
        Volley.newRequestQueue(requireContext()).add(logoutRequest);

        // 2. Limpiar todas las SharedPreferences de sesión
        requireContext().getSharedPreferences("sesion_usuario", Context.MODE_PRIVATE)
                .edit().clear().apply();
        requireContext().getSharedPreferences("SESSIONS_APP_PREFS", Context.MODE_PRIVATE)
                .edit().clear().apply();

        // 3. Ir a LoginActivity limpiando el back stack (no se puede volver atrás)
        Intent intent = new Intent(requireContext(), LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }

    private void irAMisDatos(){
        if (usuarioActual != null) {
            Intent intent = new Intent(requireContext(), DatosUsuarioActivity.class);
            // Mandamos los datos del usuario logueado a la nueva Activity
            intent.putExtra("id", usuarioActual.getId());
            intent.putExtra("username", usuarioActual.getUsername());
            intent.putExtra("nombre", usuarioActual.getNombre());
            intent.putExtra("correo", usuarioActual.getCorreo());
            intent.putExtra("empresa", usuarioActual.getEmpresa());
            startActivity(intent);
        } else {
            Toast.makeText(requireContext(), "Datos de usuario no cargados aún", Toast.LENGTH_SHORT).show();
        }
    }

    private void irAMisTickets(){
        if (getActivity() != null) {
            // Buscamos el BottomNavigationView de la MainActivity para cambiar de fragmento
            BottomNavigationView nav = getActivity().findViewById(R.id.bottomNavigation);
            if (nav != null) {
                // Seleccionamos el item de tickets para activar el listener de la MainActivity
                nav.setSelectedItemId(R.id.tickets);
            }
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_perfil, container, false);

        tvNombreUsuario = root.findViewById(R.id.tvNombreUsuario);
        tvPerfilCorreo = root.findViewById(R.id.tvPerfilCorreo);
        tvLogout = root.findViewById(R.id.btnCerrarSesion);
        tvMisDatos = root.findViewById(R.id.btnMisDatos);
        tvMisTickets = root.findViewById(R.id.btnMisTickets);

        // Pre-rellenar con los datos que tenemos en sesión
        SharedPreferences userPrefs = requireContext().getSharedPreferences("sesion_usuario", Context.MODE_PRIVATE);
        if (tvNombreUsuario != null) {
            tvNombreUsuario.setText(userPrefs.getString("key_nombre", "Usuario"));
        }

        if (tvPerfilCorreo != null) {
            tvPerfilCorreo.setText(userPrefs.getString("key_correo", "Correo"));
        }

        tvMisDatos.setOnClickListener(v -> irAMisDatos());
        tvMisTickets.setOnClickListener(v -> irAMisTickets());

        if (tvLogout != null) {
            tvLogout.setOnClickListener(v -> cerrarSesion());
        }

        return root;
    }
}
