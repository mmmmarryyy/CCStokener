package snooker.scoreboard.webservice;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import snooker.scoreboard.Match;
import snooker.scoreboard.frame.Frame;
import snooker.scoreboard.frame.Score;
import android.app.ProgressDialog;
import android.util.Log;

public class WebConnector {

    private static final String LOG_TAG = "WebConnector";

    private static String URI = "snookerboard.uw.hu";

    private static String URI_UPLOAD_MATCH = "http://" + URI + "/upload_match.php";

    private static String URI_UPLOAD_FRAME = "http://" + URI + "/upload_frame.php";

    private static WebConnector wc;

    private ProgressDialog progressDialog;

    private WebConnector() {
        Log.i(LOG_TAG, "Created Webconnector");
    }

    public static WebConnector getInstance() {
        if (wc == null) {
            wc = new WebConnector();
        }
        return wc;
    }

    public boolean uploadMatch(ProgressDialog progressDialog, Match match) {
        this.progressDialog = progressDialog;
        boolean result = true;
        result = postData(match);
        return result;
    }

    public Match loadMatch() {
        Match match = Match.getCurrentMatch();
        return match;
    }

    private boolean postData(Match match) {
        progressDialog.setProgress(5);
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost(URI_UPLOAD_MATCH);
        boolean result = false;
        try {
            UrlEncodedFormEntity en = new UrlEncodedFormEntity(getMatchPostData(match));
            httppost.setEntity(en);
            HttpResponse response = httpclient.execute(httppost);
            progressDialog.setProgress(10);
            HttpEntity he = response.getEntity();
            BufferedReader in = new BufferedReader(new InputStreamReader(he.getContent()));
            String responseLine = in.readLine();
            Log.i(LOG_TAG, responseLine);
            String[] r = responseLine.trim().split(";");
            if (r.length == 2) {
                String matchId = r[0];
                int frameId = Integer.parseInt(r[1]);
                int frames = match.getFrames().size() - frameId;
                int i = 0;
                for (Frame f : match.getFrames()) {
                    if (f.getFrameId() <= frameId) {
                        continue;
                    } else {
                        HttpPost fhttppost = new HttpPost(URI_UPLOAD_FRAME);
                        fhttppost.setEntity(new UrlEncodedFormEntity(getFramePostData(f, matchId, match.getPlayer1Name(), match.getPlayer2Name())));
                        response = httpclient.execute(fhttppost);
                        he = response.getEntity();
                        in = new BufferedReader(new InputStreamReader(he.getContent()));
                        while ((responseLine = in.readLine()) != null) {
                            Log.i(LOG_TAG, responseLine);
                        }
                        i++;
                        double p = (double) i * (double) 90 / (double) frames;
                        progressDialog.setProgress(10 + (int) p);
                    }
                }
                result = true;
            } else {
                result = false;
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }

    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    private List<NameValuePair> getMatchPostData(Match match) {
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(9);
        nameValuePairs.add(new BasicNameValuePair("user_name", "eshu"));
        nameValuePairs.add(new BasicNameValuePair("player1", match.getPlayer1Name()));
        nameValuePairs.add(new BasicNameValuePair("player2", match.getPlayer2Name()));
        nameValuePairs.add(new BasicNameValuePair("score1", Integer.toString(match.getPlayer1FrameWins())));
        nameValuePairs.add(new BasicNameValuePair("score2", Integer.toString(match.getPlayer2FrameWins())));
        nameValuePairs.add(new BasicNameValuePair("start_date", sdf.format(match.getStartDate())));
        return nameValuePairs;
    }

    private List<NameValuePair> getFramePostData(Frame f, String matchId, String p1n, String p2n) {
        Log.i(LOG_TAG, "Saving frame: " + f.getFrameId());
        Log.i(LOG_TAG, "Saving history size: " + f.getScoreHistory().getScoreList().size());
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(9);
        int line = 0;
        nameValuePairs.add(new BasicNameValuePair("match_id", matchId));
        for (Score state : f.getScoreHistory().getScoreList()) {
            String ms = "frame[" + line + "]";
            nameValuePairs.add(new BasicNameValuePair(ms + "[player_name]", (state.getPlayerId() == Match.PLAYER1 ? p1n : p2n)));
            nameValuePairs.add(new BasicNameValuePair(ms + "[frame]", Integer.toString(f.getFrameId())));
            nameValuePairs.add(new BasicNameValuePair(ms + "[type]", state.getType().getIdString()));
            nameValuePairs.add(new BasicNameValuePair(ms + "[break]", Integer.toString(state.getBreakId())));
            nameValuePairs.add(new BasicNameValuePair(ms + "[ball]", state.getBall().getIdString()));
            nameValuePairs.add(new BasicNameValuePair(ms + "[points]", Integer.toString(state.getScore())));
            nameValuePairs.add(new BasicNameValuePair(ms + "[time]", "" + state.getEventTime()));
            nameValuePairs.add(new BasicNameValuePair(ms + "[waslucky]", state.isLuckyShot() ? "1" : "0"));
            line++;
        }
        return nameValuePairs;
    }
}
