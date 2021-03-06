package aca.com.remote.activity;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import aca.com.remote.R;
import aca.com.remote.fragment.RadioListFragment;
import aca.com.remote.tunes.BackendService;
import aca.com.remote.tunes.SessionWrapper;
import aca.com.remote.tunes.daap.ActionErrorListener;
import aca.com.remote.tunes.daap.Library;
import aca.com.remote.tunes.daap.Session;
import aca.com.remote.tunes.util.Constants;
import aca.com.remote.tunes.util.RadioRequestCallback;
import aca.com.remote.tunes.util.ThreadExecutor;
import aca.com.remote.tunes.util.TuneInElement;
import aca.com.remote.tunes.util.TuneInLink;
import aca.com.remote.tunes.util.TuneInRequest;
import aca.com.remote.uitl.CommonUtils;

/**
 * Created by jim.yu on 2017/12/27.
 */

public class TuneInActivity extends BaseActivity {
    private String LogTag = TuneInActivity.class.getName();
    private ActionBar ab;
    private TuneInRequest mTuneIn;
    private boolean need_browser_index = true;

    private List<Fragment> listFragments = new ArrayList<>();

    public String curHost;
    public String curHostLibrary;
    private BackendService backend;
    public Session session;
    private Library library;

    private TuneInElement cur_station;

    public final static int tryCnt = 40;
    protected ActionErrorListener mLibraryErrListener = new ActionErrorListener() {
        @Override
        public void onActionError(int code) {
            Log.i("wwj","shoutcast request code:"+code);
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

    private AdapterView.OnItemClickListener itemClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            Object o = ((RadioListFragment)listFragments.get(listFragments.size() - 1)).getList().get(position);
            RadioListFragment fg;

            if (o instanceof TuneInLink) {
                fg = new RadioListFragment();
                Bundle bundle = new Bundle();
                bundle.putString("title", ((TuneInLink)o).getText());
                fg.setArguments(bundle);
                addFragment(fg, "Fragment_"+listFragments.size());
                ((RadioListFragment)listFragments.get(listFragments.size() - 1)).setOnItemClickListener(itemClickListener);
                mTuneIn.radioTimeGetUrl(((TuneInLink)o).getUrl());
            } else if (o instanceof TuneInElement) {
                if (((TuneInElement)o).getType().equals("link")) {
                    fg = new RadioListFragment();
                    Bundle bundle = new Bundle();
                    bundle.putString("title", ((TuneInElement)o).getText());
                    fg.setArguments(bundle);
                    addFragment(fg, "Fragment_"+listFragments.size());
                    ((RadioListFragment)listFragments.get(listFragments.size() - 1)).setOnItemClickListener(itemClickListener);
                    mTuneIn.radioTimeGetUrl(((TuneInElement) o).getUrl());
                } else {
                    cur_station = (TuneInElement) o;
                    mTuneIn.radioTimeTuneStation(((TuneInElement) o).getGuide_id(), false);
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tunein);

        curHost = getIntent().getStringExtra(Constants.EXTRA_ADDRESS);
        curHostLibrary  = getIntent().getStringExtra(Constants.EXTRA_LIBRARY);

        /** init view **/
        //action back
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

//        ImageView search = (ImageView) findViewById(R.id.bar_search);
//        try {
//            search.setClickable(true);
//            search.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                }
//            });
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

        RadioListFragment fg = new RadioListFragment();
        Bundle bundle = new Bundle();
        bundle.putString("title", "TuneIn");
        fg.setArguments(bundle);
        addFragment(fg, "" + listFragments.size());

        mTuneIn = new TuneInRequest(TuneInRequest.PartnerID, TuneInRequest.serial, null);
        mTuneIn.setTuneInCallback(new RadioRequestCallback() {
            @Override
            public void messageCallback(int type, Object obj) {
                switch (type) {
                    case TuneInRequest.eTUNEIN_MSG_LINK:
						String key = ((TuneInLink)obj).getKey();
                        if (null != key && key.equalsIgnoreCase("Settings"))
                            break;
                        if (!listFragments.isEmpty()) {
                            RadioListFragment fg = (RadioListFragment)listFragments.get(listFragments.size()-1);
                            if (!fg.getXmlLoadStatus())
                                fg.addItem(obj);
                        }
                        break;
                    case TuneInRequest.eTUNEIN_MSG_STATION_AUDIO:
                    case TuneInRequest.eTUNEIN_MSG_STATION_TOPIC:
                    case TuneInRequest.eTUNEIN_MSG_STATION_SHOW:
                        if (!listFragments.isEmpty()) {
                            RadioListFragment fg = (RadioListFragment)listFragments.get(listFragments.size()-1);
                            if (!fg.getXmlLoadStatus())
                                fg.addItem(obj);
                        }
                        break;
                    case TuneInRequest.eTUNEIN_MSG_URL:
                        final String str = obj.toString();
                        if (null == session) {
                            Toast.makeText(TuneInActivity.this, R.string.error_no_speaker_selected, Toast.LENGTH_SHORT).show();
                            break;
                        }
                        session.setRadioTunesUrl(str, "TuneIn", cur_station.getImage());
                        break;
                    case TuneInRequest.eTUNEIN_MSG_XML_PARSER_START:
                        /** no need to set status to false, because status will be set to false when
                         * constructed. avoid parent fragment status change to false when changing
                         * from child fragment to parent fragment quickly**/
                        break;
                    case TuneInRequest.eTUNEIN_MSG_XML_PARSER_END:
                        if (!listFragments.isEmpty())
                            ((RadioListFragment)listFragments.get(listFragments.size()-1)).setXmlLoadStatus(true);
                        break;
                    case TuneInRequest.eTUNEIN_MSG_ERROR:
                        Toast.makeText(TuneInActivity.this, obj.toString(), Toast.LENGTH_SHORT).show();
                        break;
                }
            }
        });
    }

    private void addFragment(Fragment fragment, String tag) {
        FragmentManager manager = getFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        if (!listFragments.isEmpty())
            transaction.hide(listFragments.get(listFragments.size()-1));
        transaction.add(R.id.radio_list_container, fragment, tag);
        listFragments.add(fragment);
        transaction.addToBackStack(tag);
        transaction.commit();
    }

    private void removeFragment() {
        FragmentManager manager = getFragmentManager();
        manager.popBackStack();
        if (!listFragments.isEmpty())
            listFragments.remove(listFragments.size()-1);
    }

    private void showFragment(Fragment from, Fragment to, String tag) {
        FragmentManager manager = getFragmentManager();
        FragmentTransaction transaction = manager.beginTransaction();
        boolean isAdded = to.isAdded();
        if (!isAdded) {
            transaction.hide(from).add(R.id.radio_list_container, to, tag).show(to);
        } else {
            transaction.hide(from).show(to);
        }
        transaction.commit();
    }

    @Override
    public void onStart() {
        super.onStart();
        this.bindService(new Intent(this, BackendService.class), connection, Context.BIND_AUTO_CREATE);
        if (need_browser_index) {
            if (!listFragments.isEmpty())
                ((RadioListFragment)listFragments.get(listFragments.size() - 1)).setOnItemClickListener(itemClickListener);

            mTuneIn.radioTimeBrowseIndex();
            need_browser_index = false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();
        this.unbindService(connection);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (listFragments.size() > 1) {
            removeFragment();
        } else
            super.onBackPressed();
    }
}
