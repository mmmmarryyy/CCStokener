package com.scoreloop.android.coreui;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import android.app.Activity;
import android.app.ActivityGroup;
import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import com.scoreloop.client.android.core.controller.RequestController;
import com.scoreloop.client.android.core.controller.SocialProviderController;
import com.scoreloop.client.android.core.controller.SocialProviderControllerObserver;
import com.scoreloop.client.android.core.controller.UserController;
import com.scoreloop.client.android.core.controller.UserControllerObserver;
import com.scoreloop.client.android.core.model.Session;
import com.scoreloop.client.android.core.model.SocialProvider;
import com.geekadoo.R;

abstract class BaseActivity extends ActivityGroup {

    private final class SocialConnectObserver implements SocialProviderControllerObserver {

        @Override
        public void socialProviderControllerDidCancel(final SocialProviderController controller) {
            showDialogSafe(DIALOG_ERROR_SOCIAL_USER_CANCEL);
            onSocialProviderConnectionStatusChange(controller.getSocialProvider());
        }

        @Override
        public void socialProviderControllerDidEnterInvalidCredentials(final SocialProviderController controller) {
            showDialogSafe(DIALOG_ERROR_SOCIAL_USER_INVALID);
            onSocialProviderConnectionStatusChange(controller.getSocialProvider());
        }

        @Override
        public void socialProviderControllerDidFail(final SocialProviderController controller, final Throwable error) {
            showDialogSafe(DIALOG_ERROR_SOCIAL_FAILED);
            onSocialProviderConnectionStatusChange(controller.getSocialProvider());
        }

        @Override
        public void socialProviderControllerDidSucceed(final SocialProviderController controller) {
            onSocialProviderConnectionStatusChange(controller.getSocialProvider());
        }
    }

    class GenericListItemAdapter extends ArrayAdapter<ListItem> {

        ImageView image;

        ListItem listItem;

        TextView text0;

        TextView text1;

        TextView text2;

        public GenericListItemAdapter(final Context context, final int resource, final List<ListItem> objects) {
            super(context, resource, objects);
        }

