package com.aditya.adsterraapp

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.Menu
import android.view.MenuItem
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

private const val LOGIN_URL = "https://beta.publishers.adsterra.com/"
private const val PREFS_FILE = "adsterra_secure_prefs"
private const val KEY_EMAIL = "email"
private const val KEY_PASSWORD = "password"

class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var prefs: android.content.SharedPreferences

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val masterKey = MasterKey.Builder(this)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        prefs = EncryptedSharedPreferences.create(
            this,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        webView = findViewById(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.saveFormData = false // biar aman, kita yang handle sendiri

        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                super.onPageFinished(view, url)
                val email = prefs.getString(KEY_EMAIL, null)
                val password = prefs.getString(KEY_PASSWORD, null)
                if (email != null && password != null) {
                    injectAutoLogin(email, password)
                }
            }
        }

        if (prefs.getString(KEY_EMAIL, null) == null) {
            showCredentialDialog()
        }

        webView.loadUrl(LOGIN_URL)
    }

    override fun onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack()
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menu.add(0, 1, 0, "Ubah Email/Password")
        menu.add(0, 2, 1, "Refresh")
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            1 -> showCredentialDialog()
            2 -> webView.loadUrl(LOGIN_URL)
        }
        return true
    }

    private fun showCredentialDialog() {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        val padding = (16 * resources.displayMetrics.density).toInt()
        layout.setPadding(padding, padding, padding, padding)

        val emailInput = EditText(this)
        emailInput.hint = "Email Adsterra"
        emailInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        emailInput.setText(prefs.getString(KEY_EMAIL, ""))
        layout.addView(emailInput)

        val passwordInput = EditText(this)
        passwordInput.hint = "Password Adsterra"
        passwordInput.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        passwordInput.setText(prefs.getString(KEY_PASSWORD, ""))
        layout.addView(passwordInput)

        AlertDialog.Builder(this)
            .setTitle("Login Adsterra")
            .setMessage("Data ini disimpan terenkripsi cuma di HP ini.")
            .setView(layout)
            .setCancelable(false)
            .setPositiveButton("Simpan") { _, _ ->
                prefs.edit()
                    .putString(KEY_EMAIL, emailInput.text.toString().trim())
                    .putString(KEY_PASSWORD, passwordInput.text.toString())
                    .apply()
                webView.loadUrl(LOGIN_URL)
            }
            .show()
    }

    /**
     * Adsterra dashboard-nya SPA (React/Vue), form login di-render pakai JS
     * setelah halaman kebuka, jadi kita nunggu field-nya muncul dulu (polling
     * tiap 300ms selama max ~6 detik), baru diisi.
     *
     * Kalau ini gak jalan di HP kamu, buka halaman ini di Chrome desktop,
     * inspect element field email & password-nya, terus kasih tau id/name
     * yang sebenarnya biar selector-nya disesuaikan.
     */
    private fun injectAutoLogin(email: String, password: String) {
        val escapedEmail = email.replace("\\", "\\\\").replace("\"", "\\\"")
        val escapedPassword = password.replace("\\", "\\\\").replace("\"", "\\\"")

        val js = """
            (function() {
                var attempts = 0;
                var maxAttempts = 20; // 20 x 300ms = 6 detik
                var timer = setInterval(function() {
                    attempts++;
                    var emailField = document.querySelector(
                        'input[type="email"], input[name="email"], input[name="login"], input[id*="email" i], input[id*="login" i], input[autocomplete="username"]'
                    );
                    var passField = document.querySelector(
                        'input[type="password"], input[name="password"], input[id*="password" i], input[autocomplete="current-password"]'
                    );

                    if (emailField && passField) {
                        clearInterval(timer);

                        function setNativeValue(el, value) {
                            var proto = window.HTMLInputElement.prototype;
                            var setter = Object.getOwnPropertyDescriptor(proto, 'value').set;
                            setter.call(el, value);
                            el.dispatchEvent(new Event('input', { bubbles: true }));
                            el.dispatchEvent(new Event('change', { bubbles: true }));
                        }

                        setNativeValue(emailField, "$escapedEmail");
                        setNativeValue(passField, "$escapedPassword");

                        setTimeout(function() {
                            var submitBtn = document.querySelector('button[type="submit"]');
                            if (!submitBtn) {
                                var buttons = document.querySelectorAll('button');
                                for (var i = 0; i < buttons.length; i++) {
                                    var txt = (buttons[i].innerText || '').toLowerCase();
                                    if (txt.indexOf('log') !== -1 || txt.indexOf('masuk') !== -1 || txt.indexOf('sign') !== -1) {
                                        submitBtn = buttons[i];
                                        break;
                                    }
                                }
                            }
                            if (submitBtn) {
                                submitBtn.click();
                            }
                        }, 400);

                    } else if (attempts >= maxAttempts) {
                        clearInterval(timer);
                    }
                }, 300);
            })();
        """.trimIndent()

        webView.evaluateJavascript(js, null)
    }
}
