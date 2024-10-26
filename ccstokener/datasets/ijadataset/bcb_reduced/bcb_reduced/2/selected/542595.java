package jp.hackathon.voctrl.input;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class VoctrlInput extends Activity implements OnClickListener {

    private static final int VOICE_RECOGNITION_REQUEST_CODE = 1234;

    private TextView text1;

    private TextView text2;

    private TextView text3;

    private TextView speakText;

    /**
     * Called with the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        Button speakButton = (Button) findViewById(R.id.SpeachBtn);
        text1 = (TextView) findViewById(R.id.PrintText1);
        text2 = (TextView) findViewById(R.id.PrintText2);
        text3 = (TextView) findViewById(R.id.PrintText3);
        speakText = (TextView) findViewById(R.id.SpeakText);
        text1.setOnClickListener(this);
        text2.setOnClickListener(this);
        text3.setOnClickListener(this);
        PackageManager pm = getPackageManager();
        List<ResolveInfo> activities = pm.queryIntentActivities(new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH), 0);
        if (activities.size() != 0) {
            speakButton.setOnClickListener(this);
        } else {
            speakButton.setEnabled(false);
            speakButton.setText("Recognizer not present");
        }
    }

    /**
     * Handle the click on the start recognition button.
     */
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.SpeachBtn:
                startVoiceRecognitionActivity();
                break;
            case R.id.PrintText1:
                text1.setTextColor(Color.GREEN);
                text2.setTextColor(Color.BLACK);
                text3.setTextColor(Color.BLACK);
                break;
            case R.id.PrintText2:
                text1.setTextColor(Color.BLACK);
                text2.setTextColor(Color.GREEN);
                text3.setTextColor(Color.BLACK);
                break;
            case R.id.PrintText3:
                text1.setTextColor(Color.BLACK);
                text2.setTextColor(Color.BLACK);
                text3.setTextColor(Color.GREEN);
                break;
        }
    }

    /**
     * Fire an intent to start the speech recognition activity.
     */
    private void startVoiceRecognitionActivity() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speech recognition demo");
        startActivityForResult(intent, VOICE_RECOGNITION_REQUEST_CODE);
    }

    /**
	 * Handle the results from the recognition activity.
	 */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == VOICE_RECOGNITION_REQUEST_CODE && resultCode == RESULT_OK) {
            ArrayList<String> matches = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            StringBuilder sb = new StringBuilder();
            for (String str : matches) {
                sb.append(str);
            }
            speakText.setText(sb.toString());
            sendHttpRequest(sb.toString());
        }
    }

    private void sendHttpRequest(String str) {
        final String uri = "http://voctrl.appspot.com/input/" + str;
        DefaultHttpClient client = new DefaultHttpClient();
        try {
            BufferedReader bufedReader = null;
            HttpResponse httpResponse = client.execute(new HttpGet(uri));
            bufedReader = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
            String line = bufedReader.readLine();
            if (Constants.SEND_OK.equals(line)) {
                Toast.makeText(this, "���M�Ȃ�", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }
}
