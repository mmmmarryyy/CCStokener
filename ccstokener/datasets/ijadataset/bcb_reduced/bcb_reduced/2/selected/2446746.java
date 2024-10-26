package artist;

import com.slychief.lastfmapi.Artist;
import com.slychief.lastfmapi.Image;
import com.slychief.lastfmapi.Lfm;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.URL;
import java.util.List;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;
import static org.junit.Assert.*;

/**
 *
 * @author Alexander Schindler
 */
public class GetInfo_Test {

    private Lfm lfm;

    /**
     * Constructs ...
     *
     */
    public GetInfo_Test() {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @After
    public void tearDown() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @Before
    public void setUp() {
        {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream("test/xmlfiles/artist/artist.getInfo.xml");
                JAXBContext jbc = JAXBContext.newInstance("com.slychief.lastfmapi");
                Unmarshaller u = jbc.createUnmarshaller();
                lfm = (Lfm) u.unmarshal(fis);
            } catch (FileNotFoundException ex) {
                Logger.getLogger(GetInfo_Test.class.getName()).log(Level.SEVERE, null, ex);
            } catch (JAXBException ex) {
                Logger.getLogger(GetInfo_Test.class.getName()).log(Level.SEVERE, null, ex);
            } finally {
                try {
                    fis.close();
                } catch (IOException ex) {
                    Logger.getLogger(GetInfo_Test.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
        }
    }

    @Test
    public void test_validate_artist_getInfo() {
        try {
            SchemaFactory factory = SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
            File schemaLocation = new File("tes.xsd");
            Schema schema = factory.newSchema(schemaLocation);
            Validator validator = schema.newValidator();
            URL url = new URL("http://ws.audioscrobbler.com/2.0/?method=artist.getinfo&artist=Cher&api_key=b25b959554ed76058ac220b7b2e0a026");
            InputStream inputStream = url.openStream();
            Source source = new StreamSource(inputStream);
            validator.validate(source);
        } catch (IOException ex) {
            Logger.getLogger(GetInfo_Test.class.getName()).log(Level.SEVERE, null, ex);
            assertFalse("File not found", true);
        } catch (SAXException ex) {
            Logger.getLogger(GetInfo_Test.class.getName()).log(Level.SEVERE, null, ex);
            assertFalse("Schema did not validate", true);
        }
        assertTrue(true);
    }

    @Test
    public void test_lfm_status() {
        assertEquals("check if ws-response status is 'ok'", "ok", lfm.getStatus());
    }

    @Test
    public void test_artist_name() {
        assertEquals("check if artist name is equal", "Cher", lfm.getArtist().getName());
    }

    @Test
    public void test_artist_mbid() {
        assertEquals("check if mbid name is equal", "bfcc6d75-a6a5-4bc6-8282-47aec8531818", lfm.getArtist().getMbid());
    }

    @Test
    public void test_artist_images_not_null() {
        assertNotNull("check if image reference is null", lfm.getArtist().getImage());
    }

    @Test
    public void test_artist_images_not_zero() {
        assertNotSame("check if there is at least one image", 0, lfm.getArtist().getImage().size());
    }

    @Test
    public void test_artist_imageSize_1() {
        List<Image> images = lfm.getArtist().getImage();
        assertEquals("check image size", "small", images.get(0).getSize());
    }

    @Test
    public void test_artist_imageSize_2() {
        List<Image> images = lfm.getArtist().getImage();
        assertEquals("check image size", "medium", images.get(1).getSize());
    }

    @Test
    public void test_artist_imageSize_3() {
        List<Image> images = lfm.getArtist().getImage();
        assertEquals("check image size", "large", images.get(2).getSize());
    }

    @Test
    public void test_artist_stats_listeners() {
        assertEquals("check listeners", new BigInteger("330144"), lfm.getArtist().getStats().getListeners());
    }

    @Test
    public void test_artist_stats_playcount() {
        assertEquals("check listeners", new String("2683019"), lfm.getArtist().getStats().getPlaycount());
    }

    @Test
    public void test_artist_similar_not_null() {
        assertNotNull("check if similars reference is null", lfm.getArtist().getSimilar());
    }

    @Test
    public void test_artist_similar_not_empty() {
        List<Artist> artists = lfm.getArtist().getSimilar().getArtist();
        assertNotSame("check if similars reference is not empty", 0, artists.size());
    }

    @Test
    public void test_artist_similar_all_instances() {
        List<Artist> artists = lfm.getArtist().getSimilar().getArtist();
        assertSame("check if similars reference is five", 5, artists.size());
    }

    @Test
    public void test_artist_similar_name_1() {
        List<Artist> artists = lfm.getArtist().getSimilar().getArtist();
        assertEquals("check fisrt similar", "Tina Turner", artists.get(0).getName());
    }

    @Test
    public void test_artist_similar_url_1() {
        List<Artist> artists = lfm.getArtist().getSimilar().getArtist();
        assertEquals("check fisrt similar", "http://www.last.fm/music/Tina+Turner", artists.get(0).getUrl());
    }

    @Test
    public void test_artist_bio_date() {
        assertEquals("check fisrt similar", "Fri, 5 Dec 2008 19:55:12 +0000", lfm.getArtist().getBio().getPublished());
    }
}
