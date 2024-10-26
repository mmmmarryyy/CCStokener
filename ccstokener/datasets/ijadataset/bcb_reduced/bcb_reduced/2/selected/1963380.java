package ru.korusconsulting.connector.base;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.TimeZone;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import ru.korusconsulting.connector.exceptions.SoapRequestException;
import ru.korusconsulting.connector.funambol.CalendarUtils;
import ru.korusconsulting.connector.funambol.ContactUtils;
import ru.korusconsulting.connector.funambol.NoteUtils;
import ru.korusconsulting.connector.manager.ContactManager;
import ru.korusconsulting.connector.manager.Manager;
import com.funambol.common.pim.calendar.Calendar;
import com.funambol.common.pim.common.ConversionException;
import com.funambol.common.pim.common.Property;
import com.funambol.common.pim.contact.Contact;
import com.funambol.common.pim.contact.Photo;
import com.funambol.common.pim.note.Note;
import com.funambol.framework.logging.FunambolLogger;
import com.funambol.framework.logging.FunambolLoggerFactory;
import com.funambol.framework.security.Sync4jPrincipal;
import com.funambol.framework.server.Sync4jUser;

public class ZimbraPort {

    private URL endpoint;

    boolean connOpen;

    private HttpURLConnection urlConnection;

    private org.dom4j.io.SAXReader reader = new org.dom4j.io.SAXReader();

    private ConnectionContext ccontext = new ConnectionContext();

    private XMLDocumentWriter writer = new XMLDocumentWriter();

    private DocumentFactory documentFactory = DocumentFactory.getInstance();

    private FolderHolder folderHolder = new FolderHolder();

    private TagHolder tagHolder = new TagHolder();

    private String servletUploadURL;

    private FunambolLogger logger;

    private String servletDownloadURL;

    private SoapHelper soapHelper;

    /**
     * Create zimbra port with specified URL
     *
     * @param _endpoint
     */
    public ZimbraPort(URL _endpoint) {
        endpoint = _endpoint;
        servletUploadURL = endpoint.getProtocol() + "://" + endpoint.getAuthority() + "/service/upload?fmt=raw";
        servletDownloadURL = endpoint.getProtocol() + "://" + endpoint.getAuthority() + "/service/home/~/?";
        connOpen = false;
        Map<String, String> map = new HashMap<String, String>();
        map.put("zimbra", ZConst.URN_ZIMBRA);
        map.put("zimbraMail", ZConst.URN_ZIMBRA_MAIL);
        map.put("zimbraAccount", ZConst.URN_ZIMBRA_ACCOUNT);
        map.put("soap", SoapHelper.SOAP12_ENV_NS);
        documentFactory.setXPathNamespaceURIs(map);
        logger = FunambolLoggerFactory.getLogger("funambol.zimbra");
        soapHelper = SoapHelper.getInstance(documentFactory);
    }

