package com.emogoth.android.phone.mimi.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.TextView;
import android.widget.Toast;

import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.app.MimiApplication;
import com.emogoth.android.phone.mimi.db.PostOptionTableConnection;
import com.emogoth.android.phone.mimi.db.model.PostOption;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.functions.Consumer;

public class PostOptionAdapter extends BaseAdapter implements Filterable {

    private final LayoutInflater inflater;
    private List<PostOption> items = new ArrayList<>();
    private List<PostOption> originalList = new ArrayList<>();

    public void setItems(List<PostOption> items) {
        this.items.clear();
        this.items.addAll(items);

        originalList.clear();
        originalList.addAll(items);

        notifyDataSetChanged();
    }

    public PostOptionAdapter(Context context) {
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    @Override
    public int getCount() {
        return items != null ? items.size() : 0;
    }

    @Override
    public String getItem(int i) {
        return items != null ? items.get(i).option : null;
    }

    @Override
    public long getItemId(int i) {
        return items != null ? items.get(i).getId() : -1;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        final int pos = i;
        View v = inflater.inflate(R.layout.post_option_item, viewGroup, false);

        TextView optionsText = (TextView) v.findViewById(R.id.option_text);
        optionsText.setText(items.get(pos).option);

        View deleteButton = v.findViewById(R.id.option_delete);
        deleteButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PostOptionTableConnection.deletePostOption(items.get(pos).option)
                        .subscribe(new Consumer<Boolean>() {
                            @Override
                            public void accept(Boolean success) {
                                if (success) {
                                    String name = items.get(pos).option;
                                    items.remove(pos);
                                    notifyDataSetChanged();

                                    for (int i = 0; i < originalList.size(); i++) {
                                        if (name.equals(originalList.get(i).option)) {
                                            originalList.remove(i);
                                        }
                                    }
                                } else {
                                    Toast.makeText(MimiApplication.getInstance(), R.string.error_deleting_name, Toast.LENGTH_SHORT).show();
                                }

                            }
                        });
            }
        });

        return v;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence charSequence) {
                List<PostOption> postOptionResults = new ArrayList<>();
                for (PostOption item : originalList) {
                    if (charSequence == null || StringUtils.containsIgnoreCase(item.option, charSequence)) {
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
