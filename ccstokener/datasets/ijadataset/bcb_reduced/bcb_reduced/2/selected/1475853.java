package com.parexel.toolkit.main;

import com.parexel.schemas.cxdd.cmddtypes.*;
import com.parexel.toolkit.getdata.files.*;
import com.parexel.toolkit.getdata.xml.Request;
import com.parexel.toolkit.getdata.tools.vectors.VectorOfRequests;
import com.parexel.toolkit.getdata.tools.*;
import com.parexel.toolkit.main.Connection;
import com.parexel.webservices.t.cxdd.cmdd.*;
import com.parexel.schemas.standard.header.*;
import java.sql.Timestamp;
import java.text.*;
import java.util.*;
import java.io.*;
import java.net.*;

/**
 * This class is the most important class of the JAVA code for the Toolkit.
 * It initializes data, does the connection, and retrieves data.
 *
 * It has many functions, but 4 important missions:
 * - 0: INITIALIZE FILES: XmlConfigFile, success and error LogFiles.
 * - I: READ THE INITIALIZATION FILE: Get information from the XMLiniFile.
 * - II:CONNECTION: Execute the requests and create the target files.
 * - III: WRITE IN THE LOG FILES: Write the requests result in the Log Files.
 *
 * Need:
 * - The files classes: com.parexel.toolkit.getdata.files.Filemodel, com.parexel.toolkit.getdata.files.XmlConfigFile, com.parexel.toolkit.getdata.files.LOGFile, com.parexel.toolkit.getdata.files.TMPFile.
 * - com.parexel.toolkit.getdata.xml.Request: objects that contain all the requests informations.
 * - com.parexel.toolkit.main.Connection: to connect the URL and retrieve data.
 * - com.parexel.webservices.cxdd.WSCMDD.WebClient: to call the Paris' Web Service.
 *
 * Functions:
 * - Constructor.
 * - Get and set methods.
 * - Show method.
 * - Execution which connects to the URL ; creates the target file and writes in the LogFiles.
 * - Exit the program on error and return an error code.
 * - Log in the log files.
 * - Log on error in the *currentDirectory* if the initialization is not successful.
 */
public class CxddToolkit {

    private static final String DEFAULT_CONFIG_FILE = "CxDDToolkitCfg.xml";

    private static final String DEFAULT_DATETIME_FORMAT = "dd-MM-yyyy:HH.mm.ss";

    /**
     * This is the list of all the supposed valid requests: all the mandatory parameters are full.
     * This value will be initialized with the XmlConfigFile.java,
     * Then all these requests will be executed and if successful the data will be saved.
     **/
    private VectorOfRequests vectorOfRequests;

    /**
     * Connection object
     */
    Connection connection;

    /**
     * Stub object for CMDD Web service
     */
    CMDDStub stub;

    /**
     * Filemodel use to move and rename the temporary files.
     * When the TmpFile is well-done, we only use Filemodel instances.
     */
    FileModel fileTargetFile;

    /** 
     * Boolean to know if we have to log in the success log file.
     * true = create or update the file.
     * false = no. 
     **/
    private boolean bWriteSucc;

    /** 
     * Boolean to know if we have to log in the error log file.
     * true = create or update the file. 
     * false = no.
     **/
    private boolean bWriteFail;

    /** 
     * This is the time and date when the SOFTWARE CxDDToolkit starts to run. 
     **/
    private Timestamp startTimer;

    /**
     * This is the format of the Date used to create the logs files name.
     * It can be: daily | weekly | montlhy | yearly.
     **/
    private String sFrequencyDate;

    /**
     * This is the value of the date when start the execution, formatted in the well format frequencyDate.
     *  Format:
     *  - For Daily Logs: [DD]�[MMM]-[YYYY]
     *  - For Weekly Logs: [WXX]�[MMM]-[YYYY] 
     *  - For Monthly Logs: [MMM]-[YYYY] 
     *  - For Yearly Logs: [YYYY]
     *  @see com.parexel.toolkit.getdata.tools.MyDate class to see how it works.
     **/
    private String sToday;

