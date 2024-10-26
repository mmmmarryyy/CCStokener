package org.xith3d.utility.general;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * Ouput a file to the standard output
 * 
 * @author Amos Wenger (aka BlueSky)
 * @author Marvin Froehlich (aka Qudus)
 */
public class Cat {

    private static void cat(InputStream in, String filename) {
        try {
            BufferedReader bR = new BufferedReader(new InputStreamReader(in));
            if (filename != null) System.out.println("------------ Content of file : " + filename + " ------------");
            String line;
            while ((line = bR.readLine()) != null) {
                System.out.println(line);
            }
            System.out.println("------------ End of file : " + filename + " -------------");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void cat(File f) {
        try {
            cat(new FileInputStream(f), f.getPath());
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void cat(URL url) {
        try {
            cat(url.openStream(), url.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void cat(InputStream in) {
        cat(in, null);
    }
}
