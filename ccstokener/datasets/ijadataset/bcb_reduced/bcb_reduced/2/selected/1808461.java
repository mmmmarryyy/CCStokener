package net.xm;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathFactory;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import sun.misc.BASE64Encoder;

public class BasecampTodoListManager extends AbstractTodoListManager implements TodoListManager {

    public BasecampTodoListManager(Configuration conf) throws ApplicationException {
        try {
            this.configuration = conf;
            props.put("http.proxyHost", conf.getProxyHost());
            props.put("http.proxyPort", conf.getProxyPort());
            xpathFactory = XPathFactory.newInstance();
            docFactory = DocumentBuilderFactory.newInstance();
            builder = docFactory.newDocumentBuilder();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
            throw new ApplicationException(e);
        }
    }

    public TodoList addItem(TodoListItem item) throws ApplicationException {
        post(configuration.getBasecampUrl() + "todos/create_item/" + configuration.getBasecampListId(), item.getContent());
        return getAllItems();
    }

    public TodoList completeItem(TodoListItem item) throws ApplicationException {
        post(configuration.getBasecampUrl() + "todos/complete_item/" + item.getId(), null);
        return getAllItems();
    }

    public TodoList deleteItem(TodoListItem item) throws ApplicationException {
        post(configuration.getBasecampUrl() + "todos/delete_item/" + item.getId(), null);
        return getAllItems();
    }

    public TodoList uncompleteItem(TodoListItem item) throws ApplicationException {
        post(configuration.getBasecampUrl() + "todos/uncomplete_item/" + item.getId(), null);
        return getAllItems();
    }

    public TodoList getAllItems() throws ApplicationException {
        Document doc = post(configuration.getBasecampUrl() + "todos/list/" + configuration.getBasecampListId(), null);
        return rebuildList(doc);
    }

    protected Document post(String location, String content) throws ApplicationException {
        Document doc = null;
        try {
            URL url = new URL(location);
            String encoding = new BASE64Encoder().encode(configuration.getBasecampPassword().getBytes());
            HttpURLConnection uc = (HttpURLConnection) url.openConnection();
            uc.setRequestProperty("Authorization", "Basic " + encoding);
            uc.setRequestProperty("Accept", "application/xml");
            uc.setRequestProperty("Content-Type", "application/xml");
            uc.setRequestProperty("X-POST_DATA_FORMAT", "xml");
            uc.setDoOutput(true);
            OutputStreamWriter out = new OutputStreamWriter(uc.getOutputStream());
            out.write("<request><content>" + content + "</content></request>");
            out.close();
            doc = XmlUtils.readDocumentFromInputStream(uc.getInputStream());
            System.out.println("result: " + XmlUtils.toString(doc));
        } catch (IOException e) {
            e.printStackTrace();
            throw new ApplicationException(e);
        }
        return doc;
    }

    private TodoList rebuildList(Document doc) throws ApplicationException {
        TodoList resultList = new TodoList();
        try {
            NodeList list = XmlUtils.selectNodes(doc, "/todo-list/todo-items/todo-item");
            for (int i = 0; i < list.getLength(); i++) {
                Node node = list.item(i);
                String content = XmlUtils.extractSingleElementValue(node, "content");
                TodoListItem item = new TodoListItem(content);
                item.setId(XmlUtils.extractSingleElementValue(node, "id"));
                item.setCompleted(Boolean.valueOf(XmlUtils.extractSingleElementValue(node, "completed")));
                resultList.add(item);
            }
        } catch (DOMException e) {
            throw new ApplicationException(e);
        }
        return resultList;
    }

    public TodoListItem getItem(String id) {
        return null;
    }
}
