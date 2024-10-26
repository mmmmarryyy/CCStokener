package pedro.io;

import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
import pedro.mda.config.PedroConfigurationReader;
import pedro.mda.model.RecordModel;
import pedro.mda.model.RecordModelFactory;
import pedro.soa.id.IDGeneratorService;
import pedro.system.*;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class XMLSubmissionFileReader extends DefaultHandler {

    private RecordModel rootModel;

    private PedroDataFileReader pedroDataFileReader;

    public XMLSubmissionFileReader(PedroFormContext pedroFormContext, boolean readTemplate) {
        pedroDataFileReader = new PedroDataFileReader(pedroFormContext, readTemplate);
    }

    /**
	* @return root record model for the document
	*/
    public RecordModel getRootModel() {
        return rootModel;
    }

    public void readFile(InputStream inputStream) throws PedroException, IOException, ParserConfigurationException, SAXException {
        read(inputStream);
    }

    public void readFile(File xmlFile) throws PedroException, IOException, ParserConfigurationException, SAXException {
        FileInputStream fileInputStream = new FileInputStream(xmlFile);
        read(fileInputStream);
    }

    public void readFile(URL url) throws PedroException, IOException, ParserConfigurationException, SAXException {
        URLConnection urlConnection = url.openConnection();
        InputStream inputStream = urlConnection.getInputStream();
        read(inputStream);
    }

    private void read(InputStream inputStream) throws PedroException, IOException, ParserConfigurationException, SAXException {
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser p = factory.newSAXParser();
        p.parse(inputStream, pedroDataFileReader);
        rootModel = pedroDataFileReader.getRootModel();
    }

    /**
	* means Pedro will ignore whether the model stamp in the file matches the model
	* stamp supported in the application
	*/
    public void omitModelStamp() {
        pedroDataFileReader.omitModelStamp();
    }

    /**
	* sets an id generator service that generates values for identifier fields
	*/
    public void setIDGeneratorService(IDGeneratorService _idGeneratorService) {
        pedroDataFileReader.setIDGeneratorService(_idGeneratorService);
    }
}
