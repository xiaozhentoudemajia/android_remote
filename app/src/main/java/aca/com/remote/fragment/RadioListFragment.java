package aca.com.remote.fragment;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import aca.com.remote.R;
import aca.com.remote.adapter.RadioListItemAdapter;

/**
 * Created by jim.yu on 2017/12/29.
 */

public class RadioListFragment extends Fragment {
    private String TAG = RadioListFragment.class.getName();
    private ListView list_view;
    private RadioListItemAdapter adapter;
    private List<Object> list = new ArrayList<>();
    private AdapterView.OnItemClickListener listener = null;
    private String title;
    private boolean xml_load_status = false;//true means finish.

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_radio_list, container, false);
        list_view = view.findViewById(R.id.fragment_radio_list);
        adapter = new RadioListItemAdapter(getActivity(), list);
        list_view.setAdapter(adapter);
        list_view.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (null != listener)
                    listener.onItemClick(parent, view, position, id);
            }
        });

        Bundle bundle = getArguments();
        this.title = bundle.getString("title");
        if (null != this.title)
            ((TextView)getActivity().findViewById(R.id.toolbar_text)).setText(this.title);

        return view;
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public void onHiddenChanged(boolean hidden){
        super.onHiddenChanged(hidden);
        if (!hidden && null != this.title)
            ((TextView)getActivity().findViewById(R.id.toolbar_text)).setText(this.title);
    }

    public void addItem(Object obj){
        if (this.xml_load_status)
            return;
        list.add(obj);
        adapter.add(obj);
    }

    public boolean getXmlLoadStatus() {
        return this.xml_load_status;
    }

    public void setXmlLoadStatus(boolean val){
        this.xml_load_status = val;
    }

    public List<Object> getList(){
        return this.list;
    }

    public void setOnItemClickListener(AdapterView.OnItemClickListener listener) {
        this.listener = listener;
    }
}
