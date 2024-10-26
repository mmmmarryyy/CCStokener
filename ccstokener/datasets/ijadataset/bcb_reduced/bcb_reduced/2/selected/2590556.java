package jpatch;

import java.awt.*;
import java.awt.datatransfer.*;
import javax.swing.*;
import java.awt.event.*;
import java.util.*;
import java.net.*;
import java.io.*;

public class GlTest {

    public static final int OTHER = 0;

    public static final int WINDOWS_X86 = 1;

    public static final int LINUX_X86 = 2;

    public static final int OSX_PPC = 3;

    public static final int SOLARIS_SPARC = 4;

    public static final int SOLARIS_X86 = 5;

    public static final int OSX_X86 = 6;

    public static final String[] OS_ARCH = new String[] { "OTHER", "Windows/x86", "Linux/x86", "OS X/Power PC", "Solaris/Sparc", "Solaris/x86", "OS X/x86" };

    public static Logger logger;

    public static void main(String[] args) {
        new GlTest();
    }

    public GlTest() {
        try {
            logger = new Logger();
            System.setErr(logger.getPrintStream());
            logger.setVisible(true);
            logger.log("This program will test the Java OpenGL (JOGL) support on your platform. ");
            logger.log("If any error messages appear in this window, please report them ");
            logger.log("(post a message to the JPatch forum).\n");
            logger.log("Thanks a lot!\n");
            logger.log("\n");
            logger.log("Ok, let's try to find out on which platform we're running on...\n");
            Properties properties = System.getProperties();
            String osName = properties.getProperty("os.name");
            String osVersion = properties.getProperty("os.version");
            String osArch = properties.getProperty("os.arch");
            logger.log("\tos.name:\t" + osName + "\n");
            logger.log("\tos.version:\t" + osVersion + "\n");
            logger.log("\tos.arch:\t" + osArch + "\n");
            logger.log("\tjava.version:\t" + properties.getProperty("java.version") + "\n");
            logger.log("\tjava.vendor:\t" + properties.getProperty("java.vendor") + "\n");
            logger.log("\tjava.vm.version:" + properties.getProperty("java.vm.version") + "\n");
            logger.log("\tjava.vm.vendor:\t" + properties.getProperty("java.vm.vendor") + "\n");
            logger.log("\tjava.vm.name:\t" + properties.getProperty("java.vm.name") + "\n");
            logger.log("\n");
            int platform = getArch();
            if (platform == OTHER) {
                logger.log("WE HAVE A PROBLEM:\n");
                logger.log("Either JPatch was unable to detect your platform or there is no JOGL ");
                logger.log("support available for it.\n");
                logger.log("Recognized platforms are:\n");
                for (int i = 1; i < OS_ARCH.length; logger.log("\t" + OS_ARCH[i++] + "\n")) ;
                logger.log("If your platform is among the supported ones, please report it.\n");
            } else {
                logger.log("Platform recognized as:\n\t" + OS_ARCH[platform] + "\n");
                boolean loaded = false;
                while (!loaded) {
                    logger.log("\n");
                    logger.log("Searching for jogl shared libraries...");
                    String javaLibraryPath = properties.getProperty("java.library.path");
                    String[] folders = javaLibraryPath.split(properties.getProperty("path.separator"));
                    String lib = System.mapLibraryName("jogl");
                    for (int i = 0; i < folders.length; i++) {
                        File file = new File(folders[i], lib);
                        if (file.exists()) {
                            loaded = true;
                            logger.log("found in " + folders[i] + "\n");
                        }
                    }
                    if (!loaded) {
                        logger.log("not found.\n");
                        logger.log("\n");
                        if (platform != WINDOWS_X86 && platform != LINUX_X86 && platform != OSX_PPC) {
                            logger.log("NOT SUPPORTED:\n");
                            logger.log("JOGL binaries are included for Windows, Linux(x86) and OSX (PowerPC) only.");
                        } else {
                            logger.log("We'd need to install the libraries into a folder contained in java.library.path. ");
                            logger.log("If you click CONTINUE, a dialog will ask you to choose a location. ");
                            logger.log("If you'd like to use a different location, click QUIT ");
                            logger.log("and restart this application with the -Djava.library.path=<path> option.\n");
                            logger.log("\n");
                            boolean installed = false;
                            while (!installed) {
                                logger.userWait();
                                String folder = chooseLocation(folders);
                                if (folder == null) {
                                    logger.log("Shared library installation canceled.");
                                } else {
                                    try {
                                        extractJoglLibraries(platform, folder);
                                        installed = true;
                                    } catch (IOException e) {
                                        logger.log("failed.\n");
                                        logger.log(e.getMessage() + "\n");
                                        logger.log("You may need administrator or root privileges.\n");
                                        logger.log("Click CONTINUE to choose another folder.\n");
                                        logger.log("\n");
                                    }
                                }
                            }
                        }
                    }
                }
                logger.log("\n");
                logger.log("Click CONTINUE to start the OpenGL test application.\n");
                logger.log("Please try switching between single and multiple viewport modes, toggling OpenGL ");
                logger.log("rendering and resizing or minimizing/maximizing the display window several times.\n");
                logger.log("After that, check this window again for error messages and, if applicable, ");
                logger.log("post them to the JPatch forum. Thank you!\n");
                logger.userWait();
                logger.log("\n");
                logger.log("Staring OpenGL test...\n");
                new jpatch.Viewport2Test("objects/teapot.obj", "objects/teapot.mtl");
            }
        } catch (Throwable t) {
            logger.log(t);
        }
    }

