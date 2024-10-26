package net.sf.zorobot.util;

import java.io.*;
import java.net.*;
import java.util.*;
import net.sf.zorobot.core.Message;
import net.sf.zorobot.core.MessageListener;
import net.sf.zorobot.core.Question;
import net.sf.zorobot.core.ZorobotSystem;
import net.sf.zorobot.game.ZorobotGame;
import be.trc.core.*;

public class ReportUploader extends Thread {

    ZorobotGame game;

    Collection rev;

    int channelId, msgId;

    MessageListener output;

    public ReportUploader(ZorobotGame game, int channelId, int msgId, MessageListener output, Collection rev) {
        this.game = game;
        this.rev = rev;
        this.channelId = channelId;
        this.msgId = msgId;
        this.output = output;
    }

    public void run() {
        String res = uploadReport(rev);
        if (res != null) {
            if (res.length() < 10) {
                game.reportId = res;
                output.message(channelId, msgId, null, new Message(Message.COLOR1, "Review report is ready: " + ZorobotSystem.props.getProperty("zoro.url") + "review.jsp?id=" + res));
            } else {
                output.message(channelId, msgId, null, new Message(Message.COLOR1, "Please wait while zorobot is starting up the web server..."));
                boolean s = ServerStarter.startServer();
                if (s) {
                    try {
                        Thread.sleep(5000);
                    } catch (Exception uuu) {
                    }
                    String rus = uploadReport(rev);
                    if (rus != null && rus.length() < 10) {
                        game.reportId = res;
                        output.message(channelId, msgId, null, new Message(Message.COLOR1, "Review report is ready: " + ZorobotSystem.props.getProperty("zoro.url") + "review.jsp?id=" + rus));
                    } else {
                        output.message(channelId, msgId, null, new Message(Message.COLOR1, "Report cannot be prepared at this moment. You may save the game and try again later."));
                    }
                } else {
                    output.message(channelId, msgId, null, new Message(Message.COLOR1, "Report cannot be prepared at this moment. You may save the game and try again later."));
                }
            }
        } else {
            output.message(channelId, msgId, null, new Message(Message.COLOR1, "Report cannot be prepared at this moment. You may save the game and try again later."));
        }
    }

    public String uploadReport(Collection c) {
        try {
            String id = generateRandomId();
            Iterator iter = c.iterator();
            URL url = new URL(ZorobotSystem.props.getProperty("zoro.url") + "auplo.jsp");
            URLConnection connection = url.openConnection();
            connection.setDoOutput(true);
            PrintWriter out = new PrintWriter(connection.getOutputStream());
            out.print("id=" + id + "&");
            StringBuffer sb = new StringBuffer();
            int gg = 0;
            while (iter.hasNext()) {
                if (gg++ >= 500) break;
                Question tq = (Question) iter.next();
                sb.append("a=");
                sb.append(URLEncoder.encode(tq.question, "UTF-8"));
                sb.append("*");
                sb.append(tq.familiarityLevel);
                sb.append("*");
                sb.append(tq.hitCount);
                sb.append("*");
                sb.append(tq.allCount - tq.hitCount);
                sb.append("*");
                StringBuffer ss = new StringBuffer();
                String[] ans;
                if (tq.ansDisplay != null) {
                    ans = tq.ansDisplay;
                } else {
                    ans = tq.answer;
                }
                for (int j = 0; j < ans.length; j++) {
                    if (j > 0) ss.append(", ");
                    ss.append(ans[j]);
                }
                sb.append(URLEncoder.encode(ss.toString(), "UTF-8"));
                sb.append("*");
                if (tq.winner != null) sb.append(tq.winner);
                if (iter.hasNext() && gg < 500) sb.append("&");
            }
            out.println(sb.toString());
            out.close();
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            if ((inputLine = in.readLine()) != null) {
                if (!inputLine.equals("OK!") && inputLine.length() > 3) {
                    System.out.println("Not OK: " + inputLine);
                    return "xxxxxxxxxx";
                }
            }
            in.close();
            return id;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String generateRandomId() {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < 5; i++) {
            char x = (char) ('a' + (int) (Math.random() * 26));
            sb.append(x);
        }
        return sb.toString();
    }
}
