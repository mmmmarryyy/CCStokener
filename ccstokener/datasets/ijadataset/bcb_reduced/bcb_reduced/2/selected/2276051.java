package jmathlib.core.functions;

import jmathlib.core.interpreter.*;
import java.io.*;
import java.net.*;

/**Class for storing and managing the m- and p-functions*/
public class MFileLoader extends RootObject {

    boolean pFileCachingEnabledB = false;

    /**Default constructor*/
    public MFileLoader() {
    }

    /**loads an .m-file via the web
    @param directory = the directory containing the file
    @param mFileName = the name of the m file
    @return the result of the file as a FunktionToken*/
    public UserFunction loadMFileViaWeb(URL codeBase, String directoryAndFile, String mFileName) {
        String code = "";
        UserFunction function = null;
        ErrorLogger.debugLine("MFileLoader: loading >" + mFileName + ".m<");
        try {
            URL url = new URL(codeBase, directoryAndFile);
            InputStream in = url.openStream();
            BufferedReader inReader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = inReader.readLine()) != null) {
                code += line + "\n";
            }
            inReader.close();
        } catch (Exception e) {
            Errors.throwMathLibException("MFileLoader: m-file exception via web");
        }
        ErrorLogger.debugLine("MFileLoader: code: begin \n" + code + "\ncode end");
        FunctionParser funcParser = new FunctionParser();
        function = funcParser.parseFunction(code);
        function.setName(mFileName);
        ErrorLogger.debugLine("MFileLoader: finished webloading >" + mFileName + ".m<");
        return function;
    }

    public UserFunction loadMFile(File file) {
        return loadMFile(file.getParent(), file.getName());
    }

    public UserFunction loadPFile(File file) {
        return loadPFile(file.getParent(), file.getName());
    }

    /**reads in an .m-file
       @param directory = the directory containing the file
       @param mFileName = the name of the m file
       @return the result of the file as a FunktionToken*/
    public UserFunction loadMFile(String directory, String mFileName) {
        String code = "";
        UserFunction function = null;
        String fullFileName = directory + File.separator + mFileName + ".m";
        File file = null;
        ErrorLogger.debugLine("MFileLoader: loading >" + mFileName + ".m<");
        try {
            file = new File(fullFileName);
            BufferedReader inReader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = inReader.readLine()) != null) {
                code += line + "\n";
            }
            inReader.close();
        } catch (Exception e) {
            Errors.throwMathLibException("MFileLoader: m-file exception");
        }
        ErrorLogger.debugLine("MFileLoader: code: begin \n" + code + "\ncode end");
        FunctionParser funcParser = new FunctionParser();
        function = funcParser.parseFunction(code);
        function.setName(mFileName);
        function.setLastModified(file.lastModified());
        function.setPathAndFileName(fullFileName);
        if (pFileCachingEnabledB) {
            try {
                ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(directory + File.separator + mFileName + ".p"));
                out.writeObject(function);
            } catch (Exception e) {
                Errors.throwMathLibException("Error serialising function: " + e);
                e.printStackTrace();
            }
        }
        ErrorLogger.debugLine("MFileLoader: finished loading >" + mFileName + ".m<");
        return function;
    }

    /**reads in an .p-file
       @param directory = the directory containing the file
       @param pFileName = the name of the p file
       @return the result of the file as a FunktionToken*/
    public UserFunction loadPFile(String directory, String pFileName) {
        UserFunction function = null;
        ErrorLogger.debugLine("MFileLoader: loading >" + pFileName + ".p<");
        try {
            ObjectInputStream in = new ObjectInputStream(new FileInputStream(directory + File.separator + pFileName + ".p"));
            function = ((UserFunction) in.readObject());
            ErrorLogger.debugLine("MFileLoader: code: " + function);
        } catch (Exception e) {
            ErrorLogger.debugLine("Error reading serialised function: " + e);
            e.printStackTrace();
        }
        ErrorLogger.debugLine("MFileLoader: finished loading >" + pFileName + ".p<");
        return function;
    }

    /** set caching of p-file to on of off
     * 
     * @param pFileCaching  true= caching of p-files on; false: caching of p-files off
     */
    public void setPFileCaching(boolean pFileCaching) {
        pFileCachingEnabledB = pFileCaching;
    }

    /** return whether of not caching of p-files is enabled of not 
     * 
     * @return status of caching p-files
     */
    public boolean getPFileCaching() {
        return pFileCachingEnabledB;
    }
}
