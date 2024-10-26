import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.lang.reflect.*;
import java.util.*;
import java.io.*;
import java.util.zip.*;
import java.net.*;

public class PageLoader {

    public static void main(String[] args) {
        new PageLoader(args);
    }

    public PageLoader(String[] args) {
        if (args.length < 2) return;
        try {
            URL url = new URL(args[0]);
            InputStream in = url.openStream();
            FileOutputStream out = new FileOutputStream(args[1]);
            byte[] buffer = new byte[10];
            int read;
            while (true) {
                read = in.read(buffer);
                if (read == -1) break;
                out.write(buffer, 0, read);
            }
            out.flush();
            out.close();
            in.close();
        } catch (Exception exc) {
            System.out.println(exc);
        }
        ;
    }
}
