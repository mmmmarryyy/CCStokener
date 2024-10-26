package titan.server;

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
import titan.applications.Application;

public class ClientGetService {

    /** Path to the local resoruce directory */
    private static String SERVICE_PATH = "titan/applications/";

    private static String RESOURCE_PATH = "cfg/";

    /** server location */
    private static String SERVER_URL = "http://localhost:9000/";

    public ClientGetService() {
    }

    private static String[] split(String str, String split) {
        int iCurIndex = 0;
        int iOccurrences = 0;
        while ((iCurIndex = str.indexOf(split, iCurIndex) + split.length()) >= split.length()) iOccurrences++;
        String[] result = new String[iOccurrences + 1];
        if (result.length == 1) {
            result[0] = str;
            return result;
        }
        iCurIndex = 0;
        for (int i = 0; i < iOccurrences; i++) {
            if (i == 0) {
                int iNextIndex = str.indexOf(split);
                if (iNextIndex == -1) break;
                result[i] = str.substring(iCurIndex, iNextIndex);
                iCurIndex = iNextIndex + split.length() - 1;
            } else {
                int iNextIndex = str.indexOf(split, iCurIndex + split.length());
                if (iNextIndex == -1) break;
                result[i] = str.substring(iCurIndex + split.length(), iNextIndex);
                iCurIndex = iNextIndex + split.length() - 1;
            }
        }
        if (iCurIndex + split.length() >= str.length()) {
            result[result.length - 1] = "";
        } else {
            result[result.length - 1] = str.substring(iCurIndex + split.length(), str.length() - 1);
        }
        return result;
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
    public static String[] getServices(int[] availServices) {
        try {
            String strServices = "services=0";
            for (int i = 0; i < availServices.length; i++) {
                strServices += "+" + availServices[i];
            }
            URL url = new URL(SERVER_URL);
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
            writer.write(strServices);
            writer.flush();
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String strLine;
            List<Service> services = new ArrayList<Service>();
            while ((strLine = reader.readLine()) != null) {
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
                strReturn[i] = services.get(i).getName();
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
            Service newService = new Service(new String(data));
            getServiceFile(SERVER_URL + strService + ".class", SERVICE_PATH + strService + ".class");
            for (int i = 0; i < newService.resources.length; i++) {
                getServiceFile(SERVER_URL + newService.resources[i], RESOURCE_PATH + newService.resources[i]);
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
        ClientGetService.split("hello, there, how are you doing, now? test,gaga,,gool,", ",");
    }
}
