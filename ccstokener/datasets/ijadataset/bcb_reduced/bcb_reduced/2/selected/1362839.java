package projetosd.android.view;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HTTP;
import org.xml.sax.SAXException;
import projetosd.android.R;
import projetosd.android.domain.DatabaseManager;
import projetosd.android.domain.Form;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;

public class ViewDataActivity extends Activity {

    private static final String URL_SERVICE = "http://localhost:8080/ServletTeste/Teste";

    public int responseCode;

    public String responseMessage;

    public ArrayList<Form> forms;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.view_data);
        DatabaseManager dbm = new DatabaseManager(ViewDataActivity.this);
        SQLiteDatabase db = dbm.getReadableDatabase();
        forms = new ArrayList<Form>();
        Cursor cursor = db.query(dbm.getTableDados(), new String[] { dbm.getTableDadosFichaId(), dbm.getTableDadosResultado(), DatabaseManager.TABLE_DADOS_CREATED_DATE, DatabaseManager.TABLE_DADOS_NAME }, null, null, null, null, dbm.getTableDadosFichaId() + " desc");
        if (cursor.moveToFirst()) {
            do {
                Form form = new Form();
                form.setId(cursor.getString(0));
                form.setRepostasXML(cursor.getString(1));
                form.setCreationDate(cursor.getString(2));
                form.setFormName(cursor.getString(3));
                forms.add(form);
            } while (cursor.moveToNext());
        }
        if (cursor != null && !cursor.isClosed()) cursor.close();
        List<String> textos = new ArrayList<String>();
        for (Iterator iterator = forms.iterator(); iterator.hasNext(); ) {
            Form form = (Form) iterator.next();
            textos.add(form.getFormName() + " | " + form.getCreationDate());
        }
        ListView list = (ListView) findViewById(R.id.listSalvos);
        if (textos.isEmpty()) textos.add("Nenhum dado foi salvo.");
        list.setAdapter(new ArrayAdapter<String>(this, R.layout.item_list, textos));
        Button sendButton = (Button) findViewById(R.id.sendButton);
        sendButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                try {
                    for (Form form : forms) {
                        sendToServer(form.getId(), form.getRepostasXML());
                    }
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (ParserConfigurationException e) {
                    e.printStackTrace();
                } catch (SAXException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
        });
        Button backButton = (Button) findViewById(R.id.backButton);
        backButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                backToMain();
            }
        });
    }

    private void backToMain() {
        Intent mainIntent = new Intent(this, MainActivity.class);
        startActivity(mainIntent);
        finish();
    }

    private void sendToServer(String fichaID, String respostas) throws FileNotFoundException, ParserConfigurationException, SAXException, IOException, URISyntaxException {
        ArrayList params = new ArrayList();
        params.add(new BasicNameValuePair("xml", respostas));
        params.add(new BasicNameValuePair("idForm", fichaID));
        URI uri = URIUtils.createURI("http", "172.20.9.144", 8080, "/PSFServer/SaveAnswers", URLEncodedUtils.format(params, "UTF-8"), null);
        HttpPost request = new HttpPost(uri);
        request.setEntity(new UrlEncodedFormEntity(params, HTTP.UTF_8));
        HttpClient client = new DefaultHttpClient();
        HttpResponse httpResponse = client.execute(request);
        BufferedReader in = new BufferedReader(new InputStreamReader(httpResponse.getEntity().getContent()));
        StringBuffer sb = new StringBuffer("");
        String line = "";
        String NL = System.getProperty("line.separator");
        while ((line = in.readLine()) != null) {
            sb.append(line + NL);
        }
        in.close();
        String resposta = sb.toString();
        if (resposta != null || resposta != "") {
            new DatabaseManager(this).getWritableDatabase().execSQL("delete from " + DatabaseManager.getTableDados());
        }
        backToMain();
    }
}
