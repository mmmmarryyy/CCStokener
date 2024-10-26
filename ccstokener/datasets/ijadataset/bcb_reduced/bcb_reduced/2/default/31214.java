import waba.applet.*;
import java.io.*;
import java.net.*;

public class VJUltimaToolJava extends VJUltimaTool {

    /**
	 * Load a given URL to a byte array. A given range can be specified through
	 * offset and size.
	 * 
	 * @param	szName - URL name relative to currentApplet.getCodeBase().
	 * @param	offset - offset in the stream
	 * @param	size - size of the byte array (0 means up to the end of the stream)
	 * @return	byte array
	 */
    byte[] loadUrlByteArray(String szName, int offset, int size) {
        byte[] baBuffer = new byte[size];
        try {
            URL url = new URL(waba.applet.Applet.currentApplet.getCodeBase(), szName);
            try {
                InputStream file = url.openStream();
                if (size == 0) {
                    int n = file.available();
                    baBuffer = new byte[n - offset];
                }
                DataInputStream dataFile = new DataInputStream(file);
                try {
                    dataFile.skip(offset);
                    dataFile.readFully(baBuffer);
                } catch (EOFException e) {
                    System.err.print(e.getMessage());
                }
                file.close();
            } catch (IOException e) {
                System.err.print(e.getMessage());
            }
        } catch (MalformedURLException e) {
            System.err.print(e.getMessage());
        }
        return baBuffer;
    }

    /**
	 * Save a given URL from a byte array.
	 * 
	 * @param	szName - URL name relative to currentApplet.getCodeBase().
	 * @param	baBuffer - byte array
	 */
    void saveUrlByteArray(String szName, byte baBuffer[]) {
        com.ms.security.PolicyEngine.assertPermission(com.ms.security.PermissionID.FILEIO);
        java.net.URL codeBase = Applet.currentApplet.getCodeBase();
        String cb = codeBase.toString().substring(6);
        try {
            FileOutputStream file = new FileOutputStream(cb + szName);
            try {
                file.write(baBuffer);
                file.close();
            } catch (IOException e) {
                System.err.print(e.getMessage());
            }
        } catch (IOException e) {
            System.err.print(e.getMessage());
        }
    }
}
