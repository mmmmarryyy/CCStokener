package moler;

import java.applet.Applet;
import java.awt.*;
import java.awt.event.*;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Iterator;
import java.net.*;
import java.io.*;

public class Molerdemo extends Applet implements ActionListener, Runnable {

    public static final String userAgent = "moler 0.1 -- www.fh-kl.de --";

    public static final String JSONusernetwork = "http://del.icio.us/feeds/json/network/";

    public static final String JSONusernetworkappend = "?callback=displayNetwork";

    public static final String JSONuserfans = "http://del.icio.us/feeds/json/fans/";

    public static final String JSONuserfansappend = "?callback=displayFans";

    public static final String JSONusertags = "http://del.icio.us/feeds/json/tags/";

    public static final String JSONusertagsappend = "?callback=displayTags&sort=count";

    public static final String HTMLuserpage = "http://del.icio.us/";

    private static final long serialVersionUID = 1L;

    public static final String SEARCH = "Search";

    public static final String STOP = "Stop";

    public static final String QUIT = "Quit";

    public static final String CLEAR = "clear";

    public static final String DISALLOW = "Disallow:";

    public static final int SEARCH_LIMIT = 1;

    Panel panelMain;

    List listMatches;

    Label labelStatus;

    Thread searchThread;

    TextField textINPUT;

    Choice choiceType;

    private static long _startMs = Long.MIN_VALUE;

    /** Zeitpunkt des letzten Aufrufs von stoppen() [Millisekunden ab 1.1.1970] */
    private static long _stopMs = Long.MIN_VALUE;

    /** Startet die Zeitstoppuhr */
    public static void startTimer() {
        _startMs = System.currentTimeMillis();
    }

    /** Stoppt die Zeitstoppuhr */
    public static void stopTimer() {
        _stopMs = System.currentTimeMillis();
    }

    /**
	 * Liefert Dauer des letzten Stoppintervalls.
	 * 
	 * @return Differenz zwischen dem letzten Stoppzeitpunkt und dem letzten
	 *         Startzeitpunkt in Millisekunden.
	 */
    public static long dauerMs() {
        final long result = _stopMs - _startMs;
        return result;
    }

    public synchronized void sleeper(int i) {
        try {
            listMatches.add("sleep... " + i + " ms");
            Thread.sleep(i);
        } catch (Exception e) {
            System.out.println("threat error: " + e);
            System.exit(-1);
        }
    }

    public String[] splitUserNetwork(String feed) {
        String[] result = null;
        String buffer = null;
        String page = feed;
        if (page.indexOf("[\"") > -1) {
            buffer = page.substring(page.indexOf("[\""), page.indexOf("\"]"));
            buffer = buffer.replace("[\"", "").trim();
            result = buffer.split("\",\"");
        }
        return result;
    }

    public String[] splitUserTags(String feed) {
        String[] result = null;
        String buffer, bufresult = null;
        String page = feed;
        if (page.indexOf("{\"") > -1) {
            buffer = page.substring(page.indexOf("{\""), page.indexOf("}"));
            buffer = buffer.replace("{\"", "").trim();
            bufresult = buffer.replace("\":", " ");
            result = bufresult.split(",\"");
        }
        return result;
    }

    public int getNumberOfPagesToCrawl(String feed) {
        int ende = 0;
        String result[] = null;
        String buffer = null;
        String page = feed;
        if (page.indexOf("<p class=\"pager\">") > -1) {
            buffer = page.substring(page.indexOf("<p class=\"pager\">"));
            buffer = buffer.substring(0, buffer.indexOf("</p>"));
            buffer = buffer.substring(buffer.indexOf("</span>&nbsp; &nbsp; &nbsp;"));
            if (buffer.indexOf("page") > -1) {
                buffer = buffer.substring(buffer.indexOf("page")).replace("page", "");
                result = buffer.split("of");
                ende = Integer.parseInt(result[1].trim());
                System.out.println(ende + " more pages to crawl for this user");
            } else if (buffer.indexOf("showing all") > -1) {
                ende = 0;
                System.out.println("no more pages to crawl for this user");
            } else if (buffer.indexOf("showing the only item") > -1) {
                ende = 0;
                System.out.println("no more pages to crawl for this user");
            } else if (buffer.indexOf("<h3>No items</h3>") > -1) {
                ende = 0;
                System.out.println("no more pages to crawl for this user");
            }
        }
        return ende;
    }

