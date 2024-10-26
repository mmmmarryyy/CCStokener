package org.siberia.trans.type.repository;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.WeakHashMap;
import javax.xml.bind.JAXBException;
import org.apache.log4j.Logger;
import org.siberia.trans.exception.FileCheckException;
import org.siberia.trans.exception.InvalidPluginDeclaration;
import org.siberia.trans.exception.ResourceNotFoundException;
import org.siberia.trans.type.plugin.PluginBuild;
import org.siberia.trans.type.plugin.PluginStructure;
import org.siberia.type.annotation.bean.Bean;
import org.siberia.utilities.io.IOUtilities;
import org.siberia.utilities.security.check.CheckSum;
import org.siberia.utilities.task.TaskStatus;
import org.siberia.xml.schema.pluginarch.Module;

/**
 *
 * default implementation of a siberia plugin repository
 *
 * @author alexis
 */
@Bean(name = "default repository", internationalizationRef = "org.siberia.rc.i18n.type.AbstractSiberiaRepository", expert = true, hidden = true, preferred = true, propertiesClassLimit = Object.class, methodsClassLimit = Object.class)
public class DefaultSiberiaRepository extends AbstractSiberiaRepository {

    /** logger */
    private static Logger logger = Logger.getLogger(DefaultSiberiaRepository.class);

    /** weak hashmap linking url and local file
     *	it allow to avoid the download of a resource if it has already been downloaded
     */
    private Map<URL, File> cache = new WeakHashMap<URL, File>(40);

    /** Creates a new instance of DefaultSiberiaRepository
     *  @param url the url representing the repository
     */
    public DefaultSiberiaRepository() {
        super();
    }

    /**
	 * method that allow to copy the content of a remote file in a local temporary file
	 * 
	 * @param url the url of the file to copy to local
	 * @param status a TaskStatus
	 * @param check the method of check
	 * @return the result of the copy
	 * @exception FileCheckException if check is not null and checksum check failed
	 * @exception IOException if IOException occurs !!
	 * @exception ResourcResourceNotFoundException resource is not found
	 */
    protected File copyToLocal(URL url, TaskStatus status, CheckSum check) throws FileCheckException, IOException, ResourceNotFoundException {
        return this.copyToLocal(url, null, status, check, true);
    }

