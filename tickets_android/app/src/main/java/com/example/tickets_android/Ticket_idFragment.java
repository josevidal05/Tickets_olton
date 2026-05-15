package com.example.tickets_android;

import static androidx.core.content.ContentProviderCompat.requireContext;

import static java.security.AccessController.getContext;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class Ticket_idFragment extends AppCompatActivity {

    private String url = Conexiones.TICKETS_ID;
    // Esta url es para cuando el servidor se arranque para un movil físico
    private Button btnEditar;
    private Button btnEliminar;
    private TextView tvTitulo;
    private TextView tvId;
    private TextView tvEmpresa;
    private TextView tvContacto;
    private TextView tvTipoDispositivo;
    private TextView tvIdDispositivo;
    private TextView tvObservaciones;
    private TextView tvPortes;
    private TextView tvTransporte;
    private TextView tvFecha;
    private Context context;
    private RecyclerView recyclerView;




    private Uri archivoUri; // Uri del archivo seleccionado (galería o archivos)
    private String currentPhotoPath; // Ruta de la foto tomada con la cámara





}