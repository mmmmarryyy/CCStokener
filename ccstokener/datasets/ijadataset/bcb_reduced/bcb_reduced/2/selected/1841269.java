package ch.elca.el4ant.helper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Location;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.helper.AntXMLContext;
import org.apache.tools.ant.util.FileUtils;
import org.apache.tools.ant.util.JAXPUtils;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Ant ProjectHelper to support Ant build file loading from a resource and
 * replacement of targets in a already parsed build file.
 *
 * <script type="text/javascript">printFileStatus
 *   ("$URL: http://el4ant.svn.sourceforge.net/svnroot/el4ant/trunk/buildsystem/core/java/ch/elca/el4ant/helper/ProjectHelper.java $",
 *   "$Revision: 344 $", "$Date: 2006-04-19 10:43:21 -0400 (Wed, 19 Apr 2006) $", "$Author: yma $"
 * );</script>
 *
 * @author Yves Martin (YMA)
 * @version $Revision: 344 $
 */
public class ProjectHelper extends org.apache.tools.ant.helper.ProjectHelper2 {

    /** Helper class for path to URI and URI to path conversions. */
    private static FileUtils fu = FileUtils.newFileUtils();

    /**
     * Parses the project file or resource, configuring the project as it
     * goes. A <code>BuildException</code> is thrown if the configuration is
     * invalid or cannot be read
     *
     * @param project the current project
     * @param source  the xml source
     * @param handler the root handler to use (contains the current context)
     */
    public void parse(Project project, Object source, RootHandler handler) {
        AntXMLContext context = (AntXMLContext) Reflect.getField(handler, "context");
        File buildFile = null;
        URL url = null;
        String buildFileName = null;
        if (source instanceof File) {
            buildFile = (File) source;
            buildFile = fu.normalize(buildFile.getAbsolutePath());
            context.setBuildFile(buildFile);
            buildFileName = buildFile.toString();
        } else if (source instanceof URL) {
            url = (URL) source;
            buildFileName = url.toString();
        } else {
            throw new BuildException("Source " + source.getClass().getName() + " not supported by this plugin");
        }
        InputStream inputStream = null;
        InputSource inputSource = null;
        try {
            XMLReader parser = JAXPUtils.getNamespaceXMLReader();
            String uri = null;
            if (buildFile != null) {
                uri = fu.toURI(buildFile.getAbsolutePath());
                inputStream = new FileInputStream(buildFile);
            } else {
                inputStream = url.openStream();
                uri = url.toString();
            }
            inputSource = new InputSource(inputStream);
            if (uri != null) {
                inputSource.setSystemId(uri);
            }
            project.log("parsing buildfile " + buildFileName + " with URI = " + uri, Project.MSG_VERBOSE);
            DefaultHandler hb = handler;
            parser.setContentHandler(hb);
            parser.setEntityResolver(hb);
            parser.setErrorHandler(hb);
            parser.setDTDHandler(hb);
            parser.parse(inputSource);
        } catch (SAXParseException exc) {
            Location location = new Location(exc.getSystemId(), exc.getLineNumber(), exc.getColumnNumber());
            Throwable t = exc.getException();
            if (t instanceof BuildException) {
                BuildException be = (BuildException) t;
                if (be.getLocation() == Location.UNKNOWN_LOCATION) {
                    be.setLocation(location);
                }
                throw be;
            }
            throw new BuildException(exc.getMessage(), t, location);
        } catch (SAXException exc) {
            Throwable t = exc.getException();
            if (t instanceof BuildException) {
                throw (BuildException) t;
            }
            throw new BuildException(exc.getMessage(), t);
        } catch (FileNotFoundException exc) {
            throw new BuildException(exc);
        } catch (UnsupportedEncodingException exc) {
            throw new BuildException("Encoding of project file " + buildFileName + " is invalid.", exc);
        } catch (IOException exc) {
            throw new BuildException("Error reading project file " + buildFileName + ": " + exc.getMessage(), exc);
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ioe) {
                }
            }
        }
    }
}