    /** 
     * The name of the emails server.
     * For example: cliff.parexel.com
     **/
    private String sSMTPServer;

    /** 
     * The Email style sheet URL.
     * For example: http://corpdir.parexel.com/Ressources/CSS/Global/MailStyle.css
     **/
    private String sEmailStyleSheetURL;

    /** 
     * The CxDD Home page URL.
     * For example: http://corpdir.parexel.com
     **/
    private String sCxDDHomePageURL;

    /** 
     * The path of the current directory. 
     * - This attribute is the directory where we are currently working during the execution.
     * - It is the .jar and CxDDToolkitCfg.xml directory path.
     * - It allows to launch the Toolkit from everywhere, without going in the directory of the .jar.
     * - It has to be an existant and correct path of a directory.
     **/
    private String sCurrentDirectory;

    /** 
     * The path of the *log files directory*.
     * It is the directory where the two log files will be created (for the success and the error).
     **/
    private String sLogFilesDirectory;

    /**
     * Attribute value to say that not any error occurred during the execution of a function.
     **/
    private static String noError = "no";

    /** Error message when the initialization file does not exist **/
    private static String errorMessageInexistantXMLiniFile = "The initialization file does not exist. \n";

    /** Error message when the file retrieved is empty **/
    private static String errorMessageFileEmpty = "The transfer from the server was successful but no data has been retrieved. The file's size is 0. \n";

    /** Error message when the tags are not found/deleted successfully **/
    private int repeatCount = 0;

    private int repeatTimeout = 0;

    public static void main(String[] args) throws IOException {
        if (args.length > 0) {
            String sCurrentDirectory = args[0].trim();
            if (sCurrentDirectory != null && (new File(sCurrentDirectory)).exists()) {
                (new CxddToolkit(new File(sCurrentDirectory).getAbsolutePath())).process();
            } else {
                System.out.println("Sorry. The path " + sCurrentDirectory + " of the configuration file's directory is wrong. Please give a valid path conform to your Operating System. ");
            }
        } else {
            System.out.println("The parameter 'path of the configuration file's directory' is missing. Please check the user guide of the CxDDToolkit for more informations. ");
        }
    }

    /**
     * Very important Conctructor.
     *
     * @param asCurrentDirectory the path of the *currentDirectory*.
     **/
    public CxddToolkit(String asCurrentDirectory) {
        startTimer = MyDate.nowTimestamp();
        sFrequencyDate = "daily";
        bWriteSucc = false;
        bWriteFail = true;
        sToday = MyDate.formatLogFileDate(sFrequencyDate);
        sCurrentDirectory = asCurrentDirectory;
        sLogFilesDirectory = asCurrentDirectory;
    }

