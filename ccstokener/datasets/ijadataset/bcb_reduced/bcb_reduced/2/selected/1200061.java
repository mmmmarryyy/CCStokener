package bump3;

import bump3.*;
import bump3.engines.*;
import java.util.Scanner;
import java.io.*;
import javax.swing.JCheckBoxMenuItem;
import java.net.*;
import javax.swing.JOptionPane;
import java.awt.Desktop;

public class Methods {

    /** prints a line (include line break at the end)
	 *  useful because it checks if QUIET (-q) is set
	 *  @param txt text to print
	 */
    public static void p(String txt) {
        if (Main.QUIET == false) System.out.println(txt);
    }

    /** prints a single line
	 *  useful because it checks if QUIET (-q) is set
	 *  @param txt text to print
	 */
    public static void pr(String txt) {
        if (Main.QUIET == false) System.out.print(txt);
    }

    /** print verbose
	 *  only prints text that is visible in "Very Verbose" mode (-V)
	 *  @param txt text to print in only-verbose mode
	 */
    public static void pv(String txt) {
        if (Main.VERBOSE) System.out.println(txt);
    }

    /** updates progress bar text (status bar) in GUI
	 *  @param txt the status text
	 */
    public static void status(String txt) {
        if (Main.GUI && Main.theGUI != null) {
            Gui.progBar.setString(txt);
        }
    }

    /** changes the progress bar value (for GUI)
	 *  @param percent value of progress bar
	 */
    public static void progbar(int percent) {
        try {
            Gui.progBar.setValue(percent);
        } catch (NullPointerException npe) {
        }
    }

    /** checks if the user has clicked 'stop' and returns true
	 *  otherwise, returns false
	 *  @return true if GuiSearch.STOP is true, otherwise false
	 */
    public static boolean GuiStop() {
        if (Main.GUI) return GuiSearch.STOP; else return false;
    }

    /** searches each search engine (if no engines are specified)
	 *  used by the interactive (command-line) portion of the program
	 *  @param artist name of the artist
	 *  @param title title of the song
	 *  @return result of the search (1 is success, 
	 *         anything else is some degree of failure (couldn't find, error, etc)
	 */
    public static int searchSong(String artist, String title) {
        if (!Main.ENGINE.equals("")) {
            String s = Main.ENGINE;
            if (s.equalsIgnoreCase("google") || s.equalsIgnoreCase("google.com")) return Google.search(artist, title); else if (s.equalsIgnoreCase("dreammedia") || s.equalsIgnoreCase("dreammedia.ru")) return Dreammedia.search(artist, title); else if (s.equalsIgnoreCase("findmp3s") || s.equalsIgnoreCase("findmp3s.com")) return Findmp3s.search(artist, title); else if (s.equalsIgnoreCase("oth") || s.equalsIgnoreCase("oth.net")) return Oth.search(artist, title); else if (s.equalsIgnoreCase("seekasong") || s.equalsIgnoreCase("seekasong.com")) return Seekasong.search(artist, title); else if (s.equalsIgnoreCase("espew") || s.equalsIgnoreCase("espew.net")) return Espew.search(artist, title); else if (s.equalsIgnoreCase("dilandau") || s.equalsIgnoreCase("dilandau.com")) return Dilandau.search(artist, title); else if (s.equalsIgnoreCase("pepperoni") || s.equalsIgnoreCase("pepperoni.com") || s.equalsIgnoreCase("pep")) return Pepperoni.search(artist, title); else if (s.equalsIgnoreCase("emp3world") || s.equalsIgnoreCase("emp3world.com") || s.equalsIgnoreCase("emp3")) return Emp3World.search(artist, title); else if (s.equalsIgnoreCase("mp3skull") || s.equalsIgnoreCase("mp3skull.com")) return Mp3skull.search(artist, title); else if (s.equalsIgnoreCase("mp3center") || s.equalsIgnoreCase("mp3-center") || s.equalsIgnoreCase("mp3-center.org")) return Mp3center.search(artist, title); else if (s.equalsIgnoreCase("downloads.nl") || s.equalsIgnoreCase("downloadsnl") || s.equalsIgnoreCase("downloads")) return Downloadsnl.search(artist, title); else if (s.equalsIgnoreCase("4shared") || s.equalsIgnoreCase("4shared.com")) return FourShared.search(artist, title); else if (s.equalsIgnoreCase("prostopleer") || s.equalsIgnoreCase("prostopleer.com")) return Prostopleer.search(artist, title); else if (s.equalsIgnoreCase("myzuka") || s.equalsIgnoreCase("myzuka.ru")) return Myzuka.search(artist, title); else if (s.equalsIgnoreCase("plugin")) {
                Plugin pl = new Plugin("plugin.txt");
                return pl.search(artist, title);
            }
        }
        int searchResult = 0;
        if (searchResult != 1) searchResult = Dilandau.search(artist, title);
        if (searchResult != 1) searchResult = Mp3skull.search(artist, title);
        if (searchResult != 1) searchResult = Myzuka.search(artist, title);
        if (searchResult != 1) searchResult = Pepperoni.search(artist, title);
        if (searchResult != 1) searchResult = Dreammedia.search(artist, title);
        if (searchResult != 1) searchResult = FourShared.search(artist, title);
        if (searchResult != 1) searchResult = Emp3World.search(artist, title);
        if (searchResult != 1) searchResult = Findmp3s.search(artist, title);
        if (searchResult != 1) searchResult = Oth.search(artist, title);
        if (searchResult != 1) searchResult = Seekasong.search(artist, title);
        if (searchResult != 1) searchResult = Mp3center.search(artist, title);
        if (searchResult != 1) searchResult = Downloadsnl.search(artist, title);
        if (searchResult != 1) searchResult = Prostopleer.search(artist, title);
        if (searchResult != 1) searchResult = Google.search(artist, title);
        return searchResult;
    }

