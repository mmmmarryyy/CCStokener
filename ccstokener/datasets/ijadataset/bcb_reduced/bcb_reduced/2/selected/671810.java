package model;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BCFisierText implements BazaCunostinte {

    private final URL urlulSpreLocatiaCurenta;

    private final String fisier = "../exemplu_baza_cunostinte.txt";

    public BCFisierText(URL url) {
        this.urlulSpreLocatiaCurenta = url;
    }

    private String caractereAlbe = "[ |\\t]*";

    public Collection<Regula> citesteReguli() throws IOException {
        URL url = new URL(urlulSpreLocatiaCurenta, fisier);
        BufferedReader reader = new BufferedReader(new InputStreamReader((url.openStream())));
        Collection<Regula> rezultat = new ArrayList<Regula>();
        String line = "";
        while (!"".equals(line = reader.readLine())) {
            Regula r = parseazaRegula(line);
            if (r != null) rezultat.add(r);
        }
        return rezultat;
    }

    private Regula parseazaRegula(String line) {
        Pattern tiparRegula = Pattern.compile("Daca" + caractereAlbe + "(.*)" + caractereAlbe + "atunci" + caractereAlbe + "(.*)");
        Matcher matcherRegula = tiparRegula.matcher(line);
        if (matcherRegula.matches()) {
            String premise = scoateSpatiiAlbeInutile(matcherRegula.group(1));
            String concluzie = scoateSpatiiAlbeInutile(matcherRegula.group(2));
            List<String> listaPremise = new ArrayList<String>();
            for (String prem : premise.split("[ |\t]+si[ |\t]+")) {
                listaPremise.add(scoateSpatiiAlbeInutile(prem));
            }
            return new Regula(listaPremise, concluzie);
        }
        return null;
    }

    private String scoateSpatiiAlbeInutile(String group) {
        return group.replaceAll("^" + caractereAlbe, "").replaceAll(caractereAlbe + "$", "");
    }

    public Collection<String> citesteConcluzii() throws IOException {
        URL url = new URL(urlulSpreLocatiaCurenta, fisier);
        BufferedReader reader = new BufferedReader(new InputStreamReader((url.openStream())));
        Collection<String> rezultat = new ArrayList<String>();
        String line = "";
        while (!"".equals(line = reader.readLine())) ;
        while ((line = reader.readLine()) != null) {
            if (line != "") rezultat.add(line);
        }
        return rezultat;
    }
}
