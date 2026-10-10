package com.ivy.base.test.view.fragments;

import androidx.recyclerview.widget.DiffUtil;

import com.ivy.base.test.data.Event;

import java.util.List;

class Diff extends DiffUtil.Callback {

    private final List<Event> oldList;
    private final List<Event> newList;

    public Diff(List<Event> o, List<Event> n) {
        oldList = o;
        newList = n;
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList.get(oldItemPosition).seq ==
                newList.get(newItemPosition).seq;
    }

    @Override
    public boolean areContentsTheSame(int o, int n) {
        return oldList.get(o).equals(newList.get(n));
    }
}