    /**
     * Init Http connection
     *
     * @return
     */
    private boolean init() {
        try {
            urlConnection = (HttpURLConnection) endpoint.openConnection();
            urlConnection.setRequestMethod("POST");
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);
            connOpen = true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * Request autorization on server with specified credentials
     *
     * @param principal
     *            Sync principal
     * @return response
     * @throws IOException -
     *             IOError occur
     * @throws SoapRequestException
     *             Invalid soap document response was received
     */
    public Document requestAutorization(Sync4jUser user) throws IOException, SoapRequestException {
        if (!init()) {
            return null;
        }
        Document request = createZimbraCall(ZConst.AUTH_REQUEST, ZConst.URN_ZIMBRA_ACCOUNT);
        Element authRequest = (Element) soapHelper.getBody(request).elements().get(0);
        {
            Element account = documentFactory.createElement("account");
            {
                account.addAttribute(ZConst.A_BY, ZConst.A_FNAME);
                account.setText(user.getUsername());
            }
            authRequest.add(account);
            Element password = documentFactory.createElement(ZConst.E_PASSWORD);
            {
                password.setText(user.getPassword());
            }
            authRequest.add(password);
        }
        Document response = sendRequest(request);
        Element authResponse = soapHelper.getBody(response).element(ZConst.AUTH_RESPONSE);
        Element authToken = authResponse.element(ZConst.E_AUTH_TOKEN);
        ccontext.setAuthToken(authToken.getTextTrim());
        ccontext.processContext(soapHelper.getContext(response));
        folderHolder.setRootFolder(soapHelper.getRootFolder(response));
        if (soapHelper.getRootTags(response) != null) tagHolder.setTags(soapHelper.getRootTags(response).elements());
        return response;
    }

    /**
     * Request Full information about contacts, photo include
     *
     * @return Response method element
     * @throws IOException -
     *             IOError occur
     * @throws SoapRequestException
     *             Invalid soap document response was received
     */
    @SuppressWarnings("unchecked")
    public ArrayList<Contact> requestAllContact(boolean trashInclude) throws IOException, SoapRequestException {
        if (!init()) {
            return null;
        }
        Document request = createZimbraCall(ZConst.GET_CONTACTS_REQUEST, ZConst.URN_ZIMBRA_MAIL);
        Element contactsRequest = (Element) soapHelper.getBody(request).elements().get(0);
        {
            contactsRequest.addAttribute(ZConst.A_SYNC, "1");
        }
        Document response = sendRequest(request);
        Element body = soapHelper.getBody(response);
        Element contactsResponse = body.element(ZConst.GET_CONTACTS_RESPONSE);
        ccontext.processContext(soapHelper.getContext(response));
        List<Element> elements = contactsResponse.elements();
        ArrayList<Contact> allContacts = new ArrayList<Contact>(elements.size());
        for (Iterator<Element> iterator = elements.iterator(); iterator.hasNext(); ) {
            Element cn = (Element) iterator.next();
            String folderId = cn.attributeValue(ZConst.A_FOLDER);
            if (!trashInclude && folderId.equals(getTrashFolderId())) {
                continue;
            }
            Properties prop = ContactUtils.toProperties(cn);
            if (StringUtils.isBlank(prop.getProperty(ContactUtils.FIRST_NAME)) && StringUtils.isBlank(prop.getProperty(ContactUtils.LAST_NAME)) && StringUtils.isBlank(prop.getProperty(ContactUtils.EMAIL)) && StringUtils.isBlank(prop.getProperty(ContactUtils.COMPANY))) continue;
            Contact contact = ContactUtils.createContactFromProperties(prop);
            String contactId = cn.attributeValue(ZConst.A_ID);
            Element imageAttrib = (Element) cn.selectSingleNode("zimbraMail:a[@n='image']");
            if (imageAttrib != null) {
                String part = imageAttrib.attributeValue("part");
                String url = servletDownloadURL + "id=" + contactId + "&part=" + part;
                byte[] image = ConnectionUtils.download(url, ccontext);
                Photo photo = new Photo();
                photo.setImage(image);
                contact.getPersonalDetail().setPhotoObject(photo);
            }
            String folderName = folderHolder.getNameById(folderId, FolderHolder.CONTACT);
            contact.setFolder(folderName);
            String tagIds = convertTagsIdsToNames(cn.attributeValue(ZConst.A_TAG));
            contact.setCategories(tagIds == null ? new Property() : new Property(tagIds));
            allContacts.add(contact);
        }
        processFault(body, request, response);
        return allContacts;
    }

    /**
     * Request All available information about contact
     *
     * @param syncPhoto
     * @return Contact
     * @throws IOException -
     *             IOError occur
     * @throws SoapRequestException
     *             Invalid soap document response was received
     */
    public Contact requestContactById(String contactId, boolean syncPhoto) throws IOException, SoapRequestException {
        if (!init()) {
            return null;
        }
        Document request = createZimbraCall(ZConst.GET_CONTACTS_REQUEST, ZConst.URN_ZIMBRA_MAIL);
        Element contactsRequest = (Element) soapHelper.getBody(request).elements().get(0);
        {
            Element cn = documentFactory.createElement(ZConst.E_CN);
            {
                cn.addAttribute(ZConst.A_ID, contactId);
            }
            contactsRequest.add(cn);
            contactsRequest.addAttribute(ZConst.A_SYNC, "1");
        }
        Document response = sendRequest(request);
        Element contactsResponse = soapHelper.getBody(response).element(ZConst.GET_CONTACTS_RESPONSE);
        ccontext.processContext(soapHelper.getContext(response));
        Element cn = contactsResponse.element(ZConst.E_CN);
        Contact contact = ContactUtils.asContact(cn);
        Element imageAttrib = (Element) cn.selectSingleNode("zimbraMail:a[@n='image']");
        if (imageAttrib != null && syncPhoto) {
            String part = imageAttrib.attributeValue("part");
            String url = servletDownloadURL + "id=" + contactId + "&part=" + part;
            byte[] image = ConnectionUtils.download(url, ccontext);
            Photo photo = new Photo();
            photo.setImage(image);
            contact.getPersonalDetail().setPhotoObject(photo);
        }
        String folderId = cn.attributeValue(ZConst.A_FOLDER);
        String folderName = folderHolder.getNameById(folderId, FolderHolder.CONTACT);
        contact.setFolder(folderName);
        String tagIds = convertTagsIdsToNames(cn.attributeValue(ZConst.A_TAG));
        contact.setCategories(tagIds == null ? new Property() : new Property(tagIds));
        return contact;
    }

    /**
     * Request all contact with identification information We have two way
     * detect indentical Contact, first way - by Id, second way - by firstName,
     * lastName and email1
     *
     * @return Response method element
     * @throws IOException -
     *             IOError occur
     * @throws SoapRequestException
     *             Invalid soap document response was received
     */
    public Element requestGALContactIds() throws IOException, SoapRequestException {
        if (!init()) {
            return null;
        }
        Document request = createZimbraCall(ZConst.AUTO_COMPLETE_GAL_REQUEST, ZConst.URN_ZIMBRA_ACCOUNT);
        Element contactsRequest = (Element) soapHelper.getBody(request).elements().get(0);
        {
            Element name = documentFactory.createElement(ZConst.A_FNAME);
            {
            }
            contactsRequest.addAttribute("type", "account");
            contactsRequest.addAttribute(ZConst.A_LIMIT, "0");
            contactsRequest.add(name);
        }
        Document response = sendRequest(request);
        Element contactsResponse = soapHelper.getBody(response).element(ZConst.AUTO_COMPLETE_GAL_RESPONSE);
        ccontext.processContext(soapHelper.getContext(response));
        return contactsResponse;
    }

    /**
     * Request all contact with identification information We have two way
     * detect identical Contact, first way - by Id, second way - by firstName,
     * lastName and email1
     *
     * @return Response method element
     * @throws IOException -
     *             IOError occur
     * @throws SoapRequestException
     *             Invalid soap document response was received
     */
    public Element requestAllContactIds() throws IOException, SoapRequestException {
        if (!init()) {
            return null;
        }
        Document request = createZimbraCall(ZConst.GET_CONTACTS_REQUEST, ZConst.URN_ZIMBRA_MAIL);
        Element contactsRequest = (Element) soapHelper.getBody(request).elements().get(0);
        {
            Element attr = documentFactory.createElement(ZConst.A_ATTRIBUTE);
            {
                attr.addAttribute(ZConst.A_NAME, ContactUtils.FIRST_NAME);
            }
            contactsRequest.add(attr);
            attr = documentFactory.createElement(ZConst.A_ATTRIBUTE);
            {
                attr.addAttribute(ZConst.A_NAME, ContactUtils.LAST_NAME);
            }
            contactsRequest.add(attr);
            attr = documentFactory.createElement(ZConst.A_ATTRIBUTE);
            {
                attr.addAttribute(ZConst.A_NAME, ContactUtils.EMAIL);
            }
            contactsRequest.add(attr);
            attr = documentFactory.createElement(ZConst.A_ATTRIBUTE);
            {
                attr.addAttribute(ZConst.A_NAME, ContactUtils.COMPANY);
            }
            contactsRequest.add(attr);
            contactsRequest.addAttribute(ZConst.A_SYNC, "1");
        }
        Document response = sendRequest(request);
        Element contactsResponse = soapHelper.getBody(response).element(ZConst.GET_CONTACTS_RESPONSE);
        ccontext.processContext(soapHelper.getContext(response));
        for (Iterator iterator = contactsResponse.elementIterator(); iterator.hasNext(); ) {
            Element cn = (Element) iterator.next();
            Properties prop = ContactUtils.toProperties(cn);
            if (StringUtils.isBlank(prop.getProperty(ContactUtils.FIRST_NAME)) && StringUtils.isBlank(prop.getProperty(ContactUtils.LAST_NAME)) && StringUtils.isBlank(prop.getProperty(ContactUtils.EMAIL)) && StringUtils.isBlank(prop.getProperty(ContactUtils.COMPANY))) iterator.remove();
        }
        return contactsResponse;
    }

    /**
     * Remove all items in the element itemsResponse
     *
     * @param itemsResponse -
     *            The element that contains items (contact, appt, etc.)
     * @throws IOException -
     *             IOError occur
     * @throws SoapRequestException
     *             Invalid soap document response was received
     */
    @SuppressWarnings("unchecked")
    public void requestRemoveAllItems(Element itemsResponse, String attrIdName) throws IOException, SoapRequestException {
        StringBuilder sb = new StringBuilder();
        for (Iterator<Element> iterator = itemsResponse.elementIterator(); iterator.hasNext(); ) {
            Element cn = (Element) iterator.next();
            Attribute attr = cn.attribute(attrIdName);
            if (attr == null) throw new SoapRequestException("Invalid attribute in element");
            sb.append(attr.getValue());
            if (iterator.hasNext()) sb.append(",");
        }
        String listId = sb.toString();
        if (!listId.trim().equals("")) {
            requestDeleteItem(listId, false);
        }
    }

    /**
     * Create contact and push it on server
     *
     * @param c
     *            the contact from phone
     * @return response of create
     * @throws IOException -
     *             IOError occur
     * @throws SoapRequestException
     *             Invalid soap document response was received
     */
    public Element requestCreateContact(Contact c) throws IOException, SoapRequestException {
        if (!init()) {
            return null;
        }
        String firstName = ContactUtils.getFirstName(c);
        String lastName = ContactUtils.getLastName(c);
        String email = ContactUtils.getEmail(c);
        String company = ContactUtils.getCompany(c);
        if (StringUtils.isBlank(firstName) && StringUtils.isBlank(lastName) && StringUtils.isBlank(email) && StringUtils.isBlank(company)) {
            return null;
        }
        Document request = createZimbraCall(ZConst.CREATE_CONTACT_REQUEST, ZConst.URN_ZIMBRA_MAIL);
        String folderName = c.getFolder();
        String folderId = null;
        if (folderName != null) {
            folderId = createFolderRequest(folderName, FolderHolder.CONTACT);
        }
        Element contactsRequest = (Element) soapHelper.getBody(request).elements().get(0);
        {
            Element contact = ContactUtils.asElement(c, documentFactory, ZConst.URN_ZIMBRA_MAIL, false);
            if (contact.hasContent()) {
                contactsRequest.add(contact);
                if (folderId != null) {
                    contact.addAttribute(ZConst.A_FOLDER, folderId);
                }
                String tagsStr = Manager.getValue(c.getCategories());
                String tagsIds = createTagsRequest(tagsStr);
                contact.addAttribute(ZConst.A_TAG, tagsIds);
            }
            addPhotoAttributeToContact(c, contact, false);
        }
        Document response = sendRequest(request);
        Element contactsResponse = soapHelper.getBody(response).element(ZConst.CREATE_CONTACT_RESPONSE);
        ccontext.processContext(soapHelper.getContext(response));
        return contactsResponse;
    }

    private void addPhotoAttributeToContact(Contact c, Element contact, boolean u) {
        Photo photo = c.getPersonalDetail().getPhotoObject();
        if (photo != null && photo.getImage() != null && photo.getImage().length > 0) {
            try {
                String aid = ConnectionUtils.upload("image", photo.getImage(), ccontext, servletUploadURL);
                Element attr = documentFactory.createElement(ZConst.A_ATTRIBUTE);
                {
                    attr.addAttribute(ZConst.A_NAME, "image");
                    attr.addAttribute(ZConst.A_AID, aid);
                }
                contact.add(attr);
            } catch (Exception e) {
                logger.error("Can't load Photo to contact");
            }
        } else if (u) {
            Element attr = documentFactory.createElement(ZConst.A_ATTRIBUTE);
            {
                attr.addAttribute(ZConst.A_NAME, "image");
            }
            contact.add(attr);
        }
    }

    /**
     * Modify contact from phone
     *
     * @param c -
     *            userContact
     * @param oldTags
     * @param trashElem
     *            if sets, then contact from Trash folder recover
     * @return response of Modify Contact
     * @throws IOException -
     *             IOError occur
     * @throws SoapRequestException
     *             Invalid soap document response was received
     */
    public Element requestModifyContact(Contact c, String oldFolderId, Object oldTags) throws IOException, SoapRequestException {
        if (!init()) {
            return null;
        }
        String folderName = c.getFolder();
        String folderId = null;
        if (folderName == null) {
            folderName = FolderHolder.DEFAULT_FOLDER;
        }
        folderId = createFolderRequest(folderName, FolderHolder.CONTACT);
        Document request = createZimbraDoc();
        Element body = soapHelper.getBody(request);
        Element contactsRequest = body.addElement(ZConst.MODIFY_CONTACT_REQUEST, ZConst.URN_ZIMBRA_MAIL);
        {
            Element contact = ContactUtils.asElement(c, documentFactory, ZConst.URN_ZIMBRA_MAIL, true);
            if (contact.hasContent()) {
                contact.addAttribute(ZConst.A_ID, c.getUid());
                contactsRequest.add(contact);
                addPhotoAttributeToContact(c, contact, true);
                String tagsStr = Manager.getValue(c.getCategories());
                String tagsIds = createTagsRequest(tagsStr);
                contact.addAttribute(ZConst.A_TAG, tagsIds);
            }
            contactsRequest.addAttribute(ZConst.A_REPLACE, "0");
            contactsRequest.addAttribute(ZConst.A_FORCE, "1");
        }
        String tagsStr = Manager.getValue(c.getCategories());
        String tagsIds = createTagsRequest(tagsStr);
        Document response = sendRequest(request);
        body = soapHelper.getBody(response);
        Element contactsResponse = body.element(ZConst.MODIFY_CONTACT_RESPONSE);
        if (contactsResponse == null) {
            logger.error("Error occur, can't find correct response");
            writer.write(request);
            writer.write(response);
            throw new SoapRequestException();
        }
        ccontext.processContext(soapHelper.getContext(response));
        if (!init()) {
            return null;
        }
        request = createZimbraDoc();
        body = soapHelper.getBody(request);
        Element batch = body.addElement(ZConst.BATCH_REQUEST, ZConst.URN_ZIMBRA);
        if (!oldFolderId.equals(folderId)) {
            attachMoveContactRequest(batch, c.getUid(), folderId);
        }
        if (!oldTags.equals(tagsIds)) {
            attachTagItemRequest(batch, c.getUid(), tagsIds);
        }
        response = sendRequest(request);
        batch = soapHelper.getBody(response).element(ZConst.BATCH_RESPONSE);
        processFault(batch, request, response);
        ccontext.processContext(soapHelper.getContext(response));
        return contactsResponse;
    }

    /**
     * Delete contact by contact Id, if soft parameter is true then contact
     * moved in the trash folder
     *
     * @param contactId -
     *            Contact Id
     * @param soft -
     *            soft delete flag
     * @return response of Delete Contact Action
     * @throws IOException -
     *             IOError occur
     * @throws SoapRequestException
     *             Invalid soap document response was received
     */
    public Element requestDeleteContact(String contactId, boolean soft) throws IOException, SoapRequestException {
        if (!init()) {
            return null;
        }
        Document request = createZimbraCall(ZConst.CONTACT_ACTION_REQUEST, ZConst.URN_ZIMBRA_MAIL);
        Element deleteRequest = (Element) soapHelper.getBody(request).elements().get(0);
        {
            Element action = documentFactory.createElement(ZConst.E_ACTION);
            {
                action.addAttribute(ZConst.A_ID, contactId);
                action.addAttribute(ZConst.A_OPERATION, soft ? ZConst.OP_TRASH : ZConst.OP_DELETE);
            }
            deleteRequest.add(action);
        }
        Document response = sendRequest(request);
        Element contactsResponse = soapHelper.getBody(response).element(ZConst.CONTACT_ACTION_RESPONSE);
        ccontext.processContext(soapHelper.getContext(response));
        return contactsResponse;
    }

    /**
     * Delete item by id, if soft parameter is true then item moved in the trash
     * folder
     *
     * @param contactId -
     *            Contact Id
     * @param soft -
     *            soft delete flag
     * @return response of ItemActionResponse
     * @throws IOException -
     *             IOError occur
     * @throws SoapRequestException
     *             Invalid soap document response was received
     */
    public Element requestDeleteItem(String id, boolean soft) throws IOException, SoapRequestException {
        if (!init()) {
            return null;
        }
        Document request = createZimbraCall(ZConst.ITEM_ACTION_REQUEST, ZConst.URN_ZIMBRA_MAIL);
        Element deleteRequest = (Element) soapHelper.getBody(request).elements().get(0);
        {
            Element action = documentFactory.createElement(ZConst.E_ACTION);
            {
                action.addAttribute(ZConst.A_ID, id);
                action.addAttribute(ZConst.A_OPERATION, soft ? ZConst.OP_TRASH : ZConst.OP_DELETE);
            }
            deleteRequest.add(action);
        }
        Document response = sendRequest(request);
        Element contactsResponse = soapHelper.getBody(response).element(ZConst.ITEM_ACTION_RESPONSE);
        ccontext.processContext(soapHelper.getContext(response));
        return contactsResponse;
    }

    /**
     * Send request to zimra and return response from them
     *
     * @param request
     *            Request
     * @return response
     * @throws IOException -
     *             IOError occur
     * @throws SoapRequestException
     *             Invalid soap document response was received
     */
    private Document sendRequest(Document request) throws IOException, SoapRequestException {
        try {
            writer.write(request);
            byte[] b = safeEncode(request.asXML(), ZConst.ENCODING).getBytes();
            ConnectionUtils.writeToStream(b, urlConnection.getOutputStream());
            Document d = reader.read(urlConnection.getInputStream());
            writer.write(d);
            return d;
        } catch (DocumentException e) {
            urlConnection.disconnect();
            throw new SoapRequestException(e);
        } catch (IOException e) {
            if (!writer.isVerboseEnable()) writer.write(request);
            throw e;
        }
    }

    private String safeEncode(String str, String charset) throws UnsupportedEncodingException {
        CharsetDecoder d = Charset.forName(charset).newDecoder();
        byte[] bytearray = str.getBytes();
        try {
            CharBuffer r = d.decode(ByteBuffer.wrap(bytearray));
            r.toString();
            return str;
        } catch (CharacterCodingException e) {
            return new String(str.getBytes(charset));
        }
    }

    /**
     * Send request to zimra and return response from them
     *
     * @param request
     *            Request
     * @return response
     * @throws IOException -
     *             IOError occur
     * @throws SoapRequestException
     *             Invalid soap document response was received
     */
    Document sendRequest(String request) throws IOException, SoapRequestException {
        try {
            writer.write(request, "");
            byte[] b = safeEncode(request, ZConst.ENCODING).getBytes();
            ConnectionUtils.writeToStream(b, urlConnection.getOutputStream());
            Document d = reader.read(urlConnection.getInputStream());
            writer.write(d);
            return d;
        } catch (DocumentException e) {
            urlConnection.disconnect();
            throw new SoapRequestException(e);
        }
    }

    /**
     * Create template for Zimbra soap call, with specified function name and
     * namespace
     *
     * @param function
     *            Function name
     * @param namespace
     *            Namespace
     * @return Document template with zimbra call
     */
    private Document createZimbraCall(String function, String namespace) {
        return soapHelper.createZimbraCall(function, namespace, documentFactory, ccontext);
    }

    private Document createZimbraDoc() {
        return soapHelper.createZimbraDoc(documentFactory, ccontext);
    }

    /**
     * Close connection
     */
    public void close() {
        if (urlConnection != null) urlConnection.disconnect();
    }

    /**
     * Return Trash folder id
     *
     * @return trash folder id
     */
    public String getTrashFolderId() {
        return folderHolder.getTrashFolderId();
    }

    /**
     * Return document Factory
     *
     * @return Document Factory
     */
    public DocumentFactory getDocumentFactory() {
        return documentFactory;
    }

    /**
     * Request Full information about calendars
     *
     * @return Response method element
     * @throws IOException -
     *             IOError occur
     * @throws SoapRequestException
     *             Invalid soap document response was received
     */
    public Element requestAllCalendarsIds(boolean task) throws IOException, SoapRequestException {
        Element allCalendarResponse = null;
        boolean hasMore = true;
        int offset = 0;
        while (hasMore) {
            if (!init()) {
                return null;
            }
            Document request = createZimbraCall(ZConst.SEARCH_REQUEST, ZConst.URN_ZIMBRA_MAIL);
            Element calendarRequest = (Element) soapHelper.getBody(request).elements().get(0);
            {
                Element query = documentFactory.createElement(ZConst.E_QUERY);
                {
                    query.setText("item:all");
                }
                calendarRequest.add(query);
                calendarRequest.addAttribute(ZConst.A_OFFSET, String.valueOf(offset));
                calendarRequest.addAttribute(ZConst.A_LIMIT, "1000");
                calendarRequest.addAttribute(ZConst.A_TYPES, task ? ZConst.E_TASK : "appointment");
                calendarRequest.addAttribute(ZConst.A_SORT_BY, "dateDesc");
            }
            Document response = sendRequest(request);
            Element calendarsResponse = soapHelper.getBody(response).element(ZConst.SEARCH_RESPONSE);
            hasMore = "1".equals(calendarsResponse.attributeValue("more"));
            offset += calendarsResponse.elements().size();
            ccontext.processContext(soapHelper.getContext(response));
            if (allCalendarResponse == null) {
                allCalendarResponse = (Element) calendarsResponse.detach();
            } else {
                List<Element> elements = calendarsResponse.elements();
                ListIterator<Element> iterator = elements.listIterator();
                while (iterator.hasNext()) {
                    Element node = iterator.next();
                    allCalendarResponse.add(node.detach());
                }
            }
        }
        return allCalendarResponse;
    }

    /**
     * Request Full information about calendars
     * @param phoneTimeZone
     *
     * @return Response method element
     * @throws IOException -
     *             IOError occur
     * @throws SoapRequestException
     *             Invalid soap document response was received
     * @throws ConversionException -
     *             if any exception with convertion any calendar
     */
    public ArrayList<Calendar> requestAllCalendars(ArrayList<String> ids, boolean task, TimeZone phoneTimeZone) throws IOException, SoapRequestException, ConversionException {
        if (!init()) {
            return null;
        }
        Document request = createZimbraDoc();
        Element body = soapHelper.getBody(request);
        Element batch = body.addElement(ZConst.BATCH_REQUEST, ZConst.URN_ZIMBRA);
        String methodRequest = task ? ZConst.GET_TASK_REQUEST : ZConst.GET_APPOINTMENT_REQUEST;
        String methodResponse = task ? ZConst.GET_TASK_RESPONSE : ZConst.GET_APPOINTMENT_RESPONSE;
        for (String id : ids) {
            Element calendarRequest = batch.addElement(methodRequest, ZConst.URN_ZIMBRA_MAIL);
            calendarRequest.addAttribute(ZConst.A_ID, id);
            calendarRequest.addAttribute(ZConst.A_SYNC, "1");
        }
        Document response = sendRequest(request);
        ccontext.processContext(soapHelper.getContext(response));
        body = soapHelper.getBody(response);
        batch = body.element(ZConst.BATCH_RESPONSE);
        List<Element> calResponses = batch.elements(methodResponse);
        ArrayList<Calendar> calendars = new ArrayList<Calendar>(calResponses.size());
        boolean isExceptionOccur = false;
        for (Element calResponse : calResponses) {
            Element item = calResponse.element(task ? ZConst.E_TASK : ZConst.E_APPT);
            try {
                Calendar cal = CalendarUtils.getInstance().asCalendar(item, !task, phoneTimeZone);
                String folderId = item.attributeValue(ZConst.A_FOLDER);
                String folderName = folderHolder.getNameById(folderId, FolderHolder.APPOINTMENT);
                cal.getCalendarContent().setFolder(new Property(folderName));
                CalendarUtils.setTags(cal, convertTagsIdsToNames(item.attributeValue(ZConst.A_TAG)));
                calendars.add(cal);
            } catch (Throwable e) {
                logger.error("Error occur while converting calendar");
                writer.write(item);
                logger.error("", e);
                isExceptionOccur = true;
            }
        }
        processFault(batch, request, response);
        if (isExceptionOccur) throw new ConversionException("Error occur while converting calendars");
        return calendars;
    }

    /**
     * Request All available information about calendar
     * @param phoneTimeZone
     *
     * @return Contact
     * @throws IOException -
     *             IOError occur
     * @throws SoapRequestException
     *             Invalid soap document response was received
     */
    public Calendar requestCalendarById(String calendarId, boolean task, TimeZone phoneTimeZone) throws IOException, SoapRequestException {
        if (!init()) {
            return null;
        }
        Document request = createZimbraCall(task ? ZConst.GET_TASK_REQUEST : ZConst.GET_APPOINTMENT_REQUEST, ZConst.URN_ZIMBRA_MAIL);
        Element calendarRequest = (Element) soapHelper.getBody(request).elements().get(0);
        {
            calendarRequest.addAttribute(ZConst.A_ID, calendarId);
            calendarRequest.addAttribute(ZConst.A_SYNC, "1");
        }
        Document response = sendRequest(request);
        Element body = soapHelper.getBody(response);
        Element callResponse = body.element(task ? ZConst.GET_TASK_RESPONSE : ZConst.GET_APPOINTMENT_RESPONSE);
        ccontext.processContext(soapHelper.getContext(response));
        Element item = callResponse.element(task ? ZConst.E_TASK : ZConst.E_APPT);
        Calendar cal = CalendarUtils.getInstance().asCalendar(item, !task, phoneTimeZone);
        String folderId = item.attributeValue(ZConst.A_FOLDER);
        String folderName = folderHolder.getNameById(folderId, FolderHolder.APPOINTMENT);
        cal.getCalendarContent().setFolder(new Property(folderName));
        CalendarUtils.setTags(cal, convertTagsIdsToNames(item.attributeValue(ZConst.A_TAG)));
        return cal;
    }

    private String convertTagsIdsToNames(String tagsStr) {
        if (tagsStr == null || tagsStr.trim().equals("")) return null;
        String tags[] = tagsStr.split(",");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < tags.length; i++) {
            if (tags[i].trim().equals("")) continue;
            sb.append(tagHolder.getById(tags[i]));
            if (i != tags.length - 1) {
                sb.append(",");
            }
        }
        return sb.toString();
    }

