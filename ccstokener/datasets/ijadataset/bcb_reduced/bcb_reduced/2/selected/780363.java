package com.hme.tivo.videostream;

import java.io.File;
import java.io.FileReader;
import java.io.BufferedReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Stack;
import java.util.Hashtable;
import com.tivo.hme.bananas.BApplication;
import com.tivo.hme.bananas.BView;
import com.tivo.hme.interfaces.IContext;
import com.tivo.hme.sdk.Factory;

public class videostream extends BApplication {

    public static final String TITLE = GLOBAL.TITLE;

    public static String DIR;

    public int level = 0;

    public InitialScreen screen;

    private Stack<String> lastEntry = new Stack<String>();

    private Stack<String> topDir = new Stack<String>();

    public void init(IContext context) {
        try {
            super.init(context);
        } catch (Exception e) {
        }
        debug.print("context=" + context);
        GLOBAL.topDirName = new Hashtable<String, String>();
        GLOBAL.positions = new Hashtable<String, Long>();
        GLOBAL.time2byte = new Hashtable<Long, Long>();
        parseConfig(GLOBAL.configFile);
        parseBookmarksFile();
        DIR = topDir.firstElement();
        screen = new InitialScreen(this, topDir);
        push(screen, TRANSITION_NONE);
    }

    public static class videostreamFactory extends Factory {

        @Override
        public InputStream getStream(String uri) throws IOException {
            debug.print("uri=" + uri);
            boolean isStreamFile = false;
            for (int i = 0; i < GLOBAL.extList.length; i++) {
                if (uri.toLowerCase().endsWith(GLOBAL.extList[i].toLowerCase())) {
                    isStreamFile = true;
                }
            }
            if (isStreamFile) {
                GLOBAL.streamFile = DIR + File.separator + uri;
                File file = new File(GLOBAL.streamFile);
                URL url = file.toURI().toURL();
                System.out.println("url=" + url);
                GLOBAL.cstream = new CountInputStream(url.openStream());
                if (GLOBAL.Resume && GLOBAL.positions.containsKey(GLOBAL.streamFile)) {
                    GLOBAL.Resume = false;
                    if (uri.toLowerCase().endsWith(".mpg") || uri.toLowerCase().endsWith(".vob") || uri.toLowerCase().endsWith(".mp2") || uri.toLowerCase().endsWith(".mpeg") || uri.toLowerCase().endsWith(".mpeg2")) {
                        System.out.println("--Skipping to last bookmark=" + GLOBAL.positions.get(GLOBAL.streamFile));
                        GLOBAL.cstream.skip(GLOBAL.positions.get(GLOBAL.streamFile));
                    }
                }
                return GLOBAL.cstream;
            }
            return super.getStream(uri);
        }
    }

    public String makeFileName(String name) {
        debug.print("name=" + name);
        return name.replaceFirst(GLOBAL.FOLDER_PREFIX, "");
    }

    public boolean handleAction(BView view, Object action) {
        debug.print("action=" + action);
        if (action.equals("push")) {
            Object entry = screen.list.get(screen.list.getFocus());
            String name = makeFileName(entry.toString());
            if (level == 0) {
                DIR = GLOBAL.topDirName.get(name);
                File d = new File(DIR);
                if (d.exists() == false) {
                    screen.list.remove(entry);
                    screen.list.setFocus(0, true);
                    return true;
                }
                lastEntryPush(entry.toString());
                level += 1;
                screen.updateFileList(DIR);
                if (GLOBAL.Resume) {
                    GLOBAL.Resume = false;
                    playList(DIR);
                }
                return true;
            } else {
                String fullName = DIR + File.separator + name;
                File d = new File(fullName);
                if (d.exists() == false) {
                    screen.updateFileList(DIR);
                    return true;
                }
                lastEntryPush(entry.toString());
                if (d.isDirectory() == true) {
                    if (GLOBAL.Resume) {
                        GLOBAL.Resume = false;
                        playList(fullName);
                    } else {
                        DIR = fullName;
                        level += 1;
                        screen.updateFileList(DIR);
                    }
                } else {
                    String fileName = new GLOBAL().makeFileName(name);
                    ViewScreen newScreen = (ViewScreen) entry;
                    newScreen.DIR = DIR;
                    newScreen.initialScreen = screen;
                    push(newScreen, TRANSITION_LEFT);
                    String url = getBelow().getContext().getBaseURI().toString() + fileName;
                    newScreen.startStream(url, fullName, fileName);
                }
            }
            return true;
        }
        if (action.equals("pop")) {
            return true;
        }
        if (action.equals("enter")) {
            int index = screen.list.getFocus();
            Stack<String> files = new Stack<String>();
            for (int i = index; i < screen.list.size(); i++) {
                String fullName = DIR + File.separator + makeFileName(screen.list.get(i).toString());
                File f = new File(fullName);
                if (f.isFile()) {
                    files.add(fullName);
                }
            }
            if (files.size() > 0) {
                GLOBAL.playList_lock = false;
                GLOBAL.playList.clear();
                GLOBAL.playList_index = 0;
                for (int i = 0; i < files.size(); i++) {
                    GLOBAL.playList.addElement(files.get(i));
                }
                playList_start(0);
            }
        }
        return super.handleAction(view, action);
    }

