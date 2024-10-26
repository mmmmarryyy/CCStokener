package util;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import javax.swing.JColorChooser;
import javax.swing.JFileChooser;

public class EasyInput {

    public static String quote(String s) {
        return "\"" + s + "\"";
    }

    /**
	 * Gets input from the console window typed by the user on the keyboard. If
	 * an IOException occurs, then an Error is thrown.
	 * 
	 * @return The string typed by the user.
	 */
    public static String input() {
        try {
            return rawInput();
        } catch (IOException e) {
            throw new Error("An IOException has occurred inside the input() method.");
        }
    }

    /**
	 * Gets input from the console window typed by the user on the keyboard. If
	 * an IOException occurs, then an Error is thrown.
	 * 
	 * @param prompt
	 *            The message to be displayed before the user enters their
	 *            input.
	 * @return The string typed by the user.
	 */
    public static String input(String prompt) {
        try {
            return rawInput(prompt);
        } catch (IOException e) {
            throw new Error("An IOException has occurred inside the input(String prompt) method.");
        }
    }

    /**
	 * Gets input from the console window typed by the user on the keyboard. The
	 * user's code is responsible for handling a possible IOException.
	 * 
	 * @return The string typed by the user.
	 * @throws IOException
	 */
    public static String rawInput() throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String message = in.readLine();
        return message;
    }

    /**
	 * Gets input from the console window typed by the user on the keyboard. The
	 * user's code is responsible for handling a possible IOException.
	 * 
	 * @param prompt
	 *            The message to be displayed before the user enters their
	 *            input.
	 * @return The string typed by the user.
	 * @throws IOException
	 */
    public static String rawInput(String prompt) throws IOException {
        System.out.print(prompt);
        return rawInput();
    }

    /**
	 * Lets the user choose a color by popping up a color chooser box.
	 * 
	 * @return color selected by the user; an Error is thrown if the user opts
	 *         out of the chooser
	 */
    public static Color chooseColor() {
        Color c = JColorChooser.showDialog(null, "Color Chooser", Color.RED);
        if (c == null) {
            throw new Error("No color chosen.");
        } else {
            return c;
        }
    }

    /**
	 * Returns the current working directory (cwd). The cwd is where files
	 * without any path name will be read from or written to.
	 * 
	 * @return The current working directory.
	 */
    public static String getcwd() {
        String cwd = System.getProperty("user.dir");
        return cwd;
    }

    /**
	 * Prints the current working directory to the console.
	 * 
	 */
    public static void printCwd() {
        System.out.println("Current working directory: " + getcwd());
    }

    /**
	 * <p>
	 * Returns an array of strings containing the names of all the files and
	 * folders in the current working directory.
	 * </p>
	 * <p>
	 * If you want, say, a list of strings instead of a an array of strings
	 * (e.g. lists are easier to print than arrays), then use the Arrays.asList
	 * method, e.g.
	 * </p>
	 * 
	 * <pre>
	 * System.out.println(&quot;Files and folders: &quot; + Arrays.asList(listdir()));
	 * </pre>
	 * 
	 * @return An array of strings containing the names of all files and folders
	 *         in the current working directory.
	 */
    public static String[] listdir() {
        return listdir(getcwd());
    }

    /**
	 * <p>
	 * Returns an array of strings containing the names of all the files and
	 * folders in the folder specified by path.
	 * </p>
	 * <p>
	 * If you want, say, a list of strings instead of a an array of strings
	 * (e.g. lists are easier to print than arrays), then use the Arrays.asList
	 * method, e.g.
	 * </p>
	 * 
	 * <pre>
	 * System.out.println(&quot;Files and folders: &quot; + Arrays.asList(listdir()));
	 * </pre>
	 * 
	 * @param path
	 *            The name of the folder to read.
	 * @return An array of strings containing the names of all the files and and
	 *         folders in path.
	 */
    public static String[] listdir(String path) {
        return listdir(new File(path));
    }

    public static String[] listdir(File dir) {
        return dir.list();
    }

    /**
	 * Pops up a file dialog box to allow the user to get a file name.
	 * 
	 * @return the fully qualified name of the file selected by the user
	 * @throws Error
	 *             if the user does not select a file
	 */
    public static String chooseFile() {
        JFileChooser fc = new JFileChooser(getcwd());
        int returnVal = fc.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();
            return file.getPath();
        } else {
            throw new Error("openFileDialog error: No file selected.");
        }
    }

    /**
	 * Converts a text file into a string.
	 * 
	 * @param path
	 *            The path of the text file to read.
	 * @return The contents of the file as a string.
	 * @throws IOException
	 */
    public static String rawFileToString(File path) throws IOException {
        FileReader in = new FileReader(path);
        StringBuffer sb = new StringBuffer();
        for (int c = -1; (c = in.read()) != -1; ) {
            sb.append(c);
        }
        in.close();
        return sb.toString();
    }

    public static String rawFileToString(String path) throws IOException {
        return rawFileToString(new File(path));
    }

    /**
	 * Converts a text file into a string.
	 * 
	 * @param path
	 *            The path of the text file to read.
	 * @return The contents of the file as a string.
	 */
    public static String fileToString(File path) {
        try {
            FileReader in = new FileReader(path);
            StringBuffer sb = new StringBuffer();
            for (int c = -1; (c = in.read()) != -1; ) {
                sb.append((char) c);
            }
            in.close();
            return sb.toString();
        } catch (FileNotFoundException e) {
            throw new Error("\nThe file " + quote(path.getName()) + " could not be found in the directory:\n" + quote(getcwd()) + "\n" + "Check that the file you want to read is in the directory\n" + "you think it's in, and check that all names are spelled\n" + "correctly, including any spaces or punctuation. The case\n" + "of a file name matters, e.g. \"MyFile.txt\" is different\n" + "than \"myfile.txt\"");
        } catch (IOException e) {
            throw new Error("An IOException has been thrown by the fileToString method:\n" + e);
        }
    }

    public static String fileToString(String path) {
        return fileToString(new File(path));
    }

    /**
	 * Opens a given web page an downloads the source code as a string.
	 * 
	 * @param url
	 *            the URL of a web page
	 * @return The contents of the web page as a string.
	 */
    public static String getWebPage(String url) {
        try {
            return getWebPage(new URL(url));
        } catch (MalformedURLException urlEx) {
            throw new Error("URL creation failed:\n" + quote(url) + "\nis not a validly formatted URL.");
        }
    }

    /**
	 * Opens a given web page an downloads the source code as a string.
	 * 
	 * @param urlObj
	 *            the URL of a web page
	 * @return The contents of the web page as a string.
	 */
    public static String getWebPage(URL urlObj) {
        try {
            String content = "";
            InputStreamReader is = new InputStreamReader(urlObj.openStream());
            BufferedReader reader = new BufferedReader(is);
            String line;
            while ((line = reader.readLine()) != null) {
                content += line;
            }
            return content;
        } catch (IOException e) {
            throw new Error("The page " + quote(urlObj.toString()) + "could not be retrieved." + "\nThis is could be caused by a number of things:" + "\n" + "\n  - the computer hosting the web page you want is down, or has returned an error" + "\n  - your computer does not have Internet access" + "\n  - the heat death of the universe has occurred, taking down all web servers with it");
        }
    }

    public static void main(String[] args) {
        System.out.println("This is a test of the EasyInput class ...\n");
        String name = input("What is your name? ");
        String age = input("What is your age?");
        System.out.println("name = " + quote(name));
        System.out.println("age = " + quote(age));
        System.out.println("Current working directory = " + quote(getcwd()));
        System.out.println("Files and folders: " + Arrays.asList(listdir()));
        String fname = chooseFile();
        System.out.println(fname);
        String file = fileToString(fname);
        System.out.println(file);
    }
}
