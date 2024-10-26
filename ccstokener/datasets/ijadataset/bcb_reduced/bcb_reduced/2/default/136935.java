import javax.swing.JOptionPane;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.io.PrintStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.BorderLayout;
import javax.swing.JFrame;
import javax.swing.JLabel;

public class Launcher {

    public static void copy(URL url, String outPath) throws IOException {
        System.out.println("copying from: " + url + " to " + outPath);
        InputStream in = url.openStream();
        FileOutputStream fout = new FileOutputStream(outPath);
        byte[] data = new byte[8192];
        int read = -1;
        while ((read = in.read(data)) != -1) {
            fout.write(data, 0, read);
        }
        fout.close();
    }

    public static void main(final String[] args) throws Exception {
        EventQueue.invokeLater(new Runnable() {

            public void run() {
                final LoadFrame f = new LoadFrame();
                f.setLocationRelativeTo(null);
                f.setVisible(true);
                Runnable r = new Runnable() {

                    public void run() {
                        for (String item : args) {
                            System.out.println("loading: " + args[0]);
                            f.setText("loading: " + item);
                            try {
                                copy(item);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                        try {
                            String runPath = "\"" + userHome + fileSeparator + jar + "\"";
                            String line = javaHome + fileSeparator + "bin" + fileSeparator + "java -jar " + runPath;
                            System.out.println("executing: " + line);
                            Runtime.getRuntime().exec(line);
                            f.dispose();
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                };
                new Thread(r).start();
            }
        });
    }

    private static void copy(String item) throws Exception {
        String userHome = System.getProperty("user.home");
        String fileSeparator = System.getProperty("file.separator");
        String file = item.substring(item.lastIndexOf("/") + 1);
        String location = userHome + fileSeparator + file;
        copy(new URL(item), location);
    }

    static class LoadFrame extends JFrame {

        private JLabel lblInfo = new JLabel("Hello World");

        public LoadFrame() {
            super("Downloading...");
            setLayout(new BorderLayout());
            setPreferredSize(new Dimension(400, 80));
            setUndecorated(true);
            add(lblInfo, BorderLayout.CENTER);
            pack();
            addWindowListener(new WindowAdapter() {

                public void windowClosing(WindowEvent e) {
                    System.exit(0);
                }
            });
        }

        public void setText(String text) {
            lblInfo.setText(text);
        }
    }
}
