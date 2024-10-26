package com.snakoid.droidlibrary.classes;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import javax.xml.parsers.*;
import org.w3c.dom.*;
import org.xml.sax.*;
import org.xmlpull.v1.XmlSerializer;
import android.util.*;

public class XMLGetter {

    private URL url;

    private DocumentBuilderFactory dbf;

    private DocumentBuilder db;

    private Document dom;

    private Element root;

    private ArrayList<Book> books;

    private String action;

    public XMLGetter() {
    }

    public boolean ReadData() {
        Log.d("snakoid", "start reading config");
        DocumentBuilderFactory dbf;
        DocumentBuilder db;
        Document dom;
        Element root;
        NodeList bookList;
        Node book;
        NamedNodeMap attrs;
        Node attr;
        NodeList bookChildList;
        Node bookChild;
        Book _b;
        try {
            dbf = DocumentBuilderFactory.newInstance();
            db = dbf.newDocumentBuilder();
            dom = db.parse(new File(Tools.configFile));
            root = dom.getDocumentElement();
            bookList = root.getElementsByTagName("book");
            for (int i = 0; i < bookList.getLength(); i++) {
                book = bookList.item(i);
                _b = new Book();
                bookChildList = book.getChildNodes();
                for (int j = 0; j < bookChildList.getLength(); j++) {
                    bookChild = bookChildList.item(j);
                    if (bookChild.getNodeName().equals("imageurl")) {
                        _b.setImageUrl(ValueOrNull(bookChild.getTextContent()));
                        attrs = bookChild.getAttributes();
                        for (int m = 0; m < attrs.getLength(); m++) {
                            attr = attrs.item(m);
                            if (attr.getNodeName().equals("width")) _b.setImageWidth(ValueOrNull(attr.getNodeValue())); else if (attr.getNodeName().equals("height")) _b.setImageHeight(ValueOrNull(attr.getNodeValue()));
                        }
                    } else if (bookChild.getNodeName().equals("mediumimageurl")) _b.setMediumImageUrl(ValueOrNull(bookChild.getTextContent())); else if (bookChild.getNodeName().equals("isbn")) _b.setISBN(ValueOrNull(bookChild.getTextContent())); else if (bookChild.getNodeName().equals("pagecount")) _b.setPageCount(ValueOrNull(bookChild.getTextContent())); else if (bookChild.getNodeName().equals("pubdate")) _b.setPubDate(ValueOrNull(bookChild.getTextContent())); else if (bookChild.getNodeName().equals("publisher")) _b.setPublisher(ValueOrNull(bookChild.getTextContent())); else if (bookChild.getNodeName().equals("studio")) _b.setStudio(ValueOrNull(bookChild.getTextContent())); else if (bookChild.getNodeName().equals("title")) _b.setTitle(ValueOrNull(bookChild.getTextContent())); else if (bookChild.getNodeName().equals("url")) _b.setUrl(ValueOrNull(bookChild.getTextContent())); else if (bookChild.getNodeName().equals("authors")) _b.addAuthor(ValueOrNull(bookChild.getTextContent()));
                }
                Tools.AddBook2Library(_b);
            }
            Log.d("snakoid", "finish reading config");
            return true;
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            Log.d("snakoid", "error during reading config");
        } catch (SAXException e) {
            e.printStackTrace();
            Log.d("snakoid", "error during reading config");
        } catch (IOException e) {
            e.printStackTrace();
            Log.d("snakoid", "error during reading config");
        }
        return false;
    }