    /** This seemed like a good idea at the time...
	 *  ... bah i probably won't finish it
	 *  @param artist the artist' name
	 *  @param album the title of the album
	 *  @return integer showing the result of the search (0 fail, 1 success, etc)
	 */
    public static int searchAlbum(String artist, String album) {
        return 0;
    }

    /** converts hexidecimal characters to ascii
	 *  useful for webpages that link to hexidecimal urls
	 *  does NOT convert every character from hex to ascii,
	 *  only converts hex values that are preceded with a % symbol
	 *
	 *  example: hexToString("Hello%20World") would return "Hello World"
	 *
	 *  @param hex the string to convert
	 *  @return the converted string
	 */
    public static String hexToString(String hex) {
        String result = "";
        for (int i = 0; i < hex.length(); i++) {
            if (hex.charAt(i) == '%' && hex.length() - i >= 3) {
                char c = '%';
                try {
                    c = (char) Integer.parseInt(hex.substring(i + 1, i + 3), 16);
                } catch (NumberFormatException nfe) {
                    i -= 2;
                }
                result = result + c;
                i += 2;
            } else {
                result = result + hex.charAt(i);
            }
        }
        return result;
    }

    /** converts seconds into the H:M:S format
	 * doesn't print hours. will add 0's to the beginning of numbers to be easier to read
	 * i.e. 3m11s 0m02s 4h09m20s
	 * @param s the seconds to convert
	 * @return H:M:S format of the seconds
	*/
    public static String secToTime(int s) {
        int h, m;
        h = s / 3600;
        s = s % 3600;
        m = s / 60;
        s = s % 60;
        return (h == 0 ? "" : h + "h") + (m < 9 && h != 0 ? "0" : "") + m + "m" + (s < 9 ? "0" : "") + s + "s";
    }

    /** converts bytes into human-readable format
	 *  i.e. 1024 bytes = 1kb, 1024*1024 bytes = 1mb, and so on
	 *  only shows up to 1 significant digit
	 *  @param bytes number of bytes to convert
	 *  @return human readable format of bytes entered
	 */
    public static String bytesToSize(int bytes) {
        String result = "?";
        String b[] = new String[] { "b", "kb", "mb", "gb", "tb" };
        double d = (double) bytes;
        for (int i = 0; i < b.length; i++) {
            if (d < 1024) return String.format("%.1f", d) + "" + b[i];
            d /= 1024;
        }
        return result;
    }

    /** converts milliseconds to seconds, without a crazy-long decimal
	 *  @param ms amount in milliseconds to convert
	 *  @return a seconds-representation of the milliseconds, to one significant digit
	 */
    public static String msToSec(int ms) {
        double d = (double) ms / 1000;
        return String.format("%.1f", d);
    }

