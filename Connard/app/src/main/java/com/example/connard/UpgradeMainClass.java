package com.example.connard;

// <-- THAY BẰNG PACKAGE CỦA BẠN
import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.SearchView;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.view.GestureDetector; // <<< THÊM IMPORT NÀY
import android.view.MotionEvent;
import androidx.annotation.NonNull; // <<
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.connard.widget.DateTimePickerWidget;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap; // Import HashMap
import java.util.List;
import java.util.Map;    // Import Map
import java.time.*;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.Headers;
import okhttp3.HttpUrl; // Vẫn cần HttpUrl
import okhttp3.RequestBody;

// Import lớp ApiCall và ApiCallback
// import com.example.connard.ApiCall;
// import com.example.connard.ApiCallback;

public class UpgradeMainClass extends AppCompatActivity implements GestureDetector.OnGestureListener{

    private static final String TAG = "MainClassApp";
    private static String BASE_URL = "http://192.168.1.6:8000";
    private static final String LOGIN_PATH = "login";
    private static final String ABOUT_PATH = "about";

    private static final String PREFS_NAME = "MyAppAuthPrefs";
    private static final String LOGIN_STATUS_KEY = "LoginStatus";
    private static final String AUTH_TOKEN_KEY = "jwt_access_token";
    public List<String> devices = new ArrayList<>() ;
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
    public ArrayAdapter<String> adapter;
    private Boolean stateAllow = false;
    private Boolean statePause = false;
    private Boolean stateLimit = false;
    private JSONArray listGame = new JSONArray();
    private AtomicReference<Long> timestamp = new AtomicReference<>(0L);
    private AtomicReference<Integer> timeLimited = new AtomicReference<>(0);
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 101;
    private static final int QR_SCAN_REQUEST_CODE = 100;
    private boolean isLogged = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_dropdown_item_1line, devices);
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        // --- Khởi tạo ApiCall ---
        apiCaller = new ApiCall(); // Khởi tạo đúng lớp ApiCall

        // ... (khởi tạo SharedPreferences như trước) ...
        try { sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE); editor = sharedPref.edit(); }

        catch (Exception e) { Log.e(TAG, "Prefs init error", e); Toast.makeText(this, "App init error", Toast.LENGTH_LONG).show(); finish(); return; }
        String ips = sharedPref.getString("ip", null);
        if (ips == null){
            editor.putString("ip", BASE_URL);
        }
        if (ips != BASE_URL){
            if (ips != null) {
                BASE_URL = ips;
            }
        }
        editor.apply();


        checkLoginStateAndNavigate();

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
            isLogged = true;
            showMainWindow();
        }
    }
    private void showSettingScreen() {
        Log.d(TAG, "Showing Setting Screen");
        try {
            setContentView(R.layout.setting_pages);
            TextInputEditText ip = findViewById(R.id.ip_server);
            ip.setText(sharedPref.getString("ip", null));
            Button save = findViewById(R.id.save);
            RelativeLayout rootLayout = findViewById(R.id.setting_pages);
            gestureDetector = new GestureDetector(this, this);
            rootLayout.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
            save.setOnClickListener(v -> {
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putString("ip", ip.getText().toString());
                editor.apply();
                BASE_URL = ip.getText().toString();
                Toast.makeText(UpgradeMainClass.this, "Lưu thành công", Toast.LENGTH_SHORT).show();
            });
        } catch (Exception e) {
            Log.e(TAG, "Error setting setting_pages view!", e);
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
            if (welcomeTextView == null) { Log.e(TAG, "Welcome text view not found!"); }
            Switch status = findViewById(R.id.switch1);
            String jwt = sharedPref.getString(AUTH_TOKEN_KEY, null);
            Map<String, String> headers = new HashMap<>();
            headers.put("jwt",jwt);
            apiCaller.get(BASE_URL + "/" + "run", headers, new ApiCallback() {
                @Override
                public JSONObject onSuccess(@Nullable String responseBody) {
//                    runOnUiThread(()->{
//                            Toast.makeText(UpgradeMainClass.this,responseBody,Toast.LENGTH_SHORT);
//                    });
                    if (responseBody.equals("1")){
                        status.setChecked(true);
                    }else {
                        status.setChecked(false);
                    }
                    return null;
                }

                @Override
                public void onFailure(int statusCode, @Nullable String errorBody, @Nullable Exception e) {
//                    runOnUiThread(()->{
//                        Toast.makeText(UpgradeMainClass.this,errorBody,Toast.LENGTH_SHORT);
//                    });
                }
            });
            status.setOnCheckedChangeListener((buttonView,isChecked) -> {
                if (isChecked) {
                    Toast.makeText(UpgradeMainClass.this, "Chế độ hoạt động đã được bật", Toast.LENGTH_SHORT).show();
                    HttpUrl url = HttpUrl.parse(BASE_URL + "/" + "setStatus").newBuilder() // Key "user" theo API
                            .addQueryParameter("status", "1")
                            .build();
                    Map<String, String> headerss = new HashMap<>();
                    headerss.put("Token",jwt);
                    status.setEnabled(false);
                    RequestBody emptyBody = RequestBody.create(new byte[0], null);
                    apiCaller.patch(url.toString(), headerss,emptyBody, new ApiCallback() {
                        @Override
                        public JSONObject onSuccess(@Nullable String responseBody) {
                            Toast.makeText(UpgradeMainClass.this, "Cập nhật trạng thái thành công", Toast.LENGTH_SHORT).show();
                            status.setEnabled(true);
                            return null;
                        }

                        @Override
                        public void onFailure(int statusCode, @Nullable String errorBody, @Nullable Exception e) {
                            Toast.makeText(UpgradeMainClass.this, "Cập nhật trạng thái thất bại", Toast.LENGTH_SHORT).show();
                            status.setChecked(false);
                            status.setEnabled(true);
                        }
                    });
                }

                else {
                    Toast.makeText(UpgradeMainClass.this, "Chế độ hoạt động đã được tắt", Toast.LENGTH_SHORT).show();
                    HttpUrl url = HttpUrl.parse(BASE_URL + "/" + "setStatus").newBuilder() // Key "user" theo API
                            .addQueryParameter("status", "0")
                            .build();
                    Map<String, String> headerss = new HashMap<>();
                    headerss.put("Token",jwt);
                    status.setEnabled(false);
                    RequestBody emptyBody = RequestBody.create(new byte[0], null);
                    apiCaller.patch(url.toString(), headerss,emptyBody, new ApiCallback() {
                        @Override
                        public JSONObject onSuccess(@Nullable String responseBody) {
                            Toast.makeText(UpgradeMainClass.this, "Cập nhật trạng thái thành công", Toast.LENGTH_SHORT).show();
                            status.setEnabled(true);
                            return null;
                        }

                        @Override
                        public void onFailure(int statusCode, @Nullable String errorBody, @Nullable Exception e) {
                            Toast.makeText(UpgradeMainClass.this, "Cập nhật trạng thái thất bại", Toast.LENGTH_SHORT).show();
                            status.setChecked(true);
                        }
                    });
                }

            });

            fetchUserInfo(); // Tải thông tin user khi hiển thị màn hình chính
            Button btnScanQr = findViewById(R.id.btn_scan_qr);
            btnScanQr.setOnClickListener(v -> {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, CAMERA_PERMISSION_REQUEST_CODE);
                    return;
                }
                IntentIntegrator integrator = new IntentIntegrator(this);
                IntentIntegrator intentIntegrator = new IntentIntegrator(this);
                intentIntegrator.setPrompt("Scan a barcode or QR Code");
                intentIntegrator.setOrientationLocked(true);
                intentIntegrator.initiateScan();
            });

        } catch (Exception e) { Log.e(TAG, "Error setting home_pages view!", e); }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult intentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        // if the intentResult is null then
        // toast a message as "cancelled"
        if (intentResult != null) {
            if (intentResult.getContents() == null) {
                Toast.makeText(getBaseContext(), "Thất bại, Đọc QR thất bại", Toast.LENGTH_SHORT).show();
            } else {
                // if the intentResult is not null we'll set
                // the content and format of scan message
                String QR_String = intentResult.getContents();
                if (QR_String.startsWith("BGADWQ:")) {
                    HttpUrl url = HttpUrl.parse(BASE_URL + "/" + "addPCAccount").newBuilder().addQueryParameter("code", QR_String.substring(7)).build();
                    Toast.makeText(getBaseContext(), QR_String.substring(7), Toast.LENGTH_SHORT).show();
                    Map<String, String> headers = new HashMap<>();
                    RequestBody emptyBody = RequestBody.create(new byte[0], null);
                    headers.put("Token", sharedPref.getString(AUTH_TOKEN_KEY, null));
                    apiCaller.post(url.toString(), headers, emptyBody, new ApiCallback() {

                        @Override
                        public JSONObject onSuccess(@Nullable String responseBody) {
                            Toast.makeText(getBaseContext(), "Thêm thiết bị thành công", Toast.LENGTH_SHORT).show();
                            return null;
                        }

                        @Override
                        public void onFailure(int statusCode, @Nullable String errorBody, @Nullable Exception e) {
                            Toast.makeText(getBaseContext(), "Thêm thiết bị thất bại,Đảm bảo thiết bị cần thêm và điện thoại được kết nối internet", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                else {
                    Toast.makeText(getBaseContext(), "Thất bại,QR Không Hợp Lệ", Toast.LENGTH_SHORT).show();
                }
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
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
            LinearLayout rootLayout = findViewById(R.id.mainlayout);
            gestureDetector = new GestureDetector(this, this);
            rootLayout.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));

            if (loginButton != null) {
                loginButton.setEnabled(true);
                loginButton.setOnClickListener(view -> handleLoginClick()); // Gán lại listener
            } else { Log.e(TAG, "Login button not found!"); }
            if (usernameEditText == null || passwordEditText == null) { Log.e(TAG, "Username or Password EditText not found!"); }

        } catch (Exception e) { Log.e(TAG, "Error setting mainlayout view!", e); }
    }
    private interface onDevicesCallback {
        void onSuccess(JSONObject config);
    }
    private interface onConfigCallback {
        void onSuccess(JSONArray config);
        void onFailed();
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

        Toast.makeText(UpgradeMainClass.this, "Đang đăng nhập...", Toast.LENGTH_SHORT).show();
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

    private void fetchDeviceToken(){
        if (apiCaller == null || sharedPref == null) { Log.e(TAG,"ApiCall/Prefs null in fetch"); return; }

        String jwt = sharedPref.getString(AUTH_TOKEN_KEY, null);
        if (jwt == null || jwt.isEmpty()) { logout(); return; }
        Map<String, String> headers = new HashMap<>();
        headers.put("jwt", jwt); // Header là "jwt"
        String url = BASE_URL + "/" + "getAllDevices";
        apiCaller.get(url, headers, new ApiCallback() { // <--- SỬA Ở ĐÂY
            @Override
            public JSONObject onSuccess(String responseBody) {
                // ... (Xử lý thành công như trước, cập nhật welcomeTextView) ...
                try {
                    JSONArray redevcies = new JSONArray(responseBody);
                    for (int i = 0; i < redevcies.length(); i++) {
                        devices.add(redevcies.getString(i));

                    }
                    Toast.makeText(UpgradeMainClass.this, devices.toString() , Toast.LENGTH_SHORT).show();
                    adapter.notifyDataSetChanged();
                    Spinner spinner = findViewById(R.id.chooseDevice);
                    spinner.setAdapter(adapter);
                    Button btnAllow = findViewById(R.id.btnAllow);
                    Button btnPause = findViewById(R.id.btnPause);
                    Button btnLimit = findViewById(R.id.btnLimit);

                    RecyclerView rvAllow = findViewById(R.id.rvAllow);
                    RecyclerView rvPause = findViewById(R.id.rvPause);
                    RecyclerView rvLimit = findViewById(R.id.rvLimit);

                    boolean noDevice = devices.size() == 1 && devices.get(0).equals("Không có thiết bị nào");

                    btnAllow.setEnabled(!noDevice);
                    btnPause.setEnabled(!noDevice);
                    btnLimit.setEnabled(!noDevice);

                    rvAllow.setVisibility(noDevice ? View.GONE : View.VISIBLE);
                    rvPause.setVisibility(noDevice ? View.GONE : View.VISIBLE);
                    rvLimit.setVisibility(noDevice ? View.GONE : View.VISIBLE);

                } catch (JSONException e) {
                    Log.e(TAG, "Error parsing JSON", e);
                }
                return null;
            }

            @Override
            public void onFailure(int statusCode, String errorBody, Exception e) {
                // ... (Xử lý thất bại như trước, kiểm tra 404/405 và logout) ...
                Log.w(TAG, "Failed fetch /about. Code: " + statusCode, e);

            }
        });
    }
    private void fetchDeviceConfig(String token, onDevicesCallback callback) {
        HttpUrl url = HttpUrl.parse(BASE_URL + "/" + "appConfig").newBuilder()
                .addQueryParameter("code", token)
                .build();

        apiCaller.get(url.toString(), null, new ApiCallback() {
            @Override
            public JSONObject onSuccess(String responseBody) {
                try {
                    callback.onSuccess(new JSONObject(responseBody));
                } catch (JSONException e) {
                    callback.onSuccess(null);
                }
                return null;
            }

            @Override
            public void onFailure(int statusCode, String errorBody, Exception e) {
                callback.onSuccess(null);  // or callback.onFailure(...) if you define one
            }
        });
    }
    private void fetchUserInfo() {
        if (apiCaller == null || sharedPref == null) { Log.e(TAG,"ApiCall/Prefs null in fetch"); return; }

        String jwt = sharedPref.getString(AUTH_TOKEN_KEY, null);
        if (jwt == null || jwt.isEmpty()) { logout(); return; }
        Map<String, String> headers = new HashMap<>();
        headers.put("jwt", jwt); // Header là "jwt"


        String url = BASE_URL + "/" + ABOUT_PATH;
        Log.d(TAG, "Fetching user info...");

        // Tạo Map headers theo yêu cầu API /about


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
                Toast.makeText(UpgradeMainClass.this, message, Toast.LENGTH_LONG).show();
                if (welcomeTextView != null) welcomeTextView.setText("Chào mừng Bạn!");
            }
        });
    }
    private void fetchListGames(onConfigCallback callback){
        if (apiCaller == null || sharedPref == null) { Log.e(TAG,"ApiCall/Prefs null in fetch"); return; }
        HttpUrl url = HttpUrl.parse(BASE_URL+"/"+ "listGame").newBuilder().build();
        apiCaller.get(url.toString(), null, new ApiCallback() {
            @Override
            public JSONObject onSuccess(@Nullable String responseBody) {
                try{
                    callback.onSuccess(new JSONArray(responseBody));
                } catch (JSONException e) {
                    callback.onFailed();
                }
                return null;
            }

            @Override
            public void onFailure(int statusCode, @Nullable String errorBody, @Nullable Exception e) {
                callback.onFailed();
            }
        });
    }
    private void configLimit(LinearLayout layout) {
        layout.setGravity(Gravity.CENTER);
        TimePicker timePicker = new TimePicker(this);
        timePicker.setIs24HourView(true);
        layout.addView(timePicker);
        Button btnSet = new Button(this);
        btnSet.setText("Set");
        layout.addView(btnSet);
        btnSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                timeLimited.set(timePicker.getHour() * 60 + timePicker.getMinute());
            }});
    }
    private void configPause(LinearLayout layout) {
        // Canh giữa toàn bộ nội dung trong LinearLayout
        layout.setGravity(Gravity.CENTER);

        // Tạo nút "Set"
        Button myButton = new Button(this);
        myButton.setText("Set");
        TextView textView = new TextView(this);
        textView.setText("");
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );;


        // Thiết lập layout cho nút
        myButton.setLayoutParams(params);
        params.setMargins(0, 50, 0, 0);
        textView.setLayoutParams(params);
        textView.setTextColor(Color.BLACK);
        // Thêm TextView vào layout
        layout.addView(textView);
        // Thêm nút vào layout
        layout.addView(myButton);

        // Sự kiện khi bấm nút: mở DateTimePicker
        myButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Tạo DateTimePicker mới
                DateTimePickerWidget dateTimePicker = new DateTimePickerWidget(UpgradeMainClass.this,
                        new DateTimePickerWidget.ICustomDateTimeListener() {
                            @Override
                            public void onSet(Dialog dialog, Calendar calendarSelected, Date dateSelected,
                                              int year, String monthFullName, String monthShortName, int monthNumber,
                                              int day, String weekDayFullName, String weekDayShortName,
                                              int hour24, int hour12, int min, int sec, String AM_PM) {
                                long timestampSeconds = calendarSelected.getTimeInMillis() / 1000;
                                String selectedTime = DateTimePickerWidget.pad(hour24) + ":" +
                                        DateTimePickerWidget.pad(min);

                                String selectedDate = day + "/" + String.valueOf(monthNumber) + "/" + year;
                                textView.setText(selectedTime + " " + selectedDate);
                                timestamp.set(timestampSeconds);
                                Toast.makeText(UpgradeMainClass.this,
                                        "Bạn đã chọn: (" + selectedTime + " " + selectedDate +  "), Timestamp: " + timestampSeconds,
                                        Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onCancel() {
                                Toast.makeText(UpgradeMainClass.this, "Hủy chọn thời gian", Toast.LENGTH_SHORT).show();
                            }
                        });

                // Mở dialog lên (không cần đóng alertdialog)
                dateTimePicker.showDialog();
            }
        });
    }

    // ... (Các hàm saveLoginState, handleLoginError, logout giữ nguyên) ...
    @SuppressLint("NotifyDataSetChanged")
    private void showConfigScreen() {
        Log.d(TAG, "Showing Config Screen");
        try {

            devices.clear();
            fetchDeviceToken();
            setContentView(R.layout.config_page);
            Spinner spinner = findViewById(R.id.chooseDevice);
            spinner.setAdapter(adapter);
            fetchListGames(new onConfigCallback()  {

                @Override
                public void onSuccess(JSONArray config) {
                    listGame = config;
                }

                @Override
                public void onFailed() {
                    Toast.makeText(UpgradeMainClass.this, "Lỗi tải danh sách game", Toast.LENGTH_SHORT).show();
                }

            });
            List<GameAllowInfo> gameAllowList = new ArrayList<>();
            List<GameAllowLimitInfo> gameAllowLimitList = new ArrayList<>();
            List<GamePauseInfo> gamePauseList = new ArrayList<>();
            Button btnAllow = findViewById(R.id.btnAllow);
            Button btnPause = findViewById(R.id.btnPause);
            Button btnLimit = findViewById(R.id.btnLimit);
            FloatingActionButton btnEditConfig = findViewById(R.id.btnEditConfig);

            RecyclerView rvAllow = findViewById(R.id.rvAllow);
            rvAllow.setLayoutManager(new LinearLayoutManager(this));
            RecyclerView rvPause = findViewById(R.id.rvPause);
            rvPause.setLayoutManager(new LinearLayoutManager(this));
            RecyclerView rvLimit = findViewById(R.id.rvLimit);
            rvLimit.setLayoutManager(new LinearLayoutManager(this));

            AllowConfigRecycler allowConfigRecycler = new AllowConfigRecycler(gameAllowList);
            rvAllow.setAdapter(allowConfigRecycler);

            AllowLimitConfigRecycler allowLimitConfigRecycler = new AllowLimitConfigRecycler(gameAllowLimitList);
            rvLimit.setAdapter(allowLimitConfigRecycler);
            PauseConfigRecycler pauseConfigRecycler = new PauseConfigRecycler(gamePauseList);
            rvPause.setAdapter(pauseConfigRecycler);

            if (btnAllow != null) {
                btnAllow.setOnClickListener(view -> {
                    stateAllow = !stateAllow;
                    if (spinner.getAdapter().getCount() == 0){
                        Toast.makeText(UpgradeMainClass.this, "Bạn chưa thêm thiết bị!", Toast.LENGTH_SHORT).show();
                    }
                    if (stateAllow & spinner.getAdapter().getCount() != 0) {
                        fetchDeviceConfig(spinner.getSelectedItem().toString(), (config) -> {
                            try {
                                JSONArray configArray = config.getJSONArray(spinner.getSelectedItem().toString());

                                for (int i = 0; i < configArray.length(); i++) {
                                    JSONObject item = configArray.getJSONObject(i);
                                    if (item.getString("mode").equals("allow")){
                                        String name = String.valueOf(item.getInt("id"));
                                        Toast.makeText(UpgradeMainClass.this, gameAllowList.toString(), Toast.LENGTH_SHORT).show();
                                        for (int j = 0; j < listGame.length(); j++) {
                                            JSONObject game = listGame.getJSONObject(j);

                                            if (game.getInt("id")==item.getInt("id")){
                                                name = game.getString("name");
                                            }
                                        }
                                        if (item.getBoolean("running")){
                                            gameAllowList.add(new GameAllowInfo(name, "Đang chạy"));
                                        }else {
                                            gameAllowList.add(new GameAllowInfo(name, "Không chạy"));
                                        }
                                    }
                                }
                                allowConfigRecycler.updateGameList(gameAllowList);
                                btnAllow.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_down_arrow, 0);
                            } catch (JSONException e) {
                                Log.e(TAG, "Error parsing JSON", e);
                            }
                        });
                    } else {
                        gameAllowList.clear();
                        allowConfigRecycler.updateGameList(gameAllowList);
                        btnAllow.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_up_arrow, 0);
                    }
                });
            }
            if (btnLimit != null) {
                btnLimit.setOnClickListener(view -> {
                    stateLimit = !stateLimit;
                    if (spinner.getSelectedItem() == null){
                        Toast.makeText(UpgradeMainClass.this, "Bạn chưa thêm thiết bị!", Toast.LENGTH_SHORT).show();
                    }
                    if (stateLimit& spinner.getAdapter().getCount() != 0) {
                        fetchDeviceConfig(spinner.getSelectedItem().toString(), (config) -> {
                            try {
                                JSONArray configArray = config.getJSONArray(spinner.getSelectedItem().toString());

                                for (int i = 0; i < configArray.length(); i++) {
                                    JSONObject item = configArray.getJSONObject(i);
                                    if (item.getString("mode").equals("allow_limit")){
                                        String name = String.valueOf(item.getInt("id"));
                                        Toast.makeText(UpgradeMainClass.this, gameAllowLimitList.toString(), Toast.LENGTH_SHORT).show();
                                        for (int j = 0; j < listGame.length(); j++) {
                                            JSONObject game = listGame.getJSONObject(j);

                                            if (game.getInt("id")==item.getInt("id")){
                                                name = game.getString("name");
                                            }
                                        }
                                        if (item.getBoolean("running")){
                                            gameAllowLimitList.add(new GameAllowLimitInfo(name, "Đang chạy",String.valueOf(item.getInt("limit")),String.valueOf(item.getInt("played"))));
                                        }else {
                                            gameAllowLimitList.add(new GameAllowLimitInfo(name, "Không chạy",String.valueOf(item.getInt("limit")),String.valueOf(item.getInt("played"))));
                                        }
                                    }
                                }
                                allowLimitConfigRecycler.updateGameList(gameAllowLimitList);
                                btnLimit.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_down_arrow, 0);
                            } catch (JSONException e) {
                                Log.e(TAG, "Error parsing JSON", e);
                            }
                        });
                    } else {
                        gameAllowLimitList.clear();
                        allowLimitConfigRecycler.updateGameList(gameAllowLimitList);
                        btnLimit.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_up_arrow, 0);
                    }
                });
            }
            if (btnPause != null) {
                btnPause.setOnClickListener(view -> {
                    statePause = !statePause;
                    if (spinner.getSelectedItem() == null){
                        Toast.makeText(UpgradeMainClass.this, "Bạn chưa thêm thiết bị!", Toast.LENGTH_SHORT).show();
                    }
                    if (statePause& spinner.getAdapter().getCount() != 0) {
                        fetchDeviceConfig(spinner.getSelectedItem().toString(), (config) -> {
                            try {
                                JSONArray configArray = config.getJSONArray(spinner.getSelectedItem().toString());

                                for (int i = 0; i < configArray.length(); i++) {
                                    JSONObject item = configArray.getJSONObject(i);
                                    if (item.getString("mode").equals("pause")){
                                        String name = String.valueOf(item.getInt("id"));
                                        Toast.makeText(UpgradeMainClass.this, gamePauseList.toString(), Toast.LENGTH_SHORT).show();
                                        for (int j = 0; j < listGame.length(); j++) {
                                            JSONObject game = listGame.getJSONObject(j);

                                            if (game.getInt("id")==item.getInt("id")){
                                                name = game.getString("name");
                                            }
                                        }
                                        Instant instant = Instant.ofEpochSecond(item.getInt("timeEnd"));


                                        ZoneId zone = ZoneId.systemDefault();
                                        LocalDateTime ldt = LocalDateTime.ofInstant(instant, zone);
                                        String formatted = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                                                .format(ldt);
                                        if (item.getBoolean("running")){
                                            gamePauseList.add(new GamePauseInfo(name, "Đang chạy",formatted));
                                        }else {
                                            gamePauseList.add(new GamePauseInfo(name, "Không chạy",formatted));
                                        }
                                    }
                                }
                                pauseConfigRecycler.updateGameList(gamePauseList);
                                btnPause.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_down_arrow, 0);
                            } catch (JSONException e) {
                                Log.e(TAG, "Error parsing JSON", e);
                            }
                        });
                    } else {
                        gamePauseList.clear();
                        pauseConfigRecycler.updateGameList(gamePauseList);
                        btnPause.setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_up_arrow, 0);
                    }
                });
            }
            if (btnEditConfig != null){
                if (spinner.getSelectedItem() == null){
                    Toast.makeText(UpgradeMainClass.this, "Bạn chưa thêm thiết bị!", Toast.LENGTH_SHORT).show();
                }
                if (true) {
                    btnEditConfig.setOnClickListener(view -> {
                        timestamp.set(0L);
                        timeLimited.set(0);
                        AlertDialog.Builder builder = new AlertDialog.Builder(this);
                        LayoutInflater inflater = getLayoutInflater();
                        View dialogView = inflater.inflate(R.layout.dialog_edit, null);
                        builder.setView(dialogView);
                        ArrayList<String> listMode = new ArrayList<>(Arrays.asList("Allow", "Pause", "Limit", "Stop"));
                        TextView gameNameConfig = dialogView.findViewById(R.id.gameNameConfig);
                        SearchView searchView = dialogView.findViewById(R.id.findGameID);
                        ListView listViewGame = dialogView.findViewById(R.id.listViewGame);
                        Spinner chooseMode = dialogView.findViewById(R.id.chooseMode);
                        Button applyConfig = dialogView.findViewById(R.id.applyConfig);
                        LinearLayout mainContent = dialogView.findViewById(R.id.mainContent);
                        chooseMode.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, listMode));

                        ArrayList<String> listName = new ArrayList<>();
                        ArrayList<String> listNameTemp = new ArrayList<>();
                        for (int i = 0; i < listGame.length(); i++) {
                            try {
                                listName.add(listGame.getJSONObject(i).getString("name"));
                            } catch (JSONException e) {
                                Log.e(TAG, "Error parsing JSON", e);
                            }

                        }
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listNameTemp);
                        listViewGame.setAdapter(adapter);
                        AlertDialog dialog = builder.create();
                        dialog.show();
                        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                            @Override
                            public boolean onQueryTextSubmit(String query) {
                                return false;
                            }

                            @Override
                            public boolean onQueryTextChange(String newText) {
                                listViewGame.setVisibility(View.VISIBLE);  // hiện list khi gõ
                                listNameTemp.clear();
                                int count = 0;
                                for (int i = 0; i < listName.size(); i++) {
                                    if (listName.get(i).toLowerCase().contains(newText.toLowerCase()) && count <= 10) {
                                        listNameTemp.add(listName.get(i));
                                        count++;
                                    }
                                }
                                adapter.notifyDataSetChanged();
                                return false;
                            }
                        });
                        AtomicReference<Boolean> alreadyExit = new AtomicReference<>(false);
                        final AtomicReference<Integer>[] idGame = new AtomicReference[]{new AtomicReference<>(-1)};
                        listViewGame.setOnItemClickListener((parent, viewer, position, id) -> {
                            String selectedGame = adapter.getItem(position);
                            Toast.makeText(this, "Đã chọn: " + selectedGame, Toast.LENGTH_SHORT).show();
                            gameNameConfig.setText(selectedGame);

                            // Có thể ẩn list sau khi chọn
                            searchView.setQuery("", true);
                            searchView.clearFocus();
                            listViewGame.setVisibility(View.GONE);
                            /*Let me check if this game already had in config :improve gemini:*/

                            fetchDeviceConfig(spinner.getSelectedItem().toString(), (config) -> {
                                idGame[0].set(-1);

                                for (int i = 0; i < listGame.length(); i++) {
                                    try {
                                        JSONObject game = listGame.getJSONObject(i);
                                        if (game.getString("name").equals(selectedGame)) {

                                            idGame[0].set(game.getInt("id"));
                                            runOnUiThread(() -> {
                                                Toast.makeText(UpgradeMainClass.this, idGame[0].toString(), Toast.LENGTH_SHORT).show();

                                            });
                                            break;
                                        }
                                    } catch (JSONException e) {
                                        Log.e(TAG, "Error parsing JSON", e);
                                    }
                                }
                                try {
                                    AtomicReference<Boolean> flagExits = new AtomicReference<>(false);
                                    JSONArray items = config.getJSONArray(spinner.getSelectedItem().toString());
                                    for (int y = 0; y < items.length(); y++) {
                                        JSONObject item = items.getJSONObject(y);
                                        if (item.getInt("id") == idGame[0].get()) {
                                            runOnUiThread(() -> {
                                                try {
                                                    Toast.makeText(UpgradeMainClass.this, "Game đã có trong cấu hình", Toast.LENGTH_SHORT).show();
                                                    alreadyExit.set(true);
                                                    flagExits.set(true);
                                                    switch (item.getString("mode")) {
                                                        case "allow":
                                                            chooseMode.setSelection(0, true);
                                                            break;
                                                        case "pause":
                                                            chooseMode.setSelection(1, true);

                                                            break;
                                                        case "allow_limit":
                                                            chooseMode.setSelection(2, true);
                                                            break;
                                                    }

                                                } catch (JSONException e) {
                                                    Log.e(TAG, "Error parsing JSON", e);
                                                }
                                            });

                                        }
                                    }
                                    if (!flagExits.get()) {
                                        runOnUiThread(() -> {
                                            Toast.makeText(UpgradeMainClass.this, "Game chưa có trong cấu hình", Toast.LENGTH_SHORT).show();
                                            chooseMode.setSelection(3, true);
                                            alreadyExit.set(false);
                                        });
                                    }


                                } catch (JSONException e) {
                                    Log.e(TAG, "Error parsing JSON", e);
                                }
                            });
                        });
                        chooseMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                            @Override
                            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                runOnUiThread(() -> {

                                    mainContent.removeAllViews();
                                    Toast.makeText(UpgradeMainClass.this, "Đã chọn: " + listMode.get(position), Toast.LENGTH_SHORT).show();
                                    if (position == 0) {
                                        applyConfig.setText("Thêm game cho phép");
                                    }
                                    if (position == 1) {
                                        applyConfig.setText("Cho phép game tới khi tới thời gian đó");
                                        configPause(mainContent);
                                    }
                                    if (position == 2) {
                                        applyConfig.setText("Cho phép game tới khi tới giới hạn thời gian chơi");
                                        configLimit(mainContent);

                                    }
                                    if (position == 3) {
                                        applyConfig.setText("Chặn game");
                                    }


                                });
                            }

                            @Override
                            public void onNothingSelected(AdapterView<?> parent) {

                            }
                        });
                        applyConfig.setOnClickListener(viewers -> {
                            try {
                                Toast.makeText(this, idGame[0].toString(), Toast.LENGTH_SHORT).show();
                                if (idGame[0].get() != -1) {
                                    JSONObject jsonConfig = new JSONObject();
                                    jsonConfig.put("id", idGame[0]);
                                    if (chooseMode.getSelectedItemPosition() == 0) {
                                        jsonConfig.put("mode", "allow");
                                    }
                                    if (chooseMode.getSelectedItemPosition() == 2) {
                                        jsonConfig.put("mode", "allow_limit");
                                        jsonConfig.put("limit", timeLimited.get());
                                        jsonConfig.put("played", 0);

                                    }
                                    if (chooseMode.getSelectedItemPosition() == 1) {
                                        jsonConfig.put("mode", "pause");
                                        jsonConfig.put("timeEnd", timestamp.get());
                                    }
                                    if (chooseMode.getSelectedItemPosition() == 3) {
                                        jsonConfig.put("mode", "delete");
                                    }

                                    String jsons = jsonConfig.toString();
                                    HttpUrl url = HttpUrl.parse(BASE_URL + "/changeConfig").newBuilder()
                                            .addQueryParameter("device", spinner.getSelectedItem().toString())
                                            .addQueryParameter("jsons", jsons)
                                            .addQueryParameter("update", String.valueOf(false)).build();
                                    Map<String, String> headers = new HashMap<>();
                                    headers.put("jwt", sharedPref.getString(AUTH_TOKEN_KEY, ""));
                                    RequestBody emptyBody = RequestBody.create(new byte[0], null);
                                    apiCaller.patch(url.toString(), headers, emptyBody, new ApiCallback() {
                                        @Override
                                        public JSONObject onSuccess(@Nullable String responseBody) {
                                            Toast.makeText(UpgradeMainClass.this, "Cập nhật cấu hình thành công", Toast.LENGTH_SHORT).show();// Reset sau khi gửi
                                            timestamp.set(0L);
                                            timeLimited.set(0);
                                            idGame[0].set(-1);
                                            return null;
                                        }

                                        @Override
                                        public void onFailure(int statusCode, @Nullable String errorBody, @Nullable Exception e) {
                                            Log.e(TAG, "API failed - Status: " + statusCode + ", Error: " + errorBody, e);
                                            timestamp.set(0L);
                                            timeLimited.set(0);
                                            idGame[0].set(-1);
                                            Toast.makeText(UpgradeMainClass.this, "Lỗi cập nhật cấu hình", Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                                dialog.dismiss();
                            } catch (JSONException e) {
                                dialog.dismiss();
                                Log.e(TAG, "Error creating JSON", e);
                            }
                        });


                    });
                }

            }

            ScrollView rootLayout = findViewById(R.id.config_pages);
            gestureDetector = new GestureDetector(this, this);
            rootLayout.setOnTouchListener((v, event) -> gestureDetector.onTouchEvent(event));
        } catch (Exception e) { Log.e(TAG, "Error setting config_pages view!", e); }
    }
    private void saveLoginState(String token) {
        if (editor != null) {
            editor.putBoolean(LOGIN_STATUS_KEY, true);
            editor.putString(AUTH_TOKEN_KEY, token);
            editor.apply();
            Log.i(TAG, "Login state and token saved.");
            Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show();
            isLogged = true;
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
        if (this.gestureDetector != null && this.gestureDetector.onTouchEvent(event)) {
            return true; // Cử chỉ đã được xử lý và đã tiêu thụ sự kiện
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
        if (isLogged == true) {
            if (page == 0) {
                showConfigScreen();
                page = 1;
            }
            if (page == -1) {
                showMainWindow();
                page = 0;
            }
        }else{
            if (page == -1) {
                showLoginScreen();
                page = 0;
            }
        }
    }

    public void onSwipeLeft() {
        if (isLogged == true) {
            if (page == 0) {
                showSettingScreen();
                page = -1;
            }
            if (page == 1) {
                showMainWindow();
                page = 0;
            }
        }else {
            if (page == 0) {
                showSettingScreen();
                page = -1;
            }
        }
    }

    public void onSwipeTop() {
    }

    public void onSwipeBottom() {
    }
}