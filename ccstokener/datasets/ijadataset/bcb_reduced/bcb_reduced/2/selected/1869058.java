package net.sf.dozer.util.mapping.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * Internal class used to perform various validations. Validates mapping requests, field mappings, URL's, etc. Only
 * intended for internal use.
 * 
 * @author tierney.matt
 * @author garsombke.franz
 */
public abstract class MappingValidator {

    public static void validateMappingRequest(Object srcObj) {
        if (srcObj == null) {
            MappingUtils.throwMappingException("source object must not be null");
        }
    }

    public static void validateMappingRequest(Object srcObj, Object destObj) {
        if (srcObj == null) {
            MappingUtils.throwMappingException("source object must not be null");
        }
        if (destObj == null) {
            MappingUtils.throwMappingException("destination object must not be null");
        }
    }

    public static void validateMappingRequest(Object srcObj, Class destClass) {
        if (srcObj == null) {
            MappingUtils.throwMappingException("source object must not be null");
        }
        if (destClass == null) {
            MappingUtils.throwMappingException("destination class must not be null");
        }
    }

    public static URL validateURL(String fileName) {
        ResourceLoader loader = new ResourceLoader();
        URL url = loader.getResource(fileName);
        if (url == null) {
            MappingUtils.throwMappingException("Unable to locate dozer mapping file [" + fileName + "] in the classpath!!!");
        }
        InputStream stream = null;
        try {
            stream = url.openStream();
        } catch (IOException e) {
            MappingUtils.throwMappingException("Unable to open URL input stream for dozer mapping file [" + url + "]");
        } finally {
            if (stream != null) {
                try {
                    stream.close();
                } catch (IOException e) {
                    MappingUtils.throwMappingException("Unable to close input stream for dozer mapping file [" + url + "]");
                }
            }
        }
        return url;
    }
}