    private static int getArch() {
        Properties properties = System.getProperties();
        String name = properties.getProperty("os.name");
        String arch = properties.getProperty("os.arch");
        if (name.startsWith("Windows")) {
            if (arch.equals("x86") || arch.equals("i386") || arch.equals("i586") || arch.equals("i686")) {
                return WINDOWS_X86;
            }
        } else if (name.equals("Linux")) {
            if (arch.equals("x86") || arch.equals("i386") || arch.equals("i586") || arch.equals("i686")) {
                return LINUX_X86;
            }
        } else if (name.equals("Mac OS X")) {
            if (arch.equals("ppc") || arch.equals("PowerPC")) {
                return OSX_PPC;
            } else {
                return OSX_X86;
            }
        } else if (name.equals("Solaris")) {
            if (arch.equals("sparc")) {
                return SOLARIS_SPARC;
            } else if (arch.equals("x86") || arch.equals("i386") || arch.equals("i586") || arch.equals("i686")) {
                return SOLARIS_X86;
            }
        }
        return OTHER;
    }

    private void extractJoglLibraries(int platform, String dir) throws IOException {
        switch(platform) {
            case WINDOWS_X86:
                {
                    copyFromURL(ClassLoader.getSystemResource("lib/native/windows_x86/jogl.dll"), dir);
                    copyFromURL(ClassLoader.getSystemResource("lib/native/windows_x86/jogl_cg.dll"), dir);
                }
                break;
            case LINUX_X86:
                {
                    copyFromURL(ClassLoader.getSystemResource("lib/jogl/native/linux_x86/libjogl.so"), dir);
                    copyFromURL(ClassLoader.getSystemResource("lib/jogl/native/linux_x86/libjogl_cg.so"), dir);
                }
                break;
            case OSX_PPC:
                {
                    copyFromURL(ClassLoader.getSystemResource("lib/jogl/native/osx_ppc/libjogl.jnilib"), dir);
                    copyFromURL(ClassLoader.getSystemResource("lib/jogl/native/osx_ppc/libjogl_cg.jnilib"), dir);
                }
                break;
            case OSX_X86:
                {
                    copyFromURL(ClassLoader.getSystemResource("src/nativelibs/osx/x86/libjogl.jnilib"), dir);
                    copyFromURL(ClassLoader.getSystemResource("src/nativelibs/osx/x86/libjogl_cg.jnilib"), dir);
                }
                break;
            case SOLARIS_SPARC:
                {
                    copyFromURL(ClassLoader.getSystemResource("lib/jogl/native/solaris_sparc/libjogl.so"), dir);
                }
                break;
            case SOLARIS_X86:
                {
                    copyFromURL(ClassLoader.getSystemResource("lib/jogl/native/solaris_x86/libjogl.so"), dir);
                }
                break;
        }
    }

