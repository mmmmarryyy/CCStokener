import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;

/**
 * A class that implements configurable color schemes that can be saved to a file.
 * @author Joni Toivanen (jomiolto@gmail.com)
 */
public class ColorSchemes {

    /**
	 * A header text that is included in the beginning of all the saved scheme files.
	 */
    private final String schemeHeader = "# This is a color scheme file for Jave Lines and Dots v. " + JLad.getVersion() + "\n" + "# This file was written by the game executable, but you can freely\n" + "# modify this file by hand (as long as you keep it valid)...\n\n";

    /**
	 * The filename of the colour schemes file.
	 */
    private String filename;

    /**
	 * A list of the colour schemes.
	 */
    private LinkedList schemes;

    /**
	 * Constructor that loads the colour schemes from the given file.
	 * @param file the full path name of the file to load the color schemes from
	 * @throws IOException if the schemes could not be loaded from the given file
	 */
    public ColorSchemes(String file) throws IOException {
        this.schemes = new LinkedList();
        this.filename = file;
        this.load();
    }

    /**
	 * Constructor that loads the color schemes from the given URL.
	 * @param url an URL to the file where the color schemes are loaded from
	 * @throws IOException if the schemes could not be loaded from the given URL
	 */
    public ColorSchemes(URL url) throws IOException {
        this.schemes = new LinkedList();
        this.filename = null;
        this.load(url);
    }

    /**
	 * Returns the number of color schemes.
	 * @return the number of color schemes
	 */
    public int getCount() {
        return (this.schemes.size());
    }

    /**
	 * Returns an iterator for going through the list of color schemes.
	 * @return an iterator for stepping through the color schemes
	 */
    public Iterator iterator() {
        return this.schemes.iterator();
    }

    /**
	 * Loads the color schemes from the file that has been set for the class.
	 * @throws IOException if the file couldn't be read
	 */
    public void load() throws IOException {
        if (this.filename == null) throw new IOException("Filename not set in ColorSchemes.load()");
        BufferedReader br = new BufferedReader(new FileReader(this.filename));
        this.load(br);
        br.close();
    }

    /**
	 * Loads the color schemes from an URL.
	 * @param url the url to load the schemes from
	 * @throws IOException if the reading fails
	 */
    public void load(URL url) throws IOException {
        if (url == null) throw new IOException("null URL in ColorSchemes.load(url)");
        InputStream is = url.openConnection().getInputStream();
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        this.load(br);
        br.close();
        is.close();
    }

    /**
	 * Tells if a warning about parse error has already been shown, to avoid showing multiple warnings if
	 * the color scheme file has more than one error.
	 */
    private boolean warningShown = false;

    /**
	 * Shows a warning about the color scheme file not passing the parser.
	 */
    public void parseWarning() {
        if (this.warningShown) return;
        Dialogues.warning("Error parsing color schemes, some of the color schemes might be unusable!");
    }

    /**
	 * Loads the color schemes from the given BufferedReader.
	 * @param reader the BufferedReader object to use for IO
	 * @throws IOException if the reading fails
	 */
    public void load(BufferedReader br) throws IOException {
        this.warningShown = false;
        schemes = new LinkedList();
        String line;
        while ((line = br.readLine()) != null) {
            line = ConfigFile.stripComment(line);
            line = line.trim();
            if (line.length() == 0) continue;
            if (!line.matches("begin \".*\"")) {
                Debug.msg("Error parsing line: \"" + line + "\", begin expected\n");
                this.parseWarning();
                continue;
            }
            int i = line.indexOf('\"');
            if (i == -1) {
                Debug.msg("Error parsing line: \"" + line + "\", couldn't parse name\n");
                this.parseWarning();
                continue;
            }
            String name = ConfigFile.parseQuotedString(line.substring(i));
            if (name == null) {
                Debug.msg("Error parsing line: \"" + line + "\", couldn't parse name\n");
                this.parseWarning();
                continue;
            }
            this.parseColorScheme(name, br);
        }
    }

