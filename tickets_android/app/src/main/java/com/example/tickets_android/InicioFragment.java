package com.example.tickets_android;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import com.android.volley.Request;
import com.android.volley.toolbox.Volley;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class InicioFragment extends Fragment {

    private String url = Conexiones.TICKETS_URL;
    private Button btnEnviar;
    private TextView tvNombreArchivo;
    private Uri archivoUri;
    private String currentPhotoPath;

    // Launchers para permisos y archivos (adaptados para Fragment)
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK) {
                    File file = new File(currentPhotoPath);
                    archivoUri = Uri.fromFile(file);
                    tvNombreArchivo.setText("Foto: " + file.getName());
                }
            });

    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> { if (uri != null) validarYAsignarArchivo(uri); });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Usamos el layout que tenías en la Activity
        View root = inflater.inflate(R.layout.fragment_inicio, container, false);

        btnEnviar = root.findViewById(R.id.btnEnviarTicket);
        Button btnSeleccionarArchivo = root.findViewById(R.id.btnSeleccionarArchivo);
        tvNombreArchivo = root.findViewById(R.id.tvNombreArchivo);

        btnEnviar.setOnClickListener(v -> enviarTicket(root));
        btnSeleccionarArchivo.setOnClickListener(v -> mostrarOpcionesArchivo());

        return root;
    }

    private void mostrarOpcionesArchivo() {
        String[] opciones = {"Cámara", "Galería"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Seleccionar archivo")
                .setItems(opciones, (dialog, which) -> {
                    if (which == 0) verificarPermisoCamara();
                    else galleryLauncher.launch("image/*");
                }).show();
    }

    private void validarYAsignarArchivo(Uri uri) {
        archivoUri = uri;
        tvNombreArchivo.setText("Archivo seleccionado");
    }

    private void verificarPermisoCamara() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            abrirCamara();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted -> { if (isGranted) abrirCamara(); });

    private void abrirCamara() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = null;
        try { photoFile = createImageFile(); } catch (IOException ignored) {}
        if (photoFile != null) {
            Uri photoURI = FileProvider.getUriForFile(requireContext(), "com.example.tickets_android.fileprovider", photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            cameraLauncher.launch(takePictureIntent);
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile("JPEG_" + timeStamp + "_", ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void enviarTicket(View root) {
        // Implementa aquí tu lógica de VolleyMultipartRequest igual que la tenías,
        // pero usando requireContext() en lugar de "this".
        Toast.makeText(requireContext(), "Enviando...", Toast.LENGTH_SHORT).show();
    }
}