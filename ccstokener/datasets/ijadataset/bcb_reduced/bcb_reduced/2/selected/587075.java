package com.petersoft;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.lang.reflect.Array;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.Document;
import javax.swing.text.Highlighter;
import javax.swing.text.JTextComponent;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

public class CommonLib {

    public static boolean isNumeric(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (!(s.charAt(i) >= '0' && s.charAt(i) <= '9')) {
                return false;
            }
        }
        return true;
    }

    public static byte[] readFileByte(File file, long offset, int size) {
        return readFileByte(file.getAbsolutePath(), offset, size);
    }

    public static byte[] readFileByte(String filepath, long offset, int size) {
        try {
            RandomAccessFile br = new RandomAccessFile(filepath, "r");
            br.seek(offset);
            byte bytes[] = new byte[size];
            br.readFully(bytes);
            br.close();
            return bytes;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static Long string2decimal(String s) {
        if (s == null) {
            return 0L;
        } else {
            s = s.trim().toLowerCase();
            if (s.startsWith("0x")) {
                return Long.parseLong(s.substring(2), 16);
            } else {
                return Long.parseLong(s, 10);
            }
        }
    }

    public static boolean isNumber(String s) {
        try {
            long l = string2decimal(s);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public static long convertFilesize(String filesize) {
        filesize = filesize.trim().toLowerCase();
        if (filesize == null) {
            return 0;
        }
        int isHex = 10;
        if (filesize.startsWith("0x")) {
            filesize = filesize.substring(2);
            isHex = 16;
        }
        long returnValue = -1;
        if (filesize.length() == 0) {
            return 0;
        } else if (filesize.length() == 1) {
            return Long.parseLong(filesize, isHex);
        } else if (filesize.substring(filesize.length() - 1).toLowerCase().equals("t")) {
            try {
                return Long.parseLong(filesize.substring(0, filesize.length() - 1), isHex) * 1024 * 1024 * 1024 * 1024;
            } catch (Exception ex) {
            }
        } else if (filesize.substring(filesize.length() - 2).toLowerCase().equals("tb")) {
            try {
                return Long.parseLong(filesize.substring(0, filesize.length() - 2), isHex) * 1024 * 1024 * 1024 * 1024;
            } catch (Exception ex) {
            }
        } else if (filesize.substring(filesize.length() - 1).toLowerCase().equals("g")) {
            try {
                return Long.parseLong(filesize.substring(0, filesize.length() - 1), isHex) * 1024 * 1024 * 1024;
            } catch (Exception ex) {
            }
        } else if (filesize.substring(filesize.length() - 2).toLowerCase().equals("gb")) {
            try {
                return Long.parseLong(filesize.substring(0, filesize.length() - 2), isHex) * 1024 * 1024 * 1024;
            } catch (Exception ex) {
            }
        } else if (filesize.substring(filesize.length() - 1).toLowerCase().equals("m")) {
            try {
                return Long.parseLong(filesize.substring(0, filesize.length() - 1), isHex) * 1024 * 1024;
            } catch (Exception ex) {
            }
        } else if (filesize.substring(filesize.length() - 2).toLowerCase().equals("mb")) {
            try {
                return Long.parseLong(filesize.substring(0, filesize.length() - 2), isHex) * 1024 * 1024;
            } catch (Exception ex) {
            }
        } else if (filesize.substring(filesize.length() - 1).toLowerCase().equals("k")) {
            try {
                return Long.parseLong(filesize.substring(0, filesize.length() - 1), isHex) * 1024;
            } catch (Exception ex) {
            }
        } else if (filesize.substring(filesize.length() - 2).toLowerCase().equals("kb")) {
            try {
                return Long.parseLong(filesize.substring(0, filesize.length() - 2), isHex) * 1024;
            } catch (Exception ex) {
            }
        } else {
            try {
                return Long.parseLong(filesize, isHex);
            } catch (Exception ex) {
            }
        }
        return returnValue;
    }

    public static void centerDialog(JFrame dialog) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = dialog.getSize();
        if (frameSize.height > screenSize.height) {
            frameSize.height = screenSize.height;
        }
        if (frameSize.width > screenSize.width) {
            frameSize.width = screenSize.width;
        }
        dialog.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
    }

    public static void centerDialog(JDialog dialog) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = dialog.getSize();
        if (frameSize.height > screenSize.height) {
            frameSize.height = screenSize.height;
        }
        if (frameSize.width > screenSize.width) {
            frameSize.width = screenSize.width;
        }
        dialog.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
    }

    public static void centerDialog(JFileChooser chooser) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Dimension frameSize = chooser.getSize();
        if (frameSize.height > screenSize.height) {
            frameSize.height = screenSize.height;
        }
        if (frameSize.width > screenSize.width) {
            frameSize.width = screenSize.width;
        }
        chooser.setLocation((screenSize.width - frameSize.width) / 2, (screenSize.height - frameSize.height) / 2);
    }

    private static char numberOfFile_runningChar[] = { '-', '\\', '|', '/' };

    private static Object obj;

    public static int numberOfFile(File node, boolean isSlient) {
        int x = 0;
        for (File file : node.listFiles()) {
            if (!isSlient) {
                System.out.print("\r" + numberOfFile_runningChar[x % numberOfFile_runningChar.length]);
            }
            if (file.isDirectory()) {
                x += numberOfFile(file, isSlient);
            } else {
                x++;
            }
        }
        System.out.print("\r \r");
        return x;
    }

    public static int getNumberOfFileAndDirectory(String file) {
        return getNumberOfFileAndDirectory(new File(file));
    }

    public static int getNumberOfFileAndDirectory(File file) {
        if (file.isFile()) {
            return -1;
        } else {
            int num = file.listFiles().length;
            for (int x = 0; x < file.listFiles().length; x++) {
                if (file.listFiles()[x].isDirectory()) {
                    num += getNumberOfFileAndDirectory(file.listFiles()[x]);
                }
            }
            return num;
        }
    }

    public static int getNumberOfFile(String file) {
        return getNumberOfFile(new File(file));
    }

    public static int getNumberOfFile(File file) {
        if (file.isFile()) {
            return -1;
        } else {
            int num = 0;
            for (int x = 0; x < file.listFiles().length; x++) {
                if (file.listFiles()[x].isDirectory()) {
                    num += getNumberOfFile(file.listFiles()[x]);
                } else if (file.listFiles()[x].isFile()) {
                    num++;
                }
            }
            return num;
        }
    }

    public static int getNumberOfDirectory(String file) {
        return getNumberOfDirectory(new File(file));
    }

    public static int getNumberOfDirectory(File file) {
        if (file.isFile()) {
            return -1;
        } else {
            int num = 0;
            for (int x = 0; x < file.listFiles().length; x++) {
                if (file.listFiles()[x].isDirectory()) {
                    num += getNumberOfDirectory(file.listFiles()[x]);
                    num++;
                }
            }
            return num;
        }
    }

    public static byte[] readFile(File file) {
        return readFile(file.getAbsolutePath(), 0, (int) file.length());
    }

    public static byte readFileByte(File file, long offset) {
        return readFileByte(file.getAbsolutePath(), offset);
    }

    public static byte readFileByte(String filepath, long offset) {
        try {
            RandomAccessFile br = new RandomAccessFile(filepath, "r");
            br.seek(offset);
            byte b = br.readByte();
            br.close();
            return b;
        } catch (Exception ex) {
            ex.printStackTrace();
            return 0;
        }
    }

    public static boolean deleteDirectory(String path) {
        if (new File(path).exists()) {
            File[] files = new File(path).listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i].getPath());
                } else {
                    files[i].delete();
                }
            }
        }
        return (new File(path).delete());
    }

    public static boolean deleteDirectory(File path) {
        if (path.exists()) {
            File[] files = path.listFiles();
            for (int i = 0; i < files.length; i++) {
                if (files[i].isDirectory()) {
                    deleteDirectory(files[i].getPath());
                } else {
                    files[i].delete();
                }
            }
        }
        return path.delete();
    }

    public static byte[] readFile(File file, long offset, int size) {
        return readFile(file.getAbsolutePath(), offset, size);
    }

    public static byte[] readFile(String filepath, long offset, int size) {
        try {
            RandomAccessFile br = new RandomAccessFile(filepath, "r");
            byte data[] = new byte[size];
            br.seek(offset);
            br.read(data);
            br.close();
            return data;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static int[] readFileUnsigned(File file, long offset, int size) {
        return readFileUnsigned(file.getAbsolutePath(), offset, size);
    }

    public static int[] readFileUnsigned(String filepath, long offset, int size) {
        try {
            RandomAccessFile br = new RandomAccessFile(filepath, "r");
            int data[] = new int[size];
            br.seek(offset);
            for (int x = 0; x < size; x++) {
                data[x] = br.readUnsignedByte();
            }
            br.close();
            return data;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static long readLongFromInputStream(DataInputStream in) throws IOException {
        long l = in.readUnsignedByte() + (in.readUnsignedByte() << 8) + (in.readUnsignedByte() << 16) + (in.readUnsignedByte() << 24);
        l &= 0xffffffffL;
        return l;
    }

    public static long readShortFromInputStream(DataInputStream in) throws IOException {
        long l = in.readUnsignedByte() + (in.readUnsignedByte() << 8);
        l &= 0xffffffffL;
        return l;
    }

    public static Object[] reverseArray(Object objects[]) {
        try {
            List l = Arrays.asList(objects);
            java.util.Collections.reverse(l);
            objects = l.toArray();
            return objects;
        } catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public static String convertFilesize(long filesize) {
        if (filesize < 1024) {
            return filesize + " bytes";
        } else if (filesize >= 1024 && filesize < 1024 * 1024) {
            return filesize / 1024 + " KB";
        } else {
            return filesize / 1024 / 1024 + " MB";
        }
    }

    public static void printSecond(long second) {
        if (second > 60 * 60 * 24 * 365) {
            System.out.println(second / (60 * 60 * 24 * 365) + " years, " + second + " seconds");
        } else if (second > 60 * 60 * 24) {
            System.out.println(second / (60 * 60 * 24) + " days, " + second + " seconds");
        } else if (second > 60 * 60) {
            System.out.println(second / (60 * 60) + " hours, " + second + " seconds");
        } else if (second > 60) {
            System.out.println(second / (60) + " minutes, " + second + " seconds");
        } else {
            System.out.println(second + " seconds");
        }
    }

    public static String getRelativePath(File file) {
        return getRelativePath(file.getAbsolutePath());
    }

    public static String getRelativePath(String file) {
        return file.substring(System.getProperty("user.dir").length());
    }

    public static void highlightUsingRegularExpression(JTextComponent textComp, String regularExpression, Color color) {
        removeHighlights(textComp);
        Highlighter.HighlightPainter myHighlightPainter = new MyHighlightPainter(color);
        try {
            Highlighter hilite = textComp.getHighlighter();
            Document doc = textComp.getDocument();
            String text = doc.getText(0, doc.getLength());
            int pos = 0;
            String inputStr = textComp.getText();
            Pattern pattern = Pattern.compile(regularExpression);
            Matcher matcher = pattern.matcher(inputStr);
            boolean matchFound = matcher.find();
            while (matchFound) {
                for (int i = 0; i <= matcher.groupCount(); i++) {
                    String groupStr = matcher.group(i);
                }
                hilite.addHighlight(matcher.start(), matcher.end(), myHighlightPainter);
                if (matcher.end() + 1 <= inputStr.length()) {
                    matchFound = matcher.find(matcher.end());
                } else {
                    break;
                }
            }
        } catch (BadLocationException e) {
        }
    }

    public static void highlight(JTextComponent textComp, String pattern, Color color) {
        removeHighlights(textComp);
        Highlighter.HighlightPainter myHighlightPainter = new MyHighlightPainter(color);
        try {
            Highlighter hilite = textComp.getHighlighter();
            Document doc = textComp.getDocument();
            String text = doc.getText(0, doc.getLength());
            int pos = 0;
            while ((pos = text.indexOf(pattern, pos)) >= 0) {
                hilite.addHighlight(pos, pos + pattern.length(), myHighlightPainter);
                pos += pattern.length();
            }
        } catch (BadLocationException e) {
        }
    }

    public static void removeHighlights(JTextComponent textComp) {
        Highlighter hilite = textComp.getHighlighter();
        Highlighter.Highlight[] hilites = hilite.getHighlights();
        for (int i = 0; i < hilites.length; i++) {
            if (hilites[i].getPainter() instanceof MyHighlightPainter) {
                hilite.removeHighlight(hilites[i]);
            }
        }
    }

    public static void printFilesize(long fileOffset, long filesize) {
        System.out.print("\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r\r");
        if (fileOffset < 1024) {
            System.out.print(fileOffset + " /" + filesize + " bytes, " + fileOffset * 100 / filesize + "%            ");
        } else if (fileOffset >= 1024 & fileOffset < 1024 * 1024) {
            System.out.print(fileOffset / 1024 + " /" + filesize / 1024 + " KB, " + fileOffset * 100 / filesize + "%          ");
        } else if (fileOffset >= 1024 * 1024) {
            System.out.print(fileOffset / 1024 / 1024 + "MB (" + fileOffset / 1024 + " KB) / " + filesize / 1024 / 1024 + " MB, " + fileOffset * 100 / filesize + "%         ");
        }
    }

    public static String trimByteZero(String str) {
        if (str.indexOf(0) == -1) {
            return str;
        } else {
            return str.substring(0, str.indexOf(0));
        }
    }

    static class MyHighlightPainter extends DefaultHighlighter.DefaultHighlightPainter {

        public MyHighlightPainter(Color color) {
            super(color);
        }
    }

    public static void writeFile(String filepath, String content) {
        try {
            FileWriter fstream = new FileWriter(filepath);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(content);
            out.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void appendFile(String filepath, String content) {
        try {
            FileOutputStream file = new FileOutputStream(filepath, true);
            DataOutputStream out = new DataOutputStream(file);
            out.writeBytes(content);
            out.flush();
            out.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static String empty(Object str) {
        if (str == null) {
            return "";
        } else {
            return str.toString();
        }
    }

    public static String runCommand(String command) {
        return runCommand(command, 0);
    }

    public static String runCommand(String command, int skipLine) {
        StringBuffer sb = new StringBuffer(4096);
        try {
            int x = 0;
            String s;
            Process p = Runtime.getRuntime().exec(command);
            BufferedReader stdInput = new BufferedReader(new InputStreamReader(p.getInputStream()));
            while ((s = stdInput.readLine()) != null) {
                if (x >= skipLine) {
                    sb.append(s);
                    sb.append(System.getProperty("line.separator"));
                }
                x++;
            }
            stdInput.close();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }
        return sb.toString();
    }

    public static void captureComponentToJpeg(Component c, File destFile) throws IOException {
        BufferedImage image = new BufferedImage(c.getWidth(), c.getHeight(), BufferedImage.TYPE_INT_RGB);
        c.paint(image.createGraphics());
        ImageIO.write(image, "JPEG", destFile);
    }

    public static String getWebpage(String url) {
        String content = "";
        if (!url.trim().toLowerCase().startsWith("http://")) {
            url = "http://" + url;
        }
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(new URL(url).openStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                content += line + "\n";
            }
            reader.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e2) {
            e2.printStackTrace();
        }
        return content;
    }

    public static Enumeration makeEnumeration(final Object obj) {
        Class type = obj.getClass();
        if (!type.isArray()) {
            throw new IllegalArgumentException(obj.getClass().toString());
        } else {
            return (new Enumeration() {

                int size = Array.getLength(obj);

                int cursor;

                public boolean hasMoreElements() {
                    return (cursor < size);
                }

                public Object nextElement() {
                    return Array.get(obj, cursor++);
                }
            });
        }
    }

    public static void expandAll(JTree tree, boolean expand) {
        TreeNode root = (TreeNode) tree.getModel().getRoot();
        if (root != null) {
            expandAll(tree, new TreePath(root), expand);
        }
    }

    private static void expandAll(JTree tree, TreePath parent, boolean expand) {
        TreeNode node = (TreeNode) parent.getLastPathComponent();
        if (node.getChildCount() >= 0) {
            for (Enumeration e = node.children(); e.hasMoreElements(); ) {
                TreeNode n = (TreeNode) e.nextElement();
                TreePath path = parent.pathByAddingChild(n);
                expandAll(tree, path, expand);
            }
        }
        if (expand) {
            tree.expandPath(parent);
        } else {
            tree.collapsePath(parent);
        }
    }

    public static TreePath findTreeNode(TreeNode node, String pattern, TreePath treePath) {
        Enumeration e = node.children();
        while (e.hasMoreElements()) {
            TreeNode obj = (TreeNode) e.nextElement();
            if (obj.toString().equals(pattern)) {
                return treePath.pathByAddingChild(obj);
            }
            if (!obj.isLeaf()) {
                return findTreeNode(obj, pattern, treePath.pathByAddingChild(obj));
            }
        }
        return null;
    }

    public static void saveFile(String str, File file) {
        try {
            FileWriter fstream = new FileWriter(file);
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(str);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void writeFile(InputStream is, File file) {
        BufferedOutputStream fOut = null;
        try {
            fOut = new BufferedOutputStream(new FileOutputStream(file));
            byte[] buffer = new byte[32 * 1024];
            int bytesRead = 0;
            while ((bytesRead = is.read(buffer)) != -1) {
                fOut.write(buffer, 0, bytesRead);
            }
            fOut.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static int[] hexStringToByteArray(String s) {
        if (s.length() % 2 == 1) {
            s = "0" + s;
        }
        int len = s.length();
        int[] data = new int[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static int[] integerStringToByteArray(String s) {
        if (s.length() % 2 == 1) {
            s = "0" + s;
        }
        int len = s.length();
        int[] data = new int[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 10) * 10) + Character.digit(s.charAt(i + 1), 10));
        }
        return data;
    }

    public static int[] stringToByteArray(String s) {
        int len = s.length();
        int[] data = new int[len];
        for (int i = 0; i < len; i++) {
            data[i] = (byte) s.charAt(i);
        }
        return data;
    }

    public static void putShort(int b[], short s, int index) {
        b[index] = (byte) (s >> 0);
        b[index + 1] = (byte) (s >> 8);
    }

    public static long getShort(int b0, int b1) {
        return (long) ((b0 & 0xff << 0) | (b1 & 0xff) << 8);
    }

    public static long getShort(int[] bb, int index) {
        return (long) (((bb[index + 0] & 0xff) << 0) | ((bb[index + 1] & 0xff) << 8));
    }

    public static void putInt(int[] bb, int x, int index) {
        bb[index + 0] = (byte) (x >> 0);
        bb[index + 1] = (byte) (x >> 8);
        bb[index + 2] = (byte) (x >> 16);
        bb[index + 3] = (byte) (x >> 24);
    }

    public static long getInt(byte[] bb, int index) {
        return (long) ((((bb[index + 0] & 0xff) << 0) | ((bb[index + 1] & 0xff) << 8) | ((bb[index + 2] & 0xff) << 16) | ((bb[index + 3] & 0xff) << 24)));
    }

    public static long getInt(byte b0, byte b1, byte b2, byte b3) {
        return (long) ((((b0 & 0xff) << 0) | ((b1 & 0xff) << 8) | ((b2 & 0xff) << 16) | ((b3 & 0xff) << 24)));
    }

    public static long getInt(int[] bb, int index) {
        return (long) ((((bb[index + 0] & 0xff) << 0) | ((bb[index + 1] & 0xff) << 8) | ((bb[index + 2] & 0xff) << 16) | ((bb[index + 3] & 0xff) << 24)));
    }

    public static long getInt(int b0, int b1, int b2, int b3) {
        return (long) ((((b0 & 0xff) << 0) | ((b1 & 0xff) << 8) | ((b2 & 0xff) << 16) | ((b3 & 0xff) << 24)));
    }

    public static void putLong(int[] bb, long x, int index) {
        bb[index + 0] = (byte) (x >> 0);
        bb[index + 1] = (byte) (x >> 8);
        bb[index + 2] = (byte) (x >> 16);
        bb[index + 3] = (byte) (x >> 24);
        bb[index + 4] = (byte) (x >> 32);
        bb[index + 5] = (byte) (x >> 40);
        bb[index + 6] = (byte) (x >> 48);
        bb[index + 7] = (byte) (x >> 56);
    }

    public static long getLong(int b0, byte b1) {
        return (long) (b0 & 0xff) | (long) (b1 & 0xff) << 8;
    }

    public static long getLong(int[] bb, int index) {
        return ((((long) bb[index + 0] & 0xff) << 0) | (((long) bb[index + 1] & 0xff) << 8) | (((long) bb[index + 2] & 0xff) << 16) | (((long) bb[index + 3] & 0xff) << 24) | (((long) bb[index + 4] & 0xff) << 32) | (((long) bb[index + 5] & 0xff) << 40) | (((long) bb[index + 6] & 0xff) << 48) | (((long) bb[index + 7] & 0xff) << 56));
    }

    public static long getLong(int b0, int b1, int b2, int b3, int b4, int b5, int b6, int b7) {
        return ((((long) b0 & 0xff) << 0) | (((long) b1 & 0xff) << 8) | (((long) b2 & 0xff) << 16) | (((long) b3 & 0xff) << 24) | (((long) b4 & 0xff) << 32) | (((long) b5 & 0xff) << 40) | (((long) b6 & 0xff) << 48) | (((long) b7 & 0xff) << 56));
    }

    public static long getBit(long value, int bitNo) {
        return value >> bitNo & 1;
    }

    public static long getValue(long l, int startBit, int endBit) {
        if (startBit > endBit) {
            int temp = startBit;
            startBit = endBit;
            endBit = temp;
        }
        l = l >> startBit;
        return l & new Double(Math.pow(2, (endBit - startBit + 1)) - 1).longValue();
    }

    public static int[] byteArrayToIntArray(byte[] b) {
        int i[] = new int[b.length];
        for (int x = 0; x < i.length; x++) {
            i[x] = b[x];
        }
        return i;
    }

    public static byte[] intArrayToByteArray(int[] b) {
        byte i[] = new byte[b.length];
        for (int x = 0; x < i.length; x++) {
            i[x] = (byte) b[x];
        }
        return i;
    }

    public static int[] getBytes(long value) {
        int b[] = new int[8];
        for (int x = 0; x < 8; x++) {
            b[x] = (int) ((value >> (x * 8)) & 0xff);
        }
        return b;
    }
}
