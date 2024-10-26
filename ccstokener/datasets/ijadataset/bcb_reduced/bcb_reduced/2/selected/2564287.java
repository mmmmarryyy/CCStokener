package com.cyberkinetx.ecr.country;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import com.cyberkinetx.ecr.CurrencyUnit;
import com.cyberkinetx.ecr.util.DataUrlResolver;
import com.cyberkinetx.ecr.util.DomainName;

/**
 * The country object for exchange currency data for Europe/Croatia
 * @author Andrei Erimicioi <erani.mail@gmail.com>
 */
public class Croatia extends AbstractCountry {

    @Override
    public void parse() throws IOException {
        URL url = new URL((new DataUrlResolver()).getDataUrl(DomainName.CROATIA));
        URLConnection con = url.openConnection();
        BufferedReader bStream = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String str;
        bStream.readLine();
        while ((str = bStream.readLine()) != null) {
            String[] tokens = str.split("(\\s+)");
            String charCode = tokens[0].replaceAll("([0-9+])", "");
            Float value = Float.parseFloat(tokens[2].trim().replace(",", "."));
            CurrencyUnit unit = new CurrencyUnit(charCode, value, DEFAULT_MULTIPLIER);
            this.set.add(unit);
        }
    }
}
