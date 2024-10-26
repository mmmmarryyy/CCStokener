package com.cyberkinetx.ecr.country;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;
import com.cyberkinetx.ecr.CurrencyUnit;
import com.cyberkinetx.ecr.util.DataUrlResolver;
import com.cyberkinetx.ecr.util.DomainName;

/**
 * The country object for exchange currency data for Europe/Montenegru
 * @author Andrei Erimicioi <erani.mail@gmail.com>
 */
public class Montenegru extends AbstractCountry {

    @Override
    public void parse() throws IOException {
        URL url = new URL(getDataUrl());
        URLConnection con = url.openConnection();
        BufferedReader bStream = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String s = bStream.readLine();
        String[] tokens = s.split("</html>");
        tokens = tokens[1].split("<br>");
        for (String sToken : tokens) {
            String[] sTokens = sToken.split(";");
            CurrencyUnit unit = new CurrencyUnit(sTokens[4], Float.valueOf(sTokens[9]), Integer.valueOf(sTokens[5]));
            this.set.add(unit);
        }
    }

    private String getDataUrl() {
        Calendar cal = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf.setTimeZone(TimeZone.getDefault());
        return (new DataUrlResolver()).getDataUrl(DomainName.MONTENEGRU) + sdf.format(cal.getTime());
    }
}