    public boolean WriteData() {
        Log.d("snakoid", "start writing data");
        boolean error = false;
        try {
            Tools.CreateBackupOfDataFile(Tools.configFile, Tools.configFileBackup);
            XmlSerializer serializer = Xml.newSerializer();
            FileOutputStream fos = new FileOutputStream(Tools.configFile, false);
            serializer.setOutput(fos, "utf-8");
            serializer.startDocument("utf-8", Boolean.valueOf(true));
            serializer.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            serializer.startTag(null, "droidlibrary");
            for (Book b : Tools.library) {
                serializer.startTag(null, "book");
                serializer.startTag(null, "isbn");
                if (b.getISBN() != null) serializer.text(b.getISBN());
                serializer.endTag(null, "isbn");
                serializer.startTag(null, "title");
                if (b.getTitle() != null) serializer.text(b.getTitle());
                serializer.endTag(null, "title");
                serializer.startTag(null, "pagecount");
                if (b.getPageCount() != null) serializer.text(b.getPageCount());
                serializer.endTag(null, "pagecount");
                serializer.startTag(null, "publisher");
                if (b.getPublisher() != null) serializer.text(b.getPublisher());
                serializer.endTag(null, "publisher");
                serializer.startTag(null, "studio");
                if (b.getStudio() != null) serializer.text(b.getStudio());
                serializer.endTag(null, "studio");
                serializer.startTag(null, "url");
                if (b.getUrl() != null) serializer.text(b.getUrl());
                serializer.endTag(null, "url");
                serializer.startTag(null, "imageurl");
                if ((b.getImageWidth() != null) && (b.getImageHeight() != null)) {
                    serializer.attribute(null, "width", b.getImageWidth());
                    serializer.attribute(null, "height", b.getImageHeight());
                }
                if (b.getImageUrl() != null) serializer.text(b.getImageUrl());
                serializer.endTag(null, "imageurl");
                serializer.startTag(null, "mediumimageurl");
                if (b.getMediumImageUrl() != null) serializer.text(b.getMediumImageUrl());
                serializer.endTag(null, "mediumimageurl");
                serializer.startTag(null, "pubdate");
                if (b.getPubDate() != null) serializer.text(b.getPubDate());
                serializer.endTag(null, "pubdate");
                serializer.startTag(null, "authors");
                if (b.getAuthorsString() != null) serializer.text(b.getAuthorsString());
                serializer.endTag(null, "authors");
                serializer.endTag(null, "book");
            }
            serializer.endTag(null, "droidlibrary");
            serializer.endDocument();
            serializer.flush();
            fos.close();
            Log.d("snakoid", "finish writing data");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            error = true;
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            error = true;
        } catch (IllegalStateException e) {
            e.printStackTrace();
            error = true;
        } catch (IOException e) {
            e.printStackTrace();
            error = true;
        }
        if (error) {
            Log.d("snakoid", "error during writing data");
            Tools.CreateBackupOfDataFile(Tools.configFileBackup, Tools.configFile);
            return false;
        } else return true;
    }

