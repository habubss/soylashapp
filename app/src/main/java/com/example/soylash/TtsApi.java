package com.example.soylash;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;
import com.google.gson.JsonObject;

public interface TtsApi {
    @POST("synthesize") // Путь должен совпадать с серверным роутом
    Call<ResponseBody> synthesizeText(@Body JsonObject text);
}