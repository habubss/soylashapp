package com.example.soylash;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PronunciationResult {
    private String result;
    private String words; // Изменили тип на String
    private List<Integer> accuracy;
    private String feedbackAudio;

    // Геттеры и сеттеры
    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public String getWords() {
        return words;
    }

    public void setWords(String words) {
        this.words = words;
    }

    public List<Integer> getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(List<Integer> accuracy) {
        this.accuracy = accuracy;
    }

    public String getFeedbackAudio() {
        return feedbackAudio;
    }

    public void setFeedbackAudio(String feedbackAudio) {
        this.feedbackAudio = feedbackAudio;
    }

    // Метод для преобразования строки в список
    public List<String> getWordsList() {
        if (words == null || words.isEmpty()) {
            return new ArrayList<>();
        }
        return Arrays.asList(words.split(" ")); // Разделение по пробелам
    }
}