    public void playList(String dir) {
        debug.print("dir=" + dir);
        File newDir = new File(dir);
        FilenameFilter filter = new FilenameFilter() {

            public boolean accept(File dir, String name) {
                for (int i = 0; i < GLOBAL.extList.length; i++) {
                    if (name.toLowerCase().endsWith(GLOBAL.extList[i].toLowerCase())) {
                        return true;
                    }
                }
                return false;
            }
        };
        String[] files = newDir.list(filter);
        String[] sortedFiles = screen.getSortedByName(dir, files);
        GLOBAL.playList_lock = false;
        GLOBAL.playList.clear();
        GLOBAL.playList_index = 0;
        for (int i = 0; i < sortedFiles.length; i++) {
            GLOBAL.playList.addElement(dir + File.separator + sortedFiles[i]);
        }
        playList_start(0);
    }

    public void playList_start(int index) {
        debug.print("index=" + index);
        String fullName = GLOBAL.playList.get(index);
        File f = new File(fullName);
        String fileName = f.getName();
        String dir = f.getParent();
        System.out.println("Playlist item " + index + ": " + fullName);
        ViewScreen newScreen = new ViewScreen(getBelow().getBApp(), fileName);
        DIR = dir;
        newScreen.DIR = dir;
        newScreen.NAME = fileName;
        newScreen.initialScreen = screen;
        push(newScreen, TRANSITION_LEFT);
        String url = getBelow().getContext().getBaseURI().toString() + fileName;
        newScreen.startStream(url, fullName, fileName);
    }

    public boolean handleKeyPress(int code, long rawcode) {
        debug.print("code=" + code + " rawcode=" + rawcode);
        switch(code) {
            case KEY_LEFT:
                level -= 1;
                if (level < 0) {
                    setActive(false);
                }
                if (level == 0) {
                    screen.topDirList(topDir);
                    screen.focusOn(lastEntryPop());
                }
                if (level > 0) {
                    DIR = (new File(DIR)).getParent();
                    screen.updateFileList(DIR);
                    screen.focusOn(lastEntryPop());
                }
                return true;
            case KEY_NUM1:
                if (GLOBAL.sortOrder.equals("date")) {
                    GLOBAL.sortOrder = "alphanumeric";
                } else {
                    GLOBAL.sortOrder = "date";
                }
                screen.updateFileList(DIR);
                return true;
        }
        return super.handleKeyPress(code, rawcode);
    }

    public void lastEntryPush(String name) {
        debug.print("name=" + name);
        lastEntry.push(name);
    }

    public String lastEntryPop() {
        String name = lastEntry.pop();
        debug.print("name=" + name);
        return name;
    }

    public boolean handleApplicationError(int code, String message) {
        debug.print("code=" + code + " message=" + message);
        if (code != 3 && code != 4) {
            System.out.println("ERROR code=" + code + " Message: " + message);
        }
        return false;
    }

