package preprocessing.methods.Import;

import preprocessing.Parameters.Parameter;
import preprocessing.methods.BasePreprocessorConfig;
import preprocessing.methods.Import.FileLoader.URLEnterDialog;
import preprocessing.methods.PreprocessingUnitHolder;
import preprocessing.methods.Preprocessor;
import java.awt.*;
import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.zip.GZIPInputStream;

/**
 * Created by IntelliJ IDEA.
 * User: vacekp (Petr Vacek)
 * Date: 2/1/11
 * Time: 01:00
 * To change this template use File | Settings | File Templates.
 */
public class LoadDataFromURL extends AbstractBaseLoader {

    public static final String DESCRIPTION = "CSV - Comma-separated Values Type File";

    public HashMap<String, Preprocessor> MethodCallTable;

    public LoadDataFromURL() {
        super();
        methodName = "Load file from URL";
        methodDescription = "Loads data from URL, calls correct filter due to extension in URL, handles GZIP decompression";
        methodTree = "Imports.";
        baseConfig = new LoadCSVPreprocessorConfig();
        extensions = new String[2];
        extensions[0] = "csv";
        extensions[1] = "txt";
        methodDescription = DESCRIPTION;
        MethodCallTable = new HashMap();
        Preprocessor arrfLdr = new LoadWekaARFFPreprocessor();
        Preprocessor gameLdr = new GAMEDataLoader();
        Preprocessor rawLdr = new LoadRAWPreprocessor();
        Preprocessor csvLdr = new LoadCSVPreprocessor();
        Preprocessor matLdr = new LoadMatlabMATPreprocessor();
        MethodCallTable.put("arff", arrfLdr);
        MethodCallTable.put("arf", arrfLdr);
        MethodCallTable.put("xrff", arrfLdr);
        MethodCallTable.put("xrf", arrfLdr);
        MethodCallTable.put("data", gameLdr);
        MethodCallTable.put("dat", gameLdr);
        MethodCallTable.put("raw", rawLdr);
        MethodCallTable.put("txt", rawLdr);
        MethodCallTable.put("csv", csvLdr);
        MethodCallTable.put("default", csvLdr);
        MethodCallTable.put("mat", matLdr);
    }

    private String getFileName() throws NoSuchFieldException {
        Parameter p;
        try {
            p = baseConfig.getParameterObjByKey("FileName");
        } catch (NoSuchFieldException e) {
            logger.error("No parameter called \"Filename\" found in " + methodName + ".\n", e);
            throw e;
        }
        String s = (String) p.getValue();
        if (s.compareTo("") == 0) {
            try {
                URLEnterDialog urlDialog = new URLEnterDialog();
                urlDialog.pack();
                urlDialog.setVisible(true);
                s = urlDialog.finalURL;
            } catch (HeadlessException e) {
                logger.fatal("The preprocessing method runs from command line and can not create open dialog to obtain file name to load.");
                return null;
            }
        }
        return s;
    }

    public boolean run() {
        String url;
        try {
            url = getFileName();
        } catch (NoSuchFieldException e) {
            return false;
        }
        if (url == null) {
            logger.error("URL not specified! Cannot continue.");
            return false;
        }
        try {
            URL newURL = new URL(url);
            String extension = url.substring((url.lastIndexOf(".")) + 1, url.length());
            File temp = File.createTempFile("temp", "." + extension);
            System.out.printf("Storing URL contents to a temp file : %s\n", temp);
            temp.deleteOnExit();
            InputStream urlINS = new BufferedInputStream(newURL.openConnection().getInputStream());
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(temp));
            int len = 0;
            for (int singleByte = urlINS.read(); singleByte != -1; singleByte = urlINS.read()) {
                out.write(singleByte);
                len++;
            }
            System.out.printf("Stored %d bytes from URL contents\n", len);
            out.flush();
            out.close();
            System.out.printf("URL/Temp extension : %s\n", extension);
            if (extension.equalsIgnoreCase("gz")) {
                String shorterName = url.substring(0, url.lastIndexOf("."));
                String extension2 = shorterName.substring((shorterName.lastIndexOf(".")) + 1, shorterName.length());
                File temp2 = File.createTempFile("temp", "." + extension2);
                temp2.deleteOnExit();
                System.out.printf("URL/Temp extension after decompressing gzip : %s\n", extension2);
                GZIPInputStream decompressor = new GZIPInputStream(new FileInputStream(temp));
                OutputStream target = new FileOutputStream(temp2);
                byte[] buffer = new byte[1024];
                int length;
                while ((length = decompressor.read(buffer)) > 0) {
                    target.write(buffer, 0, length);
                }
                target.flush();
                target.close();
                temp = temp2;
            }
            String tempName = temp.getName();
            extension = tempName.substring((tempName.lastIndexOf(".")) + 1, tempName.length());
            Preprocessor fileLoader = null;
            BasePreprocessorConfig configObject = null;
            if (MethodCallTable.containsKey(extension) == false) {
                fileLoader = MethodCallTable.get("default");
            } else {
                fileLoader = MethodCallTable.get(extension);
            }
            System.out.printf("Calling filter '%s' for extension: %s\n", fileLoader.getPreprocessingMethodName(), extension);
            configObject = new LoadCSVPreprocessorConfig();
            configObject.setValueByName("FileName", temp.getAbsolutePath());
            fileLoader.setConfigurationClass(configObject);
            return fileLoader.run();
        } catch (Exception e) {
            logger.error(e);
            return false;
        }
    }

    @Override
    public void finish() {
    }

    public boolean isApplyOnTestingData() {
        return false;
    }

    private ArrayList generateNewNames(int numDatasets) {
        ArrayList names = new ArrayList();
        for (int i = 0; i < numDatasets; i++) {
            String s = "Attribute_" + i;
            if (i == numDatasets - 1) s = "!" + s;
            names.add(s);
        }
        return names;
    }

    public static void main(String[] args) throws Exception {
        LoadDataFromURL filter = new LoadDataFromURL();
        filter.baseConfig.setValueByName("FileName", "http://localhost/bak/data/xrffgz/iris.xrff.gz");
        filter.run();
    }
}
