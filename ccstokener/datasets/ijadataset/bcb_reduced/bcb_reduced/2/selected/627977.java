package hu.sztaki.lpds.pgportal.wfeditor.client.communication.http;

import java.net.*;
import java.io.*;
import hu.sztaki.lpds.pgportal.wfeditor.client.utils.*;
import hu.sztaki.lpds.pgportal.wfeditor.client.WorkflowEditor;
import hu.sztaki.lpds.pgportal.wfeditor.common.jdl.JDLDocument;
import java.util.Iterator;
import java.util.zip.GZIPOutputStream;
import java.util.zip.GZIPInputStream;

public class HTTPCommunication extends java.lang.Object {

    public static HTTPCommunication myHTTPCommunication;

    private String urlString;

    private String method;

    private String returnValue;

    public java.util.HashMap parameters;

    public int progressPercent, current, proccessProgressPercent;

    private boolean finished = true;

    public String fileName, errorStr;

    public java.util.Vector files;

    public InputFile currentFile;

    public boolean error = false;

    public WorkflowEditor parentWorkflow;

    public String downloadDir = "";

    private boolean isThreadStopped;

    private int status;

    private boolean isResponseError;

    private int responseError;

    private javax.swing.Timer communicationTimer;

    private int commTimerTriggerCounter;

    private hu.sztaki.lpds.pgportal.wfeditor.client.dialog.PostRequestDialog PRDialog;

    public static final int STATUS_INIT = 0;

    public static final int STATUS_COMPRESSING = 1;

    public static final int STATUS_UPLOADING = 2;

    public static final int STATUS_DOWNLOADING = 3;

    public static final int STATUS_DECOMPRESSING = 4;

    public static final int STATUS_SUCCESS = 5;

    public static final int STATUS_CANCELD = 6;

    public static final int ERROR_NO_USERNAME_TO_SESSION = 1;

    public static final int ERROR_NO_DATA_FROM_SERVER = 2;

    public static final int ERROR_NO_EDITOR_ROLE_PARAMETER = 3;

    /** Creates a new instance of HTTPCommunication */
    public HTTPCommunication(String url, WorkflowEditor parentwkf) {
        urlString = url;
        parameters = new java.util.HashMap();
        parentWorkflow = parentwkf;
        isThreadStopped = false;
        status = STATUS_INIT;
        errorStr = new String("");
        isResponseError = false;
        PRDialog = null;
        try {
            System.setProperty("sun.net.client.defaultConnectTimeout", String.valueOf(parentwkf.COMMUNICATION_TIMEOUT));
            System.setProperty("sun.net.client.defaultReadTimeout ", String.valueOf(parentwkf.COMMUNICATION_TIMEOUT));
        } catch (Exception e) {
        }
    }

    public void uploadFiles() {
        isThreadStopped = false;
        error = false;
        errorStr = "";
        isResponseError = false;
        current = 0;
        progressPercent = 0;
        proccessProgressPercent = 0;
        status = STATUS_INIT;
        if (files == null) {
            finished = true;
            return;
        }
        final SwingWorker worker = new SwingWorker() {

            String ret = "", j = "", ret1 = "";

            String currentFileRetStr = new String("");

            public Object construct() {
                finished = false;
                ActualTask conn = new ActualTask();
                for (int i = 0; i < files.size(); i++) {
                    if (isThreadStopped) {
                        return ret;
                    }
                    currentFile = (InputFile) (files.get(i));
                    if (currentFile == null) continue;
                    try {
                        parameters.put("sessionId", parentWorkflow.getSessionId());
                        parameters.put("targetDirectory", currentFile.getDirectory());
                        currentFileRetStr = conn.postFileRequest(currentFile.getAbsolutePath(), currentFile.getInternalFileName());
                        if (currentFileRetStr.equals("Error;NO_USERNAME_TO_SESSION\n")) {
                            System.out.println("HTTPCommunication.uploadFiles()-Error: no username to session.");
                            isThreadStopped = true;
                            parentWorkflow.closingWorkflow(responseError);
                        }
                        if (isThreadStopped) {
                            return ret;
                        }
                        currentFile.setUploadedFileStatus(currentFileRetStr, parentWorkflow);
                        ret += currentFileRetStr;
                        System.out.println("upload thread:" + currentFileRetStr);
                    } catch (Exception e) {
                        error = true;
                        finished = true;
                    }
                    proccessProgressPercent = java.lang.Math.round((i + 1) * 100 / files.size());
                }
                System.out.println("Thread done.");
                finished = true;
                return ret;
            }

            public void finished() {
                System.out.println("Thread finished.");
                finished = true;
                returnValue = ret;
            }
        };
        worker.start();
    }

