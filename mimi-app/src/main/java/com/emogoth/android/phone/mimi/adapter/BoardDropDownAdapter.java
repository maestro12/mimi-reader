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
import android.widget.TextView;

import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.db.BoardTableConnection;
import com.emogoth.android.phone.mimi.db.DatabaseUtils;
import com.emogoth.android.phone.mimi.util.RxUtil;
import com.mimireader.chanlib.models.ChanBoard;

import java.util.List;

import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;


public class BoardDropDownAdapter extends BaseAdapter {

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
    private Subscription favoriteSubscription;
    private Subscription removeBoardSubscription;

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
        removeBoardSubscription = BoardTableConnection.setBoardVisibility(boardName, false)
                .compose(DatabaseUtils.<Boolean>applySchedulers())
                .subscribe();
    }

    @Override
    public int getCount() {
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

        if (position < boards.size()) {

            final ViewHolder viewHolder;
            if (convertView == null) {
                convertView = inflater.inflate(layoutResource, parent, false);

                viewHolder = new ViewHolder(convertView);
                convertView.setTag(viewHolder);
            } else {
                viewHolder = (ViewHolder) convertView.getTag();
            }

            final ChanBoard board = boards.get(position);
            if (viewHolder.boardName != null) {
                if (viewType != MODE_ACTIONBAR) {
                    viewHolder.boardName.setText("/" + board.getName() + "/");
                } else {
                    viewHolder.boardName.setVisibility(View.GONE);
                }
            }

            if (viewHolder.boardTitle != null) {
                viewHolder.boardTitle.setText(board.getTitle());
            }

            if (viewHolder.dragHandle != null) {
                if (editMode) {
                    viewHolder.dragHandle.setVisibility(View.VISIBLE);
                } else {
                    viewHolder.dragHandle.setVisibility(View.GONE);
                }
            }

            if (viewHolder.favorite != null) {
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
                viewHolder.favorite.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(final View v) {
                        final TextView checkBox = (TextView) v;
                        final boolean isFavorite = !checkBox.getText().equals(context.getString(R.string.ic_favorite_set));

                        RxUtil.safeUnsubscribe(favoriteSubscription);
                        favoriteSubscription = BoardTableConnection.setBoardFavorite(board.getName(), isFavorite)
                                .onErrorReturn(new Func1<Throwable, Boolean>() {
                                    @Override
                                    public Boolean call(Throwable throwable) {
                                        Log.e(LOG_TAG, "Error setting board favorite: board=" + board.getName() + ", favorite=" + isFavorite, throwable);
                                        return false;
                                    }
                                })
                                .subscribe(new Action1<Boolean>() {
                                    @Override
                                    public void call(Boolean success) {
                                        if (success) {
                                            board.setFavorite(isFavorite);
                                        } else {
                                            if (isFavorite) {
                                                checkBox.setText(R.string.ic_favorite_unset);
                                            } else {
                                                checkBox.setText(R.string.ic_favorite_set);
                                            }
                                        }
                                    }
                                });

                        if (isFavorite) {
                            checkBox.setText(R.string.ic_favorite_set);
                        } else {
                            checkBox.setText(R.string.ic_favorite_unset);
                        }
                    }
                });
            }
        }

        return convertView;

    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {

        final ViewHolder viewHolder;
        if (convertView == null) {
            convertView = inflater.inflate(layoutResource, parent, false);

            viewHolder = new ViewHolder(convertView);
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        final ChanBoard board = boards.get(position);
        if (viewHolder.boardName != null) {
            viewHolder.boardName.setText("/" + board.getName() + "/");
        }

        if (viewHolder.boardTitle != null) {
            viewHolder.boardTitle.setText(board.getTitle());
        }

//        if (position == 0) {
//            if (viewHolder.boardName != null) {
//                viewHolder.boardName.setVisibility(View.GONE);
//            }
//
//            if (viewHolder.boardTitle != null) {
//                viewHolder.boardTitle.setText(R.string.history_tab);
//            }
//        } else if (position == 1) {
//            if (viewHolder.boardName != null) {
//                viewHolder.boardName.setVisibility(View.GONE);
//            }
//
//            if (viewHolder.boardTitle != null) {
//                viewHolder.boardTitle.setText(R.string.bookmarks_tab);
//            }
//        } else {
//            final ChanBoard board = boards.get(position - 2);
//            if (viewHolder.boardName != null) {
//                viewHolder.boardName.setVisibility(View.VISIBLE);
//                viewHolder.boardName.setText("/" + board.getName() + "/");
//            }
//
//            if (viewHolder.boardTitle != null) {
//                viewHolder.boardTitle.setText(board.getTitle());
//            }
//        }

        return convertView;
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
