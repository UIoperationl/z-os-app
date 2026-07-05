package com.zos.app;

import android.app.Activity;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Button;
import android.widget.TextView;
import android.content.SharedPreferences;
import android.widget.Toast;
import android.net.Uri;
import android.os.Environment;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends Activity {

    private WebView webView;
    private EditText urlInput;
    private LinearLayout settingsPanel;
    private SharedPreferences prefs;

    private static final String DEFAULT_URL = "https://preview-chat-8faf28d5-3f19-45cf-bd33-bdcf8ba3dcbc.space-z.ai/";
    private static final int PERM_REQUEST = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("z-os", MODE_PRIVATE);

        // Request permissions (needed for downloads on Android 10+)
        requestPermissions();

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        // Settings panel (URL changer) - hidden by default
        settingsPanel = new LinearLayout(this);
        settingsPanel.setOrientation(LinearLayout.HORIZONTAL);
        settingsPanel.setPadding(8, 8, 8, 8);
        settingsPanel.setVisibility(LinearLayout.GONE);

        TextView label = new TextView(this);
        label.setText("URL: ");
        label.setPadding(0, 8, 8, 8);

        urlInput = new EditText(this);
        urlInput.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        urlInput.setText(prefs.getString("url", DEFAULT_URL));

        Button saveBtn = new Button(this);
        saveBtn.setText("Load");
        saveBtn.setOnClickListener(v -> {
            String url = urlInput.getText().toString().trim();
            if (!url.isEmpty()) {
                prefs.edit().putString("url", url).apply();
                webView.loadUrl(url);
                settingsPanel.setVisibility(LinearLayout.GONE);
                Toast.makeText(this, "Loading: " + url, Toast.LENGTH_SHORT).show();
            }
        });

        Button closeBtn = new Button(this);
        closeBtn.setText("✕");
        closeBtn.setOnClickListener(v -> settingsPanel.setVisibility(LinearLayout.GONE));

        settingsPanel.addView(label);
        settingsPanel.addView(urlInput);
        settingsPanel.addView(saveBtn);
        settingsPanel.addView(closeBtn);
        root.addView(settingsPanel);

        // WebView
        webView = new WebView(this);
        webView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT));
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setDatabaseEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setJavaScriptCanOpenWindowsAutomatically(true);
        settings.setSupportMultipleWindows(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }
        });

        webView.setWebChromeClient(new WebChromeClient());

        // DOWNLOAD LISTENER - handle file downloads via Android DownloadManager
        webView.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimetype, long contentLength) {
                try {
                    android.app.DownloadManager.Request request = new android.app.DownloadManager.Request(Uri.parse(url));
                    request.setMimeType(mimetype != null ? mimetype : "*/*");
                    request.allowScanningByMediaScanner();
                    request.setAllowedOverMetered(true);
                    request.setAllowedOverRoaming(true);
                    request.setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);

                    // Extract filename
                    String filename = "download";
                    if (contentDisposition != null && contentDisposition.contains("filename=")) {
                        String[] parts = contentDisposition.split("filename=");
                        if (parts.length > 1) {
                            filename = parts[1].replace("\"", "").replace(";", "").trim();
                        }
                    } else {
                        filename = url.substring(url.lastIndexOf('/') + 1);
                        if (filename.contains("?")) filename = filename.split("\\?")[0];
                    }
                    request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "Z-OS/" + filename);
                    request.setTitle(filename);

                    android.app.DownloadManager dm = (android.app.DownloadManager) getSystemService(DOWNLOAD_SERVICE);
                    dm.enqueue(request);

                    Toast.makeText(MainActivity.this, "Downloading: " + filename, Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    // Fallback: open in browser
                    try {
                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        startActivity(i);
                    } catch (Exception e2) {
                        Toast.makeText(MainActivity.this, "Download failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        // Enable immersive mode
        webView.setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            | View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        root.addView(webView);
        setContentView(root);

        // Load the URL
        String url = prefs.getString("url", DEFAULT_URL);
        webView.loadUrl(url);

        // Keep screen on while app is visible
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void requestPermissions() {
        // Only request permissions that are needed and not already granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ - POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, "android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{"android.permission.POST_NOTIFICATIONS"}, PERM_REQUEST);
            }
        }
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            // Android 9 and below - need storage permission for downloads
            if (ContextCompat.checkSelfPermission(this, "android.permission.WRITE_EXTERNAL_STORAGE") != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{"android.permission.WRITE_EXTERNAL_STORAGE"}, PERM_REQUEST);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // Don't care about result - app works without permissions, just no downloads
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (webView != null) webView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Don't pause WebView - let background JS continue
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView != null && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            settingsPanel.setVisibility(settingsPanel.getVisibility() == LinearLayout.VISIBLE ? LinearLayout.GONE : LinearLayout.VISIBLE);
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus && webView != null) {
            webView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }
}
