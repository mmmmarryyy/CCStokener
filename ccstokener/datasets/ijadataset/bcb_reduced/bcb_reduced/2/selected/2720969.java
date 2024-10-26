package com.echodrama.upturner;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import com.echodrama.upturner.R;
import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class GameResultDialog extends Dialog {

    private int gameScore;

    private boolean canbeLocalRecord = false;

    private ScoreGroupModel localScoreGroupModel = new ScoreGroupModel();

    private Resources res;

    private int result;

    public GameResultDialog(Context context, int pGameScore) {
        super(context);
        this.gameScore = pGameScore;
        this.res = context.getResources();
        setContentView(R.layout.gameresult);
        buildDialog();
        renderDialog();
    }

    protected void buildDialog() {
        final EditText gameresultUsernameValueEditText = (EditText) findViewById(R.id.gameresultUsernameValue);
        final Button gameresultUsernamePostBtn = (Button) findViewById(R.id.gameresultUsernamePostBtn);
        gameresultUsernamePostBtn.setOnClickListener(new android.view.View.OnClickListener() {

            @Override
            public void onClick(View v) {
                String value = gameresultUsernameValueEditText.getText().toString();
                if (value == null || value.trim().equals("")) {
                    Toast.makeText(getContext(), res.getString(R.string.message_username_empty), Toast.LENGTH_SHORT).show();
                    return;
                }
                value = value.trim();
                int valueLength = value.length();
                if (valueLength > 20) {
                    Toast.makeText(getContext(), res.getString(R.string.message_username_extendmaxlength), Toast.LENGTH_SHORT).show();
                    return;
                }
                value = value.substring(0, valueLength > 20 ? 20 : valueLength);
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
                String dateStr = format.format(new Date());
                ScoreItemModel itemModel = new ScoreItemModel(value, "" + gameScore, dateStr, true);
                localScoreGroupModel.addItem(itemModel);
                SharedPreferences preferences = getContext().getSharedPreferences(Constants.PROPERTIES, Context.MODE_WORLD_WRITEABLE);
                SharedPreferences.Editor editor = preferences.edit();
                for (int i = 0; i < localScoreGroupModel.getItemCount(); i++) {
                    ScoreItemModel localScoreItemModel = localScoreGroupModel.getItem(i);
                    editor.putString(Constants.PROPERTIES_GAMEBEST_USERNAME + i, localScoreItemModel.getUsername());
                    editor.putString(Constants.PROPERTIES_GAMEBEST_SCORE + i, localScoreItemModel.getScore());
                    editor.putString(Constants.PROPERTIES_GAMEBEST_DATE + i, localScoreItemModel.getDate());
                }
                editor.commit();
                if (itemModel.equals(localScoreGroupModel.getItem(0))) {
                    String httpUrl = Constants.SERVER_URL + "/rollingcard.php?op=addnewscore&username=" + itemModel.getUsername() + "&score=" + itemModel.getScore();
                    HttpGet request = new HttpGet(httpUrl);
                    HttpClient httpClient = new DefaultHttpClient();
                    try {
                        HttpResponse response = httpClient.execute(request);
                        if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                            Toast.makeText(getContext(), res.getString(R.string.message_addnewscore_success), Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getContext(), res.getString(R.string.message_networkerror), Toast.LENGTH_SHORT).show();
                        }
                    } catch (ClientProtocolException e) {
                        Toast.makeText(getContext(), res.getString(R.string.message_networkerror), Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    } catch (IOException e) {
                        Toast.makeText(getContext(), res.getString(R.string.message_networkerror), Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                }
                renderLayoutById(R.id.gameresultScoreLayout);
            }
        });
        final Button gameresultUsernameCancelBtn = (Button) findViewById(R.id.gameresultUsernameCancelBtn);
        gameresultUsernameCancelBtn.setOnClickListener(new android.view.View.OnClickListener() {

            @Override
            public void onClick(View v) {
                renderLayoutById(R.id.gameresultScoreLayout);
            }
        });
        final Button gameresultRestartBtn = (Button) findViewById(R.id.gameresultRestartBtn);
        gameresultRestartBtn.setOnClickListener(new android.view.View.OnClickListener() {

            @Override
            public void onClick(View v) {
                result = Constants.RESULT_GAME_RESTART;
                dismiss();
            }
        });
        final Button gameresultCloseBtn = (Button) findViewById(R.id.gameresultExitBtn);
        gameresultCloseBtn.setOnClickListener(new android.view.View.OnClickListener() {

            @Override
            public void onClick(View v) {
                result = Constants.RESULT_GAME_CLOSE;
                dismiss();
            }
        });
    }

    protected void renderDialog() {
        boolean noLocalRecord = true;
        SharedPreferences preferences = getContext().getSharedPreferences(Constants.PROPERTIES, Context.MODE_WORLD_READABLE);
        for (int i = 0; i < ScoreGroupModel.MAX_ITEM_NUM; i++) {
            String username = preferences.getString(Constants.PROPERTIES_GAMEBEST_USERNAME + i, "");
            if (username != null && !username.equals("")) {
                noLocalRecord = false;
                String gameBestScore = preferences.getString(Constants.PROPERTIES_GAMEBEST_SCORE + i, "");
                String gameBestDate = preferences.getString(Constants.PROPERTIES_GAMEBEST_DATE + i, "");
                ScoreItemModel itemModel = new ScoreItemModel(username, gameBestScore, gameBestDate);
                this.localScoreGroupModel.addItem(itemModel);
                if (this.gameScore > Integer.parseInt(gameBestScore)) {
                    this.canbeLocalRecord = true;
                }
            }
        }
        if (noLocalRecord && this.gameScore > 0) {
            this.canbeLocalRecord = true;
        }
        if (this.canbeLocalRecord) {
            this.setTitle(R.string.gameresult_title_canbelocalrecord_yes);
            renderLayoutById(R.id.gameresultUsernameInputLayout);
        } else {
            this.setTitle(R.string.gameresult_title_canbelocalrecord_no);
            renderLayoutById(R.id.gameresultScoreLayout);
        }
        final TextView gameResultScoreValue = (TextView) findViewById(R.id.gameresultScoreValue);
        gameResultScoreValue.setText("" + gameScore);
    }

    private void renderLayoutById(int id) {
        LinearLayout gameresultUsernameInputLayout = (LinearLayout) findViewById(R.id.gameresultUsernameInputLayout);
        LinearLayout gameresultScoreLayout = (LinearLayout) findViewById(R.id.gameresultScoreLayout);
        final EditText gameresultUsernameValueEditText = (EditText) findViewById(R.id.gameresultUsernameValue);
        switch(id) {
            case R.id.gameresultUsernameInputLayout:
                gameresultUsernameInputLayout.setVisibility(View.VISIBLE);
                gameresultScoreLayout.setVisibility(View.GONE);
                gameresultUsernameValueEditText.requestFocus();
                break;
            case R.id.gameresultScoreLayout:
                gameresultUsernameInputLayout.setVisibility(View.GONE);
                gameresultScoreLayout.setVisibility(View.VISIBLE);
                BestScoreTableLayout gamebestlocaltablelayout = (BestScoreTableLayout) findViewById(R.id.gamebestlocaltablelayout);
                gamebestlocaltablelayout.setScoreGroupModel(localScoreGroupModel);
                InputMethodManager im = (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (im != null) {
                    im.hideSoftInputFromWindow(gameresultUsernameValueEditText.getWindowToken(), 0);
                }
                final Button gameresultRestartBtn = (Button) findViewById(R.id.gameresultRestartBtn);
                gameresultRestartBtn.requestFocus();
                break;
        }
    }

    public int getResult() {
        return result;
    }
}