    public void downloadFiles() {
        error = false;
        errorStr = "";
        current = 0;
        progressPercent = 0;
        proccessProgressPercent = 0;
        if (files == null) {
            finished = true;
            return;
        }
        final SwingWorker worker = new SwingWorker() {

            String ret = "", j = "";

            public Object construct() {
                finished = false;
                ActualTask conn = new ActualTask();
                for (int i = 0; i < files.size(); i++) {
                    currentFile = (InputFile) (files.get(i));
                    if (currentFile == null) continue;
                    try {
                        parameters.put("targetDirectory", parentWorkflow.getSessionId() + "/" + currentFile.getDirectory());
                        parameters.put("targetFile", currentFile.getName());
                        ret = conn.postDownloadRequest(currentFile.getLocalFile());
                    } catch (Exception e) {
                        error = true;
                        finished = true;
                    }
                    proccessProgressPercent = java.lang.Math.round((i + 1) * 100 / files.size());
                }
                finished = true;
                return ret;
            }

            public void finished() {
                finished = true;
                returnValue = ret;
            }
        };
        worker.start();
    }

    private void postRequestThread() {
        final SwingWorker worker = new SwingWorker() {

            String ret = new String("");

            public Object construct() {
                finished = false;
                if (isThreadStopped) return ret;
                ret = postRequest2();
                System.out.println("HTTPCommunication.postRequestThread()$Construct()-ret:" + ret);
                finished = true;
                return ret;
            }

            public void finished() {
                checkResponseError(ret);
                communicationTimer.stop();
                System.out.println("HTTPCommunication.postRequestThread()$Finished()-stopped. ret: " + ret);
                finished = true;
                returnValue = ret;
                if (PRDialog != null) {
                    PRDialog.closingDialog();
                }
                System.out.println("HTTPCommunication.postRequestThread()$Finished()-returnValue:" + returnValue);
            }
        };
        worker.start();
    }

    public void prDPrint() {
        System.out.println("HTTPCommunication.prDPrint()-PRDialog:" + PRDialog);
    }

    public int getUploadProgress() {
        return progressPercent;
    }

    public int getProcessProgress() {
        return proccessProgressPercent;
    }

    public void setVector(java.util.Vector pvect) {
        files = pvect;
    }

    public String getCurrentFile() {
        if (currentFile != null) return currentFile.getName();
        return "";
    }

    public boolean done() {
        return finished;
    }

    public String getErrorStr() {
        return errorStr;
    }

    public boolean isErrorOccured() {
        return error;
    }

    public boolean isResponseErrorOccured() {
        return isResponseError;
    }

    public int getResponseError() {
        return responseError;
    }

    private void setResponseError(String errorStr) {
        if (errorStr.equals("NO_USERNAME_TO_SESSION")) {
            responseError = ERROR_NO_USERNAME_TO_SESSION;
            isResponseError = true;
        } else if (errorStr.equals("NO_EDITOR_ROLE_PARAMETER")) {
            responseError = ERROR_NO_EDITOR_ROLE_PARAMETER;
            isResponseError = true;
        }
    }

    public synchronized int getStatus() {
        return status;
    }

    private synchronized void setStatus(int s) {
        status = s;
    }

    private void setDecompressStatusAtUpload(String str) {
        if (str.equals("Status;DECOMPRESS_START")) {
            status = STATUS_DECOMPRESSING;
        } else if (str.equals("Status;DECOMPRESS_STOP")) {
            status = STATUS_SUCCESS;
        }
    }

    public void setIsThreadStopped(boolean flag) {
        isThreadStopped = flag;
    }

    public String getReturnValue() {
        return returnValue;
    }