        View init(final int position, View convertView) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.sl_list_item, null);
            }
            listItem = getItem(position);
            text0 = (TextView) convertView.findViewById(R.id.text0);
            text1 = (TextView) convertView.findViewById(R.id.text1);
            text2 = (TextView) convertView.findViewById(R.id.text2);
            image = (ImageView) convertView.findViewById(R.id.image);
            return convertView;
        }
    }

    class UserGenericObserver implements UserControllerObserver {

        @Override
        public void requestControllerDidFail(final RequestController requestController, final Exception exception) {
        }

        @Override
        public void requestControllerDidReceiveResponse(final RequestController requestController) {
        }

        @Override
        public void userControllerDidFailOnEmailAlreadyTaken(final UserController controller) {
        }

        @Override
        public void userControllerDidFailOnInvalidEmailFormat(final UserController controller) {
        }

        @Override
        public void userControllerDidFailOnUsernameAlreadyTaken(final UserController controller) {
        }
    }

    private static Map<String, Drawable> map = Collections.synchronizedMap(new HashMap<String, Drawable>());

    private static Runnable notify;

    static final SimpleDateFormat DEFAULT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    static final int DIALOG_ERROR_NETWORK = 0;

    static final int DIALOG_ERROR_SOCIAL_FAILED = 5;

    static final int DIALOG_ERROR_SOCIAL_USER_CANCEL = 4;

    static final int DIALOG_ERROR_SOCIAL_USER_INVALID = 6;

    static final int DIALOG_ERROR_USER_EMAIL_ALREADY_TAKEN = 1;

    static final int DIALOG_ERROR_USER_INVALID_EMAIL_FORMAT = 2;

    static final int DIALOG_ERROR_USER_NAME_ALREADY_TAKEN = 3;

    static final SocialProvider facebookProvider = SocialProvider.getSocialProviderForIdentifier(SocialProvider.FACEBOOK_IDENTIFIER);

    static final int MENU_GAMES = 2;

    static final int MENU_HIGHSCORES = 0;

    static final int MENU_PROFILE = 1;

    static final SocialProvider myspaceProvider = SocialProvider.getSocialProviderForIdentifier(SocialProvider.MYSPACE_IDENTIFIER);

    static final SocialProvider twitterProvider = SocialProvider.getSocialProviderForIdentifier(SocialProvider.TWITTER_IDENTIFIER);

    private final Handler handler = new Handler();

    private boolean shouldShowDialogs;

    @Override
    public void onWindowFocusChanged(final boolean hasFocus) {
        if (hasFocus) {
            final ImageView imageView = ((ImageView) getTopActivity().findViewById(R.id.progress_indicator));
            if ((imageView != null) && (imageView.getVisibility() == View.VISIBLE)) {
                final AnimationDrawable frameAnimation = (AnimationDrawable) imageView.getBackground();
                frameAnimation.start();
            }
        }
    }

    private Dialog createErrorDialog(final int resId) {
        final Dialog dialog = new Dialog(this);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        final View view = getLayoutInflater().inflate(R.layout.sl_dialog_custom, null);
        dialog.setContentView(view);
        dialog.setCanceledOnTouchOutside(true);
        ((TextView) view.findViewById(R.id.message)).setText(getString(resId));
        return dialog;
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
    }

    @Override
    protected Dialog onCreateDialog(final int id) {
        switch(id) {
            case DIALOG_ERROR_NETWORK:
                return createErrorDialog(R.string.sl_error_message_network);
            case DIALOG_ERROR_USER_EMAIL_ALREADY_TAKEN:
                return createErrorDialog(R.string.sl_error_message_email_already_taken);
            case DIALOG_ERROR_USER_NAME_ALREADY_TAKEN:
                return createErrorDialog(R.string.sl_error_message_name_already_taken);
            case DIALOG_ERROR_USER_INVALID_EMAIL_FORMAT:
                return createErrorDialog(R.string.sl_error_message_invalid_email);
            case DIALOG_ERROR_SOCIAL_USER_CANCEL:
                return createErrorDialog(R.string.sl_error_message_user_cancel);
            case DIALOG_ERROR_SOCIAL_FAILED:
                return createErrorDialog(R.string.sl_error_message_connect_failed);
            case DIALOG_ERROR_SOCIAL_USER_INVALID:
                return createErrorDialog(R.string.sl_error_message_user_invalid);
            default:
                return null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        shouldShowDialogs = false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        shouldShowDialogs = true;
    }

    void connectToSocialProvider(final SocialProvider socialProvider) {
        if (socialProvider.isUserConnected(Session.getCurrentSession().getUser())) {
            onSocialProviderConnectionStatusChange(socialProvider);
        } else {
            final SocialProviderController socialController = SocialProviderController.getSocialProviderController(socialProvider.getIdentifier(), new SocialConnectObserver());
            socialController.connect(this);
        }
    }

    Drawable getDrawable(final String url) {
        if (!map.containsKey(url)) {
            final Drawable drawable = getResources().getDrawable(R.drawable.sl_game_default);
            map.put(url, drawable);
            final Thread thread = new Thread() {

                @Override
                public void run() {
                    try {
                        final DefaultHttpClient httpClient = new DefaultHttpClient();
                        final HttpGet request = new HttpGet(url);
                        final HttpResponse response = httpClient.execute(request);
                        final InputStream inputStream = response.getEntity().getContent();
                        final Drawable drawable = Drawable.createFromStream(inputStream, "src");
                        map.put(url, drawable);
                    } catch (final MalformedURLException e) {
                        map.remove(url);
                    } catch (final IOException e) {
                        map.remove(url);
                    }
                    handler.post(notify);
                }
            };
            thread.start();
        }
        return map.get(url);
    }

    Activity getTopActivity() {
        return (getParent() == null ? this : getParent());
    }

    <T> boolean isEmpty(final T t) {
        return (t == null) || "".equals(t.toString().trim());
    }

    void onSocialProviderConnectionStatusChange(final SocialProvider socialProvider) {
    }

    void setNotify(final Runnable notify) {
        BaseActivity.notify = notify;
    }

    void setProgressIndicator(final boolean visible) {
        final ImageView imageView = ((ImageView) getTopActivity().findViewById(R.id.progress_indicator));
        if (visible) {
            imageView.setVisibility(View.VISIBLE);
        } else {
            imageView.setVisibility(View.INVISIBLE);
        }
    }

    void showDialogSafe(final int res) {
        if (shouldShowDialogs) {
            showDialog(res);
        }
    }

    void showToast(final String message) {
        final View view = getLayoutInflater().inflate(R.layout.sl_dialog_custom, null);
        ((TextView) view.findViewById(R.id.message)).setText(message);
        final Toast toast = new Toast(getApplicationContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(view);
        toast.show();
    }

    void updateHeading(final String heading, final boolean showIcon) {
        ((TextView) findViewById(R.id.heading_text)).setText(heading);
        if (!showIcon) {
            ((ImageView) findViewById(R.id.icon_image)).setVisibility(View.GONE);
        }
    }

    void updateStatusBar() {
        if (Session.getCurrentSession().isAuthenticated()) {
            ((TextView) getTopActivity().findViewById(R.id.login_text)).setText(Session.getCurrentSession().getUser().getLogin());
        }
    }
}
