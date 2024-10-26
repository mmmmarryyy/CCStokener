package edutex.swt;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Locale;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import edutex.Builder;
import edutex.Console;
import edutex.LatexToPdfBuilder;
import edutex.LatexToPsBuilder;
import edutex.NLSMessages;
import edutex.config.EdutexConfig;
import edutex.util.EdutexProperties;

/**
 * Gestion de l'interface graphique
 * 
 * @author Cali
 * 
 */
public class Mediator {

    public static final String EDITOR = "Mediator.Editor";

    public static final String BUILDERS = "Mediator.Builders";

    public static final String TREES = "Mediator.Trees";

    public static final Color SELECTED_TAB_COLOR = new org.eclipse.swt.graphics.Color(Display.getDefault(), 200, 200, 255);

    private static String BASE_URL = "http://www.jfigure.fr/";

    private static String REMOTE_VERSION_FILE = "edutex.version";

    public static final String VERSION = "0.9.1 beta 1";

    /**
	 * Retourne l'�diteur
	 * 
	 * @param shell
	 * @return
	 */
    public static final EdutexEditor editor(Composite comp) {
        return (EdutexEditor) comp.getShell().getData(EDITOR);
    }

    /**
	 * Enregistrement de l'�diteur
	 * 
	 * @param shell
	 */
    public static final void registerEditor(EdutexEditor editor) {
        editor.getShell().setData(EDITOR, editor);
    }

    /**
	 * Retourne les arborescence
	 * 
	 * @param shell
	 * @return
	 */
    public static final EdutexTabTrees trees(Composite comp) {
        return (EdutexTabTrees) comp.getShell().getData(TREES);
    }

    /**
	 * Enregistrement des arboresence
	 * 
	 * @param shell
	 */
    public static final void registerTrees(EdutexTabTrees trees) {
        trees.getShell().setData(TREES, trees);
    }

    /**
	 * Enregistrement des builders
	 * 
	 * @param shell
	 * @param config
	 * @param console
	 */
    public static final void registerBuilders(Shell shell, EdutexConfig config, Console console) {
        Builder[] builders = new Builder[2];
        builders[0] = new LatexToPdfBuilder(console, config);
        builders[1] = new LatexToPsBuilder(console, config);
        shell.setData(BUILDERS, builders);
    }

    /**
	 * R�cuperation des builders
	 */
    public static final Builder[] getBuilders(Shell shell) {
        return (Builder[]) shell.getData(BUILDERS);
    }

    public static void locateToMiddle(Composite parent, Composite comp) {
        Rectangle parentLoc = parent.getBounds();
        comp.setLocation(parentLoc.x + (int) ((parentLoc.width - comp.getSize().x) / 2.0), parentLoc.y + (int) ((parentLoc.height - comp.getSize().y) / 2.0));
    }

    public static final void showImputStringDialog(Shell shell, String title, String inputString, final StringBuffer output) {
        final Shell dialog = new Shell(shell, SWT.DIALOG_TRIM | SWT.APPLICATION_MODAL);
        dialog.setText(title);
        FormLayout formLayout = new FormLayout();
        formLayout.marginWidth = 10;
        formLayout.marginHeight = 10;
        formLayout.spacing = 10;
        dialog.setLayout(formLayout);
        Label label = new Label(dialog, SWT.NONE);
        label.setText(inputString);
        FormData data = new FormData();
        label.setLayoutData(data);
        Button cancel = new Button(dialog, SWT.PUSH);
        cancel.setText("Cancel");
        data = new FormData();
        data.width = 60;
        data.right = new FormAttachment(100, 0);
        data.bottom = new FormAttachment(100, 0);
        cancel.setLayoutData(data);
        cancel.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                dialog.close();
                output.setLength(0);
            }
        });
        final Text text = new Text(dialog, SWT.BORDER);
        data = new FormData();
        data.width = 200;
        data.left = new FormAttachment(label, 0, SWT.DEFAULT);
        data.right = new FormAttachment(100, 0);
        data.top = new FormAttachment(label, 0, SWT.CENTER);
        data.bottom = new FormAttachment(cancel, 0, SWT.DEFAULT);
        text.setLayoutData(data);
        Button ok = new Button(dialog, SWT.PUSH);
        ok.setText("OK");
        data = new FormData();
        data.width = 60;
        data.right = new FormAttachment(cancel, 0, SWT.DEFAULT);
        data.bottom = new FormAttachment(100, 0);
        ok.setLayoutData(data);
        ok.addSelectionListener(new SelectionAdapter() {

            @Override
            public void widgetSelected(SelectionEvent e) {
                output.append(text.getText());
                dialog.close();
            }
        });
        dialog.setDefaultButton(ok);
        dialog.pack();
        locateToMiddle(shell, dialog);
        dialog.open();
    }

    public static final void init(Shell shell) {
        EdutexProperties properties = new EdutexProperties("edutex");
        String lang = "fr";
        if (properties.getProperty("lang") != null) lang = properties.getProperty("lang");
        initLang(lang, shell, false);
    }

    public static final void initLang(String lang, Shell shell, boolean redraw) {
        for (Locale l : NLSMessages.supportedLocales) {
            if (lang.equals(l.getLanguage())) {
                NLSMessages.init(NLSMessages.supportedLocales.indexOf(l));
                EdutexProperties properties = new EdutexProperties("edutex");
                properties.setProperty("lang", l.getLanguage(), true);
                break;
            }
        }
    }

    /**
	 * about
	 */
    public static final void about(Shell shell) {
        MessageBox box = new MessageBox(shell);
        box.setMessage(NLSMessages.getString("Mediator.About", VERSION));
        box.open();
    }

    /**
     * V�rification d'une version
     */
    public static final void checkVersion(Shell shell) {
        String localVersion = VERSION;
        String remoteVersion = "";
        try {
            URL url = new URL(BASE_URL + REMOTE_VERSION_FILE);
            BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
            String line = in.readLine();
            remoteVersion = new String(line.trim());
            in.close();
        } catch (Exception err) {
            err.printStackTrace();
            return;
        }
        if (!localVersion.equals(remoteVersion)) {
            MessageBox box = new MessageBox(shell);
            box.setMessage(NLSMessages.getString("Mediator.NewVersion", localVersion, remoteVersion));
            box.open();
            return;
        }
    }
}
