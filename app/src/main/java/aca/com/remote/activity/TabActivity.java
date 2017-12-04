package aca.com.remote.activity;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;

import aca.com.remote.R;
import aca.com.remote.fragment.ArtistDetailFragment;
import aca.com.remote.fragment.TabPagerFragment;

/**
 * Created by wm on 2016/4/11.
 */
public class TabActivity extends BaseActivity {

    private int page, artistId, albumId;

    @Override
    public void onCreate(final Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if (getIntent().getExtras() != null) {
            page = getIntent().getIntExtra("page_number", 0);
            artistId = getIntent().getIntExtra("artist", 0);
            albumId = getIntent().getIntExtra("album", 0);
        }
        setContentView(R.layout.activity_tab);

        if (artistId != 0) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            ArtistDetailFragment fragment = ArtistDetailFragment.newInstance(artistId);
            transaction.hide(getSupportFragmentManager().findFragmentById(R.id.tab_container));
            transaction.add(R.id.tab_container, fragment);
            transaction.addToBackStack(null).commit();
        }
        if (albumId != 0) {

        }

        String[] title = {"Songs", "Artists", "Albums", "Folders"};
        TabPagerFragment fragment = TabPagerFragment.newInstance(page, title);
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.add(R.id.tab_container, fragment);
        transaction.commitAllowingStateLoss();

    }
}