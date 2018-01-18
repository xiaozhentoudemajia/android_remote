package aca.com.remote.fragment;

import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.content.Intent;
import android.widget.CheckBox;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import aca.com.nanohttpd.HttpServerImpl;
import aca.com.nanohttpd.HttpService;
import aca.com.remote.R;
import aca.com.remote.activity.SearchLibraryActivity;
import aca.com.remote.tunes.BackendService;
import aca.com.remote.tunes.SessionWrapper;
import aca.com.remote.activity.SmartLinkExActivity;
import aca.com.remote.tunes.daap.Session;
import aca.com.remote.tunes.daap.Speaker;
import aca.com.remote.tunes.daap.Status;
import aca.com.remote.tunes.util.ThreadExecutor;

/**
 * Created by ali_mac on 2017/11/15.
 */

public class LibraryFragment extends BaseFragment {
    public static final String TAG = LibraryFragment.class.toString();

    private BackendService backendService;
    protected static Session session;
    protected static Status status;
    protected String curHost;
    protected String curHostLibrary;
    private Button curLibary;
    protected List<Speaker> SPEAKERS = new ArrayList<Speaker>();
    protected ListView list;
    protected SpeakersAdapter adapter;
    private View headerView;
    private int requestCode = 0;
    private ProgressBar progress;

    protected long cachedVolume = -1;
    protected long cachedTime = -1;
    public static final int CHECK_STATUS = 0x10;
    public static final int NOTIFY_SPEAKERS = 0x80;
    public static final int CMD_FORCE_SCAN = 0x20;
    public final static long CACHE_TIME = 10000;
    public final static int tryCnt = 10;
    private Timer mTimer = new Timer();
    private Speaker mHostSpeaker;

    public ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, final IBinder service) {
            backendService = ((BackendService.BackendBinder) service).getService();
            if (backendService != null) {
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mContext);
                backendService.setPrefs(settings);
                statusUpdate.sendEmptyMessage(CHECK_STATUS);
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            backendService = null;
            session = null;
        }
    };

    void setTimerTask() {
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                statusUpdate.sendEmptyMessage(CHECK_STATUS);
            }
        }, 15000, 15000);
    }

    protected Handler statusUpdate = new Handler() {

        @Override
        public void handleMessage(Message msg) {
            // update gui based on severity
            switch (msg.what) {
                case CHECK_STATUS:
                    if (backendService != null) {
                        Log.i(TAG, "current host: " + backendService.getCurHost());
                        Log.i(TAG, "current library: " + backendService.getCurHostLibrary());
                        Log.i(TAG, "current service: " + backendService.getCurHostServices());
                        Log.i(TAG, "last play internal: " + backendService.getLastPlayInternal());

                        clearList();
                        if (backendService.checkPlayInternal() == true) {
//                            clearList();
                            progress.setVisibility(View.VISIBLE);
                            if (backendService.getCurHost() != null
                                    && backendService.getCurHostLibrary() != null) {
                                curHost = backendService.getCurHost();
                                setCurHost(curHost);
                                curHostLibrary = backendService.getCurHostLibrary();
                                setCurHostLibrary(curHostLibrary);

                                curLibary.setBackgroundColor(R.color.menu_item_blue);
                                curLibary.setText(backendService.getCurHostLibrary());
                                TextView text = (TextView) headerView.findViewById(R.id.speaker_caption);
                                text.setText(R.string.speaker_title);
                                adapter = new SpeakersAdapter(mContext);
                                list.setAdapter(adapter);

                                sessionTask();
                            }
                        } else {
//                            clearList();
                        }
                    }
                    break;
                case NOTIFY_SPEAKERS:
                    adapter.notifyDataSetChanged();
                    mContext.setProgressBarIndeterminateVisibility(false);
                    break;
                case Status.UPDATE_SPEAKERS:
                    ThreadExecutor.runTask(new Runnable() {
                        public void run() {
                            try {
                                if (status == null) {
                                    return;
                                }
                                status.getSpeakers(SPEAKERS);
                                if(SPEAKERS.size() > 0)
                                    statusUpdate.sendEmptyMessage(NOTIFY_SPEAKERS);
                            } catch (Exception e) {
                                Log.e(TAG, "Speaker Exception:" + e.getMessage());
                            }
                        }

                    });
                    break;
                case CMD_FORCE_SCAN:
                    break;
            }

            //checkShuffle();
            //checkRepeat();
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
        View view = inflater.inflate(R.layout.fragment_library, container, false);

        Button search = (Button) view.findViewById(R.id.search_library);
        search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mContext, SearchLibraryActivity.class);
                startActivityForResult(intent, requestCode);
            }
        });

        Button wps = (Button) view.findViewById(R.id.wps);
        wps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(mContext, SmartLinkExActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                startActivity(intent);
            }
        });

        curLibary = (Button) view.findViewById(R.id.cur_library);

        this.adapter = new SpeakersAdapter(mContext);
        this.list = (ListView) view.findViewById(android.R.id.list);

        //this.list.addHeaderView(adapter.footerView, null, false);
        this.list.setAdapter(adapter);

        headerView = inflater.inflate(R.layout.speaker_caption, null, false);
        this.list.addHeaderView(headerView);
        progress = (ProgressBar) headerView.findViewById(R.id.progress);
        progress.setVisibility(View.GONE);

