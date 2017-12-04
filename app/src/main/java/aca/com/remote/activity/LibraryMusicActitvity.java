package aca.com.remote.activity;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import java.util.Collections;
import java.util.HashSet;

import aca.com.remote.R;
import aca.com.remote.fragment.LibraryMusicArtistsFragment;
import aca.com.remote.tunes.BackendService;
import aca.com.remote.tunes.SessionWrapper;
import aca.com.remote.tunes.daap.ActionErrorListener;
import aca.com.remote.tunes.daap.Library;
import aca.com.remote.tunes.daap.Session;
import aca.com.remote.tunes.util.Constants;
import aca.com.remote.tunes.util.ThreadExecutor;
import aca.com.remote.fragment.LibraryMusicAlbumsFragment;
import aca.com.remote.fragment.LibraryMusicPlaylistFragment;
import aca.com.remote.uitl.CommonUtils;

/**
 * Created by ali_mac on 2017/11/20.
 */

public class LibraryMusicActitvity extends BaseActivity implements ViewPager.OnPageChangeListener {

    public static final String TAG = LibraryMusicActitvity.class.toString();
    public String curHost;
    public String curHostLibrary;
    public BackendService backend;
    public Session session;
    public Library library;
    public ViewPager pager;
    public TabHandler handler;
    public int positionViewed;
    public final static int tryCnt = 40;

    private SharedPreferences prefs;
    private LibraryMusicArtistsFragment artists = null;
    private LibraryMusicAlbumsFragment albums = null;
    private LibraryMusicPlaylistFragment playlists = null;

    public boolean isConnected = false;
    public boolean isTablet = false;

    private final HashSet<ConnectionListener> listeners = new HashSet<ConnectionListener>();

