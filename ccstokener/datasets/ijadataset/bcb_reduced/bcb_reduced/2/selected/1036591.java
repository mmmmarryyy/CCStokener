package com.cyberkinetx.ecr.countries;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import org.dom4j.DocumentException;
import com.cyberkinetx.ecr.ResultUnit;

public class Croatia extends AbstractCountry {

    private static final String XMLAddress = "http://www.hnb.hr/tecajn/f040609.dat?tsfsg=feff0935585865c16b247bc9585fcba5";

    @Override
    public void parse() throws DocumentException, IOException {
        URL url = new URL(this.XMLAddress);
        URLConnection con = url.openConnection();
        BufferedReader bStream = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String str;
        bStream.readLine();
        while ((str = bStream.readLine()) != null) {
            String[] tokens = str.split("(\\s+)");
            String charCode = tokens[0].replaceAll("([0-9+])", "");
            Float value = Float.parseFloat(tokens[2].trim().replace(",", "."));
            ResultUnit unit = new ResultUnit(charCode, value, DEFAULT_MULTIPLIER);
            this.set.add(unit);
        }
    }
}
