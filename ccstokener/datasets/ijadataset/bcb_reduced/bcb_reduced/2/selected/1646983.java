package edu.psu.lias;

import edu.psu.its.lionshare.LionShareAccessPoint;
import edu.psu.its.lionshare.share.gnutella.GnutellaDataObjectGroup;
import java.io.*;
import java.net.*;
import org.hibernate.Session;
import edu.psu.its.lionshare.database.VirtualDirectorySelect;
import edu.psu.its.lionshare.database.VirtualDirectory;
import edu.psu.its.lionshare.metadata.MetadataImporter;
import edu.psu.its.lionshare.metadata.MetadataImportException;

public class ContentDMImporter extends MetadataImporter {

    public void parseImportFile(java.io.File file_to_parse) {
        long virtual_directory_id = 0;
        String inLine = "";
        String imageFilePath = "";
        String imageFile;
        String imageFileURL = "";
        String MetadataXMLSchemaURI = "";
        String XMLData = "";
        String virtualDir = null;
        BufferedReader infile = null;
        imageFilePath = file_to_parse.getParent();
        try {
            infile = new BufferedReader(new FileReader(file_to_parse));
            inLine = getNextTag("<collection>", infile);
            inLine = getNextTag("<name>", infile);
            virtualDir = getNextDataElement(infile);
            virtualDir.trim();
            VirtualDirectory vir_dir = new VirtualDirectory(virtualDir, null, System.currentTimeMillis(), 0l, 0l, 0l, 0l, 0l, true, false);
            GnutellaDataObjectGroup gdog = new GnutellaDataObjectGroup(vir_dir, LionShareAccessPoint.getInstance().getDefaultShareManager());
            LionShareAccessPoint.getInstance().getDefaultShareManager().addDataGroup(gdog);
            inLine = getNextTag("<dtd>", infile);
            MetadataXMLSchemaURI = getNextDataElement(infile);
            MetadataXMLSchemaURI.trim();
            inLine = getNextTag("<object>", infile);
            inLine = getNextTag("<item>", infile);
            while (inLine != null) {
                inLine = getNextTag("<image_file>", infile);
                imageFileURL = getNextDataElement(infile);
                imageFileURL.trim();
                inLine = getNextTag("<metadata>", infile);
                XMLData = getNextTag("<dc>", infile);
                XMLData = XMLData.substring(4, XMLData.length());
                inLine = getNextDataElement(infile);
                while (inLine != null) {
                    if (inLine.length() >= 5) {
                        if (inLine.substring(0, 5).equals("</dc>")) {
                            break;
                        }
                    }
                    if (XMLData.endsWith(" ")) {
                        XMLData = XMLData + inLine;
                    } else {
                        XMLData = XMLData + " " + inLine;
                    }
                    inLine = getNextDataElement(infile);
                }
                XMLData = "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:dc=\"http://purl.org/dc/elements/1.1/\">" + "<rdf:Description rdf:about=\"" + imageFileURL + "\">" + XMLData + "</rdf:Description></rdf:RDF>";
                System.out.println("XML: " + XMLData);
                imageFile = copyImageFile(imageFileURL, imageFilePath);
                File indexFile = new File(imageFile);
                LionShareAccessPoint.getInstance().getDefaultShareManager().addObjectToGroup(gdog, indexFile);
                try {
                    insertMetadataForFilePath(XMLData, imageFile, MetadataXMLSchemaURI);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                inLine = getNextTag("<item>", infile);
            }
        } catch (FileNotFoundException ex) {
            System.out.println("File not found: " + file_to_parse);
        } catch (IOException ex) {
            System.out.println("Other IO error.");
        } finally {
            try {
                if (infile != null) infile.close();
            } catch (IOException ex) {
                System.out.println("Close error.");
            }
        }
    }

    private String getNextTag(String xmlTag, BufferedReader inFile) throws IOException {
        String inLine;
        try {
            while ((inLine = inFile.readLine()) != null) {
                inLine.trim();
                if (xmlTag.length() <= inLine.length()) {
                    if (inLine.substring(0, xmlTag.length()).equals(xmlTag)) return inLine;
                }
            }
            return null;
        } catch (IOException ex) {
            throw new IOException();
        }
    }

    private String getNextDataElement(BufferedReader inFile) throws IOException {
        String inLine = "";
        try {
            if ((inLine = inFile.readLine()) != null) {
                inLine.trim();
            }
            return inLine;
        } catch (IOException ex) {
            throw new IOException();
        }
    }

    private long createVirtualDirectory(String virtualDir) {
        VirtualDirectory vir_dir = new VirtualDirectory(virtualDir, null, System.currentTimeMillis(), 0l, 0l, 0l, 0l, 0l, true, false);
        try {
            Session session = VirtualDirectorySelect.getSession();
            VirtualDirectorySelect.insert(vir_dir, session);
            session.close();
        } catch (Exception e) {
            System.out.println("Virtual Directory Exception");
        }
        return (vir_dir.getId().longValue());
    }

    private String copyImageFile(String urlString, String filePath) {
        FileOutputStream destination = null;
        File destination_file = null;
        String inLine;
        String dest_name = "";
        byte[] buffer;
        int bytes_read;
        int last_offset = 0;
        int offset = 0;
        InputStream imageFile = null;
        try {
            URL url = new URL(urlString);
            imageFile = url.openStream();
            dest_name = url.getFile();
            offset = 0;
            last_offset = 0;
            offset = dest_name.indexOf('/', offset + 1);
            while (offset > -1) {
                last_offset = offset + 1;
                offset = dest_name.indexOf('/', offset + 1);
            }
            dest_name = filePath + File.separator + dest_name.substring(last_offset);
            destination_file = new File(dest_name);
            if (destination_file.exists()) {
                if (destination_file.isFile()) {
                    if (!destination_file.canWrite()) {
                        System.out.println("FileCopy: destination " + "file is unwriteable: " + dest_name);
                    }
                    System.out.println("File " + dest_name + " already exists. File will be overwritten.");
                } else {
                    System.out.println("FileCopy: destination " + "is not a file: " + dest_name);
                }
            } else {
                File parentdir = parent(destination_file);
                if (!parentdir.exists()) {
                    System.out.println("FileCopy: destination " + "directory doesn't exist: " + dest_name);
                }
                if (!parentdir.canWrite()) {
                    System.out.println("FileCopy: destination " + "directory is unwriteable: " + dest_name);
                }
            }
            destination = new FileOutputStream(dest_name);
            buffer = new byte[1024];
            while (true) {
                bytes_read = imageFile.read(buffer);
                if (bytes_read == -1) break;
                destination.write(buffer, 0, bytes_read);
            }
        } catch (MalformedURLException ex) {
            System.out.println("Bad URL " + urlString);
        } catch (IOException ex) {
            System.out.println(" IO error: " + ex.getMessage());
        } finally {
            if (imageFile != null) {
                try {
                    imageFile.close();
                } catch (IOException e) {
                }
            }
            if (destination != null) {
                try {
                    destination.close();
                } catch (IOException e) {
                }
            }
        }
        return (dest_name);
    }

    private static File parent(File f) {
        String dirname = f.getParent();
        if (dirname == null) {
            if (f.isAbsolute()) {
                return new File(File.separator);
            } else {
                return new File(System.getProperty("user.dir"));
            }
        }
        return new File(dirname);
    }
}
