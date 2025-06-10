package com.example.soylash;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface PronunciationApi {
    @Multipart
    @POST("check_pronunciation")
    Call<PronunciationResult> checkPronunciation(
            @Part MultipartBody.Part text,
            @Part MultipartBody.Part audio
    );
}