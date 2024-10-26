package eu.soa4all.execution.soaprest.linkedopenservices;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Scanner;

/**
 * An utility class to load resources accessible by the class loader of this
 * class.
 */
public class Loader {

    /**
	 * Finds the resource with the given name and returns a {@link URL} for that
	 * resource, or <code>null</code> if the resource can not be found.
	 * 
	 * @param name The name of the resource to load.
	 * @return An {@link URL} for the resource.
	 */
    public static URL loadResourceURL(String name) {
        return Loader.class.getClassLoader().getResource(name);
    }

    /**
	 * Reads the contents of the specified resource and returns an
	 * {@link InputStream} on that resource, or <code>null</code> if the
	 * resource can not be loaded.
	 * 
	 * @param name The name of the resource to load.
	 * @return An {@link InputStream} on the resource.
	 */
    public static InputStream loadResource(String name) {
        return Loader.class.getClassLoader().getResourceAsStream(name);
    }

    /**
	 * Returns a {@link File} object on the resource with the specified name, or
	 * <code>null</code> if the resource can not be loaded.
	 * 
	 * @param name The name of the resource.
	 * @return A {@link File} on the resource, or <code>null</code> if the
	 *         resource can not be loaded.
	 */
    public static File loadResourceAsFile(String name) {
        URL url = loadResourceURL(name);
        try {
            URI uri = url.toURI();
            return new File(uri);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * Reads the contents of the specified resource and returns a string
	 * containing the contents. The resource must be accessible by the class
	 * loader of this class.
	 * 
	 * @param name The name of the resource to load.
	 * @return The contents of the specified resource in string form.
	 */
    public static String loadResourceAsString(String name) {
        URL url = Loader.class.getClassLoader().getResource(name);
        InputStream urlInput = null;
        try {
            StringWriter stringWriter = new StringWriter();
            BufferedWriter writer = new BufferedWriter(stringWriter);
            urlInput = url.openStream();
            Scanner scanner = new Scanner(urlInput);
            while (scanner.hasNextLine()) {
                writer.append(scanner.nextLine());
                if (scanner.hasNextLine()) {
                    writer.newLine();
                }
            }
            writer.close();
            return stringWriter.getBuffer().toString();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (urlInput != null) {
                try {
                    urlInput.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
