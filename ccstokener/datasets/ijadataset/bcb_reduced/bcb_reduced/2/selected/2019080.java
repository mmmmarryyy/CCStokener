package pedro.io;

import org.xml.sax.SAXException;
import pedro.metaData.*;
import pedro.mda.model.RecordModel;
import pedro.mda.model.RecordModelFactory;
import pedro.mda.config.PedroConfigurationReader;
import pedro.system.*;
import pedro.soa.id.IDGeneratorService;
import java.io.*;
import java.net.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.ArrayList;
import org.xml.sax.*;
import org.xml.sax.helpers.*;
import javax.xml.parsers.*;

public class NativeFileFormatReader {

    private final int BUFFER_SIZE = 2048;

    private File nativeFormatFile;

    private File metaDataFile;

    private URL applicationSchemaURL;

    private RecordModel rootModel;

    private RecordModel metaDataRootModel;

    private PedroDataFileReader pedroDataFileReader;

    private ArrayList temporaryFiles;

    private PedroFormContext pedroFormContext;

    private DocumentMetaData documentMetaData;

    public NativeFileFormatReader(PedroFormContext pedroFormContext) {
        this.pedroFormContext = pedroFormContext;
        temporaryFiles = new ArrayList();
        pedroDataFileReader = new PedroDataFileReader(pedroFormContext, false);
    }

    public RecordModel getRootModel() {
        return rootModel;
    }

    public DocumentMetaData getDocumentMetaData() {
        return documentMetaData;
    }

    public RecordModel getMetaDataRootModel() {
        return metaDataRootModel;
    }

    public void readFile(URL zipFile) throws PedroException, IOException, ParserConfigurationException, SAXException {
        URLConnection urlConnection = zipFile.openConnection();
        InputStream inputStream = urlConnection.getInputStream();
        String zipFileName = zipFile.getFile();
        readFile(zipFileName, inputStream);
    }

    public void readFile(File zipFile) throws PedroException, IOException, ParserConfigurationException, SAXException {
        String zipFileName = zipFile.getName();
        FileInputStream zipFileInputStream = new FileInputStream(zipFile);
        readFile(zipFileName, zipFileInputStream);
    }

    public void readFile(String zipFileName, InputStream inputStream) throws PedroException, IOException, ParserConfigurationException, SAXException {
        unzipNativeFormatFile(zipFileName, inputStream);
        parseNativeFormatFile(nativeFormatFile);
        if (metaDataFile.exists() == true) {
            try {
                parseMetaData(metaDataFile);
            } catch (Exception err) {
                System.out.println("Error reading meta data file");
            }
        }
        int numberOfTemporaryFiles = temporaryFiles.size();
        for (int i = 0; i < numberOfTemporaryFiles; i++) {
            File temporaryFile = (File) temporaryFiles.get(i);
            temporaryFile.delete();
        }
    }

    private void parseMetaData(File file) {
        PedroFormContext metaDataFormContext = (PedroFormContext) pedroFormContext.getApplicationProperty(PedroApplicationContext.META_DATA_FORM_CONTEXT);
        PedroDataFileReader metaDataFileReader = new PedroDataFileReader(metaDataFormContext, false);
        metaDataFileReader.omitModelStamp();
        try {
            FileInputStream fileInputStream = new FileInputStream(file);
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser p = factory.newSAXParser();
            p.parse(fileInputStream, metaDataFileReader);
            DocumentMetaDataConverter documentMetaDataConverter = new DocumentMetaDataConverter(metaDataFormContext);
            metaDataRootModel = metaDataFileReader.getRootModel();
            documentMetaData = documentMetaDataConverter.convertToPedroMetaDataStructures(file.getAbsolutePath(), metaDataRootModel);
        } catch (Exception e) {
            System.out.println("Native File Format Reader meta data reader error!!");
            documentMetaData = new DocumentMetaData();
        }
    }

    private void parseNativeFormatFile(File file) throws ParserConfigurationException, IOException, SAXException, PedroException {
        FileInputStream fileInputStream = new FileInputStream(file);
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            SAXParser p = factory.newSAXParser();
            p.parse(fileInputStream, pedroDataFileReader);
            rootModel = pedroDataFileReader.getRootModel();
        } catch (Exception e) {
            throw new PedroException(e.getLocalizedMessage());
        }
    }

    private void unzipNativeFormatFile(String zipFileName, InputStream inputStream) throws IOException, PedroException {
        int dotPosition = zipFileName.lastIndexOf(".");
        String fileRootName = zipFileName.substring(0, dotPosition);
        String nativeFileName = fileRootName + ".pdr";
        String metaDataFileName = fileRootName + ".meta";
        ZipInputStream zipIn = new ZipInputStream(inputStream);
        ZipEntry entry;
        boolean pdrFileFound = false;
        while ((entry = zipIn.getNextEntry()) != null) {
            String currentFileName = entry.getName();
            if (currentFileName.toUpperCase().endsWith(".PDR") == true) {
                nativeFormatFile = File.createTempFile("Pedro_PDR", "pdr");
                pdrFileFound = true;
                temporaryFiles.add(nativeFormatFile);
                writeTemporaryFile(nativeFormatFile, zipIn);
            } else if (currentFileName.toUpperCase().endsWith(".META") == true) {
                metaDataFile = File.createTempFile("Pedro_META", "meta");
                temporaryFiles.add(metaDataFile);
                writeTemporaryFile(metaDataFile, zipIn);
            }
        }
        zipIn.close();
        if (pdrFileFound == false) {
            String errorMessage = PedroResources.getMessage("io.experimentFileReader.badZipFile", zipFileName);
            throw new PedroException(errorMessage);
        }
    }

    private void writeTemporaryFile(File temporaryFile, ZipInputStream zipIn) throws IOException {
        int count = 0;
        byte data[] = new byte[BUFFER_SIZE];
        FileOutputStream fileOutputStream = new FileOutputStream(temporaryFile);
        BufferedOutputStream dest = new BufferedOutputStream(fileOutputStream, BUFFER_SIZE);
        while ((count = zipIn.read(data, 0, BUFFER_SIZE)) != -1) {
            dest.write(data, 0, count);
        }
        dest.flush();
        dest.close();
    }

    public void setIDGeneratorService(IDGeneratorService _idGeneratorService) {
        pedroDataFileReader.setIDGeneratorService(_idGeneratorService);
    }

    public void omitModelStamp() {
        pedroDataFileReader.omitModelStamp();
    }
}
