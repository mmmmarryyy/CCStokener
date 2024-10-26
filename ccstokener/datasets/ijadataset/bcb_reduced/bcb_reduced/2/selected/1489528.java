package fr.slvn.badass.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import fr.slvn.badass.data.BadassEntry;
import fr.slvn.badass.data.BadassHandler;
import fr.slvn.badass.parser.BadassListParser;
import fr.slvn.badass.tools.FileManager;

public class DataService extends IntentService {

    private static final String PARSING_URL = "http://www.badassoftheweek.com/list.html";

    private static final String URL_DEFAULT = "http://www.badassoftheweek.com/";

    private static final String URL_START = "http://";

    private static final String ACTION_PREFIX = DataService.class.getName() + ".action.";

    public static final String ACTION_REFRESH_LIST = ACTION_PREFIX + "refresh_list";

    public static final String ACTION_LOAD_ENTRY = ACTION_PREFIX + "get_entry";

    public static final String ACTION_CLEAN_CACHE = ACTION_PREFIX + "cleat_cache";

    private static final String RESPONSE_PREFIX = DataService.class.getName() + ".response.";

    public static final String RESPONSE_REFRESH_LIST = RESPONSE_PREFIX + "refresh_list";

    public static final String RESPONSE_LOAD_ENTRY = RESPONSE_PREFIX + "get_entry";

    public static final String RESPONSE_CLEAN_CACHE = RESPONSE_PREFIX + "cleat_cache";

    public static final String ACTION_RESULT = "action_result";

    public static final String LOAD_ENTRY_NAME = "load_entry_name";

    public static final String LOAD_ENTRY_ID = "load_entry_id";

    private boolean mInitiated = false;

    private BadassHandler mDb;

    public DataService(String name) {
        super(name);
        if (!mInitiated) {
            init(this);
        }
    }

    public void init(Context context) {
        mDb = new BadassHandler(context).open();
        mInitiated = true;
    }

    @Override
    public void onDestroy() {
        if (mDb != null) mDb.close();
        super.onDestroy();
    }

    private class RefreshList extends AsyncTask<Void, Void, Integer> {

        @Override
        protected Integer doInBackground(Void... params) {
            List<BadassEntry> entries = new BadassListParser(PARSING_URL).parse();
            mDb.fillDatabaseWith(entries);
            return Integer.valueOf(entries.size());
        }

        @Override
        protected void onPostExecute(Integer result) {
            Intent intent = new Intent(RESPONSE_REFRESH_LIST);
            intent.putExtra(ACTION_RESULT, result);
            sendBroadcast(intent);
        }
    }

    private class LoadTextView extends AsyncTask<String, Integer, Integer> {

        public int mEntryId = -1;

        public LoadTextView(int entryId) {
            super();
            mEntryId = entryId;
        }

        protected Integer doInBackground(String... urls) {
            ArrayList<String> images = new ArrayList<String>();
            if (!FileManager.INSTANCE.isFileCached(urls[0])) {
                URL url;
                try {
                    Writer out = FileManager.INSTANCE.getFileWriterBuffered(urls[0]);
                    url = new URL(checkUrl(urls[0]));
                    HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                    connection.setRequestMethod("GET");
                    connection.setDoOutput(true);
                    connection.connect();
                    InputStreamReader is = new InputStreamReader(connection.getInputStream());
                    BufferedReader reader = new BufferedReader(is);
                    String line;
                    boolean flag1 = false;
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.contains("</center>")) {
                            flag1 = true;
                            break;
                        }
                    }
                    if (flag1) {
                        while ((line = reader.readLine()) != null) {
                            if (line.contains("<br><center>")) {
                                break;
                            }
                            if (line.startsWith("<img vspace=")) {
                                images.add(getImageName(line));
                            }
                            out.write(line);
                        }
                    }
                    out.close();
                    reader.close();
                    is.close();
                    connection.disconnect();
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                } catch (ProtocolException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            int totalImage = images.size();
            for (int i = 0; i < totalImage; i++) {
                FileManager.INSTANCE.downloadFileFromInternet(images.get(i), checkUrl(images.get(i)));
            }
            return 0;
        }

        @Override
        protected void onPostExecute(Integer result) {
            Intent intent = new Intent(RESPONSE_LOAD_ENTRY);
            intent.putExtra(LOAD_ENTRY_ID, mEntryId);
            intent.putExtra(ACTION_RESULT, result);
            sendBroadcast(intent);
        }

        private String checkUrl(String pUrl) {
            if (pUrl.startsWith(URL_START)) return pUrl; else return URL_DEFAULT + pUrl;
        }

        private String fullRegExp = "<img.*\"(.*)\">";

        private int imgUrlGroup = 1;

        private Pattern mPattern = Pattern.compile(fullRegExp);

        private String getImageName(String line) {
            Matcher pMatcher = mPattern.matcher(line);
            while (pMatcher.find()) {
                return pMatcher.group(imgUrlGroup);
            }
            return null;
        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        if (action.equals(ACTION_REFRESH_LIST)) {
            new RefreshList().execute();
        } else if (action.equals(ACTION_LOAD_ENTRY)) {
            Bundle extras = intent.getExtras();
            String entryName = extras.getString(LOAD_ENTRY_NAME);
            int entryId = extras.getInt(LOAD_ENTRY_ID);
            new LoadTextView(entryId).execute(entryName);
        } else if (action.equals(ACTION_CLEAN_CACHE)) {
        }
    }
}
