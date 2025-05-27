package com.example.soylash;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.RotateAnimation;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.gson.JsonObject;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import android.util.Base64;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;


public class WordLearningActivity extends AppCompatActivity {
    private DatabaseReference favoritesRef;
    private boolean isFavorite = false;
    private static final String TAG = "WordLearningActivity";
    private static final int REQUEST_RECORD_AUDIO_PERMISSION = 200;
    private static final String[][] CATEGORY_MAPPING = {
            {"N", "Существительные"},
            {"V", "Глаголы"},
            {"ADJ", "Прилагательные"},
            {"ADV", "Наречия"}
    };

    private List<String[]> allWordsData = new ArrayList<>();
    private List<String[]> filteredWords = new ArrayList<>();
    private int currentIndex = 0;
    private boolean isFilterExpanded = false;
    private CheckBox checkAll, checkNouns, checkVerbs, checkAdjectives, checkAdverbs;
    private MediaPlayer mediaPlayer;
    private MediaRecorder mediaRecorder;
    private Button btnSpeak, btnRecord, btnCheck;
    private String currentAudioPath;
    private boolean isRecording = false;
    private String currentWord;
    private LinearLayout audioPreviewContainer;
    private ImageButton btnPlayRecording;
    private SeekBar audioSeekBar;
    private Handler seekBarHandler = new Handler();
    private Runnable updateSeekBar;

    private void toggleFavorite() {
        if (currentWord == null) return;

        if (isFavorite) {
            return; // Already favorited, button should be disabled
        }

        HashMap<String, Object> favoriteWord = new HashMap<>();
        favoriteWord.put("word", currentWord);
        favoriteWord.put("translation", filteredWords.get(currentIndex)[3]);
        favoriteWord.put("partOfSpeech", filteredWords.get(currentIndex)[1]);

        favoritesRef.child(currentWord).setValue(favoriteWord)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        isFavorite = true;
                        updateFavoriteButton();
                        Toast.makeText(this, "Добавлено в избранное", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Ошибка добавления", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void updateFavoriteButton() {
        Button btn = findViewById(R.id.btnAddToFavorites);
        if (isFavorite) {
            btn.setText("Добавлено в избранное");
            btn.setEnabled(false);
            btn.setBackgroundColor(getResources().getColor(R.color.gray));
        } else {
            btn.setText("Добавить в избранное");
            btn.setEnabled(true);
            btn.setBackgroundColor(getResources().getColor(R.color.green));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_word_learning);

        allWordsData = loadWordsFromJson();

        ImageButton backButton = findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> finish());

        setupFilter();
        updateFilteredWords();
        updateContent();
        setupNextButton();

        // Инициализация кнопок
        btnSpeak = findViewById(R.id.btnSpeak);
        btnSpeak.setOnClickListener(v -> synthesizeText());

        btnRecord = findViewById(R.id.btnRecord);
        btnRecord.setOnClickListener(v -> toggleRecording());

        btnCheck = findViewById(R.id.btnCheck);
        btnCheck.setOnClickListener(v -> checkPronunciation());
        btnCheck.setEnabled(false);

        audioPreviewContainer = findViewById(R.id.audioPreviewContainer);
        btnPlayRecording = findViewById(R.id.btnPlayRecording);
        audioSeekBar = findViewById(R.id.audioSeekBar);

        btnPlayRecording.setOnClickListener(v -> togglePlayback());

        requestAudioPermissions();

        favoritesRef = FirebaseDatabase.getInstance().getReference()
                .child("Users")
                .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                .child("Favorites");

        Button btnAddToFavorites = findViewById(R.id.btnAddToFavorites);
        btnAddToFavorites.setOnClickListener(v -> toggleFavorite());
    }

    private void requestAudioPermissions() {
        // Список необходимых разрешений
        List<String> requiredPermissions = new ArrayList<>();

        // Обязательное разрешение для записи аудио
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            requiredPermissions.add(Manifest.permission.RECORD_AUDIO);
        }

