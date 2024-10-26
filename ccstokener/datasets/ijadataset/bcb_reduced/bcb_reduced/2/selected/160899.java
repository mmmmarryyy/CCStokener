package com.jalyoo.recon.net;

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import com.jalyoo.recon.dao.TransactionDAO;
import com.jalyoo.recon.parser.MessageListDOMParser;
import com.jalyoo.recon.parser.MessageObj;

public class GetContentSpider {

    public boolean getContent(String userId, String latestMsgId) {
        try {
            String targetUrl = "http://api.fanfou.com/statuses/user_timeline.xml?id=" + userId + "&since_id=" + latestMsgId;
            URL url = new URL(targetUrl);
            InputStream in = url.openStream();
            ArrayList<MessageObj> list;
            if (in != null) {
                MessageListDOMParser parser = new MessageListDOMParser();
                list = (ArrayList<MessageObj>) parser.parseXML(in);
                TransactionDAO dao = new TransactionDAO();
                dao.insert(list);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static void main(String[] args) {
        try {
            String user = "techbeherca";
            String targetUrl = "http://api.fanfou.com/statuses/user_timeline.xml?id=" + user;
            URL url = new URL(targetUrl);
            InputStream in = url.openStream();
            ArrayList<MessageObj> list;
            if (in != null) {
                MessageListDOMParser parser = new MessageListDOMParser();
                list = (ArrayList<MessageObj>) parser.parseXML(in);
                TransactionDAO dao = new TransactionDAO();
                dao.insert(list);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
