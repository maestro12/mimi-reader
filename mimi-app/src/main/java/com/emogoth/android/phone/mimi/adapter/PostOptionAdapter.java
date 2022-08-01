package com.emogoth.android.phone.mimi.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Filter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;

import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.app.MimiApplication;
import com.emogoth.android.phone.mimi.db.DatabaseUtils;
import com.emogoth.android.phone.mimi.db.PostOptionTableConnection;
import com.emogoth.android.phone.mimi.db.models.PostOption;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class PostOptionAdapter extends ArrayAdapter<PostOption> {

    private final Context context;

    @LayoutRes
    private final int layoutRes;

    private List<PostOption> items;
    private List<PostOption> originalList;

    public PostOptionAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull List<PostOption> objects) {
        super(context, resource, objects);

        this.items = new ArrayList<>(objects);
        this.originalList = new ArrayList<>(objects);
        this.context = context;
        this.layoutRes = resource;
    }

    @Override
    public int getCount() {
        return items.size();
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        final int pos = i;

        View v = LayoutInflater.from(this.context).inflate(this.layoutRes, viewGroup, false);

        TextView optionsText = (TextView) v.findViewById(R.id.option_text);
        optionsText.setText(items.get(pos).getOption());

        View deleteButton = v.findViewById(R.id.option_delete);
        deleteButton.setOnClickListener(v1 ->
                PostOptionTableConnection.deletePostOption(items.get(pos).getOption())
                .compose(DatabaseUtils.applySingleSchedulers())
                .subscribe(success -> {
                    if (success) {
                        String name = items.get(pos).getOption();
                        items.remove(pos);
                        notifyDataSetChanged();

                        for (int i1 = 0; i1 < originalList.size(); i1++) {
                            if (name.equals(originalList.get(i1).getOption())) {
                                originalList.remove(i1);
                            }
                        }
                    } else {
                        Toast.makeText(MimiApplication.getInstance(), R.string.error_deleting_name, Toast.LENGTH_SHORT).show();
                    }

                }, throwable -> Log.e("PostOptionAdapter", "Caught exception while deleting post option", throwable)));

        return v;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                List<PostOption> postOptionResults = new ArrayList<>();
                for (PostOption item : originalList) {
                    if (charSequence == null || StringUtils.containsIgnoreCase(item.getOption(), charSequence)) {
                        postOptionResults.add(item);
                    }
                }

                FilterResults results = new FilterResults();
                results.count = postOptionResults.size();
                results.values = postOptionResults;

                return results;
            }

            @Override
            protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
                if (items != null && filterResults != null && filterResults.values != null) {
                    items.clear();
                    items.addAll((List<PostOption>) filterResults.values);
                    notifyDataSetChanged();
                }
            }
        };
    }
}
