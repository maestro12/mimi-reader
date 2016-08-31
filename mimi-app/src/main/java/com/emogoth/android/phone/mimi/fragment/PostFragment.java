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

import android.Manifest;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatEditText;
import android.support.v7.widget.AppCompatTextView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.ImageView;
import android.widget.ViewSwitcher;

import com.emogoth.android.phone.mimi.R;
import com.emogoth.android.phone.mimi.activity.GalleryActivity;
import com.emogoth.android.phone.mimi.activity.MimiActivity;
import com.emogoth.android.phone.mimi.app.MimiApplication;
import com.emogoth.android.phone.mimi.db.BoardTableConnection;
import com.emogoth.android.phone.mimi.db.DatabaseUtils;
import com.emogoth.android.phone.mimi.db.HistoryTableConnection;
import com.emogoth.android.phone.mimi.dialog.CaptchaDialog;
import com.emogoth.android.phone.mimi.event.UpdateHistoryEvent;
import com.emogoth.android.phone.mimi.fourchan.FourChanCommentParser;
import com.emogoth.android.phone.mimi.fourchan.FourChanConnector;
import com.emogoth.android.phone.mimi.interfaces.OnRecaptchaResponseCallback;
import com.emogoth.android.phone.mimi.util.BusProvider;
import com.emogoth.android.phone.mimi.util.Extras;
import com.emogoth.android.phone.mimi.util.HttpClientFactory;
import com.emogoth.android.phone.mimi.util.MimiUtil;
import com.emogoth.android.phone.mimi.util.PostUtil;
import com.emogoth.android.phone.mimi.util.RxUtil;
import com.emogoth.android.phone.mimi.view.IconTextView;
import com.mimireader.chanlib.ChanConnector;
import com.mimireader.chanlib.models.ChanBoard;
import com.mimireader.chanlib.models.ChanPost;

import java.io.File;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.adapter.rxjava.HttpException;
import rx.Observable;
import rx.Subscription;
import rx.functions.Action1;
import rx.functions.Func1;


public class PostFragment extends DialogFragment {
    private static final String LOG_TAG = PostFragment.class.getSimpleName();

    private static final String EXTRA_NAME = "name";
    private static final String EXTRA_COMMENT = "comment";
    private static final String EXTRA_SUBJECT = "subject";
    private static final String EXTRA_EMAIL = "email";
    private static final String EXTRA_FILE = "file";

    public static final int PICK_IMAGE = 2;
    private static final String CAPTCHA_DIALOG_TAG = "captcha_dialog";

    private int threadId;
    private String boardName;
    private String postUrl;
    private String boardTitle;

    private ViewGroup shrinkFormContainer;
    //    private ViewGroup headerContainer;
    private AppCompatEditText commentField;
    private AppCompatTextView nameText;

    private AppCompatEditText nameInput;
    private AppCompatEditText optionsInput;
    private AppCompatEditText subjectInput;

    private ViewSwitcher replyFormViewSwitcher;
    private ImageView editUserInfoButton;
    private AppCompatTextView doneEditingButton;

    private String name = "";
    private String comment;
    private String email = "";
    private String subject = "";
    private boolean isEditMode = false;

    public String captchaChallenge = null;
    public String captchaVerification;
    private ImageView attachedImage;
    private String imagePath;
    private PostListener postListener;
    private boolean isLightTheme = false;
    private boolean isNewPost = false;
    private boolean captchaRequired = true;
    private IconTextView clearImage;
    private ViewGroup attachedImageContainer;
    private AppCompatTextView attachImageButton;
    private AppCompatTextView sageButton;
    private boolean formCleared = false;
    private View sendButton;
    private View cancelButton;

    private Subscription boardInfoSubscription;

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Dialog d = super.onCreateDialog(savedInstanceState);
        d.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        d.requestWindowFeature(Window.FEATURE_NO_TITLE);
        return d;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        if (MimiUtil.getInstance().getThemeResourceId() == R.style.Theme_Mimi_Light) {
            isLightTheme = true;
        }

