package aca.com.remote.fragment;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import aca.com.remote.R;
import aca.com.remote.tunes.daap.Session;
import aca.com.remote.tunes.util.Constants;
import aca.com.remote.tunes.util.ThreadExecutor;

/**
 * Created by ali_mac on 2017/11/17.
 */

public class ThirdPartyFragment extends AttachFragment {
    public final static String TAG = ThirdPartyFragment.class.toString();

    protected static Session session;
    private String service;
    private Button btnSpotify;
    private Button btnDlna;
    private TextView title;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_thirdparty, container, false);
        btnSpotify = (Button) view.findViewById(R.id.spotify);
        btnDlna = (Button) view.findViewById(R.id.dlna);
        title = (TextView) view.findViewById(R.id.title);

        btnSpotify.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (session != null) {
                    playService(Constants.SPOTIFYSERVICE);
                }
            }
        });

        btnDlna.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (session != null) {
                    playService(Constants.DLNASERVICE);
                }
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        refreshView();
    }

    @Override
    public void onHiddenChanged(boolean hidden) {
        super.onHiddenChanged(hidden);

        Log.d(TAG, "onHiddenChanged: "+hidden);
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser == true) {
            refreshView();
        }
    }

    private void refreshView() {
        int spotifyFlag = View.INVISIBLE;
        int dlnaFlag = View.INVISIBLE;
        String text = mContext.getString(R.string.select_library);

        service = getMusicService();
        if (service != null) {
            String[] split = service.split(",");
            for (int i = 0; i < split.length; i++) {
                String str = new String(split[i]);
                Log.i(TAG, "support service: " + new String(split[i]));
                if (str.equals(Constants.SPOTIFYSERVICE)) {
                    spotifyFlag = View.VISIBLE;
                } else if (str.equals(Constants.DLNASERVICE)) {
                    dlnaFlag = View.VISIBLE;
                }
            }
            text = mContext.getString(R.string.thirdparty);
        }

        btnSpotify.setVisibility(spotifyFlag);
        btnDlna.setVisibility(dlnaFlag);
        title.setText(text);
        session = getSession();
    }

    protected  void playService(final String service){
        if(service.equals(Constants.SPOTIFYSERVICE)){
            ThreadExecutor.runTask(new Runnable() {

                public void run() {
                    try {
                        session.onlinePlay(service);
                        startSpotify();
                    }catch (Exception e){
                        e.printStackTrace();
                        Toast.makeText(mContext, "Reauest failed!", Toast.LENGTH_LONG).show();
                    }
                }
            });
        } else if(service.equals(Constants.DLNASERVICE)){
            ThreadExecutor.runTask(new Runnable() {
                public void run() {
                    try {
                        session.onlinePlay(service);
                        startBubbleupnp();
                    }catch (Exception e){
                        e.printStackTrace();
                        Toast.makeText(mContext, "Reauest failed!", Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }

    private void startSpotify(){
        PackageManager packageManager = mContext.getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage("com.spotify.music");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK| Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_CLEAR_TOP) ;
        this.startActivity(intent);
    }

    public void startBubbleupnp(){
        PackageManager packageManager = mContext.getPackageManager();
        Intent intent = packageManager.getLaunchIntentForPackage("com.bubblesoft.android.bubbleupnp");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK| Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED | Intent.FLAG_ACTIVITY_CLEAR_TOP) ;
        this.startActivity(intent);
    }
}
