package org.dozer.util;

import org.dozer.config.BeanContainer;
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
public final class MappingValidator {

    private MappingValidator() {
    }

    public static void validateMappingRequest(Object srcObj) {
        if (srcObj == null) {
            MappingUtils.throwMappingException("Source object must not be null");
        }
    }

    public static void validateMappingRequest(Object srcObj, Object destObj) {
        validateMappingRequest(srcObj);
        if (destObj == null) {
            MappingUtils.throwMappingException("Destination object must not be null");
        }
    }

    public static void validateMappingRequest(Object srcObj, Class<?> destClass) {
        validateMappingRequest(srcObj);
        if (destClass == null) {
            MappingUtils.throwMappingException("Destination class must not be null");
        }
    }

    public static URL validateURL(String fileName) {
        DozerClassLoader classLoader = BeanContainer.getInstance().getClassLoader();
        if (fileName == null) {
            MappingUtils.throwMappingException("File name is null");
        }
        URL url = classLoader.loadResource(fileName);
        if (url == null) {
            MappingUtils.throwMappingException("Unable to locate dozer mapping file [" + fileName + "] in the classpath!");
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
