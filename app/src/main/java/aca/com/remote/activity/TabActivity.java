package aca.com.remote.activity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;

import aca.com.remote.R;
import aca.com.remote.fragment.ArtistDetailFragment;
import aca.com.remote.fragment.TabPagerFragment;
import aca.com.remote.tunes.BackendService;
import aca.com.remote.tunes.SessionWrapper;
import aca.com.remote.tunes.daap.ActionErrorListener;
import aca.com.remote.tunes.daap.Library;
import aca.com.remote.tunes.daap.Session;
import aca.com.remote.tunes.util.Constants;
import aca.com.remote.tunes.util.ThreadExecutor;

/**
 * Created by wm on 2016/4/11.
 */
public class TabActivity extends BaseActivity {

    private int page, artistId, albumId;
    public String curHost;
    public String curHostLibrary;
    private BackendService backend;
    public Session session;
    private Library library;

    public final static int tryCnt = 40;
    protected ActionErrorListener mLibraryErrListener = new ActionErrorListener() {
        @Override
        public void onActionError(int code) {
            Log.i("wwj","http request code:"+code);
        }
    };

    public ServiceConnection connectionTunes = new ServiceConnection() {
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

    @Override
    public void onCreate(final Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        curHost = getIntent().getStringExtra(Constants.EXTRA_ADDRESS);
        curHostLibrary  = getIntent().getStringExtra(Constants.EXTRA_LIBRARY);

        if (getIntent().getExtras() != null) {
            page = getIntent().getIntExtra("page_number", 0);
            artistId = getIntent().getIntExtra("artist", 0);
            albumId = getIntent().getIntExtra("album", 0);
        }
        setContentView(R.layout.activity_tab);

        if (artistId != 0) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            ArtistDetailFragment fragment = ArtistDetailFragment.newInstance(artistId);
            transaction.hide(getSupportFragmentManager().findFragmentById(R.id.tab_container));
            transaction.add(R.id.tab_container, fragment);
            transaction.addToBackStack(null).commit();
        }
        if (albumId != 0) {

        }

        String[] title = {"Songs", "Artists", "Albums", "Folders"};
        TabPagerFragment fragment = TabPagerFragment.newInstance(page, title);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.tab_container, fragment);
        transaction.commitAllowingStateLoss();

    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(new Intent(this, BackendService.class), connectionTunes, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(connectionTunes);
    }
}
