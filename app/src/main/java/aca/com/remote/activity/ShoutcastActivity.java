package aca.com.remote.activity;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
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
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import aca.com.remote.fragment.RadioListFragment;
import aca.com.remote.fragmentnet.RankingFragment;
import aca.com.remote.tunes.BackendService;
import aca.com.remote.tunes.daap.Library;
import aca.com.remote.tunes.daap.Session;
import aca.com.remote.R;
import aca.com.remote.tunes.util.RadioRequestCallback;
import aca.com.remote.tunes.util.ShoutCastRadioGenre;
import aca.com.remote.tunes.util.ShoutCastRadioStation;
import aca.com.remote.tunes.util.ShoutCastRequest;
import aca.com.remote.uitl.CommonUtils;

/**
 * Created by Administrator on 2017/6/27.
 */

public class ShoutcastActivity extends BaseActivity {
    public static final String TAG = ShoutcastActivity.class.toString();

    private WebView shoutcast_webview;
    private ProgressBar shoutcast_loading_bar;
    private long exitTime;
    private ActionBar ab;
    private ShoutCastRequest mShoutCast;
    private String tune_base = null;

    private List<Fragment> listFragments = new ArrayList<>();

    private BackendService backend;
    private Session session;
    private Library library = null;

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

    private AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Object o = ((RadioListFragment)listFragments.get(listFragments.size() - 1)).getList().get(position);
            RadioListFragment fg;

            if (o instanceof ShoutCastRadioGenre) {
                fg = new RadioListFragment();
                String name = ((ShoutCastRadioGenre)o).getName();
                Bundle bundle = new Bundle();
                bundle.putString("title", name);
                fg.setArguments(bundle);
                addFragment(fg, "Fragment_"+listFragments.size());
                ((RadioListFragment)listFragments.get(listFragments.size() - 1)).setOnItemClickListener(itemClickListener);
                if (name.equals(getString(R.string.shoutcast_top_20_station))) {
                    mShoutCast.getTop500Stations(20, 0, null);
                } else if (name.equals(getString(R.string.shoutcast_genre))) {
                    mShoutCast.getPrimaryGenre();
                } else {
                    if (((ShoutCastRadioGenre) o).getHasChildren())
                        mShoutCast.getSecondGenre(((ShoutCastRadioGenre) o).getId());
                    else
                        mShoutCast.getStationsByBitrateOrCodecTypeOrGenreID(
                                0, null, ((ShoutCastRadioGenre) o).getId(), 0, null);
                }
            } else if (o instanceof ShoutCastRadioStation) {
                /** station tune**/
                if (null != tune_base)
                    mShoutCast.tuneIntoStation(tune_base, ((ShoutCastRadioStation) o).getId());
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shoutcast);

        /** init view **/
        //action back
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

        RadioListFragment fg = new RadioListFragment();
        Bundle bundle = new Bundle();
        bundle.putString("title", "ShoutCast");
        fg.setArguments(bundle);
        fg.setOnItemClickListener(itemClickListener);
        addFragment(fg, "" + listFragments.size());