    private void checkResponseError(String resStr) {
        if (resStr == null) {
            isResponseError = true;
            responseError = ERROR_NO_DATA_FROM_SERVER;
        } else {
            String[] lines = resStr.split("\n");
            if (lines.length > 0) {
                String[] line = lines[0].split(";");
                if (line[0].equals("Error")) {
                    setResponseError(line[1]);
                }
            } else {
                isResponseError = true;
                responseError = ERROR_NO_DATA_FROM_SERVER;
            }
        }
    }

    private void communicationTimerActionPerformed() {
        this.commTimerTriggerCounter++;
        if ((this.commTimerTriggerCounter * WorkflowEditor.COMMUNICATION_TRIGGER) > WorkflowEditor.COMMUNICATION_TIMEOUT + 1000) {
            setIsThreadStopped(true);
            javax.swing.JOptionPane.showMessageDialog(this.parentWorkflow.getFrame(), "Communication is timeouted.", "Error", javax.swing.JOptionPane.ERROR_MESSAGE);
            this.communicationTimer.stop();
        } else if (!this.returnValue.equals("")) {
            setIsThreadStopped(true);
        }
    }

    private void initPostRequest() {
        this.communicationTimer = new javax.swing.Timer(WorkflowEditor.COMMUNICATION_TRIGGER, new java.awt.event.ActionListener() {

            public void actionPerformed(java.awt.event.ActionEvent evt) {
                communicationTimerActionPerformed();
            }
        });
        this.communicationTimer.start();
    }

    private void createPostRequestDialog(String text) {
        this.PRDialog = new hu.sztaki.lpds.pgportal.wfeditor.client.dialog.PostRequestDialog(this.parentWorkflow.getFrame(), this, true);
        this.PRDialog.setTextLabel(text);
        this.PRDialog.setLocation((int) (this.parentWorkflow.getFrame().getX() + this.parentWorkflow.getFrame().getWidth() / 2 - this.PRDialog.getWidth() / 2), (int) (this.parentWorkflow.getFrame().getY() + this.parentWorkflow.getFrame().getHeight() / 2 - this.PRDialog.getHeight() / 2));
        this.PRDialog.show();
    }

    public void doPostRequest(String dialogText) throws Exception {
        this.isThreadStopped = false;
        error = false;
        errorStr = "";
        isResponseError = false;
        status = STATUS_INIT;
        this.returnValue = "";
        this.commTimerTriggerCounter = 0;
        initPostRequest();
        postRequestThread();
        createPostRequestDialog(dialogText);
    }

    private String postRequest2() {
        error = false;
        errorStr = "";
        String emptyStr = "";
        isResponseError = false;
        String responseString = null;
        String requestString = new String("");
        this.isThreadStopped = false;
        try {
            for (java.util.Iterator i = parameters.entrySet().iterator(); i.hasNext(); ) {
                java.util.Map.Entry e = (java.util.Map.Entry) i.next();
                requestString = requestString + URLEncoder.encode((String) e.getKey(), "UTF-8") + "=" + URLEncoder.encode((String) e.getValue(), "UTF-8") + "&";
            }
            URL url = new URL(urlString);
            URLConnection connection = url.openConnection();
            HttpURLConnection httpConn = (HttpURLConnection) connection;
            httpConn.setRequestProperty("Content-Length", String.valueOf(requestString.length()));
            httpConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            httpConn.setRequestMethod("POST");
            httpConn.setDoOutput(true);
            httpConn.setDoInput(true);
            if (isThreadStopped) return emptyStr;
            connection.connect();
            System.out.println("Done.");
            if (isThreadStopped) return emptyStr;
            PrintWriter out = new PrintWriter(httpConn.getOutputStream());
            if (isThreadStopped) return emptyStr;
            out.println(requestString);
            out.close();
            if (isThreadStopped) return emptyStr;
            InputStreamReader isr = new InputStreamReader(httpConn.getInputStream());
            BufferedReader br = new BufferedReader(isr);
            String temp;
            String tempResponse = "";
            if (isThreadStopped) return emptyStr;
            while ((temp = br.readLine()) != null) tempResponse = tempResponse + temp + "\n";
            System.out.println("HTTPCommunication.postRequest2()-Reading response finished.");
            if (isThreadStopped) return emptyStr;
            responseString = tempResponse;
            br.close();
            isr.close();
            httpConn.disconnect();
        } catch (java.net.ConnectException conne) {
            error = true;
            finished = true;
            errorStr = "Cannot connect to: " + urlString + "\n" + "                     Server is not responding.";
            if (isThreadStopped) return emptyStr;
        } catch (java.io.InterruptedIOException e) {
            error = true;
            finished = true;
            errorStr = "Connection to Portal lost: communication is timeouted.";
            this.parentWorkflow.getMenuButtonEventHandler().stopAutomaticRefresh();
            if (isThreadStopped) return emptyStr;
        } catch (java.net.MalformedURLException e) {
            error = true;
            finished = true;
            errorStr = "Error in communication: " + e.getMessage();
            System.out.println("postRequest()-MalformedURLException:" + e);
            if (isThreadStopped) return emptyStr;
        } catch (Exception e) {
            error = true;
            finished = true;
            errorStr = "Error while trying to communicate the server: " + e.getMessage();
            System.out.println("postRequest()-Exception." + e);
            if (isThreadStopped) return emptyStr;
        }
        return responseString;
    }

