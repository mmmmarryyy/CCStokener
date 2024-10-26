package com.tensegrity.palobrowser.dialogs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import com.tensegrity.palobrowser.PalobrowserPlugin;

/**
 * <code>LicenseDialog</code>, fallback license dialog.
 * 
 * @author Stepan Rutz
 * @version $ID$
 */
public class LicenseDialog extends TitleAreaDialog {

    public LicenseDialog(Shell shell) {
        super(shell);
        setShellStyle(getShellStyle() | SWT.RESIZE);
    }

    protected void configureShell(Shell shell) {
        super.configureShell(shell);
        shell.setText(DialogsMessages.getString("LicenseDialog.Title"));
    }

    protected Point getInitialSize() {
        Point p = super.getInitialSize();
        p.x = 600;
        return new Point(p.x, p.x * 3 / 4);
    }

    protected Control createDialogArea(Composite parent) {
        Composite composite = (Composite) super.createDialogArea(parent);
        setTitle(DialogsMessages.getString("LicenseDialog.Caption"));
        setMessage(DialogsMessages.getString("LicenseDialog.Explanation"));
        Composite content = new Composite(composite, SWT.NONE);
        content.setLayoutData(new GridData(GridData.FILL_BOTH));
        final int ncol = 1;
        GridLayout layout = new GridLayout(1, false);
        layout.numColumns = ncol;
        content.setLayout(layout);
        Browser browser = null;
        Text text = null;
        try {
            browser = new Browser(content, SWT.NONE);
            browser.setLayoutData(new GridData(GridData.FILL_BOTH));
        } catch (Throwable t) {
            text = new Text(content, SWT.MULTI | SWT.WRAP | SWT.VERTICAL);
            text.setLayoutData(new GridData(GridData.FILL_BOTH));
        }
        URL url = PalobrowserPlugin.getDefault().getBundle().getResource(browser != null ? "license.html" : "license.txt");
        InputStream in = null;
        BufferedReader r = null;
        StringBuffer sb = new StringBuffer();
        try {
            in = url.openStream();
            r = new BufferedReader(new InputStreamReader(in, "ISO-8859-1"));
            String line;
            while ((line = r.readLine()) != null) sb.append(line).append("\r\n");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (r != null) {
                try {
                    r.close();
                } catch (IOException e) {
                }
            }
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
        if (browser != null) browser.setText(sb.toString()); else text.setText(sb.toString());
        return composite;
    }

    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.CLOSE_LABEL, true);
    }

    protected void okPressed() {
        super.okPressed();
    }
}
