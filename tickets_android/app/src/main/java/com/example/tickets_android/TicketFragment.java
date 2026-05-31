package com.example.tickets_android;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;

public class TicketFragment extends Fragment {

    private String url = Conexiones.TICKET_USUARIO;
    private RecyclerView recyclerView;
    private Context context;
    private TicketList ticketlists;
    private TicketAdapter adapter;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tickets, container, false);
        context = requireContext();

        recyclerView = view.findViewById(R.id.rv_tickets);

        // Configuramos el RecyclerView con un LayoutManager lineal (vertical)
        recyclerView.setLayoutManager(new LinearLayoutManager(context));

        cargarTickets();

        return view;
    }

    private void cargarTickets() {
        RequestQueue queue = Volley.newRequestQueue(context);

        // Usamos JsonArrayRequestWithCustomAuth para incluir automáticamente el token de sesión en las cabeceras
        JsonArrayRequestWithCustomAuth jsonArrayRequest = new JsonArrayRequestWithCustomAuth(
                Request.Method.GET,
                url,
                null,
                new Response.Listener<JSONArray>() {
                    @Override
                    public void onResponse(JSONArray response) {
                        try {
                            // Convertimos el JSONArray del servidor en nuestra lista de objetos Ticket
                            ticketlists = new TicketList(response);

                            // Si el adaptador no existe, lo creamos; si existe, lo actualizamos
                            if (adapter == null) {
                                adapter = new TicketAdapter(ticketlists.getTicketList());
                                recyclerView.setAdapter(adapter);
                            } else {
                                adapter.updateTickets(ticketlists.getTicketList());
                            }

                            // Configurar el click listener
                            adapter.setOnItemClickListener(new TicketAdapter.OnItemClickListener() {
                                @Override
                                public void onItemClick(Ticket ticket) {
                                    Intent intent = new Intent(context, Ticket_idActivity.class);
                                    intent.putExtra("ticket_id", ticket.getId());
                                    startActivity(intent);
                                }
                            });

                        } catch (Exception e) {
                            e.printStackTrace();
                            Toast.makeText(context, "Error al procesar los datos de tickets", Toast.LENGTH_SHORT).show();
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // Manejo de errores basado en los códigos de estado de tu views.py
                        String message = "Error al conectar con el servidor";
                        if (error.networkResponse != null) {
                            int statusCode = error.networkResponse.statusCode;
                            if (statusCode == 401) {
                                message = "Sesión inválida. Por favor, inicia sesión de nuevo.";
                            } else if (statusCode == 405) {
                                message = "Método HTTP no soportado.";
                            } else if (statusCode == 404) {
                                message = "No se encontraron tickets.";
                            }
                        }
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
                    }
                },
                context // Pasamos el contexto para que CustomAuth pueda leer las SharedPreferences
        );

        // Añadimos la petición a la cola de Volley
        queue.add(jsonArrayRequest);
    }
}