    public String postRequest() {
        error = false;
        errorStr = "";
        isResponseError = false;
        String responseString = null;
        String requestString = new String("");
        try {
            for (java.util.Iterator i = parameters.entrySet().iterator(); i.hasNext(); ) {
                java.util.Map.Entry e = (java.util.Map.Entry) i.next();
                requestString = requestString + URLEncoder.encode((String) e.getKey(), "UTF-8") + "=" + URLEncoder.encode((String) e.getValue(), "UTF-8") + "&";
            }
            URL url = new URL(urlString);
            URLConnection connection = url.openConnection();
            HttpURLConnection httpConn = (HttpURLConnection) connection;
            httpConn.setRequestProperty("Content-Length", String.valueOf(requestString.length()));
            httpConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            httpConn.setRequestMethod("POST");
            httpConn.setDoOutput(true);
            httpConn.setDoInput(true);
            connection.connect();
            PrintWriter out = new PrintWriter(httpConn.getOutputStream());
            out.println(requestString);
            out.close();
            InputStreamReader isr = new InputStreamReader(httpConn.getInputStream());
            BufferedReader br = new BufferedReader(isr);
            String temp;
            String tempResponse = "";
            while ((temp = br.readLine()) != null) tempResponse = tempResponse + temp + "\n";
            responseString = tempResponse;
            checkResponseError(responseString);
            br.close();
            isr.close();
            httpConn.disconnect();
        } catch (java.net.ConnectException conne) {
            error = true;
            finished = true;
            errorStr = "Cannot connect to: " + urlString + "\n" + "                     Server is not responding.";
        } catch (java.io.InterruptedIOException e) {
            error = true;
            finished = true;
            errorStr = "Connection to Portal lost: communication is timeouted.";
            this.parentWorkflow.getMenuButtonEventHandler().stopAutomaticRefresh();
        } catch (java.net.MalformedURLException e) {
            error = true;
            finished = true;
            errorStr = "Error in communication: " + e.getMessage();
            System.out.println("postRequest()-MalformedURLException.");
            e.printStackTrace();
        } catch (Exception e) {
            error = true;
            finished = true;
            errorStr = "Error while trying to communicate the server: " + e.getMessage();
        }
        return responseString;
    }

