package server;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import webWof.StrippedGameDB;
import webWof.WebMessage;

public class testmain {

    /**
	 * @param args
	 * @throws IOException 
	 * @throws ClassNotFoundException 
	 */
    public static void main(String[] args) throws IOException, ClassNotFoundException {
        URL urlServlet = null;
        try {
            urlServlet = new URL("http://wofproj.appspot.com/test");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        URLConnection con = null;
        try {
            con = urlServlet.openConnection();
        } catch (IOException e) {
            e.printStackTrace();
        }
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setUseCaches(false);
        con.setRequestProperty("Content-Type", "application/x-java-serialized-object");
        OutputStream outstream = con.getOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(outstream);
        oos.writeObject("tom");
        oos.flush();
        oos.close();
        InputStream instr = con.getInputStream();
        ObjectInputStream inputFromServlet = new ObjectInputStream(instr);
        Object retval = inputFromServlet.readObject();
        inputFromServlet.close();
        instr.close();
        System.out.println(retval.getClass().toString());
    }
}
