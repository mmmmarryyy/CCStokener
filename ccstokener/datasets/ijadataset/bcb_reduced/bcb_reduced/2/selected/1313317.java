package org.yaoqiang.bpmn.editor.util;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Random;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.imageio.ImageIO;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.yaoqiang.bpmn.editor.BPMNEditor;
import org.yaoqiang.bpmn.editor.action.BPMNModelActions;
import org.yaoqiang.bpmn.editor.swing.BPMNGraphComponent;
import org.yaoqiang.bpmn.editor.view.BPMNGraph;
import org.yaoqiang.graph.editor.swing.BaseEditor;
import org.yaoqiang.graph.editor.util.EditorUtils;
import org.yaoqiang.graph.util.Constants;
import org.yaoqiang.graph.util.Utils;
import sun.misc.BASE64Encoder;
import com.mxgraph.model.mxCell;
import com.mxgraph.model.mxGeometry;
import com.mxgraph.model.mxGraphModel;
import com.mxgraph.model.mxICell;
import com.mxgraph.util.mxCellRenderer;
import com.mxgraph.util.mxResources;

/**
 * BPMNEditorUtils
 * 
 * @author Shi Yaoqiang(shi_yaoqiang@yahoo.com)
 */
public class BPMNEditorUtils {

    public static boolean isWSDL11File(String importType) {
        if (importType.equals("http://schemas.xmlsoap.org/wsdl/")) {
            return true;
        }
        return false;
    }

    public static boolean isWSDL20File(String importType) {
        if (importType.equals("http://www.w3.org/TR/wsdl20/")) {
            return true;
        }
        return false;
    }

    public static boolean isXSDFile(String importType) {
        if (importType.equals("http://www.w3.org/2001/XMLSchema")) {
            return true;
        }
        return false;
    }

    public static String getFilePath(String location) {
        String filepath = location;
        File file = new File(location);
        if (file.exists() && file.isFile()) {
            return filepath;
        } else {
            File tmp = BPMNEditor.getCurrentFile();
            if (tmp != null) {
                filepath = tmp.getParent() + File.separator + location;
                file = new File(filepath);
                if (file.exists() && file.isFile()) {
                    return filepath;
                }
            }
        }
        return filepath;
    }

    public static String getRelativeFilePath(String location) {
        String filepath = location;
        File file = new File(location);
        if (file.exists() && file.isFile()) {
            File tmp = BPMNEditor.getCurrentFile();
            if (tmp != null) {
                if (tmp.getParent().equals(file.getParent())) {
                    filepath = file.getName();
                }
            }
        } else {
            return filepath;
        }
        return filepath;
    }

