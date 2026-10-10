package com.ivy.base.test.view;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import java.util.List;

public class PagerAdapter extends FragmentStateAdapter {

    private final List<Fragment> fragments;

    public PagerAdapter(FragmentActivity fa, List<Fragment> fragments) {
        super(fa);
        this.fragments = fragments;
    }

    @Override
    public Fragment createFragment(int position) {
        return fragments.get(position);
    }

    @Override
    public int getItemCount() {
        return fragments.size();
    }
}