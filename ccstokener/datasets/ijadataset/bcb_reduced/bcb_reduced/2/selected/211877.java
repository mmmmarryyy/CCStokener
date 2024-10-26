package net.sourceforge.jruntimedesigner.provider;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import net.sourceforge.jruntimedesigner.JRuntimeDesignerController;
import net.sourceforge.jruntimedesigner.utils.XmlException;

/**
 * Layout provider that stores the layouts as a file and offers dialogs for
 * selection the file to open or selecting the name of the file for saving.
 * 
 * @author ikunin
 * @author $Author: ikunin $ (Last change)
 * @version $Revision: 10780 $ $Date: 2007-08-17 14:32:53 +0200 (Fr, 17 Aug
 *          2007) $
 * @since 1.0
 */
public class URLLayoutDataProvider implements ILayoutDataProvider {

    private URL fileURL;

    public URLLayoutDataProvider(URL fileURL) {
        this.fileURL = fileURL;
    }

    public void load(JRuntimeDesignerController controller) throws LayoutDataProviderException {
        try {
            controller.load(fileURL.openStream());
        } catch (IOException ex) {
            throw new LayoutDataProviderException(ex.getMessage(), ex);
        } catch (XmlException ex) {
            throw new LayoutDataProviderException(ex.getMessage(), ex);
        }
    }

    public void save(JRuntimeDesignerController controller) throws LayoutDataProviderException {
        try {
            URLConnection urlCon = fileURL.openConnection();
            urlCon.setDoOutput(true);
            controller.save(fileURL.openConnection().getOutputStream());
        } catch (IOException ex) {
            throw new LayoutDataProviderException(ex.getMessage(), ex);
        } catch (XmlException ex) {
            throw new LayoutDataProviderException(ex.getMessage(), ex);
        }
    }
}
