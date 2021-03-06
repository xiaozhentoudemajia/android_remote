package aca.com.remote.activity;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import aca.com.nanohttpd.HttpServerImpl;
import aca.com.nanohttpd.HttpService;
import aca.com.remote.MediaAidlInterface;
import aca.com.remote.R;
import aca.com.remote.fragment.QuickControlsFragment;
import aca.com.remote.service.MediaService;
import aca.com.remote.service.MusicPlayer;
import aca.com.remote.tunes.daap.Session;
import aca.com.remote.uitl.IConstants;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

import static aca.com.remote.service.MusicPlayer.mService;

/**
 * Created by wm on 2016/2/25.
 * activity基类
 */
public class BaseActivity extends AppCompatActivity implements ServiceConnection {

    private MusicPlayer.ServiceToken mToken;
    private PlaybackStatus mPlaybackStatus; //receiver 接受播放状态变化等
    private QuickControlsFragment fragment; //底部播放控制栏
    private String TAG = "BaseActivity";
    private ArrayList<MusicStateListener> mMusicListener = new ArrayList<>();
    private HttpService.httpBinder binder = null;
    private static Session mSession;
    private static String mMusicService;
    private static String mCurHost;
    private static String mCurHostLibary;

    public ServiceConnection connectionHttpServer = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            binder = (HttpService.httpBinder)iBinder;
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            binder = null;
        }
    };

    /**
     * 更新播放队列
     */
    public void updateQueue() {

    }

    /**
     * 更新歌曲状态信息
     */
    public void updateTrackInfo() {
        for (final MusicStateListener listener : mMusicListener) {
            if (listener != null) {
                listener.reloadAdapter();
                listener.updateTrackInfo();
            }
        }
    }

    /**
     *  fragment界面刷新
     */
    public void refreshUI() {
        for (final MusicStateListener listener : mMusicListener) {
            if (listener != null) {
                listener.reloadAdapter();
            }
        }

    }

    public void updateTime() {
        for (final MusicStateListener listener : mMusicListener) {
            if (listener != null) {
                listener.updateTime();
            }
        }
    }

    /**
     *  歌曲切换
     */
    public void updateTrack() {

    }



    public void updateLrc() {

    }

    /**
     * @param p 更新歌曲缓冲进度值，p取值从0~100
     */
    public void updateBuffer(int p) {

    }

    public void changeTheme() {
        for (final MusicStateListener listener : mMusicListener) {
            if (listener != null) {
                listener.changeTheme();
            }
        }
    }

    /**
     * @param l 歌曲是否加载中
     */
    public void loading(boolean l){

    }


    /**
     * @param outState 取消保存状态
     */
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        //super.onSaveInstanceState(outState);
    }

    /**
     * @param savedInstanceState 取消保存状态
     */
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        //super.onRestoreInstanceState(savedInstanceState);
    }


    /**
     * @param show 显示或关闭底部播放控制栏
     */
    protected void showQuickControl(boolean show) {
        Log.d(TAG, MusicPlayer.getQueue().length + "");
        FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
        if (show) {
            if (fragment == null) {
                fragment = QuickControlsFragment.newInstance();
                ft.add(R.id.bottom_container, fragment).commitAllowingStateLoss();
            } else {
                ft.show(fragment).commitAllowingStateLoss();
            }
        } else {
            if (fragment != null)
                ft.hide(fragment).commitAllowingStateLoss();
        }
    }

    public void updateTrackInfo(Session session) {
        if (fragment != null) {
            fragment.updateTrackInfo(session);
            fragment.updateHttpBinder(binder);
        }
    }

    public void httpPlay(String url) {
        if (binder != null)
            binder.setTransPath(url);
    }

    public int getHttpServerPort() {
        if (binder != null)
            return binder.getHttpServerPort();
        else
            return HttpServerImpl.DEFAULT_SERVER_PORT;
    }

    public void setUpgPath(String path) {
        if (binder != null)
            binder.setUpgPath(path);
    }

    public void setCurHost(String host) {
        mCurHost = host;
    }

    public String getCurHost() {
        return mCurHost;
    }

    public void setCurHostLibrary(String library) {
        mCurHostLibary = library;
    }

    public String getCurHostLibrary() {
        return mCurHostLibary;
    }

    public void setSession(Session session) {
        mSession = session;
    }

    public Session getSession() {
        return mSession;
    }

    public void setMusicService(String Service) {
        mMusicService = Service;
    }

    public String getMusicService() {
        return mMusicService;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mToken = MusicPlayer.bindToService(this, this);
        mPlaybackStatus = new PlaybackStatus(this);

        IntentFilter f = new IntentFilter();
        f.addAction(MediaService.PLAYSTATE_CHANGED);
        f.addAction(MediaService.META_CHANGED);
        f.addAction(MediaService.QUEUE_CHANGED);
        f.addAction(IConstants.MUSIC_COUNT_CHANGED);
        f.addAction(MediaService.TRACK_PREPARED);
        f.addAction(MediaService.BUFFER_UP);
        f.addAction(IConstants.EMPTY_LIST);
        f.addAction(MediaService.MUSIC_CHANGED);
        f.addAction(MediaService.LRC_UPDATED);
        f.addAction(IConstants.PLAYLIST_COUNT_CHANGED);
        f.addAction(MediaService.MUSIC_LODING);
        registerReceiver(mPlaybackStatus, new IntentFilter(f));
        showQuickControl(true);
        bindService(new Intent(this, HttpService.class), connectionHttpServer, Context.BIND_AUTO_CREATE);
    }


    @Override
    public void onServiceConnected(final ComponentName name, final IBinder service) {
        mService = MediaAidlInterface.Stub.asInterface(service);
    }

    @Override
    public void onServiceDisconnected(final ComponentName name) {
        mService = null;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unbind from the service
        unbindService();
        try {
            unregisterReceiver(mPlaybackStatus);
        } catch (final Throwable e) {
        }
        mMusicListener.clear();
        unbindService(connectionHttpServer);
    }

    public void unbindService() {
        if (mToken != null) {
            MusicPlayer.unbindFromService(mToken);
            mToken = null;
        }
    }

    public void setMusicStateListenerListener(final MusicStateListener status) {
        if (status == this) {
            throw new UnsupportedOperationException("Override the method, don't add a listener");
        }

        if (status != null) {
            mMusicListener.add(status);
        }
    }

    public void removeMusicStateListenerListener(final MusicStateListener status) {
        if (status != null) {
            mMusicListener.remove(status);
        }
    }


    private final static class PlaybackStatus extends BroadcastReceiver {

        private final WeakReference<BaseActivity> mReference;


        public PlaybackStatus(final BaseActivity activity) {
            mReference = new WeakReference<>(activity);
        }


        @Override
        public void onReceive(final Context context, final Intent intent) {
            final String action = intent.getAction();
            BaseActivity baseActivity = mReference.get();
            if (baseActivity != null) {
                if (action.equals(MediaService.META_CHANGED)) {
                    baseActivity.updateTrackInfo();

                } else if (action.equals(MediaService.PLAYSTATE_CHANGED)) {

                } else if (action.equals(MediaService.TRACK_PREPARED)) {
                    baseActivity.updateTime();
                } else if (action.equals(MediaService.BUFFER_UP)) {
                    baseActivity.updateBuffer(intent.getIntExtra("progress", 0));
                } else if (action.equals(MediaService.MUSIC_LODING)) {
                    baseActivity.loading(intent.getBooleanExtra("isloading",false));
                } else if (action.equals(MediaService.REFRESH)) {

                } else if (action.equals(IConstants.MUSIC_COUNT_CHANGED)) {
                    baseActivity.refreshUI();
                } else if (action.equals(IConstants.PLAYLIST_COUNT_CHANGED)) {
                    baseActivity.refreshUI();
                } else if (action.equals(MediaService.QUEUE_CHANGED)) {
                    baseActivity.updateQueue();
                } else if (action.equals(MediaService.TRACK_ERROR)) {
                    final String errorMsg = context.getString(R.string.exit,
                            intent.getStringExtra(MediaService.TrackErrorExtra.TRACK_NAME));
                    Toast.makeText(baseActivity, errorMsg, Toast.LENGTH_SHORT).show();
                } else if (action.equals(MediaService.MUSIC_CHANGED)) {
                    baseActivity.updateTrack();
                } else if (action.equals(MediaService.LRC_UPDATED)) {
                    baseActivity.updateLrc();
                }

            }
        }
    }
}