    /**
     * It initializes all the parameters, especially the requests to execute 
     * (with reading the initialization file).
     *
     * - start the timer.
     * - set the default values to the attributes.
     * - try to parse the XML initialization file to see if it is well formed.
     * - retrieve the settings (log and emails).
     * - retrieve the requests informations (+ lists of emails if needed).
     * - execution of all the valid requests.
     */
    public void process() {
        try {
            System.out.println(getCurrentTime() + "SETTINGS INITIALIZATION...");
            sCurrentDirectory = sCurrentDirectory + File.separator;
            if (!(new File(sCurrentDirectory + DEFAULT_CONFIG_FILE)).exists()) logInCurrentDirectoryOnError(sCurrentDirectory, sToday, errorMessageInexistantXMLiniFile, startTimer, sSMTPServer, sEmailStyleSheetURL, sCxDDHomePageURL); else {
                XmlConfigFile XMLini = new XmlConfigFile(sCurrentDirectory + DEFAULT_CONFIG_FILE, sCurrentDirectory);
                System.out.println("- Initialization File: " + XMLini.getAbsolutePath());
                XMLini.initXmlReader();
                if (!XMLini.getError().equalsIgnoreCase(noError)) {
                    logInCurrentDirectoryOnError(sCurrentDirectory, sToday, XMLini.getError(), startTimer, sSMTPServer, sEmailStyleSheetURL, sCxDDHomePageURL);
                }
                XMLini.getAllParameters();
                if (!XMLini.getError().equalsIgnoreCase(noError)) {
                    logInCurrentDirectoryOnError(sCurrentDirectory, sToday, XMLini.getError(), startTimer, sSMTPServer, sEmailStyleSheetURL, sCxDDHomePageURL);
                }
                sFrequencyDate = XMLini.getFrequency();
                sToday = MyDate.formatLogFileDate(sFrequencyDate);
                bWriteFail = XMLini.getLogWhenError();
                bWriteSucc = XMLini.getLogWhenSuccess();
                if (XMLini.getPathLog().equalsIgnoreCase(".")) sLogFilesDirectory = sCurrentDirectory; else sLogFilesDirectory = XMLini.getPathLog() + File.separator;
                XMLini.getEmailsSettings(startTimer);
                sCxDDHomePageURL = XMLini.getCxDDHomePageURL();
                sEmailStyleSheetURL = XMLini.getEmailStyleSheetURL();
                sSMTPServer = XMLini.getSMTPServer();
                XMLini.getRepeatParameters();
                if (!XMLini.getError().equalsIgnoreCase(noError)) {
                    logInCurrentDirectoryOnError(sLogFilesDirectory + File.separator, sToday, XMLini.getError(), startTimer, sSMTPServer, sEmailStyleSheetURL, sCxDDHomePageURL);
                }
                XMLini.getAllRequests(startTimer);
                if (!XMLini.getError().equalsIgnoreCase(noError)) {
                    logInCurrentDirectoryOnError(sLogFilesDirectory + File.separator, sToday, XMLini.getError(), startTimer, sSMTPServer, sEmailStyleSheetURL, sCxDDHomePageURL);
                }
                repeatCount = XMLini.getRepeatCount();
                repeatTimeout = XMLini.getRepeatTimeout() * 1000;
                System.out.println(getCurrentTime() + "REQUEST INITIALIZATION...");
                vectorOfRequests = XMLini.getVectCommandes();
                System.out.print(vectorOfRequests.show());
                if (vectorOfRequests != null && vectorOfRequests.size() > 0) this.validRequestsExecution();
            }
        } catch (Exception e) {
            logInCurrentDirectoryOnError(sLogFilesDirectory + File.separator, sToday, e.getMessage(), startTimer, sSMTPServer, sEmailStyleSheetURL, sCxDDHomePageURL);
        }
    }

