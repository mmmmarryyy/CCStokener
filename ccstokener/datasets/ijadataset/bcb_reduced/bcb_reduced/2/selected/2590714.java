package org.cydonia.ip;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.cydonia.ip.interfaces.CaptureIp;

/**
 *
 * @author franklin
 */
public class ExternalIp implements CaptureIp {

    public ExternalIp() {
    }

    public String getIpAddress() {
        try {
            URL url = new URL("http://checkip.dyndns.org");
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String linha;
            String rtn = "";
            while ((linha = in.readLine()) != null) rtn += linha;
            ;
            in.close();
            return filtraRetorno(rtn);
        } catch (IOException ex) {
            Logger.getLogger(ExternalIp.class.getName()).log(Level.SEVERE, null, ex);
            return "ERRO.";
        }
    }

    private String filtraRetorno(String str) {
        String rtn = str.replaceAll("<html><head><title>Current IP Check</title></head><body>Current IP Address: ", "");
        rtn = rtn.replaceAll("</body></html>", "");
        return rtn;
    }
}
