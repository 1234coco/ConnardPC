package com.example.connard; // <-- THAY BẰNG PACKAGE CỦA BẠN

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import android.view.GestureDetector; // <<< THÊM IMPORT NÀY
import android.view.MotionEvent;
import androidx.annotation.NonNull; // <<

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap; // Import HashMap
import java.util.HashSet;
import java.util.Map;    // Import Map
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import okhttp3.HttpUrl; // Vẫn cần HttpUrl
import okhttp3.RequestBody;

// Import lớp ApiCall và ApiCallback
// import com.example.connard.ApiCall;
// import com.example.connard.ApiCallback;

public class MainClass extends AppCompatActivity implements GestureDetector.OnGestureListener{
    ScheduledExecutorService scheduler;
    private static final String GAME_NOTIFICATION_CHANNEL_ID = "game_status_channel"; // ID duy nhất cho kênh
    private static final String GAME_NOTIFICATION_CHANNEL_NAME = "Trạng Thái Game"; // Tên hiển thị cho người dùng
    private static final String GAME_NOTIFICATION_CHANNEL_DESC = "Thông báo khi game đang chạy hoặc dừng"; // Mô tả (tùy chọn)
    public static Set<Integer> notifiedGameIds = new HashSet<>();
    private static final String NOTIFIED_IDS_KEY = "NotifiedGameIds";
    private static final String TAG = "MainClassApp";
    public static final String BASE_URL = "http://192.168.1.4:8000";
    private static final String LOGIN_PATH = "login";
    private static final String ABOUT_PATH = "about";

    public static final String PREFS_NAME = "MyAppAuthPrefs";
    public static final String LOGIN_STATUS_KEY = "LoginStatus";
    public static final String AUTH_TOKEN_KEY = "jwt_access_token";
    private int page = 0;

    // --- SỬ DỤNG ApiCall ---
    private ApiCall apiCaller; // Đảm bảo tên lớp là ApiCall

    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;
    private GestureDetector gestureDetector;
    private static final int SWIPE_THRESHOLD = 40;
    private static final int SWIPE_VELOCITY_THRESHOLD = 40;
    // UI Elements
    private EditText usernameEditText, passwordEditText;
    private Button loginButton, logoutButton;
    private TextView welcomeTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        createNotificationChannel();
        // --- Khởi tạo ApiCall ---
        apiCaller = new ApiCall(); // Khởi tạo đúng lớp ApiCall

