package net.sf.sail.core.curnit;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.util.Collection;
import java.util.Iterator;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import org.apache.commons.io.IOUtils;
import net.sf.sail.core.beans.Pod;
import net.sf.sail.core.util.MarshallUtils;
import net.sf.sail.core.uuid.PodUuid;

/**
 * Provides static methods to output a Curnit archive to an OutputStream
 * 
 * @see net.sf.sail.test.beans.Curnit
 * @author turadg
 */
public class CurnitArchive {

    static class CurnitManifest extends Manifest {

        CurnitManifest(Curnit curnit) {
            super();
            Attributes attrs = getMainAttributes();
            attrs.put(Attributes.Name.MANIFEST_VERSION, "1.0");
            attrs.put(new Attributes.Name("Created-By"), "SAIL Curnit Builder 0.1");
            attrs.put(CurnitFile.CURNIT_TITLE_ATTR, curnit.getTitle());
            attrs.put(CurnitFile.CURNIT_FORMAT_ATTR, "dev20050513");
            attrs.put(CurnitFile.MAIN_CLASS_ATTR, CurnitFile.MAIN_CLASS.getName());
        }
    }

    public static void writeArchive(Curnit curnit, OutputStream out) throws IOException {
        JarOutputStream jarout = new JarOutputStream(out, new CurnitManifest(curnit));
        jarout.putNextEntry(new JarEntry(CurnitFile.CURNITXML_NAME));
        MarshallUtils.writeCurnit(curnit, jarout);
        jarout.closeEntry();
        Collection<Pod> pods = curnit.getReferencedPods();
        for (Pod pod : pods) {
            JarEntry entry = CurnitFile.entryFor(pod.getPodId());
            jarout.putNextEntry(entry);
            MarshallUtils.writePod(pod, jarout);
            jarout.closeEntry();
        }
        includePodDependencies(curnit, jarout);
        jarout.close();
    }

    private static void includePodDependencies(Curnit curnit, JarOutputStream jarout) throws IOException {
        Properties props = new Properties();
        Collection<Pod> pods = curnit.getReferencedPods();
        for (Pod pod : pods) {
            PodUuid podId = pod.getPodId();
            URL weburl = PodArchiveResolver.getSystemResolver().getUrl(podId);
            String urlString = "";
            if (weburl != null) {
                String uriPath = weburl.getPath();
                String zipPath = CurnitFile.WITHINCURNIT_BASEPATH + uriPath;
                jarout.putNextEntry(new JarEntry(zipPath));
                IOUtils.copy(weburl.openStream(), jarout);
                jarout.closeEntry();
                urlString = CurnitFile.WITHINCURNIT_PROTOCOL + uriPath;
            }
            props.put(podId.toString(), urlString);
        }
        jarout.putNextEntry(new JarEntry(CurnitFile.PODSREFERENCED_NAME));
        props.store(jarout, "pod dependencies");
        jarout.closeEntry();
    }
}
