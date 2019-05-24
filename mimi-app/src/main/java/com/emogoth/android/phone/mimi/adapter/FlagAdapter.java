package com.emogoth.android.phone.mimi.adapter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.SpinnerAdapter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class FlagAdapter extends BaseAdapter implements SpinnerAdapter {
    private final List<String> flagKeys;
    private final List<String> flagValues;

    public FlagAdapter(Map<String, String> flagsMap) {
        if (flagsMap == null || flagsMap.size() == 0) {
            throw new IllegalArgumentException("Flags map cannot be null or emtpy");
        }

        flagKeys = new ArrayList<>(flagsMap.keySet());
        flagValues = new ArrayList<>(flagsMap.values());
    }

    @Override
    public int getCount() {
        return flagKeys.size();
    }

    @Override
    public String getItem(int i) {
        return flagKeys.get(i);
    }

    @Override
    public long getItemId(int i) {
        return flagKeys.get(i).hashCode();
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        return null;
    }
}
