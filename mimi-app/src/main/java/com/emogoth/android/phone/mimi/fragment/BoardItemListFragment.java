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

package com.emogoth.android.phone.mimi.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.Space;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.text.TextUtils;
import android.util.Log;
import android.view.ActionMode;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebView;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.emogoth.android.phone.mimi.BuildConfig;
import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.activity.MimiActivity;
import com.emogoth.android.phone.mimi.adapter.BoardListAdapter;
import com.emogoth.android.phone.mimi.db.BoardTableConnection;
import com.emogoth.android.phone.mimi.db.DatabaseUtils;
import com.emogoth.android.phone.mimi.db.model.Board;
import com.emogoth.android.phone.mimi.event.ActionModeEvent;
import com.emogoth.android.phone.mimi.fourchan.FourChanConnector;
import com.emogoth.android.phone.mimi.interfaces.BoardItemClickListener;
import com.emogoth.android.phone.mimi.interfaces.ContentInterface;
import com.emogoth.android.phone.mimi.interfaces.IToolbarContainer;
import com.emogoth.android.phone.mimi.interfaces.OnBoardsUpdatedCallback;
import com.emogoth.android.phone.mimi.interfaces.TabInterface;
import com.emogoth.android.phone.mimi.util.AppRater;
import com.emogoth.android.phone.mimi.util.BusProvider;
import com.emogoth.android.phone.mimi.util.MimiUtil;
import com.emogoth.android.phone.mimi.util.RequestQueueUtil;
import com.emogoth.android.phone.mimi.util.RxUtil;
import com.emogoth.android.phone.mimi.view.DividerItemDecoration;
import com.mimireader.chanlib.ChanConnector;
import com.mimireader.chanlib.models.ChanBoard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;


/**
 * A list fragment representing a list of PostItems. This fragment
 * also supports tablet devices by allowing list items to be given an
 * 'activated' state upon selection. This helps indicate which item is
 * currently being viewed in a {@link ThreadPagerFragment}.
 * <p/>
 * Activities containing this fragment MUST implement the {@link com.emogoth.android.phone.mimi.interfaces.BoardItemClickListener}
 * interface.
 */
