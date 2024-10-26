package clump.language.analysis;

import clump.kernel.engine.ModelLoader;
import clump.language.ast.IEntity;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.URL;

public class ResourceUtils {

    public static File getResourceFileName(File resourceDirName, IEntity definition) {
        return getResourceFileName(resourceDirName, definition.getSpecification().getName(), definition.getDefinitionKind());
    }

    public static File getResourceFileName(File directory, String entityName, clump.common.EntityKind kind) {
        return new File(directory, EntityUtils.getRelativeSystemEntityName(kind, entityName) + ".clobj");
    }

    public static IEntity readFromFile(File resourceName) {
        InputStream inputStream = null;
        try {
            URL urlResource = ModelLoader.solveResource(resourceName.getPath());
            if (urlResource != null) {
                inputStream = urlResource.openStream();
                return (IEntity) new ObjectInputStream(inputStream).readObject();
            }
        } catch (IOException e) {
        } catch (ClassNotFoundException e) {
        } finally {
            if (inputStream != null) try {
                inputStream.close();
            } catch (IOException e) {
            }
        }
        return null;
    }

    public static void writeToFile(File resourceName, IEntity entity) throws IOException {
        OutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(resourceName);
            new ObjectOutputStream(outputStream).writeObject(entity);
        } finally {
            if (outputStream != null) try {
                outputStream.close();
            } catch (IOException e) {
            }
        }
    }
}
