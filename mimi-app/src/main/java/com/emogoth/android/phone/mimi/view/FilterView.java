package com.emogoth.android.phone.mimi.view;

import android.content.Context;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.AppCompatButton;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.AppCompatEditText;
import androidx.appcompat.widget.AppCompatSpinner;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.adapter.BoardDropDownAdapter;
import com.emogoth.android.phone.mimi.db.BoardTableConnection;
import com.emogoth.android.phone.mimi.db.FilterTableConnection;
import com.emogoth.android.phone.mimi.db.model.Board;
import com.emogoth.android.phone.mimi.util.MimiUtil;
import com.emogoth.android.phone.mimi.util.RxUtil;
import com.mimireader.chanlib.models.ChanBoard;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;

public class FilterView extends LinearLayout implements View.OnClickListener {
    private static final String LOG_TAG = FilterView.class.getSimpleName();

    private List<ChanBoard> boards;
    private ChanBoard currentBoard;
    private String boardName;

    private AppCompatSpinner activeBoard;
    private AppCompatEditText nameInput;
    private AppCompatEditText filterInput;
    private AppCompatCheckBox highlightCheckBox;

    private ButtonClickListener buttonClickListener;
    private AppCompatButton saveButton;

    private Disposable fetchBoardsSubscription;
    private Disposable fetchFiltersSubscription;
    private Disposable addFilterSubscription;

    public FilterView(Context context) {
        this(context, null);
    }

