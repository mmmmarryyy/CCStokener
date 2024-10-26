package espider.gui.mp3;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import espider.gui.mainWindow.guielements.MiniPlayerSWT;
import espider.libs.file.AudioSpider;
import espider.webservices.amazon.Amazon;
import espider.webservices.amazon.AmazonItem;
import espider.webservices.amazon.Cover;

/**
 * @author vincent
 *
 * 
 * 
 */
public class CoverGUI {

    protected Shell shell;

    private Display display;

    private Table table;

    private String artist = null;

    private String album = null;

    private File path = null;

    private ArrayList<URL> coverAvailable = new ArrayList<URL>();

    public CoverGUI(AudioSpider file) {
        this.artist = file.getArtist();
        this.album = file.getAlbum();
        this.path = new File(file.getPath());
    }

    public void open() {
        display = Display.getDefault();
        createContents();
        shell.layout();
        shell.open();
        while (!shell.isDisposed()) {
            if (!display.readAndDispatch()) display.sleep();
        }
    }

    protected void createContents() {
        shell = new Shell(display, SWT.BORDER | SWT.CLOSE);
        shell.setSize(400, 600);
        shell.setText("Pochettes disponibles");
        table = new Table(shell, SWT.BORDER);
        table.setSize(400, 600);
        TableColumn colonne1 = new TableColumn(table, SWT.LEFT);
        colonne1.setText("");
        colonne1.setWidth(200);
        TableColumn colonne2 = new TableColumn(table, SWT.LEFT);
        colonne2.setText("");
        colonne2.setWidth(200);
        table.setHeaderVisible(false);
        table.setLinesVisible(true);
        table.addMouseListener(new org.eclipse.swt.events.MouseAdapter() {

            public void mouseDoubleClick(MouseEvent e) {
                Cover.getCoverFromUrl(coverAvailable.get(table.indexOf(table.getSelection()[0])), path.getParent() + "//folder.jpg");
                MiniPlayerSWT.getInstance().updateCover();
                shell.dispose();
            }
        });
        ArrayList<AmazonItem> cover = Amazon.searchAmazonItem(artist, album);
        for (int i = 0; i < cover.size(); i++) {
            addLine(cover.get(i));
        }
    }

    private void addLine(AmazonItem coverAdress) {
        try {
            URL url = new URL("" + coverAdress.getMediumImageURL());
            TableItem ligne1 = new TableItem(table, SWT.DRAW_DELIMITER | SWT.DRAW_TAB | SWT.DRAW_MNEMONIC);
            url.openConnection();
            InputStream is = url.openStream();
            Image coverPicture = new Image(display, is);
            coverAvailable.add(url);
            ligne1.setImage(new Image[] { coverPicture, null });
            ligne1.setText(new String[] { null, coverAdress.getArtist() + "\n" + coverAdress.getCDTitle() + "\nTrack : " + coverAdress.getNbTrack() });
        } catch (MalformedURLException e) {
        } catch (IOException e) {
            System.err.println(e.toString());
        }
    }
}
