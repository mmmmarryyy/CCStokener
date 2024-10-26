package uk.co.whisperingwind.framework;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;

/**
** Methods to access files contained in the same Jar file I am using.
*/
public class JarFile {

    /**
    ** Open an input stream from a file in my .JAR.
    **
    ** @param fileName the name of the file to open.
    ** @return an InputStream for the selected file.
    ** @exception IOException if the file cannot be opened.
    */
    public static InputStream openFileFromJar(String fileName) throws IOException {
        String path = "/uk/co/whisperingwind/docs/" + fileName;
        URL url = path.getClass().getResource(path);
        return url.openStream();
    }

    /**
    ** Copy a file, a line at a time, from the .JAR file to an output
    ** file. By using readLine and println, I can be sure the target
    ** file will have the correct text file format for the platform I
    ** am running on.
    **
    ** @param fileName the name of the input file (in the .JAR).
    ** @param target the target File -- the file to which the output
    ** will be written.
    ** @exception IOException if the input file cannot be opened or the
    ** output file cannot be written.
    */
    public static void copyTextFromJar(String fileName, File target) throws IOException {
        InputStream is = openFileFromJar(fileName);
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader input = new BufferedReader(isr);
        FileWriter writer = new FileWriter(target);
        PrintWriter output = new PrintWriter(writer);
        String line = input.readLine();
        while (line != null) {
            output.println(line);
            line = input.readLine();
        }
        input.close();
        output.close();
    }
}