    /**
     * Try to connect the URL for all the requests:
     * - for all the requests:
     *   
     *   1. Part I: creation of the URL and connection.
     *      a) Call the web service and get the url of the file.
     *      b) valid the URL where the data is retrieved.
     *      c) test if it is with html protocol, for Boston's server.
     *      d) URL connection.
     *   --> if successfull, a temporary file is created in the *currentDirectory* with the request result.
     *   2. Part II:  Check the success of the transfert.
     *      a) Verify the size of the file --> zero = error. Create the file but log an error.
     *   3. Part III:  Transfert the File.
     *      a) delete the tags.
     *      b) rename the file.
     *      c) move the file in its directory.
     *   4. Part IV: Write the result in the Log Files.
     *
     **/
    private void validRequestsExecution() {
        String sUrlTmpFile;
        String sSuccessOrFail = "N/A", sErrorDescription = "N/A";
        System.out.println(getCurrentTime() + "REQUEST EXECUTION...");
        for (int i = 0; i < vectorOfRequests.size(); i++) {
            boolean bInvalidWebService = false;
            Request request = vectorOfRequests.getRequestAtIndex(i);
            boolean bWithColumnHeading = request.getFirstRowHasColumnName().equalsIgnoreCase("TRUE");
            boolean bEncapsulate = request.getEncapsulate().equalsIgnoreCase("TRUE");
            int iRequestNumber = (request.getNumber());
            sUrlTmpFile = request.getUrl();
            System.out.println("\n- Call Web Service for Request number " + iRequestNumber + " ...");
            try {
                startTimer = MyDate.nowTimestamp();
                request.setStartTime(MyDate.formatDate(startTimer));
                String sColumnDelimiter = request.getColumnDelimiter();
                String sExportEncoding = request.getExportEncoding();
                String sRowDelimiter = request.getRowDelimiter();
                String sTextQualifier = request.getTextQualifier();
                Map<String, String> mParameters = request.getCommandParameters();
                stub = new CMDDStub(request.getServer());
                ExecuteCommandRequest executeCommandRequest = new ExecuteCommandRequest();
                StandardHeader standardHeader = new StandardHeader();
                ExtendedErrorHeader extendedErrorHeader = new ExtendedErrorHeader();
                executeCommandRequest.setApplicationID(request.getApplicationIdentifier());
                executeCommandRequest.setCommand(request.getCommandName());
                executeCommandRequest.setIncludeCheckingTags(bEncapsulate);
                executeCommandRequest.setExportFormat(ExportFormatType.fromValue(request.getExportFormat()));
                executeCommandRequest.setIncludeColumnsHeadings(bWithColumnHeading);
                executeCommandRequest.setColumnDelimiter(ColumnDelimiterType.fromValue((sColumnDelimiter.equals("null") ? "COMMA" : sColumnDelimiter)));
                executeCommandRequest.setCustomColumnDelimiter(request.getCustomColumnDelimiter());
                executeCommandRequest.setRowDelimiter(RowDelimiterType.fromValue((sRowDelimiter.equals("null") ? "CRLF" : sRowDelimiter)));
                executeCommandRequest.setTextQualifier(TextQualifierType.fromValue(sTextQualifier.equals("null") | sTextQualifier.equals("no") ? "DOUBLEQUOTE" : sTextQualifier));
                executeCommandRequest.setConsumerInterface(ConsumerInterfaceType.fromValue("TOOLKIT"));
                executeCommandRequest.setEncoding(EncodingType.fromValue(sExportEncoding.equals("null") ? "DEFAULT" : sExportEncoding));
                executeCommandRequest.setCustomEncoding(request.getCustomExportEncoding());
                ArrayOfParameterType arrayOfParameter = new ArrayOfParameterType();
                for (String sname : mParameters.keySet()) {
                    ParameterType parameter = new ParameterType();
                    parameter.setParamName(sname);
                    parameter.setParamValue(mParameters.get(sname));
                    arrayOfParameter.getParameterItem().add(parameter);
                }
                executeCommandRequest.setParameterList(arrayOfParameter);
                standardHeader.setWsCID(request.getWsIdentifier());
                ExecuteCommandResponse executeCommandResponse = null;
                int attemptsNumber = 0;
                int iRepeatCount = repeatCount;
                do {
                    try {
                        attemptsNumber++;
                        executeCommandResponse = stub.ExecuteCommand(executeCommandRequest, standardHeader, extendedErrorHeader);
                        bInvalidWebService = false;
                    } catch (Exception ex) {
                        bInvalidWebService = true;
                        Thread.sleep(repeatTimeout);
                        iRepeatCount--;
                        sSuccessOrFail = "FAILURE";
                        sErrorDescription = "In request " + iRequestNumber + ": " + ex.getMessage();
                    }
                } while (bInvalidWebService && iRepeatCount > 0);
                request.setAttemptsNumber(attemptsNumber);
                if (executeCommandResponse == null) bInvalidWebService = true; else {
                    sUrlTmpFile = executeCommandResponse.getGeneratedFileURL();
                    if (sUrlTmpFile == null) {
                        bInvalidWebService = true;
                        sSuccessOrFail = "FAILURE";
                        sErrorDescription = "In request " + iRequestNumber + ": " + extendedErrorHeader.getErrorDescription() + " ";
                    }
                }
            } catch (Exception ex) {
                bInvalidWebService = true;
                System.out.println("Exception: " + ex);
            }
            if (bInvalidWebService == false) {
                connection = new Connection(sUrlTmpFile);
                RandomString sRandomName = new RandomString(13, 16);
                TmpFile tempFile = new TmpFile(sCurrentDirectory, sRandomName, false, sCurrentDirectory, request.getExportFormat());
                File tempTargetFile = new File(request.getTargetPath());
                if (!connection.getError().equalsIgnoreCase(noError)) {
                    sSuccessOrFail = "FAILURE";
                    sErrorDescription = "In request " + iRequestNumber + ": " + connection.getError() + " ";
                    tempFile.delete();
                } else {
                    if (!download(sUrlTmpFile, tempFile.getPath())) {
                        sSuccessOrFail = "FAILURE";
                        sErrorDescription = "In request " + iRequestNumber + ": Download error";
                        tempFile.delete();
                    } else {
                        if (tempFile.emptyFile()) {
                            sSuccessOrFail = "FAILURE";
                            sErrorDescription = "For request number " + iRequestNumber + ": " + errorMessageFileEmpty + " ";
                            if (request.getExportFormat().equalsIgnoreCase("XML")) tempFile.writeXmlDataNotFound(); else tempFile.writeDataNotFound();
                            fileTargetFile = tempFile.renamingFile(tempTargetFile.getName(), true);
                            if (!fileTargetFile.getError().equalsIgnoreCase(noError)) {
                                sSuccessOrFail = "FAILURE";
                                sErrorDescription = "In request " + iRequestNumber + ": " + errorMessageFileEmpty + tempFile.getError() + " ";
                                tempFile.delete();
                            } else {
                                fileTargetFile = fileTargetFile.moveFromDirectoryToAnother(tempTargetFile.getParent(), true);
                                if (fileTargetFile.getError().compareTo(noError) != 0) {
                                    sSuccessOrFail = "FAILURE";
                                    sErrorDescription = "In request " + iRequestNumber + ": " + errorMessageFileEmpty + fileTargetFile.getError() + " ";
                                    fileTargetFile.delete();
                                }
                            }
                        } else {
                            try {
                                if (request.getExportFormat().equalsIgnoreCase("XML")) {
                                    XmlConfigFile TempXML = new XmlConfigFile(tempFile.getAbsolutePath(), sCurrentDirectory);
                                    TempXML.initXmlReader();
                                    if (TempXML.getError().compareTo(noError) != 0) {
                                        sSuccessOrFail = "FAILURE";
                                        sErrorDescription = "File XML created " + TempXML.getAbsolutePath() + " is not a valid XML file: " + TempXML.getError() + " ";
                                        tempFile.delete();
                                        bInvalidWebService = true;
                                    }
                                }
                                if (tempFile.getError().compareToIgnoreCase(noError) != 0) {
                                    sSuccessOrFail = "FAILURE";
                                    sErrorDescription = "In request " + iRequestNumber + ": " + tempFile.getError();
                                    tempFile.delete();
                                } else {
                                    if (bInvalidWebService == false) {
                                        fileTargetFile = tempFile.renamingFile(tempTargetFile.getName(), true);
                                        if (fileTargetFile.getError().compareTo(noError) != 0) {
                                            sSuccessOrFail = "FAILURE";
                                            sErrorDescription = "In request " + iRequestNumber + ": " + tempFile.getError() + " ";
                                            tempFile.delete();
                                        } else {
                                            fileTargetFile = fileTargetFile.moveFromDirectoryToAnother(tempTargetFile.getParent(), true);
                                            if (fileTargetFile.getError().compareTo(noError) != 0) {
                                                sSuccessOrFail = "FAILURE";
                                                sErrorDescription = "In request " + iRequestNumber + ": " + fileTargetFile.getError() + " ";
                                                fileTargetFile.delete();
                                            } else {
                                                sSuccessOrFail = "SUCCESS";
                                                sErrorDescription = "The data transfer was successfully completed for request " + iRequestNumber + ". ";
                                            }
                                        }
                                    }
                                }
                            } catch (Exception e) {
                                sSuccessOrFail = "FAILURE";
                                sErrorDescription = "Target File fail: Exception in request " + iRequestNumber + " " + e.getMessage() + ". ";
                                tempFile.delete();
                            }
                        }
                    }
                }
            }
            Timestamp endTimer = MyDate.nowTimestamp();
            String duration = MyDate.GetdurationMillis(startTimer, endTimer);
            request.setEndTime(MyDate.formatDate(endTimer));
            request.setDuration(duration);
            request.setSuccorFail(sSuccessOrFail);
            request.setErrorDescription(sErrorDescription);
            System.out.print(request.show());
            logAtTheEnd(sLogFilesDirectory, sCurrentDirectory, sToday, request, sSuccessOrFail, sErrorDescription, startTimer, bWriteSucc, bWriteFail, sSMTPServer, sEmailStyleSheetURL, sCxDDHomePageURL);
        }
    }

