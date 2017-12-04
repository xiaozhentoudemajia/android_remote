package aca.com.remote.fragment;

import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

import aca.com.magicasakura.utils.ThemeUtils;
import aca.com.remote.R;
import aca.com.remote.fragmentnet.ChangeView;
import aca.com.remote.fragmentnet.RecommendFragment;

/**
 * Created by ali_mac on 2017/11/17.
 */

public class TabLibraryPagerFragment extends AttachFragment implements ChangeView {

    private ViewPager viewPager;
    private int page = 0;
    private boolean isFirstLoad = true;

    public static final TabLibraryPagerFragment newInstance(int page, String[] title) {
        TabLibraryPagerFragment f = new TabLibraryPagerFragment();
        Bundle bdl = new Bundle(1);
        bdl.putInt("page_number", page);
        f.setArguments(bdl);
        return f;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewPager.setCurrentItem(page);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(
                R.layout.fragment_net_tab, container, false);


        viewPager = (ViewPager) rootView.findViewById(R.id.viewpager);
        if (viewPager != null) {
            setupViewPager(viewPager);
            viewPager.setOffscreenPageLimit(2);
        }

        final TabLayout tabLayout = (TabLayout) rootView.findViewById(R.id.tabs);
        tabLayout.setTabTextColors(R.color.text_color, ThemeUtils.getThemeColorStateList(mContext, R.color.theme_color_primary).getDefaultColor());
        tabLayout.setSelectedTabIndicatorColor(ThemeUtils.getThemeColorStateList(mContext, R.color.theme_color_primary).getDefaultColor());
        tabLayout.setupWithViewPager(viewPager);

        return rootView;
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);
        if(recommendFragment == null){
            return;
        }
        if(isVisibleToUser && isFirstLoad){
            recommendFragment.requestData();
            isFirstLoad = false;
        }

        if (isVisibleToUser == true) {
            viewPager.setCurrentItem(0);
        }
    }
    RecommendFragment recommendFragment;
    private void setupViewPager(ViewPager viewPager) {
        TabLibraryPagerFragment.Adapter adapter = new TabLibraryPagerFragment.Adapter(getChildFragmentManager());
        recommendFragment = new RecommendFragment();
        recommendFragment.setChanger(this);
        adapter.addFragment(recommendFragment, "Radio");
        adapter.addFragment(new LibraryMusicFragment(), "My Device");
        adapter.addFragment(new ThirdPartyFragment(), "Thirdparty");

        viewPager.setAdapter(adapter);
    }

    @Override
    public void changeTo(int page) {
        if (viewPager != null)
            viewPager.setCurrentItem(page);
    }

    static class Adapter extends FragmentStatePagerAdapter {
        private final List<Fragment> mFragments = new ArrayList<>();
        private final List<String> mFragmentTitles = new ArrayList<>();

        public Adapter(FragmentManager fm) {
            super(fm);
        }

        public void addFragment(Fragment fragment, String title) {
            mFragments.add(fragment);
            mFragmentTitles.add(title);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position);
        }

        @Override
        public int getCount() {
            return mFragments.size();
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return mFragmentTitles.get(position);
        }
    }
}
