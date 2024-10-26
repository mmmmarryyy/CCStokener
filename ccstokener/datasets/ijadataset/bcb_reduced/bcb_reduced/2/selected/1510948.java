package espider.gui.plugin;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import espider.libs.helliker.id3.MP3File;
import espider.player.MyPlayerControl;

/**
 * @author christophe
 *
 */
public class LyricsPlugin extends Plugin {

    private Browser browser;

    private String prevArtist = "";

    private String prevTitle = "";

    public LyricsPlugin(CTabFolder tabFolder) {
        super(tabFolder);
    }

    /**
	 * @see espider.gui.plugin.Plugin#getTabName()
	 */
    public String getTabName() {
        return "Lyrics";
    }

    /**
	 * @see espider.gui.plugin.Plugin#createContents()
	 */
    public void createContents() {
        this.setLayout(new FillLayout());
        browser = new Browser(this, SWT.NONE);
    }

    public void updateContents(Display display) {
        try {
            MP3File file = new MP3File(MyPlayerControl.getInstance().getFilename());
            final String artist = URLEncoder.encode(file.getArtist(), "UTF-8");
            final String song = URLEncoder.encode(file.getTitle(), "UTF-8");
            if (!artist.equals(prevArtist) && !song.equals(prevTitle)) {
                prevArtist = artist;
                prevTitle = song;
                display.asyncExec(new Runnable() {

                    public void run() {
                        try {
                            URL url = new URL("http://www.lyrc.com.ar/tema1en.php?songname=" + song + "&artist=" + artist);
                            URLConnection urlConnection = url.openConnection();
                            byte[] bytes = new byte[1024];
                            StringBuilder html = new StringBuilder();
                            while (urlConnection.getInputStream().read(bytes) > -1) {
                                html.append(new String(bytes));
                                bytes = new byte[1024];
                            }
                            int indexScript = html.indexOf("<script");
                            while (indexScript > -1) {
                                html = html.delete(indexScript, html.indexOf("</script>", indexScript) + 9);
                                indexScript = html.indexOf("<script");
                            }
                            String css = new File("./ressources/css/lyrics.css").getAbsolutePath();
                            String newHTML = "<html>" + "<head>" + "<link rel=\"stylesheet\" href=\"" + css + "\" style=\"css/text\">" + "</head>" + html.substring(html.indexOf("<body"), html.indexOf("</body>") + 7);
                            browser.setText(newHTML);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        } catch (NullPointerException npe) {
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean useSWTBrowser() {
        return true;
    }
}
