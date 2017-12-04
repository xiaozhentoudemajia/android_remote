package aca.com.remote.fragment;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import aca.com.remote.R;
import aca.com.remote.activity.LibraryMusicActitvity;
import aca.com.remote.activity.MainActivity;
import aca.com.remote.tunes.BackendService;
import aca.com.remote.tunes.SessionWrapper;
import aca.com.remote.tunes.TagListener;
import aca.com.remote.tunes.daap.ActionErrorListener;
import aca.com.remote.tunes.daap.Library;
import aca.com.remote.tunes.daap.Response;
import aca.com.remote.tunes.daap.Session;
import aca.com.remote.tunes.daap.Status;
import aca.com.remote.tunes.util.Constants;
import aca.com.remote.tunes.util.ThreadExecutor;

/**
 * Created by ali_mac on 2017/11/17.
 */

public class LibraryMusicFragment extends AttachFragment {
    public final static String TAG = "wwj";//LibraryMusicFragment.class.toString();

    public Session session;
    public Library library;
    protected static Status status;
    protected String curHost;
    protected String curHostLibrary;
    private BackendService backendService;
    public final static int tryCnt = 10;
    Test test;
    private String service;
    private Button btnLibaryMusic;
    private TextView title;

    protected ActionErrorListener mLibraryErrListener = new ActionErrorListener() {
        @Override
        public void onActionError(int code) {
            Log.i(TAG, "http request code:"+code);
        }
    };

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = new Intent(mContext, BackendService.class);
        mContext.bindService(intent, connection, Context.BIND_AUTO_CREATE);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_library_music, container, false);
        title = (TextView) view.findViewById(R.id.title);
        btnLibaryMusic = (Button) view.findViewById(R.id.library_music);
        btnLibaryMusic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mContext, LibraryMusicActitvity.class);
                intent.putExtra(Constants.EXTRA_ADDRESS, getCurHost());
                intent.putExtra(Constants.EXTRA_LIBRARY, getCurHostLibary());
                mContext.startActivity(intent);
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
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
/*
        if (isVisibleToUser == true) {
            Log.i(TAG, "set Library");

            curHost = backendService.getCurHost();
            curHostLibrary = backendService.getCurHostLibrary();
            sessionTask();
        }
*/
        if (isVisibleToUser == true) {
            refreshView();
        }
    }

    private void refreshView() {
        int libraryFlag = View.INVISIBLE;
        String text = mContext.getString(R.string.select_library);

        service = getMusicService();
        if (service != null) {
            String[] split = service.split(",");
            for (int i = 0; i < split.length; i++) {
                String str = new String(split[i]);
                Log.i(TAG, "support service: " + new String(split[i]));
                if (str.equals(Constants.MYMUSICSERVICE)) {
                    Log.i(TAG, "set spotify");
                    libraryFlag = View.VISIBLE;
                }
            }
            text = mContext.getString(R.string.library_music);
        }

        btnLibaryMusic.setVisibility(libraryFlag);
        title.setText(text);
    }

    public class Test implements TagListener {
        public void foundTag(String tag, final Response resp) {
            if (resp == null) {
                Log.i("wwj", "resp is null");
                return;
            }
            Log.i("wwj", "start search tag");
            getActivity().runOnUiThread(new Runnable() {

                public void run() {
                    try {
                        Log.i("wwj", "foundTag:" + resp);
                        // add a found search result to our list
                        if (resp.containsKey("mlit")) {
                            String mlit = resp.getString("mlit");
                            if (mlit.length() > 0 && !mlit.startsWith("mshc")) {
                                Log.i("wwj", "foundTag:" + resp);
                            }
                        }
                    } catch (Exception e) {
                        Log.i("wwj", "foundTag:" + e.getMessage());
                    }
                }

            });
        }

        public void searchDone() {
            try {
                Log.i("wwj", "foundTag searchDone");
            } catch (Exception e) {
                Log.i("wwj", "searchDone:" + e.getMessage());
            }
        }
    }

    public ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, final IBinder service) {
            backendService = ((BackendService.BackendBinder) service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            backendService = null;
            session = null;
        }
    };

    void sessionTask() {
        ThreadExecutor.runTask(new Runnable() {
            public void run() {
                int timeout=0;
                try {
                    if (null != backendService) {

                        session = null;
                        do {
                            Thread.sleep(300);
                            SessionWrapper sessionWrapper =  backendService.getSession(curHost);
                            if(null != sessionWrapper){
                                if(!sessionWrapper.isTimeout()) {
                                    session = sessionWrapper.getSession(curHost);
                                }else{
                                    timeout = tryCnt;
                                }
                                Log.d("wwj",sessionWrapper.toString());
                            }else{
                                Log.w("wwj","waiting session to been created");
                            }

                            timeout++;
                            if(timeout > tryCnt){
                                if(null == session) {
                                    session = backendService.getSession(curHost, curHostLibrary);
                                    Log.i("wwj", "force create session ");
                                }
                                break;
                            }
                        }while ((null == session)&&(null != backendService));

                        ((MainActivity)mContext).setSession(session);
                        ((MainActivity)mContext).setMusicService(backendService.getCurHostServices());
                        //status = session.singletonStatus(statusUpdate);
                        //status.updateHandler(statusUpdate);

                        // push update through to make sure we get updated
                        library = new Library(session, mLibraryErrListener);
                        Log.i("wwj", "force create session ");
                        //statusUpdate.sendEmptyMessage(Status.GET_TRACK_LIST);
                        test = new Test();
                        library.readArtists(test);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                    Log.e(TAG, "get session error:"+e.getMessage());
                }

            }
        });
    }
}