public class BoardItemListFragment extends MimiFragmentBase implements ListView.OnItemClickListener,
        TabInterface, ContentInterface {

    /**
     * The serialization (saved instance state) Bundle key representing the
     * activated item position. Only used on tablets.
     */
    private static final String STATE_ACTIVATED_POSITION = "activated_position";
    private static final String LOG_TAG = BoardItemListFragment.class.getSimpleName();

    private static final int NO_ORDER_SELECTED = -1;

    public static final int TAB_ID = 100;

    /**
     * The fragment's current callback object, which is notified of list item
     * clicks.
     */
    private BoardItemClickListener mCallbacks;

    public List<ChanBoard> boards = new ArrayList<>();
    private boolean activateOnItemClick;

    /**
     * The current activated item position. Only used on tablets.
     */
    private int mActivatedPosition = ListView.INVALID_POSITION;
    private RecyclerView boardsList;
    private BoardItemClickListener boardItemClickListener;
    private OnBoardsUpdatedCallback boardsCallback;

    private BoardListAdapter boardListAdapter;

    private boolean dbWasUpdated = false;
    private MenuItem bookmarkCountMenu;
    private int newPostCount = 0;
    private ViewGroup boardOrderContainer;
    private TextView showContentButton;
    private TextView boardOrderText;
    private ViewGroup orderTypeList;
    private TextView orderByFavorites;
    private TextView orderByName;
    private TextView orderByTitle;
    private TextView orderByAccess;
    private TextView orderByLast;
    private TextView orderByPost;
    private TextView orderbyCustom;
    private Space actionBarSpacer;
    private ViewGroup boardOrderBackground;
    private Spinner toolbarSpinner;

    private Animation revealListAnimation;
    private Animation showBoardOrderBackground;
    private Animation hideListAnimation;
    private Animation hideBoardOrderBackground;
    private String[] orderByNames;
    private boolean boardOrderListVisible;
    private boolean editMode = false;
    private Toolbar toolbar;
    private ChanConnector chanConnector;
    private View listFooter;
    private ItemTouchHelper itemTouchHelper;
    private ActionMode.Callback actionModeCallback;

    private Subscription boardInfoSubscription;
    private Subscription fetchBoardsSubscription;


    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public BoardItemListFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final String userAgent = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(getString(R.string.user_agent_pref), null);
        if (userAgent != null) {
            RequestQueueUtil.getInstance().setUserAgent(userAgent);
        } else {
            try {
                final WebView webView = new WebView(getActivity());
                final String webViewAgent = webView.getSettings().getUserAgentString();

                PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putString(getString(R.string.user_agent_pref), webViewAgent).apply();
                webView.destroy();
            } catch (Exception e) {
                Log.e(LOG_TAG, "Could not set user agent from webview, using default", e);

                if (PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(getString(R.string.user_agent_pref), null) == null) {
                    final String defaultAgent = System.getProperty("http.agent");
                    PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putString(getString(R.string.user_agent_pref), defaultAgent).apply();
                }
            }
        }

        if (getActivity() instanceof MimiActivity) {
            toolbar = ((MimiActivity) getActivity()).getToolbar();
        }

        final View boardsView = inflater.inflate(R.layout.fragment_boards_list, container, false);

        listFooter = inflater.inflate(R.layout.footer_board_list, container, false);

        actionBarSpacer = (Space) boardsView.findViewById(R.id.spacer);

        boardListAdapter = new BoardListAdapter(getActivity(), boards);


        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        boardsList = (RecyclerView) boardsView.findViewById(R.id.boards_list);
        boardsList.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.LIST_VERTICAL));
        boardsList.setLayoutManager(layoutManager);
        boardsList.setAdapter(boardListAdapter);
        boardListAdapter.setDragListener(new BoardListAdapter.OnStartDragListener() {
            @Override
            public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
                itemTouchHelper.startDrag(viewHolder);
            }
        });
        boardListAdapter.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (getActivity() != null) {

                    final MimiActivity activity = ((MimiActivity) getActivity());
                    final Toolbar toolbar = activity.getToolbar();

                    toolbar.startActionMode(getActionMode());
                }
                return false;
            }
        });
        return boardsView;
    }

    private ActionMode.Callback getActionMode() {
        editMode = true;
        boardListAdapter.editMode(true);

        if (actionModeCallback == null) {
            actionModeCallback = new ActionMode.Callback() {
                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    if (getActivity() == null || !(getActivity() instanceof MimiActivity)) {
                        return false;
                    }

                    MimiActivity activity = (MimiActivity) getActivity();

                    final MenuInflater inflater = mode.getMenuInflater();
                    inflater.inflate(R.menu.edit_boards, menu);

                    if (activity.getDrawerLayout() != null) {
                        activity.getDrawerLayout().setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
                    }

                    toolbar.setVisibility(View.GONE);

                    actionBarSpacer.setVisibility(View.GONE);
                    boardOrderContainer.setVisibility(View.GONE);

                    boardsList.setClickable(false);
                    boardListAdapter.setOnItemClickListener(null);

                    mode.setTitle(R.string.manage_boards);

                    BusProvider.getInstance().post(new ActionModeEvent(true));

                    return true;
                }

                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    return false;
                }

                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    if (item.getItemId() == R.id.add_board) {
                        showAddBoardDialog();
                    }
                    return true;
                }

                @Override
                public void onDestroyActionMode(ActionMode mode) {
//                            ((MimiActivity) getActivity()).getToolbar().setVisibility(View.VISIBLE);
                    if (getActivity() == null || !(getActivity() instanceof MimiActivity)) {
                        return;
                    }

                    MimiActivity activity = (MimiActivity) getActivity();

                    editMode = false;

                    activity.getDrawerLayout().setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                    toolbar.setVisibility(View.VISIBLE);

                    boardListAdapter.editMode(false);
//                            boardsList.disableDragAndDrop();
//                            boardsList.disableSwipeToDismiss();
                    boardsList.setClickable(true);
                    boardListAdapter.setOnItemClickListener(BoardItemListFragment.this);

                    boardOrderContainer.setVisibility(View.VISIBLE);
                    actionBarSpacer.setVisibility(View.VISIBLE);

                    final int order = MimiUtil.getBoardOrder(getActivity());
                    boardOrderText.setText(orderByNames[order]);

                    boards = boardListAdapter.getBoards();

                    BusProvider.getInstance().post(new ActionModeEvent(false));
                }
            };
        }

        return actionModeCallback;
    }

    private void showAddBoardDialog() {
        if (getActivity() == null) {
            return;
        }

        final AlertDialog.Builder alertBuilder = new AlertDialog.Builder(getActivity());
        final EditText input = new EditText(getActivity());

        input.setHint(R.string.board_name_input_hint);
        input.setSingleLine();
        input.setImeOptions(EditorInfo.IME_ACTION_DONE);

        alertBuilder.setView(input);
        alertBuilder.setPositiveButton(R.string.add, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                addBoard(input);
            }
        });
        alertBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        alertBuilder.setTitle(R.string.add_board);

        final AlertDialog d = alertBuilder.create();
        d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        d.show();

        input.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    addBoard(input);
                    d.dismiss();
                }
                return true;
            }
        });
    }

    private void addBoard(EditText input) {
        if (TextUtils.isEmpty(input.getText())) {
            return;
        }

        final String boardName = input.getText().toString().replaceAll("/", "").toLowerCase().trim();

        RxUtil.safeUnsubscribe(boardInfoSubscription);
        boardInfoSubscription = BoardTableConnection.fetchBoard(boardName)
                .flatMap(new Func1<ChanBoard, Observable<Boolean>>() {
                    @Override
                    public Observable<Boolean> call(ChanBoard chanBoard) {
                        return BoardTableConnection.setBoardVisibility(boardName, true);
                    }
                })
                .flatMap(new Func1<Boolean, Observable<List<Board>>>() {
                    @Override
                    public Observable<List<Board>> call(Boolean success) {

                        if (success) {
                            final int orderId = MimiUtil.getBoardOrder(getActivity());
                            return BoardTableConnection.fetchBoards(orderId, false);
                        }

                        return Observable.just(Collections.<Board>emptyList());
                    }
                })
                .flatMap(new Func1<List<Board>, Observable<List<ChanBoard>>>() {
                    @Override
                    public Observable<List<ChanBoard>> call(List<Board> boards) {
                        return Observable.just(BoardTableConnection.convertBoardDbModelsToChanBoards(boards));
                    }
                })
                .compose(DatabaseUtils.<List<ChanBoard>>applySchedulers())
                .subscribe(new Action1<List<ChanBoard>>() {
                    @Override
                    public void call(List<ChanBoard> boards) {
                        if (boards != null && boards.size() > 0) {
                            boardListAdapter.setBoards(boards);
                            BoardItemListFragment.this.boards = boards;
                        } else {
                            Toast.makeText(getActivity(), R.string.error_adding_board, Toast.LENGTH_SHORT).show();
                        }
                    }
                });


    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        setupHeader(view);

        if (getUserVisibleHint()) {
            initMenu();
        }

        setupTouchListeners();

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
        int lastVersion = preferences.getInt(getString(R.string.last_version_code_pref), 0);
        if (BuildConfig.VERSION_CODE > lastVersion) {
            showChangeLog();
            preferences.edit().putInt(getString(R.string.last_version_code_pref), BuildConfig.VERSION_CODE).apply();
        }

        chanConnector = new FourChanConnector
                .Builder()
                .setCacheDirectory(MimiUtil.getInstance().getCacheDir())
                .setEndpoint(FourChanConnector.getDefaultEndpoint(MimiUtil.isSecureConnection(getActivity())))
                .setPostEndpoint(FourChanConnector.getDefaultPostEndpoint())
                .build();

        boardListAdapter.setOnItemClickListener(this);

        loadBoards(false);


        // Restore the previously serialized activated item position.
        if (savedInstanceState != null
                && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
//            setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
        }

    }

    private void loadBoards(final boolean allBoards) {
        RxUtil.safeUnsubscribe(fetchBoardsSubscription);
        fetchBoardsSubscription = BoardTableConnection.fetchBoards(MimiUtil.getBoardOrder(getActivity()), allBoards)
                .flatMap(new Func1<List<Board>, Observable<List<ChanBoard>>>() {
                    @Override
                    public Observable<List<ChanBoard>> call(List<Board> dbBoards) {
                        Observable<List<ChanBoard>> mappedObservable = chanConnector.fetchBoards();

                        if (dbBoards.size() > 0) {
                            List<ChanBoard> chanBoards = new ArrayList<>(dbBoards.size());
                            List<String> visibleBoardNames = new ArrayList<>(dbBoards.size());
                            for (Board board : dbBoards) {
                                if (board.visible != null && board.visible) {
                                    chanBoards.add(BoardTableConnection.convertBoardDbModelToBoard(board));
                                    visibleBoardNames.add(board.name);
                                }
                            }

                            boards = chanBoards;
                            mappedObservable = mappedObservable.doOnNext(BoardTableConnection.saveBoards(visibleBoardNames));
                        } else {
                            mappedObservable = mappedObservable.flatMap(new Func1<List<ChanBoard>, Observable<List<ChanBoard>>>() {
                                @Override
                                public Observable<List<ChanBoard>> call(List<ChanBoard> chanBoards) {
                                    List<ChanBoard> filteredBoards = BoardTableConnection.filterVisibleBoards(getActivity(), chanBoards);
                                    Observable<List<ChanBoard>> saveBoardsObservable = Observable.just(filteredBoards);

                                    List<String> filteredBoardNames = new ArrayList<>();
                                    for (ChanBoard filteredBoard : filteredBoards) {
                                        filteredBoardNames.add(filteredBoard.getName());
                                    }

                                    BoardTableConnection.saveBoards(chanBoards, filteredBoardNames);
                                    return saveBoardsObservable;
                                }
                            });
                        }

                        return mappedObservable;
                    }
                })
                .compose(DatabaseUtils.<List<ChanBoard>>applySchedulers())
                .subscribe(new Action1<List<ChanBoard>>() {
                    @Override
                    public void call(List<ChanBoard> chanBoard) {

                        if (boards == null || boards.size() == 0) {
                            boards = chanBoard;
                        }

                        if (boardsList != null) {
                            boardOrderContainer.setVisibility(View.VISIBLE);
                            boardListAdapter.setBoards(boards);
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        if (getActivity() != null) {
                            Toast.makeText(getActivity(), R.string.error_occurred, Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void setupTouchListeners() {

        ItemTouchHelper.SimpleCallback simpleItemTouchCallback = new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            RecyclerView.ViewHolder mCurrentTarget = null;

            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                if (boardListAdapter.IsEditMode()) {
                    mCurrentTarget = target;
                    boardListAdapter.onItemMove(viewHolder.getAdapterPosition(), target.getAdapterPosition());
                    return true;
                }

                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                boardListAdapter.onDismiss(viewHolder.getAdapterPosition());
            }

            @Override
            public int getSwipeDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                if (boardListAdapter.IsEditMode())
                    return super.getSwipeDirs(recyclerView, viewHolder);
                return 0;

            }

            @Override
            public int getDragDirs(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder) {
                if (boardListAdapter.IsEditMode())
                    return super.getDragDirs(recyclerView, viewHolder);
                return 0;
            }
        };

        itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(boardsList);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        if (boards == null || boards.size() <= position) {
            if (boardsList != null) {
                Snackbar.make(boardsList, R.string.error_occurred, Snackbar.LENGTH_LONG).show();
            }

            return;
        }
        if (boardsCallback != null) {
            boardsCallback.onBoardsUpdated(boards);
        }

        BoardTableConnection.incrementAccessCount(boards.get(position).getName())
                .compose(DatabaseUtils.<Boolean>applySchedulers())
                .subscribe();
        mCallbacks.onBoardItemClick(boards.get(position), true);

    }

    @Override
    public void initMenu() {
        super.initMenu();

        if (toolbar != null) {
            setupToolBar();
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        BusProvider.getInstance().unregister(this);

        RxUtil.safeUnsubscribe(fetchBoardsSubscription);
        RxUtil.safeUnsubscribe(boardInfoSubscription);
    }

    @Override
    public void onResume() {
        super.onResume();

        BusProvider.getInstance().register(this);

        if (getActivity() != null) {
            AppRater.appLaunched(getActivity());
        }

        if (getActivity() != null) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            boolean showAllBoards = prefs.getBoolean(getString(R.string.show_all_boards), false);

            if (showAllBoards) {
                loadBoards(false);
                prefs.edit().putBoolean(getString(R.string.show_all_boards), false).apply();
            }
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // Activities containing this fragment must implement its callbacks.
        if (!(context instanceof BoardItemClickListener)) {
            throw new IllegalStateException("Activity must implement fragment's callbacks.");
        }

        mCallbacks = (BoardItemClickListener) context;
    }

    private void showManageBoardsTutorial() {
        final LayoutInflater inflater = LayoutInflater.from(getActivity());
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        final View dialogView = inflater.inflate(R.layout.dialog_manage_boards_tutorial, null, false);
        final CheckBox dontShow = (CheckBox) dialogView.findViewById(R.id.manage_boards_dont_show);

        dialogBuilder.setTitle(R.string.manage_boards)
                .setView(dialogView)
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (getActivity() != null) {
                            final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
                            pref.edit().putBoolean(getString(R.string.show_manage_boards_tutorial), !dontShow.isChecked()).apply();
                        }
                    }
                })
                .show();

    }

    private void showChangeLog() {
        LicensesFragment.displayLicensesFragment(getActivity().getSupportFragmentManager(), R.raw.changelog, "ChangeLog");
    }

    private void setupHeader(final View rootView) {
        showContentButton = (TextView) rootView.findViewById(R.id.board_header_show_content);
        boardOrderText = (TextView) rootView.findViewById(R.id.board_order_subtitle);
        orderTypeList = (ViewGroup) rootView.findViewById(R.id.board_order_content);

        orderByFavorites = (TextView) rootView.findViewById(R.id.board_order_type_favorite);
        orderByName = (TextView) rootView.findViewById(R.id.board_order_type_name);
        orderByTitle = (TextView) rootView.findViewById(R.id.board_order_type_title);
        orderByAccess = (TextView) rootView.findViewById(R.id.board_order_type_access_count);
        orderByLast = (TextView) rootView.findViewById(R.id.board_order_type_last_access);
        orderByPost = (TextView) rootView.findViewById(R.id.board_order_type_post_count);
        orderbyCustom = (TextView) rootView.findViewById(R.id.board_order_type_custom);

        boardOrderBackground = (ViewGroup) rootView.findViewById(R.id.board_order_background);
        boardOrderBackground.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideList(NO_ORDER_SELECTED);
            }
        });

        orderByNames = getResources().getStringArray(R.array.orderbyName);
        final int boardOrder = MimiUtil.getBoardOrder(getActivity());

        showBoardOrderBackground = new AlphaAnimation(0, 1);
        showBoardOrderBackground.setDuration(400);

        revealListAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.board_order_slide_down);
        hideListAnimation = AnimationUtils.loadAnimation(getActivity(), R.anim.board_order_slide_up);

        hideBoardOrderBackground = new AlphaAnimation(1, 0);
        hideBoardOrderBackground.setDuration(400);

        revealListAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                orderTypeList.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        showBoardOrderBackground.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                boardOrderBackground.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        hideListAnimation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                orderTypeList.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        hideBoardOrderBackground.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                boardOrderBackground.setVisibility(View.GONE);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });

        boardOrderContainer = (ViewGroup) rootView.findViewById(R.id.board_order_container);

        boardOrderText.setText(orderByNames[boardOrder]);

        orderByFavorites.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideList(6);
            }
        });

        orderByName.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideList(2);
            }
        });

        orderByTitle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideList(1);
            }
        });

        orderByAccess.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideList(3);
            }
        });

        orderByLast.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideList(5);
            }
        });

        orderByPost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideList(4);
            }
        });

        orderbyCustom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hideList(7);
            }
        });

        boardOrderContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (orderTypeList.getVisibility() == View.VISIBLE) {
                    hideList(NO_ORDER_SELECTED);
                } else {
                    showList();
                }
            }
        });
    }

    public void showList() {
        boardOrderListVisible = true;
        orderTypeList.startAnimation(revealListAnimation);
        boardOrderBackground.startAnimation(showBoardOrderBackground);
        showContentButton.setText(R.string.ic_content_shown);
    }

    public void hideList(final int index) {
        boardOrderListVisible = false;

        if (index >= 0) {
            orderList(index);
            boardOrderText.setText(orderByNames[index]);
        }

        orderTypeList.startAnimation(hideListAnimation);
        boardOrderBackground.startAnimation(hideBoardOrderBackground);
        showContentButton.setText(R.string.ic_content_hidden);
    }

    public void orderList(final int orderType) {
        if (getActivity() != null) {
            MimiUtil.setBoardOrder(getActivity(), orderType);

            RxUtil.safeUnsubscribe(fetchBoardsSubscription);
            fetchBoardsSubscription = BoardTableConnection.fetchBoards(orderType, false)
                    .flatMap(new Func1<List<Board>, Observable<List<ChanBoard>>>() {
                        @Override
                        public Observable<List<ChanBoard>> call(List<Board> boards) {
                            return Observable.just(BoardTableConnection.convertBoardDbModelsToChanBoards(boards));
                        }
                    })
                    .compose(DatabaseUtils.<List<ChanBoard>>applySchedulers())
                    .subscribe(new Action1<List<ChanBoard>>() {
                        @Override
                        public void call(List<ChanBoard> boards) {
                            if (boardListAdapter != null) {
                                boardListAdapter.setBoards(boards);
                            }
                        }
                    });
        }
    }

    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        super.setUserVisibleHint(isVisibleToUser);

        if (isVisibleToUser) {
            if (getActivity() instanceof IToolbarContainer) {
                IToolbarContainer toolbarContainer = (IToolbarContainer) getActivity();
                toolbarContainer.setExpandedToolbar(true, true);
            }
        }
    }

    @Override
    public boolean onBackPressed() {
        if (boardOrderListVisible) {
            hideList(NO_ORDER_SELECTED);
            return true;
        }

        return super.onBackPressed();
    }

    private void setupToolBar() {
        toolbar.setTitle(R.string.app_name);
        toolbar.setSubtitle(null);
        toolbar.getMenu().clear();
        toolbar.inflateMenu(R.menu.board_list);
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.manage_boards_menu) {
                    toolbar.startActionMode(getActionMode());
                }
                return true;
            }
        });
        toolbarSpinner = (Spinner) toolbar.findViewById(R.id.board_spinner);
        if (toolbarSpinner != null) {
            toolbarSpinner.setVisibility(View.GONE);
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (mActivatedPosition != ListView.INVALID_POSITION) {
            // Serialize and persist the activated item position.
            outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
        }
    }

    /**
     * Turns on activate-on-click mode. When this mode is on, list items will be
     * given the 'activated' state when touched.
     */
    public void setActivateOnItemClick(final boolean activateOnItemClick) {
        // When setting CHOICE_MODE_SINGLE, ListView will automatically
        // give items the 'activated' state when touched.

        this.activateOnItemClick = activateOnItemClick;

    }

    public void setBoardsListener(final OnBoardsUpdatedCallback listener) {
        boardsCallback = listener;
    }

    @Override
    public boolean showFab() {
        return true;
    }

    @Override
    public String getTitle() {
        return null;
    }

    @Override
    public String getSubtitle() {
        return null;
    }

    @Override
    public String getPageName() {
        return "board_list";
    }

    @Override
    public int getTabId() {
        return TAB_ID;
    }

    @Override
    public void addContent() {
        showAddBoardDialog();
    }
}