//        setTimerTask();

        return view;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.i(TAG, "onActivityResult:"+resultCode);
        switch(requestCode) {
            case SearchLibraryActivity.CHANGE_LIBRARY:
                break;
            case SearchLibraryActivity.ORIGIN_LIBRARY:
                break;
            default:
                break;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        statusUpdate.sendEmptyMessage(CHECK_STATUS);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mContext.unbindService(connection);
    }

    private void clearList() {
        int size = SPEAKERS.size();

        if (size > 0) {
            SPEAKERS.removeAll(SPEAKERS);
            statusUpdate.sendEmptyMessage(NOTIFY_SPEAKERS);
        }
    }

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
                                Log.d(TAG,sessionWrapper.toString());
                            }else{
                                Log.w(TAG,"waiting session to been created");
                            }

                            timeout++;
                            if(timeout > tryCnt){
                                if(null == session) {
                                    session = backendService.getSession(curHost, curHostLibrary);
                                    Log.w(TAG, "force create session ");
                                }
                                break;
                            }
                        }while ((null == session)&&(null != backendService));

                        setSession(session);
                        updateTrackInfo(session);
                        setMusicService(backendService.getCurHostServices());
                        status = session.singletonStatus(statusUpdate);
                        status.updateHandler(statusUpdate);

                        // push update through to make sure we get updated
                        statusUpdate.sendEmptyMessage(Status.UPDATE_SPEAKERS);
                    }
                }catch (Exception e){
                    e.printStackTrace();
                    Log.e(TAG, "get session error:"+e.getMessage());
                }

            }
        });
    }

    int choice = -1;
    private void showRenameDialog() {
        final String[] items = { "diningroom","kitchen","sittingroom","bedroom","familyroom","livingroom","study" };
        final EditText editText = new EditText(mContext);
        AlertDialog.Builder inputDialog =
                new AlertDialog.Builder(mContext);
        inputDialog.setTitle("Rename speaker").setView(editText);
        inputDialog.setSingleChoiceItems(items, 0,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        choice = which;
                    }
                });
        inputDialog.setNegativeButton("Cancel",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        inputDialog.setPositiveButton("Enter",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (choice != -1) {
                            Toast.makeText(mContext,
                                    "rename to " + items[choice],
                                    Toast.LENGTH_SHORT).show();
                            session.rename(items[choice]);
                            try {
                                Thread.sleep(1000);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                            statusUpdate.sendEmptyMessage(Status.UPDATE_SPEAKERS);
                        }
                    }
                }).show();
    }

    public class SpeakersAdapter extends BaseAdapter {

        protected Context context;
        protected LayoutInflater inflater;
        //public View footerView;

        public SpeakersAdapter(Context context) {
            this.context = context;
            inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            //this.footerView = inflater.inflate(R.layout.item_network, null, false);
        }

        public int getCount() {
            if (SPEAKERS == null) {
                return 0;
            }
            return SPEAKERS.size();
        }

        public Object getItem(int position) {
            return SPEAKERS.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        /**
         * Toggles activation of a given speaker and refreshes the view
         * @param active Flag indicating, whether the speaker shall be activated
         * @param speaker the speaker to be activated or deactivated
         */
        public void setSpeakerActive(boolean active, final Speaker speaker) {
            if (speaker == null) {
                return;
            }
            if (status == null) {
                return;
            }
            speaker.setActive(active);

            ThreadExecutor.runTask(new Runnable() {
                public void run() {
                    try {
                        status.setSpeakers(SPEAKERS);
                    } catch (Exception e) {
                        Log.e(TAG, "Speaker Exception:" + e.getMessage());
                    }
                }

            });

            notifyDataSetChanged();
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            try {

                View row;
                if (null == convertView) {
                    row = inflater.inflate(R.layout.item_speaker, null);
                } else {
                    row = convertView;
                }

                /*************************************************************
                 * Find the necessary sub views
                 *************************************************************/
                TextView nameTextview = (TextView) row.findViewById(R.id.speakerNameTextView);
                TextView speakerTypeTextView = (TextView) row.findViewById(R.id.speakerTypeTextView);
                final CheckBox activeCheckBox = (CheckBox) row.findViewById(R.id.speakerActiveCheckBox);
                SeekBar volumeBar = (SeekBar) row.findViewById(R.id.speakerVolumeBar);
                Button setChannel = (Button) row.findViewById(R.id.setChannel);

                /*************************************************************
                 * Set view properties
                 *************************************************************/
                final Speaker speaker = SPEAKERS.get(position);
                nameTextview.setText(speaker.getName());
                speakerTypeTextView.setText(speaker.isLocalSpeaker() ? R.string.speakers_dialog_computer_speaker
                        : R.string.speakers_dialog_airport_express);
                activeCheckBox.setChecked(speaker.isActive());
                activeCheckBox.setOnClickListener(new View.OnClickListener() {

                    public void onClick(View v) {
                        if (mHostSpeaker != speaker) {
                            setSpeakerActive(activeCheckBox.isChecked(), speaker);
                        }
                    }
                });
                nameTextview.setOnClickListener(new View.OnClickListener() {

                    public void onClick(View v) {
                        if (mHostSpeaker != speaker) {
                            activeCheckBox.toggle();
                            setSpeakerActive(activeCheckBox.isChecked(), speaker);
                        }
                    }
                });
                speakerTypeTextView.setOnClickListener(new View.OnClickListener() {

                    public void onClick(View v) {
                        if (mHostSpeaker != speaker) {
                            activeCheckBox.toggle();
                            setSpeakerActive(activeCheckBox.isChecked(), speaker);
                        }
                    }
                });
                if (speaker.getName().contains(curHostLibrary)) {
                    mHostSpeaker = speaker;
                    activeCheckBox.setClickable(false);
                }
                // If the speaker is active, enable the volume bar
                if (speaker.isActive()) {
                    volumeBar.setEnabled(true);
                    volumeBar.setProgress(speaker.getAbsoluteVolume());
                    volumeBar.setOnSeekBarChangeListener(new VolumeSeekBarListener(speaker));
                } else {
                    volumeBar.setEnabled(true);
                    volumeBar.setProgress(0);
                }

                setChannel.setOnClickListener(new SetChannelListener(speaker));
                return row;
            } catch (RuntimeException e) {
                Log.e(TAG, "Error when rendering speaker item: ", e);
                throw e;
            }
        }
    }

    public class SetChannelListener implements View.OnClickListener {
        private final Speaker speaker;
        private int index=0;

        public SetChannelListener(Speaker speaker) {
            this.speaker = speaker;
        }

        @Override
        public void onClick(View view) {
            if (index < 2) {
                index++;
            } else {
                index = 0;
            }
            switch (index) {
                case 0:
                    ((Button)view).setText("L/R");
                    break;
                case 1:
                    ((Button)view).setText("L");
                    break;
                case 2:
                    ((Button)view).setText("R");
                    break;
            }
            ThreadExecutor.runTask(
                    new Runnable() {
                        @Override
                        public void run() {
                            status.setSpeakerChannel(speaker.getId(), index);
                        }
                    }
            );

        }
    }

    public class VolumeSeekBarListener implements SeekBar.OnSeekBarChangeListener {
        private final Speaker speaker;

        public VolumeSeekBarListener(Speaker speaker) {
            this.speaker = speaker;
        }

        public void onStopTrackingTouch(SeekBar seekBar) {
            final int newVolume = seekBar.getProgress();
            ThreadExecutor.runTask(new Runnable() {
                public void run() {
                    try {
                        // Volume of the loudest speaker
                        int maxVolume = 0;
                        // Volume of the second loudest speaker
                        int secondMaxVolume = 0;
                        for (Speaker speaker : SPEAKERS) {
                            if (speaker.getAbsoluteVolume() > maxVolume) {
                                secondMaxVolume = maxVolume;
                                maxVolume = speaker.getAbsoluteVolume();
                            } else if (speaker.getAbsoluteVolume() > secondMaxVolume) {
                                secondMaxVolume = speaker.getAbsoluteVolume();
                            }
                        }
                        // fetch the master volume if necessary
                        checkCachedVolume();
                        int formerVolume = speaker.getAbsoluteVolume();
                        status.setSpeakerVolume(speaker.getId(), newVolume, formerVolume, maxVolume, secondMaxVolume,
                                cachedVolume);
                        speaker.setAbsoluteVolume(newVolume);
                    } catch (Exception e) {
                        Log.e(TAG, "Speaker Exception:" + e.getMessage());
                    }
                }

            });
        }

        public void onStartTrackingTouch(SeekBar seekBar) {
        }

        public void onProgressChanged(SeekBar seekBar, int newVolume, boolean fromUser) {
        }
    }

    /**
     * Updates the cachedVolume if necessary
     */
    protected void checkCachedVolume() {
        // try assuming a cached volume instead of requesting it each time
        if (System.currentTimeMillis() - cachedTime > CACHE_TIME) {
            if (status == null) {
                return;
            }
            this.cachedVolume = status.getVolume();
            this.cachedTime = System.currentTimeMillis();
        }
    }
}