    /**
     * Create contact and push it on server
     *
     * @param c
     *            the contact from phone
     * @return response of create
     * @throws IOException -
     *             IOError occur
     * @throws SoapRequestException
     *             Invalid soap document response was received
     */
    public Element requestCreateCalendar(Calendar c) throws IOException, SoapRequestException {
        if (!init()) {
            return null;
        }
        boolean task = c.getEvent() == null;
        Document request = createZimbraDoc();
        Element body = soapHelper.getBody(request);
        Element batch = body.addElement(ZConst.BATCH_REQUEST, ZConst.URN_ZIMBRA);
        Property folder = c.getCalendarContent().getFolder();
        String folderType = task ? FolderHolder.TASK : FolderHolder.APPOINTMENT;
        String folderName = null;
        if (folder != null) {
            folderName = folder.getPropertyValueAsString();
        }
        if (folderName == null || folderName.equals("")) {
            folderName = FolderHolder.DEFAULT_FOLDER;
        }
        String folderId = createFolderRequest(folderName, folderType);
        Element calendarRequest = batch.addElement(task ? ZConst.CREATE_TASK_REQUEST : ZConst.CREATE_APPOINTMENT_REQUEST, ZConst.URN_ZIMBRA_MAIL);
        {
            Element calendar = CalendarUtils.getInstance().asElement(c, documentFactory, ZConst.URN_ZIMBRA_MAIL, false);
            if (calendar.hasContent()) {
                calendar.addAttribute(ZConst.A_FOLDER, folderId);
                calendarRequest.add(calendar);
            }
        }
        String tagsStr = CalendarUtils.getTags(c);
        String tagsIds = createTagsRequest(tagsStr);
        Document response = sendRequest(request);
        body = soapHelper.getBody(response);
        batch = body.element(ZConst.BATCH_RESPONSE);
        Element calendarResponse = batch.element(task ? ZConst.CREATE_TASK_RESPONSE : ZConst.CREATE_APPOINTMENT_RESPONSE);
        processFault(batch, request, response);
        ccontext.processContext(soapHelper.getContext(response));
        if (!init()) {
            return null;
        }
        if (tagsIds != null && !tagsIds.equals("") && !tagsIds.equals("null")) {
            request = createZimbraDoc();
            body = soapHelper.getBody(request);
            batch = body.addElement(ZConst.BATCH_REQUEST, ZConst.URN_ZIMBRA);
            boolean empty = attachTagItemRequest(batch, calendarResponse.attributeValue("invId"), tagsIds);
            if (!empty) {
                response = sendRequest(request);
                body = soapHelper.getBody(response);
                batch = body.element(ZConst.BATCH_RESPONSE);
                processFault(batch, request, response);
                List<Element> tags = batch.elements(ZConst.CREATE_TAG_RESPONSE);
                for (Element resp : tags) {
                    Element tag = resp.element("tag");
                    tagHolder.put(tag.attributeValue(ZConst.A_ID), tag.attributeValue(ZConst.A_TAGNAME));
                }
                ccontext.processContext(soapHelper.getContext(response));
            }
        }
        return calendarResponse;
    }

