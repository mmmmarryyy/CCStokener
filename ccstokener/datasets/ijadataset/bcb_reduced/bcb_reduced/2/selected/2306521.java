package by.oslab.hackathon.easycar;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import by.oslab.hackathon.easycar.android.R;
import by.oslab.hackathon.easycar.model.Adv;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

public class AdvsAdapter extends BaseAdapter {

    private static final String TAG = AdvsAdapter.class.getSimpleName();

    private static final String SERVER_URL = "http://178.79.169.112:8080/EasyCar/json.do";

    private Context mContext;

    private List<Adv> mAdvs;

    public AdvsAdapter(Context context) {
        mContext = context;
        mAdvs = new ArrayList<Adv>();
        final Gson gson = new Gson();
        final String jsonString = readJsonString();
        if (!TextUtils.isEmpty(jsonString)) {
            final JsonParser parser = new JsonParser();
            try {
                final JsonArray array = parser.parse(jsonString).getAsJsonArray();
                for (int i = 0; i < array.size(); i++) {
                    mAdvs.add(gson.fromJson(array.get(i), Adv.class));
                }
            } catch (JsonSyntaxException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View itemView = inflater.inflate(R.layout.reminder_list_item, null);
        TextView title = (TextView) itemView.findViewById(R.id.reminder_item_title);
        TextView resource = (TextView) itemView.findViewById(R.id.reminder_item_resource);
        title.setText(mAdvs.get(position).getTitle());
        resource.setText(mAdvs.get(position).getText() + "");
        return itemView;
    }

    @Override
    public long getItemId(int position) {
        return mAdvs.get(position).getId();
    }

    @Override
    public Object getItem(int position) {
        return mAdvs.get(position);
    }

    @Override
    public int getCount() {
        return mAdvs.size();
    }

    private String readJsonString() {
        StringBuilder builder = new StringBuilder();
        HttpClient client = new DefaultHttpClient();
        HttpGet httpGet = new HttpGet(SERVER_URL);
        try {
            HttpResponse response = client.execute(httpGet);
            StatusLine statusLine = response.getStatusLine();
            int statusCode = statusLine.getStatusCode();
            if (statusCode == 200) {
                HttpEntity entity = response.getEntity();
                InputStream content = entity.getContent();
                BufferedReader reader = new BufferedReader(new InputStreamReader(content));
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
            } else {
                Log.e(TAG, "Failed to download file");
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return builder.toString();
    }
}