        if (savedInstanceState != null) {
            if (!formCleared) {
                extractExtras(savedInstanceState);
            } else {
                formCleared = false;
            }
        } else {
            extractExtras(getArguments());
        }

        final View v = inflater.inflate(R.layout.reply_form_layout, container, false);
        sendButton = v.findViewById(R.id.submit_button);
        cancelButton = v.findViewById(R.id.cancel_button);
        return v;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendPost();
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelPost();
            }
        });

        nameText = (AppCompatTextView) view.findViewById(R.id.edit_user_info);
        nameText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                replyFormViewSwitcher.setDisplayedChild(1);
            }
        });
        if (!TextUtils.isEmpty(name)) {
            nameText.setText(name);
        } else {
            nameText.setText("Anonymous");
        }

        nameInput = (AppCompatEditText) view.findViewById(R.id.name_input);
        if (!TextUtils.isEmpty(name)) {
            nameInput.setText(name);
        }

        optionsInput = (AppCompatEditText) view.findViewById(R.id.options_input);
        if (!TextUtils.isEmpty(email)) {
            optionsInput.setText(email);
        }

        sageButton = (AppCompatTextView) view.findViewById(R.id.sage_info);
        sageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (optionsInput != null) {
                    optionsInput.setText("sage");
                }
            }
        });

        subjectInput = (AppCompatEditText) view.findViewById(R.id.subject_input);
        if (!TextUtils.isEmpty(subject)) {
            subjectInput.setText(subject);
        }

        replyFormViewSwitcher = (ViewSwitcher) view.findViewById(R.id.reply_form_switcher);
        replyFormViewSwitcher.setInAnimation(getActivity(), R.anim.abc_fade_in);
        replyFormViewSwitcher.setOutAnimation(getActivity(), R.anim.abc_fade_out);
        replyFormViewSwitcher.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.i(LOG_TAG, "form clicked");
            }
        });

        doneEditingButton = (AppCompatTextView) view.findViewById(R.id.done_info);
        doneEditingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!TextUtils.isEmpty(nameInput.getText().toString())) {
                    nameText.setText(nameInput.getText().toString());
                } else {
                    nameText.setText("Anonymous");
                }

                setEmail(optionsInput.getText().toString());
                setName(nameInput.getText().toString());
                setSubject(subjectInput.getText().toString());

                replyFormViewSwitcher.setDisplayedChild(0);
            }
        });

        commentField = (AppCompatEditText) view.findViewById(R.id.comment_input);
        commentField.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                commentField.post(new Runnable() {
                    @Override
                    public void run() {
                        if (getActivity() != null) {
                            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                            imm.showSoftInput(commentField, InputMethodManager.SHOW_IMPLICIT);
                        }
                    }
                });
            }
        });
        commentField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                Log.d(LOG_TAG, "before text changed");
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        commentField.requestFocus();

        if (!TextUtils.isEmpty(comment)) {
            commentField.setText(comment);
            commentField.setSelection(comment.length());
        }

        attachedImage = (ImageView) view.findViewById(R.id.attached_image);

        clearImage = (IconTextView) view.findViewById(R.id.clear_image);
        clearImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clearAttachedImage();
            }
        });

        attachedImageContainer = (ViewGroup) view.findViewById(R.id.image_container);
        attachedImageContainer.setVisibility(View.GONE);

        attachImageButton = (AppCompatTextView) view.findViewById(R.id.attach_image_button);
        attachImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (ContextCompat.checkSelfPermission(getActivity(),
                        Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        != PackageManager.PERMISSION_GRANTED) {

                    // Should we show an explanation?
                    if (ActivityCompat.shouldShowRequestPermissionRationale(getActivity(),
                            Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                        Snackbar.make(v, R.string.app_needs_your_permission_to_attach, Snackbar.LENGTH_LONG).show();
                    } else {
                        ActivityCompat.requestPermissions(getActivity(),
                                new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                GalleryActivity.PERMISSIONS_REQUEST_EXTERNAL_STORAGE);
                    }
                } else {
                    pickImage();
                }
            }
        });

        if (MimiUtil.getInstance().isLoggedIn()) {
            disableCaptcha();
        } else {
            enableCaptcha();
        }
    }

    private void pickImage() {
        Intent intent = new Intent();
        intent.setType("*/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            String[] mimetypes = {"image/*", "video/*"};
            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
        }

        if (getActivity() != null && getActivity() instanceof MimiActivity) {
//                    getParentFragment().startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE);
            ((MimiActivity) getActivity()).setResultFragment(getParentFragment());
            getActivity().startActivityForResult(Intent.createChooser(intent, getString(R.string.select_picture)), PICK_IMAGE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    private void clearAttachedImage() {
        attachedImageContainer.setVisibility(View.GONE);
        attachImageButton.setVisibility(View.VISIBLE);
    }

    private void disableCaptcha() {
        captchaRequired = false;
    }

    private void enableCaptcha() {
        captchaRequired = true;
    }

    private void processResponse(Response<ResponseBody> response) {
        String html;
        try {
            if (response == null) {
                html = MimiApplication.getInstance().getString(R.string.empty_response_error);
                throw new Exception(html);
            } else if (response.isSuccessful()) {
                html = response.body().string();
            } else {
                html = response.errorBody().string();
            }
        } catch (Exception e) {
            if (postListener != null) {
                postListener.onError(e);
            }
            Log.e(LOG_TAG, "Error processing response", e);

            return;
        }

        final int i = html.indexOf("errmsg");
        if (i > 0) {
            final int msgStartIndex = html.indexOf(">", i) + 1;
            final int brIndex = html.indexOf("br", i);
            final int spanIndex = html.indexOf("</span", i);
            final int msgEndIndex = (spanIndex < brIndex) ? spanIndex : brIndex - 1;

            final String errorMsg = html.substring(msgStartIndex, msgEndIndex);

            Log.i(LOG_TAG, errorMsg);

            if (postListener != null) {
                final HttpException exception = new HttpException(response);
                postListener.onError(new Exception(errorMsg, exception));
            }
        } else {
            String postId = null;
            int index = html.indexOf("no:");
            if (index >= 0) {
                postId = html.substring(index + 3, html.lastIndexOf(" "));
            } else {
                index = html.indexOf("thread:");
                if (index >= 0) {
                    postId = html.substring(index + 3, html.lastIndexOf(" "));
                }
            }

            if (postId != null && isNewPost) {
                final String fileName = MimiUtil.removeExtention(imagePath);
                final String fileExt = imagePath.substring(imagePath.lastIndexOf("."));
                final String name = TextUtils.isEmpty(getName()) ? "Anonymous" : getName();
                final ChanPost firstPost = new ChanPost();
                final FourChanCommentParser.Builder parserBuilder = new FourChanCommentParser.Builder();

                parserBuilder.setContext(getActivity())
                        .setComment(getComment())
                        .setQuoteColor(MimiUtil.getInstance().getQuoteColor())
                        .setReplyColor(MimiUtil.getInstance().getReplyColor())
                        .setHighlightColor(MimiUtil.getInstance().getHighlightColor())
                        .setLinkColor(MimiUtil.getInstance().getLinkColor());

                firstPost.setName(name);
                firstPost.setTim(Calendar.getInstance(Locale.getDefault()).getTime().toString());
                firstPost.setCom(getComment());
                firstPost.setComment(parserBuilder.build().parse());
                firstPost.setEmail(getEmail());
                firstPost.setSub(getSubject());
                firstPost.setSubject(getSubject());
                firstPost.setNo(Integer.valueOf(postId));
                firstPost.setFilename(fileName);
                firstPost.setExt(fileExt);

                HistoryTableConnection.putHistory(boardName, firstPost, 1, true).subscribe();
            }

            final UpdateHistoryEvent event = new UpdateHistoryEvent();
            event.setBoardName(boardName);
            event.setThreadId(threadId);
            BusProvider.getInstance().post(event);

            BoardTableConnection.incrementPostCount(boardName).subscribe();

            clearForm();

            if (postListener != null) {
                postListener.onSuccess(postId);
            }
        }
    }

    private Action1<Response<ResponseBody>> onPostComplete() {
        return new Action1<Response<ResponseBody>>() {
            @Override
            public void call(Response<ResponseBody> responseBodyResponse) {
                processResponse(responseBodyResponse);
            }
        };
    }

    private Action1<Throwable> onPostFail() {
        return new Action1<Throwable>() {
            @Override
            public void call(Throwable throwable) {
                if (postListener != null) {
                    postListener.onError(throwable);
                }
            }
        };
    }

    public void post() {

        final ChanConnector chanConnector = new FourChanConnector.Builder()
                .setEndpoint(FourChanConnector.getDefaultEndpoint(MimiUtil.isSecureConnection(getActivity())))
                .setPostEndpoint(FourChanConnector.getDefaultPostEndpoint())
                .setClient(HttpClientFactory.getInstance().getOkHttpClient())
                .build();

        if (captchaRequired && captchaChallenge == null) {
            final Exception e = new Exception("Could not authorize with reCaptcha");
            if (postListener != null) {
                postListener.onError(e);
            }

            return;
        }

        final File imageLocation;
        if (TextUtils.isEmpty(imagePath)) {
            imageLocation = null;
        } else {
            imageLocation = new File(imagePath);
        }

        RxUtil.safeUnsubscribe(boardInfoSubscription);
        boardInfoSubscription = BoardTableConnection.fetchBoard(boardName)
                .map(new Func1<ChanBoard, Map<String, Object>>() {
                    @Override
                    public Map<String, Object> call(ChanBoard chanBoard) {
                        final Map<String, Object> params = new HashMap<>();
                        params.put("name", name);
                        params.put("email", email);
                        params.put("subject", subject);
                        params.put("com", comment);
                        params.put("MAX_FILE_SIZE", chanBoard.getMaxFilesize());
                        params.put("pwd", "password");
                        params.put("mode", "regist");

                        if (threadId > 0) {
                            params.put("resto", String.valueOf(threadId));
                        }

                        if (captchaRequired) {
                            params.put("g-recaptcha-response", captchaChallenge);
                        }

                        if (imageLocation != null) {
                            params.put("upfile", imageLocation);
                        }
                        return params;
                    }
                })
                .flatMap(new Func1<Map<String, Object>, Observable<Response<ResponseBody>>>() {
                    @Override
                    public Observable<Response<ResponseBody>> call(Map<String, Object> params) {
                        return chanConnector.post(boardName, params);
                    }
                })
                .compose(DatabaseUtils.<Response<ResponseBody>>applySchedulers())
                .subscribe(onPostComplete(), onPostFail());
    }

    private void saveFormData() {
        comment = commentField.getText().toString();
        subject = subjectInput.getText().toString();
        email = optionsInput.getText().toString();
    }

    private void clearForm() {
        formCleared = true;
        nameInput.setText(null);
        commentField.setText(null);
        subjectInput.setText(null);
        optionsInput.setText(null);

        name = null;
        comment = null;
        subject = null;
        email = null;
        imagePath = null;

        clearAttachedImage();
    }

    @Override
    public void onResume() {
        super.onResume();

        int deviceWidth = getResources().getDisplayMetrics().widthPixels;
        getDialog().getWindow().setLayout(deviceWidth - 4, ViewGroup.LayoutParams.WRAP_CONTENT);

        if (MimiUtil.getInstance().isLoggedIn()) {
            disableCaptcha();
        } else {
            enableCaptcha();
        }

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(Extras.EXTRAS_THREAD_ID, threadId);
        outState.putString(Extras.EXTRAS_BOARD_NAME, boardName);
        outState.putString(Extras.EXTRAS_BOARD_TITLE, boardTitle);

        outState.putString(Extras.EXTRAS_POST_COMMENT, comment);
        outState.putString(EXTRA_SUBJECT, subject);
        outState.putString(EXTRA_EMAIL, email);
        outState.putString(EXTRA_NAME, name);
        outState.putString(EXTRA_FILE, imagePath);

        outState.putBoolean(Extras.EXTRAS_POST_NEW, isNewPost);

        super.onSaveInstanceState(outState);
    }

    private void extractExtras(final Bundle bundle) {
        if (bundle.containsKey(Extras.EXTRAS_THREAD_ID)) {
            threadId = bundle.getInt(Extras.EXTRAS_THREAD_ID);
        } else {
            isNewPost = true;
        }
        if (bundle.containsKey(Extras.EXTRAS_BOARD_NAME)) {
            boardName = bundle.getString(Extras.EXTRAS_BOARD_NAME);
            if (getActivity() != null) {
                final String url = getResources().getString(R.string.sys_link);
                postUrl = "https://" + url + "/" + boardName + "/post";
            }
        }
        if (bundle.containsKey(Extras.EXTRAS_BOARD_TITLE)) {
            boardTitle = bundle.getString(Extras.EXTRAS_BOARD_TITLE);
        }
        if (bundle.containsKey(Extras.EXTRAS_POST_COMMENT) && TextUtils.isEmpty(comment)) {
            this.comment = bundle.getString(Extras.EXTRAS_POST_COMMENT);
        }
        if (bundle.containsKey(EXTRA_SUBJECT)) {
            subject = bundle.getString(EXTRA_SUBJECT);
        }
        if (bundle.containsKey(EXTRA_EMAIL)) {
            email = bundle.getString(EXTRA_EMAIL);
        }
        if (bundle.containsKey(EXTRA_NAME)) {
            name = bundle.getString(EXTRA_NAME);
        }
        if (bundle.containsKey(Extras.EXTRAS_POST_NEW)) {
            isNewPost = bundle.getBoolean(Extras.EXTRAS_POST_NEW);
        }
        if (bundle.containsKey(EXTRA_FILE)) {
            imagePath = bundle.getString(EXTRA_FILE);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PICK_IMAGE && data != null) {
            final Uri imageUri = data.getData();
            imagePath = PostUtil.getPath(getActivity(), imageUri);

            if (attachedImage != null && imagePath != null) {
                attachedImage.setImageURI(imageUri);
                attachedImageContainer.setVisibility(View.VISIBLE);
                attachImageButton.setVisibility(View.GONE);
            }
        }
    }

    private void enableEditMode(final boolean enabled) {
        if (isEditMode != enabled) {
//            if (enabled) {
//                headerContainer.setVisibility(View.GONE);
//            } else {
//                headerContainer.setVisibility(View.VISIBLE);
//            }

            isEditMode = enabled;
        }
    }

    public String getName() {
        if (name == null && nameInput != null) {
            name = nameInput.getText().toString();
        }

        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getComment() {
        if (TextUtils.isEmpty(comment) && commentField != null) {
            comment = commentField.getText().toString();
        }
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getEmail() {
        if (email == null && optionsInput != null) {
            email = optionsInput.getText().toString();
        }

        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setPostListener(final PostListener listener) {
        postListener = listener;
    }

    private void cancelPost() {
        clearForm();
        if (postListener != null) {
            postListener.onCanceled();
        } else if (getActivity() != null) {
            getActivity().onBackPressed();
        }
    }

    private void sendPost() {
//        setEmail(optionsInput.getText().toString());
//        setName(nameInput.getText().toString());
//        setSubject(subjectInput.getText().toString());
//        setComment(commentField.getText().toString());

        saveFormData();

        if (captchaRequired) {
            final CaptchaDialog dialog = new CaptchaDialog();
            dialog.setOnRecaptchaResponseCallback(new OnRecaptchaResponseCallback() {
                @Override
                public void onResponse(String response) {
                    dialog.dismiss();
                    dismiss();

                    captchaChallenge = response;

                    if (getActivity() != null) {
                        if (postListener != null) {
                            postListener.onStartPost();
                        }

                        post();
                    }
                }
            });

            dialog.show(getActivity().getSupportFragmentManager(), CAPTCHA_DIALOG_TAG);
        } else {
            if (getActivity() != null) {
                if (postListener != null) {
                    postListener.onStartPost();
                }
                post();
            }
        }
    }

    public interface PostListener {
        void onCanceled();

        void onStartPost();

        void onSuccess(final String postId);

        void onError(final Throwable error);
    }
}