    /**
     * Create folder if it's doesn't exist with name from pfolder property.
     *
     * @param pfolder -
     *            property that contain folder name
     * @param folderType -
     *            type of folder, see FolderHolder
     * @return new folder or exist folder id
     * @throws IOException -
     *             IOError occur
     * @throws SoapRequestException
     *             Invalid soap document response was received
     */
    private String createFolderRequest(String folderName, String folderType) throws IOException, SoapRequestException {
        String folderId = folderHolder.getIdByName(folderName, folderType);
        if (folderId == null) {
            Document request = createZimbraDoc();
            Element body = soapHelper.getBody(request);
            Element createFolderRequest = body.addElement(ZConst.CREATE_FOLDER_REQUEST, ZConst.URN_ZIMBRA_MAIL);
            Element folder = createFolderRequest.addElement(ZConst.E_FOLDER);
            folder.addAttribute(ZConst.A_FNAME, folderName);
            folder.addAttribute(ZConst.A_FOLDER, folderHolder.getDefaultFolderId(folderType));
            folder.addAttribute(ZConst.A_FIE, "1");
            folder.addAttribute(ZConst.A_VIEW, folderType);
            Document response = sendRequest(request);
            ccontext.processContext(soapHelper.getContext(response));
            Element folderResponse = soapHelper.getBody(response).element(ZConst.CREATE_FOLDER_RESPONSE);
            Element folderElement = folderResponse.element(ZConst.E_FOLDER);
            folderId = folderElement.attributeValue(ZConst.A_ID);
            folderHolder.notify(folderElement);
            if (!init()) {
                return null;
            }
        }
        return folderId;
    }

