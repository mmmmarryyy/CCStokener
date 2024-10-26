package moler;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;
import moleDTO.UserDTO;
import moleDTO.UserPageDTO;
import parser.Parser;
import stuff.DataDonkey;

public class Crawler {

    public static final String userAgent = "moler 0.3 -- www.fh-kl.de --";

    public static String thread = new String();

    public static String[] proxy = new String[] { "http://www.coolestsite.info/cgiproxy/nph-proxy.pl/000100A/http/", "http://www.backdoorproxy.com/cgi-bin/nph-proxy.pl/000100A/http/", "http://myspacewaiter.com/nph-proxy.cgi/000100A/http/", "http://myspaceproxy.gr/nph-proxy.cgi/000100A/http/", "http://www.tunnelsurfing.com/cgiproxy/nph-proxy.pl/000100A/http/", "http://www.dawwar.net/cgi.cgi/000100A/http/" };

    public static final String HTMLuserpage = "del.icio.us/";

    public static final String JSONusernetwork = HTMLuserpage + "feeds/json/network/";

    public static final String JSONusernetworkappend = "?callback=displayNetwork";

    public static final String JSONuserfans = HTMLuserpage + "feeds/json/fans/";

    public static final String JSONuserfansappend = "?callback=displayFans";

    public static final String JSONusertags = HTMLuserpage + "feeds/json/tags/";

    public static final String JSONusertagsappend = "?callback=displayTags&sort=count";

    public static UserPageDTO GetAllUserPages(DataDonkey datadonkey, UserDTO User, UserPageDTO userpages) {
        UserPageDTO DTOuserpage = userpages;
        UserPageDTO DTOuserpagebuffer = new UserPageDTO();
        int maxpagestocrawl = -1;
        int crawledpages = 0;
        try {
            DTOuserpagebuffer = GetSingleUserPage(datadonkey, User, DTOuserpage, 1);
            DTOuserpagebuffer.getPages().iterator().next().toString().length();
            DTOuserpage = DTOuserpagebuffer;
            crawledpages = DTOuserpage.getLastcrawledpage();
            maxpagestocrawl = DTOuserpage.getMaxpages();
            System.out.println(maxpagestocrawl);
            while (maxpagestocrawl > 0 && crawledpages < maxpagestocrawl) {
                sleeper(1500);
                DTOuserpagebuffer = GetSingleUserPage(datadonkey, User, DTOuserpage, crawledpages + 1);
                DTOuserpagebuffer.getPages().iterator().next().toString().length();
                DTOuserpage = DTOuserpagebuffer;
                crawledpages = DTOuserpage.getLastcrawledpage();
            }
        } catch (FileNotFoundException fnf) {
            System.err.println("IOException: " + fnf);
            fnf.printStackTrace();
            if (DTOuserpage.getMaxpages() > 0) {
                DTOuserpage.setMaxpages(DTOuserpage.getLastcrawledpage());
            } else DTOuserpage.setMaxpages(0);
            DTOuserpage.setUserpagesmoled(true);
            DTOuserpage.setScrewedup(false);
            return DTOuserpage;
        } catch (Exception e) {
            e.printStackTrace();
            if (crawledpages > 0) {
                System.out.println("Userpage download interrupted");
                DTOuserpage.setMaxpages(maxpagestocrawl);
                DTOuserpage.setLastcrawledpage(crawledpages);
                DTOuserpage.setScrewedup(true);
                return DTOuserpage;
            } else {
                System.out.println("crawledpages is 0");
                DTOuserpage.setScrewedup(true);
                return DTOuserpage;
            }
        }
        DTOuserpage.setScrewedup(false);
        return DTOuserpage;
    }

    public static UserPageDTO GetTheRestOfTheUsersPages(DataDonkey datadonkey, UserDTO User, UserPageDTO userpages) {
        UserPageDTO DTOuserpage = userpages;
        UserPageDTO DTOuserpagebuffer = new UserPageDTO();
        int maxpagestocrawl = userpages.getMaxpages();
        int crawledpages = DTOuserpage.getLastcrawledpage();
        try {
            sleeper(5000);
            if (maxpagestocrawl <= 0 || crawledpages == 0) {
                DTOuserpagebuffer = GetSingleUserPage(datadonkey, User, DTOuserpage, 1);
                DTOuserpagebuffer.getPages().iterator().next().toString().length();
                DTOuserpage = DTOuserpagebuffer;
                crawledpages = DTOuserpage.getLastcrawledpage();
                maxpagestocrawl = DTOuserpage.getMaxpages();
            }
            while (maxpagestocrawl > 0 && crawledpages < maxpagestocrawl) {
                sleeper(1500);
                DTOuserpagebuffer = GetSingleUserPage(datadonkey, User, DTOuserpage, crawledpages + 1);
                DTOuserpagebuffer.getPages().iterator().next().toString().length();
                DTOuserpage = DTOuserpagebuffer;
                crawledpages = DTOuserpage.getLastcrawledpage();
            }
        } catch (Exception e) {
            if (crawledpages > 0) {
                System.out.println("resumed Userpage download interrupted");
                DTOuserpage.setMaxpages(maxpagestocrawl);
                DTOuserpage.setLastcrawledpage(crawledpages);
                DTOuserpage.setScrewedup(true);
                return DTOuserpage;
            } else {
                System.out.println("crawledpages is 0");
                DTOuserpage.setScrewedup(true);
                return DTOuserpage;
            }
        }
        DTOuserpage.setScrewedup(false);
        return DTOuserpage;
    }

