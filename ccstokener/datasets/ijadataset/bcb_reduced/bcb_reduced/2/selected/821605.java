package com.ideo.jso;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.io.Writer;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.PageContext;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import com.ideo.jso.minimifier.JSMin;
import com.ideo.jso.minimifier.YUICompressorAdaptor;
import com.ideo.jso.minimifier.JSMin.UnterminatedCommentException;
import com.ideo.jso.minimifier.JSMin.UnterminatedRegExpLiteralException;
import com.ideo.jso.minimifier.JSMin.UnterminatedStringLiteralException;
import com.ideo.jso.servlet.JsoServlet;

/**
 * Represents a group of resources
 * @author Julien Maupoux
 *
 */
public class Group {

    /**
	 * Logger
	 */
    private static final Logger log = Logger.getLogger(Group.class);

    /**
	 * Buffers for merged resources
	 */
    private ResourcesBuffer completeBuffer = new ResourcesBuffer();

    private ResourcesBuffer minimizedBuffer = new ResourcesBuffer();

    private ResourcesBuffer cssBuffer = new ResourcesBuffer();

    /**
	 * Parameters from the configuration file 
	 */
    private boolean minimize;

    private boolean minimizeCss;

    /**
	 * Groups referenced into this group
	 */
    private List subgroups = new ArrayList();

    /**
	 * List of resources name
	 */
    private List jsNames = new ArrayList();

    private List cssNames = new ArrayList();

    private List deepJsNames;

    /**
	 * Name of the group
	 */
    private String name;

    /**
	 * 
	 * @param name the name of the group
	 */
    public Group(String name) {
        this.name = name;
    }

    /**
	 * Display the css import tags, depending of the group parameters set in the XML descriptor.
	 * @param pageContext
	 * @param out the writer into which will be written the tags  
	 * @throws IOException
	 */
    public void printIncludeCSSTag(PageContext pageContext, Writer out) throws IOException {
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        if (!isMinimizeCss()) {
            for (int i = 0; i < getCssNames().size(); i++) includeResource(pageContext, out, (String) getCssNames().get(i), "<link rel=\"stylesheet\" type=\"text/css\" href=\"", "\"/>");
        } else {
            if (getCssNames().size() != 0) {
                long cssTimeStamp = getMaxCSSTimestamp(pageContext.getServletContext());
                out.write("<link rel=\"stylesheet\" type=\"text/css\" href=\"");
                out.write(request.getContextPath());
                out.write("/jso/");
                out.write(getName());
                out.write(".css?" + JsoServlet.TIMESTAMP + "=");
                out.write("" + cssTimeStamp);
                out.write("\"></link>\n");
            }
        }
        for (Iterator iterator = getSubgroups().iterator(); iterator.hasNext(); ) {
            Group subGroup = (Group) iterator.next();
            subGroup.printIncludeCSSTag(pageContext, out);
        }
    }

    /**
	 * Display the js import tags, depending of the group parameters set in the XML descriptor and of the tag exploded parameter.
	 * @param pageContext
	 * @param out the writer into which will be written the tags  
	 * @param exploded the way to import JS files : merged or not
	 * @throws IOException
	 */
    public void printIncludeJSTag(PageContext pageContext, Writer out, boolean exploded) throws IOException {
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        if (exploded) {
            for (int i = 0; i < getDeepJsNames().size(); i++) includeResource(pageContext, out, (String) getDeepJsNames().get(i), "<script type=\"text/javascript\" src=\"", "\"></script>");
        } else {
            long maxJSTimestamp = getMaxJSTimestamp(pageContext.getServletContext());
            out.write("<script type=\"text/javascript\" src=\"");
            out.write(request.getContextPath());
            out.write("/jso/");
            out.write(getName());
            out.write(".js?" + JsoServlet.TIMESTAMP + "=");
            out.write("" + maxJSTimestamp);
            out.write("\"></script>\n");
        }
    }

    /**
	 * 
	 * @param fileName
	 * @return the last modification date of the file, as a long
	 */
    private long getFileTimeStamp(String fileName) {
        if (fileName != null && fileName.length() > 0) {
            long lastModif = new File(fileName).lastModified();
            if (lastModif == 0) log.info("The file named : " + fileName + " could not be found on the disk. Maybe into jars.");
            return lastModif;
        }
        return 0;
    }