    /**
    * Download a file from the URL Address
    * @param address Address of the file to download.
    * @param localFileName FileName given to the downloaded file on the local machine.
    * @return boolean success or failure status
    */
    public boolean download(String address, String localFileName) {
        boolean result = false;
        int attemptsNumber = 0;
        int iRepeatCount = repeatCount;
        try {
            do {
                attemptsNumber++;
                OutputStream out = null;
                URLConnection conn = null;
                InputStream in = null;
                try {
                    URL url = new URL(address);
                    out = new BufferedOutputStream(new FileOutputStream(localFileName));
                    conn = url.openConnection();
                    in = conn.getInputStream();
                    byte[] buffer = new byte[1024];
                    int numRead;
                    while ((numRead = in.read(buffer)) != -1) {
                        out.write(buffer, 0, numRead);
                    }
                    result = true;
                } catch (Exception ex) {
                    result = false;
                    Thread.sleep(repeatTimeout);
                    iRepeatCount--;
                    if (iRepeatCount > 0) System.out.println("Connection to file " + address + " ... Attempt number " + (attemptsNumber + 1));
                } finally {
                    if (in != null) in.close();
                    if (out != null) out.close();
                }
            } while (!result && iRepeatCount > 0);
        } catch (Exception ex) {
            result = false;
        }
        return result;
    }

