package aca.com.remote.activity;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import aca.com.remote.R;
import aca.com.remote.fragment.LibraryFragment;
import aca.com.remote.tunes.BackendService;
import aca.com.remote.tunes.daap.Session;
import aca.com.remote.tunes.daap.Status;
import aca.com.remote.tunes.jmdns.ServiceInfo;
import aca.com.remote.tunes.util.Constants;
import aca.com.remote.tunes.util.ThreadExecutor;
import aca.com.remote.uitl.CommonUtils;

/**
 * Created by ali_mac on 2017/11/15.
 */

public class SearchLibraryActivity extends BaseActivity implements BackendService.ProbeListener{
    public final static String TAG = SearchLibraryActivity.class.toString();

    public final static int NOTIFY_POBR_START  = 0x10;
    public final static int NOTIFY_POBR_CACHE_LIBRARY  = 0x11;
    public final static int NOTIFY_POBR_ADD_LIBRARY  = 0x12;
    public final static int NOTIFY_CHECK_STATUS  = 0x13;
    public final static int CMD_FORCE_SCAN = 0x20;
    public final static int CHANGE_LIBRARY = 0x01;
    public final static int ORIGIN_LIBRARY = 0x02;

    private BackendService backendService;
    protected static Session session;
    protected static Status status;
    protected LibraryAdapter adapter;
    protected ListView list;
    private Context context;
    private ActionBar ab;
    private Timer mTimer = new Timer();

