package com.whizziwig.BetterBookmarks;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import javax.xml.parsers.SAXParser;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Browser;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;

public class BetterBookmarks extends Activity {

    public static final int DONE = 1;

    public static final int MESSAGE = 2;

    public static final int NO_ICON = 3;

    public class FavIconHandler extends DefaultHandler {

        public ArrayList<Attributes> favicon_urls = new ArrayList<Attributes>();

        public class DoneParsingException extends SAXException {
        }

        public void startElement(String uri, String name, String qName, Attributes atts) {
            Log.v("bb", name);
            if (name.trim().equals("link")) {
                Log.v("bb", atts.getValue("rel"));
                if (atts.getValue("rel").trim().equals("icon") || atts.getValue("rel").trim().equals("shortcut icon") || atts.getValue("rel").trim().equals("SHORTCUT ICON") || atts.getValue("rel").trim().equals("apple-touch-icon")) {
                    Log.v("bb", "Adding: " + atts.getValue("href"));
                    favicon_urls.add(atts);
                }
            }
        }

        public void endElement(String uri, String name, String qName) throws SAXException {
            if (name.trim().equals("head")) {
                throw new DoneParsingException();
            }
        }

        String getBestFavIcon() {
            Log.v("BB", favicon_urls.toString());
            Log.v("BB", Integer.toString(favicon_urls.size()));
            for (Attributes atts : favicon_urls) {
                if (atts.getValue("rel").trim().equals("apple-touch-icon")) {
                    return atts.getValue("href");
                }
            }
            for (Attributes atts : favicon_urls) {
                if (atts.getValue("rel").trim().equals("icon")) {
                    if (atts.getValue("href").contains("png")) {
                        if (atts.getValue("sizes") == "64x64") {
                            return atts.getValue("href");
                        }
                    }
                }
            }
            for (Attributes atts : favicon_urls) {
                if (atts.getValue("rel").trim().equals("icon")) {
                    if (atts.getValue("href").contains("png")) {
                        if (atts.getValue("sizes") == "32x32") {
                            return atts.getValue("href");
                        }
                    }
                }
            }
            for (Attributes atts : favicon_urls) {
                if (atts.getValue("rel").trim().equals("icon")) {
                    if (atts.getValue("href").contains("png")) {
                        return atts.getValue("href");
                    }
                }
            }
            for (Attributes atts : favicon_urls) {
                return atts.getValue("href");
            }
            return null;
        }
    }

    private static final String[] mColumnStrings = { Browser.BookmarkColumns._ID, Browser.BookmarkColumns.TITLE, Browser.BookmarkColumns.URL };

    private Handler handler;

    private Thread bgThread;

    private Dialog lastDialog;

    private Bitmap downloaded_icon;

    private String final_host;

