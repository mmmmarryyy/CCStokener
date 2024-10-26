package de.offis.semanticmm4u.tools.copy_media_elements;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import component_interfaces.semanticmm4u.realization.compositor.provided.IAudio;
import component_interfaces.semanticmm4u.realization.compositor.provided.IComplexOperator;
import component_interfaces.semanticmm4u.realization.compositor.provided.IImage;
import component_interfaces.semanticmm4u.realization.compositor.provided.IMedium;
import component_interfaces.semanticmm4u.realization.compositor.provided.IVariable;
import component_interfaces.semanticmm4u.realization.compositor.provided.IVideo;
import component_interfaces.semanticmm4u.realization.compositor.realization.IBasicOperator;
import de.offis.semanticmm4u.compositors.variables.AbstractVariable;
import de.offis.semanticmm4u.compositors.variables.VariableList;
import de.offis.semanticmm4u.failures.MM4UToolsException;
import de.offis.semanticmm4u.generators.GeneratorToolkit;
import de.offis.semanticmm4u.global.Debug;
import de.offis.semanticmm4u.global.Utilities;

public class CopyMediaElements {

    public static final String EXPORT_MEDIA_DIR = "media";

    /**
	 * Copys all media elements used by the presentation to the given exportPath. Also
	 * change the uri of the media elements to the new media location.
	 * 
	 * @param rootVariable the presentation root variable
	 * @param exportDirectory the path to exported presentation (not media!)
	 *            without trailing slash.
	 * @param outputFormat 
	 */
    public static void copyContent(IVariable rootVariable, String exportDirectory, int outputFormat) throws MM4UToolsException {
        VariableList varList = null;
        if (rootVariable instanceof IBasicOperator) {
            varList = (VariableList) rootVariable.getVariables();
        } else if (rootVariable instanceof IComplexOperator) {
            IComplexOperator tempComplexOperator = (IComplexOperator) rootVariable;
            copyContent(tempComplexOperator.getRootOperator(), exportDirectory, outputFormat);
        }
        if (varList == null) return;
        for (Iterator iter = varList.iterator(); iter.hasNext(); ) {
            AbstractVariable element = (AbstractVariable) iter.next();
            if ((element instanceof IVideo) || (element instanceof IImage) || (element instanceof IAudio)) {
                IMedium medium = (IMedium) element;
                String uri = medium.getURI();
                String uniqueMediaName = uri.hashCode() + "." + Utilities.getURISuffix(uri);
                String outputPathAndFilename = exportDirectory + EXPORT_MEDIA_DIR + File.separator + uniqueMediaName;
                Debug.println("CopyMediaElements->copyContent: media uri: " + uri + " - unique: " + uniqueMediaName);
                if (!(new File(outputPathAndFilename)).exists()) {
                    try {
                        URL url = new URL(uri);
                        Debug.println("Source file: " + url.toString());
                        Debug.println("Destination file: " + outputPathAndFilename);
                        InputStream srcStream = url.openStream();
                        FileOutputStream output = new FileOutputStream(outputPathAndFilename);
                        Utilities.copy(srcStream, output);
                    } catch (IOException excp) {
                        throw new MM4UToolsException(null, "copyContent", "Could not copy the media:\n" + excp);
                    }
                }
                File output = new File(outputPathAndFilename);
                try {
                    if (outputFormat == GeneratorToolkit.SMIL2_0 || outputFormat == GeneratorToolkit.SMIL2_0_BASIC_LANGUAGE_PROFILE || outputFormat == GeneratorToolkit.REALPLAYER_SMIL2_0 || outputFormat == GeneratorToolkit.REALPLAYER_SMIL2_0_BASIC_LANGUAGE_PROFILE || outputFormat == GeneratorToolkit.XMT_OMEGA) {
                        medium.setURI(EXPORT_MEDIA_DIR + "/" + uniqueMediaName);
                    } else medium.setURI(output.toURL().toString());
                } catch (MalformedURLException excp) {
                    throw new MM4UToolsException(null, "copyContent", "Could not transform the filename " + outputPathAndFilename + " into an url.");
                }
            }
            copyContent(element, exportDirectory, outputFormat);
        }
    }
}