    private String createTagsRequest(String tagsStr) throws IOException, SoapRequestException {
        if (tagsStr == null) return "";
        String tags[] = tagsStr.split(",");
        Document request = createZimbraDoc();
        Element body = soapHelper.getBody(request);
        Element batch = body.addElement(ZConst.BATCH_REQUEST, ZConst.URN_ZIMBRA);
        StringBuilder tagIds = new StringBuilder();
        for (String tagName : tags) {
            tagName = tagName.trim();
            if (tagName.equals("")) continue;
            String tagId = tagHolder.getByName(tagName);
            if (tagId != null) {
                tagIds.append(tagId).append(",");
                continue;
            }
            Element createTagRequest = batch.addElement(ZConst.CREATE_TAG_REQUEST, ZConst.URN_ZIMBRA_MAIL);
            Element tagEl = createTagRequest.addElement(ZConst.E_TAG);
            tagEl.addAttribute(ZConst.A_TAGNAME, tagName);
        }
        if (batch.hasContent()) {
            Document response = sendRequest(request);
            ccontext.processContext(soapHelper.getContext(response));
            batch = soapHelper.getBody(response).element(ZConst.BATCH_RESPONSE);
            processFault(batch, request, response);
            List<Element> tagsResponse = batch.elements(ZConst.CREATE_TAG_RESPONSE);
            for (Element tagEl : tagsResponse) {
                String tagId = tagEl.attributeValue(ZConst.A_ID);
                tagHolder.put(tagId, tagEl.attributeValue(ZConst.A_TAGNAME));
                tagIds.append(tagId).append(",");
            }
            if (!init()) {
                return null;
            }
        }
        if (tagIds.length() > 0) {
            return tagIds.substring(0, tagIds.length() - 1);
        }
        return "";
    }