    public int getNumberOfActualPage(String feed) {
        int anfang = 0;
        String result[] = null;
        String buffer = null;
        String page = feed;
        if (page.indexOf("<p class=\"pager\">") > -1) {
            buffer = page.substring(page.indexOf("<p class=\"pager\">"));
            buffer = buffer.substring(0, buffer.indexOf("</p>"));
            buffer = buffer.substring(buffer.indexOf("</span>&nbsp; &nbsp; &nbsp;"));
            if (buffer.indexOf("page") > -1) {
                buffer = buffer.substring(buffer.indexOf("page")).replace("page", "");
                result = buffer.split("of");
                anfang = Integer.parseInt(result[0].trim());
                System.out.println(anfang + " Seite");
            } else if (buffer.indexOf("showing all") > -1) {
                anfang = 1;
                System.out.println(anfang + " Seite");
            } else if (buffer.indexOf("showing the only item") > -1) {
                anfang = 1;
                System.out.println(anfang + " Seite");
            } else if (buffer.indexOf("<h3>No items</h3>") > -1) {
                anfang = 1;
                System.out.println(anfang + " Seite");
            }
        }
        return anfang;
    }

    public String[] splitUserBookmarks(String feed) {
        char prozent = '%';
        System.out.println(prozent);
        int i = 0;
        boolean goon = true;
        String result[] = null;
        String pagebuffer = null;
        String buffer = null;
        String itembuffer[] = new String[100];
        String page = feed;
        if (page.indexOf("class=\"commands\"") > -1) {
            pagebuffer = page;
            while (goon == true) {
                buffer = pagebuffer.substring(pagebuffer.indexOf("class=\"commands\">"));
                itembuffer[i] = buffer.substring(0, buffer.indexOf("</span> </div>"));
                itembuffer[i] = itembuffer[i].substring(itembuffer[i].indexOf("2F" + prozent + "2F"));
                itembuffer[i] = itembuffer[i].replace("2F" + prozent + "2F", "");
                itembuffer[i] = itembuffer[i].substring(0, itembuffer[i].indexOf(prozent + "2F"));
                buffer = buffer.substring(buffer.indexOf("</span> </div>"));
                pagebuffer = buffer;
                pagebuffer.trim();
                if ((pagebuffer.indexOf("class=\"commands\">") == -1)) {
                    goon = false;
                }
                i++;
            }
            result = itembuffer;
        }
        return result;
    }

    public String getUserNetwork(String User) {
        URL url;
        String line, finalstring;
        StringBuffer buffer = new StringBuffer();
        setStatus("Start moling....");
        startTimer();
        try {
            url = new URL(JSONusernetwork + User + JSONusernetworkappend);
            HttpURLConnection connect = (HttpURLConnection) url.openConnection();
            connect.addRequestProperty("User-Agent", userAgent);
            System.out.println("moling: network of " + User);
            BufferedReader input = new BufferedReader(new InputStreamReader(connect.getInputStream()));
            while ((line = input.readLine()) != null) {
                buffer.append(line);
            }
            input.close();
            connect.disconnect();
            stopTimer();
            setStatus("Dauer : " + dauerMs() + " ms");
            finalstring = buffer.toString();
            return finalstring;
        } catch (MalformedURLException e) {
            System.err.println("Bad URL: " + e);
            return null;
        } catch (IOException io) {
            System.err.println("IOException: " + io);
            return null;
        }
    }

    public String getUserTags(String User) {
        URL url;
        String line, finalstring;
        StringBuffer buffer = new StringBuffer();
        setStatus("Start moling....");
        startTimer();
        try {
            url = new URL(JSONusertags + User + JSONusertagsappend);
            HttpURLConnection connect = (HttpURLConnection) url.openConnection();
            connect.addRequestProperty("User-Agent", userAgent);
            System.out.println("moling: tags of " + User);
            BufferedReader input = new BufferedReader(new InputStreamReader(connect.getInputStream()));
            while ((line = input.readLine()) != null) {
                buffer.append(line);
            }
            input.close();
            connect.disconnect();
            stopTimer();
            setStatus("Dauer : " + dauerMs() + " ms");
            finalstring = buffer.toString();
            return finalstring;
        } catch (MalformedURLException e) {
            System.err.println("Bad URL: " + e);
            return null;
        } catch (IOException io) {
            System.err.println("IOException: " + io);
            return null;
        }
    }

