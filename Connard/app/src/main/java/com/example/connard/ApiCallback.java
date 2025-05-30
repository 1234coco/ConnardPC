package com.example.connard;

import androidx.annotation.Nullable;

import org.json.JSONObject;

// ApiCallback.java
interface ApiCallback {
    JSONObject onSuccess(@Nullable String responseBody); // Thêm @Nullable
    void onFailure(int statusCode, @Nullable String errorBody, @Nullable Exception e); // Thêm @Nullable, dùng Exception
}