package aca.com.remote.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.Toast;

import aca.com.remote.tunes.BackendService;
import aca.com.remote.tunes.daap.Library;
import aca.com.remote.tunes.daap.Session;
import aca.com.remote.R;
import aca.com.remote.uitl.CommonUtils;

/**
 * Created by Administrator on 2017/6/27.
 */

public class ShoutcastActivity extends BaseActivity implements View.OnClickListener {
    public static final String TAG = ShoutcastActivity.class.toString();

    private WebView shoutcast_webview;
    private ProgressBar shoutcast_loading_bar;
    //private Button shoutcast_btn_back;
    //private Button shoutcast_btn_top;
    //private Button shoutcast_btn_refresh;
    //private TextView shoutcast_txt_title;
    private long exitTime;
    private ActionBar ab;

    private BackendService backend;
    private Session session;
    private  Library library;

    public ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            try {
                backend = ((BackendService.BackendBinder) service).getService();

                Log.w(TAG, "onServiceConnected for ShoutcastActivity");

                session = backend.getSession();
                if (session == null) {
                    return;
                }

                library = new Library(session);
            } catch (Exception e) {
                Log.e(TAG, "onServiceConnected:"+e.getMessage());
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            backend = null;
            session = null;
        }
    };

    @Override
    public void onStart(){
        super.onStart();
        this.bindService(new Intent(this, BackendService.class), connection,
                Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop(){
        super.onStop();
        this.unbindService(connection);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shoutcast);

        this.initWebView();
    }

    private void initWebView() {
        shoutcast_webview = (WebView)findViewById(R.id.shoutcast_webview);
        shoutcast_loading_bar = (ProgressBar)findViewById(R.id.shoutcast_loading_bar);
        //shoutcast_btn_back = (Button)findViewById(R.id.shoutcast_btn_back);
        //shoutcast_btn_top = (Button)findViewById(R.id.shoutcast_btn_top);
        //shoutcast_btn_refresh = (Button)findViewById(R.id.shoutcast_btn_refresh);
        //shoutcast_txt_title = (TextView)findViewById(R.id.shoutcast_txt_title);

        WebSettings shoutcast_webSettings = shoutcast_webview.getSettings();
        shoutcast_webSettings.setJavaScriptEnabled(true);

        shoutcast_webview.setWebViewClient(new shoutcastWebViewClient());
        shoutcast_webview.setWebChromeClient(new shoutcastWebChromeClient());
        shoutcast_webview.loadUrl("http://www.shoutcast.com/");


        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        ((AppCompatActivity) this).setSupportActionBar(toolbar);
        toolbar.setPadding(0, CommonUtils.getStatusHeight(this), 0, 0);

        ab = ((AppCompatActivity) this).getSupportActionBar();
        ab.setHomeAsUpIndicator(R.drawable.actionbar_back);
        ab.setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        //shoutcast_btn_back.setOnClickListener(this);
        //shoutcast_btn_top.setOnClickListener(this);
        //shoutcast_btn_refresh.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        /*
        switch (v.getId()) {
            case R.id.shoutcast_btn_back:
                onBackPressed();
                break;
            case R.id.shoutcast_btn_top:
                this.shoutcast_webview.setScaleY(0);
                break;
            case R.id.shoutcast_btn_refresh:
                shoutcast_webview.reload();
                break;
        }
        */
    }

    @Override
    public void onBackPressed() {
        if (shoutcast_webview.canGoBack()) {
            shoutcast_webview.goBack();
        } else {
            if ((System.currentTimeMillis()-exitTime) > 2000 ) {
                Toast.makeText(getApplicationContext(), "Press back again to exit",
                        Toast.LENGTH_SHORT).show();
                exitTime = System.currentTimeMillis();
            } else {
                finish();
            }
        }
    }

    private class shoutcastWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            super.onPageStarted(view, url, favicon);
            shoutcast_loading_bar.setVisibility(View.VISIBLE);
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            shoutcast_loading_bar.setVisibility(View.GONE);
        }

        @Override
        public void onReceivedError(WebView view, int errorCode,
                                    String description, String failingUrl) {
            super.onReceivedError(view, errorCode, description, failingUrl);
        }

        @Override
        public void doUpdateVisitedHistory(WebView view, String url,
                                           boolean isReload) {
            super.doUpdateVisitedHistory(view, url, isReload);
        }

        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, String url) {
            WebResourceResponse response = null;
            String shoutcastUrl;

            if (url.contains("?icy=")) {
                shoutcastUrl = url.substring(0, url.lastIndexOf('/'));
                Log.d(TAG, "shoutcastUrl:"+shoutcastUrl);
                library.setShoutcastUrl(shoutcastUrl);
            }

            return response;
        }
    }

    private class shoutcastWebChromeClient extends WebChromeClient {
        @Override
        public void onProgressChanged(WebView view, int newProgress) {
            super.onProgressChanged(view, newProgress);
        }

        @Override
        public void onReceivedTitle(WebView view, String title) {
            super.onReceivedTitle(view, title);
            //shoutcast_txt_title.setText(title);
        }
    }
}