    public String getUserFans(String User) {
        URL url;
        String line, finalstring;
        StringBuffer buffer = new StringBuffer();
        setStatus("Start moling....");
        startTimer();
        try {
            url = new URL(JSONuserfans + User + JSONuserfansappend);
            HttpURLConnection connect = (HttpURLConnection) url.openConnection();
            connect.addRequestProperty("User-Agent", userAgent);
            System.out.println("moling: fans of " + User);
            BufferedReader input = new BufferedReader(new InputStreamReader(connect.getInputStream()));
            while ((line = input.readLine()) != null) {
                buffer.append(line);
            }
            input.close();
            connect.disconnect();
            stopTimer();
            setStatus("Dauer : " + dauerMs() + " ms");
            finalstring = buffer.toString();
            return finalstring;
        } catch (MalformedURLException e) {
            System.err.println("Bad URL: " + e);
            return null;
        } catch (IOException io) {
            System.err.println("IOException: " + io);
            return null;
        }
    }

    public String GetUserPage(String User) {
        int page = 1;
        URL url;
        String line, finalstring;
        StringBuffer buffer = new StringBuffer();
        setStatus("Start moling....");
        startTimer();
        try {
            url = new URL(HTMLuserpage + User + "?setcount=100&page=" + page);
            HttpURLConnection connect = (HttpURLConnection) url.openConnection();
            connect.addRequestProperty("User-Agent", userAgent);
            System.out.println("moling: page of " + User);
            BufferedReader input = new BufferedReader(new InputStreamReader(connect.getInputStream()));
            while ((line = input.readLine()) != null) {
                buffer.append(line);
            }
            input.close();
            connect.disconnect();
            stopTimer();
            setStatus("Dauer : " + dauerMs() + " ms");
            finalstring = buffer.toString();
            return finalstring;
        } catch (MalformedURLException e) {
            System.err.println("Bad URL: " + e);
            return null;
        } catch (IOException io) {
            System.err.println("IOException: " + io);
            return null;
        }
    }

    public String GetUserPage(String User, int pagetocrawl) {
        int page = pagetocrawl;
        URL url;
        String line, finalstring;
        StringBuffer buffer = new StringBuffer();
        setStatus("Start moling....");
        startTimer();
        try {
            url = new URL(HTMLuserpage + User + "?setcount=100&page=" + page);
            HttpURLConnection connect = (HttpURLConnection) url.openConnection();
            connect.addRequestProperty("User-Agent", userAgent);
            System.out.println("moling: page " + page + " of " + User);
            BufferedReader input = new BufferedReader(new InputStreamReader(connect.getInputStream()));
            while ((line = input.readLine()) != null) {
                buffer.append(line);
                buffer.append("\n");
            }
            input.close();
            connect.disconnect();
            stopTimer();
            setStatus("Dauer : " + dauerMs() + " ms");
            finalstring = buffer.toString();
            return finalstring;
        } catch (MalformedURLException e) {
            System.err.println("Bad URL: " + e);
            return null;
        } catch (IOException io) {
            System.err.println("IOException: " + io);
            return null;
        }
    }

    public void init() {
        panelMain = new Panel();
        panelMain.setLayout(new BorderLayout(5, 5));
        Panel panelEntry = new Panel();
        panelEntry.setLayout(new BorderLayout(5, 5));
        Panel panelType = new Panel();
        panelType.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        Label labelType = new Label("Search Content: ", Label.RIGHT);
        panelType.add(labelType);
        choiceType = new Choice();
        choiceType.addItem("User/Tags");
        choiceType.addItem("User/Network");
        choiceType.addItem("UserPage");
        panelType.add(choiceType);
        panelEntry.add("North", panelType);
        Label labelType2 = new Label("search in * / find matching * ", Label.RIGHT);
        panelType.add(labelType2);
        Panel panelURL = new Panel();
        panelURL.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
        Label labelURL = new Label("Search String: ", Label.RIGHT);
        panelURL.add(labelURL);
        textINPUT = new TextField("", 40);
        panelURL.add(textINPUT);
        panelEntry.add("South", panelURL);
        panelMain.add("North", panelEntry);
        Panel panelListButtons = new Panel();
        panelListButtons.setLayout(new BorderLayout(5, 5));
        Panel panelList = new Panel();
        panelList.setLayout(new BorderLayout(5, 5));
        Label labelResults = new Label("Search results");
        panelList.add("North", labelResults);
        Panel panelListCurrent = new Panel();
        panelListCurrent.setLayout(new BorderLayout(5, 5));
        listMatches = new List(10);
        ;
        panelListCurrent.add("North", listMatches);
        labelStatus = new Label("");
        panelListCurrent.add("South", labelStatus);
        panelList.add("South", panelListCurrent);
        panelListButtons.add("North", panelList);
        Panel panelButtons = new Panel();
        Button buttonSearch = new Button(SEARCH);
        buttonSearch.addActionListener(this);
        panelButtons.add(buttonSearch);
        Button buttonStop = new Button(STOP);
        buttonStop.addActionListener(this);
        panelButtons.add(buttonStop);
        Button buttonClear = new Button(CLEAR);
        buttonClear.addActionListener(this);
        panelButtons.add(buttonClear);
        Button buttonQuit = new Button(QUIT);
        buttonQuit.addActionListener(this);
        panelButtons.add(buttonQuit);
        panelListButtons.add("South", panelButtons);
        panelMain.add("South", panelListButtons);
        add(panelMain);
        setVisible(true);
        repaint();
    }

