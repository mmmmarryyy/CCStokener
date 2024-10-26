package com.google.cdiscount.jayelco;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.ByteArrayBuffer;
import org.xml.sax.SAXException;

public class WebServiceData {

    public static Product myProduct;

    public static ArrayList<Product> myProducts;

    public Product GetData(String bareCode) throws ClientProtocolException, IOException, ParserConfigurationException, SAXException {
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost("http://djey01.fourretout.net/Service1.asmx/GetProduct");
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
        nameValuePairs.add(new BasicNameValuePair("bareCode", bareCode));
        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
        HttpResponse response = httpclient.execute(httppost);
        InputStream is = response.getEntity().getContent();
        BufferedInputStream bis = new BufferedInputStream(is);
        ByteArrayBuffer baf = new ByteArrayBuffer(20);
        int current = 0;
        while ((current = bis.read()) != -1) {
            baf.append((byte) current);
        }
        String text = new String(baf.toByteArray());
        ByteArrayInputStream xmlParseInputStream = new ByteArrayInputStream(text.getBytes());
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parseur = factory.newSAXParser();
        myProduct = new Product();
        ProductHandler productHandler = new ProductHandler();
        parseur.parse(xmlParseInputStream, productHandler);
        return productHandler.myProduct;
    }

    public ArrayList<Product> GetProducts(String txtProduct) throws ClientProtocolException, IOException, ParserConfigurationException, SAXException {
        HttpClient httpclient = new DefaultHttpClient();
        HttpPost httppost = new HttpPost("http://djey01.fourretout.net/Service1.asmx/GetProducts");
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
        nameValuePairs.add(new BasicNameValuePair("txtSearch", "tele"));
        httppost.setEntity(new UrlEncodedFormEntity(nameValuePairs));
        HttpResponse response = httpclient.execute(httppost);
        InputStream is = response.getEntity().getContent();
        BufferedInputStream bis = new BufferedInputStream(is);
        ByteArrayBuffer baf = new ByteArrayBuffer(20);
        int current = 0;
        while ((current = bis.read()) != -1) {
            baf.append((byte) current);
        }
        String text = new String(baf.toByteArray());
        ByteArrayInputStream xmlParseInputStream = new ByteArrayInputStream(text.getBytes());
        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser parseur = factory.newSAXParser();
        ProductHandler productHandler = new ProductHandler();
        parseur.parse(xmlParseInputStream, productHandler);
        return productHandler.myProducts;
    }
}
