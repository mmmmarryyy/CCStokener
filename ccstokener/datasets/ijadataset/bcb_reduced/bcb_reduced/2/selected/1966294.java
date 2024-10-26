package sk.tuke.ess.editor.base.helpers;

import sk.tuke.ess.editor.base.components.logger.Logger;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Created by IntelliJ IDEA.
 * User: zladovan
 * Date: 2.11.2011
 * Time: 15:48
 * To change this template use File | Settings | File Templates.
 */
public class FileHelper {

    private static final int BUFFER_SIZE = 1024;

    public static ByteArrayInputStream readInputStreamToByteArrayIntupStream(InputStream zis) throws IOException {
        return new ByteArrayInputStream(readInputStreamToByteArray(zis));
    }

    public static byte[] readInputStreamToByteArray(InputStream zis) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream(zis.available());
        byte[] buffer = new byte[BUFFER_SIZE];
        int bytesReaded;
        while ((bytesReaded = zis.read(buffer, 0, BUFFER_SIZE)) > 0) {
            bos.write(buffer, 0, bytesReaded);
        }
        bos.close();
        return bos.toByteArray();
    }

    public static byte[] readURIToByteArray(URI zipEntryUri) throws IOException {
        return readInputStreamToByteArray(zipEntryUri.toURL().openConnection().getInputStream());
    }

    public static byte[] readURLToByteArray(URL url) throws IOException {
        return readInputStreamToByteArray(url.openConnection().getInputStream());
    }

    public static byte[] readFileToByteArray(File file) throws IOException {
        return readURIToByteArray(file.toURI());
    }

    public static InputStream openStreamWithoutCaching(URI uri) throws IOException {
        URLConnection connection = uri.toURL().openConnection();
        connection.setUseCaches(false);
        return connection.getInputStream();
    }

    public static void unzipToDir(File zipFile, File destDir) throws IOException {
        unzipToDir(new FileInputStream(zipFile), destDir);
    }

    public static void unzipToDir(InputStream zipInputStream, File destDir) throws IOException {
        ZipInputStream zis = new ZipInputStream(zipInputStream);
        ZipEntry zipEntry;
        destDir.mkdirs();
        while ((zipEntry = zis.getNextEntry()) != null) {
            File file = new File(destDir, PathHelper.getRealPathFromSafeForm(zipEntry.getName()));
            if (zipEntry.isDirectory()) {
                file.mkdirs();
            } else {
                new File(file.getParent()).mkdirs();
                file.createNewFile();
                FileOutputStream fos = new FileOutputStream(file);
                fos.write(readInputStreamToByteArray(zis));
                fos.close();
            }
        }
        zis.close();
    }

    public static String getFileExtension(URI uri) {
        return getFileExtension(uri.toString());
    }

    public static String getFileExtension(String filePath) {
        return filePath.replaceAll("^.*\\.([^\\.]+)$", "$1");
    }

    public static String getFileName(URI uri) {
        return uri.toString().replaceAll("^.*/([^/]+)$", "$1");
    }

    public static String getParentPath(URI uri) {
        return uri.toString().replaceFirst("(^.*)[/][^/]+$", "$1");
    }

    public static String getCommonDirPath(URI[] uris) {
        if (uris.length == 0) return "";
        if (uris.length == 1) return getParentPath(uris[0]);
        List<String> pathList = new ArrayList<String>(uris.length);
        for (URI uri : uris) {
            pathList.add(uri.toString());
        }
        Collections.sort(pathList);
        String[] pathElements1 = getPathArray(pathList.get(0));
        String[] pathElements2 = getPathArray(pathList.get(pathList.size() - 1));
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < Math.min(pathElements1.length, pathElements2.length); i++) {
            if (pathElements1[i].equals(pathElements2[i])) {
                if (i > 0) stringBuilder.append("/");
                stringBuilder.append(pathElements1[i]);
            } else {
                break;
            }
        }
        return stringBuilder.toString();
    }

    private static String[] getPathArray(String path) {
        return path.split("/");
    }

    public static File fileFromPathArray(String[] pathArray) {
        if (pathArray.length == 0) return null;
        StringBuffer stringBuffer = new StringBuffer(pathArray[0]);
        for (int i = 0; i < pathArray.length; i++) {
            stringBuffer.append(File.separator);
            stringBuffer.append(pathArray[i]);
        }
        return new File(stringBuffer.toString());
    }

    public static URI[] getUrisFromFiles(File[] files) {
        URI[] uris = new URI[files.length];
        for (int i = 0; i < files.length; i++) {
            uris[i] = (files[i].toURI());
        }
        return uris;
    }

    public static URI createJarURI(File jarFile, String entryName) throws URISyntaxException {
        return new URI(String.format("jar:%s!/%s", jarFile.toURI().toString(), entryName));
    }

    public static URI[] getUrisFormZip(File zipFile, String etrnyNameRegex) throws IOException {
        Pattern pattern = Pattern.compile(etrnyNameRegex);
        List<URI> uriList = new ArrayList<URI>();
        ZipInputStream zis = new ZipInputStream(new FileInputStream(zipFile));
        ZipEntry entry;
        try {
            while ((entry = zis.getNextEntry()) != null) {
                if (pattern.matcher(entry.getName()).matches()) {
                    try {
                        uriList.add(createJarURI(zipFile, entry.getName()));
                    } catch (URISyntaxException uriSyntaxEx) {
                        Logger.getLogger().addException(uriSyntaxEx, "Chyba pri ziskavaní uri z jar entry <b>%s</b>", entry.getName());
                    }
                }
            }
        } finally {
            zis.close();
        }
        return uriList.toArray(new URI[uriList.size()]);
    }

    public static void copyWithRewrite(File srcFile, File destFile) throws IOException {
        if (destFile.exists()) {
            destFile.delete();
        }
        FileInputStream fileInputStream = new FileInputStream(srcFile);
        FileOutputStream fileOutputStream = new FileOutputStream(destFile);
        byte[] buffer = new byte[BUFFER_SIZE];
        int byteCount;
        while ((byteCount = fileInputStream.read(buffer)) > 0) {
            fileOutputStream.write(buffer, 0, byteCount);
        }
        fileOutputStream.close();
        fileInputStream.close();
    }

    public static String[] readLinesFromStream(InputStream inputStream) throws IOException {
        List<String> lineList = new ArrayList<String>();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream));
        String line;
        while ((line = bufferedReader.readLine()) != null) {
            lineList.add(line);
        }
        return lineList.toArray(new String[lineList.size()]);
    }

    public static URL saveToFile(InputStream inputStream, File file) throws IOException {
        new File(file.getParent()).mkdirs();
        file.createNewFile();
        FileOutputStream fos = new FileOutputStream(file);
        fos.write(readInputStreamToByteArray(inputStream));
        fos.close();
        return file.toURI().toURL();
    }
}
