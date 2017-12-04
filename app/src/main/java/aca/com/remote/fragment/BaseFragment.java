package aca.com.remote.fragment;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

import aca.com.remote.activity.BaseActivity;
import aca.com.remote.activity.MainActivity;
import aca.com.remote.tunes.daap.Session;
import aca.com.remote.activity.MusicStateListener;

/**
 * Created by wm on 2016/3/17.
 */
public class BaseFragment extends Fragment implements MusicStateListener {

    public Activity mContext;

    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);
        this.mContext = activity;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public void onResume() {
        super.onResume();
        ((BaseActivity) getActivity()).setMusicStateListenerListener(this);
        reloadAdapter();
    }

    @Override
    public void onStop() {
        super.onStop();
        ((BaseActivity) getActivity()).removeMusicStateListenerListener(this);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    @Override
    public void updateTrackInfo() {

    }

    @Override
    public void updateTime() {

    }

    @Override
    public void changeTheme() {

    }

    @Override
    public void reloadAdapter() {

    }

    void setCurHost(String host) {
        ((MainActivity)mContext).setCurHost(host);
    }


    void setCurHostLibrary(String library) {
        ((MainActivity)mContext).setCurHostLibrary(library);
    }

    void setSession(Session session) {
        ((MainActivity)mContext).setSession(session);
    }

    void setMusicService(String service) {
        ((MainActivity)mContext).setMusicService(service);
    }

    void updateTrackInfo(Session session) {
        ((MainActivity)mContext).updateTrackInfo(session);
    }
}
