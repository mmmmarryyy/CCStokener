package it.could.confluence.autoexport;

import com.atlassian.plugin.osgi.factory.OsgiPlugin;
import com.atlassian.plugin.osgi.container.OsgiContainerManager;
import com.atlassian.plugin.Plugin;
import com.atlassian.plugin.PluginAccessor;
import com.atlassian.plugin.PluginController;
import com.atlassian.plugin.JarPluginArtifact;
import org.osgi.framework.Bundle;
import org.osgi.framework.Constants;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.service.packageadmin.PackageAdmin;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.osgi.context.BundleContextAware;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.File;
import java.io.StringReader;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.zip.ZipOutputStream;
import java.util.zip.ZipEntry;
import java.util.Enumeration;
import java.util.Dictionary;
import java.util.Map;
import java.util.HashMap;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.jar.Attributes;
import java.net.URL;

public class PluginBuilder implements BundleContextAware {

    private static final String TEMPLATES_SYMBOLIC_NAME = "autoexport.templates";

    private static final String TEMPLATES_PLUGIN_KEY = TEMPLATES_SYMBOLIC_NAME + "-1";

    private final PluginAccessor pluginAccessor;

    private final PluginController pluginController;

    private BundleContext bundleContext;

    public PluginBuilder(PluginAccessor accessor, @Qualifier("pluginController") PluginController pluginController) {
        pluginAccessor = accessor;
        this.pluginController = pluginController;
    }

    private void forBundle(BundleManipulator manip) {
        ByteArrayOutputStream bout = null;
        try {
            bout = new ByteArrayOutputStream();
            ZipOutputStream zout = new ZipOutputStream(bout);
            Bundle bundle = getBundle();
            Enumeration<URL> files = bundle.findEntries("/", "*.vm", false);
            if (files != null) {
                while (files.hasMoreElements()) {
                    URL url = files.nextElement();
                    String name = url.getFile();
                    if (name.startsWith("/")) {
                        name = name.substring(1);
                    }
                    if (manip.includeEntry(name)) {
                        zout.putNextEntry(new ZipEntry(name));
                        IOUtils.copy(url.openStream(), zout);
                    }
                }
            }
            manip.finish(bundle, zout);
            Manifest mf = new Manifest(bundle.getEntry("META-INF/MANIFEST.MF").openStream());
            zout.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
            mf.write(zout);
            zout.close();
            File tmpFile = File.createTempFile(TEMPLATES_SYMBOLIC_NAME, ".jar");
            FileUtils.writeByteArrayToFile(tmpFile, bout.toByteArray());
            if (pluginAccessor.getPlugin(TEMPLATES_SYMBOLIC_NAME) != null) {
                pluginController.uninstall(pluginAccessor.getPlugin(TEMPLATES_SYMBOLIC_NAME));
            } else if (pluginAccessor.getPlugin(TEMPLATES_PLUGIN_KEY) != null) {
                pluginController.uninstall(pluginAccessor.getPlugin(TEMPLATES_PLUGIN_KEY));
            }
            pluginController.installPlugin(new JarPluginArtifact(tmpFile));
            ServiceReference ref = bundleContext.getServiceReference(PackageAdmin.class.getName());
            ((PackageAdmin) bundleContext.getService(ref)).refreshPackages(null);
            tmpFile.delete();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            IOUtils.closeQuietly(bout);
        }
    }

    public boolean remove(final String name) {
        forBundle(new BundleManipulator() {

            public boolean includeEntry(String entryName) {
                return !name.equals(entryName);
            }

            public void finish(Bundle bundle, ZipOutputStream zout) {
            }
        });
        return true;
    }

    public void add(final String name, final String content) {
        forBundle(new BundleManipulator() {

            public boolean includeEntry(String entryName) {
                return !name.equals(entryName);
            }

            public void finish(Bundle bundle, ZipOutputStream zout) throws IOException {
                zout.putNextEntry(new ZipEntry(name));
                IOUtils.copy(new StringReader(content), zout, "UTF-8");
            }
        });
    }

    private Bundle getBundle() {
        Plugin plugin = pluginAccessor.getPlugin(TEMPLATES_SYMBOLIC_NAME);
        if (plugin == null) {
            plugin = pluginAccessor.getPlugin(TEMPLATES_PLUGIN_KEY);
        }
        if (plugin == null) {
            try {
                File tmpFile = File.createTempFile(TEMPLATES_SYMBOLIC_NAME, ".jar");
                ZipOutputStream zout = new ZipOutputStream(new FileOutputStream(tmpFile));
                zout.putNextEntry(new ZipEntry("META-INF/MANIFEST.MF"));
                Manifest mf = new Manifest();
                mf.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1");
                mf.getMainAttributes().putValue(Constants.BUNDLE_SYMBOLICNAME, TEMPLATES_SYMBOLIC_NAME);
                mf.getMainAttributes().putValue(Constants.BUNDLE_VERSION, "1");
                mf.getMainAttributes().putValue(Constants.BUNDLE_DESCRIPTION, "Customized templates for the auto export plugin");
                mf.getMainAttributes().putValue(Constants.BUNDLE_NAME, "Auto export templates plugin");
                mf.getMainAttributes().putValue(Constants.BUNDLE_MANIFESTVERSION, "2");
                mf.write(zout);
                zout.close();
                pluginController.installPlugin(new JarPluginArtifact(tmpFile));
                tmpFile.delete();
            } catch (IOException e) {
                throw new RuntimeException("Unable to retrieve bundle", e);
            }
        }
        Bundle[] bundles = bundleContext.getBundles();
        for (Bundle bundle : bundles) {
            if (bundle.getSymbolicName().equals(TEMPLATES_SYMBOLIC_NAME)) {
                return bundle;
            }
        }
        throw new IllegalStateException("The templates bundle is not found");
    }

    public void setBundleContext(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    private static interface BundleManipulator {

        boolean includeEntry(String name);

        void finish(Bundle bundle, ZipOutputStream zout) throws IOException;
    }
}
