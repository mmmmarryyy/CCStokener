package org.azrul.mewit.client;

import com.thoughtworks.xstream.XStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.azrul.epice.domain.FileRepository;
import org.azrul.epice.domain.Item;
import org.azrul.epice.domain.Item.Priority;
import org.azrul.epice.rest.dto.AcceptItemRequest;
import org.azrul.epice.rest.dto.AcceptItemResponse;
import org.azrul.epice.rest.dto.GiveCommentsOnFeedbackRequest;
import org.azrul.epice.rest.dto.GiveCommentsOnFeedbackResponse;
import org.azrul.epice.rest.dto.GiveFeedbackRequest;
import org.azrul.epice.rest.dto.GiveFeedbackResponse;
import org.azrul.epice.rest.dto.NegotiateDeadlineRequest;
import org.azrul.epice.rest.dto.NegotiateDeadlineResponse;
import org.azrul.epice.rest.dto.RejectItemRequest;
import org.azrul.epice.rest.dto.RejectItemResponse;
import org.azrul.epice.rest.dto.UpdateItemRequest;
import org.azrul.epice.rest.dto.UpdateItemResponse;
import org.azrul.epice.rest.dto.RespondNegotiateDeadlineRequest;
import org.azrul.epice.rest.dto.RespondNegotiateDeadlineResponse;
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
public class ModifyItemService extends com.wavemaker.runtime.javaservice.JavaServiceSuperClass {

    public ModifyItemService() {
        super(INFO);
    }

    public String sampleJavaOperation() {
        String result = null;
        try {
            log(INFO, "Starting sample operation");
            result = "Hello World";
            log(INFO, "Returning " + result);
        } catch (Exception e) {
            log(ERROR, "The sample java service operation has failed", e);
        }
        return result;
    }