    public void requestAllFolders() throws IOException, SoapRequestException {
        if (!init()) {
            return;
        }
        Document request = createZimbraDoc();
        Element body = soapHelper.getBody(request);
        body.addElement(ZConst.GET_FOLDER_REQUEST, ZConst.URN_ZIMBRA_MAIL);
        Document response = sendRequest(request);
        ccontext.processContext(soapHelper.getContext(response));
        Element folderResponse = soapHelper.getBody(response).element(ZConst.GET_FOLDER_RESPONSE);
        Element folderElement = folderResponse.element(ZConst.E_FOLDER);
        folderHolder.setRootFolder(folderElement);
    }

    public void attachMoveItemRequest(Element body, String itemId, String folderId) {
        Element createFolderRequest = body.addElement(ZConst.ITEM_ACTION_REQUEST, ZConst.URN_ZIMBRA_MAIL);
        Element folder = createFolderRequest.addElement(ZConst.E_ACTION);
        folder.addAttribute(ZConst.A_ID, itemId);
        folder.addAttribute(ZConst.A_FOLDER, folderId);
        folder.addAttribute(ZConst.A_OPERATION, ZConst.OP_MOVE);
    }

    public void attachMoveContactRequest(Element body, String itemId, String folderId) {
        Element createFolderRequest = body.addElement(ZConst.CONTACT_ACTION_REQUEST, ZConst.URN_ZIMBRA_MAIL);
        Element folder = createFolderRequest.addElement(ZConst.E_ACTION);
        folder.addAttribute(ZConst.A_ID, itemId);
        folder.addAttribute(ZConst.A_FOLDER, folderId);
        folder.addAttribute(ZConst.A_OPERATION, ZConst.OP_UPDATE);
    }

