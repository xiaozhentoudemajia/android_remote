package aca.com.remote.fragment;

import android.app.Activity;
import android.support.v4.app.Fragment;

import aca.com.remote.activity.MainActivity;
import aca.com.remote.tunes.daap.Session;

/**
 * Created by wm on 2016/3/17.
 */
public class AttachFragment extends Fragment {

    public Activity mContext;

    @Override
    public void onAttach(Activity activity){
        super.onAttach(activity);
        this.mContext = activity;
    }

    public String getCurHost() {
        return ((MainActivity)mContext).getCurHost();
    }

    public String getCurHostLibary() {
        return ((MainActivity)mContext).getCurHostLibrary();
    }

    public Session getSession() {
        return ((MainActivity)mContext).getSession();
    }

    public String getMusicService() {
        return ((MainActivity)mContext).getMusicService();
    }
}
