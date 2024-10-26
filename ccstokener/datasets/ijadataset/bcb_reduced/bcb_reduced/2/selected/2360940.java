package org.roller.presentation.weblog.actions;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.struts.action.ActionError;
import org.apache.struts.action.ActionErrors;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionMessage;
import org.apache.struts.action.ActionMessages;
import org.apache.struts.actions.DispatchAction;
import org.roller.RollerException;
import org.roller.model.RollerSpellCheck;
import org.roller.model.UserManager;
import org.roller.model.WeblogManager;
import org.roller.pojos.CommentData;
import org.roller.pojos.UserData;
import org.roller.pojos.WeblogEntryData;
import org.roller.pojos.WebsiteData;
import org.roller.presentation.MainPageAction;
import org.roller.presentation.RollerContext;
import org.roller.presentation.RollerRequest;
import org.roller.presentation.RollerSession;
import org.roller.presentation.pagecache.PageCache;
import org.roller.presentation.velocity.PageHelper;
import org.roller.presentation.weblog.formbeans.WeblogEntryFormEx;
import org.roller.presentation.weblog.search.IndexManager;
import org.roller.presentation.weblog.search.operations.AddEntryOperation;
import org.roller.presentation.weblog.search.operations.RemoveEntryOperation;
import org.roller.presentation.xmlrpc.RollerXmlRpcClient;
import org.roller.util.Utilities;
import com.swabunga.spell.event.SpellCheckEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * Supports Weblog Entry form actions edit, remove, update, etc.
 *
 * @struts.action name="weblogEntryFormEx" path="/weblog"
 *     scope="request" parameter="method"
 *  
 * @struts.action-forward name="weblogEdit.page" path="/weblog/WeblogEdit.jsp"
 * @struts.action-forward name="weblogEntryRemove.page" path="/weblog/WeblogEntryRemove.jsp"
 */
public final class WeblogEntryFormAction extends DispatchAction {

    private static Log mLogger = LogFactory.getFactory().getInstance(WeblogEntryFormAction.class);

    /**
     * Allow user to create a new weblog entry.
     */
    public ActionForward create(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        ActionForward forward = mapping.findForward("weblogEdit.page");
        try {
            RollerRequest rreq = RollerRequest.getRollerRequest(request);
            if (rreq.isUserAuthorizedToEdit()) {
                WeblogEntryFormEx form = (WeblogEntryFormEx) actionForm;
                form.initNew(request, response);
                request.setAttribute("model", new WeblogEntryPageModel(request, response, mapping, (WeblogEntryFormEx) actionForm, WeblogEntryPageModel.EDIT_MODE));
            } else {
                forward = mapping.findForward("access-denied");
            }
        } catch (Exception e) {
            request.getSession().getServletContext().log("ERROR", e);
            throw new ServletException(e);
        }
        return forward;
    }

    /**
     * Allow user to edit a weblog entry.
     */
    public ActionForward edit(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        ActionForward forward = mapping.findForward("weblogEdit.page");
        try {
            RollerRequest rreq = RollerRequest.getRollerRequest(request);
            if (rreq.isUserAuthorizedToEdit()) {
                WeblogEntryData entry = rreq.getWeblogEntry();
                WeblogEntryFormEx form = (WeblogEntryFormEx) actionForm;
                form.copyFrom(entry, request.getLocale());
                request.setAttribute("model", new WeblogEntryPageModel(request, response, mapping, form, WeblogEntryPageModel.EDIT_MODE));
            } else {
                forward = mapping.findForward("access-denied");
            }
        } catch (Exception e) {
            request.getSession().getServletContext().log("ERROR", e);
            throw new ServletException(e);
        }
        return forward;
    }

    public ActionForward preview(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        WeblogEntryFormEx form = (WeblogEntryFormEx) actionForm;
        if (form.getId() == null) {
            save(mapping, actionForm, request, response);
        }
        return display(WeblogEntryPageModel.PREVIEW_MODE, mapping, actionForm, request, response);
    }

    public ActionForward returnToEditMode(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        return display(WeblogEntryPageModel.EDIT_MODE, mapping, actionForm, request, response);
    }

    private ActionForward display(WeblogEntryPageModel.PageMode mode, ActionMapping mapping, ActionForm actionForm, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        ActionForward forward = mapping.findForward("weblogEdit.page");
        try {
            RollerRequest rreq = RollerRequest.getRollerRequest(request);
            if (rreq.isUserAuthorizedToEdit()) {
                request.setAttribute("model", new WeblogEntryPageModel(request, response, mapping, (WeblogEntryFormEx) actionForm, mode));
            } else {
                forward = mapping.findForward("access-denied");
            }
        } catch (Exception e) {
            request.getSession().getServletContext().log("ERROR", e);
            throw new ServletException(e);
        }
        return forward;
    }

