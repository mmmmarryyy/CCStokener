package org.rjam.alert.action;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import javax.swing.JComponent;
import javax.swing.JOptionPane;
import org.rjam.alert.Rule;
import org.rjam.gui.api.IAlert;
import org.rjam.gui.api.IReport;
import org.rjam.gui.beans.Row;
import org.rjam.xml.Token;
import org.rjam.report.xml.Transformer;

public class ActionWebService extends Action {

    private String url;

    private String lastResult;

    private int timeout = 60;

    public ActionWebService() {
        super("WebService", "Send a 'POST' notification via a Web Service");
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getLastResult() {
        return lastResult;
    }

    @Override
    public Token toXml() {
        Token ret = super.toXml();
        ret.addChild(new Token(Transformer.TOKEN_URL, getUrl()));
        ret.addChild(new Token(Transformer.TOKEN_TIMEOUT, "" + getTimeout()));
        return ret;
    }

    @Override
    public void setValues(Token tok) {
        super.setValues(tok);
        setUrl(Transformer.getValue(tok, Transformer.TOKEN_URL));
        String tmp = Transformer.getValue(tok, Transformer.TOKEN_TIMEOUT);
        if (tmp != null) {
            try {
                setTimeout(Integer.parseInt(tmp));
            } catch (Exception e) {
            }
        }
    }

    @Override
    public JComponent getEditComponent() {
        return new EditActionWebService(this);
    }

    @Override
    public void execute(IAlert alert, IReport report, Rule rule, Row row) {
        try {
            URL url = new URL(getUrl());
            URLConnection con = url.openConnection();
            con.setConnectTimeout(getTimeout());
            con.setDoOutput(true);
            OutputStream out = con.getOutputStream();
            out.write(formatOutput(report, alert, rule, row).getBytes());
            BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
            StringBuilder input = new StringBuilder();
            String line = null;
            while ((line = in.readLine()) != null) {
                input.append(line);
                input.append('\n');
            }
            in.close();
            this.lastResult = input.toString();
        } catch (Throwable e) {
            logError("Error sending alert", e);
            if (!isHeadless()) {
                alert.setEnabled(false);
                JOptionPane.showMessageDialog(null, "Can't send alert " + e + "\n" + alert.getName() + " alert disabled.", "Action Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
