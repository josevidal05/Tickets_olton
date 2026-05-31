package com.example.tickets_android;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import com.android.volley.Request;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class Ticket_idActivity extends AppCompatActivity {

    private String urlBase = Conexiones.TICKETS_ID;
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
    private TextView tvArchivo;
    private android.widget.ImageView ivImagen;
    private Context context = this;
    private int ticketId = -1;

    // Campos para editar foto
    private Uri nuevoArchivoUri = null;
    private String currentPhotoPath = null;
    private TextView tvDialogArchivoNombre = null;

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == android.app.Activity.RESULT_OK && currentPhotoPath != null) {
                    File file = new File(currentPhotoPath);
                    nuevoArchivoUri = Uri.fromFile(file);
                    if (tvDialogArchivoNombre != null) {
                        tvDialogArchivoNombre.setText("Foto: " + file.getName());
                    }
                }
            });

    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    nuevoArchivoUri = uri;
                    if (tvDialogArchivoNombre != null) {
                        tvDialogArchivoNombre.setText("Imagen seleccionada");
                    }
                }
            });

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), isGranted -> { if (isGranted) abrirCamara(); });

    private void abrirCamara() {
        Intent takePictureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        File photoFile = null;
        try { photoFile = createImageFile(); } catch (java.io.IOException ignored) {}
        if (photoFile != null) {
            Uri photoURI = androidx.core.content.FileProvider.getUriForFile(this, "com.example.tickets_android.fileprovider", photoFile);
            takePictureIntent.putExtra(android.provider.MediaStore.EXTRA_OUTPUT, photoURI);
            cameraLauncher.launch(takePictureIntent);
        }
    }

    private File createImageFile() throws java.io.IOException {
        String timeStamp = new java.text.SimpleDateFormat("yyyyMMdd_HHmmss", java.util.Locale.getDefault()).format(new java.util.Date());
        File storageDir = getExternalFilesDir(android.os.Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile("JPEG_" + timeStamp + "_", ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void verificarPermisoCamara() {
        if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            abrirCamara();
        } else {
            requestPermissionLauncher.launch(android.Manifest.permission.CAMERA);
        }
    }

    private void mostrarOpcionesArchivo() {
        String[] opciones = {"Cámara", "Galería"};
        new AlertDialog.Builder(this)
                .setTitle("Seleccionar nueva foto")
                .setItems(opciones, (dialog, which) -> {
                    if (which == 0) verificarPermisoCamara();
                    else galleryLauncher.launch("image/*");
                }).show();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ticket_id);

        // Inicializar vistas
        tvTitulo = findViewById(R.id.tvTituloDetalle);
        tvId = findViewById(R.id.tvDetalleId);
        tvEmpresa = findViewById(R.id.tvDetalleEmpresa);
        tvContacto = findViewById(R.id.tvDetalleContacto);
        tvTipoDispositivo = findViewById(R.id.tvDetalleTipoDispositivo);
        tvIdDispositivo = findViewById(R.id.tvDetalleIdDispositivo);
        tvObservaciones = findViewById(R.id.tvDetalleObservaciones);
        tvPortes = findViewById(R.id.tvDetallePortes);
        tvTransporte = findViewById(R.id.tvDetalleTransporte);
        tvFecha = findViewById(R.id.tvDetalleFecha);
        tvArchivo = findViewById(R.id.tvDetalleArchivo);
        ivImagen = findViewById(R.id.ivDetalleImagen);
        btnEditar = findViewById(R.id.btnEditarTicket);
        btnEliminar = findViewById(R.id.btnEliminarTicket);

        // Obtener ID del ticket desde el Intent
        ticketId = getIntent().getIntExtra("ticket_id", -1);
        if (ticketId == -1) {
            Toast.makeText(this, "Error: No se especificó el ID del ticket", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Cargar detalles del ticket
        obtenerDetallesTicket();

        // Configurar acciones de botones
        if (btnEliminar != null) {
            btnEliminar.setOnClickListener(v -> mostrarConfirmacionEliminar());
        }

        if (btnEditar != null) {
            btnEditar.setOnClickListener(v -> mostrarDialogoEditar());
        }
    }

    private void obtenerDetallesTicket() {
        String url = urlBase.replace("<int:ticket_id>", String.valueOf(ticketId));

        JsonObjectRequestWithCustomAuth request = new JsonObjectRequestWithCustomAuth(
                Request.Method.GET,
                url,
                null,
                response -> {
                    try {
                        int id = response.optInt("id", ticketId);
                        String empresa = response.optString("empresa", "");
                        String contacto = response.optString("contacto", "");
                        String tipoDisp = response.optString("tipo_dispositivo", "");
                        int idDisp = response.optInt("id_dispositivo", 0);
                        String observaciones = response.optString("observaciones", "");
                        String portes = response.optString("portes", "");
                        String transporte = response.optString("empresa_transporte", "");
                        String fecha = response.optString("fecha_creacion", "");
                        String archivo = response.optString("archivo", "");
                        String archivoRelUrl = response.optString("archivo_url", "");
                        // Construir URL absoluta usando el BASE_URL de la app, ya que el backend
                        // puede devolver una URL con host incorrecto para el emulador (127.0.0.1 vs 10.0.2.2)
                        String archivoUrl = "";
                        if (archivoRelUrl != null && !archivoRelUrl.isEmpty() && !archivoRelUrl.equals("null")) {
                            if (archivoRelUrl.startsWith("http")) {
                                archivoUrl = archivoRelUrl; // Ya es absoluta
                            } else {
                                // Es relativa (ej: /media/tickets/imagen.jpg)
                                archivoUrl = Conexiones.BASE_URL + archivoRelUrl;
                            }
                        }
 
                        if (tvTitulo != null) tvTitulo.setText("Detalle del Ticket #" + id);
                        if (tvId != null) tvId.setText(String.valueOf(id));
                        if (tvEmpresa != null) tvEmpresa.setText(empresa);
                        if (tvContacto != null) tvContacto.setText(contacto);
                        if (tvTipoDispositivo != null) tvTipoDispositivo.setText(tipoDisp);
                        if (tvIdDispositivo != null) tvIdDispositivo.setText(String.valueOf(idDisp));
                        if (tvObservaciones != null) tvObservaciones.setText(observaciones);
                        if (tvPortes != null) tvPortes.setText(portes);
                        if (tvTransporte != null) tvTransporte.setText(transporte);
                        if (tvFecha != null) tvFecha.setText(Ticket.formatearFechaSoloDia(fecha));
                        if (tvArchivo != null) {
                            if (archivo != null && !archivo.isEmpty() && !archivo.equals("null")) {
                                tvArchivo.setText("Archivo: " + archivo);
                            } else {
                                tvArchivo.setText("Sin archivo adjunto");
                            }
                        }

                        if (ivImagen != null) {
                            cargarImagen(archivoUrl, ivImagen);
                        }

                    } catch (Exception e) {
                        Log.e("API", "Error al parsear detalles del ticket", e);
                        Toast.makeText(context, "Error al mostrar datos del ticket", Toast.LENGTH_SHORT).show();
                    }
                },
                error -> {
                    Log.e("API", "Error al obtener ticket: " + error.toString());
                    Toast.makeText(context, "Error al conectar con el servidor", Toast.LENGTH_SHORT).show();
                },
                this
        );

        Volley.newRequestQueue(this).add(request);
    }

    private void mostrarConfirmacionEliminar() {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar Ticket")
                .setMessage("¿Estás seguro de que quieres eliminar este ticket de forma permanente?")
                .setPositiveButton("Eliminar", (dialog, which) -> eliminarTicket())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void eliminarTicket() {
        String url = urlBase.replace("<int:ticket_id>", String.valueOf(ticketId));

        JsonObjectRequestWithCustomAuth request = new JsonObjectRequestWithCustomAuth(
                Request.Method.DELETE,
                url,
                null,
                response -> {
                    Toast.makeText(context, "Ticket eliminado con éxito", Toast.LENGTH_SHORT).show();
                    finish();
                },
                error -> {
                    Log.e("API", "Error al eliminar ticket: " + error.toString());
                    Toast.makeText(context, "Error al eliminar el ticket", Toast.LENGTH_SHORT).show();
                },
                this
        );

        Volley.newRequestQueue(this).add(request);
    }

    private void mostrarDialogoEditar() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Editar Ticket");

        View view = LayoutInflater.from(this).inflate(R.layout.dialog_editar_ticket, null);
        builder.setView(view);

        EditText etIdDisp = view.findViewById(R.id.dialog_etIdDispositivo);
        EditText etObservaciones = view.findViewById(R.id.dialog_etObservaciones);
        EditText etTransporte = view.findViewById(R.id.dialog_etTransporte);
        Spinner spTipo = view.findViewById(R.id.dialog_spTipoDispositivo);
        Spinner spPortes = view.findViewById(R.id.dialog_spPortes);

        Button btnSeleccionar = view.findViewById(R.id.dialog_btnSeleccionarArchivo);
        tvDialogArchivoNombre = view.findViewById(R.id.dialog_tvNombreArchivo);
        nuevoArchivoUri = null; // Resetear selección anterior

        if (btnSeleccionar != null) {
            btnSeleccionar.setOnClickListener(v -> mostrarOpcionesArchivo());
        }

        // Pre-rellenar con datos actuales
        if (etIdDisp != null && tvIdDispositivo != null) etIdDisp.setText(tvIdDispositivo.getText().toString());
        if (etObservaciones != null && tvObservaciones != null) etObservaciones.setText(tvObservaciones.getText().toString());
        if (etTransporte != null && tvTransporte != null) etTransporte.setText(tvTransporte.getText().toString());

        // Configurar spinners de acuerdo a los valores actuales
        if (spTipo != null && tvTipoDispositivo != null) {
            String tipoActual = tvTipoDispositivo.getText().toString().toLowerCase();
            if (tipoActual.contains("maquina")) spTipo.setSelection(0);
            else if (tipoActual.contains("tracker")) spTipo.setSelection(1);
            else spTipo.setSelection(2);
        }

        if (spPortes != null && tvPortes != null) {
            String portesActual = tvPortes.getText().toString().toLowerCase();
            if (portesActual.contains("pagado")) spPortes.setSelection(0);
            else spPortes.setSelection(1);
        }

        builder.setPositiveButton("Guardar", (dialog, which) -> {
            String idDispStr = etIdDisp.getText().toString().trim();
            int idDisp = idDispStr.isEmpty() ? 0 : Integer.parseInt(idDispStr);
            String obs = etObservaciones.getText().toString().trim();
            String transp = etTransporte.getText().toString().trim();
            String tipo = spTipo.getSelectedItem().toString();
            String portes = spPortes.getSelectedItem().toString();

            actualizarTicket(idDisp, obs, transp, tipo, portes);
        });

        builder.setNegativeButton("Cancelar", null);
        builder.show();
    }

    private void actualizarTicket(int idDisp, String observaciones, String transporte, String tipo, String portes) {
        String url = urlBase.replace("<int:ticket_id>", String.valueOf(ticketId));

        if (nuevoArchivoUri != null) {
            actualizarTicketConImagen(url, idDisp, observaciones, transporte, tipo, portes);
        } else {
            actualizarTicketSinImagen(url, idDisp, observaciones, transporte, tipo, portes);
        }
    }

    private void actualizarTicketSinImagen(String url, int idDisp, String observaciones, String transporte, String tipo, String portes) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("id_dispositivo", idDisp);
            jsonBody.put("observaciones", observaciones);
            jsonBody.put("empresa_transporte", transporte);
            jsonBody.put("tipo_dispositivo", tipo);
            jsonBody.put("portes", portes);
            jsonBody.put("empresa", tvEmpresa.getText().toString());
            jsonBody.put("contacto", tvContacto.getText().toString());
        } catch (JSONException e) {
            e.printStackTrace();
        }

        JsonObjectRequestWithCustomAuth request = new JsonObjectRequestWithCustomAuth(
                Request.Method.PUT,
                url,
                jsonBody,
                response -> {
                    Toast.makeText(context, "Ticket actualizado con éxito", Toast.LENGTH_SHORT).show();
                    obtenerDetallesTicket(); // Refrescar vista
                },
                error -> {
                    Log.e("API", "Error al actualizar ticket: " + error.toString());
                    Toast.makeText(context, "Error al actualizar el ticket", Toast.LENGTH_SHORT).show();
                },
                this
        );

        Volley.newRequestQueue(this).add(request);
    }

    private void actualizarTicketConImagen(String url, int idDisp, String observaciones, String transporte, String tipo, String portes) {
        String boundary = "----FormBoundary" + System.currentTimeMillis();

        com.android.volley.toolbox.StringRequest multipartRequest = new com.android.volley.toolbox.StringRequest(
                Request.Method.POST, url,
                response -> {
                    Toast.makeText(context, "Ticket actualizado con nueva imagen", Toast.LENGTH_SHORT).show();
                    nuevoArchivoUri = null;
                    obtenerDetallesTicket(); // Refrescar vista
                },
                error -> {
                    String errorMsg = "Error al actualizar con imagen";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try { errorMsg = new String(error.networkResponse.data, "UTF-8"); } catch (Exception ignored) {}
                    }
                    Log.e("UPLOAD_ERROR", errorMsg);
                    Toast.makeText(context, "Fallo: " + errorMsg, Toast.LENGTH_LONG).show();
                }
        ) {
            @Override
            public String getBodyContentType() {
                return "multipart/form-data; boundary=" + boundary;
            }

            @Override
            public Map<String, String> getHeaders() {
                Map<String, String> headers = new HashMap<>();
                headers.put("Content-Type", "multipart/form-data; boundary=" + boundary);
                android.content.SharedPreferences prefs = getSharedPreferences("SESSIONS_APP_PREFS", android.content.Context.MODE_PRIVATE);
                String token = prefs.getString("VALID_TOKEN", "");
                if (!token.isEmpty()) headers.put("Session", token);
                return headers;
            }

            @Override
            public byte[] getBody() {
                try {
                    android.content.ContentResolver cr = getContentResolver();
                    String mimeType = cr.getType(nuevoArchivoUri);
                    if (mimeType == null) mimeType = "image/jpeg";
                    String extension = mimeType.contains("png") ? ".png" : ".jpg";
                    String fileName = "imagen_ticket" + extension;

                    Log.d("UPLOAD_DEBUG", "Iniciando lectura y compresión de archivo para actualizar. URI: " + nuevoArchivoUri);

                    byte[] fileBytes = obtenerBytesImagenComprimida(nuevoArchivoUri);
                    if (fileBytes == null) {
                        Log.e("UPLOAD_DEBUG", "No se pudo obtener o comprimir la imagen");
                        return null;
                    }

                    java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();

                    // Campos de texto
                    String[] fields = {"tipo_dispositivo", "portes", "observaciones", "transporte", "id_dispositivo", "empresa", "contacto"};
                    String[] values = {tipo, portes, observaciones, transporte, String.valueOf(idDisp), tvEmpresa.getText().toString(), tvContacto.getText().toString()};

                    for (int i = 0; i < fields.length; i++) {
                        bos.write(("--" + boundary + "\r\n").getBytes());
                        bos.write(("Content-Disposition: form-data; name=\"" + fields[i] + "\"\r\n\r\n").getBytes());
                        bos.write((values[i] + "\r\n").getBytes());
                    }

                    // Campo de archivo
                    bos.write(("--" + boundary + "\r\n").getBytes());
                    bos.write(("Content-Disposition: form-data; name=\"archivo\"; filename=\"" + fileName + "\"\r\n").getBytes());
                    bos.write(("Content-Type: " + mimeType + "\r\n\r\n").getBytes());

                    bos.write(fileBytes);
                    Log.d("UPLOAD_DEBUG", "Bytes de archivo comprimido escritos: " + fileBytes.length);

                    bos.write(("\r\n--" + boundary + "--\r\n").getBytes());

                    byte[] requestBody = bos.toByteArray();
                    Log.d("UPLOAD_DEBUG", "Multipart construido con éxito. Total bytes request body: " + requestBody.length);
                    return requestBody;

                } catch (Exception e) {
                    Log.e("UPLOAD_ERROR", "Error construyendo multipart para actualizar: " + e.getMessage(), e);
                    return null;
                }
            }
        };

        Volley.newRequestQueue(this).add(multipartRequest);
    }

    private byte[] obtenerBytesImagenComprimida(Uri uri) {
        try {
            android.content.ContentResolver cr = getContentResolver();
            java.io.InputStream is = cr.openInputStream(uri);
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(is);
            if (is != null) is.close();

            if (bitmap == null) return null;

            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            // Comprimir a JPEG con calidad 80 (suficiente para verse nítido y pesar muy poco)
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, bos);
            byte[] bytes = bos.toByteArray();
            bitmap.recycle();
            return bytes;
        } catch (Exception e) {
            Log.e("COMPRESS_ERROR", "Error al comprimir la imagen: " + e.getMessage());
            return null;
        }
    }

    private void cargarImagen(String urlImagen, android.widget.ImageView imageView) {
        if (urlImagen == null || urlImagen.isEmpty() || urlImagen.equals("null")) {
            imageView.setVisibility(View.GONE);
            return;
        }

        new Thread(() -> {
            try {
                java.net.URL url = new java.net.URL(urlImagen);
                java.net.HttpURLConnection connection = (java.net.HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.connect();
                java.io.InputStream input = connection.getInputStream();
                android.graphics.Bitmap myBitmap = android.graphics.BitmapFactory.decodeStream(input);
                
                runOnUiThread(() -> {
                    if (myBitmap != null) {
                        imageView.setImageBitmap(myBitmap);
                        imageView.setVisibility(View.VISIBLE);
                    } else {
                        imageView.setVisibility(View.GONE);
                    }
                });
            } catch (Exception e) {
                Log.e("IMAGE_LOAD", "Error cargando imagen: " + e.getMessage());
                runOnUiThread(() -> imageView.setVisibility(View.GONE));
            }
        }).start();
    }
}