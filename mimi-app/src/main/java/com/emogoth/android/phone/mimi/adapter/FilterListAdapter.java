package com.emogoth.android.phone.mimi.adapter;

import androidx.appcompat.widget.AppCompatTextView;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.db.model.Filter;
import com.emogoth.android.phone.mimi.view.FilterDialog;

import java.util.List;


public class FilterListAdapter extends RecyclerView.Adapter<FilterListAdapter.FilterViewHolder> {

    private List<Filter> filters;

    public FilterListAdapter(List<Filter> filters) {
        this.filters = filters;
    }

    @Override
    public FilterViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.filter_list_item, parent, false);
        return new FilterViewHolder(v);
    }

    @Override
    public void onBindViewHolder(FilterViewHolder holder, int position) {
        final int pos = position;
        final Filter filter = filters.get(pos);

        holder.name.setText(filter.name);
        holder.regex.setText(filter.filter);
        holder.root.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FilterDialog dialog = new FilterDialog(view.getContext(), filter.board, filter.name, null);
                dialog.show();
            }
        });
    }

    @Override
    public int getItemCount() {
        return filters.size();
    }

    @Override
    public int getItemViewType(int position) {
        return super.getItemViewType(position);
    }

    public void setFilters(List<Filter> filters) {
        this.filters = filters;
        notifyDataSetChanged();
    }

    public void removeFilter(int index) {
        this.filters.remove(index);
        notifyItemRemoved(index);
    }

    public static class FilterViewHolder extends RecyclerView.ViewHolder {

        public final View root;
        public final AppCompatTextView name;
        public final AppCompatTextView regex;

        public FilterViewHolder(View itemView) {
            super(itemView);

            root = itemView;
            name = (AppCompatTextView) itemView.findViewById(R.id.filter_name);
            regex = (AppCompatTextView) itemView.findViewById(R.id.filter_regex);
        }
    }
}
