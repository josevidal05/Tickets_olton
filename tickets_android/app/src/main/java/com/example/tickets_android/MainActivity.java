package com.example.tickets_android;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.splashscreen.SplashScreen;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;


public class MainActivity extends AppCompatActivity{

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        SplashScreen.installSplashScreen(this);

        SharedPreferences sharedPref = getSharedPreferences("ajustes_tema", Context.MODE_PRIVATE);
        boolean isModoOscuro = sharedPref.getBoolean("modo_oscuro_activado", false);

        if (isModoOscuro) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }


        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        BottomNavigationView bar = findViewById(R.id.bottomNavigation);
        getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, new InicioFragment()).commit();

        bar.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.inicio) {
                    // Hacer algo si el usuario pulsa Inicio
                    InicioFragment fragment_inicioFragment = new InicioFragment();
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, fragment_inicioFragment).commit();

                }
                if (item.getItemId() == R.id.tickets) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragmentContainer, new TicketFragment()) // Carga el Fragment, no la Activity
                            .commit();

                }
                if (item.getItemId() == R.id.perfil) {
                    // Hacer algo si el usuario pulsa Perfil
                    PerfilFragment fragment_perfilFragment = new PerfilFragment();
                    getSupportFragmentManager().beginTransaction().replace(R.id.fragmentContainer, fragment_perfilFragment).commit();
                }
                return true;
            }
        });


    }




}