        // Разрешение на запись в хранилище (только для API < 29)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                requiredPermissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
            }
        }

        // Если разрешения уже предоставлены
        if (requiredPermissions.isEmpty()) {
            Log.d(TAG, "Все разрешения уже получены");
            return;
        }

        // Запрашиваем недостающие разрешения
        ActivityCompat.requestPermissions(
                this,
                requiredPermissions.toArray(new String[0]),
                REQUEST_RECORD_AUDIO_PERMISSION
        );

        // Для отладки
        Log.d(TAG, "Запрошены разрешения: " + requiredPermissions);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_RECORD_AUDIO_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Разрешение получено", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Нужно разрешение для записи", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void toggleRecording() {
        if (!isRecording) {
            startRecording();
            btnRecord.setText("Остановить запись");
            btnRecord.setBackgroundColor(getResources().getColor(R.color.accent));
            btnCheck.setEnabled(false);
        } else {
            stopRecording();
            btnRecord.setText("Начать запись");
            btnRecord.setBackgroundColor(getResources().getColor(R.color.primary));
            btnCheck.setEnabled(true);
            setupAudioPlayback();
        }
    }

    private void startRecording() {
        try {
            mediaRecorder = new MediaRecorder();
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.AAC_ADTS);
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);

            // Создаем папку для записи, если ее нет
            File musicDir = new File(getExternalFilesDir(Environment.DIRECTORY_MUSIC), "");
            if (!musicDir.exists()) {
                musicDir.mkdirs();
            }

            // Генерируем уникальный путь
            currentAudioPath = musicDir.getAbsolutePath() + "/" + UUID.randomUUID().toString() + ".aac";
            mediaRecorder.setOutputFile(currentAudioPath);

            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;

            // Логирование для отладки
            Log.d(TAG, "Начата запись. Путь: " + currentAudioPath);

        } catch (IOException e) {
            Log.e(TAG, "Ошибка записи: ", e);
            Toast.makeText(this, "Ошибка инициализации записи", Toast.LENGTH_SHORT).show();
            resetRecorder();
        } catch (IllegalStateException e) {
            Log.e(TAG, "Неподдерживаемые настройки MediaRecorder: ", e);
            Toast.makeText(this, "Ошибка кодека или формата", Toast.LENGTH_SHORT).show();
            resetRecorder();
        }
    }

    // Вспомогательный метод для сброса MediaRecorder
    private void resetRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder = null;
        }
        isRecording = false;
    }

    private void stopRecording() {
        if (mediaRecorder != null) {
            try {
                mediaRecorder.stop();
            } catch (RuntimeException e) {
                Log.e(TAG, "Ошибка остановки записи: ", e);
                // Удаляем невалидный файл
                new File(currentAudioPath).delete();
                return;
            } finally {
                mediaRecorder.release();
                mediaRecorder = null;
                isRecording = false;
            }

            File audioFile = new File(currentAudioPath);
            if (audioFile.exists() && audioFile.length() > 0) {
                audioPreviewContainer.setVisibility(View.VISIBLE);
                setupAudioPlayback();
            } else {
                Toast.makeText(this, "Файл поврежден или пуст", Toast.LENGTH_SHORT).show();
            }
            Log.d(TAG, "Размер файла: " + audioFile.length());
        }
    }

    private void playUserRecording() {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }
        mediaPlayer = new MediaPlayer();
        try {
            mediaPlayer.setDataSource(currentAudioPath);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (IOException e) {
            Log.e(TAG, "Ошибка воспроизведения записи: ", e);
        }
        if (!new File(currentAudioPath).exists()) {
            Toast.makeText(this, "Файл не найден", Toast.LENGTH_SHORT).show();
            return;
        }
    }

    // Новая логика воспроизведения
    private void setupAudioPlayback() {
        try {
            if (mediaPlayer != null) {
                mediaPlayer.release();
                mediaPlayer = null;
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(currentAudioPath);
            mediaPlayer.prepareAsync(); // Используем асинхронную подготовку

            mediaPlayer.setOnPreparedListener(mp -> {
                audioSeekBar.setMax(mediaPlayer.getDuration());
                btnPlayRecording.setEnabled(true);
                btnPlayRecording.setImageResource(R.drawable.ic_play);
            });

            mediaPlayer.setOnCompletionListener(mp -> resetPlayButton());

            updateSeekBar = new Runnable() {
                @Override
                public void run() {
                    if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                        audioSeekBar.setProgress(mediaPlayer.getCurrentPosition());
                        seekBarHandler.postDelayed(this, 100);
                    }
                }
            };

        } catch (IOException e) {
            Log.e(TAG, "Ошибка подготовки аудио: ", e);
            Toast.makeText(this, "Ошибка загрузки записи", Toast.LENGTH_SHORT).show();
        }
        mediaPlayer.setOnErrorListener((mp, what, extra) -> {
            Log.e(TAG, "Ошибка MediaPlayer: " + what + ", " + extra);
            resetPlayButton();
            return true;
        });
    }

    private void togglePlayback() {
        if (mediaPlayer == null) return;

        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
            btnPlayRecording.setImageResource(R.drawable.ic_play);
            seekBarHandler.removeCallbacks(updateSeekBar);
        } else {
            mediaPlayer.start();
            btnPlayRecording.setImageResource(R.drawable.ic_pause);
            seekBarHandler.post(updateSeekBar);
        }
    }

    private void resetPlayButton() {
        btnPlayRecording.setImageResource(R.drawable.ic_play);
        audioSeekBar.setProgress(0);
    }

    private void checkPronunciation() {
        if (currentWord == null || currentWord.isEmpty()) {
            Toast.makeText(this, "Нет текущего слова", Toast.LENGTH_SHORT).show();
            return;
        }

        File audioFile = new File(currentAudioPath);
        RequestBody audioBody = RequestBody.create(MediaType.parse("audio/aac"), audioFile);
        MultipartBody.Part audioPart = MultipartBody.Part.createFormData("audio", audioFile.getName(), audioBody);
        MultipartBody.Part textPart = MultipartBody.Part.createFormData("text", null,
                RequestBody.create(MediaType.parse("text/plain"), currentWord));

        PronunciationApiClient.getApi().checkPronunciation(textPart, audioPart).enqueue(new Callback<PronunciationResult>() {
            @Override
            public void onResponse(Call<PronunciationResult> call, Response<PronunciationResult> response) {
                if (response.isSuccessful() && response.body() != null) {
                    PronunciationResult result = response.body();
                    displayPronunciationResult(result);
                } else {
                    String errorMessage = "Ошибка сервера: " + response.code();
                    try {
                        if (response.errorBody() != null) {
                            // Логируем сырой JSON для отладки
                            String rawJson = response.errorBody().string();
                            Log.e(TAG, "Ошибка сервера. Ответ: " + rawJson);
                            errorMessage = "Неверный формат данных";
                        }
                    } catch (IOException e) {
                        Log.e(TAG, "Ошибка чтения errorBody", e);
                    }
                    Toast.makeText(WordLearningActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<PronunciationResult> call, Throwable t) {
                Log.e(TAG, "Ошибка сети: ", t);
                Toast.makeText(WordLearningActivity.this,
                        "Ошибка подключения: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void displayPronunciationResult(PronunciationResult result) {
        LinearLayout resultContainer = findViewById(R.id.resultContainer);
        resultContainer.setVisibility(View.VISIBLE);

        TextView resultText = findViewById(R.id.resultTextView);
        LinearLayout wordsLayout = findViewById(R.id.wordsLayout);
        wordsLayout.removeAllViews();

        if (result.getResult().equals("correct")) {
            resultText.setText("✅ Правильное произношение!");
            resultText.setTextColor(ContextCompat.getColor(this, R.color.green));

            // Добавляем само слово зеленым
            TextView correctWordView = new TextView(this);
            correctWordView.setText(currentWord);
            correctWordView.setTextSize(18);
            correctWordView.setTextColor(ContextCompat.getColor(this, R.color.green));
            correctWordView.setGravity(Gravity.CENTER);
            correctWordView.setPadding(0, 16, 0, 0);
            wordsLayout.addView(correctWordView);

        } else {
            resultText.setText("❌ Ошибки в произношении");
            resultText.setTextColor(ContextCompat.getColor(this, R.color.red));

            List<String> words = result.getWordsList();
            List<Integer> accuracy = result.getAccuracy();

            if (words.size() != accuracy.size()) {
                Toast.makeText(this, "Ошибка формата данных", Toast.LENGTH_SHORT).show();
                return;
            }

            for (int i = 0; i < words.size(); i++) {
                String word = words.get(i);
                int accuracyValue = accuracy.get(i);

                // Создаем Spannable для цветного текста
                SpannableString spannable = new SpannableString(word);
                int half = word.length() / 2;

                // Определяем цвета
                int greenColor = ContextCompat.getColor(this, R.color.green);
                int redColor = ContextCompat.getColor(this, R.color.red);

                switch (accuracyValue) {
                    case 1: // Первая половина красная, вторая зеленая
                        spannable.setSpan(
                                new ForegroundColorSpan(redColor),
                                0, half,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        );
                        spannable.setSpan(
                                new ForegroundColorSpan(greenColor),
                                half, word.length(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        );
                        break;

                    case 2: // Первая половина зеленая, вторая красная
                        spannable.setSpan(
                                new ForegroundColorSpan(greenColor),
                                0, half,
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        );
                        spannable.setSpan(
                                new ForegroundColorSpan(redColor),
                                half, word.length(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        );
                        break;

                    case 3: // Все слово красное
                        spannable.setSpan(
                                new ForegroundColorSpan(redColor),
                                0, word.length(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        );
                        break;

                    default: // Неизвестный статус (серый)
                        spannable.setSpan(
                                new ForegroundColorSpan(Color.GRAY),
                                0, word.length(),
                                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                        );
                }

                // Создаем TextView и добавляем в layout
                TextView wordView = new TextView(this);
                wordView.setText(spannable);
                wordView.setTextSize(18);
                wordView.setGravity(Gravity.CENTER);
                wordView.setPadding(20, 10, 20, 10);
                wordsLayout.addView(wordView);
            }
        }

        if (result.getFeedbackAudio() != null && !result.getFeedbackAudio().isEmpty()) {
            playFeedbackAudio(result.getFeedbackAudio());
        }
    }

    private void playFeedbackAudio(String base64Audio) {
        byte[] audioBytes = Base64.decode(base64Audio, Base64.DEFAULT);
        try {
            File tempFile = File.createTempFile("feedback", ".wav", getCacheDir());
            FileOutputStream fos = new FileOutputStream(tempFile);
            fos.write(audioBytes);
            fos.close();

            MediaPlayer mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(tempFile.getAbsolutePath());
            mediaPlayer.prepare();
            mediaPlayer.start();

        } catch (IOException e) {
            Log.e(TAG, "Ошибка воспроизведения feedback: ", e);
        }
    }

    private List<String[]> loadWordsFromJson() {
        List<String[]> result = new ArrayList<>();
        try {
            InputStream is = getAssets().open("tatar_words.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            String json = new String(buffer, StandardCharsets.UTF_8);

            Gson gson = new Gson();
            Type wordListType = new TypeToken<List<WordItem>>() {
            }.getType();
            List<WordItem> words = gson.fromJson(json, wordListType);

            for (WordItem item : words) {
                String category = mapCategory(item.type);
                if (category != null) {
                    result.add(new String[]{
                            item.word,
                            item.type,
                            category,
                            item.translation
                    });
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "Ошибка загрузки JSON: ", e);
            Toast.makeText(this, "Файл данных не найден", Toast.LENGTH_LONG).show();
        }
        return result;
    }

    private String mapCategory(String jsonType) {
        for (String[] mapping : CATEGORY_MAPPING) {
            if (mapping[0].equals(jsonType)) {
                return mapping[1];
            }
        }
        return null;
    }

    private void setupNextButton() {
        findViewById(R.id.nextButton).setOnClickListener(v -> {
            if (!filteredWords.isEmpty()) {
                currentIndex = (currentIndex + 1) % filteredWords.size();
                updateContent();
            } else {
                Toast.makeText(this, "Нет слов по выбранным фильтрам", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupFilter() {
        LinearLayout filterHeader = findViewById(R.id.filterHeader);
        LinearLayout filterDropdown = findViewById(R.id.filterDropdown);
        ImageView filterArrow = findViewById(R.id.filterArrow);

        checkAll = findViewById(R.id.checkAll);
        checkNouns = findViewById(R.id.checkNouns);
        checkVerbs = findViewById(R.id.checkVerbs);
        checkAdjectives = findViewById(R.id.checkAdjectives);
        checkAdverbs = findViewById(R.id.checkAdverbs);

        filterHeader.setOnClickListener(v -> toggleFilterDropdown(filterArrow, filterDropdown));
        setupCheckboxListeners();
        checkAll.setChecked(true);
    }

    private void toggleFilterDropdown(ImageView arrow, LinearLayout dropdown) {
        isFilterExpanded = !isFilterExpanded;
        RotateAnimation anim = new RotateAnimation(
                isFilterExpanded ? 0 : 180,
                isFilterExpanded ? 180 : 0,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        anim.setDuration(200);
        anim.setFillAfter(true);
        arrow.startAnimation(anim);
        dropdown.setVisibility(isFilterExpanded ? View.VISIBLE : View.GONE);
    }

    private void setupCheckboxListeners() {
        findViewById(R.id.filterAll).setOnClickListener(v -> {
            checkAll.setChecked(true);
            checkNouns.setChecked(false);
            checkVerbs.setChecked(false);
            checkAdjectives.setChecked(false);
            checkAdverbs.setChecked(false);
            updateFilteredWords();
        });

        View.OnClickListener categoryHandler = v -> {
            LinearLayout layout = (LinearLayout) v;
            CheckBox checkBox = (CheckBox) layout.getChildAt(0);
            checkBox.setChecked(!checkBox.isChecked());
            if (checkBox.isChecked()) checkAll.setChecked(false);
            updateFilteredWords();
        };

        findViewById(R.id.filterNouns).setOnClickListener(categoryHandler);
        findViewById(R.id.filterVerbs).setOnClickListener(categoryHandler);
        findViewById(R.id.filterAdjectives).setOnClickListener(categoryHandler);
        findViewById(R.id.filterAdverbs).setOnClickListener(categoryHandler);
    }

    private void updateFilteredWords() {
        filteredWords.clear();

        if (checkAll.isChecked()) {
            filteredWords.addAll(allWordsData);
        } else {
            for (String[] word : allWordsData) {
                String category = word[2];
                if ((checkNouns.isChecked() && category.equals("Существительные")) ||
                        (checkVerbs.isChecked() && category.equals("Глаголы")) ||
                        (checkAdjectives.isChecked() && category.equals("Прилагательные")) ||
                        (checkAdverbs.isChecked() && category.equals("Наречия"))) {
                    filteredWords.add(word);
                }
            }
        }

        currentIndex = 0;
        updateContent();

        if (filteredWords.isEmpty() && !checkAll.isChecked()) {
            Toast.makeText(this, "Нет совпадений", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateContent() {
        try {
            if (!filteredWords.isEmpty()) {
                String[] wordData = filteredWords.get(currentIndex);
                this.currentWord = wordData[0]; // Сохраняем текущее слово
                ((TextView) findViewById(R.id.wordTextView)).setText(wordData[0]);
                ((TextView) findViewById(R.id.partOfSpeechTextView)).setText(wordData[1]);
                ((TextView) findViewById(R.id.categoryTextView)).setText(wordData[2]);
                ((TextView) findViewById(R.id.translationTextView)).setText(wordData[3]);
                checkIfFavorite();
            }
        } catch (Exception e) {
            Toast.makeText(this, "Ошибка отображения", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkIfFavorite() {
        if (currentWord == null) return;

        favoritesRef.child(currentWord).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                isFavorite = task.getResult().exists();
                updateFavoriteButton();
            }
        });

    }

    private void synthesizeText() {
        if (filteredWords.isEmpty()) {
            Toast.makeText(this, "Нет текста для озвучки", Toast.LENGTH_SHORT).show();
            return;
        }

        String text = filteredWords.get(currentIndex)[0];
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("text", text);

        // Логирование запроса
        Log.d(TAG, "Отправка запроса на синтез: " + text);

        Toast.makeText(this, "Синтезируется речь...", Toast.LENGTH_SHORT).show();

        ApiClient.getApi().synthesizeText(jsonObject).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    if (response.body() != null) {
                        try {
                            // Логирование успешного ответа
                            Log.d(TAG, "Ответ получен. Размер данных: " + response.body().contentLength());

                            // Воспроизведение аудио
                            playAudio(response.body().byteStream());
                            Toast.makeText(WordLearningActivity.this,
                                    "Озвучка успешна", Toast.LENGTH_SHORT).show();

                        } catch (IOException e) {
                            Log.e(TAG, "Ошибка чтения аудиопотока: ", e);
                            Toast.makeText(WordLearningActivity.this,
                                    "Ошибка воспроизведения", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.e(TAG, "Пустое тело ответа");
                        Toast.makeText(WordLearningActivity.this,
                                "Сервер не вернул аудио", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    // Логирование HTTP-ошибок
                    try {
                        String errorBody = response.errorBody() != null ? response.errorBody().string() : "Нет деталей";
                        Log.e(TAG, "Ошибка сервера: " + response.code() + "\n" + errorBody);
                    } catch (IOException e) {
                        Log.e(TAG, "Ошибка чтения errorBody: ", e);
                    }
                    Toast.makeText(WordLearningActivity.this,
                            "Ошибка сервера: " + response.code(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                // Логирование ошибок сети
                Log.e(TAG, "Ошибка сети: " + Log.getStackTraceString(t));
                Toast.makeText(WordLearningActivity.this,
                        "Ошибка подключения: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void playAudio(InputStream inputStream) throws IOException {
        if (mediaPlayer != null) {
            mediaPlayer.release();
        }

        File tempFile = File.createTempFile("audio", ".wav", getCacheDir());
        FileOutputStream fos = new FileOutputStream(tempFile);
        byte[] buffer = new byte[1024];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            fos.write(buffer, 0, length);
        }
        fos.close();

        mediaPlayer = new MediaPlayer();
        mediaPlayer.setDataSource(tempFile.getAbsolutePath());
        mediaPlayer.prepare();
        mediaPlayer.start();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (mediaRecorder != null) {
            try {
                mediaRecorder.release();
            } catch (Exception e) {
                Log.e(TAG, "Ошибка освобождения MediaRecorder", e);
            }
        }
    }

    private static class WordItem {
        String word;
        String type;
        String translation;
    }

}