    /**
     *This method uploads an object to the server.
     *@param object object to upload
     *@param targetName target file name in the multipart request
     *@return server's answer
     */
    public String uploadObject(Object object, String targetName) throws CommunicationException {
        String returnStr = new String();
        String requestString = new String();
        String boundary = "--8ah6j4h8das213--\n";
        HttpURLConnection connection = null;
        System.out.println("HTTPCommunication.uploadObject() - Object:" + object.getClass().getName() + " Targetname:" + targetName);
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=8ah6j4h8das213--");
            for (Iterator i = parameters.entrySet().iterator(); i.hasNext(); ) {
                java.util.Map.Entry e = (java.util.Map.Entry) i.next();
                requestString = requestString + boundary + "Content-Disposition: form-data; name=\"" + (String) e.getKey() + "\"\n\n" + (String) e.getValue() + "\n\n";
            }
            requestString += boundary + "Content-Disposition: form-data; " + "name=\"" + object.getClass().getName() + "\"; " + "filename=\"" + targetName + "\"\n" + "Content-Type: application/x-java-object\n\n";
            connection.connect();
            OutputStream out = connection.getOutputStream();
            out.write(requestString.getBytes("UTF-8"));
            GZIPOutputStream gzipOut = new GZIPOutputStream(out);
            ObjectOutput objectOut = new ObjectOutputStream(gzipOut);
            objectOut.writeObject(object);
            gzipOut.finish();
            objectOut.flush();
            out.write(("\n\n" + boundary + "\n\n").getBytes("UTF-8"));
            objectOut.close();
            System.out.println("HTTPCommunication.uploadObject() - Response:" + connection.getResponseCode() + " : " + connection.getResponseMessage());
            if (connection.getResponseCode() == connection.HTTP_OK) {
                InputStreamReader streamReader = new InputStreamReader(connection.getInputStream());
                BufferedReader bufferedReader = new BufferedReader(streamReader);
                while (bufferedReader.ready()) {
                    returnStr += bufferedReader.readLine();
                }
                bufferedReader.close();
            } else {
                throw new CommunicationException(connection.getResponseMessage(), connection.getResponseCode());
            }
        } catch (java.net.ConnectException ce) {
            throw new CommunicationException("Cannot connect to " + urlString + ".\n" + "Server is not responding!", ce);
        } catch (java.net.MalformedURLException mfue) {
            throw new CommunicationException("Cannot connect to " + urlString + ".\n" + "Bad url string!", mfue);
        } catch (java.io.InterruptedIOException iioe) {
            this.parentWorkflow.getMenuButtonEventHandler().stopAutomaticRefresh();
            throw new CommunicationException("Communication is timeouted", iioe);
        } catch (java.io.IOException ioe) {
            throw new CommunicationException("Error while trying to communicate the server: \n" + ioe.getMessage(), ioe);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return returnStr;
    }

    /**
     *Downloads an desired object from the server
     *@return desired object
     */
    public Object downloadObject() throws CommunicationException, FileNotFoundException, InvalidClassException, ClassNotFoundException {
        Object returnObject = null;
        String requestStr = new String();
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            for (java.util.Iterator i = parameters.entrySet().iterator(); i.hasNext(); ) {
                java.util.Map.Entry e = (java.util.Map.Entry) i.next();
                requestStr += URLEncoder.encode((String) e.getKey(), "UTF-8") + "=" + URLEncoder.encode((String) e.getValue(), "UTF-8") + "&";
            }
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.connect();
            PrintWriter out = new PrintWriter(connection.getOutputStream());
            out.println(requestStr);
            out.close();
            System.out.println("HTTPCommunication.downloadObject() - Response:" + connection.getResponseCode() + " : " + connection.getResponseMessage());
            if (connection.getResponseCode() == connection.HTTP_OK) {
                GZIPInputStream gzipIn = new GZIPInputStream(connection.getInputStream());
                ObjectInputStream objectIn = new ObjectInputStream(gzipIn);
                returnObject = objectIn.readObject();
                objectIn.close();
            } else if (connection.getResponseCode() == connection.HTTP_NOT_FOUND) {
                throw new FileNotFoundException(connection.getResponseMessage());
            } else {
                throw new CommunicationException(connection.getResponseMessage(), connection.getResponseCode());
            }
        } catch (java.net.ConnectException ce) {
            throw new CommunicationException("Cannot connect to " + urlString + ".\n" + "Server is not responding!", ce);
        } catch (java.net.MalformedURLException mfue) {
            throw new CommunicationException("Cannot connect to " + urlString + ".\n" + "Bad url string!", mfue);
        } catch (ClassNotFoundException cnfe) {
            throw cnfe;
        } catch (InvalidClassException ice) {
            throw ice;
        } catch (java.io.FileNotFoundException fnfe) {
            throw fnfe;
        } catch (java.io.InterruptedIOException iioe) {
            this.parentWorkflow.getMenuButtonEventHandler().stopAutomaticRefresh();
            throw new CommunicationException("Communication is timeouted", iioe);
        } catch (java.io.IOException ioe) {
            throw new CommunicationException("Error while trying to communicate the server: \n" + ioe.getMessage(), ioe);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return returnObject;
    }

