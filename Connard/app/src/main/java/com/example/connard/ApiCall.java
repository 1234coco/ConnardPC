package com.example.connard; // <-- THAY PACKAGE CỦA BẠN

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Headers; // Import Headers
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class ApiCall {

    private static final String TAG = "ApiCall";
    private final OkHttpClient client;
    private final Handler mainThreadHandler;

    // Định nghĩa sẵn các MediaType phổ biến
    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    public static final MediaType TEXT = MediaType.parse("text/plain; charset=utf-8");
    // Thêm các MediaType khác nếu cần



    public ApiCall() {
        this.client = new OkHttpClient();
        this.mainThreadHandler = new Handler(Looper.getMainLooper());
    }

    // --- Phương thức GET tiện ích ---
    public void get(
            @NonNull String url,
            @Nullable Map<String, String> headers,
            @NonNull ApiCallback callback) {

        Request.Builder requestBuilder = new Request.Builder().url(url).get();
        addHeaders(requestBuilder, headers); // Gọi hàm thêm header chung
        executeRequestAsync(requestBuilder.build(), callback); // Gọi hàm thực thi chung
    }

    // --- Phương thức POST tiện ích (ví dụ với JSON body) ---
    public void postJson(
            @NonNull String url,
            @Nullable Map<String, String> headers,
            @NonNull String jsonBody, // Nhận chuỗi JSON
            @NonNull ApiCallback callback) {

        RequestBody body = RequestBody.create(jsonBody, JSON); // Sử dụng MediaType JSON
        Request.Builder requestBuilder = new Request.Builder().url(url).post(body);
        addHeaders(requestBuilder, headers);
        executeRequestAsync(requestBuilder.build(), callback);
    }

    // --- Phương thức POST tiện ích (với RequestBody tùy chỉnh) ---
    public void post(
            @NonNull String url,
            @Nullable Map<String, String> headers,
            @NonNull RequestBody requestBody, // Nhận RequestBody đã tạo sẵn
            @NonNull ApiCallback callback) {

        Request.Builder requestBuilder = new Request.Builder().url(url).post(requestBody);
        addHeaders(requestBuilder, headers);
        executeRequestAsync(requestBuilder.build(), callback);
    }


    // --- Phương thức PATCH tiện ích (ví dụ với JSON body) ---
    public void patchJson(
            @NonNull String url,
            @Nullable Map<String, String> headers,
            @NonNull String jsonBody,
            @NonNull ApiCallback callback) {

        RequestBody body = RequestBody.create(jsonBody, JSON);
        Request.Builder requestBuilder = new Request.Builder().url(url).patch(body);
        addHeaders(requestBuilder, headers);
        executeRequestAsync(requestBuilder.build(), callback);
    }

    // --- Phương thức PATCH tiện ích (với RequestBody tùy chỉnh) ---
    public void patch(
            @NonNull String url,
            @Nullable Map<String, String> headers,
            @NonNull RequestBody requestBody,
            @NonNull ApiCallback callback) {

        Request.Builder requestBuilder = new Request.Builder().url(url).patch(requestBody);
        addHeaders(requestBuilder, headers);
        executeRequestAsync(requestBuilder.build(), callback);
    }


    // --- Hàm private để thêm headers ---
    private void addHeaders(@NonNull Request.Builder builder, @Nullable Map<String, String> headers) {
        if (headers != null && !headers.isEmpty()) {
            // Dùng Headers.of để thay thế (phổ biến) hoặc lặp và addHeader nếu muốn giữ trùng
            builder.headers(Headers.of(headers));
            Log.d(TAG, "Headers added: " + headers.keySet());
        } else {
            Log.d(TAG, "No custom headers added.");
        }
    }

    // --- Hàm private thực thi request bất đồng bộ chung ---
    private void executeRequestAsync(@NonNull Request request, @NonNull ApiCallback callback) {
        if (client == null) {
            Log.e(TAG, "OkHttpClient is null!");
            mainThreadHandler.post(() -> callback.onFailure(-1, "Lỗi Client", new IllegalStateException("OkHttpClient is null")));
            return;
        }

        Log.d(TAG, "Executing [" + request.method() + "]: " + request.url());

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                Log.e(TAG, request.method() + " Failed: " + request.url() + " - " + e.getMessage(), e);
                mainThreadHandler.post(() -> callback.onFailure(-1, null, e));
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) {
                final int statusCode = response.code();
                String responseBodyString = null;

                try (ResponseBody responseBody = response.body()) {
                    if (responseBody != null) {
                        responseBodyString = responseBody.string(); // Đọc 1 lần
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error reading response body for " + request.url(), e);
                    mainThreadHandler.post(() -> callback.onFailure(statusCode, null, e));
                    return;
                }

                final String finalResponseBodyString = responseBodyString;

                mainThreadHandler.post(() -> {
                    if (response.isSuccessful()) { // Mã 2xx
                        Log.d(TAG, request.method() + " Success (" + statusCode + "): " + request.url());
                        callback.onSuccess(finalResponseBodyString);
                    } else { // Mã lỗi HTTP
                        Log.w(TAG, request.method() + " Not Successful (" + statusCode + "): " + request.url() + ", Body: " + finalResponseBodyString);
                        callback.onFailure(statusCode, finalResponseBodyString, null);
                    }
                });
            } // end onResponse
        }); // end enqueue
    } // end executeRequestAsync
}