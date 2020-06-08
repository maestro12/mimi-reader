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
import androidx.core.view.MotionEventCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.db.BoardTableConnection;
import com.emogoth.android.phone.mimi.db.DatabaseUtils;
import com.emogoth.android.phone.mimi.interfaces.MoveAndDismissable;
import com.emogoth.android.phone.mimi.util.MimiUtil;
import com.emogoth.android.phone.mimi.util.RxUtil;
import com.mimireader.chanlib.models.ChanBoard;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import io.reactivex.disposables.Disposable;


public class BoardListAdapter extends RecyclerView.Adapter<BoardListAdapter.BoardViewHolder> implements MoveAndDismissable {

    public static final String LOG_TAG = BoardListAdapter.class.getSimpleName();
    public static final boolean LOG_DEBUG = true;

    private List<ChanBoard> boards = new ArrayList<>();

    private boolean editMode;
    private Context context;

    private View footer;
    private OnBoardClickListener boardClickListener;
    private AdapterView.OnItemLongClickListener itemLongClickListener;
    private OnStartDragListener dragListener;

    private Disposable updateBoardsSubscription;
    private Disposable favoriteSubscription;
    private Disposable removeBoardSubscription;

    public BoardListAdapter(final Context context, final List<ChanBoard> boards) {
        init(context, boards);
    }

    private void init(final Context context, final List<ChanBoard> boards) {
        if (boards == null) {
            throw new IllegalStateException("boards object is null");
        }

        this.context = context;
        this.boards = boards;
    }

    public void setBoards(final List<ChanBoard> updatedBoards) {
        if (boards.size() > 0) {
            DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override
                public int getOldListSize() {
                    return boards.size();
                }

                @Override
                public int getNewListSize() {
                    return updatedBoards.size();
                }

                @Override
                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                    return boards.get(oldItemPosition).equals(updatedBoards.get(newItemPosition));
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    return boards.get(oldItemPosition).compareContents(updatedBoards.get(newItemPosition));
                }
            }, true);
            this.boards = updatedBoards;
            result.dispatchUpdatesTo(this);
        } else {
            this.boards.clear();
            this.boards.addAll(updatedBoards);
            notifyDataSetChanged();
        }
    }

    public List<ChanBoard> getBoards() {
        return boards;
    }

    public void removeBoard(int item) {
        final String boardName = boards.get(item).getName();

        boards.remove(item);
        notifyDataSetChanged();

        RxUtil.safeUnsubscribe(removeBoardSubscription);
        removeBoardSubscription = BoardTableConnection.setBoardVisibility(boardName, false)
                .compose(DatabaseUtils.applySingleSchedulers())
                .subscribe();
    }

    public void setFooter(View footer) {
        this.footer = footer;
    }

    @Override
    public BoardViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        final View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.board_list_item, parent, false);
        return new BoardViewHolder(v);
    }

    @Override
    public void onBindViewHolder(final BoardViewHolder holder, int position) {
        final ChanBoard board = boards.get(position);

        if (holder.boardTitle != null) {
            holder.boardTitle.setText(board.getTitle());
        }

        if (holder.boardName != null) {
            holder.boardName.setText(board.getName());
        }

        if (holder.dragHandle != null) {
            holder.dragHandle.setOnTouchListener((v, event) -> {
                if (dragListener != null) {
                    if (MotionEventCompat.getActionMasked(event) ==
                            MotionEvent.ACTION_DOWN) {
                        dragListener.onStartDrag(holder);
                    }
                }
                return false;
            });

            if (editMode) {
                holder.dragHandle.setVisibility(View.VISIBLE);
            } else {
                holder.dragHandle.setVisibility(View.GONE);
            }
        }

        if (holder.favorite != null) {
            if (editMode) {
                holder.favorite.setVisibility(View.VISIBLE);
            } else {
                holder.favorite.setVisibility(View.GONE);
            }

            if (board.isFavorite()) {
                holder.favorite.setText(R.string.ic_favorite_set);
            } else {
                holder.favorite.setText(R.string.ic_favorite_unset);
            }
            holder.favorite.setOnClickListener(v -> {
                final TextView checkBox = (TextView) v;
                final boolean isFavorite = !checkBox.getText().equals(context.getString(R.string.ic_favorite_set));

                RxUtil.safeUnsubscribe(favoriteSubscription);
                favoriteSubscription = BoardTableConnection.setBoardFavorite(board.getName(), isFavorite)
                        .onErrorReturn(throwable -> {
                            Log.e(LOG_TAG, "Error setting board favorite: board=" + board.getName() + ", favorite=" + isFavorite, throwable);
                            return false;
                        })
                        .compose(DatabaseUtils.applySingleSchedulers())
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

        if (boardClickListener != null && holder.root != null) {
            if (editMode) {
                holder.root.setOnClickListener(null);
            } else {
                holder.root.setOnClickListener(v -> {
                    if (!editMode) {
                        boardClickListener.onBoardClick(board);
                    }
                });
            }
        }

        if (itemLongClickListener != null && holder.root != null) {
            if (editMode) {
                holder.root.setOnLongClickListener(null);
            } else {
                holder.root.setOnLongClickListener(v -> {
                    if (!editMode) {
                        itemLongClickListener.onItemLongClick(null, holder.root, holder.getAdapterPosition(), 0);
                    }
                    return true;
                });
            }
        }
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
    public int getItemCount() {
        return boards.size() + (footer != null ? 1 : 0);
    }

    @Override
    public void onItemMove(int from, int to) {
        final ChanBoard item = boards.set(from, boards.get(to));
        boards.set(to, item);

        RxUtil.safeUnsubscribe(updateBoardsSubscription);
        updateBoardsSubscription = BoardTableConnection.updateBoardOrder(boards)
                .delay(500, TimeUnit.MILLISECONDS)
                .compose(DatabaseUtils.applySchedulers())
                .subscribe(success -> {
                    if (success) {
                        MimiUtil.setBoardOrder(context, 7);
                    }
                });

        notifyDataSetChanged();
    }

    @Override
    public void onDismiss(int position) {
        removeBoard(position);
    }

    public void editMode(final boolean enabled) {
        editMode = enabled;

        notifyDataSetChanged();
    }

    public boolean IsEditMode() {
        return editMode;
    }

    public void setDragListener(OnStartDragListener listener) {
        dragListener = listener;
    }

    public void setOnBoardClickListener(OnBoardClickListener listener) {
        boardClickListener = listener;
    }

    public void setOnItemLongClickListener(AdapterView.OnItemLongClickListener listener) {
        itemLongClickListener = listener;
    }

    public static class BoardViewHolder extends RecyclerView.ViewHolder {
        private final View root;
        private final TextView boardName;
        private final TextView boardTitle;
        private final View dragHandle;
        private final TextView favorite;

        public BoardViewHolder(final View root) {
            super(root);
            this.root = root;

            boardName = root.findViewById(R.id.board_name);
            boardTitle = root.findViewById(R.id.board_title);
            dragHandle = root.findViewById(R.id.drag_handle);
            favorite = root.findViewById(R.id.favorite);

        }
    }

    public interface OnStartDragListener {

        /**
         * Called when a view is requesting a start of a drag.
         *
         * @param viewHolder The holder of the view to drag.
         */
        void onStartDrag(RecyclerView.ViewHolder viewHolder);
    }

    public interface OnBoardClickListener {
        void onBoardClick(ChanBoard board);
    }
}