    public javax.swing.Timer getCommunicationTimer() {
        return this.communicationTimer;
    }

    class ActualTask {

        ActualTask() {
        }

        public void setUploadProgress(int size, int position) {
            if (size <= 0) {
                progressPercent = 0;
                return;
            }
            progressPercent = java.lang.Math.round(((long) ((long) (position + 1) * 100)) / size);
        }

        public String postFileRequest(String fileName, String internalFileName) throws Exception {
            status = STATUS_INIT;
            String responseString = null;
            String requestStringPostFix = new String("");
            if (isThreadStopped) {
                return "";
            }
            status = STATUS_UPLOADING;
            if (isThreadStopped) {
                return "";
            }
            String requestString = new String("");
            int contentLength = 0, c = 0, counter = 0;
            try {
                for (java.util.Iterator i = parameters.entrySet().iterator(); i.hasNext(); ) {
                    java.util.Map.Entry e = (java.util.Map.Entry) i.next();
                    requestString = requestString + "-----------------------------7d338a374003ea\n" + "Content-Disposition: form-data; name=\"" + (String) e.getKey() + "\"\n\n" + (String) e.getValue() + "\n\n";
                }
                URL url = new URL(urlString);
                URLConnection connection = url.openConnection();
                HttpURLConnection httpConn = (HttpURLConnection) connection;
                requestString = requestString + "-----------------------------7d338a374003ea\n" + "Content-Disposition: form-data; name=\"" + internalFileName + "\"; filename=\"" + fileName + "\"\n" + "Content-Type: text/plain\n\n";
                requestStringPostFix = requestStringPostFix + "\n\n" + "-----------------------------7d338a374003ea\n" + "\n";
                FileInputStream fis = null;
                String str = null;
                try {
                    fis = new FileInputStream(fileName);
                    int fileSize = fis.available();
                    contentLength = requestString.length() + requestStringPostFix.length() + fileSize;
                    httpConn.setRequestProperty("Content-Length", String.valueOf(contentLength));
                    httpConn.setRequestProperty("Content-Type", "multipart/form-data; boundary=---------------------------7d338a374003ea");
                    httpConn.setRequestMethod("POST");
                    httpConn.setDoOutput(true);
                    httpConn.setDoInput(true);
                    try {
                        connection.connect();
                    } catch (ConnectException ec2) {
                        error = true;
                        finished = true;
                        errorStr = "Cannot connect to: " + urlString;
                        System.out.println("Cannot connect to:" + urlString);
                    } catch (java.io.InterruptedIOException e) {
                        error = true;
                        finished = true;
                        errorStr = "Connection to Portal lost: communication is timeouted.";
                        parentWorkflow.getMenuButtonEventHandler().stopAutomaticRefresh();
                    } catch (IllegalStateException ei) {
                        error = true;
                        finished = true;
                        errorStr = "IllegalStateException: " + ei.getMessage();
                    }
                    OutputStream out = httpConn.getOutputStream();
                    byte[] toTransfer = requestString.getBytes("UTF-8");
                    for (int i = 0; i < toTransfer.length; i++) {
                        out.write(toTransfer[i]);
                    }
                    int count;
                    int zBUFFER = 8 * 1024;
                    setUploadProgress(fileSize, counter);
                    byte data[] = new byte[zBUFFER];
                    GZIPOutputStream zos = new GZIPOutputStream(out);
                    while ((count = fis.read(data, 0, zBUFFER)) != -1) {
                        if (isThreadStopped) {
                            return "";
                        }
                        zos.write(data, 0, count);
                        setUploadProgress(fileSize, counter);
                        counter += count;
                    }
                    zos.flush();
                    zos.finish();
                    setUploadProgress(fileSize, counter);
                    toTransfer = requestStringPostFix.getBytes("UTF-8");
                    for (int i = 0; i < toTransfer.length; i++) {
                        out.write(toTransfer[i]);
                    }
                    out.close();
                } catch (IOException e) {
                    finished = true;
                    error = true;
                    errorStr = "Error in Uploading file: " + fileName;
                } finally {
                    try {
                        fis.close();
                    } catch (IOException e2) {
                    }
                }
                InputStreamReader isr = new InputStreamReader(httpConn.getInputStream());
                BufferedReader br = new BufferedReader(isr);
                String temp;
                String tempResponse = "";
                while ((temp = br.readLine()) != null) {
                    if (isThreadStopped) {
                        return "";
                    }
                    tempResponse = tempResponse + temp + "\n";
                    setDecompressStatusAtUpload(temp);
                }
                responseString = tempResponse;
                isr.close();
            } catch (ConnectException ec) {
                error = true;
                finished = true;
                errorStr = "Cannot connect to: " + urlString + "\nServer is not responding.";
            } catch (java.io.InterruptedIOException e) {
                error = true;
                finished = true;
                errorStr = "Connection to Portal lost: communication is timeouted.";
                parentWorkflow.getMenuButtonEventHandler().stopAutomaticRefresh();
            } catch (IOException e2) {
                finished = true;
                error = true;
                errorStr = "IOError in postFileRequest: " + e2.getMessage();
            } catch (Exception e4) {
                finished = true;
                error = true;
                errorStr = "Error while trying to communicate the server: " + e4.getMessage();
            }
            return responseString;
        }