    /**
	 * Parses a color scheme from a color scheme file.
	 * @param name the name of the color scheme
	 * @param br the BufferedReader used to access the file
	 * @throws IOException if the reading fails
	 */
    public void parseColorScheme(String name, BufferedReader br) throws IOException {
        Scheme scheme = new Scheme(name);
        String line;
        while ((line = br.readLine()) != null) {
            line = ConfigFile.stripComment(line);
            line = line.trim();
            if (line.length() == 0) continue;
            if (line.compareToIgnoreCase("end") == 0) break;
            String[] strings = line.split("=");
            if (strings.length != 2) {
                Debug.msg("Error parsing line \"" + line + "\", color definition expected!\n");
                this.parseWarning();
                continue;
            }
            strings[0] = strings[0].trim();
            strings[1] = ConfigFile.parseQuotedString(strings[1]);
            if (strings[1] == null) {
                Debug.msg("Error parsing line: \"" + line + "\", syntax error!\n");
                this.parseWarning();
                continue;
            }
            try {
                scheme.setColor(strings[0], ConfigFile.parseColor(strings[1]));
            } catch (ColorFormatException e) {
                Debug.msg("Error parsing line: \"" + line + "\", bad color string\n");
                this.parseWarning();
                continue;
            }
        }
        if (line == null) {
            Debug.msg("Warning, color scheme without \"end\" found.\n");
            this.parseWarning();
        }
        this.schemes.add(scheme);
    }

    /**
	 * Save the color schemes to a file.
	 * @throws IOException if the color schemes could not be written
	 */
    public void save() throws IOException {
        this.warningShown = false;
        if (this.filename == null) {
            Dialogues.warning("Error while trying to save the color schemes!");
            throw new IOException("Filename not set in ColorSchemes.save()");
        }
        BufferedWriter bw = new BufferedWriter(new FileWriter(this.filename));
        bw.write(this.schemeHeader);
        Iterator i = this.iterator();
        while (i.hasNext()) {
            Scheme scheme = (Scheme) i.next();
            bw.write("begin \"" + scheme.getName() + "\"\n");
            for (int c = 0; c < Preferences.colors.length; c++) bw.write("\t" + Preferences.colors[c] + " = \"" + ConfigFile.colorString(scheme.getColor(c)) + "\"\n");
            bw.write("end\n\n");
        }
        bw.close();
    }

    /**
	 * Changes the filename of the color schemes.
	 * @param file the new filename of the color schemes
	 */
    public void setFilename(String file) {
        this.filename = file;
        if (file == null) return;
        try {
            this.save();
        } catch (IOException e) {
            Dialogues.warning("Could not save the color schemes!");
            Debug.msg("Error, could not save the color schemes in ColorSchemes.setFilename()\n");
        }
    }

    /**
	 * Sets another color scheme.
	 * @param name the name of color scheme to change to
	 * @throws ColorSchemeNotFoundException if the given color scheme does not exist
	 */
    public void setScheme(String name) {
        Iterator i = this.iterator();
        while (i.hasNext()) {
            Scheme scheme = (Scheme) i.next();
            if (scheme.getName().equals(name)) {
                ColorSchemes.setSchemeColors(scheme);
                return;
            }
        }
        Debug.msg("Trying to set non-existing color scheme \"" + name + "\"\n");
        Dialogues.warning("Error while trying to set color scheme \"" + name + "\".");
    }

    /**
	 * Adds the given color scheme to the list of color schemes.
	 * @param scheme the Scheme to add
	 */
    public void addScheme(Scheme scheme) {
        this.schemes.add(scheme);
    }

    /**
	 * Removes the given color scheme from the list of color schemes.
	 * @param name the name of the scheme to remove.
	 */
    public void removeScheme(String name) {
        this.removeScheme(this.getColorScheme(name));
    }

    /**
	 * Removes the given color scheme from the list of color schemes.
	 * @param scheme the scheme to remove
	 */
    public void removeScheme(Scheme scheme) {
        this.schemes.remove(scheme);
    }

    /**
	 * Returns a color scheme by its name.
	 * @param name the name of the color scheme to look for
	 */
    public Scheme getColorScheme(String name) {
        Iterator i = this.iterator();
        while (i.hasNext()) {
            Scheme scheme = (Scheme) i.next();
            if (scheme.getName().equals(name)) return scheme;
        }
        Debug.msg("Color scheme \"" + name + "\" not found!\n");
        return null;
    }

    /**
	 * Sets the colors of the given color scheme.
	 * @param scheme the color scheme from which to set the colors
	 */
    public static void setSchemeColors(Scheme scheme) {
        Preferences.setColors(scheme.getColorList());
    }
}