    protected ActionErrorListener mLibraryErrListener = new ActionErrorListener() {
        @Override
        public void onActionError(int code) {
            Log.d(TAG,"http request code:"+code);
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
                            Log.d(TAG, "get session for host:" + curHost);
                            SessionWrapper sessionWrapper = backend.getSession(curHost);
                            if (null != sessionWrapper) {
                                if (!sessionWrapper.isTimeout()) {
                                    session = sessionWrapper.getSession(curHost);
                                }else{
                                    timeout = tryCnt;
                                }
                                Log.d(TAG, sessionWrapper.toString());
                            } else {
                                Log.w(TAG, "waiting session to been created");
                            }

                            timeout++;
                            if (timeout > tryCnt) {
                                if (null == session) {
                                    session = backend.getSession(curHost, curHostLibrary);
                                    Log.w(TAG, "------force create session !");
                                }
                                break;
                            }
                        } while ((null == session) && (null != backend));


                        if (session == null)
                            return;

                        updateTrackInfo(session);
                        backend.updateCurSession(session);

                        // begin search now that we have a backend
                        library = new Library(session,mLibraryErrListener);

                        if (listeners.size() > 0)
                            for (ConnectionListener l : listeners)
                                l.onServiceConnected();
                        isConnected = true;
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            for (ConnectionListener l : listeners)
                l.onServiceDisconnected();
            backend = null;
            session = null;
            library = null;
        }
    };

    @SuppressLint("NewApi")
    @Override
    public void onCreate(Bundle saved) {
        super.onCreate(saved);
        this.prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (this.prefs.getBoolean(this.getString(R.string.pref_fullscreen), true)
                && Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            this.requestWindowFeature(Window.FEATURE_NO_TITLE);
        }
        setContentView(R.layout.activity_library_music);
        curHost = getIntent().getStringExtra(Constants.EXTRA_ADDRESS);
        curHostLibrary  = getIntent().getStringExtra(Constants.EXTRA_LIBRARY);
        artists = new LibraryMusicArtistsFragment();
        albums = new LibraryMusicAlbumsFragment();
        playlists = new LibraryMusicPlaylistFragment();

//    wwj  isTablet = findViewById(R.id.frame_artists) != null;

        if (!isTablet) {

            (pager = (ViewPager) findViewById(R.id.view_pager)).setAdapter(new LibraryPagerAdapter(
                    getSupportFragmentManager()));
            pager.setOnPageChangeListener(this);
            pager.setOffscreenPageLimit(2);
            findViewById(R.id.tab_artists).setSelected(true);

        } else {
/* wwj
         getSupportFragmentManager().beginTransaction().add(R.id.frame_artists, artists).add(R.id.frame_albums, albums)
                  .add(R.id.frame_playlists, playlists).commit();
         registerListener(artists, albums, playlists);
         setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
*/
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        ((AppCompatActivity) this).setSupportActionBar(toolbar);
        toolbar.setPadding(0, CommonUtils.getStatusHeight(this), 0, 0);

        ActionBar ab = ((AppCompatActivity) this).getSupportActionBar();
        ab.setHomeAsUpIndicator(R.drawable.actionbar_back);
        ab.setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        TextView text = (TextView) findViewById(R.id.toolbar_text);
        text.setText("Device Music");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            handler = new TabHandler();
         /* wwj
         ActionBar ab = getActionBar();
         ab.setTitle(R.string.control_menu_library);
         ab.setHomeButtonEnabled(true);
         ab.setDisplayHomeAsUpEnabled(true);
         if (!isTablet) {
            findViewById(R.id.legacy_tabs).setVisibility(View.GONE);
            ab.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
            Tab artTab = ab.newTab().setText(R.string.control_menu_artists).setTabListener(handler);
            Tab albTab = ab.newTab().setText(R.string.control_menu_albums).setTabListener(handler);
            Tab plyTab = ab.newTab().setText(R.string.control_menu_playlists).setTabListener(handler);
            ab.addTab(artTab);
            ab.addTab(albTab);
            ab.addTab(plyTab);
         }
*/
        }

    }

    @Override
    public void onStart() {
        super.onStart();
        bindService(new Intent(this, BackendService.class), connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    public void onStop() {
        super.onStop();
        unbindService(connection);
    }

    @Override
    protected void onResume() {
        /*wwj
        final boolean fullscreen = this.prefs.getBoolean(this.getString(R.string.pref_fullscreen), true);
        if (fullscreen) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }*/
        super.onResume();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.act_library_browse, menu);
        return true;
    }

    public void tabSelected(View v) {
        int id = v.getId();
        if (id == R.id.tab_album) {
            pager.setCurrentItem(1, true);
            findViewById(R.id.tab_album).setSelected(true);
            findViewById(R.id.tab_artists).setSelected(false);
            findViewById(R.id.tab_playlists).setSelected(false);
        }
        if (id == R.id.tab_artists) {
            pager.setCurrentItem(0, true);
            findViewById(R.id.tab_album).setSelected(false);
            findViewById(R.id.tab_artists).setSelected(true);
            findViewById(R.id.tab_playlists).setSelected(false);
        }
        if (id == R.id.tab_playlists) {
            pager.setCurrentItem(2, true);
            findViewById(R.id.tab_album).setSelected(false);
            findViewById(R.id.tab_artists).setSelected(false);
            findViewById(R.id.tab_playlists).setSelected(true);
        }
    }

    public void registerListener(ConnectionListener... l) {
        Collections.addAll(listeners, l);
    }

    public boolean unregisterListener(ConnectionListener key) {
        return listeners.remove(key);
    }

    public class LibraryPagerAdapter extends FragmentStatePagerAdapter {

        public LibraryPagerAdapter(FragmentManager fm) {
            super(fm);
            registerListener(artists, albums, playlists);
        }

        @Override
        public Fragment getItem(int position) {
            if (position == 0)
                return artists;
            if (position == 1)
                return albums;
            return playlists;
        }

        @Override
        public int getCount() {
            return 3;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return getString(position == 0 ? R.string.control_menu_artists : R.string.control_menu_albums);
        }

    }

    public interface ConnectionListener {
        public void onServiceConnected();

        public void onServiceDisconnected();
    }

    @Override
    public void onPageScrollStateChanged(int arg0) {
    }

    @Override
    public void onPageScrolled(int arg0, float arg1, int arg2) {
    }

    @TargetApi(11)
    @Override
    public void onPageSelected(int arg0) {
        positionViewed = arg0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            /* wwj
            handler.fudge = true;
            getActionBar().setSelectedNavigationItem(arg0);
            */

        } else {

            if (arg0 == 0) {
                findViewById(R.id.tab_album).setSelected(false);
                findViewById(R.id.tab_artists).setSelected(true);
                findViewById(R.id.tab_playlists).setSelected(false);
            } else if (arg0 == 1) {
                findViewById(R.id.tab_album).setSelected(true);
                findViewById(R.id.tab_artists).setSelected(false);
                findViewById(R.id.tab_playlists).setSelected(false);
            } else {
                findViewById(R.id.tab_album).setSelected(false);
                findViewById(R.id.tab_artists).setSelected(false);
                findViewById(R.id.tab_playlists).setSelected(true);
            }

        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem arg0) {
        if (arg0.getItemId() == android.R.id.home) {
            finish();
        } else {
            playlists.setListShownNoAnimation(false);
            artists.setListShownNoAnimation(false);
            albums.setListShownNoAnimation(true);
            ThreadExecutor.runTask(new Runnable() {

                @Override
                public void run() {
                    for (ConnectionListener l : listeners)
                        l.onServiceConnected();
                }

            });
        }
        return true;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        // If canceled stay at current level
        if (resultCode == RESULT_CANCELED)
            return;

        // Otherwise pass this back up the chain
        this.setResult(resultCode, intent);
        this.finish();
    }

    @TargetApi(11)
    public class TabHandler implements android.app.ActionBar.TabListener {

        public boolean fudge = false;

        @Override
        public void onTabReselected(android.app.ActionBar.Tab tab, FragmentTransaction ft) {
            // Do nothing
        }

        @Override
        public void onTabSelected(android.app.ActionBar.Tab tab, FragmentTransaction ft) {
            if (!fudge)
                pager.setCurrentItem(tab.getPosition(), true);
            fudge = false;
        }

        @Override
        public void onTabUnselected(android.app.ActionBar.Tab tab, FragmentTransaction ft) {
            // Do nothing
        }

    }
}
