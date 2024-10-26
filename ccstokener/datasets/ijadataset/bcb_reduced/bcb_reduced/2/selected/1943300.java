package net.sf.zorobot.util;

import java.io.*;
import java.net.*;
import java.util.*;
import net.sf.zorobot.core.Message;
import net.sf.zorobot.core.MessageListener;
import net.sf.zorobot.core.ZorobotSystem;
import net.sf.zorobot.tr.ParseTreeNode;
import net.sf.zorobot.tr.Symbol;

public class ZtreeUploader extends Thread {

    int channelId, msgId;

    Symbol[] sym;

    String stc;

    MessageListener output;

    public ZtreeUploader(int channelId, int msgId, Symbol[] rev, String stc, MessageListener output) {
        this.channelId = channelId;
        this.msgId = msgId;
        this.sym = rev;
        this.stc = stc;
        this.output = output;
    }

    public void run() {
        ArrayList str = prepare(sym);
        String res = uploadZtree(str);
        if (res != null) {
            if (res.length() < 10) {
                output.message(channelId, msgId, null, new Message(Message.COLOR1, ZorobotSystem.props.getProperty("zoro.url") + "ztree.jsp?id=" + res));
            } else {
                output.message(channelId, msgId, null, new Message(Message.COLOR1, "Please wait while zorobot is starting up the web server..."));
                boolean s = ServerStarter.startServer();
                if (s) {
                    try {
                        Thread.sleep(6000);
                    } catch (Exception uuu) {
                    }
                    String rus = uploadZtree(str);
                    if (rus != null && rus.length() < 10) {
                        output.message(channelId, msgId, null, new Message(Message.COLOR1, "Ztree is ready: " + ZorobotSystem.props.getProperty("zoro.url") + "ztree.jsp?id=" + rus));
                    } else {
                        output.message(channelId, msgId, null, new Message(Message.COLOR1, "Ztree cannot be created at this moment. Please try again later."));
                    }
                } else {
                    output.message(channelId, msgId, null, new Message(Message.COLOR1, "Ztree cannot be created at this moment. Please try again later."));
                }
            }
        } else {
            output.message(channelId, msgId, null, new Message(Message.COLOR1, "Ztree cannot be created at this moment. Please try again later."));
        }
    }

    public ArrayList prepare(Symbol[] s) {
        ArrayList retVal = new ArrayList();
        if (s.length == 1) {
            ParseTreeNode pn = s[0].getNode();
            prepareStub(retVal, pn, 0);
        } else {
            retVal.add("n1|root|" + stc);
            for (int i = 0; i < s.length; i++) {
                ParseTreeNode pn = s[i].getNode();
                prepareStub(retVal, pn, 1);
            }
        }
        return retVal;
    }

    public void prepareStub(ArrayList c, ParseTreeNode pn, int parent) {
        int id = c.size() + 1;
        System.out.println(pn.clz);
        StringBuilder sb = new StringBuilder();
        sb.append("n");
        sb.append(id);
        sb.append("|");
        if (parent == 0) sb.append("root"); else {
            sb.append("n");
            sb.append(parent);
        }
        sb.append("|");
        sb.append("<b>");
        sb.append(pn.jap);
        sb.append("</b>");
        if (pn.tag != null) {
            sb.append(":");
            sb.append("<small><i>");
            sb.append(pn.tag);
            sb.append("</i></small>");
        }
        sb.append(" ");
        sb.append(pn.eng);
        c.add(sb.toString());
        for (int i = 0; i < pn.children.size(); i++) {
            prepareStub(c, (ParseTreeNode) pn.children.get(i), id);
        }
    }

    public String uploadZtree(ArrayList c) {
        try {
            String id = generateRandomId();
            Iterator iter = c.iterator();
            URL url = new URL(ZorobotSystem.props.getProperty("zoro.url") + "auplo1.jsp");
            URLConnection connection = url.openConnection();
            connection.setDoOutput(true);
            PrintWriter out = new PrintWriter(connection.getOutputStream());
            out.print("id=" + id + "&");
            StringBuffer sb = new StringBuffer();
            int gg = 0;
            while (iter.hasNext()) {
                if (gg++ >= 500) break;
                String st = (String) iter.next();
                sb.append("a=");
                sb.append(URLEncoder.encode(st, "UTF-8"));
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
