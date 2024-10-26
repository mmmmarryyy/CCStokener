package net.blogbotplatform.blogbot;

import java.io.*;
import java.net.*;

public class TwitterBrigde {

    long twitterTimer;

    int TWITTER_INTERVAL = 1000;

    private static String userPassword;

    private static String encoding;

    TwitterBrigde() {
        twitterTimer = System.currentTimeMillis() - TWITTER_INTERVAL;
        setTwitterAccount();
    }

    static void setTwitterAccount() {
        userPassword = BlogBotShop.twitterUsername + ":" + BlogBotShop.twitterPassword;
        encoding = new sun.misc.BASE64Encoder().encode(userPassword.getBytes());
    }

    boolean checkAlive() {
        System.out.println("Checking your Authentication");
        String twitterOutput = "";
        try {
            URL url = new URL("http://www.twitter.com/account/verify_credentials");
            URLConnection myConnection = url.openConnection();
            myConnection.setRequestProperty("Authorization", "Basic " + encoding);
            myConnection.setDoInput(true);
            myConnection.setDoOutput(true);
            BufferedReader in = new BufferedReader(new InputStreamReader(myConnection.getInputStream()));
            String inputLine;
            System.out.println("connection: " + myConnection.toString());
            while (true) {
                System.out.println("reading line..");
                inputLine = in.readLine();
                if (inputLine == null) {
                    System.out.println("nothing there anymore");
                    break;
                }
                System.out.println("hello" + inputLine);
                twitterOutput += inputLine;
            }
            in.close();
        } catch (MalformedURLException me) {
            System.out.println("MalformedURLException: " + me);
        } catch (IOException ioe) {
            System.out.println("IOException: " + ioe);
        }
        if (twitterOutput.equals("Authorized")) {
            System.out.println("Twitter Authutication had been Verifyed");
            return true;
        } else {
            System.out.println("Access to Twitter API has been Denied");
            return false;
        }
    }

    void update(String whichText) {
        System.out.println("Making a Twitter update");
        if (System.currentTimeMillis() < twitterTimer + TWITTER_INTERVAL) {
            System.out.println("Too Quick! Post Slower!");
            return;
        }
        String updateText = "status=" + whichText;
        String twitterOutput = "";
        try {
            URL url = new URL("http://twitter.com/statuses/update.xml");
            URLConnection myConnection = url.openConnection();
            myConnection.setRequestProperty("Authorization", "Basic " + encoding);
            myConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            myConnection.setRequestProperty("Content-Length", "" + Integer.toString(updateText.getBytes().length));
            myConnection.setDoInput(true);
            myConnection.setDoOutput(true);
            DataOutputStream dos;
            dos = new DataOutputStream(myConnection.getOutputStream());
            dos.writeBytes(updateText);
            dos.flush();
            dos.close();
            DataInputStream dis;
            String inputLine;
            dis = new DataInputStream(myConnection.getInputStream());
            while ((inputLine = dis.readLine()) != null) {
                System.out.println(inputLine);
                twitterOutput += inputLine;
            }
            dis.close();
        } catch (MalformedURLException me) {
            System.out.println("MalformedURLException: " + me);
        } catch (IOException ioe) {
            System.out.println("IOException: " + ioe);
        }
    }
}
