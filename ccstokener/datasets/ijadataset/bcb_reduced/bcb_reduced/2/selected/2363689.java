package org.dago.common;

import java.io.*;
import java.net.URL;
import java.util.jar.*;

/**
 * Utility class to read the version from the manifest.<br>
 * The version is given by the {@link java.util.jar.Attribute.IMPLEMENTATION_VERSION} attribute
 */
public class VersionReader {

    /**
	 * Gives the version for the specified class
	 * @param clazz the class included in the manifest file to read
 	 * @return the version as string
	 * @throws DagoException when the manifest file can't be read
	 */
    public static String readVersion(Class<?> clazz) throws DagoException {
        InputStream manifestStream = null;
        try {
            URL urlManifest = new URL(getPathToManifest(clazz));
            manifestStream = urlManifest.openStream();
            Manifest manifest = new Manifest(manifestStream);
            return manifest.getMainAttributes().getValue(Attributes.Name.IMPLEMENTATION_VERSION);
        } catch (Exception err) {
            throw new DagoException(I18NMessages.failToReadVersion, err);
        } finally {
            if (manifestStream != null) {
                try {
                    manifestStream.close();
                } catch (IOException err) {
                }
            }
        }
    }

    private static String getPathToManifest(Class<?> clazz) {
        String classSimpleName = clazz.getSimpleName() + ".class";
        String pathToClass = clazz.getResource(classSimpleName).toString();
        String classFullName = clazz.getName().replace('.', '/') + ".class";
        return pathToClass.substring(0, pathToClass.length() - (classFullName.length())) + "META-INF/MANIFEST.MF";
    }
}
