package at.jku.semwiq.endpoint;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FilterReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import at.jku.semwiq.rmi.EndpointMetadata;
import at.jku.semwiq.rmi.RuntimeReplacements;
import at.jku.semwiq.rmi.SpawnedEndpointMetadata;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.util.FileUtils;

/**
 * @author dorgon
 *
 */
public class Utils {

    public static Reader getReader(String url) throws MalformedURLException, IOException {
        if (url.startsWith("file:")) return new FileReader(url.substring(5)); else if (url.startsWith("http:")) return new InputStreamReader(new URL(url).openStream());
        throw new MalformedURLException("Invalid URI schema, file: or http: expected.");
    }

    /**
	 * @param url
	 * @param meta
	 * @return
	 * @throws IOException
	 * @throws MalformedURLException
	 */
    public static Model loadFiltered(String url, SpawnedEndpointMetadata meta) throws MalformedURLException, IOException {
        final SpawnedEndpointMetadata m = meta;
        FilterReader filtered = new LineFilterReader(new BufferedReader(Utils.getReader(url)), new LineReplacer() {

            @Override
            public String replaceLine(String line) {
                return RuntimeReplacements.apply(line, m);
            }
        });
        Model model = ModelFactory.createDefaultModel();
        model.read(filtered, url, FileUtils.guessLang(url));
        return model;
    }

    public static String getTempDir(SpawnedEndpointMetadata meta) {
        return Constants.TDB_TEMP_DIR + meta.getPort();
    }
}
