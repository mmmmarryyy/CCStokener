package org.mrh.JMrTools.telnet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Timer;
import org.apache.log4j.Logger;
import org.mrh.JMrTools.Execute;
import org.mrh.JMrTools.Options;
import org.mrh.JMrTools.Config.Host;
import thor.net.TelnetURLConnection;

/**
 * @author ed_perry
 *
 */
public class TelnetExecute extends Execute {

    private static Logger log = Options.logger;

    private Timer timer = new Timer();

    public TelnetExecute(Host h, String cmd) {
        host = h;
        command = cmd += "\n";
        Options.logger.debug("Creating TelnetExec for '" + host.address + "'");
    }

    @Override
    public void run() {
        String prompt = host.options.get("PROMPT") != null ? (String) host.options.get("PROMPT") : ".*\\$$";
        try {
            URL url = new URL("telnet", host.address, Integer.parseInt(host.port), "", new thor.net.URLStreamHandler());
            URLConnection urlConnection = url.openConnection();
            urlConnection.connect();
            ((TelnetURLConnection) urlConnection).setTelnetTerminalHandler(new SimpleTelnetHandler());
            OutputStream out = urlConnection.getOutputStream();
            InputStream in = urlConnection.getInputStream();
            login(in, out);
            output += command;
            out.write(command.getBytes());
            WaitFor rw = new WaitFor(in, prompt);
            output += rw.output;
        } catch (MalformedURLException e) {
            log.error(e.getMessage(), e);
        } catch (IOException e) {
            log.error(e.getMessage(), e);
        }
    }

    public void login(InputStream in, OutputStream out) {
        host.userName += "\n";
        host.passWord += "\n";
        String loginPrompt = (String) host.options.get("LOGINPROMPT");
        String passwdPrompt = (String) host.options.get("LOGINPASSWORDPROMPT");
        String prompt = host.options.get("PROMPT") != null ? (String) host.options.get("PROMPT") : ".*\\$$";
        WaitFor rw = new WaitFor(in, loginPrompt);
        output += rw.output;
        try {
            rw.output += host.userName;
            out.write(host.userName.getBytes());
            rw = new WaitFor(in, passwdPrompt);
            output += rw.output;
            out.write(host.passWord.getBytes());
            output += "********\n";
            rw = new WaitFor(in, prompt);
            output += rw.output;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String call() throws Exception {
        return output;
    }

    /**
	 * 
	 */
    public TelnetExecute() {
    }
}
