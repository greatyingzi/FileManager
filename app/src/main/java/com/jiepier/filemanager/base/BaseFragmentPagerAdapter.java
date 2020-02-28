package com.jiepier.filemanager.base;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentStatePagerAdapter;
import androidx.viewpager.widget.PagerAdapter;

import java.util.LinkedList;

/**
 * Created by prj on 2016/9/13.
 */
public class BaseFragmentPagerAdapter extends FragmentStatePagerAdapter {

    private LinkedList<Fragment> fragmentsList;
    public LinkedList<String> titleList;

    public BaseFragmentPagerAdapter(FragmentManager fm) {
        super(fm);
    }

    public BaseFragmentPagerAdapter(FragmentManager fm, LinkedList<Fragment> fragments, LinkedList<String> titleList) {
        super(fm);
        this.fragmentsList = fragments;
        this.titleList = titleList;
    }

    public BaseFragmentPagerAdapter(FragmentManager fm, LinkedList<Fragment> fragments) {
        super(fm);
        this.fragmentsList = fragments;
    }

    @Override
    public int getCount() {
        return fragmentsList == null? 0 : fragmentsList.size();
    }

    @Override
    public Fragment getItem(int arg0) {
        return fragmentsList.get(arg0);
    }

    @Override
    public int getItemPosition(Object object) {
        return PagerAdapter.POSITION_NONE;
    }

    @Override
    public CharSequence getPageTitle(int position) {
        return titleList.get(position);
    }

}
