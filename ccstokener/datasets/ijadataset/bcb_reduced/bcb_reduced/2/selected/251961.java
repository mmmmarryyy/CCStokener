package edu.tsinghua.eea.powermanagement.control;

import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Vector;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import edu.tsinghua.eea.powermanagement.R;
import edu.tsinghua.eea.powermanagement.data.PUData;
import edu.tsinghua.eea.powermanagement.data.PlugInfoCell;
import edu.tsinghua.eea.powermanagement.data.XMLData;
import edu.tsinghua.eea.powermanagement.data.XMLPowerData;
import edu.tsinghua.eea.powermanagement.data.XMLPowerDataCell;
import edu.tsinghua.eea.powermanagement.enums.PlugStatusEnums.CFLAG;
import edu.tsinghua.eea.powermanagement.enums.PlugStatusEnums.SFLAG;
import edu.tsinghua.eea.powermanagement.gui.StatusActivity;
import edu.tsinghua.eea.powermanagement.plot.BarCompareView;
import edu.tsinghua.eea.powermanagement.plot.PlotView;
import android.R.integer;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class XmlPlugLoader extends AsyncTask<String, Integer, Vector<PlugInfoCell>> {

    private ProgressDialog mProgDiag;

    private TextView mMessage;

    private ArrayList<HashMap<String, Object>> mPlugItems;

    private SimpleAdapter mPlugItemAdapter;

    private Context mContext;

    public XmlPlugLoader(Context context, TextView msg, ArrayList<HashMap<String, Object>> pi, SimpleAdapter sa) {
        mContext = context;
        mMessage = msg;
        mProgDiag = new ProgressDialog(context, 0);
        mPlugItemAdapter = sa;
        mProgDiag.setCancelable(true);
        mProgDiag.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgDiag.setMessage("Loading XML...");
        mProgDiag.show();
        mPlugItems = pi;
    }

    @Override
    protected Vector<PlugInfoCell> doInBackground(String... params) {
        try {
            HttpClient client = new DefaultHttpClient();
            HttpGet get = new HttpGet(params[0]);
            HttpResponse response = client.execute(get);
            HttpEntity entity = response.getEntity();
            long length = entity.getContentLength();
            InputStream is = entity.getContent();
            DocumentBuilderFactory docBuilderFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docBuilderFactory.newDocumentBuilder();
            Document doc = docBuilder.parse(is);
            NodeList nlRoot = doc.getElementsByTagName("power");
            return xmlToData(doc);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onPostExecute(Vector<PlugInfoCell> result) {
        super.onPostExecute(result);
        mProgDiag.dismiss();
        if (result == null) {
            Log.e("XmlData", "Xml data is null!");
            return;
        }
        addMessage("Data size:" + result.size() + "\n");
        mPlugItems.clear();
        refreshPlugData(result);
        mPlugItemAdapter.notifyDataSetChanged();
    }

    private void addMessage(String s) {
    }

    private Vector<PlugInfoCell> xmlToData(Document doc) {
        Vector<PlugInfoCell> va = new Vector<PlugInfoCell>();
        NodeList nlPlug = doc.getElementsByTagName("plug-detail");
        if (nlPlug.getLength() > 0) {
            NodeList nlCells = nlPlug.item(0).getChildNodes();
            for (int i = 0; i < nlCells.getLength(); ++i) {
                Node n = nlCells.item(2 * i + 1);
                if (n == null) break;
                NamedNodeMap nAttr = n.getAttributes();
                Log.d("XmlCell", "Cell node:" + i + " data:" + n.getTextContent());
                if (nAttr == null) {
                    Log.d("XmlCell", "Cell node:" + i + " attribute is null!");
                    continue;
                }
                for (int k = 0; k < nAttr.getLength(); ++k) Log.d("XmlCell", "i=" + i + ", attr name=" + nAttr.item(k).getNodeName() + ", attr value=" + nAttr.item(k).getNodeValue());
                String sID = nAttr.getNamedItem("id").getNodeValue();
                String sMAC = nAttr.getNamedItem("mac").getNodeValue();
                String sEID = nAttr.getNamedItem("eid").getNodeValue();
                String sSF = nAttr.getNamedItem("sflag").getNodeValue();
                String sCF = nAttr.getNamedItem("cflag").getNodeValue();
                int iID = Integer.parseInt(sID);
                int iEID = Integer.parseInt(sEID);
                int iSF = Integer.parseInt(sSF);
                int iCF = Integer.parseInt(sCF);
                SFLAG cSF = SFLAG.get(iSF);
                CFLAG cCF = CFLAG.get(iCF);
                PlugInfoCell cell = new PlugInfoCell(iID, sMAC, iEID, cSF, cCF);
                va.add(cell);
            }
        } else {
        }
        return va;
    }

    private void refreshPlugData(Vector<PlugInfoCell> result) {
        Iterator<PlugInfoCell> it = result.iterator();
        while (it.hasNext()) {
            PlugInfoCell cell = it.next();
            HashMap<String, Object> hItem = new HashMap<String, Object>();
            hItem.put(StatusActivity.MENU_NAME_TITLE, cell.mMAC);
            if (cell.mSFlag == SFLAG.NotAvailable) hItem.put(StatusActivity.MENU_NAME_TEXT, "ID:" + cell.mID + ", " + mContext.getString(R.string.na)); else if (cell.mCFlag == CFLAG.Open) hItem.put(StatusActivity.MENU_NAME_TEXT, "ID:" + cell.mID + ", " + mContext.getString(R.string.open_action_performing)); else if (cell.mCFlag == CFLAG.Close) hItem.put(StatusActivity.MENU_NAME_TEXT, "ID:" + cell.mID + ", " + mContext.getString(R.string.close_action_performing));
            if (cell.mSFlag == SFLAG.Open) hItem.put(StatusActivity.MENU_NAME_STATUS, true); else if (cell.mSFlag == SFLAG.Close) hItem.put(StatusActivity.MENU_NAME_STATUS, false); else hItem.put(StatusActivity.MENU_NAME_STATUS, "N/A");
            hItem.put(StatusActivity.PLUG_ID, cell.mID);
            hItem.put(StatusActivity.PLUG_EID, cell.mEID);
            mPlugItems.add(hItem);
        }
    }
}
