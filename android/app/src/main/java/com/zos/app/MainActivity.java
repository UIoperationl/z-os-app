package com.zos.app;

import android.app.Activity;
import android.os.Bundle;
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

public class MainActivity extends Activity {

    private WebView webView;
    private EditText urlInput;
    private LinearLayout settingsPanel;
    private SharedPreferences prefs;

    // Default URL - the Z-OS sandbox
    // User can change this in settings (e.g. to a deployed Vercel URL)
    private static final String DEFAULT_URL = "https://preview-chat-8faf28d5-3f19-45cf-bd33-bdcf8ba3dcbc.space-z.ai/";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        prefs = getSharedPreferences("z-os", MODE_PRIVATE);

        // Create layout
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

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                return false;
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
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        // Show settings on long-press menu key or volume down twice
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
