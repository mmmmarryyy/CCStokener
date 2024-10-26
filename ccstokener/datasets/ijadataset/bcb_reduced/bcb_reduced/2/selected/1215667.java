package com.lovehorsepower.imagemailerapp.html;

import au.id.jericho.lib.html.Source;
import java.net.URL;
import au.id.jericho.lib.html.TextExtractor;
import au.id.jericho.lib.html.StartTag;
import au.id.jericho.lib.html.HTMLElementName;
import java.util.List;
import java.util.Iterator;
import au.id.jericho.lib.html.Element;
import java.util.ArrayList;
import java.io.IOException;
import au.id.jericho.lib.html.Attribute;
import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.Tag;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.HashMap;
import javax.swing.JLabel;

public class ExtractText {

    private static int MIN_IMAGE_SIZE_IN_BYTES = 1024 * 15;

    ArrayList returnList = null;

    HashMap uniqueList = null;

    HashMap uniqueHrefList = null;

    JLabel updateLabel = null;

    String labelText = "";

    StringBuffer dots = new StringBuffer(".");

    int dotCount = 0;

    public ExtractText(JLabel updateLabel) {
        uniqueList = new HashMap();
        uniqueHrefList = new HashMap();
        returnList = new ArrayList();
        this.updateLabel = updateLabel;
        this.labelText = updateLabel.getText();
    }

    public String getTextFromHTML(String htmlText) {
        Source source = new Source(htmlText.subSequence(0, htmlText.length()));
        source.fullSequentialParse();
        TextExtractor textExtractor = new TextExtractor(source) {

            public boolean excludeElement(StartTag startTag) {
                return startTag.getName() == HTMLElementName.P || "control".equalsIgnoreCase(startTag.getAttributeValue("class"));
            }
        };
        return textExtractor.setIncludeAttributes(false).toString();
    }

    public ArrayList getDeepImageLinksFromHTML(String sourceUrlString, boolean deepScan) {
        Iterator it = null;
        String newHref = "";
        try {
            getImageLinksFromHTML(sourceUrlString, true);
        } catch (IOException ex) {
            System.out.println("Got error on link: " + sourceUrlString + ": " + ex);
            return null;
        }
        System.out.println("UniqeList size: " + uniqueList.size());
        if (deepScan) {
            it = uniqueHrefList.keySet().iterator();
            System.out.println("HRef size: " + uniqueHrefList.size());
            while (it != null && it.hasNext()) {
                newHref = (String) it.next();
                try {
                    getImageLinksFromHTML(newHref, false);
                } catch (IOException ex1) {
                    System.out.println("Error on link: " + newHref + ": " + ex1 + " Attempt to continue...");
                }
            }
        }
        it = uniqueList.keySet().iterator();
        System.out.println("UniqueListSize (2): " + uniqueList.size());
        while (it != null && it.hasNext()) {
            returnList.add((String) it.next());
        }
        java.util.Collections.sort(returnList);
        return returnList;
    }

    private void getImageLinksFromHTML(String sourceUrlString, boolean buildHrefHash) throws IOException {
        Source source = null;
        try {
            source = new Source(new URL(sourceUrlString));
        } catch (Exception ex) {
            System.out.println("Got exception: " + ex + " attempt to continue..");
            return;
        }
        Attribute linkAttr = null;
        String baseURL = "";
        String rootURL = "";
        int index = 0;
        List linkElements = null;
        index = sourceUrlString.lastIndexOf("/");
        if (index > 8 && index != -1) {
            if (index != -1) {
                baseURL = sourceUrlString.substring(0, index);
            } else {
                baseURL = new String(sourceUrlString);
            }
            if (baseURL.indexOf("/?") != -1) {
                baseURL = baseURL.substring(0, baseURL.indexOf("/?"));
            }
            System.out.println("SourceURL: " + sourceUrlString);
            System.out.println("urlBase: " + baseURL);
            dots.append(".");
            dotCount++;
            updateLabel.setText("Scanning " + baseURL + dots.toString());
            if (dotCount > 10) {
                dotCount = 0;
                dots = new StringBuffer(".");
            }
            index = sourceUrlString.substring("http://".length()).indexOf("/");
            rootURL = sourceUrlString.substring(0, index + "http://".length());
        } else {
            baseURL = "" + sourceUrlString;
            rootURL = "" + sourceUrlString;
        }
        System.out.println("Have rootURL: " + rootURL);
        if (rootURL.length() < 15 || baseURL.length() < 15) {
            System.out.println("Bogus root or base URL: " + rootURL + ", " + baseURL);
            return;
        }
        source.fullSequentialParse();
        linkElements = source.findAllElements();
        for (Iterator i = linkElements.iterator(); i.hasNext(); ) {
            Element linkElement = (Element) i.next();
            if (linkElement == null || linkElement.getAttributes() == null) {
                continue;
            }
            Iterator it = linkElement.getAttributes().iterator();
            while (it != null && it.hasNext()) {
                linkAttr = (Attribute) it.next();
                if (linkAttr == null || linkAttr.getValue() == null) {
                    continue;
                }
                if (linkAttr.getValue().endsWith(".jpg") || linkAttr.getValue().endsWith(".jpeg") || linkAttr.getValue().endsWith(".JPG")) {
                    if (linkAttr.getValue().startsWith("http://")) {
                        uniqueList.put(linkAttr.getValue(), linkAttr.getValue());
                    } else {
                        if (linkAttr.getValue().startsWith("/")) {
                            uniqueList.put(rootURL + linkAttr.getValue(), rootURL + linkAttr.getValue());
                        } else {
                            uniqueList.put(baseURL + "/" + linkAttr.getValue(), baseURL + "/" + linkAttr.getValue());
                        }
                    }
                } else if (linkAttr.getValue().indexOf("youtube.com") == -1) {
                    if (buildHrefHash) {
                        if (linkAttr.getName().equalsIgnoreCase("href")) {
                            if (linkAttr.getValue().startsWith("http://")) {
                                uniqueHrefList.put(linkAttr.getValue(), linkAttr.getValue());
                            } else {
                                if (linkAttr.getValue().startsWith("/")) {
                                    uniqueHrefList.put(rootURL + linkAttr.getValue(), rootURL + linkAttr.getValue());
                                } else {
                                    uniqueHrefList.put(baseURL + "/" + linkAttr.getValue(), baseURL + "/" + linkAttr.getValue());
                                }
                            }
                        }
                    }
                } else {
                    uniqueList.put(linkAttr.getValue(), linkAttr.getValue());
                }
            }
        }
    }

