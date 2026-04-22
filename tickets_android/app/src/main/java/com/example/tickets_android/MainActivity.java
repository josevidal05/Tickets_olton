package com.example.tickets_android;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.splashscreen.SplashScreen;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.Volley;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private final String url = "http://192.168.0.151:8000/tickets_android/";
    private Button btnEnviar;
    private TextView tvNombreArchivo;
    
    private Uri archivoUri; // Uri del archivo seleccionado (galería o archivos)
    private String currentPhotoPath; // Ruta de la foto tomada con la cámara

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    File file = new File(currentPhotoPath);
                    archivoUri = Uri.fromFile(file);
                    tvNombreArchivo.setText("Foto: " + file.getName());
                }
            });

    private final ActivityResultLauncher<String> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    validarYAsignarArchivo(uri);
                }
            });

    private final ActivityResultLauncher<String[]> documentLauncher = registerForActivityResult(
            new ActivityResultContracts.OpenDocument(),
            uri -> {
                if (uri != null) {
                    validarYAsignarArchivo(uri);
                }
            });

    private final ActivityResultLauncher<String> requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) abrirCamara();
                else Toast.makeText(this, "Permiso denegado", Toast.LENGTH_SHORT).show();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnEnviar = findViewById(R.id.btnEnviarTicket);
        Button btnSeleccionarArchivo = findViewById(R.id.btnSeleccionarArchivo);
        tvNombreArchivo = findViewById(R.id.tvNombreArchivo);

        btnEnviar.setOnClickListener(v -> enviarTicket());
        btnSeleccionarArchivo.setOnClickListener(v -> mostrarOpcionesArchivo());
    }

    private void mostrarOpcionesArchivo() {
        String[] opciones = {"Cámara", "Galería", "Archivos"};
        new AlertDialog.Builder(this)
                .setTitle("Seleccionar archivo")
                .setItems(opciones, (dialog, which) -> {
                    if (which == 0) verificarPermisoCamara();
                    else if (which == 1) galleryLauncher.launch("image/*");
                    else documentLauncher.launch(new String[]{"image/*", "application/pdf"});
                }).show();
    }

    private void validarYAsignarArchivo(Uri uri) {
        String mimeType = getContentResolver().getType(uri);
        if (mimeType != null && (mimeType.startsWith("image/") || mimeType.equals("application/pdf"))) {
            archivoUri = uri;
            tvNombreArchivo.setText("Archivo: " + uri.getLastPathSegment());
        } else {
            Toast.makeText(this, "Formato de archivo inválido", Toast.LENGTH_LONG).show();
        }
    }

    private void verificarPermisoCamara() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            abrirCamara();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void abrirCamara() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try { photoFile = createImageFile(); } catch (IOException ignored) {}
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this, "com.example.tickets_android.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                cameraLauncher.launch(takePictureIntent);
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile("JPEG_" + timeStamp + "_", ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private void enviarTicket() {
        String contacto = ((EditText) findViewById(R.id.etNombre)).getText().toString().trim();
        String empresa = ((EditText) findViewById(R.id.etNombreEmpresa)).getText().toString().trim();
        String tipo_raw = ((Spinner) findViewById(R.id.spinnerTipoDispositivo)).getSelectedItem().toString();
        String id_str = ((EditText) findViewById(R.id.etIdDispositivo)).getText().toString().trim();
        String observaciones = ((EditText) findViewById(R.id.etDuda)).getText().toString().trim();
        String portes_raw = ((Spinner) findViewById(R.id.spinnerPortes)).getSelectedItem().toString();
        String transporte = ((EditText) findViewById(R.id.etTransporte)).getText().toString().trim();

        if (contacto.isEmpty() || empresa.isEmpty() || id_str.isEmpty() || observaciones.isEmpty() || transporte.isEmpty()) {
            Toast.makeText(this, "Rellena todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        try { Integer.parseInt(id_str); } catch (NumberFormatException e) {
            Toast.makeText(this, "El ID debe ser un número", Toast.LENGTH_SHORT).show();
            return;
        }

        btnEnviar.setEnabled(false);
        btnEnviar.setText("Enviando...");

        VolleyMultipartRequest multipartRequest = new VolleyMultipartRequest(Request.Method.POST, url,
                response -> {
                    Toast.makeText(MainActivity.this, "¡TICKET GUARDADO!", Toast.LENGTH_LONG).show();
                    btnEnviar.setEnabled(true);
                    btnEnviar.setText("Enviar Ticket");
                    limpiarCampos();
                },
                error -> {
                    btnEnviar.setEnabled(true);
                    btnEnviar.setText("Reintentar");
                    if (error.networkResponse != null) {
                        String body = new String(error.networkResponse.data, StandardCharsets.UTF_8);
                        Log.e("DJANGO_ERROR", body);
                        Toast.makeText(MainActivity.this, "Error " + error.networkResponse.statusCode + ": " + body, Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(MainActivity.this, "Error de conexión", Toast.LENGTH_SHORT).show();
                    }
                }) {
            @Override
            protected Map<String, String> getParams() {
                Map<String, String> params = new HashMap<>();
                params.put("empresa", empresa);
                params.put("contacto", contacto);
                params.put("tipo_dispositivo", tipo_raw.toLowerCase().replace("á", "a"));
                params.put("id_dispositivo", id_str);
                params.put("observaciones", observaciones);
                params.put("portes", portes_raw.toLowerCase());
                params.put("transporte", transporte);
                return params;
            }

            @Override
            protected Map<String, DataPart> getByteData() {
                Map<String, DataPart> params = new HashMap<>();
                if (archivoUri != null) {
                    try {
                        byte[] fileData = getBytes(archivoUri);
                        if (fileData != null) {
                            params.put("archivo", new DataPart("upload.jpg", fileData, "image/jpeg"));
                        }
                    } catch (IOException e) { Log.e("UPLOAD", e.getMessage()); }
                }
                return params;
            }
        };

        Volley.newRequestQueue(this).add(multipartRequest);
    }

    private byte[] getBytes(Uri uri) throws IOException {
        InputStream iStream = getContentResolver().openInputStream(uri);
        if (iStream == null) return null;
        try {
            ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];
            int len;
            while ((len = iStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            return byteBuffer.toByteArray();
        } finally {
            iStream.close();
        }
    }

    private void limpiarCampos() {
        ((EditText) findViewById(R.id.etNombre)).setText("");
        ((EditText) findViewById(R.id.etNombreEmpresa)).setText("");
        ((EditText) findViewById(R.id.etIdDispositivo)).setText("");
        ((EditText) findViewById(R.id.etDuda)).setText("");
        ((EditText) findViewById(R.id.etTransporte)).setText("");
        tvNombreArchivo.setText("Ningún archivo seleccionado");
        archivoUri = null;
    }

    public static class VolleyMultipartRequest extends Request<NetworkResponse> {
        private final Response.Listener<NetworkResponse> mListener;

        public VolleyMultipartRequest(int method, String url, Response.Listener<NetworkResponse> listener, Response.ErrorListener errorListener) {
            super(method, url, errorListener);
            this.mListener = listener;
        }

        @Override
        public String getBodyContentType() { return "multipart/form-data;boundary=volley_boundary"; }

        @Override
        public byte[] getBody() throws AuthFailureError {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            try {
                Map<String, String> params = getParams();
                if (params != null) {
                    for (Map.Entry<String, String> entry : params.entrySet()) {
                        buildTextPart(bos, entry.getKey(), entry.getValue());
                    }
                }
                Map<String, DataPart> data = getByteData();
                if (data != null) {
                    for (Map.Entry<String, DataPart> entry : data.entrySet()) {
                        buildDataPart(bos, entry.getKey(), entry.getValue());
                    }
                }
                bos.write("--volley_boundary--\r\n".getBytes());
            } catch (IOException e) { Log.e("VOLLEY", e.getMessage()); }
            return bos.toByteArray();
        }

        @Override
        protected Response<NetworkResponse> parseNetworkResponse(NetworkResponse response) {
            return Response.success(response, HttpHeaderParser.parseCacheHeaders(response));
        }

        @Override
        protected void deliverResponse(NetworkResponse response) { mListener.onResponse(response); }

        protected Map<String, DataPart> getByteData() { return null; }

        private void buildTextPart(ByteArrayOutputStream bos, String name, String value) throws IOException {
            bos.write("--volley_boundary\r\n".getBytes());
            bos.write(("Content-Disposition: form-data; name=\"" + name + "\"\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            bos.write((value + "\r\n").getBytes(StandardCharsets.UTF_8));
        }

        private void buildDataPart(ByteArrayOutputStream bos, String name, DataPart data) throws IOException {
            bos.write("--volley_boundary\r\n".getBytes());
            bos.write(("Content-Disposition: form-data; name=\"" + name + "\"; filename=\"" + data.getFileName() + "\"\r\n").getBytes());
            bos.write(("Content-Type: " + data.getType() + "\r\n\r\n").getBytes());
            bos.write(data.getContent());
            bos.write("\r\n".getBytes());
        }

        public static class DataPart {
            private final String fileName;
            private final byte[] content;
            private final String type;
            public DataPart(String name, byte[] data, String type) { this.fileName = name; this.content = data; this.type = type; }
            String getFileName() { return fileName; }
            byte[] getContent() { return content; }
            String getType() { return type; }
        }
    }
}