    public void parseConfig(String configFile) {
        debug.print("configFile=" + configFile);
        String PWD = System.getProperty("user.dir");
        try {
            BufferedReader config = new BufferedReader(new FileReader(configFile));
            System.out.println(">> Reading config file: " + configFile);
            try {
                String line = null;
                String key = null;
                while ((line = config.readLine()) != null) {
                    line = line.replaceFirst("^\\s*(.*$)", "$1");
                    line = line.replaceFirst("^(.*)\\s*$", "$1");
                    if (line.length() == 0) continue;
                    if (line.matches("^#.+")) continue;
                    if (line.matches("<.+>")) {
                        key = line.replace("<", "");
                        key = key.replace(">", "");
                        continue;
                    }
                    if (key.equals("topdir")) {
                        System.out.println(">>config: " + key + "=" + line);
                        String name;
                        String value;
                        if (line.matches("^.+\\s*==\\s*.+$")) {
                            name = line.replaceFirst("^(.+)\\s*==\\s*(.+)$", "$1");
                            value = line.replaceFirst("^(.+)\\s*==\\s*(.+)$", "$2");
                        } else {
                            name = line;
                            value = line;
                        }
                        File dir = new File(value);
                        if (dir.isDirectory()) {
                            if (value.equals(".")) value = PWD;
                            if (name.equals(".")) name = PWD;
                            topDir.push(value);
                            GLOBAL.topDirName.put(name, value);
                            GLOBAL.topDirName.put(value, name);
                        } else {
                            System.out.println("          ERROR: topdir does not exist or is a file: " + value);
                        }
                    }
                    if (key.equals("extensions")) {
                        System.out.println(">>config: " + key + "=" + line);
                        GLOBAL.extList = line.split("[ |\t]");
                    }
                    if (key.equals("font")) {
                        System.out.println(">>config: " + key + "=" + line);
                        if (line.matches("small") || line.matches("medium") || line.equals("large")) {
                            GLOBAL.FONT_SIZE = line;
                        } else {
                            System.out.println("          ERROR: font size not valid: must be small,medium,large");
                        }
                    }
                    if (key.equals("timeout_status_bar")) {
                        System.out.println(">>config: " + key + "=" + line);
                        GLOBAL.timeout_status_bar = Integer.parseInt(line);
                    }
                    if (key.equals("timeout_info")) {
                        System.out.println(">>config: " + key + "=" + line);
                        GLOBAL.timeout_info = Integer.parseInt(line);
                    }
                    if (key.equals("skip_back")) {
                        System.out.println(">>config: " + key + "=" + line);
                        GLOBAL.skip_back = Integer.parseInt(line);
                    }
                    if (key.equals("skip_forwards")) {
                        System.out.println(">>config: " + key + "=" + line);
                        GLOBAL.skip_forwards = Integer.parseInt(line);
                    }
                    if (key.equals("slow_speed")) {
                        System.out.println(">>config: " + key + "=" + line);
                        GLOBAL.slow_speed = Float.parseFloat(line);
                    }
                    if (key.equals("ffmpeg")) {
                        System.out.println(">>config: " + key + "=" + line);
                        GLOBAL.ffmpeg = line;
                    }
                }
            } finally {
                config.close();
            }
        } catch (IOException ex) {
            System.out.println(">> NOTE: No config file found, using defaults");
        }
        if (topDir.empty()) {
            topDir.push(PWD);
            GLOBAL.topDirName.put(PWD, PWD);
        }
    }

    public void parseBookmarksFile() {
        debug.print("");
        try {
            BufferedReader bookmark = new BufferedReader(new FileReader(GLOBAL.bookmarkFile));
            System.out.println(">> Reading bookmarks file: " + GLOBAL.bookmarkFile);
            try {
                String line = null;
                while ((line = bookmark.readLine()) != null) {
                    line = line.replaceFirst("^\\s*(.*$)", "$1");
                    line = line.replaceFirst("^(.*)\\s*$", "$1");
                    if (line.length() == 0) continue;
                    if (line.matches("^#.+")) continue;
                    String pair[] = line.split("__##__");
                    GLOBAL.positions.put(pair[0], new Long(pair[1]));
                    debug.print(pair[0] + "=" + pair[1]);
                }
            } finally {
                bookmark.close();
            }
        } catch (IOException ex) {
            System.out.println(">> NOTE: No bookmark file found");
        }
    }

    public static InputStream ffmpegStream(String file) {
        debug.print("file=" + file);
        try {
            String cmdline = GLOBAL.ffmpeg + " -i \"" + file + "\" -acodec copy -vcodec copy t.mpg";
            Process p = Runtime.getRuntime().exec(cmdline);
            return p.getInputStream();
        } catch (Exception err) {
            err.printStackTrace();
        }
        return null;
    }
}