    /**
     * Modify calendar from phone
     *
     * @param uid -
     *            invite UID
     * @param c -
     *            user Calendar
     * @param shouldMove
     *            if sets, then calendar from Trash folder recover
     * @return response of Modify Contact
     * @throws IOException -
     *             IOError occur
     * @throws SoapRequestException
     *             Invalid soap document response was received
     */
    public Element requestModifyCalendar(String uid, Calendar c, String oldFolderId, String oldTags) throws IOException, SoapRequestException {
        if (!init()) {
            return null;
        }
        boolean task = c.getEvent() == null;
        Property folder = c.getCalendarContent().getFolder();
        String folderType = task ? FolderHolder.TASK : FolderHolder.APPOINTMENT;
        String folderName = null;
        if (folder != null) {
            folderName = folder.getPropertyValueAsString();
        }
        if (folderName == null || folderName.equals("")) {
            folderName = FolderHolder.DEFAULT_FOLDER;
        }
        String folderId = createFolderRequest(folderName, folderType);
        Document request = createZimbraDoc();
        Element body = soapHelper.getBody(request);
        Element calendarRequest = body.addElement(task ? ZConst.MODIFY_TASK_REQUEST : ZConst.MODIFY_APPOINTMENT_REQUEST, ZConst.URN_ZIMBRA_MAIL);
        {
            Element calendar = CalendarUtils.getInstance().asElement(c, documentFactory, ZConst.URN_ZIMBRA_MAIL, true);
            if (calendar.hasContent()) {
                calendarRequest.add(calendar);
            }
            calendarRequest.addAttribute(ZConst.A_REPLACE, "1");
            calendarRequest.addAttribute(ZConst.A_FORCE, "1");
            calendarRequest.addAttribute(ZConst.A_ID, uid);
        }
        String tagsStr = CalendarUtils.getTags(c);
        String tagsIds = createTagsRequest(tagsStr);
        Document response = sendRequest(request);
        body = soapHelper.getBody(response);
        Element callResponse = body.element(task ? ZConst.MODIFY_TASK_RESPONSE : ZConst.MODIFY_APPOINTMENT_RESPONSE);
        if (callResponse == null) {
            logger.error("Error occur, can't find correct response");
            writer.write(request);
            writer.write(response);
            throw new SoapRequestException();
        }
        ccontext.processContext(soapHelper.getContext(response));
        if (!init()) {
            return null;
        }
        request = createZimbraDoc();
        body = soapHelper.getBody(request);
        Element batch = body.addElement(ZConst.BATCH_REQUEST, ZConst.URN_ZIMBRA);
        if (!oldFolderId.equals(folderId)) {
            attachMoveItemRequest(batch, uid, folderId);
        }
        if (!oldTags.equals(tagsIds)) {
            attachTagItemRequest(batch, uid, tagsIds);
        }
        response = sendRequest(request);
        batch = soapHelper.getBody(response).element(ZConst.BATCH_RESPONSE);
        processFault(batch, request, response);
        ccontext.processContext(soapHelper.getContext(response));
        return callResponse;
    }

    public Element requestSyncronization(String lastToken) throws IOException, SoapRequestException {
        if (!init()) {
            return null;
        }
        Document request = createZimbraDoc();
        Element body = soapHelper.getBody(request);
        Element batch = body.addElement(ZConst.BATCH_REQUEST, ZConst.URN_ZIMBRA);
        Element calendarRequest = batch.addElement("SyncRequest", ZConst.URN_ZIMBRA_MAIL);
        {
            calendarRequest.addAttribute("token", lastToken);
            calendarRequest.addAttribute(ZConst.A_FOLDER, "10");
        }
        Document response = sendRequest(request);
        body = soapHelper.getBody(response);
        body = body.element(ZConst.BATCH_RESPONSE);
        Element callResponse = body.element("SyncResponse");
        if (callResponse == null) {
            logger.error("Error occur, can't find correct response");
            writer.write(request);
            writer.write(response);
            throw new SoapRequestException();
        }
        processFault(body, request, response);
        ccontext.processContext(soapHelper.getContext(response));
        return callResponse;
    }

