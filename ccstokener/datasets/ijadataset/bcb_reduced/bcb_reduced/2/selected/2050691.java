package com.cyberkinetx.ecr.countries;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;
import org.dom4j.DocumentException;
import com.cyberkinetx.ecr.ResultUnit;

/**
 * The country object for exchange currency data for Europe/Montenegru
 * @author Andrei Erimicioi <erani.mail@gmail.com>
 */
public class Montenegru extends AbstractCountry {

    private String XMLAddress = "http://www.cb-mn.org/kursna_lista_download.php?vazi_od=";

    public Montenegru() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(TimeZone.getDefault());
        this.XMLAddress += sdf.format(cal.getTime());
    }

    @Override
    public void parse() throws DocumentException, IOException {
        URL url = new URL(this.XMLAddress);
        URLConnection con = url.openConnection();
        BufferedReader bStream = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String s = bStream.readLine();
        String[] tokens = s.split("</html>");
        tokens = tokens[1].split("<br>");
        for (String sToken : tokens) {
            String[] sTokens = sToken.split(";");
            ResultUnit unit = new ResultUnit(sTokens[4], Float.valueOf(sTokens[9]), Integer.valueOf(sTokens[5]));
            this.set.add(unit);
        }
    }
}