    /** loads the program's settings from bump3.ini
	 *  most of the values are stored in public static variables in Main.java
	 */
    public static void loadSettings() {
        Scanner f = null;
        try {
            String dir = Main.PROG_DIR;
            f = new Scanner(new FileReader(dir + "bump3.ini"));
        } catch (FileNotFoundException fnfe) {
            return;
        }
        while (f.hasNext()) {
            String line = f.nextLine();
            if (line.indexOf("=") < 0) continue;
            String p = line.substring(0, line.indexOf("=")), v = line.substring(line.indexOf("=") + 1);
            if (p.equals("min_filesize")) Main.MIN_FILESIZE = Integer.parseInt(v); else if (p.equals("save_dir")) {
                if (fileExists(v)) Main.SAVE_DIR = v;
            } else if (p.equals("autoplay")) Main.AUTOPLAY = Boolean.parseBoolean(v); else if (p.equals("autoplay_wait")) Main.AUTOPLAY_WAIT = Integer.parseInt(v); else if (p.equals("playstring")) Main.PLAYSTRING = v; else if (p.equals("connect_timeout")) Main.CONNECT_TIMEOUT = Integer.parseInt(v); else if (p.equals("read_timeout")) Main.READ_TIMEOUT = Integer.parseInt(v); else if (p.equals("recent_files")) {
                Main.RECENTS = v.split(";");
                String temp = "";
                for (int i = 0; i < Main.RECENTS.length; i++) {
                    if (fileExists(Main.RECENTS[i])) {
                        temp += Main.RECENTS[i];
                        if (i + 1 != Main.RECENTS.length) temp += ";";
                    }
                }
                if (temp.equals("")) Main.RECENTS = new String[] {}; else Main.RECENTS = temp.split(";");
            } else if (p.equals("engines")) {
                String[] temp = v.split(",");
                for (int i = 0; i < temp.length; i++) {
                    if (temp[i].equals("0")) Main.ENGINES[i] = false;
                }
            } else if (p.equals("theme")) Main.THEME = Boolean.parseBoolean(v); else if (p.equals("agreed")) Main.AGREED = Boolean.parseBoolean(v); else if (p.equals("nospaces")) Main.NOSPACES = Boolean.parseBoolean(v); else if (p.equals("msg")) {
                try {
                    if (Main.OS.startsWith("Windows")) {
                        if (fileExists(Main.PROG_DIR + "upgrayedd.vbs")) Runtime.getRuntime().exec("del \"" + Main.PROG_DIR + "upgrayedd.vbs\"");
                        if (fileExists(Main.PROG_DIR + "upgrayedd.bat")) Runtime.getRuntime().exec("del \"" + Main.PROG_DIR + "upgrayedd.bat\"");
                    } else {
                    }
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
                JOptionPane.showMessageDialog(null, v, "BuMP3 - Startup Message", JOptionPane.INFORMATION_MESSAGE);
            } else if (p.equals("downloaded_urls")) {
                Main.DOWNLOADED = v.split(";");
            } else if (p.equals("nodupes")) Main.NODUPES = Boolean.parseBoolean(v);
        }
    }

    /** saves the program's settings in bump3.ini
	 *  most of the values are saved from public static variables in Main.java
	 *  gives the option to add onto of the default settings that will be saved
	 *  @param newSetting a new setting to save
	 */
    public static void saveSettings(String newSetting) {
        FileWriter f = null;
        try {
            String dir = Main.PROG_DIR;
            f = new FileWriter(dir + "bump3.ini");
            f.write("min_filesize=" + Main.MIN_FILESIZE + "\n");
            f.write("save_dir=" + Main.SAVE_DIR + "\n");
            f.write("autoplay=" + Main.AUTOPLAY + "\n");
            f.write("autoplay_wait=" + Main.AUTOPLAY_WAIT + "\n");
            f.write("playstring=" + Main.PLAYSTRING + "\n");
            f.write("connect_timeout=" + Main.CONNECT_TIMEOUT + "\n");
            f.write("read_timeout=" + Main.READ_TIMEOUT + "\n");
            f.write("theme=" + Main.THEME + "\n");
            f.write("agreed=" + Main.AGREED + "\n");
            f.write("nospaces=" + Main.NOSPACES + "\n");
            f.write("nodupes=" + Main.NODUPES + "\n");
            if (Main.RECENTS != null) f.write("recent_files=" + join(Main.RECENTS, ";") + "\n");
            if (Main.theGUI != null) f.write("engines=" + join(Gui.mnuEngines, ",") + "\n");
            if (Main.DOWNLOADED != null && Main.NODUPES) f.write("downloaded_urls=" + join(Main.DOWNLOADED, ";") + "\n");
            if (!newSetting.equals("")) {
                f.write(newSetting + "\n");
            }
        } catch (IOException ioe) {
            System.out.println("ERROR! " + ioe.getMessage());
        } finally {
            try {
                if (f != null) f.close();
            } catch (IOException ioe) {
            }
        }
    }

    /** checks if a url has already been downloaded
	 *  returns true if it has already been downloaded
	 *  returns false if it hasn't, and adds it to the list of downloaded urls
	 *  @param url URL of the site to check
	 *  @return true if it has been downloaded, false if it has NOT (and adds url to the list)
	 */
    public static boolean hasBeenDownloaded(String url) {
        for (int i = 0; i < Main.DOWNLOADED.length; i++) {
            if (url.equals(Main.DOWNLOADED[i])) return true;
        }
        Main.DOWNLOADED = add(Main.DOWNLOADED, url);
        return false;
    }

    /** joins a String array into one string, separated by a delimiter
	 *  @param s the string array to join
	 *  @param delimiter what to separate each array item with
	 *  @return the string array separated by delimiters
	 */
    public static String join(String[] s, String delimiter) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < s.length; i++) {
            buffer.append(s[i].toString());
            if (i + 1 != s.length) buffer.append(delimiter);
        }
        return buffer.toString();
    }

    /** add a String to an array of Strings
     *  @param s the string array to add to
     *  @param item the item to add to the string
     *  @return the string array, including the item to add
     */
    public static String[] add(String[] s, String item) {
        String b = "";
        int min = 0;
        if (s.length > 8) min = s.length - 8;
        for (int i = min; i < s.length; i++) {
            if (s[i].equals(item)) return s;
            if (s[i].equals("")) continue;
            b += s[i] + ";";
        }
        b += item;
        return b.split(";");
    }

    /** combines an array of JCheckBoxMenuItems' values into a string, separated by a string delimiter
     *  @param b the array of JCheckBoxMenuItems from which to grab values and combine into a string
     *  @param delimiter what to separate each menu item's value with
     *  @return a string of the menu box's values separated by delimiber
     */
    public static String join(JCheckBoxMenuItem[] b, String delimiter) {
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < b.length; i++) {
            buffer.append((b[i].getState() == true ? "1" : "0"));
            if (i + 1 != b.length) buffer.append(delimiter);
        }
        return buffer.toString();
    }

    /** checks if a file OR a directory exists
     *  @param file the file or directory to check
     *  @return whether or not a file exists (true=exists, false=does not exist)
     */
    public static boolean fileExists(String file) {
        File f = null;
        boolean result = false;
        try {
            f = new File(file);
            result = f.exists();
        } catch (NullPointerException npe) {
            npe.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    /** launches the command to play a song
     *  uses Main's "PLAYSTRING" string
     *  @param file path of the song to play
     */
    public static void playSong(String file) {
        String[] cmd = Main.PLAYSTRING.split(" ");
        try {
            for (int i = 0; i < cmd.length; i++) {
                if (cmd[i].equalsIgnoreCase("%s")) cmd[i] = file;
            }
            Runtime.getRuntime().exec(cmd);
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /** launches a web-browser to the specified URL
	 *  @param url the website to go to
	 */
    public static void openUrl(String url) {
        String os = System.getProperty("os.name");
        Runtime runtime = Runtime.getRuntime();
        try {
            if (os.startsWith("Windows")) {
                String cmd = "rundll32 url.dll,FileProtocolHandler " + url;
                Process p = runtime.exec(cmd);
            } else if (os.startsWith("Mac OS")) {
            } else {
                String[] browsers = { "firefox", "opera", "konqueror", "epiphany", "mozilla", "netscape" };
                String browser = null;
                for (int count = 0; count < browsers.length && browser == null; count++) if (runtime.exec(new String[] { "which", browsers[count] }).waitFor() == 0) {
                    browser = browsers[count];
                    break;
                }
                if (browser != null) runtime.exec(new String[] { browser, url });
            }
        } catch (Exception x) {
            System.err.println("Exception occurd while invoking Browser!");
            x.printStackTrace();
        }
    }

    /** checks google code for the latest revision
	 *  if we're at the latest revision, it returns "up2date"
	 *  if there's an update available, it returns the changes in the newest version (that we don't have yet)
	 *  if any errors occur, it returns ""
	 *  @return the latest changes to the revision, or "up2date" if we're already up to date
	 */
    public static String getLatestRevisionChanges() {
        String page = GetUrl.getURL("http://code.google.com/p/bump3/source/list");
        int i, j;
        i = page.indexOf("detail?r=");
        if (i < 0) return "";
        j = page.indexOf("\">", i);
        String rev = page.substring(i + 9, j);
        int r = -1;
        try {
            r = Integer.parseInt(rev);
        } catch (NumberFormatException nfe) {
            nfe.printStackTrace();
        }
        if (r <= Main.REVISION) return "up2date";
        i = page.indexOf("\"detail?r=", j);
        j = page.indexOf("</a>", i);
        if (i < 0 || j < 0) return "";
        i = page.indexOf("\">", i) + 2;
        String info = page.substring(i, j);
        info = info.replaceAll("&#39;", "\'");
        info = info.replaceAll("&quot;", "\"");
        int count = 0;
        for (i = 0; i < info.length(); i++) {
            String c = info.substring(i, i + 1);
            if (c.equals("\n")) count = 0; else if (c.equals(" ") && count > 40) {
                info = info.substring(0, i) + "\n   " + info.substring(i);
                count = 0;
            } else count++;
        }
        return info;
    }

    /** removes all spaces in a string
	 *  @param txt the text to remove spaces from
	 *  @return txt minus the spaces
	 */
    public static String trimspaces(String txt) {
        String result = "";
        for (int i = 0; i < txt.length(); i++) {
            char c = txt.charAt(i);
            if (c != ' ') result += c;
        }
        return result;
    }

    /** upgrades the program:
	 *  downoads the latest version from the site,
	 *  writes a shell script (or batch file) to replace the current .jar file with the updated one
	 *  and then execute the newer version
	 */
    public static boolean upgrayedd(String url) {
        int size = GetUrl.getFilesize(url);
        if (size <= 0) return false;
        String save = Main.PROG_DIR + "bump3_new.jar";
        BufferedInputStream in;
        URLConnection uc;
        try {
            uc = new URL(url).openConnection();
            uc.setConnectTimeout(Main.CONNECT_TIMEOUT);
            uc.setReadTimeout(Main.READ_TIMEOUT);
            uc.setRequestProperty("User-Agent", Main.USER_AGENT);
            in = new BufferedInputStream(uc.getInputStream());
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return false;
        }
        FileOutputStream fos;
        try {
            fos = new FileOutputStream(save);
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
            return false;
        }
        int BYTE_SIZE = 2048;
        BufferedOutputStream bout = new BufferedOutputStream(fos, BYTE_SIZE);
        byte data[] = new byte[BYTE_SIZE];
        int count, current = 0, per = 0, perd = 0;
        Methods.status("upgrayedding...");
        Methods.p("\n" + Main.GR + "[+]" + Main.G + " Upgrayedding...");
        try {
            while ((count = in.read(data, 0, BYTE_SIZE)) != -1) {
                bout.write(data, 0, count);
                current += count;
                per = (100 * current) / size;
                Methods.progbar(per);
            }
            bout.close();
            in.close();
            Methods.p("\n" + Main.GR + "[+]" + Main.G + " Upgrayedd download complete");
        } catch (SocketTimeoutException ste) {
            return false;
        } catch (IOException ioe) {
            ioe.printStackTrace();
            return false;
        } finally {
            try {
                bout.close();
            } catch (IOException ioe) {
            }
            try {
                in.close();
            } catch (IOException ioe) {
            }
        }
        String newline = System.getProperty("line.separator");
        if (Main.OS.startsWith("Windows")) {
            try {
                FileWriter fw = new FileWriter(Main.PROG_DIR + "upgrayedd.vbs");
                fw.write("CreateObject(\"Wscript.Shell\").Run \"\"\"\" & WScript.Arguments(0) & \"\"\"\", 0, False");
                fw.close();
                fw = new FileWriter(Main.PROG_DIR + "upgrayedd.bat");
                fw.write("@echo off" + newline + "del \"" + Main.PROG_DIR + Main.FILE_NAME + "\"" + newline + "move \"" + Main.PROG_DIR + "bump3_new.jar\" \"" + Main.PROG_DIR + Main.FILE_NAME + "\"" + newline + "java -jar \"" + Main.PROG_DIR + Main.FILE_NAME + "\"" + newline + "del \"" + Main.PROG_DIR + "upgrayedd.vbs" + newline + "del \"" + Main.PROG_DIR + "upgrayedd.bat" + newline);
                fw.close();
                if (Main.theGUI != null) {
                    Main.theGUI.setVisible(false);
                    saveSettings("msg=BuMP3 has been \"upgrayedd\"-ed to the latest revision.\n\nEnjoy!");
                }
                Runtime.getRuntime().exec(new String[] { "wscript.exe", "upgrayedd.vbs", "upgrayedd.bat" });
                System.exit(0);
            } catch (IOException ioe) {
                ioe.printStackTrace();
                return false;
            }
        } else {
            try {
                FileWriter fw = new FileWriter(Main.PROG_DIR + "upgrayedd.sh");
                fw.write("#/bin/sh" + newline + "rm \"" + Main.PROG_DIR + Main.FILE_NAME + "\"" + newline + "mv \"" + Main.PROG_DIR + "bump3_new.jar\" \"" + Main.PROG_DIR + Main.FILE_NAME + "\"" + newline + "java -jar \"" + Main.PROG_DIR + Main.FILE_NAME + "\"" + newline + "rm \"" + Main.PROG_DIR + "upgrayedd.sh\"" + newline);
                fw.close();
                if (Main.theGUI != null) {
                    Main.theGUI.setVisible(false);
                    saveSettings("msg=BuMP3 has been \"upgrayedd\"-ed to the latest revision.\n\nEnjoy!");
                }
                if (!System.getProperty("user.name").equals("root")) {
                    JOptionPane.showMessageDialog(null, "the newest version of BuMP3 has been downloaded as \n" + "'bump3_new.jar' in the same folder as this program.\n\n" + "you are not running as root, so the shell script that\n" + "replaces the old .jar with the _new.jar may not run properly.\n\n" + "if BuMP3 does not reload, simply move bump3_new.jar in place of " + Main.FILE_NAME + "!", "BuMP3 - Linux is iffy", JOptionPane.WARNING_MESSAGE);
                }
                Runtime.getRuntime().exec(new String[] { "chmod", "+x", Main.PROG_DIR + "upgrayedd.sh" });
                Runtime.getRuntime().exec(new String[] { "sh", "upgrayedd.sh" });
                System.exit(0);
            } catch (IOException ioe) {
                ioe.printStackTrace();
                return false;
            }
        }
        return true;
    }

    public static void launchDirectory(String dir) {
        boolean worked = false;
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(new File(dir));
                worked = true;
            }
        } catch (IOException ioe) {
            worked = false;
        }
        if (worked) return;
        if (Main.OS.startsWith("Windows")) {
            try {
                Runtime.getRuntime().exec(new String[] { "explorer", dir });
            } catch (IOException ioe) {
                System.out.println("Got erroragain : " + ioe.getMessage());
            }
        } else {
            String[] viewers = new String[] { "konqueror", "nautilus", "thunar", "emelfm", "rox-filer", "pcmanfm", "xnc", "mc" };
            try {
                for (int i = 0; i < viewers.length; i++) {
                    try {
                        if (Runtime.getRuntime().exec(new String[] { "which", viewers[i] }).waitFor() == 0) {
                            Runtime.getRuntime().exec(new String[] { viewers[i], "file:" + dir });
                            break;
                        }
                    } catch (InterruptedException ie) {
                        System.out.println("Got error: " + ie.getMessage());
                    }
                }
            } catch (IOException ioe) {
                System.out.println("Got error again: " + ioe.getMessage());
            }
        }
    }
}