        // ... (khởi tạo SharedPreferences như trước) ...
        try { sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE); editor = sharedPref.edit(); }
        catch (Exception e) { Log.e(TAG, "Prefs init error", e); Toast.makeText(this, "App init error", Toast.LENGTH_LONG).show(); finish(); return; }

        checkLoginStateAndNavigate();

    }
    private void createNotificationChannel() {
        // Chỉ tạo kênh trên Android 8.0 (API 26) trở lên
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Importance quyết định mức độ ưu tiên (âm thanh, popup,...)
            int importance = NotificationManager.IMPORTANCE_DEFAULT; // Hoặc IMPORTANCE_HIGH nếu muốn nó pop-up

            // Tạo đối tượng Channel
            NotificationChannel channel = new NotificationChannel(
                    GAME_NOTIFICATION_CHANNEL_ID, // ID kênh đã định nghĩa
                    GAME_NOTIFICATION_CHANNEL_NAME, // Tên kênh hiển thị cho người dùng
                    importance);
            channel.setDescription(GAME_NOTIFICATION_CHANNEL_DESC); // Thêm mô tả

            // Đăng ký kênh với hệ thống; không cần làm gì nếu kênh đã tồn tại
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
                Log.d(TAG, "Notification channel created or already exists.");
            } else {
                Log.e(TAG, "Failed to get NotificationManager service.");
            }
        }
    }
    private void checkLoginStateAndNavigate() {
        if (sharedPref == null) {
            Log.e(TAG, "checkLoginStateAndNavigate: SharedPreferences is null!");
            showLoginScreen(); // Mặc định hiển thị login nếu có lỗi
            return;
        }
        boolean isLoggedIn = sharedPref.getBoolean(LOGIN_STATUS_KEY, false);
        Log.d(TAG, "Checking login state. isLoggedIn: " + isLoggedIn);

        if (!isLoggedIn) {
            showLoginScreen();
        } else {
            showMainWindow();
        }
    }

    /**
     * Hiển thị màn hình chính (home_pages).
     */
    private void showMainWindow() {
        Log.d(TAG, "Showing Main Window (Home Screen)");
        try {
            setContentView(R.layout.home_pages);
            welcomeTextView = findViewById(R.id.welcome_text);
            LinearLayout rootLayout = findViewById(R.id.home_pages);
            gestureDetector = new GestureDetector(this, this);
            rootLayout.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
            notification();
            if (welcomeTextView == null) { Log.e(TAG, "Welcome text view not found!"); }
            Switch status = findViewById(R.id.switch1);
            String jwt = sharedPref.getString(AUTH_TOKEN_KEY, null);
            status.setOnCheckedChangeListener((buttonView,isChecked) -> {
                if (isChecked) {
                    Toast.makeText(MainClass.this, "Chế độ hoạt động đã được bật", Toast.LENGTH_SHORT).show();
                    HttpUrl url = HttpUrl.parse(BASE_URL + "/" + "setStatus").newBuilder() // Key "user" theo API
                            .addQueryParameter("status", "1")
                            .build();
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Token",jwt);
                    status.setEnabled(false);
                    RequestBody emptyBody = RequestBody.create(new byte[0], null);
                    apiCaller.patch(url.toString(), headers,emptyBody, new ApiCallback() {
                        @Override
                        public JSONObject onSuccess(@Nullable String responseBody) {
                            Toast.makeText(MainClass.this, "Cập nhật trạng thái thành công", Toast.LENGTH_SHORT).show();
                            status.setEnabled(true);
                            return null;
                        }

                        @Override
                        public void onFailure(int statusCode, @Nullable String errorBody, @Nullable Exception e) {
                            Toast.makeText(MainClass.this, "Cập nhật trạng thái thất bại", Toast.LENGTH_SHORT).show();
                            status.setChecked(false);
                            status.setEnabled(true);
                        }
                    });
                    }

                else {
                    Toast.makeText(MainClass.this, "Chế độ hoạt động đã được tắt", Toast.LENGTH_SHORT).show();
                    HttpUrl url = HttpUrl.parse(BASE_URL + "/" + "setStatus").newBuilder() // Key "user" theo API
                            .addQueryParameter("status", "0")
                            .build();
                    Map<String, String> headers = new HashMap<>();
                    headers.put("Token",jwt);
                    status.setEnabled(false);
                    RequestBody emptyBody = RequestBody.create(new byte[0], null);
                    apiCaller.patch(url.toString(), headers,emptyBody, new ApiCallback() {
                        @Override
                        public JSONObject onSuccess(@Nullable String responseBody) {
                            Toast.makeText(MainClass.this, "Cập nhật trạng thái thành công", Toast.LENGTH_SHORT).show();
                            status.setEnabled(true);
                            return null;
                        }

                        @Override
                        public void onFailure(int statusCode, @Nullable String errorBody, @Nullable Exception e) {
                            Toast.makeText(MainClass.this, "Cập nhật trạng thái thất bại", Toast.LENGTH_SHORT).show();
                            status.setChecked(true);
                        }
                    });
                }

            });

            fetchUserInfo(); // Tải thông tin user khi hiển thị màn hình chính

        } catch (Exception e) { Log.e(TAG, "Error setting home_pages view!", e); }
    }

    /**
     * Hiển thị màn hình đăng nhập (mainlayout).
     */
    private void showLoginScreen() {
        Log.d(TAG, "Showing Login Screen");
        try {
            setContentView(R.layout.mainlayout);
            usernameEditText = findViewById(R.id.username);
            passwordEditText = findViewById(R.id.password);
            loginButton = findViewById(R.id.login);

            if (loginButton != null) {
                loginButton.setEnabled(true);
                loginButton.setOnClickListener(view -> handleLoginClick()); // Gán lại listener
            } else { Log.e(TAG, "Login button not found!"); }
            if (usernameEditText == null || passwordEditText == null) { Log.e(TAG, "Username or Password EditText not found!"); }

        } catch (Exception e) { Log.e(TAG, "Error setting mainlayout view!", e); }
    }


    /**
     * Xử lý sự kiện click nút đăng nhập.
     */
    private void handleLoginClick() {
        if (usernameEditText == null || passwordEditText == null || loginButton == null) return;
        String username = usernameEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        if (username.isEmpty()) {
            usernameEditText.setError("Vui lòng nhập user");
            usernameEditText.requestFocus();
            return;
        }
        if (password.isEmpty()) {
            passwordEditText.setError("Vui lòng nhập mật khẩu");
            passwordEditText.requestFocus();
            return;
        }

        Toast.makeText(MainClass.this, "Đang đăng nhập...", Toast.LENGTH_SHORT).show();
        loginButton.setEnabled(false); // Vô hiệu hóa nút trong khi chờ
        performGetLoginRequest(username, password); // Gọi hàm thực hiện request
    }

    private void performGetLoginRequest(String username, String password) {
        if (apiCaller == null) { Log.e(TAG,"ApiCall instance is null!"); return; }

        HttpUrl url = HttpUrl.parse(BASE_URL + "/" + LOGIN_PATH).newBuilder()
                .addQueryParameter("user", username) // Key "user" theo API
                .addQueryParameter("password", password)
                .build();
        Log.d(TAG, "Login URL: " + url.toString());

        // --- SỬA LỖI: Gọi đúng tên phương thức 'get' và đúng tham số ---
        // Tham số thứ 2 là headers (Map), truyền null vì không cần header tùy chỉnh
        // Tham số thứ 3 là callback
        apiCaller.get(url.toString(), null, new ApiCallback() { // <--- SỬA Ở ĐÂY
            @Override
            public JSONObject onSuccess(String responseBody) {
                // ... (Xử lý thành công như trước, gọi saveLoginState) ...
                if (loginButton != null) loginButton.setEnabled(true);
                Log.i(TAG, "Login Success Body: " + responseBody);
                try {
                    if (responseBody == null || responseBody.isEmpty()) throw new JSONException("Response empty");
                    JSONObject jsonObject = new JSONObject(responseBody);
                    if (jsonObject.has("access_token")) {
                        saveLoginState(jsonObject.getString("access_token"));
                    } else { handleLoginError("Phản hồi không chứa token.", null); }
                } catch (JSONException e) { handleLoginError("Lỗi dữ liệu đăng nhập.", e); }
                return null;
            }

            @Override
            public void onFailure(int statusCode, String errorBody, Exception e) {
                // ... (Xử lý thất bại như trước, gọi handleLoginError) ...
                if (loginButton != null) loginButton.setEnabled(true);
                String message = (e != null) ? "Lỗi mạng/IO." : "Tài khoản/Mật khẩu sai (Lỗi "+statusCode+")";
                handleLoginError(message, e);
            }
        });
    }

    private void notification() {
        if (scheduler == null || scheduler.isShutdown()) {
            scheduler = Executors.newSingleThreadScheduledExecutor();
        } else {
            Log.d(TAG, "Scheduler already running.");
            return;
        }

        scheduler.scheduleWithFixedDelay(() -> {
            Log.d(TAG, "Notification task running...");
            try {
                HttpUrl url = HttpUrl.parse(BASE_URL + "/config").newBuilder().build();
                Map<String, String> headers = new HashMap<>();
                String jwt = sharedPref.getString(AUTH_TOKEN_KEY, null);
                if (jwt == null || jwt.isEmpty()) {
                    runOnUiThread(this::logout);
                    return;
                }
                headers.put("jwt", jwt);

                apiCaller.get(url.toString(), headers, new ApiCallback() {
                    @Override
                    public JSONObject onSuccess(String responseBody) {
                        Log.d(TAG, "!!!!!! INSIDE /config onSuccess CALLBACK !!!!!!");
                        Log.d(TAG, "/config RAW responseBody: [" + responseBody + "]");
                        try {
                            JSONObject responseJson = new JSONObject(responseBody);
                            int status = responseJson.getInt("status");
                            Log.d(TAG, "/config status: " + status);

                            if (status == 1) {
                                JSONArray gameArray = responseJson.getJSONArray("games");
                                HttpUrl gameUrl = HttpUrl.parse(BASE_URL + "/listGame").newBuilder().build();

                                apiCaller.get(gameUrl.toString(), null, new ApiCallback() {
                                    @Override
                                    public JSONObject onSuccess(String gameListBody) {
                                        Log.d(TAG, "/listGame RAW responseBody: [" + gameListBody + "]");
                                        try {
                                            JSONArray gameList = new JSONArray(gameListBody);
                                            // --- Không cần Set tạm nữa nếu dùng lưu trữ bền bỉ ---
                                            // Set<Integer> idsToAdd = new HashSet<>();
                                            // Set<Integer> idsToRemove = new HashSet<>();

                                            for (int i = 0; i < gameArray.length(); i++) {
                                                JSONObject gameObj = gameArray.getJSONObject(i);
                                                int gameId = gameObj.getInt("id");
                                                String mode = gameObj.getString("mode");
                                                boolean allow = gameObj.getBoolean("allow");
                                                boolean isRunning = gameObj.getBoolean("running");
                                                Log.d(TAG, "Processing gameId: " + gameId + ", isRunning: " + isRunning + ", alreadyNotified: " + notifiedGameIds.contains(gameId));

                                                // --- NHÁNH XỬ LÝ KHI GAME BẮT ĐẦU CHẠY ---
                                                if (!notifiedGameIds.contains(gameId)
                                                        && mode.equals("ask") && !allow) {
                                                    for (int j = 0; j < gameList.length(); j++) {
                                                        JSONObject gameInfo = gameList.getJSONObject(j);
                                                        if (gameInfo.getInt("id") == gameId) {
                                                            final String gameName = gameInfo.getString("name");
                                                            final int notificationId = gameId;

                                                            runOnUiThread(() -> {
                                                                try {
                                                                    String message = "Game " + gameName + " đang chạy.";
                                                                    Log.d(TAG,"Preparing notification with actions: " + message);

                                                                    // --- Tạo Intent và PendingIntent cho Action "Cho phép" ---
                                                                    Intent allowIntent = new Intent(MainClass.this, notificationAction.class);
                                                                    allowIntent.setAction(notificationAction.ACTION_ALLOW_GAME);
                                                                    allowIntent.putExtra(notificationAction.EXTRA_GAME_ID, gameId);
                                                                    allowIntent.putExtra(notificationAction.EXTRA_NOTIFICATION_ID, notificationId);
                                                                    PendingIntent allowPendingIntent = PendingIntent.getBroadcast(
                                                                            MainClass.this,
                                                                            notificationId * 10 + 1, // Request code unique
                                                                            allowIntent,
                                                                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                                                                    );

                                                                    // --- Tạo Intent và PendingIntent cho Action "Chặn" ---
                                                                    Intent blockIntent = new Intent(MainClass.this, notificationAction.class);
                                                                    blockIntent.setAction(notificationAction.ACTION_BLOCK_GAME);
                                                                    blockIntent.putExtra(notificationAction.EXTRA_GAME_ID, gameId);
                                                                    blockIntent.putExtra(notificationAction.EXTRA_NOTIFICATION_ID, notificationId);
                                                                    PendingIntent blockPendingIntent = PendingIntent.getBroadcast(
                                                                            MainClass.this,
                                                                            notificationId * 10 + 2, // Request code unique khác
                                                                            blockIntent,
                                                                            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                                                                    );

                                                                    // --- Tạo Notification Builder và THÊM ACTIONS ---
                                                                    NotificationCompat.Builder builder = new NotificationCompat.Builder(MainClass.this, GAME_NOTIFICATION_CHANNEL_ID)
                                                                            .setSmallIcon(R.drawable.ic_stat_gamepad) // <<< Đảm bảo icon tồn tại
                                                                            .setContentTitle("Game đang chạy")
                                                                            .setContentText(message)
                                                                            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                                                                            .setOngoing(true) // Hủy khi nhấn vào nội dung notification
                                                                            // Thêm Action Buttons
                                                                            .addAction(R.drawable.ic_action_allow, "Cho phép", allowPendingIntent) // <<< Icon và Text cho nút
                                                                            .addAction(R.drawable.ic_action_block, "Chặn", blockPendingIntent); // <<< Icon và Text cho nút

                                                                    NotificationManagerCompat notificationManager = NotificationManagerCompat.from(MainClass.this);

                                                                    // Kiểm tra quyền và gửi notification
                                                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                                                        if (ActivityCompat.checkSelfPermission(MainClass.this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                                                                            Log.w(TAG,"POST_NOTIFICATIONS permission not granted. Cannot show notification for gameId: " + notificationId);
                                                                            Toast.makeText(MainClass.this, "Cần cấp quyền Thông báo", Toast.LENGTH_SHORT).show();
                                                                        } else {
                                                                            notificationManager.notify(notificationId, builder.build()); // Gửi Notification
                                                                            Log.d(TAG, "Notification with actions sent for gameId: " + notificationId);
                                                                        }
                                                                    } else {
                                                                        notificationManager.notify(notificationId, builder.build()); // Gửi Notification
                                                                        Log.d(TAG, "Notification with actions sent for gameId: " + notificationId);
                                                                    }
                                                                } catch (Exception e) {
                                                                    Log.e(TAG, "Error inside runOnUiThread for notification gameId: " + notificationId, e);
                                                                }
                                                            }); // Kết thúc runOnUiThread

                                                            // Cập nhật và Lưu trạng thái
                                                            notifiedGameIds.add(gameId);
                                                            saveNotifiedIdsToPrefs(); // <<< LƯU NGAY
                                                            break; // Thoát vòng lặp tìm tên
                                                        }
                                                    } // Kết thúc vòng lặp listGame
                                                }
                                                // --- NHÁNH XỬ LÝ KHI GAME DỪNG LẠI ---
//                                                else if (!isRunning && notifiedGameIds.contains(gameId)) {
//                                                    final int notificationId = gameId;
//                                                    runOnUiThread(() -> {
//                                                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(MainClass.this);
//                                                        notificationManager.cancel(notificationId); // Hủy notification cũ
//                                                        Log.d(TAG, "Cancelled notification for gameId: " + notificationId);
//                                                    });
//                                                    // Cập nhật và Lưu trạng thái
//                                                    boolean removed = notifiedGameIds.remove(gameId);
//                                                    if(removed) {
//                                                        saveNotifiedIdsToPrefs(); // <<< LƯU NGAY
//                                                        Log.d(TAG, "Removed and saved after stopping gameId: " + gameId);
//                                                    }
//                                                }
                                            } // Kết thúc vòng lặp gameArray

                                            // --- XÓA TOAST DEBUG ---
                                            // ...

                                        } catch (Exception e) {
                                            Log.e(TAG, "Error processing /listGame response", e);
                                        }
                                        return null;
                                    } // Kết thúc onSuccess /listGame
                                    @Override public void onFailure(int statusCode, String errorBody, Exception e) { /*...*/ }
                                }); // Kết thúc gọi /listGame
                            } else { // status != 1
                                if (!notifiedGameIds.isEmpty()) {
                                    Log.d(TAG,"Status is 0, clearing notifiedGameIds and cancelling notifications.");
                                    final Set<Integer> idsToCancel = new HashSet<>(notifiedGameIds);
                                    runOnUiThread(() -> {
                                        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(MainClass.this);
                                        for(Integer id : idsToCancel) {
                                            notificationManager.cancel(id);
                                            Log.d(TAG, "Cancelled notification due to status 0 for gameId: " + id);
                                        }
                                    });
                                    notifiedGameIds.clear();
                                    saveNotifiedIdsToPrefs(); // <<< LƯU SET RỖNG
                                }
                            }
                        } catch (Exception e) { /*...*/ }
                        return null;
                    } // Kết thúc onSuccess /config
                    @Override public void onFailure(int statusCode, String errorBody, Exception e) { /*...*/ }
                }); // Kết thúc gọi /config
            } catch (Exception e) { /*...*/ }
        }, 0, 5, TimeUnit.SECONDS);
        Log.d(TAG,"Notification scheduler started.");
    }
    private void saveNotifiedIdsToPrefs() {
        // Kiểm tra xem editor có bị null không (quan trọng!)
        if (editor == null) {
            // Cố gắng lấy lại editor nếu nó null (có thể xảy ra nếu có lỗi trước đó)
            if (sharedPref != null) {
                editor = sharedPref.edit();
            } else {
                Log.e(TAG, "Cannot save notified IDs, SharedPreferences is null!");
                return; // Không thể lưu nếu cả pref và editor đều null
            }
        }

        // Nếu editor vẫn null sau khi thử lấy lại, thì không thể làm gì hơn
        if (editor == null) {
            Log.e(TAG, "Cannot save notified IDs, editor is still null!");
            return;
        }


        // Chuyển đổi Set<Integer> thành Set<String> để lưu
        Set<String> idsToSave = new HashSet<>();
        if (notifiedGameIds != null) { // Kiểm tra null cho notifiedGameIds
            for (Integer id : notifiedGameIds) {
                if (id != null) { // Kiểm tra null cho từng ID
                    idsToSave.add(String.valueOf(id));
                }
            }
        }

        // Lưu vào SharedPreferences
        editor.putStringSet(NOTIFIED_IDS_KEY, idsToSave);
        editor.apply(); // Dùng apply() để lưu bất đồng bộ trong nền
        Log.d(TAG, "Saved notifiedGameIds to Prefs: " + idsToSave.toString());
    }
    // --- THÊM: Phương thức onDestroy để dừng scheduler ---
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (scheduler != null && !scheduler.isShutdown()) {
            Log.d(TAG, "Shutting down notification scheduler.");
            scheduler.shutdown(); // Ngừng nhận task mới
            try {
                // Chờ tối đa 5 giây để task hiện tại hoàn thành
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow(); // Buộc dừng nếu chờ quá lâu
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }



    private void fetchUserInfo() {
        if (apiCaller == null || sharedPref == null) { Log.e(TAG,"ApiCall/Prefs null in fetch"); return; }

        String jwt = sharedPref.getString(AUTH_TOKEN_KEY, null);
        if (jwt == null || jwt.isEmpty()) { logout(); return; }

        String url = BASE_URL + "/" + ABOUT_PATH;
        Log.d(TAG, "Fetching user info...");

        // Tạo Map headers theo yêu cầu API /about
        Map<String, String> headers = new HashMap<>();
        headers.put("jwt", jwt); // Header là "jwt"

        // --- SỬA LỖI: Gọi đúng tên phương thức 'get' và đúng tham số ---
        // Tham số thứ 2 là Map headers đã tạo
        // Tham số thứ 3 là callback
        apiCaller.get(url, headers, new ApiCallback() { // <--- SỬA Ở ĐÂY
            @Override
            public JSONObject onSuccess(String responseBody) {
                // ... (Xử lý thành công như trước, cập nhật welcomeTextView) ...
                Log.i(TAG, "User info fetched: " + responseBody);
                String displayName = (responseBody != null && !responseBody.isEmpty()) ? responseBody : "Bạn";
                if (welcomeTextView != null) welcomeTextView.setText("Chào mừng " + displayName + "!");
                else Log.e(TAG, "welcomeTextView is null!");
                return null;
            }

            @Override
            public void onFailure(int statusCode, String errorBody, Exception e) {
                // ... (Xử lý thất bại như trước, kiểm tra 404/405 và logout) ...
                Log.w(TAG, "Failed fetch /about. Code: " + statusCode, e);
                String message;
                if (e != null) message = "Không lấy được thông tin.";
                else if (statusCode == 404 || statusCode == 405) { logout(); return; }
                else message = "Lỗi tải thông tin ("+statusCode+")";
                Toast.makeText(MainClass.this, message, Toast.LENGTH_LONG).show();
                if (welcomeTextView != null) welcomeTextView.setText("Chào mừng Bạn!");
            }
        });
    }

    // ... (Các hàm saveLoginState, handleLoginError, logout giữ nguyên) ...

    private void saveLoginState(String token) {
        if (editor != null) {
            editor.putBoolean(LOGIN_STATUS_KEY, true);
            editor.putString(AUTH_TOKEN_KEY, token);
            editor.apply();
            Log.i(TAG, "Login state and token saved.");
            Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
            showMainWindow();
        } else { /* Log lỗi editor null */ }
    }

    private void handleLoginError(String message, @Nullable Exception e) {
        if (e != null) Log.e(TAG, "Login Error: " + message, e);
        else Log.w(TAG, "Login Error: " + message);
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void logout() {
        Log.i(TAG, "Logging out.");
        if (editor == null) {
            if (sharedPref == null) { Log.e(TAG,"Prefs null"); return; }
            editor = sharedPref.edit();
        }
        if (editor != null) {
            editor.remove(LOGIN_STATUS_KEY);
            editor.remove(AUTH_TOKEN_KEY);
            editor.commit();
            Log.d(TAG, "Cleared login status and token.");
        } else { Log.e(TAG, "Editor null!"); }
        showLoginScreen();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        Toast.makeText(this, "onTouchEvent", Toast.LENGTH_SHORT).show();
            if (this.gestureDetector.onTouchEvent(event)) {
                return super.onTouchEvent(event); // Cử chỉ đã được xử lý bởi GestureDetector
            }

        return super.onTouchEvent(event); // Để hệ thống xử lý bình thường
    }
    @Override
    public boolean onDown(@NonNull MotionEvent e) {
        return true;
    }

    @Override
    public void onShowPress(@NonNull MotionEvent e) {

    }
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev){
        super.dispatchTouchEvent(ev);
        return super.onTouchEvent(ev);
    }


    @Override
    public boolean onSingleTapUp(@NonNull MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(@Nullable MotionEvent e1, @NonNull MotionEvent e2, float distanceX, float distanceY) {
        Toast.makeText(this, "onScroll", Toast.LENGTH_SHORT).show();
        return false;
    }

    @Override
    public void onLongPress(@NonNull MotionEvent e) {

    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        boolean result = false;
        try {
            float diffY = e2.getY() - e1.getY();
            float diffX = e2.getX() - e1.getX();
            if (Math.abs(diffX) > Math.abs(diffY)) {
                if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                    if (diffX > 0) {
                        onSwipeRight();
                    } else {
                        onSwipeLeft();
                    }
                    result = true;
                }
            }
            else if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                if (diffY > 0) {
                    onSwipeBottom();
                } else {
                    onSwipeTop();
                }
                result = true;
            }
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return result;
    }


    public void onSwipeRight() {
        if (page == 0) {
            page = 1;
        }
    }

    public void onSwipeLeft() {
        if (page == 1) {
            showMainWindow();
            page = 0;
        }
    }

    public void onSwipeTop() {
    }

    public void onSwipeBottom() {
    }
}