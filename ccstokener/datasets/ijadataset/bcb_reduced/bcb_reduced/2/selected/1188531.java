package com.echodrama.upturner;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import com.echodrama.upturner.R;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseExpandableListAdapter;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class BestScoreExpandableListAdapter extends BaseExpandableListAdapter {

    private Activity activity;

    private ScoreGroupModel localScoreGroupModel;

    private ScoreGroupModel internetScoreGroupModel;

    private Resources res;

    private boolean loadingFlag;

    public BestScoreExpandableListAdapter(Activity a) {
        this.activity = a;
        this.res = this.activity.getResources();
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        if (groupPosition == 0) {
            return this.localScoreGroupModel;
        } else if (groupPosition == 1) {
            return this.internetScoreGroupModel;
        }
        return null;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return groupPosition * 10 + childPosition;
    }

    @Override
    public View getChildView(int groupPosition, int childPosition, boolean isLastChild, View convertView, ViewGroup parent) {
        ScoreGroupModel groupModel = (ScoreGroupModel) getChild(groupPosition, childPosition);
        if (groupModel == null) {
            AbsListView.LayoutParams lp = new AbsListView.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 40);
            TextView text = new TextView(activity);
            text.setLayoutParams(lp);
            text.setGravity(Gravity.CENTER_VERTICAL | Gravity.CENTER);
            text.setTextColor(android.graphics.Color.BLACK);
            if (groupPosition == 0) {
                text.setText(res.getString(R.string.message_norecord));
                Toast.makeText(activity, res.getString(R.string.message_networkerror), Toast.LENGTH_SHORT).show();
            } else {
                text.setText(res.getString(R.string.message_networkerror));
                Toast.makeText(activity, res.getString(R.string.message_networkerror), Toast.LENGTH_SHORT).show();
            }
            return text;
        }
        BestScoreTableLayout tableLayout = new BestScoreTableLayout(activity);
        tableLayout.setScoreGroupModel(groupModel);
        return tableLayout;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return 1;
    }

    @Override
    public Object getGroup(int groupPosition) {
        if (groupPosition == 0) {
            return res.getString(R.string.game_best_local);
        } else if (groupPosition == 1) {
            return res.getString(R.string.game_best_internet);
        }
        return "";
    }

    @Override
    public int getGroupCount() {
        return 2;
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded, View convertView, final ViewGroup parent) {
        AbsListView.LayoutParams lp = new AbsListView.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, 64);
        TextView text = new TextView(activity);
        text.setLayoutParams(lp);
        text.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
        text.setPadding(40, 0, 0, 0);
        text.setText((String) getGroup(groupPosition));
        text.setTextColor(android.graphics.Color.BLACK);
        return text;
    }

    @Override
    public void onGroupExpanded(int groupPosition) {
        if (groupPosition == 0) {
            loadFromLocal(false);
        } else if (groupPosition == 1) {
            loadFromInternet(false);
        }
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }

    public void loadFromLocal(boolean reload) {
        if (!reload && this.localScoreGroupModel != null) {
            return;
        }
        SharedPreferences preferences = activity.getSharedPreferences(Constants.PROPERTIES, Context.MODE_WORLD_READABLE);
        this.localScoreGroupModel = new ScoreGroupModel();
        for (int i = 0; i < ScoreGroupModel.MAX_ITEM_NUM; i++) {
            String username = preferences.getString(Constants.PROPERTIES_GAMEBEST_USERNAME + i, "");
            if (username != null && !username.equals("")) {
                String gameBestScore = preferences.getString(Constants.PROPERTIES_GAMEBEST_SCORE + i, "");
                String gameBestDate = preferences.getString(Constants.PROPERTIES_GAMEBEST_DATE + i, "");
                ScoreItemModel itemModel = new ScoreItemModel(username, gameBestScore, gameBestDate);
                this.localScoreGroupModel.addItem(itemModel);
            }
        }
    }

    public void loadFromInternet(boolean reload) {
        if (!reload && this.internetScoreGroupModel != null) {
            return;
        }
        loadingFlag = true;
        ProgressBar settingProgressBar = (ProgressBar) this.activity.findViewById(R.id.settingProgressBar);
        settingProgressBar.setVisibility(View.VISIBLE);
        final Timer timer = new Timer();
        final Handler handler = new Handler() {

            @Override
            public void handleMessage(Message msg) {
                if (loadingFlag == false) {
                    ProgressBar settingProgressBar = (ProgressBar) BestScoreExpandableListAdapter.this.activity.findViewById(R.id.settingProgressBar);
                    settingProgressBar.setVisibility(View.INVISIBLE);
                    timer.cancel();
                }
                super.handleMessage(msg);
            }
        };
        final TimerTask task = new TimerTask() {

            @Override
            public void run() {
                Message message = new Message();
                handler.sendMessage(message);
            }
        };
        timer.schedule(task, 1, 50);
        String httpUrl = Constants.SERVER_URL + "/rollingcard.php?op=viewbestscore";
        HttpGet request = new HttpGet(httpUrl);
        HttpClient httpClient = new DefaultHttpClient();
        try {
            HttpResponse response = httpClient.execute(request);
            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                String entity = EntityUtils.toString(response.getEntity());
                String[] itemArray = entity.split(";");
                this.internetScoreGroupModel = new ScoreGroupModel();
                for (int i = 0; i < itemArray.length; i++) {
                    String[] itemValueArray = itemArray[i].split("\\|");
                    if (itemValueArray.length != 3) {
                        continue;
                    }
                    ScoreItemModel itemModel = new ScoreItemModel(itemValueArray[0], itemValueArray[1], itemValueArray[2]);
                    this.internetScoreGroupModel.addItem(itemModel);
                }
            }
        } catch (ClientProtocolException e) {
            this.internetScoreGroupModel = null;
            e.printStackTrace();
        } catch (IOException e) {
            this.internetScoreGroupModel = null;
            e.printStackTrace();
        }
        loadingFlag = false;
    }
}
