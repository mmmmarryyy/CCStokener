package nl.groenlinks.brabant;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import java.net.URL;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ToggleButton;

public class Main extends ListActivity {

    private static final int MENU_ABOUT = 0;

    private static final int MENU_LICENSE = 1;

    private static final int MENU_QUIT = 2;

    private Context context = this;

    private RSSFeed myRssFeed = null;

    private String rssUrl = null;

    private int button = 0;

    private static View progress = null;

    private SAXParserFactory mySAXParserFactory = SAXParserFactory.newInstance();

    private SAXParser mySAXParser;

    private XMLReader myXMLReader;

    private int threadnr = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        progress = findViewById(R.id.progress);
        try {
            mySAXParser = mySAXParserFactory.newSAXParser();
            myXMLReader = mySAXParser.getXMLReader();
        } catch (Exception e) {
            e.printStackTrace();
        }
        button = 0;
        rssUrl = "http://brabant.groenlinks.nl/rss";
        if (savedInstanceState != null) {
            button = savedInstanceState.getInt("button", 0);
            rssUrl = savedInstanceState.getString("rssUrl");
            if (rssUrl == null) rssUrl = "http://brabant.groenlinks.nl/rss";
        }
        loadRSSFeed();
        final ToggleButton nieuws = (ToggleButton) findViewById(R.id.nieuws);
        nieuws.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                button = 0;
                rssUrl = "http://brabant.groenlinks.nl/rss";
                loadRSSFeed();
            }
        });
        final ToggleButton blog = (ToggleButton) findViewById(R.id.blog);
        blog.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                button = 1;
                rssUrl = "http://www.paulsmeulders.nl/nieuw/rss.php";
                loadRSSFeed();
            }
        });
        final ToggleButton video = (ToggleButton) findViewById(R.id.video);
        video.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                button = 2;
                rssUrl = "http://gdata.youtube.com/feeds/base/users/GroenLinksNB/uploads?alt=rss&v=2&orderby=published";
                loadRSSFeed();
            }
        });
        final ToggleButton forum = (ToggleButton) findViewById(R.id.forum);
        forum.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                button = 3;
                rssUrl = "http://feeds.feedburner.com/blog/qYRg";
                loadRSSFeed();
            }
        });
        final ToggleButton challenges = (ToggleButton) findViewById(R.id.challenges);
        challenges.setOnClickListener(new View.OnClickListener() {

            public void onClick(View v) {
                button = 4;
                rssUrl = "http://feeds.feedburner.com/blog/HLeM";
                loadRSSFeed();
            }
        });
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Intent intent = new Intent(this, ShowDetails.class);
        intent.putExtra("link", myRssFeed.getItem(position).getLink());
        intent.putExtra("title", myRssFeed.getItem(position).getTitle());
        intent.putExtra("description", myRssFeed.getItem(position).getDescrHtml());
        intent.putExtra("encoded", myRssFeed.getItem(position).getEncoded());
        intent.putExtra("image", myRssFeed.getItem(position).getImageUrl());
        startActivity(intent);
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        savedInstanceState.putInt("button", button);
        savedInstanceState.putString("rssUrl", rssUrl);
        super.onSaveInstanceState(savedInstanceState);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, MENU_ABOUT, 0, R.string.about).setIcon(android.R.drawable.ic_menu_info_details);
        menu.add(0, MENU_LICENSE, 0, R.string.license).setIcon(android.R.drawable.ic_menu_view);
        menu.add(0, MENU_QUIT, 0, R.string.quit).setIcon(android.R.drawable.ic_menu_close_clear_cancel);
        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case MENU_ABOUT:
                Intent aboutActivity = new Intent(getBaseContext(), About.class);
                startActivity(aboutActivity);
                return true;
            case MENU_LICENSE:
                Intent licenseActivity = new Intent(getBaseContext(), License.class);
                startActivity(licenseActivity);
                return true;
            case MENU_QUIT:
                finish();
                return true;
        }
        return false;
    }

    private void loadRSSFeed() {
        progress.setVisibility(View.VISIBLE);
        setListAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1));
        myRssFeed = null;
        final ToggleButton nieuws = (ToggleButton) findViewById(R.id.nieuws);
        final ToggleButton blog = (ToggleButton) findViewById(R.id.blog);
        final ToggleButton video = (ToggleButton) findViewById(R.id.video);
        final ToggleButton forum = (ToggleButton) findViewById(R.id.forum);
        final ToggleButton challenges = (ToggleButton) findViewById(R.id.challenges);
        nieuws.setChecked(false);
        blog.setChecked(false);
        video.setChecked(false);
        forum.setChecked(false);
        challenges.setChecked(false);
        if (button == 0) nieuws.setChecked(true);
        if (button == 1) blog.setChecked(true);
        if (button == 2) video.setChecked(true);
        if (button == 3) forum.setChecked(true);
        if (button == 4) challenges.setChecked(true);
        Thread thread = new Thread(null, loadRSSAdapter, "loadRSSAdapter");
        thread.start();
    }

    private Runnable loadRSSAdapter = new Runnable() {

        public void run() {
            myRssFeed = null;
            while (threadnr != 0) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            threadnr++;
            String currentUrl = rssUrl;
            try {
                URL url = new URL(currentUrl);
                RSSHandler myRSSHandler = new RSSHandler(context);
                myXMLReader.setContentHandler(myRSSHandler);
                InputSource myInputSource = new InputSource(url.openStream());
                myXMLReader.parse(myInputSource);
                threadnr = 0;
                if (currentUrl.contentEquals(rssUrl)) {
                    myRssFeed = myRSSHandler.getFeed();
                    myInputSource = null;
                    myRSSHandler = null;
                } else {
                    myInputSource = null;
                    myRSSHandler = null;
                    return;
                }
            } catch (Exception e) {
                return;
            }
            runOnUiThread(setRSSAdapter);
        }
    };

    private Runnable setRSSAdapter = new Runnable() {

        public void run() {
            if (myRssFeed != null) {
                setListAdapter(new RSSAdapter(context, myRssFeed.getList()));
            }
            progress.setVisibility(View.GONE);
        }
    };
}
