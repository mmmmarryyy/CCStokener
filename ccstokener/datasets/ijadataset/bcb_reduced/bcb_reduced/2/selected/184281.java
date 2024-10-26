package com.vincenzofehring.ldsscriptures;

import java.net.URL;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.xml.sax.InputSource;
import org.xml.sax.XMLReader;
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class scriptures extends Activity {

    private final String MY_DEBUG_TAG = "WeatherForcaster";

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);
        TextView tv = new TextView(this);
        try {
            URL url = new URL("");
            SAXParserFactory spf = SAXParserFactory.newInstance();
            SAXParser sp = spf.newSAXParser();
            XMLReader xr = sp.getXMLReader();
            ExampleHandler myExampleHandler = new ExampleHandler();
            xr.setContentHandler(myExampleHandler);
            xr.parse(new InputSource(url.openStream()));
            ParsedExampleDataSet parsedExampleDataSet = myExampleHandler.getParsedData();
            tv.setText(parsedExampleDataSet.toString());
        } catch (Exception e) {
            tv.setText("Error: " + e.getMessage());
            Log.e(MY_DEBUG_TAG, "WeatherQueryError", e);
        }
        this.setContentView(tv);
    }
}