    private boolean processFault(Element body, Document request, Document response) {
        List errors = body.elements("Fault");
        boolean error = false;
        for (int i = 0; i < errors.size(); i++) {
            try {
                throw new Throwable("Error");
            } catch (Throwable e) {
                logger.error("Soap batch fault result at the this line", e);
                writer.write(request, "    ", "FAILED REQUEST");
                writer.write(response, "    ", "FAILED RESPONSE");
                error = true;
            }
            Element errorEl = (Element) errors.get(i);
            Element reason = errorEl.element("Reason").element("Text");
            logger.error(reason.getText());
        }
        return error;
    }

    public boolean attachTagItemRequest(Element body, String itemId, String tagIds) {
        String[] tags = tagIds.split(",");
        boolean isEmpty = true;
        for (String tag : tags) {
            if (tag == null || tag.equals("") || tag.equals("null")) {
                continue;
            }
            Element createFolderRequest = body.addElement(ZConst.ITEM_ACTION_REQUEST, ZConst.URN_ZIMBRA_MAIL);
            Element folder = createFolderRequest.addElement(ZConst.E_ACTION);
            folder.addAttribute(ZConst.A_ID, itemId);
            folder.addAttribute(ZConst.E_TAG, tag);
            folder.addAttribute(ZConst.A_OPERATION, ZConst.OP_TAG);
            isEmpty = false;
        }
        return isEmpty;
    }

    public String getLastToken() {
        return ccontext.getChangeId();
    }

    public Element requestCreateNote(Note n) throws IOException, SoapRequestException {
        if (!init()) {
            return null;
        }
        Document request = createZimbraCall(ZConst.CREATE_NOTE_REQUEST, ZConst.URN_ZIMBRA_MAIL);
        String folderId = null;
        Element contactsRequest = (Element) soapHelper.getBody(request).elements().get(0);
        {
            Element note = NoteUtils.asElement(n, documentFactory, ZConst.URN_ZIMBRA_MAIL, false);
            if (note.hasContent()) {
                contactsRequest.add(note);
                note.addAttribute(ZConst.A_FOLDER, folderId);
            }
        }
        Document response = sendRequest(request);
        Element contactsResponse = soapHelper.getBody(response).element(ZConst.CREATE_NOTE_RESPONSE);
        ccontext.processContext(soapHelper.getContext(response));
        return contactsResponse;
    }

    public static void main(String[] args) throws ParserConfigurationException, IOException, SoapRequestException {
        System.setProperty("javax.net.ssl.trustStore", "E:/Work/_Project/Funambol/funambol-connector-zimbra/ssl-truststore/keystore");
        URL url = new URL("https://mail.korusconsulting.ru/service/soap/");
        System.setProperty("showsoap", "true");
        ZimbraPort soapPort = new ZimbraPort(url);
        Sync4jPrincipal principal = Sync4jPrincipal.createPrincipal(args[0], "kkkk");
        principal.setEncodedUserPwd(args[1]);
        Sync4jUser user = ((Sync4jPrincipal) principal).getUser();
        soapPort.requestAutorization(user);
        soapPort.requestAllContact(false);
    }

    public Note requestNoteById(String noteId) throws IOException, SoapRequestException {
        if (!init()) {
            return null;
        }
        Document request = createZimbraCall("GetNoteRequest", ZConst.URN_ZIMBRA_MAIL);
        Element contactsRequest = (Element) soapHelper.getBody(request).elements().get(0);
        {
            Element cn = documentFactory.createElement(ZConst.E_CN);
            {
                cn.addAttribute(ZConst.A_ID, noteId);
            }
            contactsRequest.add(cn);
            contactsRequest.addAttribute(ZConst.A_SYNC, "1");
        }
        Document response = sendRequest(request);
        Element contactsResponse = soapHelper.getBody(response).element("GetNoteResponse");
        ccontext.processContext(soapHelper.getContext(response));
        Element noteEl = contactsResponse.element("note");
        Note note = NoteUtils.asNote(noteEl);
        return note;
    }

    public Element requestModifyNote(String key, Note n, String oldFolderId) throws IOException, SoapRequestException {
        if (!init()) {
            return null;
        }
        Document request = createZimbraCall(ZConst.MODIFY_CONTACT_REQUEST, ZConst.URN_ZIMBRA_MAIL);
        Element body = soapHelper.getBody(request);
        Element callRequest = (Element) body.elements().get(0);
        {
            Element note = NoteUtils.asElement(n, documentFactory, ZConst.URN_ZIMBRA_MAIL, true);
            if (note.hasContent()) {
                note.addAttribute(ZConst.A_ID, key);
                callRequest.add(note);
            }
            callRequest.addAttribute(ZConst.A_REPLACE, "0");
            callRequest.addAttribute(ZConst.A_FORCE, "1");
        }
        Document response = sendRequest(request);
        body = soapHelper.getBody(response);
        Element callResponse = body.element(ZConst.MODIFY_CONTACT_RESPONSE);
        ccontext.processContext(soapHelper.getContext(response));
        return callResponse;
    }

    public Element requestIcal(String calendarId) throws IOException, SoapRequestException {
        if (!init()) {
            return null;
        }
        Document request = createZimbraDoc();
        Element body = soapHelper.getBody(request);
        Element batch = body.addElement(ZConst.BATCH_REQUEST, ZConst.URN_ZIMBRA);
        Element calendarRequest = batch.addElement("GetICalRequest", ZConst.URN_ZIMBRA_MAIL);
        {
            calendarRequest.addAttribute(ZConst.A_ID, calendarId);
            calendarRequest.addAttribute(ZConst.A_SYNC, "1");
        }
        Document response = sendRequest(request);
        body = soapHelper.getBody(response);
        body = body.element(ZConst.BATCH_RESPONSE);
        Element callResponse = body.element("GetICalResponse");
        return callResponse;
    }
}
