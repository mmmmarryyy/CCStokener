package edu.ups.gamedev.converter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import com.jmex.model.converters.FormatConverter;
import com.jmex.model.converters.ObjToJme;
import edu.ups.gamedev.player.Tank;

/**
 * A general purpose utility for converting various model formats to the jme format. 
 * 
 * @author Walker Lindley
 * @version $Revision: 1.1 $, $Date: 2008/01/26 08:13:22 $
 *
 */
public class ModelConverter {

    /**
	 * @param args
	 * @throws IOException 
	 * @throws MalformedURLException 
	 */
    public static void main(String[] args) throws IOException {
        convertObjToJme(Tank.LIGHT_TANK);
    }

    /**
	 * Converts obj file to the jme format and write it to a file.
	 * 
	 * @param file	the name of the file containing the model
	 * @throws IOException 
	 * @see edu.ups.gamedev.converter.ModelConverter#convertObjToJme(URL)
	 * 		convertObjToJme(URL)
	 */
    public static void convertObjToJme(String name) throws IOException {
        convertObjToJme(new File(name));
    }

    /**
	 * Converts obj file to the jme format and write it to a file.
	 * 
	 * @param file	<code>File</code> containing the model
	 * @throws IOException 
	 * @see edu.ups.gamedev.converter.ModelConverter#convertObjToJme(URL)
	 * 		convertObjToJme(URL)
	 */
    public static void convertObjToJme(File file) throws IOException {
        convertObjToJme(file.toURI().toURL());
    }

    /**
	 * Converts the given obj file to the jme format and writes it back to the file
	 * system. The new file will have the same name as the old one, but with a .jme
	 * extension. So if the original file is <code>model.obj</code> then the new
	 * file will be <code>model.jme</code>. It also assumes that any supporting
	 * files, suarg1ch as .mtl files,  will be in the same directory as the primary file.
	 * 
	 * @param url	<code>URL</code> of file to convert
	 * @throws IOException 
	 * @throws MalformedURLException
	 */
    public static void convertObjToJme(URL url) throws IOException {
        if (url == null) {
            System.err.println("url is null");
            return;
        }
        FormatConverter converter = new ObjToJme();
        converter.setProperty("mtllib", url);
        ByteArrayOutputStream outStream = new ByteArrayOutputStream();
        converter.convert(url.openStream(), outStream);
        URL newFile = new URL(url.toString().replaceAll(".obj", ".jme"));
        FileOutputStream fileStream = new FileOutputStream(newFile.toString());
        outStream.writeTo(fileStream);
    }
}
