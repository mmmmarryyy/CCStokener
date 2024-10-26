package de.objectcode.time4u.client.feeds.job;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.eclipse.jface.preference.IPreferenceStore;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import de.objectcode.time4u.client.feeds.FeedType;
import de.objectcode.time4u.client.feeds.FeedsPlugin;
import de.objectcode.time4u.store.Day;
import de.objectcode.time4u.store.ITodoStore;
import de.objectcode.time4u.store.Priority;
import de.objectcode.time4u.store.Todo;
import de.objectcode.time4u.store.meta.MetaValue;

public class RSSFeed extends BaseHttpFeed {

    long m_taskId;

    long m_projectId;

    public RSSFeed() {
    }

    public RSSFeed(IPreferenceStore store, String base) {
        super(store, base);
        m_projectId = store.getLong(base + "projectId");
        m_taskId = store.getLong(base + "taskId");
    }

    public FeedType getType() {
        return FeedType.RSS;
    }

    public long getProjectId() {
        return m_projectId;
    }

    public void setProjectId(long projectId) {
        m_projectId = projectId;
    }

    public long getTaskId() {
        return m_taskId;
    }

    public void setTaskId(long taskId) {
        m_taskId = taskId;
    }

    public void perform(ITodoStore todoStore) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(openConnection(m_url));
            document.normalize();
            NodeList items = document.getElementsByTagName("item");
            SimpleDateFormat format = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US);
            Map<String, Todo> todosByRef = new HashMap<String, Todo>();
            List<Todo> toStore = new ArrayList<Todo>();
            for (Todo todo : todoStore.getAllAssignedTodos(false)) {
                MetaValue type = todo.getMetaProperty(FEED_META_CATEGORY, FEED_META_TYPE);
                MetaValue url = todo.getMetaProperty(FEED_META_CATEGORY, FEED_META_URL);
                MetaValue reference = todo.getMetaProperty(FEED_META_CATEGORY, FEED_META_REFERENCE);
                if (type != null && url != null && reference != null && FeedType.RSS.toString().equals(type.getValue()) && m_url.equals(url.getValue())) {
                    todosByRef.put((String) reference.getValue(), todo);
                }
            }
            for (int i = 0; i < items.getLength(); i++) {
                Element item = (Element) items.item(i);
                NodeList titleList = item.getElementsByTagName("title");
                String title = "";
                if (titleList.getLength() > 0) {
                    title = titleList.item(0).getFirstChild().getNodeValue().trim();
                }
                NodeList linkList = item.getElementsByTagName("link");
                String link = "";
                if (linkList.getLength() > 0) {
                    link = linkList.item(0).getFirstChild().getNodeValue().trim();
                }
                NodeList dateList = item.getElementsByTagName("pubDate");
                Calendar calendar = Calendar.getInstance();
                if (dateList.getLength() > 0) {
                    calendar.setTime(format.parse(dateList.item(0).getFirstChild().getNodeValue().trim()));
                }
                Todo todo = todosByRef.get(link);
                if (todo == null) {
                    todo = new Todo();
                    todo.setProjectId(m_projectId);
                    todo.setTaskId(m_taskId);
                    todo.setDescription("");
                    todo.setPriority(Priority.NORMAL.getValue());
                    todo.setMetaProperty(FEED_META_CATEGORY, FEED_META_TYPE, new MetaValue(FeedType.RSS.toString()));
                    todo.setMetaProperty(FEED_META_CATEGORY, FEED_META_URL, new MetaValue(m_url));
                    todo.setMetaProperty(FEED_META_CATEGORY, FEED_META_REFERENCE, new MetaValue(link));
                } else {
                    todosByRef.remove(link);
                }
                todo.setCompleted(false);
                todo.setCompletedAt(null);
                todo.setCreatedOn(new Day(calendar));
                todo.setHeader(title);
                toStore.add(todo);
            }
            for (Todo todo : todosByRef.values()) {
                todo.setCompleted(true);
                todo.setCompletedAt(new Day(Calendar.getInstance()));
                toStore.add(todo);
            }
            todoStore.storeTodos(toStore);
        } catch (Exception e) {
            FeedsPlugin.getDefault().log(e);
        } finally {
            closeConnection();
        }
    }

    @Override
    public void store(IPreferenceStore store, String base) {
        super.store(store, base);
        store.setValue(base + "projectId", m_projectId);
        store.setValue(base + "taskId", m_taskId);
    }
}