    /**
	 * 
	 * @param servletContext
	 * @return the more recent modification date of the css file of this group, as a long
	 * @throws MalformedURLException
	 */
    private long getMaxCSSTimestamp(ServletContext servletContext) throws MalformedURLException {
        long maxJSTimeStamp = cssBuffer.getTimestamp();
        List files = getCssNames();
        for (int i = 0; i < files.size(); i++) {
            String webPath = (String) files.get(i);
            String fileName = servletContext.getRealPath(webPath);
            long mx = getFileTimeStamp(fileName);
            if (mx > maxJSTimeStamp) maxJSTimeStamp = mx;
        }
        return maxJSTimeStamp;
    }

    /**
	 * 
	 * @param servletContext
	 * @return the more recent modification date of the js file of this group and of its subgroups, as a long
	 * @throws MalformedURLException
	 */
    private long getMaxJSTimestamp(ServletContext servletContext) throws MalformedURLException {
        long maxJSTimeStamp = 0;
        for (int i = 0; i < subgroups.size(); i++) {
            Group subGroup = (Group) subgroups.get(i);
            long mx = subGroup.getMaxJSTimestamp(servletContext);
            if (mx > maxJSTimeStamp) maxJSTimeStamp = mx;
        }
        List files = getJsNames();
        for (int i = 0; i < files.size(); i++) {
            String webPath = (String) files.get(i);
            String fileName = servletContext.getRealPath(webPath);
            long mx = getFileTimeStamp(fileName);
            if (mx > maxJSTimeStamp) maxJSTimeStamp = mx;
        }
        return maxJSTimeStamp;
    }

    /**
	 * Adds a tag into the flow to include a resource "the classic way", and suffix by a timestamp corresponding to the last modification date of the file
	 * @param pageContext
	 * @param out
	 * @param webPath
	 * @param tagBegin
	 * @param tagEnd
	 * @throws IOException
	 */
    private void includeResource(PageContext pageContext, Writer out, String webPath, String tagBegin, String tagEnd) throws IOException {
        HttpServletRequest request = (HttpServletRequest) pageContext.getRequest();
        String fileName = pageContext.getServletContext().getRealPath(webPath);
        out.write(tagBegin);
        out.write(request.getContextPath());
        if (!webPath.startsWith("/")) out.write("/");
        out.write(webPath);
        if (fileName != null && fileName.length() > 0) {
            long timestamp = new File(fileName).lastModified();
            out.write("?" + JsoServlet.TIMESTAMP + "=" + timestamp);
        }
        out.write(tagEnd);
        out.write("\n");
    }

    /**
	 * Return the JavaScript merged corresponding to a requested timestamp.
	 * @param servletContext
	 * @param timestamp the timestamp requested
	 * @return
	 * @throws IOException
	 */
    public byte[] getJsMerged(ServletContext servletContext, long timestamp) throws IOException {
        return getJsMerged(servletContext, timestamp, this.minimize);
    }

    /**
	 * Return the CSS merged corresponding to a requested timestamp.
	 * @param servletContext
	 * @param timestamp the timestamp requested
	 * @return
	 * @throws IOException 
	 */
    public byte[] getCssMerged(ServletContext servletContext, long timestamp) throws IOException {
        if (cssBuffer.getTimestamp() == timestamp) {
            log.debug("Returning buffered css data for group : " + name);
            return cssBuffer.getData();
        } else cssBuffer.clean();
        byte[] merge = getMergedContent(getCssNames(), servletContext);
        byte[] min = YUICompressorAdaptor.compressCSS(new StringReader(new String(merge))).getBytes();
        cssBuffer.update(min, timestamp);
        return min;
    }

    /**
	 * Merge the JS files of this group and of its subgroups
	 * @param servletContext
	 * @param timestamp the timestamp requested
	 * @param isMinimized the mode of merging
	 * @return
	 * @throws IOException
	 */
    private byte[] mergeDeepJsFiles(ServletContext servletContext, long timestamp, boolean isMinimized) throws IOException {
        log.debug("Concatenating js files for group : " + name + ".");
        ByteArrayOutputStream res = new ByteArrayOutputStream();
        for (int i = 0; i < subgroups.size(); i++) {
            Group group = (Group) subgroups.get(i);
            res.write(group.getJsMerged(servletContext, timestamp, isMinimized));
        }
        byte[] content = getMergedContent(getJsNames(), servletContext);
        res.write(content);
        return res.toByteArray();
    }

