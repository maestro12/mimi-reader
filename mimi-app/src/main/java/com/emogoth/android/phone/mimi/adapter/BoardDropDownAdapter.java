/*
 * Copyright (c) 2016. Eli Connelly
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.emogoth.android.phone.mimi.adapter;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import androidx.annotation.StringRes;

import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.db.BoardTableConnection;
import com.emogoth.android.phone.mimi.db.DatabaseUtils;
import com.emogoth.android.phone.mimi.util.RxUtil;
import com.mimireader.chanlib.models.ChanBoard;

import java.util.List;

import io.reactivex.disposables.Disposable;


public class BoardDropDownAdapter extends BaseAdapter implements SpinnerAdapter {

    public static final String LOG_TAG = BoardDropDownAdapter.class.getSimpleName();
    public static final boolean LOG_DEBUG = true;

    public final static int MODE_LIST = 0;
    public final static int MODE_ACTIONBAR = 1;

    private LayoutInflater inflater;
    private List<ChanBoard> boards;

    private int viewType;
    private int layoutResource;
    private boolean editMode;

    private Context context;
    private Disposable favoriteSubscription;
    private Disposable removeBoardSubscription;
    private boolean promptEnabled;
    private int promptTextRes = R.string.select_a_board;

    public BoardDropDownAdapter(final Context context, final int textViewResourceId, final List<ChanBoard> boards, final int type) {
        init(context, textViewResourceId, boards, type);
    }

    private void init(final Context context, final int textViewResourceId, final List<ChanBoard> boards, final int type) {
        if (boards == null) {
            throw new IllegalStateException("boards object is null");
        }

        this.context = context;
        this.boards = boards;
        this.layoutResource = textViewResourceId;
        this.viewType = type;
        this.inflater = LayoutInflater.from(context);

        if (type == MODE_LIST) {
            layoutResource = R.layout.board_list_item;
        } else if (type == MODE_ACTIONBAR) {
            layoutResource = R.layout.board_spinner_item;
        }
    }

    public void setBoards(final List<ChanBoard> boards) {
        this.boards.clear();
        this.boards.addAll(boards);
        notifyDataSetChanged();
    }

    public void removeBoard(int item) {
        final String boardName = boards.get(item).getName();

        boards.remove(item);
        notifyDataSetChanged();

        RxUtil.safeUnsubscribe(removeBoardSubscription);
        ChanBoard board = new ChanBoard();
        board.setName(boardName);
        removeBoardSubscription = BoardTableConnection.setBoardVisibility(board, false)
                .compose(DatabaseUtils.applySingleSchedulers())
                .subscribe();
    }

    @Override
    public int getCount() {
        if (promptEnabled) {
            return boards.size() + 1;
        }

        return boards.size();
    }

    @Override
    public ChanBoard getItem(final int position) {
        if (boards != null && position < boards.size()) {
            return boards.get(position);
        }

        return null;
    }

    @Override
    public long getItemId(final int position) {
        if (boards != null && position < boards.size()) {
            final ChanBoard board = boards.get(position);
            final String s = board.getName() + board.getTitle();
            return s.hashCode();
        }

        return -1;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getView(final int position, View convertView, final ViewGroup parent) {

        final int pos;
        if (promptEnabled) {
            pos = position - 1;
        } else {
            pos = position;
        }

        if (pos < boards.size()) {

            final ViewHolder viewHolder;
            if (convertView == null) {
                convertView = inflater.inflate(layoutResource, parent, false);

                viewHolder = new ViewHolder(convertView);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            final ChanBoard board = pos >= 0 ? boards.get(pos) : null;
            if (viewHolder.boardName != null && board != null) {
                if (viewType != MODE_ACTIONBAR) {
                    viewHolder.boardName.setText("/" + board.getName() + "/");
                } else {
                    viewHolder.boardName.setVisibility(View.GONE);
                }
            }

            if (viewHolder.boardTitle != null) {
                if (pos < 0) {
                    viewHolder.boardTitle.setText(promptTextRes);
                } else {
                    viewHolder.boardTitle.setText(board.getTitle());
                }
            }

            if (viewHolder.dragHandle != null) {
                if (editMode) {
                    viewHolder.dragHandle.setVisibility(View.VISIBLE);
                } else {
                    viewHolder.dragHandle.setVisibility(View.GONE);
                }
            }

            if (viewHolder.favorite != null && board != null) {
                if (editMode) {
                    viewHolder.favorite.setVisibility(View.VISIBLE);
                } else {
                    viewHolder.favorite.setVisibility(View.GONE);
                }

                if (board.isFavorite()) {
                    viewHolder.favorite.setText(R.string.ic_favorite_set);
                } else {
                    viewHolder.favorite.setText(R.string.ic_favorite_unset);
                }
                viewHolder.favorite.setOnClickListener(v -> {
                    final TextView checkBox = (TextView) v;
                    final boolean isFavorite = !checkBox.getText().equals(context.getString(R.string.ic_favorite_set));

                    RxUtil.safeUnsubscribe(favoriteSubscription);
                    favoriteSubscription = BoardTableConnection.setBoardFavorite(board.getName(), isFavorite)
                            .onErrorReturn(throwable -> {
                                Log.e(LOG_TAG, "Error setting board favorite: board=" + board.getName() + ", favorite=" + isFavorite, throwable);
                                return false;
                            })
                            .subscribe(success -> {
                                if (success) {
                                    board.setFavorite(isFavorite);
                                } else {
                                    if (isFavorite) {
                                        checkBox.setText(R.string.ic_favorite_unset);
                                    } else {
                                        checkBox.setText(R.string.ic_favorite_set);
                                    }
                                }
                            });

                    if (isFavorite) {
                        checkBox.setText(R.string.ic_favorite_set);
                    } else {
                        checkBox.setText(R.string.ic_favorite_unset);
                    }
                });
            }
        }

        return convertView;

    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {

        final int pos;
        if (promptEnabled) {
            pos = position - 1;
        } else {
            pos = position;
        }

        final ViewHolder viewHolder;
        if (convertView == null) {
            convertView = inflater.inflate(layoutResource, parent, false);

            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final ChanBoard board = pos >= 0 ? boards.get(pos) : null;
        if (viewHolder.boardName != null && board != null) {
            viewHolder.boardName.setText("/" + board.getName() + "/");
        }

        if (viewHolder.boardTitle != null) {
            if (pos < 0) {
                viewHolder.boardTitle.setText(promptTextRes);
            } else {
                viewHolder.boardTitle.setText(board.getTitle());
            }
        }

        return convertView;
    }

    public void setPromptEnabled(boolean enabled) {
        this.promptEnabled = enabled;
    }

    public void setPromptText(@StringRes int promptTextRes) {
        this.promptTextRes = promptTextRes;
        notifyDataSetChanged();
    }


    public static class ViewHolder {
        private final View root;
        private final TextView boardName;
        private final TextView boardTitle;
        private final View dragHandle;
        private final TextView favorite;

        public ViewHolder(final View root) {
            this.root = root;

            boardName = (TextView) root.findViewById(R.id.board_name);
            boardTitle = (TextView) root.findViewById(R.id.board_title);
            dragHandle = root.findViewById(R.id.drag_handle);
            favorite = (TextView) root.findViewById(R.id.favorite);

        }
    }

}
