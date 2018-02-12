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
import aca.com.remote.tunes.SessionWrapper;
import aca.com.remote.tunes.daap.ActionErrorListener;
import aca.com.remote.tunes.daap.Library;
import aca.com.remote.tunes.daap.Session;
import aca.com.remote.R;
import aca.com.remote.tunes.util.Constants;
import aca.com.remote.tunes.util.RadioRequestCallback;
import aca.com.remote.tunes.util.ShoutCastRadioGenre;
import aca.com.remote.tunes.util.ShoutCastRadioStation;
import aca.com.remote.tunes.util.ShoutCastRequest;
import aca.com.remote.tunes.util.ThreadExecutor;
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
    public String curHost;
    public String curHostLibrary;
    private BackendService backend;
    public Session session;
    private Library library;

    public final static int tryCnt = 40;
    protected ActionErrorListener mLibraryErrListener = new ActionErrorListener() {
        @Override
        public void onActionError(int code) {
            Log.i("wwj","shoutcast request code:"+code);
        }
    };

    public ServiceConnection connection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, final IBinder service) {
            ThreadExecutor.runTask(new Runnable() {

                public void run() {
                    int timeout=0;
                    backend = ((BackendService.BackendBinder) service).getService();
                    if (backend == null)
                        return;
                    try {
                        do {
                            Thread.sleep(300);
                            Log.d("wwj", "get session for host:" + curHost);
                            SessionWrapper sessionWrapper = backend.getSession(curHost);
                            if (null != sessionWrapper) {
                                if (!sessionWrapper.isTimeout()) {
                                    session = sessionWrapper.getSession(curHost);
                                }else{
                                    timeout = tryCnt;
                                }
                                Log.d("wwj", sessionWrapper.toString());
                            } else {
                                Log.w("wwj", "waiting session to been created");
                            }

                            timeout++;
                            if (timeout > tryCnt) {
                                if (null == session) {
                                    session = backend.getSession(curHost, curHostLibrary);
                                    Log.w("wwj", "------force create session !");
                                }
                                break;
                            }
                        } while ((null == session) && (null != backend));


                        if (session == null)
                            return;

                        updateTrackInfo(session);
                        backend.updateCurSession(session);

                        // begin search now that we have a backend
                        library = new Library(session, mLibraryErrListener);
                    }catch (Exception e){
                        Log.e("wwj", "onServiceConnected:"+e.getMessage());
                    }
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            backend = null;
            session = null;
            library = null;
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

        curHost = getIntent().getStringExtra(Constants.EXTRA_ADDRESS);
        curHostLibrary  = getIntent().getStringExtra(Constants.EXTRA_LIBRARY);

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
                        if (null == session) {
                            Toast.makeText(ShoutcastActivity.this, R.string.error_no_speaker_selected, Toast.LENGTH_SHORT).show();
                            break;
                        }
                        session.setRadioTunesUrl(str);
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

    @Override
    public void onBackPressed() {
        if (listFragments.size() > 1) {
            removeFragment();
        } else
            super.onBackPressed();
    }
}
