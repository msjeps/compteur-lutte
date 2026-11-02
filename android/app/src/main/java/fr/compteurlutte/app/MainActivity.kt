package fr.compteurlutte.app

import android.annotation.SuppressLint
import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.webkit.ServiceWorkerClientCompat
import androidx.webkit.ServiceWorkerControllerCompat
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewAssetLoader
import androidx.webkit.WebViewFeature
import java.io.IOException
import java.io.OutputStreamWriter
import androidx.activity.result.contract.ActivityResultContracts
import org.json.JSONObject

class MainActivity : AppCompatActivity() {

    companion object {
        private const val APP_HOST = "appassets.androidplatform.net"
    }

    private lateinit var webView: WebView

    private var pendingContent: String = ""
    private var pendingFilename: String = ""

    private val createDocumentLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("*/*")
    ) { uri ->
        val contenu = pendingContent
        pendingContent = ""
        pendingFilename = ""
        when {
            uri == null ->
                notifierPage("Export annule.", "warning")
            contenu.isEmpty() ->
                notifierPage("Erreur : relance l'export.", "warning")
            else -> try {
                contentResolver.openOutputStream(uri)?.use { stream ->
                    OutputStreamWriter(stream, Charsets.UTF_8).use { writer ->
                        writer.write(contenu)
                    }
                } ?: throw IOException("Fichier inaccessible")
                notifierPage("Fichier enregistre avec succes !", "success")
            } catch (e: Exception) {
                notifierPage("Erreur : ${e.message ?: "erreur inconnue"}", "warning")
            }
        }
    }

    private fun notifierPage(texte: String, classe: String) {
        webView.post {
            webView.evaluateJavascript(
                "if(typeof window.__androidNotif==='function'){window.__androidNotif(${JSONObject.quote(texte)},${JSONObject.quote(classe)});}",
                null
            )
        }
    }

    inner class WebAppInterface(private val activity: Activity) {

        @JavascriptInterface
        fun download(filename: String, mimeType: String, content: String) {
            if (content.length > 5_000_000) {
                notifierPage("Export trop volumineux (plus de 5 Mo).", "warning")
                return
            }
            val safeName = filename.replace(Regex("[^A-Za-z0-9àâäéèêëïîôùûüçÀÂÄÉÈÊËÏÎÔÙÛÜÇ._\\- ]"), "_")
                .take(120).ifEmpty { "export" }
            pendingContent = content
            pendingFilename = safeName
            activity.runOnUiThread {
                createDocumentLauncher.launch(safeName)
            }
        }

        @JavascriptInterface
        fun quitApp() {
            activity.runOnUiThread { activity.finish() }
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_main)
        webView = findViewById(R.id.webView)
        masquerBarresSysteme()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                webView.evaluateJavascript(
                    "(typeof window.__androidBack === 'function') ? window.__androidBack() : false"
                ) { reponse -> if (reponse != "true") finish() }
            }
        })

        val assetLoader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(this))
            .build()

        webView.webViewClient = object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest) =
                assetLoader.shouldInterceptRequest(request.url)

            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                request.url.host != APP_HOST
        }

        webView.addJavascriptInterface(WebAppInterface(this), "AndroidDownload")

        with(webView.settings) {
            javaScriptEnabled                  = true
            domStorageEnabled                  = true
            allowFileAccess                    = false
            allowContentAccess                 = false
            cacheMode                          = WebSettings.LOAD_DEFAULT
            setSupportZoom(false)
            setSupportMultipleWindows(false)
            javaScriptCanOpenWindowsAutomatically = false
            mediaPlaybackRequiresUserGesture   = true
        }

        if (WebViewFeature.isFeatureSupported(WebViewFeature.SAFE_BROWSING_ENABLE)) {
            WebSettingsCompat.setSafeBrowsingEnabled(webView.settings, true)
        }

        if (WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_BASIC_USAGE)) {
            ServiceWorkerControllerCompat.getInstance()
                .setServiceWorkerClient(object : ServiceWorkerClientCompat() {
                    override fun shouldInterceptRequest(request: WebResourceRequest) =
                        assetLoader.shouldInterceptRequest(request.url)
                })
        }

        if (savedInstanceState != null) {
            webView.restoreState(savedInstanceState)
        } else {
            webView.loadUrl("https://appassets.androidplatform.net/assets/index.html")
        }
    }

    private fun masquerBarresSysteme() {
        val controleur = WindowInsetsControllerCompat(window, window.decorView)
        controleur.hide(WindowInsetsCompat.Type.systemBars())
        controleur.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) masquerBarresSysteme()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        webView.saveState(outState)
    }
}
