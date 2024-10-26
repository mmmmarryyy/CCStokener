package org.azrul.mewit.client;

import com.thoughtworks.xstream.XStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.azrul.epice.domain.Item;
import org.azrul.epice.domain.Item.Priority;
import org.azrul.epice.domain.FileRepository;
import org.azrul.epice.rest.dto.DelegateItemRequest;
import org.azrul.epice.rest.dto.DelegateItemResponse;
import com.wavemaker.runtime.RuntimeAccess;

/**
 * This is a client-facing service class.  All
 * public methods will be exposed to the client.  Their return
 * values and parameters will be passed to the client or taken
 * from the client, respectively.  This will be a singleton
 * instance, shared between all requests. 
 * 
 * To log, call the superclass method log(LOG_LEVEL, String) or log(LOG_LEVEL, String, Exception).
 * LOG_LEVEL is one of FATAL, ERROR, WARN, INFO and DEBUG to modify your log level.
 * For info on these levels, look for tomcat/log4j documentation
 */
public class DelegateItem extends com.wavemaker.runtime.javaservice.JavaServiceSuperClass {

    public DelegateItem() {
        super(INFO);
    }

    public Item doDelegate(String parentId, String targets, String subject, String task, Date startDate, Date deadline, String links, String tags, FileRepository fileRepository) throws java.text.ParseException, UnsupportedEncodingException, IOException {
        log(INFO, "Delegate item: Parent id=" + parentId);
        String sessionId = (String) RuntimeAccess.getInstance().getSession().getAttribute("SESSION_ID");
        DefaultHttpClient httpclient = new DefaultHttpClient();
        DelegateItemRequest request = new DelegateItemRequest();
        request.setDeadline(deadline);
        request.setPriority(Priority.NOT_SET);
        String[] recip = targets.split(",");
        request.setRecipients(Arrays.asList(recip));
        request.setSessionId(sessionId);
        request.setStartDate(startDate);
        request.setTask(task);
        request.setParentId(parentId);
        request.setFileRepository(fileRepository);
        request.setSubject(subject);
        if (links == null || ("").equals(links.trim())) {
        } else {
            String[] alinks = links.split(",");
            List<String> linkList = new ArrayList<String>();
            linkList.addAll(Arrays.asList(alinks));
            request.setLinks(linkList);
        }
        if (tags == null || ("").equals(tags.trim())) {
        } else {
            String[] atags = tags.split(",");
            List<String> tagList = new ArrayList<String>();
            tagList.addAll(Arrays.asList(atags));
            request.setTags(tagList);
        }
        XStream writer = new XStream();
        writer.setMode(XStream.XPATH_ABSOLUTE_REFERENCES);
        writer.alias("DelegateItemRequest", DelegateItemRequest.class);
        XStream reader = new XStream();
        reader.setMode(XStream.XPATH_ABSOLUTE_REFERENCES);
        reader.alias("DelegateItemResponse", DelegateItemResponse.class);
        String strRequest = URLEncoder.encode(reader.toXML(request), "UTF-8");
        HttpPost httppost = new HttpPost(MewitProperties.getMewitUrl() + "/resources/delegateItem?REQUEST=" + strRequest);
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity entity = response.getEntity();
        DelegateItemResponse oResponse = null;
        if (entity != null) {
            String result = URLDecoder.decode(EntityUtils.toString(entity), "UTF-8");
            oResponse = (DelegateItemResponse) reader.fromXML(result);
        }
        return oResponse.getParentItem();
    }
}
