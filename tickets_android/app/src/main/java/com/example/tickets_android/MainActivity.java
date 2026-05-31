package com.example.tickets_android;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.splashscreen.SplashScreen;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.navigation.NavigationBarView;

public class MainActivity extends AppCompatActivity {

    private BottomNavigationView bar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SplashScreen.installSplashScreen(this);

        // Verificar si el usuario está autenticado antes de cargar la app
        SharedPreferences authPrefs = getSharedPreferences("SESSIONS_APP_PREFS", MODE_PRIVATE);
        String token = authPrefs.getString("VALID_TOKEN", "");

        if (token.isEmpty()) {
            Intent intent = new Intent(this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
            return;
        }

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

        bar = findViewById(R.id.bottomNavigation);

        // Carga el fragmento inicial solo la primera vez
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragmentContainer, new InicioFragment())
                    .commit();
        }

        bar.setOnItemSelectedListener(new NavigationBarView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;
                int itemId = item.getItemId();

                if (itemId == R.id.inicio) {
                    selectedFragment = new InicioFragment();
                } else if (itemId == R.id.tickets) {
                    selectedFragment = new TicketFragment();
                } else if (itemId == R.id.perfil) {
                    selectedFragment = new PerfilFragment();
                }

                if (selectedFragment != null) {
                    // Verificamos si ya estamos en ese fragmento para no duplicar la pila
                    Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
                    if (current != null && current.getClass().equals(selectedFragment.getClass())) {
                        return true;
                    }

                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragmentContainer, selectedFragment)
                            .addToBackStack(null) // <-- ESTO permite volver atrás
                            .commit();
                }
                return true;
            }
        });

        // Listener para sincronizar el icono de la barra cuando volvemos atrás
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragmentContainer);
            if (currentFragment instanceof InicioFragment) {
                bar.getMenu().findItem(R.id.inicio).setChecked(true);
            } else if (currentFragment instanceof TicketFragment) {
                bar.getMenu().findItem(R.id.tickets).setChecked(true);
            } else if (currentFragment instanceof PerfilFragment) {
                bar.getMenu().findItem(R.id.perfil).setChecked(true);
            }
        });
    }

    // Gestionamos el botón de retroceso manualmente
    @Override
    public void onBackPressed() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack(); // Vuelve al fragmento anterior
        } else {
            super.onBackPressed(); // Si no hay más historial, sale de la app
        }
    }
}