package at.langegger.xlwrap.server;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FilterReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import at.langegger.xlwrap.common.Config;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileUtils;

/**
 * @author dorgon
 *
 */
public class Utils {

    public static Reader getReader(String url) throws MalformedURLException, IOException {
        if (url.startsWith("http:")) return new InputStreamReader(new URL(url).openStream()); else return new FileReader(url);
    }

    /**
	 * @param url
	 * @return
	 * @throws IOException
	 * @throws MalformedURLException
	 */
    public static Model loadFiltered(String url) throws MalformedURLException, IOException {
        FilterReader filtered = new LineFilterReader(new BufferedReader(Utils.getReader(url)), new LineReplacer() {

            @Override
            public String replaceLine(String s) {
                return replaceLine(s);
            }
        });
        Model model = ModelFactory.createDefaultModel();
        model.read(filtered, url, FileUtils.guessLang(url));
        return model;
    }

    public static String replace(String s) {
        int p = Config.getPort();
        s = s.replaceAll("__HOSTNAME__", Config.getHostname());
        if (p == 80) s = s.replaceAll("__PORT__", ""); else s = s.replaceAll("__PORT__", ":" + p);
        s = s.replaceAll("__PATHPREFIX__", Config.PUBBY_PATHPREFIX);
        return s;
    }
}
