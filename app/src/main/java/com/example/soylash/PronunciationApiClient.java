package com.example.soylash;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class PronunciationApiClient {
    // Используйте IP вашего сервера
    private static final String BASE_URL = "http://192.168.1.53:5001/";
    private static PronunciationApi instance;

    public static PronunciationApi getApi() {
        if (instance == null) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .connectTimeout(10, TimeUnit.SECONDS)
                    .readTimeout(30, TimeUnit.SECONDS)
                    .build();

            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();

            instance = retrofit.create(PronunciationApi.class);
        }
        return instance;
    }
}