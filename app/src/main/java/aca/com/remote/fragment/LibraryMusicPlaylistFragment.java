package aca.com.remote.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.app.ListFragment;
import android.util.Log;
import android.view.ContextMenu;
import android.view.View;
import android.widget.AdapterView;

import aca.com.remote.activity.LibraryMusicActitvity;
import aca.com.remote.tunes.PlaylistsAdapter;
import aca.com.remote.tunes.daap.Playlist;

/**
 * Created by ali_mac on 2017/11/21.
 */

public class LibraryMusicPlaylistFragment extends ListFragment implements LibraryMusicActitvity.ConnectionListener, AdapterView.OnItemClickListener {

    public static final String TAG = LibraryMusicPlaylistFragment.class.toString();

    PlaylistsAdapter adapter;
    LibraryMusicActitvity host;

    @Override
    public void onCreate(Bundle saved) {
        super.onCreate(saved);
        if (adapter == null)
            adapter = new PlaylistsAdapter(getActivity(), resultsUpdated);
        setListAdapter(adapter);
    }

    @Override
    public void onServiceConnected() {
        host = (LibraryMusicActitvity) getActivity();
        if (host != null) {
            if (adapter.results.isEmpty()) {
                host.library.readPlaylists(adapter);
            }
        } else {
            // Not quite ready, snooze for a bit
            try {
                Thread.sleep(500);
                onServiceConnected();
            } catch (InterruptedException e) {
                Log.e(TAG, "Waiting for Activity connection interrupted " + e);
            }
        }
    }

    @Override
    public void onServiceDisconnected() {
    }

    @Override
    public void onActivityCreated(Bundle saved) {
        super.onActivityCreated(saved);
        setListShown(false);
        getListView().setOnItemClickListener(this);
        host = (LibraryMusicActitvity) getActivity();

    }

    public Handler resultsUpdated = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            adapter.notifyDataSetChanged();
            if (isResumed() && host.positionViewed == 2)
                setListShown(true);
            else
                setListShownNoAnimation(true);
        }
    };

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {

        final AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo) menuInfo;

        try {
            // create context menu to play entire artist
            final Playlist ply = (Playlist) adapter.getItem(info.position);
            menu.setHeaderTitle(ply.getName());
            final String playlistid = Long.toString(ply.getID());

         /* wwj
         final MenuItem browse = menu.add(R.string.albums_menu_browse);
         browse.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
               Intent intent = new Intent(host, TracksActivity.class);
               intent.putExtra(Intent.EXTRA_TITLE, "");
               intent.putExtra("Playlist", playlistid);
               intent.putExtra("PlaylistPersistentId", ply.getPersistentId());
               intent.putExtra("AllAlbums", false);
               host.startActivityForResult(intent, 1);

               return true;
            }

         });

         final MenuItem play = menu.add(R.string.playlists_menu_play);
         play.setOnMenuItemClickListener(new OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
               host.session.controlPlayPlaylist(ply.getPersistentId(), "0");
               host.finish();
               return true;
            }

         });
         */
        } catch (Exception e) {
            Log.w(TAG, "onCreateContextMenu:" + e.getMessage());
        }

    }

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int position, long arg3) {
        try {
            final Playlist ply = (Playlist) adapter.getItem(position);
            final String playlistid = Long.toString(ply.getID());
/* wwj
         Intent intent = new Intent(host, TracksActivity.class);
         intent.putExtra(Intent.EXTRA_TITLE, "");
         intent.putExtra("Playlist", playlistid);
         intent.putExtra("PlaylistPersistentId", ply.getPersistentId());
         intent.putExtra("AllAlbums", false);
         host.startActivityForResult(intent, 1);
*/
        } catch (Exception e) {
            Log.w(TAG, "onCreate:" + e.getMessage());
        }
    }
}