    public static List<String> getWSDLMessages(Document document) {
        List<String> messages = new ArrayList<String>();
        NodeList childNodes = document.getDocumentElement().getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node.getLocalName() != null && node.getLocalName().equals("message")) {
                messages.add(((Element) node).getAttribute("name"));
            }
        }
        return messages;
    }

    public static List<String> getWSDLTypes(Document document) {
        List<String> elements = new ArrayList<String>();
        NodeList childNodes = document.getDocumentElement().getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node.getLocalName() != null && node.getLocalName().equals("types")) {
                Node schema = node.getFirstChild().getNextSibling();
                if (schema != null && schema.hasChildNodes()) {
                    NodeList elementList = schema.getChildNodes();
                    for (int j = 0; j < elementList.getLength(); j++) {
                        Node element = elementList.item(j);
                        if (element.getLocalName() != null && element.getLocalName().equals("element")) {
                            elements.add(((Element) element).getAttribute("name"));
                        }
                    }
                }
                break;
            }
        }
        return elements;
    }

    public static List<String> getXSDElements(Document document) {
        List<String> elements = new ArrayList<String>();
        NodeList childNodes = document.getDocumentElement().getChildNodes();
        for (int i = 0; i < childNodes.getLength(); i++) {
            Node node = childNodes.item(i);
            if (node.getLocalName() != null && node.getLocalName().equals("element")) {
                elements.add(((Element) node).getAttribute("name"));
            }
        }
        return elements;
    }

    public static String getXmlFileType(Document document) {
        String type = "http://www.w3.org/2001/XMLSchema";
        NamedNodeMap attribs = document.getDocumentElement().getAttributes();
        for (int i = 0; i < attribs.getLength(); i++) {
            String uri = attribs.item(i).getNodeValue();
            if ("http://schemas.xmlsoap.org/wsdl/".equals(uri)) {
                type = uri;
                break;
            } else if ("http://www.w3.org/ns/wsdl".equals(uri)) {
                type = "http://www.w3.org/TR/wsdl20/";
                break;
            } else if ("http://www.omg.org/spec/BPMN/20100524/MODEL".equals(uri)) {
                type = uri;
                break;
            }
        }
        return type;
    }

    public static String getMediaType(String suffix) {
        String format = "";
        if (suffix.endsWith(".doc")) {
            format = "application/msword";
        } else if (suffix.endsWith(".docx") || suffix.endsWith(".pptx") || suffix.endsWith(".xlsx")) {
            format = "application/vnd.openxmlformats";
        } else if (suffix.endsWith(".gif")) {
            format = "image/gif";
        } else if (suffix.endsWith(".gz")) {
            format = "application/x-gzip";
        } else if (suffix.endsWith(".jpg") || suffix.endsWith(".jpeg")) {
            format = "image/jpeg";
        } else if (suffix.endsWith(".pdf")) {
            format = "application/pdf";
        } else if (suffix.endsWith(".png")) {
            format = "image/png";
        } else if (suffix.endsWith(".ppt")) {
            format = "application/mspowerpoint";
        } else if (suffix.endsWith(".ps")) {
            format = "application/postscript";
        } else if (suffix.endsWith(".vsd")) {
            format = "application/x-visio";
        } else if (suffix.endsWith(".xls")) {
            format = "application/excel";
        } else if (suffix.endsWith(".zip")) {
            format = "application/zip";
        } else {
            format = "application/octet-stream";
        }
        return format;
    }

    public static void initRecentFileList(BaseEditor editor) {
        JMenu menu = editor.getRecentFilesmenu();
        List<String> fileList = EditorUtils.getRecentFileList();
        if (!fileList.isEmpty() && fileList.size() > 0) {
            int num = 0;
            for (String filename : fileList) {
                menu.add(editor.bind(num++ + ": " + filename, BPMNModelActions.getOpenAction(filename)));
            }
        }
    }

    public static void addRecentFiletoList(BaseEditor editor, String filename) {
        JMenu menu = editor.getRecentFilesmenu();
        JMenuItem mItem;
        for (int i = 0; i < menu.getItemCount(); ++i) {
            mItem = (JMenuItem) menu.getMenuComponent(i);
            if (filename.equals(mItem.getText().substring(3))) {
                menu.remove(i);
            }
        }
        int recentFileListSize = 10;
        if (EditorUtils.getRecentFileList().size() == recentFileListSize) {
            menu.remove(menu.getItemCount() - 1);
        }
        menu.insert(editor.bind("0: " + filename, BPMNModelActions.getOpenAction(filename)), 0);
        for (int i = 0; i < menu.getItemCount(); ++i) {
            mItem = (JMenuItem) menu.getMenuComponent(i);
            mItem.setText(i + ": " + mItem.getText().substring(3));
        }
        EditorUtils.saveRecentFiles(menu);
    }

    public static void insertAdditionalParticipant(BPMNGraph graph, String id, String value, boolean toTop, Object parent) {
        mxGraphModel model = graph.getModel();
        if (model.getCell(id) != null) {
            return;
        }
        double yOffset = 0;
        String style = "";
        mxCell subprocess = Utils.getChoreographyActivity(graph, parent);
        mxGeometry subgeo = model.getGeometry(subprocess);
        if (toTop) {
            yOffset = subgeo.getY() - 1;
            style = "participantAdditionalTop";
        } else {
            yOffset = subgeo.getY() + subgeo.getHeight() - 1;
            style = "participantAdditionalBottom";
        }
        mxCell participantCell = new mxCell(value, new mxGeometry(0, yOffset, Constants.ACTIVITY_WIDTH, Constants.PARTICIPANT_HEIGHT), style);
        participantCell.setId(id);
        participantCell.setVertex(true);
        graph.addCell(participantCell, (mxICell) parent);
        Utils.arrangeChoreography(graph, parent, false);
        graph.refresh();
    }

    public static void loadConnections(Map<String, JSONObject> connections) {
        Properties props = EditorUtils.loadProperties(BPMNEditorConstants.YAOQIANG_CONNECTION_FILE);
        for (Entry<Object, Object> entry : props.entrySet()) {
            try {
                connections.put((String) entry.getKey(), new JSONObject(new JSONTokener((String) entry.getValue())));
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public static void saveConnection(String id, JSONObject connection) {
        EditorUtils.saveConfigure(id, connection.toJSONString(), BPMNEditorConstants.YAOQIANG_CONNECTION_FILE, "Yaoqiang BPMN Editor Connections");
    }

    public static void removeConnection(String id) {
        EditorUtils.removeConfigure(id, BPMNEditorConstants.YAOQIANG_CONNECTION_FILE, "Yaoqiang BPMN Editor Connections");
    }

    public static boolean testConnection(String vendor, String urlstring, String username, String password) {
        boolean success = false;
        try {
            if (vendor.equals("Activiti")) {
                urlstring += "/login";
            }
            URL url = new URL(urlstring);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestProperty("Content-type", "application/json");
            String data = "{\"userId\":\"" + username + "\",\"password\":\"" + password + "\"}";
            conn.getOutputStream().write(data.getBytes());
            conn.getOutputStream().flush();
            conn.getOutputStream().close();
            if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                InputStream is = conn.getInputStream();
                BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                String tempLine = rd.readLine();
                StringBuffer temp = new StringBuffer();
                while (tempLine != null) {
                    temp.append(tempLine);
                    tempLine = rd.readLine();
                }
                rd.close();
                is.close();
                success = new JSONObject(new JSONTokener(temp.toString())).optBoolean("success");
            } else {
                String message = "";
                if (conn.getResponseCode() == HttpURLConnection.HTTP_INTERNAL_ERROR) {
                    message = mxResources.get("checkNameAndPassword");
                } else if (conn.getResponseCode() == HttpURLConnection.HTTP_BAD_METHOD) {
                    message = mxResources.get("checkBaseURL");
                } else {
                    message = mxResources.get("checkServer");
                }
                JOptionPane.showMessageDialog(null, message, mxResources.get("connectionFailed"), JOptionPane.ERROR_MESSAGE);
            }
            conn.disconnect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ConnectException e) {
            JOptionPane.showMessageDialog(null, e.getMessage() + "\n" + mxResources.get("checkServer"), mxResources.get("connectionFailed"), JOptionPane.ERROR_MESSAGE);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return success;
    }

    public static boolean deploy(JSONObject con, File deployFile) {
        boolean success = false;
        String urlstring = con.optString("url");
        if (con.optString("vendor").equals("Activiti")) {
            urlstring += "/deployment";
        }
        URL url;
        try {
            BASE64Encoder enc = new BASE64Encoder();
            String userpassword = con.optString("username") + ":" + con.optString("password");
            String encodedAuthorization = enc.encode(userpassword.getBytes());
            String boundary = "**#Yaoqiang$**";
            int bytesRead, bytesAvailable, bufferSize;
            byte[] buffer;
            int maxBufferSize = 1 * 1024 * 1024;
            FileInputStream fileInputStream = new FileInputStream(deployFile);
            bytesAvailable = fileInputStream.available();
            bufferSize = Math.min(bytesAvailable, maxBufferSize);
            buffer = new byte[bufferSize];
            url = new URL(urlstring);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoInput(true);
            conn.setDoOutput(true);
            conn.setUseCaches(false);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Connection", "Keep-Alive");
            conn.setRequestProperty("Authorization", "Basic " + encodedAuthorization);
            conn.setRequestProperty("Content-type", "multipart/form-data;boundary=" + boundary);
            DataOutputStream dos = new DataOutputStream(conn.getOutputStream());
            dos.writeBytes("--" + boundary + "\r\nContent-Disposition: form-data; name=\"success\"\r\n\r\n");
            dos.writeBytes("success");
            dos.writeBytes("\r\n--" + boundary + "\r\nContent-Disposition: form-data; name=\"failure\"\r\n\r\n");
            dos.writeBytes("failure");
            String filename = BPMNEditor.getCurrentFile().getName();
            if (filename.endsWith(".bpmn20.xml")) {
                filename = filename.substring(0, filename.lastIndexOf(".bpmn20.xml")) + ".bar";
            } else {
                filename = filename.substring(0, filename.lastIndexOf(".")) + ".bar";
            }
            dos.writeBytes("\r\n--" + boundary + "\r\nContent-Disposition: form-data; name=\"deployment\"; filename=\"" + filename + "\"\r\n\r\n");
            bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            while (bytesRead > 0) {
                dos.write(buffer, 0, bufferSize);
                bytesAvailable = fileInputStream.available();
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
            }
            dos.writeBytes("\r\n--" + boundary + "--\r\n");
            fileInputStream.close();
            dos.flush();
            dos.close();
            int code = conn.getResponseCode();
            if (code == HttpURLConnection.HTTP_OK) {
                InputStream is = conn.getInputStream();
                BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                String tempLine = rd.readLine();
                StringBuffer temp = new StringBuffer();
                while (tempLine != null) {
                    temp.append(tempLine);
                    tempLine = rd.readLine();
                }
                rd.close();
                is.close();
                success = temp.indexOf("success()") > 0;
            }
            conn.disconnect();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return success;
    }

    public static File createZipFile(BPMNGraphComponent graphComponent, String barFilename) {
        String[] filenames;
        String filename = BPMNEditor.getCurrentFile().getName();
        if (!filename.endsWith(".bpmn20.xml")) {
            filename = filename.substring(0, filename.lastIndexOf(".")) + ".bpmn20.xml";
        }
        String randomFilename = System.getProperty("java.io.tmpdir", "/tmp") + "/yaoqiang_deploy_" + filename.substring(0, filename.lastIndexOf(".bpmn20.xml")) + new Random().nextInt(Integer.MAX_VALUE);
        String imageFilename = randomFilename + ".png";
        BufferedImage image = mxCellRenderer.createBufferedImage(graphComponent.getGraph(), null, 1, null, graphComponent.isAntiAlias(), null, graphComponent.getCanvas());
        if (image != null) {
            try {
                ImageIO.write(image, "png", new File(imageFilename));
            } catch (IOException e) {
                e.printStackTrace();
            }
            filenames = new String[] { filename, filename.substring(0, filename.lastIndexOf(".bpmn20.xml")) + ".png" };
        } else {
            filenames = new String[] { filename };
        }
        String zipFilename = barFilename;
        if (zipFilename == null) {
            zipFilename = randomFilename + ".bar";
        }
        ZipOutputStream out;
        try {
            out = new ZipOutputStream(new FileOutputStream(zipFilename));
            int len;
            byte[] buffer = new byte[1024];
            for (int i = 0; i < filenames.length; i++) {
                String tempname = BPMNEditor.getCurrentFile().getAbsolutePath();
                if (i == 1) {
                    tempname = imageFilename;
                }
                FileInputStream in = new FileInputStream(tempname);
                out.putNextEntry(new ZipEntry(filenames[i]));
                while ((len = in.read(buffer)) > 0) {
                    out.write(buffer, 0, len);
                }
                out.closeEntry();
                in.close();
            }
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new File(zipFilename);
    }
}