    /**
     * Show all the attributes values.
     * - list of requests;
     * - current directory;
     * @return A text that describes this attributes.
     **/
    public String show() {
        StringBuffer l_Show = new StringBuffer();
        l_Show.append(vectorOfRequests.show() + "\n");
        l_Show.append("\n");
        l_Show.append("Current Directory: " + sCurrentDirectory + "\n");
        return l_Show.toString();
    }

    /**
     * Exit on big error. 
     * If an error occurs, it can be used to exit the JAVA execution with a special error code.
     * returns -1. error code.
     **/
    private static void exitOnError() {
        System.out.println("\nEXIT ON ERROR\n");
        System.exit(-1);
    }

    /**
     * Log an error in the Log File.
     * This function creates the Log File when a big error occurs and that it is impossible to initialize the
     * data. It creates a LogFile in the current directory and write the specific error.
     * @param p_CurrentDirectory The current directory.
     * @param p_DateFormatted The date of start of the execution.
     * @param p_ErrorDescription Description of the error that occured.
     * @param p_StartTimer The time when starts the software CxDDToolkit
     * @param p_SMTPServer The emails server.
     * @param p_EmailStyleSheetURL The emails style sheet url.
     * @param p_CxDDHomePageURL The CxDD home page url.
     *
     * NOTA: be sure that the p_CurrentDirectory's String is finish by a File.separator.
     **/
    private static void logInCurrentDirectoryOnError(String p_CurrentDirectory, String p_DateFormatted, String p_ErrorDescription, Timestamp p_StartTimer, String p_SMTPServer, String p_EmailStyleSheetURL, String p_CxDDHomePageURL) {
        LogFile l_errorLOGFile = new LogFile(p_CurrentDirectory + "CxDDToolkit-ERROR-Logs-" + p_DateFormatted + ".xml", p_CurrentDirectory);
        if (l_errorLOGFile.getError().compareTo(noError) == 0) {
            LogFile.writeToLog(new Request(), "FAILURE", p_ErrorDescription, p_StartTimer, false, true, null, l_errorLOGFile, p_SMTPServer, p_EmailStyleSheetURL, p_CxDDHomePageURL);
            if (l_errorLOGFile != null) System.out.println(" -Creation/Update of Error LOG file: " + l_errorLOGFile.getAbsolutePath());
        }
        exitOnError();
    }

