package edu.ksu.cis.bnj.gui.dialogs;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.DisposeEvent;

public class FeedbackDlg {

    private Text text;

    protected Shell shell;

    private Color backGround;

    private Color foreGround;

    public void show(Shell parent) {
        backGround = new Color(parent.getDisplay(), 255, 255, 255);
        foreGround = new Color(parent.getDisplay(), 0, 0, 128);
        shell = new Shell(parent, SWT.PRIMARY_MODAL | SWT.BORDER | SWT.CLOSE | SWT.DIALOG_TRIM | SWT.PRIMARY_MODAL | SWT.TITLE);
        shell.addDisposeListener(new DisposeListener() {

            public void widgetDisposed(DisposeEvent e) {
                backGround.dispose();
                foreGround.dispose();
            }
        });
        int w = 440;
        int h = 320;
        shell.setBounds(parent.getBounds().x + parent.getBounds().width / 2 - w / 2, parent.getBounds().y + parent.getBounds().height / 2 - h / 2, w, h);
        createContents();
        shell.open();
    }

    public static String getURL(String txt) {
        return "http://projeny.sourceforge.net/feedback.php?feedback=" + URLEncoder.encode(txt);
    }

    public static void Send(String txt) {
        try {
            URL url = new URL(getURL(txt));
            InputStream in = url.openStream();
            BufferedInputStream bufIn = new BufferedInputStream(in);
            for (; ; ) {
                int data = bufIn.read();
                if (data == -1) break;
            }
        } catch (Exception e) {
        }
    }

    public void Send() {
        FeedbackDlg.Send(text.getText());
    }

    protected void createContents() {
        shell.setText("Feedback for Projeny 0.1 alpha");
        {
            final Label label = new Label(shell, SWT.NONE);
            label.setBounds(15, 15, 245, 15);
            label.setText("Projeny - Probabilistic Networks Generator in Java");
        }
        {
            text = new Text(shell, SWT.BORDER | SWT.V_SCROLL);
            text.setBounds(6, 40, 415, 205);
            text.setText("To leave feedback, please visit our tracker at \n " + "http://sourceforge.net/tracker/?group_id=207404 \n" + "or our mailing lists at \n" + "http://sourceforge.net/mail/?group_id=207404");
        }
    }
}