    public void start() {
    }

    public void stop() {
        if (searchThread != null) {
            setStatus("stopping...");
            searchThread = null;
        }
    }

    public void destroy() {
    }

    public static void main(String[] args) {
        Frame f = new Frame("Moler V0.1");
        Molerdemo applet = new Molerdemo();
        f.add("Center", applet);
        applet.init();
        applet.start();
        f.pack();
        f.setVisible(true);
    }

    void setStatus(String status) {
        labelStatus.setText(status);
    }

    public void paint(Graphics g) {
        panelMain.paint(g);
        panelMain.paintComponents(g);
    }

    public void actionPerformed(ActionEvent event) {
        String command = event.getActionCommand();
        if (command.compareTo(SEARCH) == 0) {
            setStatus("searching...");
            if (searchThread == null) {
                searchThread = new Thread(this);
            }
            searchThread.start();
        } else if (command.compareTo(STOP) == 0) {
            stop();
        }
        if (command.compareTo(QUIT) == 0) {
            System.exit(1);
        }
        if (command.compareTo(CLEAR) == 0) {
            listMatches.removeAll();
            ;
        }
    }

    public void run() {
        Iterator it;
        Collection<String> coll = new LinkedList<String>();
        int pages = -1;
        String[] result = null;
        String rawresult, bufferresult = new String();
        String user = textINPUT.getText();
        String strTargetType = choiceType.getSelectedItem();
        if (strTargetType.equals("User/Network")) {
            rawresult = getUserNetwork(user);
            result = splitUserNetwork(rawresult);
        } else if (strTargetType.equals("User/Fans")) {
            rawresult = getUserFans(user);
            result = splitUserNetwork(rawresult);
        } else if (strTargetType.equals("User/Tags")) {
            rawresult = getUserTags(user);
            result = splitUserTags(rawresult);
        } else if (strTargetType.equals("UserPage")) {
            rawresult = GetUserPage(user);
            coll.add(rawresult);
            pages = getNumberOfPagesToCrawl(rawresult);
            listMatches.add(Thread.currentThread().getName());
            listMatches.removeAll();
            if (pages > 0) {
                listMatches.add("--------- " + pages + " PAGES TO COLLECT------------");
                listMatches.add("page 1 collected");
                sleeper(1000);
                for (int i = 2; i <= pages; i++) {
                    bufferresult = GetUserPage(user, i);
                    coll.add(bufferresult);
                    listMatches.add("page " + i + " collected");
                    sleeper(1000);
                    if (i == pages) {
                        listMatches.add("--------- FINISHED------------");
                    }
                }
            } else {
                listMatches.add("page collected");
            }
        } else {
            setStatus("not yet....");
            return;
        }
        int counter = 1;
        String bluu;
        String[] bam;
        it = coll.iterator();
        while (it.hasNext()) {
            bluu = (String) it.next();
            getNumberOfActualPage(bluu);
            bam = (String[]) splitUserBookmarks(bluu);
            System.out.println("list of bookmarks :");
            for (int i = 0; i < bam.length; i++) {
                listMatches.add("" + counter);
                listMatches.add(bam[i]);
                System.out.println(i);
                System.out.println(bam[i]);
                counter++;
            }
        }
        if (result != null) {
            for (int i = 0; i < result.length; i++) {
                listMatches.add(result[i]);
            }
        }
        searchThread = null;
    }
}