    /**
     * Log in the Log File.
     *
     * This function create or update the Log Files, depending on the execution result.
     *
     * @param p_LogFilesDirectory The path of the log files directory.
     * @param p_CurrentDirectory The path of the current directory.
     * @param p_Today The date (well-formatted to create the log file name).
     * @param p_Request The request.
     * @param p_Successorfail The request result.
     * @param p_ErrorDescription The description of the error that occured.
     * @param p_StartTimer The time/date when start the CxDDToolkit execution.
     * @param p_LogWhenSuccess true = log success logs. Else no.
     * @param p_LogWhenError true = log error logs. Else no.
     * @param p_SMTPServer The email server.
     * @param p_EmailStyleSheetURL The email style sheet url.
     * @param p_CxDDHomePageURL The CxDD home page url.
     *
     * Nota: be sure that the p_CurrentDirectory's String is finished by a File.separator.
     **/
    public static void logAtTheEnd(String p_LogFilesDirectory, String p_CurrentDirectory, String p_Today, Request p_Request, String p_Successorfail, String p_ErrorDescription, Timestamp p_StartTimer, boolean p_LogWhenSuccess, boolean p_LogWhenError, String p_SMTPServer, String p_EmailStyleSheetURL, String p_CxDDHomePageURL) {
        LogFile l_SuccessLogFile = null, l_ErrorLogFile = null;
        if (p_LogWhenError == true || p_LogWhenSuccess == true) {
            p_LogFilesDirectory = FileModel.CreateDirectories(p_LogFilesDirectory, p_CurrentDirectory).getAbsolutePath();
            if (FileModel.getStaticError().compareTo(noError) != 0) {
                logInCurrentDirectoryOnError(p_CurrentDirectory, p_Today, FileModel.getStaticError(), p_StartTimer, p_SMTPServer, p_EmailStyleSheetURL, p_CxDDHomePageURL);
            }
        }
        String l_PathSuccessLogFile = p_LogFilesDirectory + File.separator + "CxDDToolkit-SUCCESS-Logs-" + p_Today + ".xml";
        String l_PathErrorLogFile = p_LogFilesDirectory + File.separator + "CxDDToolkit-ERROR-Logs-" + p_Today + ".xml";
        if (p_LogWhenError == true && p_Successorfail.compareToIgnoreCase("SUCCESS") != 0) {
            l_ErrorLogFile = new LogFile(l_PathErrorLogFile, p_CurrentDirectory);
            if (l_ErrorLogFile.getError().compareTo(noError) != 0) logInCurrentDirectoryOnError(p_CurrentDirectory, p_Today, l_ErrorLogFile.getError(), p_StartTimer, p_SMTPServer, p_EmailStyleSheetURL, p_CxDDHomePageURL);
        }
        if (l_ErrorLogFile != null) System.out.println("- Creation/Update of the Error logfile: " + l_ErrorLogFile.getAbsolutePath());
        if (p_LogWhenSuccess == true && p_Successorfail.compareToIgnoreCase("SUCCESS") == 0) {
            l_SuccessLogFile = new LogFile(l_PathSuccessLogFile, p_CurrentDirectory);
            if (l_SuccessLogFile.getError().compareTo(noError) != 0) logInCurrentDirectoryOnError(p_CurrentDirectory, p_Today, l_SuccessLogFile.getError(), p_StartTimer, p_SMTPServer, p_EmailStyleSheetURL, p_CxDDHomePageURL);
        }
        if (l_SuccessLogFile != null) System.out.println("- Creation/Update of the Success logfile: " + l_SuccessLogFile.getAbsolutePath());
        LogFile.writeToLog(p_Request, p_Successorfail, p_ErrorDescription, p_StartTimer, p_LogWhenSuccess, p_LogWhenError, l_SuccessLogFile, l_ErrorLogFile, p_SMTPServer, p_EmailStyleSheetURL, p_CxDDHomePageURL);
    }

    private static String getCurrentTime() {
        DateFormat dateFormat = new SimpleDateFormat(DEFAULT_DATETIME_FORMAT);
        return "[" + dateFormat.format(Calendar.getInstance().getTime()) + "] ";
    }
}
