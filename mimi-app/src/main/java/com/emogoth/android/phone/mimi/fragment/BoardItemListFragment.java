package com.emogoth.android.phone.mimi.fragment;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.inputmethod.EditorInfo;
import android.webkit.WebView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.legacy.widget.Space;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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
import com.emogoth.android.phone.mimi.interfaces.TabInterface;
import com.emogoth.android.phone.mimi.util.AppRater;
import com.emogoth.android.phone.mimi.util.BetterViewAnimator;
import com.emogoth.android.phone.mimi.util.BusProvider;
import com.emogoth.android.phone.mimi.util.HttpClientFactory;
import com.emogoth.android.phone.mimi.util.MimiUtil;
import com.emogoth.android.phone.mimi.util.RequestQueueUtil;
import com.emogoth.android.phone.mimi.util.RxUtil;
import com.emogoth.android.phone.mimi.view.DividerItemDecoration;
import com.google.android.material.snackbar.Snackbar;
import com.mimireader.chanlib.ChanConnector;
import com.mimireader.chanlib.models.ChanBoard;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Function;


/**
 * A list fragment representing a list of PostItems. This fragment
 * also supports tablet devices by allowing list items to be given an
 * 'activated' state upon selection. This helps indicate which item is
 * currently being viewed in a {@link ThreadPagerFragment}.
 * <p/>
 * Activities containing this fragment MUST implement the {@link com.emogoth.android.phone.mimi.interfaces.BoardItemClickListener}
 * interface.
 */