    /**
	 * @param User
	 * @param pagetocrawl
	 * @return a String containing the crawled page
	 */
    public static UserPageDTO GetSingleUserPage(DataDonkey datadonkey, UserDTO User, UserPageDTO userpage, int pagetocrawl) throws FileNotFoundException {
        UserPageDTO thisuserpage = userpage;
        Collection<String> DTOpage = new LinkedList<String>();
        int page = pagetocrawl;
        URL url;
        String line, finalstring = null;
        StringBuffer buffer = new StringBuffer();
        try {
            thread = Thread.currentThread().getName();
            int t = Integer.parseInt(thread);
            System.out.println("WebMoleThread " + t + " using proxy: " + proxy[t]);
            url = new URL(proxy[t] + HTMLuserpage + User.getUserName() + "?setcount=100&page=" + page);
            HttpURLConnection connect = (HttpURLConnection) url.openConnection();
            connect.addRequestProperty("User-Agent", userAgent);
            System.out.println("moling: page " + page + " of " + User.getUserName());
            BufferedReader input = new BufferedReader(new InputStreamReader(connect.getInputStream()));
            while ((line = input.readLine()) != null) {
                buffer.append(line);
            }
            input.close();
            connect.disconnect();
            finalstring = buffer.toString();
            finalstring.length();
            DTOpage.add(finalstring);
            thisuserpage.setPages(DTOpage);
            thisuserpage.setLastcrawledpage(pagetocrawl);
            if (thisuserpage.getMaxpages() <= 0) {
                thisuserpage.setMaxpages(Parser.getNumberOfPagesToCrawl(finalstring));
            }
            if (thisuserpage.getMaxpages() == pagetocrawl) {
                thisuserpage.setFinished(true);
            }
            datadonkey.parseUsersPages(User, thisuserpage);
            return thisuserpage;
        } catch (FileNotFoundException fnf) {
            System.err.println("IOException: " + fnf);
            fnf.printStackTrace();
            if (thisuserpage.getMaxpages() > 0) {
                thisuserpage.setMaxpages(thisuserpage.getLastcrawledpage());
            } else thisuserpage.setMaxpages(0);
            thisuserpage.setUserpagesmoled(true);
            thisuserpage.setScrewedup(false);
            DTOpage.add("");
            thisuserpage.setPages(DTOpage);
            return thisuserpage;
        } catch (MalformedURLException e) {
            System.err.println("Bad URL: " + e);
            return null;
        } catch (IOException io) {
            System.err.println("IOException: " + io);
            return null;
        } catch (Exception e) {
            System.err.println("Exception: " + e);
            return null;
        }
    }

    /**
	 * @param User
	 * @return a String containing the the UserNetwork JSON feed of a certain
	 *         User
	 */
    public static String getUserNetwork(String User) {
        URL url;
        String line, finalstring;
        StringBuffer buffer = new StringBuffer();
        try {
            thread = Thread.currentThread().getName();
            int t = Integer.parseInt(thread);
            System.out.println("WebMoleThread " + t + " using proxy: " + proxy[t]);
            url = new URL(proxy[t] + JSONusernetwork + User + JSONusernetworkappend);
            HttpURLConnection connect = (HttpURLConnection) url.openConnection();
            connect.addRequestProperty("User-Agent", userAgent);
            System.out.println("moling: network of " + User);
            BufferedReader input = new BufferedReader(new InputStreamReader(connect.getInputStream()));
            while ((line = input.readLine()) != null) {
                buffer.append(line);
            }
            input.close();
            connect.disconnect();
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

    /**
	 * @param User
	 * @return a String containing the Tags JSON feed of a certain User
	 */
    public static String getUserTags(String User) {
        URL url;
        String line, finalstring;
        StringBuffer buffer = new StringBuffer();
        try {
            thread = Thread.currentThread().getName();
            int t = Integer.parseInt(thread);
            System.out.println("WebMoleThread " + t + " using proxy: " + proxy[t]);
            url = new URL(proxy[t] + JSONusertags + User + JSONusertagsappend);
            HttpURLConnection connect = (HttpURLConnection) url.openConnection();
            connect.addRequestProperty("User-Agent", userAgent);
            System.out.println("moling: tags of " + User);
            BufferedReader input = new BufferedReader(new InputStreamReader(connect.getInputStream()));
            while ((line = input.readLine()) != null) {
                buffer.append(line);
            }
            input.close();
            connect.disconnect();
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

    /**
	 * @param User
	 * @return a String containing the the Fans JSON feed of a certain User
	 */
    public static String getUserFans(String User) {
        URL url;
        String line, finalstring;
        StringBuffer buffer = new StringBuffer();
        try {
            thread = Thread.currentThread().getName();
            int t = Integer.parseInt(thread);
            System.out.println("WebMoleThread " + t + " using proxy: " + proxy[t]);
            url = new URL(proxy[t] + JSONuserfans + User + JSONuserfansappend);
            HttpURLConnection connect = (HttpURLConnection) url.openConnection();
            connect.addRequestProperty("User-Agent", userAgent);
            System.out.println("moling: fans of " + User);
            BufferedReader input = new BufferedReader(new InputStreamReader(connect.getInputStream()));
            while ((line = input.readLine()) != null) {
                buffer.append(line);
            }
            input.close();
            connect.disconnect();
            finalstring = buffer.toString();
            return finalstring;
        } catch (MalformedURLException e) {
            System.err.println("Bad URL: " + e);
            return null;
        } catch (Exception e) {
            System.err.println("Exception: " + e);
            return null;
        }
    }

    public static synchronized void sleeper(int i) {
        try {
            System.out.println(Thread.currentThread().getName() + " sleeps... " + i + " ms\n");
            Thread.sleep(i);
        } catch (InterruptedException e) {
            System.out.println("Thread " + Thread.currentThread().getName() + "interrupted : " + e);
        }
    }
}