    @SuppressWarnings("unused")
    public String Parse(String _isbn, String _action) {
        Log.d("snakoid", "start parsing for isbn :" + _isbn);
        action = _action;
        try {
            action = _action;
            url = new URL(Tools.GetRequestUrl(_isbn));
            dbf = DocumentBuilderFactory.newInstance();
            db = dbf.newDocumentBuilder();
            dom = db.parse(url.openConnection().getInputStream());
            root = dom.getDocumentElement();
            NodeList nodeList1, nodeList2, nodeList3, tempsdfg;
            Node node1, node2, node3;
            Book b;
            books = new ArrayList<Book>();
            tempsdfg = root.getElementsByTagName("Error");
            if (root.getElementsByTagName("Error").getLength() != 0) {
                Log.d("snakoid", "parsing has returned an error :");
                nodeList1 = root.getElementsByTagName("Error");
                for (int i = 0; i < nodeList1.getLength(); i++) {
                    node1 = nodeList1.item(i);
                    nodeList2 = node1.getChildNodes();
                    for (int j = 0; j < nodeList2.getLength(); j++) {
                        node2 = nodeList2.item(j);
                        if (node2 != null && node2.getNodeName().equals("Code")) Log.d("snakoid", "[" + node2.getChildNodes().item(0).getNodeValue() + "]");
                        return "error : " + node2.getChildNodes().item(0).getNodeValue();
                    }
                }
            } else if (root.getElementsByTagName("Items").getLength() != 0) {
                nodeList1 = root.getElementsByTagName("Item");
                for (int i = 0; i < nodeList1.getLength(); i++) {
                    b = new Book();
                    node1 = nodeList1.item(i);
                    nodeList2 = node1.getChildNodes();
                    for (int j = 0; j < nodeList2.getLength(); j++) {
                        node2 = nodeList2.item(j);
                        if (node2 != null && node2.getNodeName().equals("DetailPageURL")) {
                            Node temp = node2.getChildNodes().item(0);
                            String temp2 = temp.getNodeValue();
                            b.setUrl(ValueOrNull(temp2));
                        } else if (node2 != null && node2.getNodeName().equals("SmallImage")) {
                            nodeList3 = node2.getChildNodes();
                            for (int m = 0; m < nodeList3.getLength(); m++) {
                                node3 = nodeList3.item(m);
                                if (node3 != null && node3.getNodeName().equals("URL")) {
                                    b.setImageUrl(ValueOrNull(node3.getChildNodes().item(0).getNodeValue()));
                                    if (b.getImageUrl() != null) b.setImageUrl(Tools.DownloadFile(b.getImageUrl(), Tools.folderThumbsPath));
                                } else if (node3 != null && node3.getNodeName().equals("Width")) b.setImageWidth(ValueOrNull(node3.getChildNodes().item(0).getNodeValue())); else if (node3 != null && node3.getNodeName().equals("Height")) b.setImageHeight(ValueOrNull(node3.getChildNodes().item(0).getNodeValue()));
                            }
                        } else if (node2 != null && node2.getNodeName().equals("MediumImage")) {
                            nodeList3 = node2.getChildNodes();
                            for (int m = 0; m < nodeList3.getLength(); m++) {
                                node3 = nodeList3.item(m);
                                if (node3 != null && node3.getNodeName().equals("URL")) {
                                    b.setMediumImageUrl(ValueOrNull(node3.getChildNodes().item(0).getNodeValue()));
                                    if (b.getMediumImageUrl() != null) b.setMediumImageUrl(Tools.DownloadFile(b.getMediumImageUrl(), Tools.folderThumbsPath));
                                }
                            }
                        } else if (node2 != null && node2.getNodeName().equals("ItemAttributes")) {
                            nodeList3 = node2.getChildNodes();
                            for (int m = 0; m < nodeList3.getLength(); m++) {
                                node3 = nodeList3.item(m);
                                if (node3 != null && node3.getNodeName().equals("Title")) b.setTitle(ValueOrNull(node3.getChildNodes().item(0).getNodeValue())); else if (node3 != null && node3.getNodeName().equals("Publisher")) b.setPublisher(ValueOrNull(node3.getChildNodes().item(0).getNodeValue())); else if (node3 != null && node3.getNodeName().equals("Studio")) b.setStudio(ValueOrNull(node3.getChildNodes().item(0).getNodeValue())); else if (node3 != null && node3.getNodeName().equals("PublicationDate")) b.setPubDate(ValueOrNull(node3.getChildNodes().item(0).getNodeValue())); else if (node3 != null && node3.getNodeName().equals("NumberOfPages")) b.setPageCount(ValueOrNull(node3.getChildNodes().item(0).getNodeValue())); else if (node3 != null && (node3.getNodeName().equals("Author") || node3.getNodeName().equals("Creator"))) b.addAuthor(ValueOrNull(node3.getChildNodes().item(0).getNodeValue())); else if (node3 != null && node3.getNodeName().equals("EAN")) b.setISBN(ValueOrNull(node3.getChildNodes().item(0).getNodeValue()));
                            }
                        }
                    }
                    books.add(b);
                }
                if (action.equals("request")) Tools.AddBooks2Temp(books);
                Log.d("snakoid", "finish parsing");
                return "parsed";
            }
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            Log.d("snakoid", "parsing error : ParserConfigurationException");
        } catch (SAXException e) {
            e.printStackTrace();
            Log.d("snakoid", "parsing error : SAXException");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "nothing done";
    }

    private String ValueOrNull(String _value) {
        if (_value == null || _value.equals("")) return null; else return _value;
    }
}