        mShoutCast = new ShoutCastRequest(null);
        mShoutCast.setShoutCastCallback(new RadioRequestCallback() {
            @Override
            public void messageCallback(int type, Object obj) {
                switch (type) {
                    case ShoutCastRequest.eSHOUTCAST_MSG_STATUS_CODE:
                        break;
                    case ShoutCastRequest.eSHOUTCAST_MSG_STATUS_TEXT:
                        break;
                    case ShoutCastRequest.eSHOUTCAST_MSG_GENRE:
                        if (!listFragments.isEmpty()) {
                            RadioListFragment fg = (RadioListFragment)listFragments.get(listFragments.size()-1);
                            if (!fg.getXmlLoadStatus())
                                fg.addItem(obj);
                        }
                        break;
                    case ShoutCastRequest.eSHOUTCAST_MSG_STATION:
                        if (!listFragments.isEmpty()) {
                            RadioListFragment fg = (RadioListFragment)listFragments.get(listFragments.size()-1);
                            if (!fg.getXmlLoadStatus())
                                fg.addItem(obj);
                        }
                        break;
                    case ShoutCastRequest.eSHOUTCAST_MSG_TUNE_BASE:
                        tune_base = obj.toString();
                        break;
                    case ShoutCastRequest.eSHOUTCAST_MSG_URL:
                        final String str = obj.toString();
                        if (null == library) {
                            Toast.makeText(ShoutcastActivity.this, R.string.error_no_speaker_selected, Toast.LENGTH_SHORT).show();
                            break;
                        }
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                if (null != library) {
                                    library.setRadioTunesUrl(str);
                                }
                            }
                        }).start();
                        break;
                    case ShoutCastRequest.eSHOUTCAST_MSG_XML_PARSER_START:
                        /** no need to set status to false, because status will be set to false when
                         * constructed. avoid parent fragment status change to false when changing
                         * from child fragment to parent fragment quickly**/
                        break;
                    case ShoutCastRequest.eSHOUTCAST_MSG_XML_PARSER_END:
                        if (!listFragments.isEmpty())
                            ((RadioListFragment)listFragments.get(listFragments.size()-1)).setXmlLoadStatus(true);
                        break;
                    default:
                        Log.e(TAG, "Un-know msg type!!!");
                }
            }
        });
    }

    @Override
    public void onStart(){
        super.onStart();
        this.bindService(new Intent(this, BackendService.class), connection,
                Context.BIND_AUTO_CREATE);
        RadioListFragment fg = (RadioListFragment) listFragments.get(0);
        if (!fg.getXmlLoadStatus()) {
            fg.addItem(new ShoutCastRadioGenre(getString(R.string.shoutcast_top_20_station), -1, -1, false));
            fg.addItem(new ShoutCastRadioGenre(getString(R.string.shoutcast_genre), -1, -1, false));
//            fg.addItem(new ShoutCastRadioGenre(getString(R.string.shoutcast_country_location), -1, -1, false));
            fg.setXmlLoadStatus(true);
        }
    }

    @Override
    public void onStop(){
        super.onStop();
        this.unbindService(connection);
    }

    private void addFragment(Fragment fragment, String tag) {
        FragmentManager manager = getFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        if (!listFragments.isEmpty())
            transaction.hide(listFragments.get(listFragments.size()-1));
        transaction.add(R.id.radio_list_container, fragment, tag);
        listFragments.add(fragment);
        transaction.addToBackStack(tag);
        transaction.commit();
    }

    private void removeFragment() {
        FragmentManager manager = getFragmentManager();
        manager.popBackStack();
        if (!listFragments.isEmpty())
            listFragments.remove(listFragments.size()-1);
    }

    private void initWebView() {
//        shoutcast_webview = (WebView)findViewById(R.id.shoutcast_webview);
//        shoutcast_loading_bar = (ProgressBar)findViewById(R.id.shoutcast_loading_bar);
//
//        WebSettings shoutcast_webSettings = shoutcast_webview.getSettings();
//        shoutcast_webSettings.setJavaScriptEnabled(true);
//
//        shoutcast_webview.setWebViewClient(new shoutcastWebViewClient());
//        shoutcast_webview.setWebChromeClient(new shoutcastWebChromeClient());
//        shoutcast_webview.loadUrl("http://www.shoutcast.com/");
//
//        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
//        ((AppCompatActivity) this).setSupportActionBar(toolbar);
//        toolbar.setPadding(0, CommonUtils.getStatusHeight(this), 0, 0);
//
//        ab = ((AppCompatActivity) this).getSupportActionBar();
//        ab.setHomeAsUpIndicator(R.drawable.actionbar_back);
//        ab.setDisplayHomeAsUpEnabled(true);
//        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                onBackPressed();
//            }
//        });
    }

    @Override
    public void onBackPressed() {
        if (listFragments.size() > 1) {
            removeFragment();
        } else
            super.onBackPressed();
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
