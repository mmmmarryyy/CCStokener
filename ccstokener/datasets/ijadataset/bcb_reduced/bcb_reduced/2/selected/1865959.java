package espider.gui.plugin;

import java.io.InputStream;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import espider.libs.helliker.id3.MP3File;
import espider.player.MyPlayerControl;
import espider.webservices.amazon.Amazon;
import espider.webservices.amazon.AmazonItem;

public class OthersAlbumsPlugin extends Plugin {

    Composite prevCompo = null, main;

    int color = SWT.COLOR_WHITE;

    ScrolledComposite contentPane;

    public OthersAlbumsPlugin(CTabFolder tabFolder) {
        super(tabFolder);
    }

    @Override
    public String getTabName() {
        return "Others Albums";
    }

    @Override
    public void createContents() {
        this.setLayout(new FormLayout());
        contentPane = new ScrolledComposite(this, SWT.V_SCROLL | SWT.BORDER);
        FormData data = new FormData();
        data.top = new FormAttachment(0, 0);
        data.left = new FormAttachment(0, 0);
        data.right = new FormAttachment(100, 0);
        data.bottom = new FormAttachment(100, 0);
        contentPane.setLayoutData(data);
        main = new Composite(contentPane, SWT.NONE);
        main.setLayout(new FormLayout());
        contentPane.setContent(main);
        try {
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void updateContents(Display display) {
        if (MyPlayerControl.getInstance().getFilename() != null) display.asyncExec(new Runnable() {

            public void run() {
                try {
                    MP3File file = new MP3File(MyPlayerControl.getInstance().getFilename());
                    final String artist = URLEncoder.encode(file.getArtist(), "UTF-8");
                    final String title = URLEncoder.encode(file.getTitle(), "UTF-8");
                    ArrayList<AmazonItem> amazonitem = Amazon.searchAmazonItem(artist, "");
                    if (amazonitem.size() > 0) {
                        System.out.println("AmazonItem : " + amazonitem.size());
                        for (int i = 0; i < amazonitem.size(); i++) {
                            URL url = new URL("" + amazonitem.get(i).getSmallImageURL());
                            album(amazonitem.get(i).getCDTitle(), amazonitem.get(i).getNbTrack(), url);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void album(String albumTitle, String albumNbSong, URL url) {
        try {
            if (color == SWT.COLOR_WHITE) {
                color = SWT.COLOR_GRAY;
            } else {
                color = SWT.COLOR_WHITE;
            }
            url.openConnection();
            InputStream is = url.openStream();
            Image coverPicture = new Image(this.getDisplay(), is);
            Composite albumComposite = new Composite(main, SWT.NONE);
            albumComposite.setLayout(new FormLayout());
            FormData data = new FormData();
            data.left = new FormAttachment(0, 5);
            data.right = new FormAttachment(100, -5);
            if (prevCompo == null) {
                data.top = new FormAttachment(0, 0);
            } else {
                data.top = new FormAttachment(prevCompo, 0, SWT.BOTTOM);
            }
            albumComposite.setLayoutData(data);
            albumComposite.setBackground(Display.getDefault().getSystemColor(color));
            Label cover = new Label(albumComposite, SWT.LEFT);
            cover.setText("cover");
            cover.setImage(coverPicture);
            data = new FormData(75, 75);
            cover.setLayoutData(data);
            Label title = new Label(albumComposite, SWT.CENTER);
            title.setFont(new Font(this.getDisplay(), "Arial", 10, SWT.BOLD));
            title.setText(albumTitle);
            data = new FormData();
            data.bottom = new FormAttachment(50, -5);
            data.left = new FormAttachment(cover, 5);
            title.setBackground(Display.getDefault().getSystemColor(color));
            title.setLayoutData(data);
            Label nbSong = new Label(albumComposite, SWT.LEFT | SWT.BOLD);
            nbSong.setFont(new Font(this.getDisplay(), "Arial", 8, SWT.ITALIC));
            nbSong.setText("Release date : " + albumNbSong);
            data = new FormData();
            data.top = new FormAttachment(50, 5);
            data.left = new FormAttachment(cover, 5);
            nbSong.setBackground(Display.getDefault().getSystemColor(color));
            nbSong.setLayoutData(data);
            prevCompo = albumComposite;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean useSWTBrowser() {
        return false;
    }
}