    private File copyFromURL(URL url, String dir) throws IOException {
        File urlFile = new File(url.getFile());
        File dest = new File(dir, urlFile.getName());
        logger.log("Extracting " + urlFile.getName() + " to " + dir + "...");
        FileOutputStream os = new FileOutputStream(dest);
        InputStream is = url.openStream();
        byte data[] = new byte[4096];
        int ct;
        while ((ct = is.read(data)) >= 0) os.write(data, 0, ct);
        is.close();
        os.close();
        logger.log("ok\n");
        return dest;
    }

    private String chooseLocation(String[] locations) {
        JComboBox comboBox = new JComboBox(locations);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(new JLabel("Choose a folder to copy the jogl shared libraries into:"), BorderLayout.NORTH);
        panel.add(comboBox, BorderLayout.SOUTH);
        if (JOptionPane.showConfirmDialog(logger, panel, "JOGL shared libraries installation", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) return (String) comboBox.getSelectedItem();
        return null;
    }

    public static class Logger extends JFrame {

        private static final long serialVersionUID = 6438038184490216826L;

        JTextArea textArea = new JTextArea(25, 80);

        JPanel panelButtons = new JPanel();

        JButton buttonCopy = new JButton("Copy to clipboard");

        JButton buttonContinue = new JButton("Continue");

        JButton buttonQuit = new JButton("Quit");

        JScrollBar scrollBar;

        boolean wait = false;

        Logger() {
            super("Log window");
            textArea.setFont(new Font("Monospaced", Font.PLAIN, 12));
            addWindowListener(new WindowAdapter() {

                public void windowClosing(WindowEvent e) {
                    dispose();
                    System.exit(0);
                }
            });
            getContentPane().setLayout(new BorderLayout());
            JScrollPane scrollPane = new JScrollPane(textArea, ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
            scrollBar = scrollPane.getVerticalScrollBar();
            getContentPane().add(scrollPane, BorderLayout.CENTER);
            buttonContinue.setEnabled(false);
            panelButtons.add(buttonCopy);
            panelButtons.add(buttonContinue);
            panelButtons.add(buttonQuit);
            buttonQuit.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    dispose();
                    System.exit(0);
                }
            });
            buttonCopy.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Clipboard cb = Toolkit.getDefaultToolkit().getSystemClipboard();
                    cb.setContents(new StringSelection(textArea.getText()), null);
                }
            });
            buttonContinue.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    wait = false;
                }
            });
            getContentPane().add(panelButtons, BorderLayout.SOUTH);
            textArea.setEditable(false);
            textArea.setLineWrap(true);
            textArea.setWrapStyleWord(true);
            pack();
        }

        public void log(String message) {
            System.out.print(message);
            textArea.append(message);
            (new Thread() {

                public void run() {
                    try {
                        Thread.sleep(50);
                        scrollBar.setValue(scrollBar.getMaximum());
                        Thread.sleep(50);
                        textArea.repaint();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        }

        public void log(Throwable t) {
            log(t.toString() + "\n");
            StackTraceElement[] ste = t.getStackTrace();
            for (int i = 0; i < ste.length; log(ste[i++].toString() + "\n")) ;
        }

        public void userWait() {
            wait = true;
            buttonContinue.setEnabled(true);
            while (wait) {
                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            buttonContinue.setEnabled(false);
        }

        public PrintStream getPrintStream() {
            return new PrintStream(new OutputStream() {

                public void close() {
                }

                public void flush() {
                }

                public void write(byte[] b) {
                    log(new String(b));
                }

                public void write(byte[] b, int off, int len) {
                    log(new String(b, off, len));
                }

                public void write(int b) {
                    log(new String(new byte[] { (byte) b }));
                }
            });
        }
    }
}
