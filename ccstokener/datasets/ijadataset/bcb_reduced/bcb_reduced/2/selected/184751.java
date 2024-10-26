package api.server.userRoles;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import api.client.bpmModel.Component;
import api.server.jUDDI.api_v2_uddi.CategoryBag;

/**
 * the class implements the interface Role, 	
 * all method are implemented on the basis ServiceEntwickler role. 
 * @author WMin
 *
 */
public final class ServiceEntwickler implements Role {

    /**
	 * constant.
	 */
    private static final int C = 3;

    /**
	 * returns the role name of user.
	 * @return String
	 */
    public String beschreiben() {
        return "serviceEntwickler";
    }

    /**
		 * the administrator cann the role of user change
		 * if parameter is enduser, 
		 * then the role changed to enduser
		 * if parameter is administrator, then the role changed to administrator
		 * returns ansonst null.
		 * @param n String
		 * @return Role
		 */
    public Role changeRole(final String n) {
        if (n.equalsIgnoreCase("administrator")) {
            return new Admin();
        }
        if (n.equalsIgnoreCase("enduser")) {
            return new Enduser();
        }
        return null;
    }

    /**
	 * .
	 */
    public void call() {
        System.out.println("SE");
    }

    /**
	 * 
	 */
    public void find() {
    }

    /**
	  * 
	  */
    public void login() {
    }

    /**
	  * 
	  */
    public void getView() {
    }

    /**  
	  * 
	  */
    public void changeView() {
    }

    /**
	  * 
	  */
    public void addEventListener() {
    }

    /**
	  * 
	  */
    public void informListener() {
    }

    /**
	  * 
	  */
    public void getLastEventQueue() {
    }

    /**reads the WAR-file that in a certain path,  
	     * writes it automatisch to the webapps of the Tomcat,
	     * returns true, if it succeed.
	     * @param webapps String
	     * @param absolutepfad String
	     * @param filename String
	     * @return boolean
	     */
    public boolean deployLocalWAR1(final String absolutepfad, final String filename, final String webapps) {
        System.out.println("die file speichert unter:" + webapps + filename);
        DataInputStream is = null;
        DataOutputStream os = null;
        try {
            is = new DataInputStream(new FileInputStream(new File(absolutepfad)));
            os = new DataOutputStream(new FileOutputStream(new File(webapps + filename)));
            final int n = 1024;
            byte[] by = new byte[n];
            int m;
            while ((m = is.read(by)) != -1) {
                os.write(by, 0, m);
                os.flush();
            }
            is.close();
            os.close();
            return true;
        } catch (IOException e) {
            System.err.println(e.getCause());
            return false;
        }
    }

    /**reads the WAR-file that in a certain path,  
	     * writes it automatisch to the webapps of the Tomcat,
	     * returns 1, which has succeed,
	     * returns 0, which indicates that a malformed URL has occurred,
	     * returns 2, which indicates that a HttpURLConnection 
	     * problem has occurred,
	     * returns 3, which indicates that a IOException has occurred.
	     * @param urlPath String
	      * @param filename String
	      * @param webapps String
	      * @return String
	      */
    public int deployRemoteWAR(final String urlPath, final String filename) {
        String message = null;
        URL url = null;
        System.out.print("path" + urlPath);
        int i = 1;
        try {
            url = new URL(urlPath);
            System.out.print("\n URL" + url.toString());
        } catch (MalformedURLException ex1) {
            message = ex1.toString() + "MalformedURLException";
            System.out.print(message);
            i = 0;
        }
        HttpURLConnection connection = null;
        try {
            connection = (HttpURLConnection) url.openConnection();
        } catch (IOException ex2) {
            message = ex2.toString() + "IOException";
            System.out.print(message);
            i = 2;
        }
        try {
            DataInputStream in = new DataInputStream(connection.getInputStream());
            DataOutputStream outStream = new DataOutputStream(new FileOutputStream(new File("tmp/" + filename)));
            final int n = 1024;
            byte[] by = new byte[n];
            int m;
            while ((m = in.read(by)) != -1) {
                outStream.write(by, 0, m);
                outStream.flush();
            }
            in.close();
            outStream.close();
            System.out.print("success");
        } catch (IOException ex3) {
            message = ex3.toString() + " rein";
            i = C;
        }
        System.out.print("over");
        return i;
    }

