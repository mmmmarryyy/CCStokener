package projetosd.android.view;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.xml.sax.SAXException;
import projetosd.android.R;
import projetosd.android.parser.XMLFormParser;
import android.app.Activity;
import android.content.Intent;
import android.net.ParseException;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

public class NewFormActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.new_form);
        Button createFormButton = (Button) findViewById(R.id.createFormButton);
        createFormButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                try {
                    createForm();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (ParserConfigurationException e) {
                    e.printStackTrace();
                } catch (SAXException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void createForm() throws FileNotFoundException, ParserConfigurationException, SAXException, IOException {
        Intent createForm = new Intent(this, DynamicForm.class);
        EditText urlInput = (EditText) findViewById(R.id.editTextURL);
        String fichaID = "";
        try {
            String id = getID(urlInput.getText().toString());
            if (id != "") {
                fichaID = id;
            }
        } catch (Exception e) {
            return;
        }
        DynamicForm.fichaIdToSave = fichaID;
        InputStream xml = null;
        if (fichaID != "") {
            if (getXML(urlInput.getText().toString()) == "null\n") return;
            xml = new ByteArrayInputStream(getXML(urlInput.getText().toString()).getBytes("UTF-8"));
        }
        if (xml == null || xml.equals("")) xml = getAssets().open(urlInput.getText().toString());
        DynamicForm.parser = new XMLFormParser(xml);
        startActivity(createForm);
        finish();
    }

    private String getID(String url) {
        if (url.contains("id=")) {
            return url.substring(url.indexOf("=") + 1, url.length());
        }
        return "";
    }

    private String getXML(String url) throws ClientProtocolException, IOException {
        HttpClient client = new DefaultHttpClient();
        HttpGet get = new HttpGet(url);
        HttpResponse responseGet = client.execute(get);
        HttpEntity resEntityGet = responseGet.getEntity();
        BufferedReader in = new BufferedReader(new InputStreamReader(resEntityGet.getContent()));
        StringBuffer sb = new StringBuffer("");
        String line = "";
        String NL = System.getProperty("line.separator");
        while ((line = in.readLine()) != null) {
            sb.append(line + NL);
        }
        in.close();
        String xml = sb.toString();
        return xml;
    }
}
