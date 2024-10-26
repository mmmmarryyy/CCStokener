package net.sf.ediknight.common.edi.directory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import net.sf.ediknight.edi.directory.Directory;
import net.sf.ediknight.edi.directory.DirectoryFactory;

/**
 * @author Holger Joest
 */
public final class DirectoryFactoryImpl extends DirectoryFactory {

    /** */
    private static Map<String, Directory> directories = new HashMap<String, Directory>();

    /**
     * {@inheritDoc}
     * @see net.sf.ediknight.edi.directory.DirectoryFactory
     *      #getDirectory(java.lang.String)
     */
    @Override
    public Directory getDirectory(String standard, String id) {
        DirectoryImpl userDirectory = (DirectoryImpl) directories.get(id);
        try {
            if (userDirectory == null) {
                String home = System.getProperty("user.home");
                File specifications = new File(home + "/.ediknight/specifications/" + standard + "/");
                if (!specifications.exists()) {
                    specifications.mkdirs();
                }
                File userCache = new File(home + "/.ediknight/cache/" + standard + "/" + id);
                if (userCache.exists()) {
                    userDirectory = DirectoryImpl.fromCache(userCache);
                } else {
                    File specification = findSpecification(id, specifications);
                    userDirectory = DirectoryImpl.fromXML(new FileInputStream(specification));
                    userDirectory.toCache(userCache);
                }
                String serviceId = null;
                if ("edifact".equals(standard)) {
                    serviceId = "40100";
                    if (id.startsWith("9")) {
                        serviceId = "40000";
                    }
                } else if ("x12".equals(standard)) {
                    serviceId = "service";
                }
                if (serviceId != null) {
                    File serviceCache = new File(home + "/.ediknight/cache/" + standard + "/" + serviceId);
                    DirectoryImpl serviceDirectory = null;
                    if (serviceCache.exists()) {
                        serviceDirectory = DirectoryImpl.fromCache(serviceCache);
                    } else {
                        File serviceSpecification = findSpecification(serviceId, specifications);
                        serviceDirectory = DirectoryImpl.fromXML(new FileInputStream(serviceSpecification));
                        serviceDirectory.toCache(serviceCache);
                    }
                    userDirectory.connect(serviceDirectory);
                }
                directories.put(id, userDirectory);
            }
            return userDirectory;
        } catch (Exception ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    /**
     * @param id the specification identifier
     * @param specifications the specification directory
     * @return the file containing the specification
     */
    private File findSpecification(String id, File specifications) {
        File specification = new File(specifications, id + ".xml");
        if (!specification.exists()) {
            extractSpecifications(id, specification);
        }
        return specification;
    }

    /**
     * @param id the specification identifier
     * @param specification the specification file
     */
    private void extractSpecifications(String id, File specification) {
        Object resource = getClass().getResource(id + ".xml");
        if (resource instanceof URL) {
            URL url = (URL) resource;
            try {
                InputStream istream = url.openStream();
                try {
                    OutputStream ostream = new FileOutputStream(specification);
                    try {
                        byte[] buffer = new byte[1024];
                        int length;
                        while ((length = istream.read(buffer)) > 0) {
                            ostream.write(buffer, 0, length);
                        }
                    } finally {
                        ostream.close();
                    }
                } finally {
                    istream.close();
                }
            } catch (IOException ex) {
                throw new RuntimeException("Failed to open " + url, ex);
            }
        }
    }
}