    /**
	 * Merge and minimize JavaScript files of this group and its subgroup, depending of the minimization passed as parameter 
	 * @param servletContext
	 * @param timestamp the requested timestamp
	 * @param isMinimized the requested minimization mode
	 * @return
	 * @throws IOException
	 */
    private byte[] getJsMergedMinimized(ServletContext servletContext, long timestamp, boolean isMinimized) throws IOException {
        if (minimizedBuffer.getTimestamp() == timestamp) {
            log.debug("Returning buffered js data for group : " + name);
            return minimizedBuffer.getData();
        } else minimizedBuffer.clean();
        byte[] merge = mergeDeepJsFiles(servletContext, timestamp, isMinimized);
        ByteArrayOutputStream jsMinout = new ByteArrayOutputStream();
        try {
            new JSMin(new ByteArrayInputStream(merge), jsMinout).jsmin();
        } catch (UnterminatedRegExpLiteralException e) {
            e.printStackTrace();
        } catch (UnterminatedCommentException e) {
            e.printStackTrace();
        } catch (UnterminatedStringLiteralException e) {
            e.printStackTrace();
        }
        byte[] min = jsMinout.toByteArray();
        minimizedBuffer.update(min, timestamp);
        return min;
    }

    /**
	 * Merge but keep safe the JavaScript files of this group and its subgroup, depending of the minimization passed as parameter 
	 * @param servletContext
	 * @param timestamp the requested timestamp
	 * @param isMinimized the requested minimization mode
	 * @return
	 * @throws IOException
	 */
    private byte[] getJsMergedComplete(ServletContext servletContext, long timestamp, boolean isMinimized) throws IOException {
        if (completeBuffer.getTimestamp() == timestamp) {
            log.debug("Returning buffered js data for group : " + name);
            return completeBuffer.getData();
        }
        byte[] merge = mergeDeepJsFiles(servletContext, timestamp, isMinimized);
        completeBuffer.update(merge, timestamp);
        return merge;
    }

    /**
	 * Merge JavaScript files of this group and its subgroup, depending of the minimization passed as parameter
	 * @param servletContext
	 * @param timestamp the timestamp requested
	 * @param isMinimized the mode requested
	 * @return
	 * @throws IOException
	 */
    private byte[] getJsMerged(ServletContext servletContext, long timestamp, boolean isMinimized) throws IOException {
        if (isMinimized) return getJsMergedMinimized(servletContext, timestamp, isMinimized); else return getJsMergedComplete(servletContext, timestamp, isMinimized);
    }

    /**
	 * 
	 * @return All the JavaScript files declared in this group and its subgroups referenced. 
	 */
    public List getDeepJsNames() {
        if (deepJsNames == null) {
            List res = new ArrayList(getJsNames());
            for (Iterator iterator = getSubgroups().iterator(); iterator.hasNext(); ) {
                Group subGroup = (Group) iterator.next();
                res.addAll(subGroup.getDeepJsNames());
            }
            deepJsNames = res;
        }
        return deepJsNames;
    }

    /**
	 * Merge the content of a list of resources
	 * @param names the name of the resources to merge
	 * @param servletContext
	 * @return an array of byte[] representing the content merged
	 * @throws IOException
	 */
    private byte[] getMergedContent(List names, ServletContext servletContext) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (Iterator iterator = names.iterator(); iterator.hasNext(); ) {
            String path = (String) iterator.next();
            if (!path.startsWith("/")) path = "/" + path;
            URL url = servletContext.getResource(path);
            if (url == null) url = getClass().getResource(path);
            if (url == null) throw new IOException("The resources '" + path + "' could not be found neither in the webapp folder nor in a jar");
            log.debug("Merging content of group : " + getName());
            InputStream inputStream = url.openStream();
            InputStreamReader r = new InputStreamReader(inputStream);
            IOUtils.copy(r, baos, "ASCII");
            baos.write((byte) '\n');
            inputStream.close();
        }
        baos.close();
        return baos.toByteArray();
    }

    public boolean isMinimize() {
        return minimize;
    }

    public void setMinimize(boolean minimize) {
        this.minimize = minimize;
    }

    public List getSubgroups() {
        return subgroups;
    }

    public void setSubgroups(List subgroups) {
        this.subgroups = subgroups;
    }

    public List getJsNames() {
        return jsNames;
    }

    public void setJsNames(List jsNames) {
        this.jsNames = jsNames;
    }

    public List getCssNames() {
        return cssNames;
    }

    public void setCssNames(List cssNames) {
        this.cssNames = cssNames;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isMinimizeCss() {
        return minimizeCss;
    }

    public void setMinimizeCss(boolean minimizeCss) {
        this.minimizeCss = minimizeCss;
    }
}
