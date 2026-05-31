package com.example.tickets_android;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
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
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

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
    private Usuario usuarioActual;

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        obtenerDatosUsuario(); // Llamamos a la base de datos al cargar
    }

    private void obtenerDatosUsuario() {
        String urlPerfil = Conexiones.PERFIL_USUARIO;

        // Intentamos obtener el perfil. Si tu Django devuelve una lista [ {...} ],
        // usamos JsonArrayRequestWithCustomAuth
        JsonArrayRequestWithCustomAuth request = new JsonArrayRequestWithCustomAuth(
                Request.Method.GET, urlPerfil, null,
                response -> {
                    try {
                        if (response.length() > 0) {
                            // Cogemos el primer usuario de la lista
                            JSONObject userJson = response.getJSONObject(0);
                            usuarioActual = new Usuario(userJson);
                            actualizarInterfazUsuario();
                        }
                    } catch (JSONException e) {
                        Log.e("ERROR", "Error al parsear el array de usuario");
                    }
                },
                error -> {
                    // Si falla como Array, intentamos como Objeto único por si acaso
                    intentarCargarComoObjeto(urlPerfil);
                },
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
                    Toast.makeText(requireContext(), "Error: No se pudo conectar con el perfil en la BD", Toast.LENGTH_SHORT).show();
                },
                requireContext()
        );
        Volley.newRequestQueue(requireContext()).add(request);
    }

    private void actualizarInterfazUsuario() {
        if (getView() != null && usuarioActual != null) {
            EditText etEmpresa = getView().findViewById(R.id.etNombreEmpresa);
            EditText etNombre = getView().findViewById(R.id.etNombre);

            if (etEmpresa != null) etEmpresa.setText(usuarioActual.getEmpresa());
            if (etNombre != null) etNombre.setText(usuarioActual.getUsername());
        }
    }
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

        // Pre-rellenar empresa y contacto con los datos de sesión y bloquear edición
        SharedPreferences userPrefs = requireContext().getSharedPreferences("sesion_usuario", Context.MODE_PRIVATE);
        EditText etEmpresa = root.findViewById(R.id.etNombreEmpresa);
        EditText etNombre = root.findViewById(R.id.etNombre);

        etEmpresa.setText(userPrefs.getString("key_empresa", ""));
        etNombre.setText(userPrefs.getString("key_nombre", ""));

        etEmpresa.setEnabled(false);
        etNombre.setEnabled(false);

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
        if (usuarioActual == null) {
            obtenerDatosUsuario();
            Toast.makeText(requireContext(), "Cargando datos de perfil...", Toast.LENGTH_SHORT).show();
            return;
        }

        // Referencias a los campos
        EditText etIdDisp = root.findViewById(R.id.etIdDispositivo);
        EditText etDuda = root.findViewById(R.id.etDuda);
        EditText etTransporte = root.findViewById(R.id.etTransporte);
        Spinner spTipo = root.findViewById(R.id.spinnerTipoDispositivo);
        Spinner spPortes = root.findViewById(R.id.spinnerPortes);

        String observaciones = etDuda.getText().toString().trim();
        String transporte = etTransporte.getText().toString().trim();

        if (observaciones.isEmpty()) {
            etDuda.setError("Este campo es obligatorio");
            return;
        }
        if (transporte.isEmpty()) {
            etTransporte.setError("Este campo es obligatorio");
            return;
        }

        String tipoStr = spTipo.getSelectedItem().toString();
        String portesStr = spPortes.getSelectedItem().toString();
        String idStr = etIdDisp.getText().toString().trim();
        String idDispositivo = idStr.isEmpty() ? "0" : idStr;

        // Si hay imagen seleccionada → multipart/form-data
        // Si no hay imagen → JSON normal
        if (archivoUri != null) {
            enviarConImagen(observaciones, transporte, tipoStr, portesStr, idDispositivo, etIdDisp, etDuda, etTransporte);
        } else {
            enviarSinImagen(observaciones, transporte, tipoStr, portesStr, idDispositivo, etIdDisp, etDuda, etTransporte);
        }
    }

    private void enviarSinImagen(String observaciones, String transporte, String tipoStr,
                                  String portesStr, String idDispositivo,
                                  EditText etIdDisp, EditText etDuda, EditText etTransporte) {
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("tipo_dispositivo", tipoStr);
            jsonBody.put("portes", portesStr);
            jsonBody.put("observaciones", observaciones);
            jsonBody.put("transporte", transporte);
            jsonBody.put("id_dispositivo", idDispositivo.isEmpty() ? 0 : Integer.parseInt(idDispositivo));
            jsonBody.put("estado", "no leido");
            Log.d("APP_DEBUG", "JSON enviado (sin imagen): " + jsonBody.toString());
        } catch (Exception e) {
            Log.e("APP_ERROR", "Error al crear JSON: " + e.getMessage());
        }

        JsonObjectRequestWithCustomAuth request = new JsonObjectRequestWithCustomAuth(
                com.android.volley.Request.Method.POST, url, jsonBody,
                response -> {
                    Toast.makeText(requireContext(), "Ticket enviado con éxito", Toast.LENGTH_SHORT).show();
                    etIdDisp.setText(""); etDuda.setText(""); etTransporte.setText("");
                    archivoUri = null;
                    tvNombreArchivo.setText("");
                },
                error -> {
                    String errorMsg = "Error al enviar";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try { errorMsg = new String(error.networkResponse.data, "UTF-8"); } catch (Exception ignored) {}
                    }
                    Toast.makeText(requireContext(), "Fallo: " + errorMsg, Toast.LENGTH_LONG).show();
                },
                requireContext()
        );
        Volley.newRequestQueue(requireContext()).add(request);
    }

    private void enviarConImagen(String observaciones, String transporte, String tipoStr,
                                  String portesStr, String idDispositivo,
                                  EditText etIdDisp, EditText etDuda, EditText etTransporte) {
        String boundary = "----FormBoundary" + System.currentTimeMillis();

        com.android.volley.toolbox.StringRequest multipartRequest = new com.android.volley.toolbox.StringRequest(
                com.android.volley.Request.Method.POST, url,
                response -> {
                    Toast.makeText(requireContext(), "Ticket con imagen enviado con éxito", Toast.LENGTH_SHORT).show();
                    etIdDisp.setText(""); etDuda.setText(""); etTransporte.setText("");
                    archivoUri = null;
                    tvNombreArchivo.setText("");
                },
                error -> {
                    String errorMsg = "Error al enviar con imagen";
                    if (error.networkResponse != null && error.networkResponse.data != null) {
                        try { errorMsg = new String(error.networkResponse.data, "UTF-8"); } catch (Exception ignored) {}
                    }
                    Log.e("UPLOAD_ERROR", errorMsg);
                    Toast.makeText(requireContext(), "Fallo: " + errorMsg, Toast.LENGTH_LONG).show();
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
                android.content.SharedPreferences prefs = requireContext()
                        .getSharedPreferences("SESSIONS_APP_PREFS", android.content.Context.MODE_PRIVATE);
                String token = prefs.getString("VALID_TOKEN", "");
                if (!token.isEmpty()) headers.put("Session", token);
                return headers;
            }

            @Override
            public byte[] getBody() {
                try {
                    android.content.ContentResolver cr = requireContext().getContentResolver();
                    String mimeType = cr.getType(archivoUri);
                    if (mimeType == null) mimeType = "image/jpeg";
                    String extension = mimeType.contains("png") ? ".png" : ".jpg";
                    String fileName = "imagen_ticket" + extension;

                    Log.d("UPLOAD_DEBUG", "Iniciando lectura y compresión de archivo. URI: " + archivoUri);

                    byte[] fileBytes = obtenerBytesImagenComprimida(archivoUri);
                    if (fileBytes == null) {
                        Log.e("UPLOAD_DEBUG", "No se pudo obtener o comprimir la imagen");
                        return null;
                    }

                    java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();

                    // Campos de texto
                    String[] fields = {"tipo_dispositivo", "portes", "observaciones", "transporte", "id_dispositivo", "estado"};
                    String[] values = {tipoStr, portesStr, observaciones, transporte, idDispositivo, "no leido"};

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
                    Log.e("UPLOAD_ERROR", "Error construyendo multipart: " + e.getMessage(), e);
                    return null;
                }
            }
        };

        Volley.newRequestQueue(requireContext()).add(multipartRequest);
    }

    private byte[] obtenerBytesImagenComprimida(Uri uri) {
        try {
            android.content.ContentResolver cr = requireContext().getContentResolver();
            java.io.InputStream is = cr.openInputStream(uri);
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(is);
            if (is != null) is.close();

            if (bitmap == null) return null;

            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            // Comprimir a JPEG con calidad 80 (reduce el tamaño a unos cientos de KB)
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, bos);
            byte[] bytes = bos.toByteArray();
            bitmap.recycle();
            return bytes;
        } catch (Exception e) {
            Log.e("COMPRESS_ERROR", "Error al comprimir la imagen: " + e.getMessage());
            return null;
        }
    }
}
