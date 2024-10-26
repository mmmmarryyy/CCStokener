package starcraft.gameclient.rcp;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.eclipse.core.runtime.Platform;
import org.newdawn.slick.util.ResourceLocation;

public class BundleResourceLocation implements ResourceLocation {

    private final String bundleID;

    public BundleResourceLocation(String bundleID) {
        this.bundleID = bundleID;
    }

    @Override
    public InputStream getResourceAsStream(String ref) {
        try {
            URL url = Platform.getBundle(bundleID).getResource(ref);
            if (url == null) {
                throw new RuntimeException("Failed to load resource: " + ref);
            }
            return url.openStream();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load resource.", e);
        }
    }

    @Override
    public URL getResource(String ref) {
        return Platform.getBundle(bundleID).getResource(ref);
    }
}
