package aspiration;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * filtre de taille de fichier
 *
 */
public class FileSizeFilter extends DownloaderDecorator {

    /**
	 * taille maximum d'un fichier
	 */
    public int fileMaxSize;

    /**
	 * constructeur
	 * @param _dl downloader d�cor�
	 */
    public FileSizeFilter(Downloader _dl) {
        super(_dl);
    }

    /**
	 * application du filtre et appel � save() sur le downloader d�cor�
	 */
    public void save() {
        if (sizeOk()) {
            downloader.save();
        }
    }

    /**
	 * acc�s � la taille max
	 * @return taille max d'un fichier
	 */
    public int getFileMaxSize() {
        return fileMaxSize;
    }

    /**
	 * modification de la taille max
	 * @param fileMaxSize nouvelle taille max d'un fichier
	 */
    public void setFileMaxSize(int fileMaxSize) {
        this.fileMaxSize = fileMaxSize;
    }

    /**
	 * test sur la taille du fichier
	 * @return revoie true si le fichier a une taille inf�rieure au max
	 */
    public boolean sizeOk() {
        int size;
        try {
            InputStream in = url.openStream();
            size = in.available();
            if (size > fileMaxSize) {
                System.out.println("Filtering " + url.toString() + " : over maximum file size");
                return false;
            } else {
                SiteCapturer.accumulatedSize += size;
                if (SiteCapturer.accumulatedSize < SiteCapturer.maxSize) {
                    return true;
                } else {
                    System.out.println("Reaching site size maximum, stopping the download");
                    return false;
                }
            }
        } catch (FileNotFoundException fn) {
            System.out.println("File not found : " + this.url);
        } catch (IOException e) {
            System.out.println("Connection failed to : " + this.url);
        } catch (NullPointerException npe) {
        }
        return true;
    }

    /**
	 * modification de l'url du site web
	 */
    public void setUrl(String _url) {
        try {
            url = new URL(_url);
            downloader.setUrl(_url);
        } catch (MalformedURLException e) {
            System.out.println("Malformed url : " + this.url);
        }
    }
}