    public FilterView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FilterView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context);
    }

    public static FilterView create(Context context, String boardName, ButtonClickListener listener) {
        FilterView view = new FilterView(context, null);
        view.setBoardName(boardName);
        view.setClickListener(listener);

        return view;
    }

    private void init(Context context) {
        inflate(context, R.layout.post_filter_view, this);

        activeBoard = (AppCompatSpinner) findViewById(R.id.active_board);
        activeBoard.setOnItemSelectedListener(onBoardClicked());

        nameInput = (AppCompatEditText) findViewById(R.id.filter_name);
        filterInput = (AppCompatEditText) findViewById(R.id.filter_text);
        highlightCheckBox = (AppCompatCheckBox) findViewById(R.id.highlight_checkbox);

        saveButton = (AppCompatButton) findViewById(R.id.save_button);

        saveButton.setOnClickListener(this);
        findViewById(R.id.edit_button).setOnClickListener(this);
        findViewById(R.id.cancel_button).setOnClickListener(this);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        loadBoards();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();

        RxUtil.safeUnsubscribe(fetchBoardsSubscription);
        RxUtil.safeUnsubscribe(fetchFiltersSubscription);
        RxUtil.safeUnsubscribe(addFilterSubscription);
    }

    private void loadBoards() {
        if (getContext() != null) {
            final int orderId = MimiUtil.getBoardOrder(getContext());
            fetchBoardsSubscription = BoardTableConnection.fetchBoards(orderId)
                    .onErrorReturn(new Function<Throwable, List<Board>>() {
                        @Override
                        public List<Board> apply(Throwable throwable) {
                            Log.w(LOG_TAG, "Error fetching boards", throwable);
                            return new ArrayList<>();
                        }
                    })
                    .map(new Function<List<Board>, List<ChanBoard>>() {
                        @Override
                        public List<ChanBoard> apply(List<Board> boards) {
                            return BoardTableConnection.convertBoardDbModelsToChanBoards(boards);
                        }
                    })
                    .subscribe(new Consumer<List<ChanBoard>>() {
                        @Override
                        public void accept(List<ChanBoard> boards) {
                            for (ChanBoard board : boards) {
                                Log.d(LOG_TAG, "Board: name=" + board.getName());
                            }

                            FilterView.this.boards = boards;

                            BoardDropDownAdapter adapter = new BoardDropDownAdapter(getContext(), R.layout.board_filter_spinner_item, boards, -1);
                            adapter.setPromptEnabled(true);
                            activeBoard.setAdapter(adapter);

                            if (!TextUtils.isEmpty(boardName)) {
                                int pos = -1;
                                for (int i = 0; i < boards.size(); i++) {
                                    if (boards.get(i).getName().equals(boardName)) {
                                        pos = i;
                                        break;
                                    }
                                }

                                if (pos >= 0) {
                                    activeBoard.setSelection(pos + 1);
                                }
                            }
                        }
                    });
        }
    }

    public void setBoardName(String boardName) {
        this.boardName = boardName;
    }

    public void setFilterName(String name) {
        nameInput.setText(name);
    }

    public void setFilterRegex(String regex) {
        filterInput.setText(regex);
    }

    public void setHighlight(boolean highlight) {
        highlightCheckBox.setChecked(highlight);
    }

    private AdapterView.OnItemSelectedListener onBoardClicked() {
        return new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (position > 0) {
                    int pos = position - 1;

                    currentBoard = FilterView.this.boards.get(pos);
                } else {
                    currentBoard = null;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        };
    }

    public void setClickListener(ButtonClickListener listener) {
        this.buttonClickListener = listener;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.save_button:
                checkIfFilterExistsAndSave();
                break;
            case R.id.edit_button:
                if (buttonClickListener != null) {
                    buttonClickListener.onEditClicked(view);
                }
                break;
            case R.id.cancel_button:
                if (buttonClickListener != null) {
                    buttonClickListener.onCancelClicked(view);
                }
                break;
            default:
                Toast.makeText(getContext(), "wtf", Toast.LENGTH_SHORT).show();
        }
    }

    private void checkIfFilterExistsAndSave() {
        if (currentBoard != null) {
            final String filterName = nameInput.getText().toString();
            final String filterText = filterInput.getText().toString();
            final String boardName = currentBoard.getName();
            final boolean isRegex = highlightCheckBox.isChecked();

            final String regex = filterText;

            if (!TextUtils.isEmpty(filterName) && !TextUtils.isEmpty(filterText) && !TextUtils.isEmpty(boardName)) {
                fetchFiltersSubscription = FilterTableConnection.fetchFilters(boardName, filterName)
                        .subscribe(filters -> {
                            if (filters != null && filters.size() > 0) {
                                showOverwriteDialog(filterName, regex, boardName, isRegex);
                            } else {
                                saveFilter(filterName, regex, boardName, isRegex);
                            }
                        });
            }
        }
    }

    private void showOverwriteDialog(final String name, final String filter, final String board, final boolean isRegex) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getContext());
        dialogBuilder.setCancelable(false)
                .setTitle(R.string.filter_already_exists)
                .setMessage(R.string.overwite_filter_question)
                .setPositiveButton(R.string.yes, (dialogInterface, i) -> {
                    saveFilter(name, filter, board, isRegex);
                    dialogInterface.dismiss();
                })
                .setNegativeButton(R.string.no, (dialogInterface, i) -> dialogInterface.dismiss()).show();
    }

    private void saveFilter(String name, String filter, final String board, boolean isRegex) {
        addFilterSubscription = FilterTableConnection.addFilter(name, filter, board, isRegex)
                .onErrorReturn(throwable -> {
                    Log.e(LOG_TAG, "Error adding/updating filter for board: " + board, throwable);
                    return null;
                })
                .subscribe(success -> {
                    if (success == null || !success) {
                        Toast.makeText(getContext(), R.string.failed_to_save_filter, Toast.LENGTH_LONG).show();
                        Log.e(LOG_TAG, "Failed to save filter");
                    } else if (buttonClickListener != null) {
                        buttonClickListener.onSaveClicked(saveButton);
                    }
                });
    }

    public interface ButtonClickListener {
        void onSaveClicked(View v);

        void onEditClicked(View v);

        void onCancelClicked(View v);
    }
}
