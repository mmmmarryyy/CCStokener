package com.homeautomate.commander;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import org.apache.log4j.Logger;

public class CommandLine {

    static Logger log = Logger.getLogger(CommandLine.class);

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        log.info("Passage paramètres " + args);
        try {
            URL url = new URL("http://localhost:8082/HomeAutomateCore/ListenerServlet/");
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));
            out.write("username = JavaWorld \r \n ");
            out.flush();
            out.close();
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String reponse = "";
            while ((reponse = in.readLine()) != null) {
                System.out.println(reponse);
            }
            System.out.println("Fin");
            in.close();
        } catch (MalformedURLException ex) {
            ex.printStackTrace();
            System.out.println("Fin1 " + ex.getMessage() + " " + ex.getCause());
        } catch (IOException ex) {
            ex.printStackTrace();
            System.out.println("Fin2 " + ex.getMessage() + " " + ex.getCause());
        }
    }
}