    public ServiceConnection connection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, final IBinder service) {
            Log.w(TAG, "onServiceConnected");
            backendService = ((BackendService.BackendBinder) service).getService();
            if(null != backendService) {
//                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
//                backendService.setPrefs(settings);

                backendService.registerProbeListener(SearchLibraryActivity.this);
                resultsUpdated.sendEmptyMessage(NOTIFY_POBR_START);
                resultsUpdated.sendEmptyMessage(NOTIFY_CHECK_STATUS);
            }
/*
            ThreadExecutor.runTask(new Runnable() {
                public void run() {
                    backendService = ((BackendService.BackendBinder) service).getService();
                    if(null != backendService) {
                        session = backendService.getSession();
                    }
                }
            });*/
        }

        public void onServiceDisconnected(ComponentName className) {
            // make sure we clean up our handler-specific status
            Log.w(TAG, "onServiceDisconnected");
            status.updateHandler(null);
            backendService = null;
            status = null;
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library_list);

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

        TextView text = (TextView) findViewById(R.id.toolbar_text);
        text.setText("Search Libary");

        context = this;
        if (context != null) {
            Intent intent = new Intent(context, BackendService.class);
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE);
            adapter = new LibraryAdapter(context);

            this.list = (ListView) findViewById(R.id.list);
            this.list.addHeaderView(adapter.footerView, null, false);
            this.list.setAdapter(adapter);

            this.list.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    // read ip/port from caption if present
                    // pass off to backend to try creating pairing session
                    Log.d(TAG,"startActivity : browserintent" );
                    if (backendService == null)
                        return;

                    String caption = ((TextView) view.findViewById(R.id.text2)).getText().toString();
                    String[] split = caption.split("-");
                    if (split.length < 2)
                        return;

                    String address = split[0].trim();

                    // Use the library name
                    String library = ((TextView) view.findViewById(R.id.text1)).getText().toString();

                    // push off fake result to try login
                    // this will start the pairing process if needed
                    Intent shell = new Intent();
                    shell.putExtra(Constants.EXTRA_ADDRESS, address);
                    shell.putExtra(Constants.EXTRA_LIBRARY, library);
                    onActivityResult(-1, Activity.RESULT_OK, shell);
                    ServiceInfo serviceInfo = (ServiceInfo)adapter.known.get((int)id);
                    String musicService = serviceInfo.getPropertyString(Constants.LIBRARYSERVICE);

                    if(null != backendService) {
                        backendService.setCurHost(address);
                        backendService.setCurHostLibrary(library);
                        backendService.setCurHostServices(musicService);
                        backendService.setLastPlayInternal();

                        finish();
                    }
                }
            });

            setTimerTask();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        try {
            checkWifiState();
//            resultsUpdated.sendEmptyMessage(NOTIFY_POBR_START);
//            resultsUpdated.sendEmptyMessage(NOTIFY_CHECK_STATUS);
        } catch (NullPointerException npe) {
            npe.printStackTrace();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (context !=null && connection!= null) {
            mTimer.cancel();
            context.unbindService(connection);
            backendService.unregisterProbeListener(this);
        }
    }

    private void clearList() {
        int size = adapter.known.size();

        if (size > 0) {
            adapter.known.removeAll(adapter.known);
            adapter.notifyDataSetChanged();
        }
    }

    void setTimerTask() {
        mTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                resultsUpdated.sendEmptyMessage(CMD_FORCE_SCAN);
            }
        }, 15000, 15000);
    }

    /**
     * Gets the current wifi state, and changes the text shown in the header as
     * required.
     */
    public void checkWifiState() {

        WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        int intaddr = wifi.getConnectionInfo().getIpAddress();

        View header = adapter.footerView;
        if (!header.equals(adapter.footerView))
            Log.e(TAG, "Header is wrong");
        else {
            TextView title = (TextView) header.findViewById(android.R.id.text1);
            TextView explanation = (TextView) header.findViewById(android.R.id.text2);
            ProgressBar progress = (ProgressBar) header.findViewById(R.id.progress);

            if (wifi.getWifiState() == WifiManager.WIFI_STATE_DISABLED) {

                // Wifi is disabled
                title.setText(R.string.wifi_disabled_title);
                explanation.setText(R.string.wifi_disabled);
//                ClickSpan.clickify(explanation, "wifi in Settings", this);
                progress.setVisibility(View.GONE);

            } else if (intaddr == 0) {

                // Wifi is enabled, but no network connection
                title.setText(R.string.no_network_title);
                explanation.setText(R.string.no_network);
//                ClickSpan.clickify(explanation, "wifi settings", this);
                progress.setVisibility(View.VISIBLE);
            } else {

                // Wifi is enabled and there's a network
                title.setText(R.string.item_network_title);
                explanation.setText(R.string.item_network_caption);
                progress.setVisibility(View.VISIBLE);

            }

        }

    }

    public Handler resultsUpdated = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what){
                case NOTIFY_POBR_START:
                    adapter.known.clear();
                    adapter.notifyDataSetChanged();
                    break;
                case NOTIFY_POBR_CACHE_LIBRARY:
                    HashMap<String, ServiceInfo> map = (HashMap<String, ServiceInfo>)msg.obj;
                    Iterator iter = map.entrySet().iterator();
                    while (iter.hasNext()) {
                        Map.Entry entry = (Map.Entry) iter.next();
                        ServiceInfo val = (ServiceInfo) entry.getValue();
                        adapter.notifyFound(val);
                    }
                    adapter.notifyDataSetChanged();
                    break;
                case NOTIFY_POBR_ADD_LIBRARY:
                    ServiceInfo serviceInfo = (ServiceInfo)msg.obj;
                    boolean result = adapter.notifyFound(serviceInfo);
                    if(result) {
                        resultsUpdated.sendEmptyMessage(-1);
                    }
                    break;
                case NOTIFY_CHECK_STATUS:
                    if (backendService != null) {
//                        clearList();
                        if (backendService.checkSearchInternal() == true) {
                            backendService.startProbe(false);
                            setResult(ORIGIN_LIBRARY);
                        } else {
                            backendService.setLastSearchInternal();
                            backendService.startProbe(true);
                            setResult(CHANGE_LIBRARY);
                        }
                    }
                    break;
                case CMD_FORCE_SCAN:
                    adapter.known.clear();
                    backendService.setLastSearchInternal();
                    backendService.startProbe(true);
                    break;
                default:
                    adapter.notifyDataSetChanged();
                    break;
            }

        }
    };

    public class LibraryAdapter extends BaseAdapter {

        protected Context context;
        protected LayoutInflater inflater;
        public View footerView;
        protected final LinkedList<ServiceInfo> known = new LinkedList<ServiceInfo>();

        public LibraryAdapter(Context context) {
            this.context = context;
            this.inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            this.footerView = inflater.inflate(R.layout.item_network, null, false);
        }

        public boolean notifyFound(ServiceInfo serviceInfo) {
            boolean result = false;
            try {
                String libraryName = serviceInfo.getPropertyString("CtlN");
                if (libraryName == null) {
                    libraryName = serviceInfo.getName();
                }
                //Log.i(TAG,"-: "+serviceInfo);
                String online = serviceInfo.getPropertyString("Service");
                if(null != online)
                    Log.i(TAG,"------:"+online);

                // check if we already have this DatabaseId
                for (ServiceInfo service : known) {
                    String knownName = service.getPropertyString("CtlN");
                    if (knownName == null) {
                        knownName = service.getName();
                    }
                    if (libraryName.equalsIgnoreCase(knownName)) {
                        Log.w(TAG, "Already have DatabaseId loaded = " + libraryName);
                        return result;
                    }
                }

                if (!known.contains(serviceInfo)) {
                    known.add(serviceInfo);
                    result = true;
                }
            } catch (Exception e) {
                Log.d(TAG, String.format("Problem getting ZeroConf information %s", e.getMessage()));
            }
            Log.d(TAG,"notifyFound:" + result);
            return result;
        }

        public Object getItem(int position) {
            return known.get(position);
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }

        public int getCount() {
            return known.size();
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {

            if (convertView == null)
                convertView = inflater.inflate(R.layout.library_info, parent, false);

            try {
                // fetch the dns txt record to get library info
                final ServiceInfo serviceInfo = (ServiceInfo) this.getItem(position);

                String title = serviceInfo.getPropertyString("CtlN");
                if (title == null) {
                    title = serviceInfo.getName();
                }
                final String addr = serviceInfo.getHostAddresses()[0]; // grab first
                // one
                final String library = String.format("%s - %s", addr, serviceInfo.getPropertyString("DbId"));

                Log.d(TAG, String.format("ZeroConf Server: %s", serviceInfo.getServer()));
                Log.d(TAG, String.format("ZeroConf Port: %s", serviceInfo.getPort()));
                Log.d(TAG, String.format("ZeroConf Title: %s", title));
                Log.d(TAG, String.format("ZeroConf Library: %s", library));

                if (backendService != null) {
                    if (backendService.getCurHostLibrary() != null) {
                        ((ImageView) convertView.findViewById(R.id.select_lib)).setImageResource(backendService.getCurHostLibrary().equals(title)?
                                R.drawable.dailly_prs
                                : R.drawable.dailly_normal);
                    } else {
                        ((ImageView) convertView.findViewById(R.id.select_lib)).setImageResource(R.drawable.dailly_normal);
                    }
                }
                ((TextView) convertView.findViewById(R.id.text1)).setText(title);
                ((TextView) convertView.findViewById(R.id.text2)).setText(library);
            } catch (Exception e) {
                Log.d(TAG, String.format("Problem getting ZeroConf information %s", e.getMessage()));
                ((TextView) convertView.findViewById(R.id.text1)).setText("Unknown");
                ((TextView) convertView.findViewById(R.id.text2)).setText("Unknown");
            }

            return convertView;
        }
    }

    @Override
    public boolean onServiceinfoCache(HashMap<String, ServiceInfo> map) {
        boolean result = false;
        resultsUpdated.sendMessage(resultsUpdated.obtainMessage(NOTIFY_POBR_CACHE_LIBRARY,map));

        return result;
    }

    @Override
    public boolean onServiceinfoFound(ServiceInfo serviceInfo) {
        Log.d(TAG,serviceInfo.toString());
        resultsUpdated.sendMessage(resultsUpdated.obtainMessage(NOTIFY_POBR_ADD_LIBRARY,serviceInfo));
        return false;
    }

    @Override
    public void OnCheckWifiState() {
        this.checkWifiState();
    }
}