    /**
     * Saves weblog entry and flushes page cache so that new entry will appear 
     * on users weblog page.
     */
    public ActionForward save(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        ActionForward forward = mapping.findForward("weblogEdit.page");
        try {
            RollerRequest rreq = RollerRequest.getRollerRequest(request);
            if (rreq.isUserAuthorizedToEdit()) {
                UserManager userMgr = rreq.getRoller().getUserManager();
                UserData user = rreq.getUser();
                WebsiteData site = userMgr.getWebsite(user.getUserName());
                WeblogEntryFormEx wf = (WeblogEntryFormEx) actionForm;
                if (wf.getAllowComments() == null) {
                    wf.setAllowComments(Boolean.FALSE);
                }
                if (wf.getRightToLeft() == null) {
                    wf.setRightToLeft(Boolean.FALSE);
                }
                if (wf.getPinnedToMain() == null) {
                    wf.setPinnedToMain(Boolean.FALSE);
                }
                if (wf.getPublishEntry() == null) {
                    wf.setPublishEntry(Boolean.FALSE);
                }
                WeblogEntryData entry = new WeblogEntryData();
                wf.copyTo(entry, request.getLocale());
                entry.setWebsite(site);
                entry.setUpdateTime(new Timestamp(new Date().getTime()));
                entry.save();
                wf.copyFrom(entry, request.getLocale());
                rreq.getRoller().commit();
                reindexEntry(entry);
                request.setAttribute(RollerRequest.WEBLOGENTRYID_KEY, entry.getId());
                PageCache.removeFromCache(request, user);
                MainPageAction.flushMainPageCache();
                HttpSession session = request.getSession(true);
                session.removeAttribute("spellCheckEvents");
                session.removeAttribute("entryText");
                if (request.getParameter("sendWeblogsPing") == null) {
                    wf.setSendWeblogsPing(Boolean.FALSE);
                }
                sendWeblogsDotComPing(request, user, wf, entry);
                request.setAttribute("model", new WeblogEntryPageModel(request, response, mapping, (WeblogEntryFormEx) actionForm, WeblogEntryPageModel.EDIT_MODE));
                ActionMessages uiMessages = new ActionMessages();
                uiMessages.add(null, new ActionMessage("weblogEdit.changesSaved"));
                saveMessages(request, uiMessages);
            } else {
                forward = mapping.findForward("access-denied");
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }
        return forward;
    }

    /**
     * Ping the weblogs.com server.  Notifies them of blog updates.
     * Several other sites reference weblogs.com for updates.
     * 
     * @param request
     * @param user
     * @param wf
     * @param entry
     */
    private void sendWeblogsDotComPing(HttpServletRequest request, UserData user, WeblogEntryFormEx wf, WeblogEntryData entry) {
        if (wf.getSendWeblogsPing() != null && wf.getSendWeblogsPing().booleanValue()) {
            RollerContext rctx = RollerContext.getRollerContext(request);
            String blogUrl = Utilities.escapeHTML(rctx.getAbsoluteContextUrl(request) + "/page/" + user.getUserName());
            String blogName = entry.getWebsite().getName();
            String result = RollerXmlRpcClient.sendWeblogsPing(blogUrl, blogName);
            if (result != null) {
                request.getSession().setAttribute(RollerSession.STATUS_MESSAGE, "Weblogs.com Response: " + result);
            }
        }
    }

    /**
     * Responds to request to remove weblog entry. Forwards user to page
     * that presents the 'are you sure?' question.
     */
    public ActionForward removeOk(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        ActionForward forward = mapping.findForward("weblogEntryRemove.page");
        try {
            RollerRequest rreq = RollerRequest.getRollerRequest(request);
            if (rreq.isUserAuthorizedToEdit()) {
                WeblogEntryFormEx wf = (WeblogEntryFormEx) actionForm;
                WeblogEntryData wd = rreq.getRoller().getWeblogManager().retrieveWeblogEntry(wf.getId());
                wf.copyFrom(wd, request.getLocale());
                if (wd == null || wd.getId() == null) {
                    throw new NullPointerException("Unable to find WeblogEntry for " + request.getParameter(RollerRequest.WEBLOGENTRYID_KEY));
                }
            } else {
                forward = mapping.findForward("access-denied");
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }
        return forward;
    }

    /**
     * Responds to request from the 'are you sure you want to remove?' page.
     * Removes the specified weblog entry and flushes the cache.
     */
    public ActionForward remove(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        try {
            RollerRequest rreq = RollerRequest.getRollerRequest(request);
            if (rreq.isUserAuthorizedToEdit()) {
                WeblogManager mgr = rreq.getRoller().getWeblogManager();
                WeblogEntryData wd = mgr.retrieveWeblogEntry(request.getParameter("id"));
                UserData user = rreq.getUser();
                PageCache.removeFromCache(request, user);
                wd.setPublishEntry(Boolean.FALSE);
                reindexEntry(wd);
                wd.remove();
                rreq.getRoller().commit();
                ActionMessages uiMessages = new ActionMessages();
                uiMessages.add(null, new ActionMessage("weblogEdit.entryRemoved"));
                saveMessages(request, uiMessages);
            } else {
                return mapping.findForward("access-denied");
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }
        actionForm = new WeblogEntryFormEx();
        request.setAttribute(mapping.getName(), actionForm);
        return create(mapping, actionForm, request, response);
    }

    public ActionForward correctSpelling(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        try {
            RollerRequest rreq = RollerRequest.getRollerRequest(request);
            if (rreq.isUserAuthorizedToEdit()) {
                HttpSession session = request.getSession(true);
                WeblogEntryFormEx wf = (WeblogEntryFormEx) actionForm;
                if (wf.getReplacementWords() != null && wf.getReplacementWords().length > 0) {
                    String[] replacementWords = wf.getReplacementWords();
                    StringBuffer entryText = new StringBuffer(wf.getText());
                    ArrayList events = (ArrayList) session.getAttribute("spellCheckEvents");
                    SpellCheckEvent event = null;
                    String oldWord = null;
                    String newWord = null;
                    int start = -1;
                    int end = -1;
                    int count = replacementWords.length;
                    for (ListIterator it = events.listIterator(events.size()); it.hasPrevious(); ) {
                        event = (SpellCheckEvent) it.previous();
                        oldWord = event.getInvalidWord();
                        newWord = replacementWords[--count];
                        if (!oldWord.equals(newWord)) {
                            start = event.getWordContextPosition();
                            end = start + oldWord.length();
                            entryText.replace(start, end, newWord);
                        }
                    }
                    wf.setText(entryText.toString());
                    return save(mapping, wf, request, response);
                }
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }
        return mapping.findForward("access-denied");
    }

    public ActionForward spellCheck(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        ActionForward forward = mapping.findForward("weblogEdit.page");
        try {
            RollerRequest rreq = RollerRequest.getRollerRequest(request);
            if (rreq.isUserAuthorizedToEdit()) {
                HttpSession session = request.getSession(true);
                WeblogEntryFormEx wf = (WeblogEntryFormEx) actionForm;
                if (wf.getId() == null) {
                    save(mapping, actionForm, request, response);
                }
                ArrayList words = RollerSpellCheck.getSpellingErrors(wf.getText());
                session.setAttribute("spellCheckEvents", words);
                request.setAttribute("model", new WeblogEntryPageModel(request, response, mapping, (WeblogEntryFormEx) actionForm, WeblogEntryPageModel.SPELL_MODE, words));
            } else {
                forward = mapping.findForward("access-denied");
            }
        } catch (Exception e) {
            throw new ServletException(e);
        }
        return forward;
    }

    /**
     * Update selected comments: delete and/or mark as spam.
     */
    public ActionForward updateComments(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        ActionForward forward = mapping.findForward("weblogEdit.page");
        ActionErrors errors = new ActionErrors();
        RollerRequest rreq = RollerRequest.getRollerRequest(request);
        try {
            if (rreq.isUserAuthorizedToEdit()) {
                WeblogEntryData wd = rreq.getWeblogEntry();
                if (wd == null || wd.getId() == null) {
                    throw new NullPointerException("Unable to find WeblogEntry for " + request.getParameter(RollerRequest.WEBLOGENTRYID_KEY));
                }
                WeblogEntryFormEx form = (WeblogEntryFormEx) actionForm;
                WeblogManager mgr = rreq.getRoller().getWeblogManager();
                String[] deleteIds = form.getDeleteComments();
                if (deleteIds != null && deleteIds.length > 0) {
                    mgr.removeComments(deleteIds);
                }
                List comments = mgr.getComments(wd.getId(), false);
                if (form.getSpamComments() != null) {
                    List spamIds = Arrays.asList(form.getSpamComments());
                    Iterator it = comments.iterator();
                    while (it.hasNext()) {
                        CommentData comment = (CommentData) it.next();
                        if (spamIds.contains(comment.getId())) {
                            comment.setSpam(Boolean.TRUE);
                        } else {
                            comment.setSpam(Boolean.FALSE);
                        }
                        comment.save();
                    }
                }
                rreq.getRoller().commit();
                reindexEntry(wd);
                request.setAttribute("model", new WeblogEntryPageModel(request, response, mapping, (WeblogEntryFormEx) actionForm, WeblogEntryPageModel.EDIT_MODE));
            } else {
                forward = mapping.findForward("access-denied");
            }
        } catch (Exception e) {
            forward = mapping.findForward("error");
            errors.add(ActionErrors.GLOBAL_ERROR, new ActionError("error.edit.comment", e.toString()));
            saveErrors(request, errors);
            mLogger.error(getResources(request).getMessage("error.edit.comment") + e.toString(), e);
        }
        return forward;
    }

    /**
    *
    */
    public ActionForward sendTrackback(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request, HttpServletResponse response) throws RollerException {
        ActionForward forward = mapping.findForward("weblogEdit.page");
        ActionErrors errors = new ActionErrors();
        WeblogEntryData entry = null;
        try {
            RollerRequest rreq = RollerRequest.getRollerRequest(request);
            if (rreq.isUserAuthorizedToEdit()) {
                WeblogEntryFormEx form = (WeblogEntryFormEx) actionForm;
                String entryid = form.getId();
                if (entryid == null) {
                    entryid = request.getParameter(RollerRequest.WEBLOGENTRYID_KEY);
                }
                RollerContext rctx = RollerContext.getRollerContext(request);
                WeblogManager wmgr = rreq.getRoller().getWeblogManager();
                entry = wmgr.retrieveWeblogEntry(entryid);
                String title = entry.getTitle();
                PageHelper pageHelper = PageHelper.createPageHelper(request, response);
                pageHelper.setSkipFlag(true);
                String excerpt = pageHelper.renderPlugins(entry);
                excerpt = StringUtils.left(Utilities.removeHTML(excerpt), 255);
                String url = rctx.createEntryPermalink(entry, request, true);
                String blog_name = entry.getWebsite().getName();
                if (form.getTrackbackUrl() != null) {
                    try {
                        String data = URLEncoder.encode("title", "UTF-8") + "=" + URLEncoder.encode(title, "UTF-8");
                        data += ("&" + URLEncoder.encode("excerpt", "UTF-8") + "=" + URLEncoder.encode(excerpt, "UTF-8"));
                        data += ("&" + URLEncoder.encode("url", "UTF-8") + "=" + URLEncoder.encode(url, "UTF-8"));
                        data += ("&" + URLEncoder.encode("blog_name", "UTF-8") + "=" + URLEncoder.encode(blog_name, "UTF-8"));
                        URL tburl = new URL(form.getTrackbackUrl());
                        URLConnection conn = tburl.openConnection();
                        conn.setDoOutput(true);
                        OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
                        wr.write(data);
                        wr.flush();
                        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        String line;
                        StringBuffer resultBuff = new StringBuffer();
                        while ((line = rd.readLine()) != null) {
                            resultBuff.append(Utilities.escapeHTML(line, true));
                            resultBuff.append("<br />");
                        }
                        ActionMessages resultMsg = new ActionMessages();
                        resultMsg.add(ActionMessages.GLOBAL_MESSAGE, new ActionMessage("weblogEdit.trackbackResults", resultBuff));
                        saveMessages(request, resultMsg);
                        wr.close();
                        rd.close();
                    } catch (IOException e) {
                        errors.add(ActionErrors.GLOBAL_ERROR, new ActionError("error.trackback", e));
                    }
                } else {
                    errors.add(ActionErrors.GLOBAL_ERROR, new ActionError("error.noTrackbackUrlSpecified"));
                }
                form.setTrackbackUrl(null);
            } else {
                forward = mapping.findForward("access-denied");
            }
        } catch (Exception e) {
            mLogger.error(e);
            String msg = e.getMessage();
            if (msg == null) {
                msg = e.getClass().getName();
            }
            errors.add(ActionErrors.GLOBAL_ERROR, new ActionError("error.general", msg));
        }
        if (!errors.isEmpty()) {
            saveErrors(request, errors);
        }
        request.setAttribute("model", new WeblogEntryPageModel(request, response, mapping, (WeblogEntryFormEx) actionForm, WeblogEntryPageModel.EDIT_MODE));
        return forward;
    }

    public ActionForward cancel(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        return (mapping.findForward("weblogEdit"));
    }

    public ActionForward unspecified(ActionMapping mapping, ActionForm actionForm, HttpServletRequest request, HttpServletResponse response) throws Exception {
        return create(mapping, actionForm, request, response);
    }

    /**
     * Attempts to remove the Entry from the Lucene index and
     * then re-index the Entry if it is Published.  If the Entry
     * is being deleted then mark it published = false.
     * @param entry
     */
    private void reindexEntry(WeblogEntryData entry) {
        IndexManager manager = RollerContext.getRollerContext(RollerContext.getServletContext()).getIndexManager();
        RemoveEntryOperation removeOp = new RemoveEntryOperation(entry);
        manager.executeIndexOperationNow(removeOp);
        if (entry.getPublishEntry() == Boolean.TRUE) {
            AddEntryOperation addEntry = new AddEntryOperation(entry);
            manager.scheduleIndexOperation(addEntry);
        }
    }
}