    /**
     * writeURLToFile
     *
     * @param url URL
     * @param path String
     */
    public String writeURLToFile(URL url, String path, int minHeight, int minWidth) throws IOException {
        String filename = "";
        String tmpFilename = "";
        int index = 0;
        File imageFile = null;
        File renameFile = null;
        Metadata metadata = null;
        Integer width = null;
        Integer height = null;
        boolean foundWidth = false;
        boolean foundHeight = false;
        String widthString = "";
        String heightString = "";
        String newFilename = "";
        String oldFilename = null;
        int count = 0;
        int numImagesSaved = 0;
        if (url.getFile() == null || url.getFile().indexOf("/") == -1) {
            throw new IOException("Invalid link");
        }
        index = url.getFile().lastIndexOf("/");
        tmpFilename = url.getFile().substring(index);
        filename = path + tmpFilename;
        System.out.println("Process link " + url.toString() + " with filename: " + filename);
        oldFilename = new String(filename);
        while (true) {
            imageFile = new File(filename);
            if (imageFile.exists()) {
                newFilename = oldFilename.substring(0, oldFilename.lastIndexOf(".")) + count + oldFilename.substring(oldFilename.lastIndexOf("."));
                filename = new String(newFilename);
                if (count > 1000) {
                    break;
                }
                count++;
            } else {
                break;
            }
        }
        InputStream in = new BufferedInputStream(url.openStream());
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buffer = new byte[16384];
        for (int read = 0; (read = in.read(buffer)) != -1; out.write(buffer, 0, read)) ;
        if (filename.indexOf("%") != -1) {
            imageFile = new File(filename);
            renameFile = new File(filename.replace("%", "_"));
            imageFile.renameTo(renameFile);
            filename = new String(renameFile.getAbsolutePath());
        }
        if (out.toByteArray().length > MIN_IMAGE_SIZE_IN_BYTES) {
            RandomAccessFile file = new RandomAccessFile(filename, "rw");
            file.write(out.toByteArray());
            file.close();
            imageFile = new File(filename);
            try {
                metadata = ImageMetadataReader.readMetadata(imageFile);
            } catch (ImageProcessingException ex) {
                System.out.println("Couldn't get meta data: " + ex);
                if (ex.toString().indexOf("not the correct format") != -1) {
                    imageFile.delete();
                    return "";
                }
                metadata = null;
            }
            if (metadata != null) {
                Iterator directories = metadata.getDirectoryIterator();
                while (directories.hasNext()) {
                    Directory directory = (Directory) directories.next();
                    Iterator tags = directory.getTagIterator();
                    while (tags.hasNext()) {
                        Tag tag = (Tag) tags.next();
                        try {
                            if (tag.getTagName().contains("Width")) {
                                try {
                                    widthString = tag.getDescription().substring(0, tag.getDescription().indexOf(" "));
                                } catch (Exception ex3) {
                                    System.out.println("Could not get width: " + ex3 + " for tag description: " + tag.getDescription());
                                    foundWidth = false;
                                }
                                try {
                                    width = new Integer(widthString);
                                    foundWidth = true;
                                } catch (NumberFormatException ex2) {
                                    foundWidth = false;
                                }
                            }
                            if (tag.getTagName().contains("Height")) {
                                try {
                                    heightString = tag.getDescription().substring(0, tag.getDescription().indexOf(" "));
                                } catch (Exception ex4) {
                                    System.out.println("Could not get height: " + ex4 + " for tag description: " + tag.getDescription());
                                    foundHeight = false;
                                }
                                try {
                                    height = new Integer(heightString);
                                    foundHeight = true;
                                } catch (NumberFormatException ex2) {
                                    foundHeight = false;
                                }
                            }
                            if (foundHeight && foundWidth) {
                                break;
                            }
                        } catch (MetadataException ex1) {
                            System.out.println("EX1: " + ex1);
                        }
                    }
                    if (foundHeight && foundWidth) {
                        break;
                    }
                }
                if (foundWidth == false || foundHeight == false) {
                    numImagesSaved++;
                    return filename;
                } else {
                    System.out.println("Have width: " + width.toString());
                    System.out.println("Have height: " + height.toString());
                    if (width.intValue() < minWidth && height.intValue() < minHeight) {
                        imageFile.delete();
                        return "";
                    }
                    if (height.intValue() < 165) {
                        imageFile.delete();
                        return "";
                    }
                    if (width.intValue() > (height.intValue() * 4)) {
                        imageFile.delete();
                        return "";
                    }
                }
            } else {
                numImagesSaved++;
                return filename;
            }
        } else {
            return "";
        }
        numImagesSaved++;
        return filename;
    }
}
