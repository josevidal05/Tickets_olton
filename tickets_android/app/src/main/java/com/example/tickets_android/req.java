//package com.example.jadmusic;
//import android.content.SharedPreferences;
//import android.os.Bundle;
//
//import androidx.appcompat.app.AppCompatActivity;
//import android.content.Context;
//import android.content.Intent;
//import android.view.View;
//import android.widget.Button;
//import android.widget.EditText;
//
//import com.example.tickets_android.R;
//
//import org.json.JSONException;
//import org.json.JSONObject;
//
//public class RegisterActivity extends AppCompatActivity {
//    private EditText editTextUsuario;
//    private EditText editTextCorreo;
//    private EditText editTextContrasena;
//    private Button registerButtom;
//    private Button sessionButton;
//    private Context context=this;
//    private String url="http://10.0.2.2:8000/usuarios/";
//    private String onResponse="Usuario creado";
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        SharedPreferences sharedPref = getSharedPreferences("ajustes_tema", Context.MODE_PRIVATE);
//        boolean isModoOscuro = sharedPref.getBoolean("modo_oscuro_activado", false);
//
//        if (isModoOscuro) {
//            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_YES);
//        } else {
//            androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_NO);
//        }
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.fragment_register);
//
//        editTextUsuario = findViewById(R.id.regusuario);
//        editTextCorreo = findViewById(R.id.regcorreo);
//        editTextContrasena = findViewById(R.id.regcontrasena);
//        registerButtom= findViewById(R.id.regboton);
//        sessionButton= findViewById(R.id.sesionboton);
//
//        registerButtom.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                JSONObject requestBody= new JSONObject();
//                try{
//                    requestBody.put("useremail", editTextCorreo.getText().toString());
//                    requestBody.put("new_username", editTextUsuario.getText().toString());
//                    requestBody.put("password", editTextContrasena.getText().toString());
//                    SendRequestsForLoginOrRegister sendRequestsForLoginOrRegister=new SendRequestsForLoginOrRegister();
//                    sendRequestsForLoginOrRegister.sendPostRequest(context, url, requestBody, onResponse);
//                }catch (JSONException e){
//                    throw new RuntimeException(e);
//                }
//            }
//        });
//        sessionButton.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent myIntent=new Intent(context , LoginActivity.class);
//                startActivity(myIntent);
//            }
//        });
//    }
//}