package crawler;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HostConfiguration;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.URI;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.methods.GetMethod;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.User;

public class Snowball {

    public Twitter twitter;

    public int counterNodes = 0;

    public int hits = 0;

    HttpClient client;

    GetMethod get;

    HostConfiguration host;

    public static final String TWITTER_USERNAME = "marcelote";

    public static final String TWITTER_PASSWORD = "cam007";

    public static final int MAX_NUMBER_OF_CENTRAL_NODES = 400;

    public static final String FILE_NAME_PREFIX = "friends_of_";

    public Snowball() {
        try {
            authenticate();
            twitter = new Twitter(TWITTER_USERNAME, TWITTER_PASSWORD);
            bfsCrawl();
            System.out.println("n=" + ct);
            System.out.println("label:\n" + labels);
            System.out.println("data:\n" + ties);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void authenticate() throws URIException, NullPointerException {
        System.out.println("authenticating...");
        String twitterhost = "www.twitter.com";
        if ((TWITTER_USERNAME == null) || (TWITTER_PASSWORD == null)) {
            throw new RuntimeException("User and/or password has not been initialized!");
        }
        client = new HttpClient();
        Credentials credentials = new UsernamePasswordCredentials(TWITTER_USERNAME, TWITTER_PASSWORD);
        client.getState().setCredentials(new AuthScope(twitterhost, 80, AuthScope.ANY_REALM), credentials);
        host = client.getHostConfiguration();
        host.setHost(new URI("http://" + twitterhost, true));
        get = new GetMethod();
        System.out.println("authentication successful!");
    }

    public void fetchAuthenticatedContent(int id) throws NullPointerException, HttpException, IOException {
        String url = "/statuses/followers.xml?id=" + id;
        String fileName = FILE_NAME_PREFIX + id + ".xml";
        java.io.File f = new java.io.File(fileName);
        if (f.exists()) {
            System.out.println("user: " + id + " not fetched because it already exists");
            return;
        }
        get.setURI(new URI(url, true));
        client.executeMethod(host, get);
        hits--;
        InputStream in = get.getResponseBodyAsStream();
        OutputStream out = System.out;
        int i = 0;
        while ((i = in.read()) != -1) {
            out.write(i);
        }
        in.close();
        out.close();
    }

    public void fetchPublicContent(int id) throws IOException {
        String fileName = FILE_NAME_PREFIX + id + ".xml";
        File file = new File(fileName);
        if (file.exists()) {
            System.out.println("user: " + id + " not fetched because it already exists");
            return;
        }
        OutputStream out = new FileOutputStream(file, false);
        URL url = new URL("http://twitter.com/statuses/followers.xml?id=" + id);
        URLConnection conn = url.openConnection();
        InputStream in = conn.getInputStream();
        int i = 0;
        while ((i = in.read()) != -1) {
            out.write(i);
        }
        in.close();
        out.close();
    }

    String ties = "";

    String labels = "";

    int ct = 0;

    public void bfsCrawl() throws TwitterException, InterruptedException {
        System.out.println("initiating crawler...");
        List nodes = new ArrayList();
        nodes.add(twitter.getAuthenticatedUser());
        hits = twitter.rateLimitStatus().getRemainingHits();
        System.out.println("remaining hits: " + hits);
        int countInterator = 0;
        while (nodes.iterator().hasNext()) {
            try {
                User u = (User) nodes.iterator().next();
                System.out.println("fetching: " + u.getName() + " | ID: " + u.getId());
                labels += u.getId() + ", ";
                ct++;
                nodes.remove(u);
                while (hits <= 2) {
                    Thread.currentThread().sleep(30 * 1000);
                    hits = twitter.rateLimitStatus().getRemainingHits();
                }
                if (counterNodes < MAX_NUMBER_OF_CENTRAL_NODES) {
                    List<User> friends = twitter.getFollowers("" + u.getId());
                    counterNodes += friends.size();
                    ties += u.getId();
                    for (User us : friends) {
                        ties += " " + (us.getId());
                    }
                    ties += "\n";
                    nodes.addAll(friends);
                    hits--;
                }
                if (countInterator % 10 == 0) {
                }
                countInterator++;
            } catch (Exception e) {
                e.printStackTrace();
                Thread.currentThread().sleep(30 * 1000);
            }
        }
    }

    public static void main(String args[]) {
        new Snowball();
    }
}
