package org.ezfusion.functionbundle;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Vector;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import org.ezfusion.dataobject.FunctionBundleInfo;
import org.ezfusion.execution.FInfo;
import org.ezfusion.execution.FParam;
import org.ezfusion.serviceint.BundleGenerator;
import org.ezfusion.serviceint.Link;

public class BundleGeneratorImp implements BundleGenerator {

    private String baseDir;

    public BundleGeneratorImp(String dir) {
        baseDir = dir;
    }

    @Override
    public String generateBundle(FunctionBundleInfo info) throws IOException, Exception {
        String fileName = baseDir + "ezf_" + info.functionName + ".jar";
        String[] jarList = null;
        if (info.loadDir) {
            jarList = getFileList(info.jarFileName);
        } else {
            jarList = new String[] { info.jarFileName };
        }
        String[] classList = getClassList();
        File ctrFile = new File(fileName);
        JarOutputStream outputStream = new JarOutputStream(new FileOutputStream(ctrFile), getManifest(info, jarList));
        byte[] buff = new byte[1024];
        int count;
        String curFileName = "";
        BufferedInputStream curFile;
        for (int i = 0; i < jarList.length; i++) {
            curFileName = jarList[i];
            if (curFileName.startsWith("http")) {
                URL url = new URL(curFileName);
                HttpURLConnection uc = (HttpURLConnection) url.openConnection();
                curFile = new BufferedInputStream(uc.getInputStream());
            } else {
                curFile = new BufferedInputStream(new FileInputStream(curFileName));
            }
            outputStream.putNextEntry(new JarEntry(getOnlyJarName(curFileName)));
            while ((count = curFile.read(buff)) != -1) {
                outputStream.write(buff, 0, count);
            }
        }
        for (int i = 0; i < classList.length; i++) {
            curFileName = classList[i];
            curFile = new BufferedInputStream(this.getClass().getResourceAsStream(curFileName));
            outputStream.putNextEntry(new JarEntry(getOnlyClassName(curFileName)));
            while ((count = curFile.read(buff)) != -1) {
                outputStream.write(buff, 0, count);
            }
        }
        outputStream.close();
        return fileName;
    }

    private String[] getFileList(String fileName) {
        String fileSep = System.getProperty("file.separator");
        String dirName = "";
        if (fileName.endsWith(".jar")) {
            dirName = fileName.substring(0, fileName.lastIndexOf(System.getProperty("file.separator")));
        } else {
            dirName = fileName;
        }
        System.out.println("jardirName=" + dirName);
        File jarDir = new File(dirName);
        String[] fileList = jarDir.list();
        Vector<String> jarList = new Vector<String>();
        for (int f = 0; f < fileList.length; f++) {
            if (fileList[f].endsWith(".jar") || fileList[f].endsWith(".dll") || fileList[f].endsWith(".so")) {
                jarList.add(new String(jarDir.toString() + fileSep + fileList[f]));
            }
        }
        String[] result = new String[jarList.size()];
        for (int i = 0; i < result.length; i++) result[i] = jarList.get(i);
        return result;
    }

    private String[] getClassList() {
        String[] classList = new String[] { "/" + FusionNodeContainer.class.getCanonicalName().replace('.', '/') + ".class", "/" + Activator.class.getCanonicalName().replace('.', '/') + ".class", "/" + org.ezfusion.functionbundle.FusionFunction.class.getCanonicalName().replace('.', '/') + ".class", "/" + org.ezfusion.execution.FusionFunction.class.getCanonicalName().replace('.', '/') + ".class", "/" + Link.class.getCanonicalName().replace('.', '/') + ".class", "/" + FParam.class.getCanonicalName().replace('.', '/') + ".class", "/" + FInfo.class.getCanonicalName().replace('.', '/') + ".class" };
        return classList;
    }

    private String getOnlyJarName(String jarPath) {
        String jarName = "";
        if (jarPath.startsWith("http")) {
            if (jarPath.contains("?")) {
                jarName = jarPath.substring(jarPath.lastIndexOf('/'), jarPath.lastIndexOf('?'));
            } else {
                jarName = jarPath.substring(jarPath.lastIndexOf('/'));
            }
        } else {
            int li = jarPath.lastIndexOf(System.getProperty("file.separator"));
            if (li < 0) li = jarPath.lastIndexOf('/');
            if (li < 0) li = jarPath.lastIndexOf('\\');
            jarName = jarPath.substring(li);
        }
        return jarName;
    }

    private String getOnlyClassName(String classPath) {
        if (baseDir.contains("/")) {
            return classPath.replaceFirst(baseDir + "bin", "");
        } else {
            return classPath.replaceFirst(baseDir.replace('\\', '/') + "bin", "");
        }
    }

    private Manifest getManifest(FunctionBundleInfo bundleInfo, String[] jarList) throws IOException {
        String outTxt = "";
        outTxt = outTxt + "Manifest-Version: 1.0\n" + "Bundle-Vendor: LISTIC\n" + "Bundle-Version: 1.0.0\n" + "Bundle-Name: ezf_" + bundleInfo.functionName + ".jar\n" + "Bundle-ManifestVersion: 2\n" + "Bundle-Description: generated bundle for the fusion function " + bundleInfo.functionName + "\n" + "Bundle-SymbolicName: " + bundleInfo.functionName + "\n" + "Import-Package: " + "org.osgi.framework";
        if (!bundleInfo.importedPackages.contains("javax.swing")) outTxt += ", javax.swing";
        outTxt += ", org.ezfusion.tools" + ", org.ezfusion.serviceint" + ", org.ezfusion.dataobject" + "";
        for (int i = 0; i < bundleInfo.importedPackages.size(); i++) {
            outTxt += ", " + bundleInfo.importedPackages.get(i);
        }
        outTxt += '\n';
        outTxt += "Bundle-ClassPath: ., ";
        for (int i = 0; i < jarList.length; i++) {
            outTxt += "." + getOnlyJarName(jarList[i]);
            if (i < jarList.length - 1) outTxt += ", "; else outTxt += "\n";
        }
        outTxt += "Bundle-Activator: " + Activator.class.getCanonicalName() + "\n";
        ByteArrayInputStream input = new ByteArrayInputStream(outTxt.getBytes());
        Manifest manifest = new Manifest();
        manifest.read(input);
        return manifest;
    }
}