public class BoardItemListFragment extends MimiFragmentBase implements BoardListAdapter.OnBoardClickListener,
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

    private boolean activateOnItemClick;

    /**
     * The current activated item position. Only used on tablets.
     */
    private int mActivatedPosition = ListView.INVALID_POSITION;
    private RecyclerView boardsList;

    private BoardListAdapter boardListAdapter;

    private View rootView;
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
    private View errorView;

    private BetterViewAnimator errorSwitcher;

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

    private Disposable boardInfoSubscription;
    private Disposable fetchBoardsSubscription;
    private Disposable boardFetchDisposable;
    private Disposable initDatabaseDisposable;

    private MenuItem manageBoardsMenuItem;


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

                if (PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(getString(R.string.user_agent_pref), null) == null) {
                    final String defaultAgent = System.getProperty("http.agent");
                    PreferenceManager.getDefaultSharedPreferences(getActivity()).edit().putString(getString(R.string.user_agent_pref), defaultAgent).apply();
                }
            }
        }

        if (getActivity() instanceof MimiActivity) {
            toolbar = ((MimiActivity) getActivity()).getToolbar();
        }

        rootView = inflater.inflate(R.layout.fragment_boards_list, container, false);
        listFooter = inflater.inflate(R.layout.footer_board_list, container, false);

        actionBarSpacer = (Space) rootView.findViewById(R.id.spacer);
        errorSwitcher = rootView.findViewById(R.id.error_switcher);

        boardListAdapter = new BoardListAdapter(getActivity(), new ArrayList<>());

        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        boardsList = rootView.findViewById(R.id.boards_list);
        boardsList.addItemDecoration(new DividerItemDecoration(getActivity(), DividerItemDecoration.LIST_VERTICAL));
        boardsList.setLayoutManager(layoutManager);
        boardsList.setAdapter(boardListAdapter);
        boardListAdapter.setDragListener(viewHolder -> itemTouchHelper.startDrag(viewHolder));
        boardListAdapter.setOnItemLongClickListener((parent, view, position, id) -> {
            if (getActivity() != null) {

                final MimiActivity activity = ((MimiActivity) getActivity());
                activity.getToolbar().startActionMode(getActionMode());
            }
            return false;
        });
        return rootView;
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
                    boardListAdapter.setOnBoardClickListener(null);

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
                    if (getActivity() == null || !(getActivity() instanceof MimiActivity)) {
                        return;
                    }

                    MimiActivity activity = (MimiActivity) getActivity();

                    editMode = false;

                    activity.getDrawerLayout().setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
                    toolbar.setVisibility(View.VISIBLE);

                    boardListAdapter.editMode(false);
                    boardsList.setClickable(true);
                    boardListAdapter.setOnBoardClickListener(BoardItemListFragment.this);

                    boardOrderContainer.setVisibility(View.VISIBLE);
                    actionBarSpacer.setVisibility(View.VISIBLE);

                    final int order = MimiUtil.getBoardOrder(getActivity());
                    boardOrderText.setText(orderByNames[order]);

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
        alertBuilder.setPositiveButton(R.string.add, (dialog, which) -> addBoard(input.getText().toString()));
        alertBuilder.setNegativeButton(R.string.cancel, (dialog, which) -> Log.v(LOG_TAG, "Cancelled adding a board"));

        alertBuilder.setTitle(R.string.add_board);

        final AlertDialog d = alertBuilder.create();
        d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        d.show();

        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                addBoard(input.getText().toString());
                d.dismiss();
            }
            return true;
        });
    }

    private void addBoard(String rawBoardName) {
        if (TextUtils.isEmpty(rawBoardName)) {
            return;
        }

        errorSwitcher.setDisplayedChildId(boardsList.getId());

        RxUtil.safeUnsubscribe(boardInfoSubscription);
        String boardName = rawBoardName.replaceAll("/", "").toLowerCase().trim();
        boardInfoSubscription = BoardTableConnection.fetchBoard(boardName)
                .flatMap((Function<ChanBoard, Flowable<ChanBoard>>) chanBoard -> BoardTableConnection.setBoardVisibility(chanBoard.getName(), true))
                .flatMap((Function<ChanBoard, Flowable<List<Board>>>) success -> {
                    final int orderId = MimiUtil.getBoardOrder(getActivity());
                    return BoardTableConnection.fetchBoards(orderId);
                })
                .flatMap((Function<List<Board>, Flowable<List<ChanBoard>>>) boards -> Flowable.just(BoardTableConnection.convertBoardDbModelsToChanBoards(boards)))
                .onErrorReturn(throwable -> null)
                .compose(DatabaseUtils.applySchedulers())
                .subscribe(boards -> {
                    if (boards != null && boards.size() > 0) {
                        boardListAdapter.setBoards(boards);

                        if (manageBoardsMenuItem != null) {
                            manageBoardsMenuItem.setEnabled(true);
                        }

                    } else {
                        showError();
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
                .setEndpoint(FourChanConnector.getDefaultEndpoint())
                .setClient(HttpClientFactory.getInstance().getClient())
                .setPostEndpoint(FourChanConnector.getDefaultPostEndpoint())
                .build();

        boardListAdapter.setOnBoardClickListener(this);

        initDatabase();

        // Restore the previously serialized activated item position.
//        if (savedInstanceState != null
//                && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
////            setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
//        }

    }

    private void initDatabase() {
        RxUtil.safeUnsubscribe(initDatabaseDisposable);
        initDatabaseDisposable = BoardTableConnection.fetchBoards(MimiUtil.getBoardOrder(getActivity()))
                .map(BoardTableConnection::convertBoardDbModelsToChanBoards)
                .flatMap((Function<List<ChanBoard>, Flowable<List<ChanBoard>>>) chanBoards -> {
                    if (chanBoards == null || chanBoards.size() == 0) {
                        return BoardTableConnection.initDefaultBoards(getActivity());
                    } else {
                        return Flowable.just(chanBoards);
                    }
                })
                .subscribe(chanBoards -> {
                    watchDatabase();
                    loadBoards();
                }, throwable -> showError());
    }

    private void loadBoards() {
        RxUtil.safeUnsubscribe(boardFetchDisposable);
        boardFetchDisposable = chanConnector.fetchBoards()
                .doOnNext(BoardTableConnection.saveBoards())
                .compose(DatabaseUtils.applySchedulers())
                .subscribe();

    }

    private void watchDatabase() {
        errorSwitcher.setDisplayedChildId(boardsList.getId());
        RxUtil.safeUnsubscribe(fetchBoardsSubscription);
        fetchBoardsSubscription = BoardTableConnection.observeBoards(MimiUtil.getBoardOrder(getActivity()))
                .onErrorReturn(throwable -> new ArrayList<>())
                .subscribe(chanBoards -> {
                    if (manageBoardsMenuItem != null) {
                        manageBoardsMenuItem.setEnabled(true);
                    }

                    if (boardsList != null) {
                        boardOrderContainer.setVisibility(View.VISIBLE);
                        boardListAdapter.setBoards(chanBoards);
                    }
                });
    }

    private void showError() {
        if (manageBoardsMenuItem != null) {
            manageBoardsMenuItem.setEnabled(false);
        }

        if (errorView != null) {
            errorSwitcher.setDisplayedChildId(errorView.getId());
            return;
        }

        ViewStub errorStub = rootView.findViewById(R.id.error_container);
        errorStub.setOnInflateListener((viewStub, view) -> {
            view.findViewById(R.id.retry_button).setOnClickListener(view1 -> loadBoards());
            errorSwitcher.setDisplayedChildId(view.getId());
            errorView = view;
        });
        errorStub.inflate();

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
    public void onBoardClick(ChanBoard board) {

        if (board == null) {
            if (boardsList != null) {
                Snackbar.make(boardsList, R.string.error_occurred, Snackbar.LENGTH_LONG).show();
            }

            return;
        }

        BoardTableConnection.incrementAccessCount(board.getName())
                .compose(DatabaseUtils.applySchedulers())
                .subscribe();
        mCallbacks.onBoardItemClick(board, true);

    }

    @Override
    public void initMenu() {
        super.initMenu();

        if (toolbar != null) {
            setupToolBar();

            if (getActivity() != null) {
                getActivity().supportInvalidateOptionsMenu();
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        BusProvider.getInstance().unregister(this);

        RxUtil.safeUnsubscribe(initDatabaseDisposable);
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
                loadBoards();
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
        final CheckBox dontShow = dialogView.findViewById(R.id.manage_boards_dont_show);

        dialogBuilder.setTitle(R.string.manage_boards)
                .setView(dialogView)
                .setPositiveButton(R.string.ok, (dialog, which) -> {
                    if (getActivity() != null) {
                        final SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(getActivity());
                        pref.edit().putBoolean(getString(R.string.show_manage_boards_tutorial), !dontShow.isChecked()).apply();
                    }
                })
                .show();

    }

    private void showChangeLog() {
        LicensesFragment.displayLicensesFragment(getActivity().getSupportFragmentManager(), R.raw.changelog, "ChangeLog");
    }

    private void setupHeader(final View rootView) {
        showContentButton = rootView.findViewById(R.id.board_header_show_content);
        boardOrderText = rootView.findViewById(R.id.board_order_subtitle);
        orderTypeList = rootView.findViewById(R.id.board_order_content);

        orderByFavorites = rootView.findViewById(R.id.board_order_type_favorite);
        orderByName = rootView.findViewById(R.id.board_order_type_name);
        orderByTitle = rootView.findViewById(R.id.board_order_type_title);
        orderByAccess = rootView.findViewById(R.id.board_order_type_access_count);
        orderByLast = rootView.findViewById(R.id.board_order_type_last_access);
        orderByPost = rootView.findViewById(R.id.board_order_type_post_count);
        orderbyCustom = rootView.findViewById(R.id.board_order_type_custom);

        boardOrderBackground = rootView.findViewById(R.id.board_order_background);
        boardOrderBackground.setOnClickListener(v -> hideList(NO_ORDER_SELECTED));

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

        boardOrderContainer = rootView.findViewById(R.id.board_order_container);

        boardOrderText.setText(orderByNames[boardOrder]);

        orderByFavorites.setOnClickListener(v -> hideList(6));

        orderByName.setOnClickListener(v -> hideList(2));

        orderByTitle.setOnClickListener(v -> hideList(1));

        orderByAccess.setOnClickListener(v -> hideList(3));

        orderByLast.setOnClickListener(v -> hideList(5));

        orderByPost.setOnClickListener(v -> hideList(4));

        orderbyCustom.setOnClickListener(v -> hideList(7));

        boardOrderContainer.setOnClickListener(v -> {
            if (orderTypeList.getVisibility() == View.VISIBLE) {
                hideList(NO_ORDER_SELECTED);
            } else {
                showList();
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

            errorSwitcher.setDisplayedChildId(boardsList.getId());

            RxUtil.safeUnsubscribe(fetchBoardsSubscription);
            fetchBoardsSubscription = BoardTableConnection.fetchBoards(orderType)
                    .flatMap((Function<List<Board>, Flowable<List<ChanBoard>>>) boards -> Flowable.just(BoardTableConnection.convertBoardDbModelsToChanBoards(boards)))
                    .compose(DatabaseUtils.applySchedulers())
                    .subscribe(orderedBoards -> {
                        if (orderedBoards.size() > 0) {
                            updateBoardsAdapter(orderedBoards);
                        }
                    });
        }
    }

    private void updateBoardsAdapter(List<ChanBoard> updatedBoards) {
        if (boardListAdapter != null) {
            boardListAdapter.setBoards(updatedBoards);
        } else if (getActivity() != null) {
            boardListAdapter = new BoardListAdapter(getActivity(), updatedBoards);
        }

        if (manageBoardsMenuItem != null) {
            manageBoardsMenuItem.setEnabled(true);
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
        if (getActivity() instanceof MimiActivity) {
            MimiActivity activity = (MimiActivity) getActivity();
            activity.getSupportActionBar().setTitle(R.string.app_name);
            activity.getSupportActionBar().setSubtitle(null);
        }

        toolbarSpinner = toolbar.findViewById(R.id.board_spinner);
        if (toolbarSpinner != null) {
            toolbarSpinner.setVisibility(View.GONE);
        }
    }

    @Override
    public int getMenuRes() {
        return R.menu.board_list;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(getMenuRes(), menu);
        manageBoardsMenuItem = menu.findItem(R.id.manage_boards_menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.manage_boards_menu) {
            if (getActivity() != null) {
                final MimiActivity activity = ((MimiActivity) getActivity());
                activity.getToolbar().startActionMode(getActionMode());
            }
        }
        return true;
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
