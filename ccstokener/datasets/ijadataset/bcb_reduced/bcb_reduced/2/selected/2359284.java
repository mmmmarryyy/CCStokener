package titancommon.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import titancommon.Performance;
import titancommon.applications.Application;
import titancommon.services.ServiceDirectory;

public class ClientGetService {

    /** Path to the local resoruce directory */
    private static String SERVICE_PATH = "titan/applications/";

    private static String RESOURCE_PATH = "cfg/";

    /** server location */
    private static String SERVER_URL = "http://emitai.ee.ethz.ch:9000/";

    public ClientGetService() {
    }

    /** writes the data received from the server into a file */
    private static boolean writeData(String strPath, byte[] data) {
        try {
            File file = new File(strPath);
            FileOutputStream rcStream = new FileOutputStream(file);
            rcStream.write(data);
            rcStream.close();
            return true;
        } catch (Exception e) {
            System.err.println("Could not write: " + strPath);
            return false;
        }
    }

    /**
	 * This function retrieves the available applications on the server, which 
	 * can be composed of the given services
	 * @param availServices List of services available to the network
	 * @return List of possible appliations and their data
	 */
    public static String[] getAppList(int[] availServices) {
        try {
            String strServices = "services=0";
            for (int i = 0; i < availServices.length; i++) {
                strServices += "+" + availServices[i];
            }
            URL url = new URL(SERVER_URL);
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
            Performance.printEvent("HTTPSend services, size: [" + strServices.length() + "]");
            writer.write(strServices);
            writer.flush();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String strLine;
            List services = new ArrayList();
            while ((strLine = reader.readLine()) != null) {
                Performance.printEvent("HTTPRecv services, size: [" + strLine.length() + "]");
                try {
                    services.add(new Service(strLine));
                } catch (Exception e) {
                    System.err.println("Could not parse service line: \"" + strLine + "\"");
                }
            }
            writer.close();
            reader.close();
            String[] strReturn = new String[services.size()];
            for (int i = 0; i < strReturn.length; i++) {
                strReturn[i] = ((Service) services.get(i)).getName();
            }
            return strReturn;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static boolean getServiceFile(String strServerPath, String strLocalPath) throws Exception {
        URL url = new URL(strServerPath);
        URLConnection ucon = url.openConnection();
        ucon.getDoInput();
        InputStream input = ucon.getInputStream();
        byte[] data = new byte[ucon.getContentLength()];
        input.read(data);
        Performance.printEvent("HTTPRecv service file, size: [" + data.length + "]");
        writeData(strLocalPath, data);
        return true;
    }

    /** Get service from the server */
    public static Application getService(String strService) {
        try {
            URL url = new URL(SERVER_URL + strService + ".srv");
            InputStream input = url.openStream();
            URLConnection ucon = url.openConnection();
            ucon.connect();
            byte[] data = new byte[ucon.getContentLength()];
            input.read(data);
            Performance.printEvent("HTTPRecv service SRV, size: [" + data.length + "]");
            Service newService = new Service(new String(data));
            getServiceFile(SERVER_URL + strService + ".class", SERVICE_PATH + strService + ".class");
            for (int i = 0; i < newService.resources.length; i++) {
                if (newService.resources[i].indexOf(".class") == -1) {
                    getServiceFile(SERVER_URL + newService.resources[i], RESOURCE_PATH + newService.resources[i]);
                } else {
                    getServiceFile(SERVER_URL + newService.resources[i], SERVICE_PATH + newService.resources[i]);
                }
            }
            Application newApp = (Application) Class.forName("titan.applications." + strService).newInstance();
            return newApp;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
	 * @param args
	 * @throws ClassNotFoundException 
	 * @throws IllegalAccessException 
	 * @throws InstantiationException 
	 */
    public static void main(String[] args) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
        System.out.println("\n\n\nTesting service download");
        Application ts = ClientGetService.getService("TestApp");
        if (ts != null) {
            ts.startApplication(null, null, null);
        }
    }
}
