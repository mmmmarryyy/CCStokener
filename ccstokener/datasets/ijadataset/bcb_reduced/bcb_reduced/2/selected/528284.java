package storage.framework;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import org.nodal.nav.Path;

public class SBNURLLoader extends SBNStreamLoader {

    URL url;

    public SBNURLLoader(AbstractRepository repo, Path path, URL url) throws IOException {
        super(repo.backend, path, url.openConnection().getInputStream(), null);
        this.url = url;
    }

    public String mimeType() {
        if (mimeType == null) {
            String name = url.getPath();
            mimeType = StreamBasedNode.guessContentTypeFromName(name);
            if (mimeType == null) {
                try {
                    mimeType = URLConnection.guessContentTypeFromStream(stream);
                } catch (IOException e) {
                    ;
                }
            }
            if (mimeType == null) {
                mimeType = "application/octet-stream";
            }
        }
        return mimeType;
    }
}
