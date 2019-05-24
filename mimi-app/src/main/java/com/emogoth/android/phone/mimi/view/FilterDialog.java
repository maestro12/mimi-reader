package com.emogoth.android.phone.mimi.view;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;
import androidx.appcompat.app.AlertDialog;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;

import com.emogoth.android.phone.mimi.db.FilterTableConnection;

import io.reactivex.disposables.Disposable;

public class FilterDialog extends AlertDialog implements FilterView.ButtonClickListener {
    public static final String LOG_TAG = FilterDialog.class.getSimpleName();

    private FilterView.ButtonClickListener clickListener;
    private String boardName;

    protected FilterDialog(@NonNull Context context) {
        super(context);
    }

    protected FilterDialog(@NonNull Context context, @StyleRes int themeResId) {
        super(context, themeResId);
    }

    protected FilterDialog(@NonNull Context context, boolean cancelable, @Nullable OnCancelListener cancelListener) {
        super(context, cancelable, cancelListener);
    }

    public FilterDialog(@NonNull Context context, String boardName, String filterName, FilterView.ButtonClickListener listener) {
        super(context, true, null);

        this.boardName = boardName;
        this.clickListener = listener;

        init(context, filterName, true);
    }

    private void init(Context context, final String filterName, boolean cancelable) {
        final FilterView filterView = FilterView.create(context, boardName, this);

        if (!TextUtils.isEmpty(filterName)) {
            Disposable sub = FilterTableConnection.fetchFiltersByName(filterName)
                    .subscribe(
                            filters -> {
                                if (filters != null && filters.size() > 0) {
                                    filterView.setFilterName(filters.get(0).name);
                                    filterView.setFilterRegex(filters.get(0).filter);
                                    filterView.setHighlight(filters.get(0).highlight == 1);
                                }
                            },
                            throwable -> Log.e(LOG_TAG, "Error fetching filter when creating a new FilterView", throwable));
        }
        setView(filterView);
        setCancelable(cancelable);
    }

    @Override
    public void onSaveClicked(View v) {
        if (clickListener != null) {
            clickListener.onSaveClicked(v);
        }
        dismiss();
    }

    @Override
    public void onEditClicked(View v) {
        if (clickListener != null) {
            clickListener.onEditClicked(v);
        }
        dismiss();
    }

    @Override
    public void onCancelClicked(View v) {
        if (clickListener != null) {
            clickListener.onCancelClicked(v);
        }
        dismiss();
    }
}