    /**
	 * 
	 */
    public void registerAtUDDI() {
    }

    /**
     * undeploy the file.
     * @param absolutepath String
     * @return boolean
     */
    public boolean undeploy(final String absolutepath) {
        File f = new File(absolutepath);
        if (f.exists()) {
            f.delete();
            return true;
        }
        return false;
    }

    /**
     * 
     */
    public void getModel() {
    }

    /**
     * 
     */
    public void test() {
    }

    /**
     * 
     */
    public void buildService() {
    }

    /**
     * 
     */
    public void addExternalervice() {
    }

    /**
     * 
     */
    public void comment() {
        System.out.print("SE");
    }

    /**
	 *
	 * @param username String
	 * @param password String
	 * @param name String
	 * @param description String
	 * @param name1 String
	 * @param description1 String
	 * @param description2 String
	 * @param accesspoint String
	 * @param bag CategoryBag
	 */
    public void registerAtUDDI(final String username, final String password, final String name, final String description, final String name1, final String description1, final String description2, final String accesspoint, final CategoryBag bag) {
    }

    /**
	 * @param c Component
	 */
    public void addComponent(final Component c) {
    }

    /**
	 * 
	 */
    public void cleanupStubs() {
    }

    /**
	 * @param f File
	 */
    public void deploy(final File f) {
    }

    /**
	 * @return Object
	 */
    public Object excecute() {
        return null;
    }

    /**
	 * @return Iterator
	 */
    public Iterator getChildrenIt() {
        return null;
    }

    /**
	 * @param s String
	 * @return Methode
	 */
    public Method getMethod(final String s) {
        return null;
    }

    /**
	 * @return Methode[]
	 */
    public Method[] getMethods() {
        return null;
    }

    /**
	 * @param m Method
	 * @return Class[]
	 */
    public Class[] getParameterList(final Method m) {
        return null;
    }

    /**
	 * @param m Method
	 * @return Class
	 */
    public Class getReturnType(final Method m) {
        return null;
    }

    /**
	 * @param o Object[]
	 * @param m Method
	 * @return Object
	 */
    public Object invoke(final Method m, final Object[] o) {
        return null;
    }

    /**
	 * 
	 */
    public void listDeployed() {
    }

    /**
	 * @param name String
	 */
    public void removeComponent(final String name) {
    }

    /**
	 * @param s String
	 * @return String
	 */
    public String toBPELFile(final String s) {
        return null;
    }

    /**
	 * @param s String
	 * @return String
	 */
    public String toCatalogFile(final String s) {
        return null;
    }

    /**
	 * @param s String
	 * @return String
	 */
    public String toPDDFile(final String s) {
        return null;
    }

    /**
	 * @param s String
	 * @return String
	 */
    public String toWSDLFile(final String s) {
        return null;
    }

    /**
	 * @param m Method
	 * @param o Object[]
	 * @return boolean
	 */
    public boolean typeCheck(final Method m, final Object[] o) {
        return false;
    }

    /**
	 * @param f File
	 */
    public void undeploy(final File f) {
        f.delete();
    }

    /**
	 * 
	 */
    public void update() {
    }

    /**
	 * 
	 * @param dis DataInputStream
	 * @param name String
	 */
    public void deploy(final DataInputStream dis, final String name) {
        DataOutputStream os = null;
        try {
            os = new DataOutputStream(new FileOutputStream(new File("tmp/" + name)));
            final int n = 1024;
            byte[] by = new byte[n];
            int m;
            while ((m = dis.read(by)) != -1) {
                os.write(by, 0, m);
                os.flush();
            }
            dis.close();
            os.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e2) {
            e2.printStackTrace();
        }
    }
}
