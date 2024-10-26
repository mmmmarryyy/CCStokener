package tr.net.ems.Market;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;

public class Ornek extends ListActivity {

    private Intent i;

    private ListView listView_xml;

    private ArrayList<String> xmlList = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.market2);
        i = new Intent();
        i.setClassName("tr.net.ems.Market", "tr.net.ems.Market.SepetimActivity");
        listView_xml = (ListView) findViewById(R.id.list);
        xmlList = getListFromXml();
        ClickListener();
    }

    private ArrayList<String> getListFromXml() {
        ArrayList<String> list = new ArrayList<String>();
        try {
            URL url = new URL("http://www.ems.net.tr/aurunlerkisa.xml");
            DocumentBuilderFactory dFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dFactory.newDocumentBuilder();
            Document document = dBuilder.parse(new InputSource(url.openStream()));
            document.getDocumentElement().normalize();
            NodeList nodeListCountry = document.getElementsByTagName("MerchantItem");
            String urunsayi = Integer.toString(nodeListCountry.getLength());
            for (int i = 0; i < 100; i++) {
                Log.d("urunsayi", urunsayi);
                Node node = nodeListCountry.item(i);
                Element elementMain = (Element) node;
                NodeList nodeListText = elementMain.getElementsByTagName("itemTitle");
                Element elementText = (Element) nodeListText.item(0);
                NodeList nodeListValue = elementMain.getElementsByTagName("kdv_dahil");
                Element elementValue = (Element) nodeListValue.item(0);
                list.add(elementText.getChildNodes().item(0).getNodeValue() + "--->" + elementValue.getChildNodes().item(0).getNodeValue());
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return list;
    }

    @Override
    protected void onStart() {
        super.onStart();
        ArrayAdapter<String> araAdapter = new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_multiple_choice, xmlList);
        listView_xml.setAdapter(araAdapter);
    }

    private void ClickListener() {
        listView_xml.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> av, View v, int pos, long id) {
                onListItemClick(listView_xml, v, pos, id);
            }
        });
    }
}