    public Item doModify(String sessionId, String itemId, String parentId, String status, String type, String subject, String sender, String recipient, Date startDate, Date deadline, String task, Date negotiatedDeadline, String reasonForNegotiation, String reasonForRejection, String feedback, String commentsOnFeedback, String priority, boolean archived, boolean reference, String acceptNegoReject, String confirmedUnconfirmed, String links, String tags, FileRepository fileRepository) throws UnsupportedEncodingException, IOException {
        sessionId = (String) RuntimeAccess.getInstance().getSession().getAttribute("SESSION_ID");
        log(INFO, "doModify Session id=" + sessionId);
        DefaultHttpClient httpclient = new DefaultHttpClient();
        UpdateItemRequest request = new UpdateItemRequest();
        log(INFO, "one");
        Item item = new Item();
        item.setArchived(archived);
        item.setCommentsOnFeedback(commentsOnFeedback);
        item.setDeadLine(deadline);
        item.setFeedback(feedback);
        item.setId(itemId);
        item.setNegotiatedDeadLine(negotiatedDeadline);
        item.setReasonForNegotiatiationOfDeadLine(reasonForNegotiation);
        item.setReasonForRejectionOfTask(reasonForRejection);
        item.setStartDate(startDate);
        item.setTask(task);
        item.setPriority(Priority.valueOf(priority));
        item.setSubject(subject);
        item.setType(type);
        item.setSender(sender);
        item.setParentId(parentId);
        item.setFileRepository(fileRepository);
        String[] recip = recipient.split(",");
        item.setRecipient(recip[0]);
        Set<String> recipSet = new HashSet<String>();
        recipSet.addAll(Arrays.asList(recip));
        item.setRecipients(recipSet);
        if (links == null || ("").equals(links.trim())) {
        } else {
            String[] alinks = links.split(",");
            Set<String> linksSet = new HashSet<String>();
            linksSet.addAll(Arrays.asList(alinks));
            item.setLinks(linksSet);
        }
        if (tags == null || ("").equals(tags.trim())) {
        } else {
            String[] atags = tags.split(",");
            Set<String> tagsSet = new HashSet<String>();
            tagsSet.addAll(Arrays.asList(atags));
            item.setTags(tagsSet);
        }
        if (status != null) {
            item.setStatus(Item.Status.valueOf(status));
        } else {
            item.setStatus(null);
        }
        Map<String, String> parameters = new HashMap<String, String>();
        log(INFO, "two");
        parameters.put("CONFIRM_STATE", "Approved".equals(confirmedUnconfirmed) ? "CONFIRMED" : "NOT_CONFIRMED");
        parameters.put("ACCEPT_STATE", "Accept".equals(acceptNegoReject) ? "ACCEPT" : "Negotiate".equals(acceptNegoReject) ? "NEGOTIATE" : "REJECT");
        request.setNewItem(item);
        request.setParameters(parameters);
        request.setSessionId(sessionId);
        log(INFO, "three");
        XStream writer = new XStream();
        writer.setMode(XStream.XPATH_ABSOLUTE_REFERENCES);
        writer.alias("UpdateItemRequest", UpdateItemRequest.class);
        XStream reader = new XStream();
        reader.setMode(XStream.XPATH_ABSOLUTE_REFERENCES);
        reader.alias("UpdateItemResponse", UpdateItemResponse.class);
        log(INFO, "four");
        String strRequest = URLEncoder.encode(reader.toXML(request), "UTF-8");
        HttpPost httppost = new HttpPost(MewitProperties.getMewitUrl() + "/resources/updateItem?REQUEST=" + strRequest);
        log(INFO, "five");
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            String result = URLDecoder.decode(EntityUtils.toString(entity), "UTF-8");
            log(INFO, "Result:" + result);
            UpdateItemResponse oResponse = (UpdateItemResponse) reader.fromXML(result);
            return oResponse.getUpdatedItem();
        }
        return null;
    }

    public Item doAccept(String sessionId, Item item) throws UnsupportedEncodingException, IOException {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        AcceptItemRequest request = new AcceptItemRequest();
        request.setItemID(item.getId());
        request.setSessionId(sessionId);
        XStream writer = new XStream();
        writer.setMode(XStream.XPATH_ABSOLUTE_REFERENCES);
        writer.alias("AcceptItemRequest", AcceptItemRequest.class);
        XStream reader = new XStream();
        reader.setMode(XStream.XPATH_ABSOLUTE_REFERENCES);
        reader.alias("AcceptItemResponse", AcceptItemResponse.class);
        String strRequest = URLEncoder.encode(reader.toXML(request), "UTF-8");
        HttpPost httppost = new HttpPost(MewitProperties.getMewitUrl() + "/resources/acceptItem?REQUEST=" + strRequest);
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            String result = URLDecoder.decode(EntityUtils.toString(entity), "UTF-8");
            AcceptItemResponse oResponse = (AcceptItemResponse) reader.fromXML(result);
            return oResponse.getItem();
        }
        return null;
    }

    public Item doReject(String sessionId, Item item, String reason) throws UnsupportedEncodingException, IOException {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        RejectItemRequest request = new RejectItemRequest();
        request.setItemID(item.getId());
        request.setSessionId(sessionId);
        request.setReason(reason);
        XStream writer = new XStream();
        writer.setMode(XStream.XPATH_ABSOLUTE_REFERENCES);
        writer.alias("RejectItemRequest", RejectItemRequest.class);
        XStream reader = new XStream();
        reader.setMode(XStream.XPATH_ABSOLUTE_REFERENCES);
        reader.alias("RejectItemResponse", RejectItemResponse.class);
        String strRequest = URLEncoder.encode(reader.toXML(request), "UTF-8");
        HttpPost httppost = new HttpPost(MewitProperties.getMewitUrl() + "/resources/rejectItem?REQUEST=" + strRequest);
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            String result = URLDecoder.decode(EntityUtils.toString(entity), "UTF-8");
            RejectItemResponse oResponse = (RejectItemResponse) reader.fromXML(result);
            return oResponse.getItem();
        }
        return null;
    }

    public Item doGiveCommentsOnFeedback(String sessionId, Item item, String comments, boolean approved) throws UnsupportedEncodingException, IOException {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        GiveCommentsOnFeedbackRequest request = new GiveCommentsOnFeedbackRequest();
        request.setItemID(item.getId());
        request.setSessionId(sessionId);
        request.setComments(comments);
        request.setApproved(approved);
        XStream writer = new XStream();
        writer.setMode(XStream.XPATH_ABSOLUTE_REFERENCES);
        writer.alias("GiveCommentsOnFeedbackRequest", GiveCommentsOnFeedbackRequest.class);
        XStream reader = new XStream();
        reader.setMode(XStream.XPATH_ABSOLUTE_REFERENCES);
        reader.alias("GiveCommentsOnFeedbackResponse", GiveCommentsOnFeedbackResponse.class);
        String strRequest = URLEncoder.encode(reader.toXML(request), "UTF-8");
        HttpPost httppost = new HttpPost(MewitProperties.getMewitUrl() + "/resources/giveCommentsOnFeedback?REQUEST=" + strRequest);
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            String result = URLDecoder.decode(EntityUtils.toString(entity), "UTF-8");
            GiveCommentsOnFeedbackResponse oResponse = (GiveCommentsOnFeedbackResponse) reader.fromXML(result);
            return oResponse.getItem();
        }
        return null;
    }

    public Item doGiveFeedback(String sessionId, Item item, String feedback) throws UnsupportedEncodingException, IOException {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        GiveFeedbackRequest request = new GiveFeedbackRequest();
        request.setItemID(item.getId());
        request.setSessionId(sessionId);
        request.setFeedback(feedback);
        XStream writer = new XStream();
        writer.setMode(XStream.XPATH_ABSOLUTE_REFERENCES);
        writer.alias("GiveFeedbackRequest", GiveFeedbackRequest.class);
        XStream reader = new XStream();
        reader.setMode(XStream.XPATH_ABSOLUTE_REFERENCES);
        reader.alias("GiveFeedbackResponse", GiveFeedbackResponse.class);
        String strRequest = URLEncoder.encode(reader.toXML(request), "UTF-8");
        HttpPost httppost = new HttpPost(MewitProperties.getMewitUrl() + "/resources/giveFeedback?REQUEST=" + strRequest);
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            String result = URLDecoder.decode(EntityUtils.toString(entity), "UTF-8");
            GiveFeedbackResponse oResponse = (GiveFeedbackResponse) reader.fromXML(result);
            return oResponse.getItem();
        }
        return null;
    }

    public Item doNegotiateDeadline(String sessionId, Item item, String reason, Date newDeadline) throws UnsupportedEncodingException, IOException {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        NegotiateDeadlineRequest request = new NegotiateDeadlineRequest();
        request.setItemID(item.getId());
        request.setSessionId(sessionId);
        request.setReasonForNegotiationOfDeadline(reason);
        request.setNewProposedDeadline(newDeadline);
        XStream writer = new XStream();
        writer.setMode(XStream.XPATH_ABSOLUTE_REFERENCES);
        writer.alias("NegotiateDeadlineRequest", NegotiateDeadlineRequest.class);
        XStream reader = new XStream();
        reader.setMode(XStream.XPATH_ABSOLUTE_REFERENCES);
        reader.alias("NegotiateDeadlineResponse", NegotiateDeadlineResponse.class);
        String strRequest = URLEncoder.encode(reader.toXML(request), "UTF-8");
        HttpPost httppost = new HttpPost(MewitProperties.getMewitUrl() + "/resources/negotiateDeadline?REQUEST=" + strRequest);
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            String result = URLDecoder.decode(EntityUtils.toString(entity), "UTF-8");
            NegotiateDeadlineResponse oResponse = (NegotiateDeadlineResponse) reader.fromXML(result);
            return oResponse.getItem();
        }
        return null;
    }

    public Item doRespondNegotiateDeadline(String sessionId, Item item, Date newDeadline) throws UnsupportedEncodingException, IOException {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        RespondNegotiateDeadlineRequest request = new RespondNegotiateDeadlineRequest();
        request.setItemID(item.getId());
        request.setSessionId(sessionId);
        request.setNewProposedDeadline(newDeadline);
        XStream writer = new XStream();
        writer.setMode(XStream.XPATH_ABSOLUTE_REFERENCES);
        writer.alias("RespondNegotiateDeadlineRequest", RespondNegotiateDeadlineRequest.class);
        XStream reader = new XStream();
        reader.setMode(XStream.XPATH_ABSOLUTE_REFERENCES);
        reader.alias("RespondNegotiateDeadlineResponse", RespondNegotiateDeadlineResponse.class);
        String strRequest = URLEncoder.encode(reader.toXML(request), "UTF-8");
        HttpPost httppost = new HttpPost(MewitProperties.getMewitUrl() + "/resources/respondNegotiateDeadline?REQUEST=" + strRequest);
        HttpResponse response = httpclient.execute(httppost);
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            String result = URLDecoder.decode(EntityUtils.toString(entity), "UTF-8");
            RespondNegotiateDeadlineResponse oResponse = (RespondNegotiateDeadlineResponse) reader.fromXML(result);
            return oResponse.getItem();
        }
        return null;
    }
}