        public String postDownloadRequest(String localFile) throws Exception {
            String responseString = "";
            String requestString = "";
            if (localFile == null) {
                error = true;
                errorStr = errorStr.concat("No local target for: " + currentFile.getRelativePath() + "\n");
                return "";
            }
            try {
                for (java.util.Iterator i = parameters.entrySet().iterator(); i.hasNext(); ) {
                    java.util.Map.Entry e = (java.util.Map.Entry) i.next();
                    requestString = requestString + URLEncoder.encode((String) e.getKey(), "UTF-8") + "=" + URLEncoder.encode((String) e.getValue(), "UTF-8") + "&";
                }
                URL url = new URL(urlString);
                URLConnection connection = url.openConnection();
                HttpURLConnection httpConn = (HttpURLConnection) connection;
                httpConn.setRequestProperty("Content-Length", String.valueOf(requestString.length()));
                httpConn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                httpConn.setRequestMethod("POST");
                httpConn.setDoOutput(true);
                httpConn.setDoInput(true);
                connection.connect();
                PrintWriter out = new PrintWriter(httpConn.getOutputStream());
                out.println(requestString);
                out.close();
                if (httpConn.HTTP_NOT_FOUND == httpConn.getResponseCode()) {
                    error = true;
                    errorStr = errorStr.concat("Cannot find file: " + currentFile.getRelativePath() + "\n");
                    return responseString;
                }
                String localFileName = new String(localFile);
                File f = new File(localFileName);
                File dir = new File(f.getParent());
                dir.mkdirs();
                FileOutputStream fis = new FileOutputStream(f);
                try {
                    InputStream is = httpConn.getInputStream();
                    java.util.zip.GZIPInputStream gin = new java.util.zip.GZIPInputStream(new BufferedInputStream(is));
                    int temp;
                    while ((temp = gin.read()) != -1) {
                        fis.write(temp);
                    }
                    if (fis.getChannel().size() > 0) {
                        fis.getChannel().truncate(fis.getChannel().size() - 1);
                    }
                    responseString = downloadDir + "/" + currentFile.getRelativePath();
                    is.close();
                    fis.close();
                    httpConn.disconnect();
                } catch (IOException io) {
                    error = true;
                    errorStr = errorStr.concat("Cannot find file: " + currentFile.getRelativePath() + "\n");
                    return responseString;
                }
            } catch (java.net.ConnectException conne) {
                error = true;
                finished = true;
                errorStr = "Cannot connect to: " + urlString;
            } catch (java.io.InterruptedIOException e) {
                error = true;
                finished = true;
                errorStr = "Connection to Portal lost: communication is timeouted.";
                parentWorkflow.getMenuButtonEventHandler().stopAutomaticRefresh();
            } catch (java.net.MalformedURLException e) {
                error = true;
                finished = true;
                errorStr = "Error in postDownloadRequest()";
            } catch (Exception e) {
                e.printStackTrace();
                error = true;
                finished = true;
                errorStr = "Error in Download: " + e.getMessage();
            }
            return responseString;
        }
    }
}