    /**
	 * method that allow to copy the content of a remote file at the given local location
	 * 
	 * @param url the url of the file to copy to local
	 * @param filePath the path where to copy the resulting file (null to create a temporary file)
	 * @param status a TaskStatus. could be null.
	 * @param check the method of check. if null, no check.
	 * @param failIfChecksumFailed true to indicate that a failed checksum must throw an exception of kind FileCheckException.
	 * 				    false to only provide error logs
	 * @return the result of the copy. the result is not marked to be deleted when jvm exit.
	 * @exception FileCheckException if failIfChecksumFailed and check is not null and checksum check failed
	 * @exception IOException if IOException occurs !!
	 * @exception ResourceNResourceNotFoundExceptionesource is not found
	 */
    protected File copyToLocal(URL url, String filePath, TaskStatus status, CheckSum check, boolean failIfChecksumFailed) throws FileCheckException, IOException, ResourceNotFoundException {
        if (url == null) {
            throw new IllegalArgumentException("the url could not be null");
        }
        File result = null;
        if (filePath == null) {
            String file = url.getFile();
            int lastSlashIndex = file.lastIndexOf("/");
            int lastBackSlashIndex = file.lastIndexOf("\\");
            file = file.substring(Math.max(lastSlashIndex + 1, lastBackSlashIndex + 1));
            try {
                result = File.createTempFile("c_f_" + url.getProtocol() + "_" + url.getHost() + "_" + file + "_", ".cop");
            } catch (IOException ex) {
                result = File.createTempFile("c_f_" + url.getProtocol() + "_" + url.getHost() + "_", ".cop");
            }
        } else {
            result = new File(filePath);
        }
        result.deleteOnExit();
        File parentFile = result.getParentFile();
        if (!parentFile.exists()) {
            parentFile.mkdirs();
        }
        if (!result.exists()) {
            result.createNewFile();
        }
        boolean checkFile = true;
        try {
            File cachedFile = this.cache.get(url);
            if (cachedFile == null) {
                logger.debug("copying to local url content \"" + url + "\" to filePath : " + filePath + " with checksum : " + check);
                IOUtilities.copy(url.openStream(), result, status, new byte[2 * 1024]);
            } else {
                if (filePath != null) {
                    IOUtilities.copy(cachedFile, result);
                } else {
                    result = cachedFile;
                }
                checkFile = false;
            }
        } catch (FileNotFoundException e) {
            throw new ResourceNotFoundException(e.getMessage());
        }
        if (checkFile) {
            if (CheckSum.NONE.equals(check)) {
                this.cache.put(url, result);
            } else {
                logger.debug("performing check of the copy with checksum : " + check);
                URL urlSign = new URL(url.getProtocol(), url.getHost(), url.getPort(), url.getPath() + check.extension());
                File tmpFileSign = File.createTempFile("repository", ".xml" + check.extension());
                tmpFileSign.deleteOnExit();
                try {
                    IOUtilities.copy(urlSign.openStream(), tmpFileSign);
                } catch (FileNotFoundException e) {
                    ResourceNotFoundException resEx = new ResourceNotFoundException(e.getMessage());
                    resEx.setStackTrace(e.getStackTrace());
                    throw resEx;
                }
                if (check.isValid(result, tmpFileSign)) {
                    if (result != null) {
                        this.cache.put(url, result);
                    }
                } else {
                    FileCheckException exception = new FileCheckException(result.getAbsolutePath(), "check (method=" + check + ") invalid for repository declaration of " + this);
                    if (failIfChecksumFailed) {
                        throw exception;
                    } else {
                        logger.error("checksum failed for file : '" + result + "' with signature file : '" + tmpFileSign + "' with checksum method : " + check, exception);
                    }
                }
            }
        }
        if (result == null) {
            throw new ResourceNotFoundException(null);
        }
        return result;
    }

    /** copy the repository declaration at a given local location
     *  @param check a CheckSum
     *  @param status a TaskStatus
     *
     *  @return the temporarly file created
     */
    public File copyRepositoryDeclarationToLocalTemp(CheckSum check, TaskStatus status) throws ResourceNotFoundException, IOException, FileCheckException {
        String file = this.getURL().getPath();
        if (!file.endsWith("/")) {
            file += "/";
        }
        file += "repository.xml";
        URL url = new URL(this.getURL().getProtocol(), this.getURL().getHost(), this.getURL().getPort(), file);
        return this.copyToLocal(url, status, check);
    }

    /** copy a plugin at a given local location
     *  @param plugin a PluginBuild
     *  @param status a TaskStatus
     *
     *  @return the temporarly file created
     */
    public File copyPluginToLocalTemp(PluginBuild plugin, TaskStatus status) throws ResourceNotFoundException, IOException, FileCheckException, InvalidPluginDeclaration {
        return this.copyToLocal(this.getBuildURL(plugin), status, plugin.getCheckType());
    }

    /** return the module declaration contains builds information
     *  @param plugin a PluginStructure
     *  @param status a TaskStatus
     *
     *  @return a Module
     */
    public Module getModuleDeclaration(PluginStructure plugin, TaskStatus status) throws ResourceNotFoundException, IOException, JAXBException {
        Module module = null;
        String path = this.getURL().getPath();
        URL url = new URL(this.getURL().getProtocol(), this.getURL().getHost(), this.getURL().getPort(), path + File.separator + plugin.getDirectoryRelativePath() + File.separator + plugin.getPluginDeclarationFilename());
        File f = null;
        try {
            f = this.copyToLocal(url, status, CheckSum.NONE);
        } catch (FileCheckException ex) {
            logger.error("unable to copy from " + url, ex);
        }
        org.siberia.trans.xml.JAXBLoader loader = new org.siberia.trans.xml.JAXBLoader();
        module = loader.loadModule(new FileInputStream(f));
        return module;
    }
}
