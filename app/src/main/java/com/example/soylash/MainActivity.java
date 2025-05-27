// Update MainActivity.java
package com.example.soylash;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Check if user is logged in
        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        }

        // Add logout button functionality
        ImageButton logoutButton = findViewById(R.id.logoutButton);
        logoutButton.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(MainActivity.this, LoginActivity.class));
            finish();
        });

        LinearLayout wordLearningLayout = findViewById(R.id.layout_word_learning);
        LinearLayout translatorLayout = findViewById(R.id.layout_translator);
        LinearLayout favoritesCard = findViewById(R.id.layout_favorites);

        translatorLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Нажатие на 'Переводчик' зафиксировано");
                try {
                    startActivity(new Intent(MainActivity.this, TranslaterActivity.class));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка перехода: " + e.getMessage());
                    showError("Не удалось открыть раздел");
                }
            }
        });

        wordLearningLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Нажатие на 'Изучение слов' зафиксировано");
                try {
                    startActivity(new Intent(MainActivity.this, WordLearningActivity.class));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка перехода: " + e.getMessage());
                    showError("Не удалось открыть раздел");
                }
            }
        });

        favoritesCard.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Нажатие на 'Избранное' зафиксировано");
                try {
                    startActivity(new Intent(MainActivity.this, FavoritesActivity.class));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка перехода: " + e.getMessage());
                    showError("Не удалось открыть раздел");
                }
            }
        });
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
