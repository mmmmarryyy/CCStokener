package org.bpaul.rtalk.thread;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.MessageFormat;
import java.util.List;
import javax.swing.SwingWorker;
import org.bpaul.rtalk.protocol.NickResponse;
import org.bpaul.rtalk.protocol.NickResponseParser;
import org.bpaul.rtalk.ui.PostLogin;

public class NickChangeTask extends SwingWorker<NickResponse, String> {

    private String strurl;

    public NickChangeTask(String newNick, String userid, String url, String session, String ip) {
        this.strurl = MessageFormat.format(url, new Object[] { userid, session, newNick, ip });
    }

    public NickResponse doInBackground() {
        updateStatus("1");
        BufferedReader reader = null;
        URL url = null;
        StringBuffer buff;
        try {
            url = new URL(strurl);
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String line = null;
            buff = new StringBuffer();
            while ((line = reader.readLine()) != null) {
                buff.append(line);
                buff.append("\n");
            }
            NickResponse resp = NickResponseParser.parse(buff.toString());
            return resp;
        } catch (IOException e) {
            updateStatus("2");
            return null;
        } finally {
            try {
                if (reader != null) reader.close();
            } catch (Exception e) {
            }
        }
    }

    public void process(List<String> msgs) {
        for (String msg : msgs) {
            PostLogin.getInstance().getUiUpdater().updateNickChange(Integer.parseInt(msg));
        }
    }

    private void updateStatus(String state) {
        publish(state);
    }

    public void done() {
        try {
            NickResponse resp = get();
            if (resp != null) {
                PostLogin.getInstance().getUiUpdater().updateNickChange(resp);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
