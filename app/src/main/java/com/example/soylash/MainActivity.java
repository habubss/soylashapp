package com.example.soylash;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;
//import com.google.firebase.FirebaseApp;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Инициализация Firebase перед setContentView
        //FirebaseApp.initializeApp(this);

        setContentView(R.layout.activity_main);

        LinearLayout wordLearningLayout = findViewById(R.id.layout_word_learning);
        LinearLayout translatorLayout = findViewById(R.id.layout_translator);
        LinearLayout guessTranslationLayout = findViewById(R.id.layout_guess_translation);
        LinearLayout listeningLayout = findViewById(R.id.layout_listening);


        listeningLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Нажатие на 'Угадать перевод' зафиксировано");
                try {
                    startActivity(new Intent(MainActivity.this, ListeningActivity.class));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка перехода: " + e.getMessage());
                    showError("Не удалось открыть раздел");
                }
            }
        });

        guessTranslationLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(TAG, "Нажатие на 'Угадать перевод' зафиксировано");
                try {
                    startActivity(new Intent(MainActivity.this, GuessTranslationActivity.class));
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка перехода: " + e.getMessage());
                    showError("Не удалось открыть раздел");
                }
            }
        });

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
    }

    private void showError(String message) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}