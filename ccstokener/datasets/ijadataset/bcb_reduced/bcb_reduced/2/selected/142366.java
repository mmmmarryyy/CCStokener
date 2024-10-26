package cards.model.acceptTests;

import java.net.URL;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import cards.model.StoryCardModel;

public class FitnesseTestStatisticsProvider extends TestStatisticsProvider {

    @Override
    public void loadTest(StoryCardModel story) {
        String strUrl = story.getStoryCard().getAcceptanceTestUrl();
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder loader;
        try {
            URL url = new URL(strUrl);
            loader = factory.newDocumentBuilder();
            Document document;
            document = loader.parse(url.openStream());
            this.numPass = Integer.parseInt(((Element) document.getElementsByTagName("num-pass").item(0)).getFirstChild().getNodeValue());
            this.numFail = Integer.parseInt(((Element) document.getElementsByTagName("num-fail").item(0)).getFirstChild().getNodeValue());
            this.numRuns = Integer.parseInt(((Element) document.getElementsByTagName("num-runs").item(0)).getFirstChild().getNodeValue());
            this.numExceptions = Integer.parseInt(((Element) document.getElementsByTagName("num-exceptions").item(0)).getFirstChild().getNodeValue());
            this.wikiText = ((Element) document.getElementsByTagName("wiki").item(0)).getFirstChild().getNodeValue();
        } catch (Exception e) {
            util.Logger.singleton().error(e);
        }
    }
}
