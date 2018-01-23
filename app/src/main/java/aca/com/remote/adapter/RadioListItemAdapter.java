package aca.com.remote.adapter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.PriorityQueue;

import aca.com.remote.R;
import aca.com.remote.tunes.util.TuneInElement;
import aca.com.remote.tunes.util.TuneInLink;

/**
 * Created by jim.yu on 2017/12/29.
 */

public class RadioListItemAdapter extends BaseAdapter {
    private Context context;
    private List<Object> mItems = null;
    private HashMap<String, Bitmap> iconMap = null;
    private boolean external_list = false;
    private final int REFRESH_ADAPTER = 0;

    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case REFRESH_ADAPTER:
                    notifyDataSetChanged();
                    break;
                default:
                    break;
            }
        }
    };

    public RadioListItemAdapter (Context context, List<Object> list) {
        this.context = context;
        this.iconMap = new HashMap<>();
        if (null == list) {
            this.mItems = new ArrayList<>();
        } else {
            external_list = true;
            this.mItems = list;
//            for (int i = 0; i < mItems.size(); i++)
//                iconList.add(null);
        }
    }

    public void add(Object item){
        if (!external_list)
            this.mItems.add(item);
//        iconList.add(null);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mItems.size();
    }

    @Override
    public Object getItem(int position) {
        return mItems.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (null == convertView) {
            convertView = LayoutInflater.from(context)
                    .inflate(R.layout.expandable_radio_list_item, parent, false);
        }
        Object o = mItems.get(position);
        if (o instanceof TuneInLink) {
            ((TextView)convertView.findViewById(R.id.radio_list_item_text)).setText(((TuneInLink)mItems.get(position)).getText());
            ((ImageView)convertView.findViewById(R.id.radio_list_item_image)).setImageResource(R.drawable.radio_link);
            ((ImageView)convertView.findViewById(R.id.radio_list_item_type)).setImageDrawable(null);
        } else if (o instanceof TuneInElement) {
            ((TextView)convertView.findViewById(R.id.radio_list_item_text)).setText(((TuneInElement)mItems.get(position)).getText());
            if (((TuneInElement) o).getType().equals("link")) {
                ((ImageView)convertView.findViewById(R.id.radio_list_item_image)).setImageResource(R.drawable.radio_link);
                ((ImageView)convertView.findViewById(R.id.radio_list_item_type)).setImageDrawable(null);
            } else {
                if (!this.iconMap.containsKey(((TuneInElement) o).getImage())) {
                    //no contain, set to default, and start to download
                    ((ImageView) convertView.findViewById(R.id.radio_list_item_image)).setImageResource(R.drawable.radio_station);
                    iconMap.put(((TuneInElement) o).getImage(), null);//set to null, means downloading
                    getHttpBitmap(((TuneInElement) o).getImage());
                } else {
                    String url = ((TuneInElement) o).getImage();
                    if (null != iconMap.get(url))
                        ((ImageView)convertView.findViewById(R.id.radio_list_item_image)).setImageBitmap(iconMap.get(url));
                    else//image is downloading, so set to default
                        ((ImageView) convertView.findViewById(R.id.radio_list_item_image)).setImageResource(R.drawable.radio_station);
                }
                ((ImageView)convertView.findViewById(R.id.radio_list_item_type)).setImageResource(R.drawable.radio_icon);

            }
        }

        return convertView;
    }

    public void getHttpBitmap(final String url) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                URL fileUrl = null;
                HttpURLConnection conn = null;
                Bitmap bitmap = null;
                try {
                    fileUrl = new URL(url);
                    conn = (HttpURLConnection) fileUrl.openConnection();
                    conn.setConnectTimeout(0);
                    conn.setDoInput(true);
                    conn.connect();
                    InputStream is = conn.getInputStream();
                    bitmap = BitmapFactory.decodeStream(is);
                    is.close();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    if (null != conn)
                        conn.disconnect();
                    if (null != bitmap) {
                        iconMap.put(url, bitmap);
                        Message msg = handler.obtainMessage();
                        msg.what = REFRESH_ADAPTER;
                        msg.obj = null;
                        handler.sendMessage(msg);
//                        notifyDataSetChanged();
                    }
                }
            }
        }).start();
    }
}
