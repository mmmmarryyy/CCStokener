package org.columba.calendar.store;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.columba.calendar.model.ComponentInfoList;
import org.columba.calendar.model.api.IComponentInfo;
import org.columba.calendar.model.api.IComponentInfoList;
import org.columba.calendar.model.api.IEventInfo;
import org.columba.calendar.parser.CalendarImporter;
import org.columba.calendar.store.api.StoreException;

public class WebICalCalendarStore extends AbstractCalendarStore {

    Map<String, IEventInfo> map;

    Map<String, String> contactEventMap;

    String id;

    URL url;

    public WebICalCalendarStore(String id, String stringUrl) {
        this.id = id;
        map = new HashMap<String, IEventInfo>();
        url = null;
        try {
            url = new URL(stringUrl);
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return;
        }
    }

    public void initActivities() throws StoreException {
        map = new HashMap<String, IEventInfo>();
        URLConnection connection;
        try {
            connection = url.openConnection();
            Iterator<IEventInfo> it = new CalendarImporter().importCalendar(id, connection.getInputStream());
            while (it.hasNext()) {
                IEventInfo eventinfo = it.next();
                map.put(eventinfo.getId(), eventinfo);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.initActivities();
    }

    public String getId() {
        return id;
    }

    public void add(IComponentInfo basicModel) throws StoreException {
        throw new StoreException("read only");
    }

    public boolean exists(Object id) throws StoreException {
        return map.containsKey(id);
    }

    public IComponentInfo get(Object id) throws StoreException {
        return map.get(id);
    }

    public void modify(Object id, IComponentInfo basicModel) throws StoreException {
        throw new StoreException("read only");
    }

    public IComponentInfoList getComponentInfoList() throws StoreException {
        ComponentInfoList list = new ComponentInfoList();
        Iterator<IEventInfo> it = map.values().iterator();
        while (it.hasNext()) {
            list.add(it.next());
        }
        return list;
    }

    public IComponentInfoList getComponentInfoList(String calendarId) throws StoreException {
        ComponentInfoList list = new ComponentInfoList();
        Iterator<IEventInfo> it = map.values().iterator();
        while (it.hasNext()) {
            list.add(it.next());
        }
        return list;
    }

    public Iterator<String> getIdIterator() throws StoreException {
        return map.keySet().iterator();
    }

    public Iterator<String> getIdIterator(String calendarId) throws StoreException {
        return map.keySet().iterator();
    }

    public boolean isReadOnly(String uid) throws StoreException {
        return true;
    }
}
