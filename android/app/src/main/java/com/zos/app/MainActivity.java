package com.zos.app;

import android.app.Activity;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
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
import android.content.Context;

public class MainActivity extends Activity {

    private WebView webView;
    private EditText urlInput;
    private LinearLayout settingsPanel;
    private SharedPreferences prefs;
    private PowerManager.WakeLock wakeLock;

    private static final String DEFAULT_URL = "https://preview-chat-8faf28d5-3f19-45cf-bd33-bdcf8ba3dcbc.space-z.ai/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("z-os", MODE_PRIVATE);

        // WakeLock to keep CPU running when screen is off (for background tasks)
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Z-OS::BackgroundWakeLock");

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);

        // Settings panel (URL changer)
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
        // Enable background JS execution
        settings.setMediaPlaybackRequiresUserGesture(false);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
            }

            @Override
            public void onReceivedError(WebView view, int errorCode, String description, String failingUrl) {
                // Show error page with retry
                view.loadData("<html><body style='background:#0a0a14;color:#00ff88;font-family:monospace;padding:20px;text-align:center;'>" +
                    "<h2>⚠ Connection lost</h2>" +
                    "<p>The server may have restarted.</p>" +
                    "<p>Tap below to retry.</p>" +
                    "<button onclick='location.reload()' style='background:#00ff88;color:#000;border:none;padding:10px 20px;font-family:monospace;cursor:pointer;border-radius:4px;'>↻ Retry</button>" +
                    "</body></html>", "text/html", "utf-8");
            }
        });

        webView.setWebChromeClient(new WebChromeClient());

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

        // Keep screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Acquire wake lock when app is active
        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire(60 * 60 * 1000L); // 1 hour max
        }
        // Reload if webView was paused
        if (webView != null) {
            webView.onResume();
            // Check if page is blank/error and reload
            webView.evaluateJavascript("(function(){ return document.body && document.body.innerText.length < 50; })();", value -> {
                if ("true".equals(value)) {
                    webView.reload();
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Keep wake lock to allow background fetch to complete
        // (released in onStop after 30s)
        if (webView != null) {
            webView.onResume(); // Don't actually pause JS — let fetches complete
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Release wake lock after 30 seconds (let in-flight requests complete)
        new android.os.Handler().postDelayed(() -> {
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
            }
        }, 30000);
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
        if (hasFocus) {
            webView.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }
}
