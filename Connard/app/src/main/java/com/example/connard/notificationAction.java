package com.example.connard;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationManagerCompat;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.RequestBody;

public class notificationAction extends BroadcastReceiver {

    private static final String TAG = "NotifActionReceiver";
    public static final String ACTION_ALLOW_GAME = "com.example.connard.ACTION_ALLOW_GAME";
    public static final String ACTION_BLOCK_GAME = "com.example.connard.ACTION_BLOCK_GAME";
    public static final String EXTRA_GAME_ID = "game_id";
    public static final String EXTRA_NOTIFICATION_ID = "notification_id";

    // Sử dụng ExecutorService để thực hiện API call trên background thread
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        int gameId = intent.getIntExtra(EXTRA_GAME_ID, -1);
        int notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1); // Lấy notificationId để hủy

        Log.d(TAG, "Received action: " + action + " for gameId: " + gameId + " (Notif ID: " + notificationId + ")");

        if (gameId == -1 || action == null) {
            Log.e(TAG, "Invalid gameId or action.");
            return;
        }

        // Hủy notification ngay lập tức để người dùng biết nút đã được nhấn
        if (notificationId != -1) {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
            notificationManager.cancel(notificationId);
            Log.d(TAG, "Cancelled notification with ID: " + notificationId);
        }

        // Xác định hành động và thực hiện API call trên background thread
        if (ACTION_ALLOW_GAME.equals(action)) {
            performApiCall(context, gameId, "allow"); // Giả sử API cần "allow"
        } else if (ACTION_BLOCK_GAME.equals(action)) {
            performApiCall(context, gameId, "block"); // Giả sử API cần "block"
        }
    }

    private void performApiCall(Context context, int gameId, String rule) {
        executorService.execute(() -> {
            // Lấy token từ SharedPreferences (cần Context)
            SharedPreferences sharedPref = context.getSharedPreferences(MainClass.PREFS_NAME, Context.MODE_PRIVATE);
            String jwt = sharedPref.getString(MainClass.AUTH_TOKEN_KEY, null);

            if (jwt == null || jwt.isEmpty()) {
                Log.e(TAG, "JWT Token not found for API call.");
                // Có thể hiển thị Toast lỗi trên UI Thread nếu cần
                // ((Activity) context).runOnUiThread(() -> Toast.makeText(context, "Lỗi xác thực", Toast.LENGTH_SHORT).show());
                return;
            }

            // Tạo đối tượng ApiCall (cần đảm bảo có cách truy cập hoặc tạo mới)
            // Tạm thời tạo mới, nhưng tốt hơn là dùng Singleton hoặc Dependency Injection
            ApiCall apiCaller = new ApiCall();

            // --- Chuẩn bị URL và Body/Headers cho API setRule (Ví dụ) ---
            // --- THAY ĐỔI ĐƯỜNG DẪN VÀ THAM SỐ CHO PHÙ HỢP VỚI API CỦA BẠN ---


            // Ví dụ nếu dùng Request Body (JSON) cho POST/PUT/PATCH
            JSONObject jsonBody = new JSONObject();
            try {
                jsonBody.put("id", gameId);
                jsonBody.put("mode","ask");
                jsonBody.put("allow",true);
                if (rule.equals("allow")){
                    jsonBody.put("running",true ); // "allow" hoặc "block"
                }else{
                    jsonBody.put("running",false );
                }
            } catch (JSONException e) {
                Log.e(TAG, "Error creating JSON body", e);
                return;
            }
            HttpUrl url = HttpUrl.parse(MainClass.BASE_URL + "/changeConfig").newBuilder()
                    .addQueryParameter("update", "true") // <<< THAY ĐƯỜNG DẪN API
                    .addQueryParameter("jsons",jsonBody.toString()) // <<< THAY ĐƯỜNG DẪN API
                    // Có thể dùng query parameter hoặc request body tùy API
                    // .addQueryParameter("game_id", String.valueOf(gameId))
                    // .addQueryParameter("rule", rule)
                    .build();
            RequestBody requestBody = RequestBody.create(jsonBody.toString(), MediaType.parse("application/json; charset=utf-8"));


            Map<String, String> headers = new HashMap<>();
            headers.put("jwt", jwt); // Hoặc "Token" tùy API

            Log.d(TAG, "Executing API call: " + rule + " game " + gameId + " at URL: " + url);
            MainClass.notifiedGameIds.remove(gameId); // <<< THÊM GAME ID VÀO SET
            // --- Gọi API (Ví dụ dùng POST, thay bằng phương thức đúng) ---
            apiCaller.patch(url.toString(), headers, requestBody, new ApiCallback() { // <<< THAY BẰNG POST/PUT/PATCH...
                @Override
                public JSONObject onSuccess(@Nullable String responseBody) {
                    Log.i(TAG, "API call (" + rule + " game " + gameId + ") successful. Response: " + responseBody);
                    // Không cần cập nhật UI phức tạp từ đây, chỉ log hoặc Toast ngắn
                    // Chạy Toast trên UI Thread
                    // ((Activity) context).runOnUiThread(() -> Toast.makeText(context, "Đã " + rule + " game " + gameId, Toast.LENGTH_SHORT).show());
                    return null;
                }

                @Override
                public void onFailure(int statusCode, @Nullable String errorBody, @Nullable Exception e) {
                    Log.e(TAG, "API call (" + rule + " game " + gameId + ") failed. Code: " + statusCode + ", Body: " + errorBody, e);
                    // ((Activity) context).runOnUiThread(() -> Toast.makeText(context, "Lỗi khi " + rule + " game " + gameId, Toast.LENGTH_SHORT).show());
                }
            });
        });
    }
}