    public InputStream fetch(String url, Boolean use_mobile_header) {
        HttpClient client = new DefaultHttpClient();
        URI uri;
        try {
            uri = new URI(url);
            HttpGet method = new HttpGet(uri);
            if (use_mobile_header) {
                method.setHeader("User-Agent", "Mozilla/5.0 (Linux; U; Android 0.5; en-us) AppleWebKit/522+ (KHTML, like Gecko) Safari/419.3");
            } else {
                method.setHeader("User-Agent", "Mozilla/5.0 (Linux; U; en-us) AppleWebKit/522+ (KHTML, like Gecko) Safari/419.3");
            }
            HttpResponse res = client.execute(method);
            Log.v("BB", res.getStatusLine().toString());
            Header location_headers[] = res.getHeaders("Content-Location");
            if (location_headers.length == 1) {
                final_host = new URL(location_headers[0].getValue()).getHost();
            }
            HttpEntity e = res.getEntity();
            Log.v("BB", Long.toString(e.getContentLength()));
            Log.v("BB", e.getContentType().toString());
            return e.getContent();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public InputStream getData(String url, Boolean use_mobile) {
        if (url == null) {
            return null;
        }
        return fetch(url, use_mobile);
    }

    Bitmap getIcon(String url) {
        if (url == null) return null;
        Log.v("BB", "Downloading: " + url);
        InputStream is = getData(url, false);
        if (is != null) {
            Log.v("BB", "Returning bitmap ");
            return BitmapFactory.decodeStream(is);
        }
        return null;
    }

    String getUrl(String url) {
        if (!url.contains("/")) return "http://" + url;
        Uri uri = Uri.parse(url);
        if (uri != null && uri.getHost() != null) return url;
        return "http://" + url;
    }

    String getHost(String url) {
        if (final_host != null) {
            return final_host;
        }
        Uri uri = Uri.parse(url);
        if (uri != null && uri.getHost() != null) return uri.getHost().toString();
        if (!url.contains("/")) return url;
        return null;
    }

    Bitmap getAppleTouchIcon(String url) {
        String host = getHost(url);
        if (host == null) return null;
        String touch_icon = "http://" + host + "/apple-touch-icon.png";
        return getIcon(touch_icon);
    }

    static Bitmap scaleIcon(Bitmap bitmap) {
        if (bitmap == null) return null;
        if (bitmap.getHeight() < android.R.dimen.app_icon_size) {
            Bitmap returnBitmap = Bitmap.createBitmap(android.R.dimen.app_icon_size, android.R.dimen.app_icon_size, Bitmap.Config.ARGB_8888);
            final Canvas canvas = new Canvas(returnBitmap);
            final Paint sStrokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            Integer color = getEdgeColor(bitmap);
            int border = (android.R.dimen.app_icon_size - 32) / 2;
            if (color != null) {
                Log.v("BB", "best color: " + color);
                if (color == 0) {
                    color = Color.WHITE;
                }
                sStrokePaint.setColor(color.intValue());
                RectF rect = new RectF(0, 0, android.R.dimen.app_icon_size, android.R.dimen.app_icon_size);
                canvas.drawRoundRect(rect, border - 1, border - 1, sStrokePaint);
            } else {
                sStrokePaint.setColor(Color.WHITE);
                canvas.drawRect(border, border, android.R.dimen.app_icon_size - border, android.R.dimen.app_icon_size - border, sStrokePaint);
            }
            Bitmap scaled = Bitmap.createScaledBitmap(bitmap, 32, 32, true);
            canvas.drawBitmap(scaled, border, border, sStrokePaint);
            sStrokePaint.setAntiAlias(true);
            return returnBitmap;
        } else if (bitmap.getHeight() > android.R.dimen.app_icon_size) {
            return Bitmap.createScaledBitmap(bitmap, android.R.dimen.app_icon_size, android.R.dimen.app_icon_size, true);
        }
        return bitmap;
    }

    private static Integer getEdgeColor(Bitmap bitmap) {
        if (bitmap == null) {
            return null;
        }
        Map<Integer, Integer> colors = new HashMap<Integer, Integer>();
        for (int i = 0; i < bitmap.getWidth(); ++i) {
            for (int j = 0; j < bitmap.getHeight(); ++j) {
                if (i == 0 || j == 0 || i == bitmap.getWidth() - 1 || j == bitmap.getHeight() - 1) {
                    Integer color = Integer.valueOf(bitmap.getPixel(i, j));
                    if (!colors.containsKey(color)) {
                        colors.put(color, Integer.valueOf(0));
                    }
                    colors.put(color, Integer.valueOf(colors.get(color).intValue() + 1));
                }
            }
        }
        Set<Integer> keys = colors.keySet();
        for (Integer key1 : keys) {
            for (Integer key2 : keys) {
                if (key1 != key2 && checkDistance(key1, key2)) {
                    colors.put(key1, colors.get(key1) + colors.get(key2));
                    Log.v("BB", "combining: " + key1 + " and " + key2);
                }
            }
        }
        for (Entry<Integer, Integer> i : colors.entrySet()) {
            Log.v("BB", "color: " + i.getKey() + " -- count: " + i.getValue());
            if (i.getValue() >= bitmap.getHeight() * 2) {
                return i.getKey();
            }
        }
        return null;
    }

    private static boolean checkDistance(int key, int key2) {
        int distance = Math.abs(Color.blue(key) - Color.blue(key2)) + Math.abs(Color.red(key) - Color.red(key2)) + Math.abs(Color.green(key) - Color.green(key2));
        return distance < 10;
    }

    Bitmap getBaseFavicon(String url) {
        String host = getHost(url);
        if (host == null) return null;
        String touch_icon = "http://" + host + "/favicon.ico";
        Bitmap icon = getIcon(touch_icon);
        return icon;
    }

    Bitmap getFavicon(String url) {
        Log.v("BB", "Trying to get favicon from: " + getUrl(url));
        try {
            InputStream instream = fetch(getUrl(url), true);
            SAXParser sp = org.ccil.cowan.tagsoup.jaxp.SAXParserImpl.newInstance(null);
            XMLReader xr = sp.getXMLReader();
            FavIconHandler myExampleHandler = new FavIconHandler();
            xr.setContentHandler(myExampleHandler);
            try {
                xr.parse(new InputSource(instream));
            } catch (FavIconHandler.DoneParsingException e) {
                Log.v("BB", "ended head!");
            }
            String bestIcon = myExampleHandler.getBestFavIcon();
            if (bestIcon == null) return null;
            Log.v("bb", "best icon: " + bestIcon);
            if (!bestIcon.startsWith("http")) {
                Log.v("BB", "best icon: " + bestIcon);
                Log.v("BB", "url: " + getUrl(url));
                if (bestIcon.startsWith("/")) {
                    Uri uri = Uri.withAppendedPath(Uri.parse(getUrl(getHost(url))), bestIcon);
                    android.net.Uri.Builder builder = uri.buildUpon();
                    builder.path(bestIcon);
                    uri = builder.build();
                    Log.v("BB final", uri.toString());
                    bestIcon = uri.toString().replace("//", "/").replace(":/", "://");
                    Log.v("BB", "using: " + bestIcon);
                } else {
                    return null;
                }
            }
            return getIcon(bestIcon);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void showDialogForConfirmation(final String name, final String url, final Bitmap icon) {
        if (lastDialog != null) {
            lastDialog.dismiss();
        }
        LayoutInflater factory = LayoutInflater.from(this);
        final View textEntryView = factory.inflate(R.layout.looks_good_dialog, null);
        final Dialog d = new AlertDialog.Builder(this).setTitle("Looks good?").setView(textEntryView).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int whichButton) {
                toggleEnabled(true);
            }
        }).setPositiveButton("OK", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int whichButton) {
                final TextView edit_label = (TextView) ((Dialog) dialog).findViewById(R.id.edit_label_name_final);
                Log.v("BB", edit_label.getText().toString());
                returnIntent(edit_label.getText().toString(), url, icon);
            }
        }).create();
        d.show();
        final TextView edit_label = (TextView) d.findViewById(R.id.edit_label_name_final);
        edit_label.setText(name);
        final ImageView image = (ImageView) d.findViewById(R.id.image_final);
        if (icon == null) {
            image.setImageResource(R.drawable.icon);
        } else {
            image.setImageBitmap(icon);
        }
    }

    private void showDialogForManualEntry() {
        toggleEnabled(false);
        LayoutInflater factory = LayoutInflater.from(this);
        final View textEntryView = factory.inflate(R.layout.alert_dialog_text_entry, null);
        final Dialog d = new AlertDialog.Builder(this).setTitle("Enter URL").setView(textEntryView).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int whichButton) {
                toggleEnabled(true);
            }
        }).setPositiveButton("OK", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int whichButton) {
                Dialog d = (Dialog) dialog;
                EditText editUrl = (EditText) d.findViewById(R.id.edit_url);
                EditText editIconUrl = (EditText) d.findViewById(R.id.edit_icon_url);
                EditText editName = (EditText) d.findViewById(R.id.edit_bookmark_name);
                dialog.dismiss();
                String iconUrl = editIconUrl.getText().toString();
                if (iconUrl != null && !iconUrl.equals("") && !iconUrl.startsWith("http")) {
                    iconUrl = "http://" + iconUrl;
                }
                runChildThead(editName.getText().toString(), editUrl.getText().toString(), iconUrl);
            }
        }).create();
        d.show();
    }

    private void shouldMakeDefaultShortcut(final String title, final String url) {
        lastDialog.dismiss();
        new AlertDialog.Builder(this).setTitle("Question").setMessage("Couldn't find a favicon. Create default bookmark?").setNegativeButton("No", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int whichButton) {
                toggleEnabled(true);
            }
        }).setPositiveButton("Yes", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int whichButton) {
                confirmIntent(title, url, null);
            }
        }).show();
    }

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        setContentView(R.layout.main);
        final Context ctx = this;
        handler = new Handler() {

            /** Gets called on every message that is received */
            public void handleMessage(Message msg) {
                switch(msg.what) {
                    case BetterBookmarks.DONE:
                        showDialogForConfirmation(msg.getData().getString("title"), msg.getData().getString("url"), downloaded_icon);
                        break;
                    case BetterBookmarks.MESSAGE:
                        showAlertMessageDialog(ctx, (String) msg.obj);
                        break;
                    case BetterBookmarks.NO_ICON:
                        lastDialog.dismiss();
                        shouldMakeDefaultShortcut(msg.getData().getString("title"), msg.getData().getString("url"));
                        break;
                }
            }
        };
        final String BOOKMARK_SELECTION = "bookmark = 1";
        Cursor c = getContentResolver().query(Browser.BOOKMARKS_URI, mColumnStrings, BOOKMARK_SELECTION, null, null);
        ListView bookmarkslist = (ListView) findViewById(R.id.bookmarks);
        SimpleCursorAdapter adapter = new SimpleCursorAdapter(this, android.R.layout.simple_list_item_1, c, new String[] { Browser.BookmarkColumns.TITLE }, new int[] { android.R.id.text1 });
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        bookmarkslist.setAdapter(adapter);
        bookmarkslist.setOnItemClickListener(new OnItemClickListener() {

            public void onItemClick(AdapterView parent, View v, int position, long id) {
                Uri uri = Uri.parse(Browser.BOOKMARKS_URI + "/" + Long.toString(id));
                Cursor c = getContentResolver().query(uri, mColumnStrings, null, null, null);
                if (c.moveToFirst()) {
                    do {
                        String title = c.getString(1);
                        String url = c.getString(2);
                        runChildThead(title, url, null);
                    } while (c.moveToNext());
                }
            }
        });
        Button buttonManual = (Button) findViewById(R.id.button_manual);
        buttonManual.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                showDialogForManualEntry();
            }
        });
    }

    private void showAlertMessageDialog(Context ctx, String message) {
        Builder alert = new AlertDialog.Builder(ctx).setTitle("Progress").setMessage(message).setIcon(android.R.drawable.ic_dialog_alert);
        alert.setCancelable(false);
        Dialog newDialog = alert.create();
        newDialog.show();
        if (lastDialog != null) {
            lastDialog.dismiss();
        }
        lastDialog = newDialog;
        Log.v("BB", "showing message in parent SAD!: " + message);
    }

    private void showAlertMessage(String messageText) {
        Message message = new Message();
        message.what = MESSAGE;
        message.obj = messageText;
        handler.sendMessage(message);
    }

    private void toggleEnabled(Boolean b) {
        findViewById(R.id.bookmarks).setEnabled(b);
        findViewById(R.id.button_manual).setEnabled(b);
    }

    private void runChildThead(final String title, final String url, final String icon_url) {
        toggleEnabled(false);
        bgThread = new Thread() {

            public void run() {
                if (icon_url == null || icon_url.equals("")) {
                    buildIntent(title, url);
                } else {
                    confirmIntent(title, url, getIcon(icon_url));
                }
            }
        };
        bgThread.start();
    }

    private void buildIntent(String title, String url) {
        showAlertMessage("Looking for apple touch icon");
        Bitmap icon = getAppleTouchIcon(url);
        if (icon == null) {
            showAlertMessage("Looking in page for favicon");
            icon = getFavicon(url);
        }
        if (icon == null) {
            showAlertMessage("Checking for favicon.ico");
            Log.v("BetterBookmarks", "trying to get base favico");
            icon = getBaseFavicon(url);
            Log.v("BetterBookmarks", "is null? " + Boolean.toString(icon == null));
        }
        if (icon == null) {
            Message message = new Message();
            message.what = NO_ICON;
            Bundle data = new Bundle();
            data.putString("title", title);
            data.putString("url", url);
            message.setData(data);
            handler.sendMessage(message);
        } else {
            confirmIntent(title, url, icon);
        }
    }

    protected void confirmIntent(String name, String url, Bitmap icon) {
        Message message = new Message();
        message.what = DONE;
        Bundle data = new Bundle();
        data.putString("title", name);
        data.putString("url", url);
        if (icon != null) {
            downloaded_icon = scaleIcon(icon);
        } else {
            downloaded_icon = null;
        }
        message.setData(data);
        handler.sendMessage(message);
    }

    protected void returnIntent(String name, String url, Bitmap icon) {
        Log.v("BB", url);
        Uri uri = Uri.parse(getUrl(url));
        Intent launch_intent = new Intent();
        launch_intent.setData(uri);
        launch_intent.setAction(Intent.ACTION_VIEW);
        final Intent shortcut_intent = new Intent();
        shortcut_intent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, launch_intent);
        shortcut_intent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name);
        if (icon != null) {
            shortcut_intent.putExtra(Intent.EXTRA_SHORTCUT_ICON, icon);
        } else {
            shortcut_intent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(this, R.drawable.icon));
        }
        if (lastDialog != null) {
            lastDialog.dismiss();
        }
        setResult(RESULT_OK, shortcut_intent);
        Toast.makeText(this, R.string.shortcutCreated, Toast.LENGTH_SHORT).show();
        finish();
    }
}
