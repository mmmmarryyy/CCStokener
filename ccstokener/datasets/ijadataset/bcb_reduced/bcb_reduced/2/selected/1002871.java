package fr.slvn.badass.parser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import fr.slvn.badass.data.BadassEntry;
import android.util.Log;

public class BadassListParser {

    private final int DATE_GROUP = 1;

    private final int LINK_GROUP = 2;

    private final int NAME_GROUP = 3;

    private final String START_PARSE = "<font size=+1>";

    private final String STOP_PARSE = "<br><center>";

    private final String ENTRY_HINT = "href";

    String mDate;

    String mLink;

    String mName;

    String mUrl;

    List<BadassEntry> mBadassEntries;

    public BadassListParser(String pUrl) {
        this.mUrl = pUrl;
    }

    public List<BadassEntry> parse() {
        mBadassEntries = new ArrayList<BadassEntry>();
        try {
            URL url = new URL(mUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setDoOutput(true);
            connection.connect();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String line;
            boolean flag1 = false;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (!flag1 && line.contains(START_PARSE)) flag1 = true;
                if (flag1 && line.contains(STOP_PARSE)) break;
                if (flag1) {
                    if (line.contains(ENTRY_HINT)) {
                        parseBadass(line);
                    }
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return mBadassEntries;
    }

    private final String fullRegExp = "([0-9\\/]*):&nbsp;\\s<a\\shref=\"([^\"]*)\">([^<]*)<\\/a>";

    private final Pattern mPattern = Pattern.compile(fullRegExp);

    private void parseBadass(String string) {
        Log.i("BADASS", string);
        BadassEntry entry = parser(mPattern, mPattern.matcher(string));
        if (entry != null) {
            mBadassEntries.add(entry);
        }
    }

    private BadassEntry parser(Pattern pPattern, Matcher pMatcher) {
        Integer code;
        while (pMatcher.find()) {
            code = Integer.parseInt(pMatcher.group(DATE_GROUP).replaceAll("/", ""));
            BadassEntry.Builder builder = new BadassEntry.Builder(0, pMatcher.group(NAME_GROUP), pMatcher.group(DATE_GROUP), pMatcher.group(LINK_GROUP), code);
            return builder.build();
        }
        return null;
    }
}
