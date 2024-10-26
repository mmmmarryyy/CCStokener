package gokul.cs3200.rsr;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class ReallySimpleReader extends Activity {

    static RSSItem selectedItem = null;

    static String RSSTitle;

    static String feedUrl;

    static File openedFileName;

    static boolean isLive;

    boolean wena = false;

    String url = feedUrl;

    File f;

    TextView tv;

    ListView rssView;

    ArrayList<RSSItem> rSSItems = new ArrayList<RSSItem>();

    ArrayList<RSSItem> newItems;

    ArrayAdapter<RSSItem> aa;

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reader_body);
        Button saveFeed = (Button) findViewById(R.id.saveFeed);
        if (isLive == true) {
            newItems = getRSSItems(connect());
            if (this.wena != false) {
                saveFeed.setVisibility(0);
            }
        } else if (isLive == false) {
            InputStream stream = this.readData();
            newItems = getRSSItems(stream);
            tv = (TextView) findViewById(R.id.label);
            tv.setText(openedFileName.getName() + "  (offline mode)");
        }
        rssView = (ListView) findViewById(R.id.rssListView);
        aa = new ArrayAdapter<RSSItem>(this, R.layout.list_item, rSSItems);
        rssView.setAdapter(aa);
        rSSItems.clear();
        rSSItems.addAll(newItems);
        aa.notifyDataSetChanged();
        rssView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> av, View view, int index, long arg3) {
                selectedItem = rSSItems.get(index);
                Intent intent = new Intent("gokul.cs3200.rsr.displayRSSItem");
                startActivity(intent);
            }
        });
        saveFeed.setOnClickListener(new OnClickListener() {

            @Override
            public void onClick(View v) {
                saveData();
            }
        });
    }

    public ArrayList<RSSItem> getRSSItems(InputStream s) {
        ArrayList<RSSItem> rSSItems = new ArrayList<RSSItem>();
        try {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document document = db.parse(s);
            Element element = document.getDocumentElement();
            NodeList nodeList = element.getElementsByTagName("item");
            if (nodeList.getLength() > 0) {
                for (int i = 0; i < nodeList.getLength(); i++) {
                    Element currentElement = (Element) nodeList.item(i);
                    Element _titleE = (Element) currentElement.getElementsByTagName("title").item(0);
                    Element _descriptionE = (Element) currentElement.getElementsByTagName("description").item(0);
                    Element _pubDateE = (Element) currentElement.getElementsByTagName("pubDate").item(0);
                    Element _linkE = (Element) currentElement.getElementsByTagName("link").item(0);
                    String _title = _titleE.getFirstChild().getNodeValue();
                    String _description = _descriptionE.getFirstChild().getNodeValue();
                    Date _pubDate = new Date(_pubDateE.getFirstChild().getNodeValue());
                    String _link = _linkE.getFirstChild().getNodeValue();
                    RSSItem rSSItem = new RSSItem(_title, _description, _pubDate, _link);
                    rSSItems.add(rSSItem);
                }
            }
        } catch (Exception e) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("The URL you provided is not valid. Please check and try again.").setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int id) {
                    ReallySimpleReader.this.finish();
                }
            });
            AlertDialog alert = builder.create();
            alert.show();
        }
        return rSSItems;
    }

    public InputStream connect() {
        try {
            URL url = new URL(this.url);
            RSSTitle = url.getHost();
            tv = (TextView) findViewById(R.id.label);
            tv.setText(RSSTitle);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream is = conn.getInputStream();
                this.wena = true;
                return is;
            } else {
                return null;
            }
        } catch (Exception e) {
            return null;
        }
    }

    public void saveData() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText input = new EditText(this);
        builder.setView(input);
        builder.setMessage("Choose a filename:").setPositiveButton("OK", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int id) {
                File dir = new File("/sdcard/Saved RSS Feeds/");
                dir.mkdirs();
                String fileName = input.getText().toString().trim();
                if (!(fileName.equals(null)) || !(fileName.equals("")) || !(fileName.equals(" "))) {
                    File f = new File(dir, fileName + ".rss");
                    try {
                        InputStreamReader ir = new InputStreamReader(connect());
                        BufferedReader buff = new BufferedReader(ir);
                        FileWriter fr = new FileWriter(f);
                        String line;
                        do {
                            line = buff.readLine();
                            fr.append(line + "\n");
                        } while (line != null);
                        fr.close();
                    } catch (IOException ex) {
                    }
                }
            }
        }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public InputStream readData() {
        InputStream is;
        RSSTitle = openedFileName.getName();
        try {
            is = new FileInputStream(openedFileName);
            return is;
        } catch (IOException E) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setMessage("An I/O error has occurred. We apologixe for the inconvenience").setCancelable(false).setPositiveButton("OK", new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int id) {
                    ReallySimpleReader.this.finish();
                }
            });
            AlertDialog alert = builder.create();
            alert.show();
            return null;
        }
    }
}
