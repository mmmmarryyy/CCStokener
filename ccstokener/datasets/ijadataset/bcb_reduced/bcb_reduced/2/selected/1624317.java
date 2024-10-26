package open.gps.gopens.poi;

import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutionException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import open.gps.gopens.R;
import open.gps.gopens.render.model.RenderModel;
import open.gps.gopens.retrievers.utils.Position;
import open.gps.gopens.utils.Items;
import android.content.Context;
import android.util.Log;

/**
 * @author Camille Tardy
 */
public class POIRetrieverImpl implements POIRetriever {

    private List<String> checkedPois;

    private Position myloc;

    private URL url;

    private RenderModel model;

    private Context ctx;

    /**
	 * Contructor
	 * @param m The RenderModelImpl.
	 */
    public POIRetrieverImpl(RenderModel m) {
        model = m;
        ctx = m.getContext();
    }

    /**
	 * Retrieve the POI list.
	 * @param loc The central GPS location display on the map.
	 * @param poisCheck The list of poi type to retrieve.
	 * @throws Exception
	 */
    public void sendPOIGpx(Position loc, List<String> poisCheck) throws Exception {
        myloc = loc;
        checkedPois = poisCheck;
        url = null;
        double left = myloc.getY() - 0.025;
        double right = myloc.getY() + 0.025;
        double top = myloc.getX() + 0.03;
        double bottom = myloc.getX() - 0.03;
        String poiSelected = poisCheck.get(0);
        for (String string : checkedPois) {
            Log.e("CHECKED", string);
        }
        try {
            if (poiSelected.compareTo("None") == 0) {
                model.setPointsOfInterest(new Items());
            } else {
                url = new URL("http://www.informationfreeway.org/api/0.6/node[" + poiSelected + "=*][bbox=" + left + "," + bottom + "," + right + "," + top + "]");
                SAXParser pars = null;
                ParsePoiGpx gpxHandler = new ParsePoiGpx(checkedPois, ctx);
                pars = SAXParserFactory.newInstance().newSAXParser();
                pars.getXMLReader().setFeature("http://xml.org/sax/features/namespaces", true);
                pars.parse(url.openStream(), gpxHandler);
                Items pois = gpxHandler.getPOIResultsItems();
                Log.d("OSMparser", "number of POIs found :: " + pois.getLength());
                if (pois.getLength() == 0) {
                    throw new ExecutionException(new Exception());
                } else {
                    model.setPointsOfInterest(pois);
                }
            }
        } catch (IOException e) {
            throw new Exception(ctx.getString(R.string.ioException));
        } catch (ExecutionException e) {
            throw new Exception(ctx.getString(R.string.noPois));
        } catch (Exception e) {
            throw new Exception(ctx.getString(R.string.parserException));
        }
    }
}
