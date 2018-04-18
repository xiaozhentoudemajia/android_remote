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

    int getHttpServerPort() {
        return ((BaseActivity) getActivity()).getHttpServerPort();
    }

    void httpPlay(String url) {
        ((BaseActivity) getActivity()).httpPlay(url);
    }

    void setCurHost(String host) {
        ((BaseActivity) getActivity()).setCurHost(host);
    }


    void setCurHostLibrary(String library) {
        ((BaseActivity) getActivity()).setCurHostLibrary(library);
    }

    void setSession(Session session) {
        ((BaseActivity) getActivity()).setSession(session);
    }

    void setMusicService(String service) {
        ((BaseActivity) getActivity()).setMusicService(service);
    }

    void updateTrackInfo(Session session) {
        ((BaseActivity) getActivity()).updateTrackInfo(session);
    }
}
