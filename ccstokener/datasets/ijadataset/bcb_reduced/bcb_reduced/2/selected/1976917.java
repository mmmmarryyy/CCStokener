package com.apelon.apps.dts.treebrowser.classify.actions;

import java.io.*;
import java.util.*;
import java.net.*;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpServletResponse;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.util.MessageResources;
import com.apelon.apps.dts.treebrowser.logon.actions.LogonUtilities;
import com.apelon.apps.dts.treebrowser.classify.beans.*;
import com.apelon.apps.dts.treebrowser.classify.application.builders.ResultBuilder;
import com.apelon.apps.dts.treebrowser.classify.application.fetchers.*;
import com.apelon.apps.dts.treebrowser.classify.application.parsers.ClassifyResultParser;
import com.apelon.dts.client.concept.SearchQuery;
import com.apelon.dts.client.concept.NavQuery;
import com.apelon.dts.client.concept.OntylogConceptQuery;
import com.apelon.dts.client.association.AssociationQuery;
import com.apelon.dts.client.namespace.NamespaceQuery;
import com.apelon.dts.client.match.MatchQuery;
import com.apelon.apps.dts.treebrowser.reporting.beans.ReportingBean;
import com.apelon.apps.dts.treebrowser.resources.BrowserUtilities;
import com.apelon.common.log4j.Categories;
import com.apelon.common.log4j.StackTracePrinter;
import com.apelon.common.xml.*;

public final class ClassifyAdderAction extends Action {

    /**
   * Gets concept display bean parameters from request, generates a formatted XML string.
   * <p>
   *
   * @param mapping       the ActionMapping used to select this instance
   * @param form          the optional ActionForm bean for this request (if any)
   * @param request       the HTTP request we are processing
   * @param response      the HTTP response we are creating
   *
   * @exception           IOException if an input/output error occurs
   * @exception           ServletException if a servlet exception occurs
   *
   * @return mapping.findForward(status)  the search outcome such as "success" or "fail"
   */
    public ActionForward perform(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        String status = null;
        HttpSession session = request.getSession();
        ReportingBean reportingBean = null;
        NewConceptBean conceptBean = null;
        String conceptName = null;
        String primitive = null;
        MessageResources errorMessages = MessageResources.getMessageResources("com.apelon.apps.dts.treebrowser.resources.error_messages");
        LogonUtilities logonUtilities = new LogonUtilities();
        if (logonUtilities.checkSession(session, status, reportingBean, errorMessages)) {
            reportingBean = (ReportingBean) session.getAttribute("reportingBean");
            if ((reportingBean.getMessageHtml().indexOf("Classify")) == -1) {
                reportingBean.setReportingHtml("");
            }
            conceptBean = (NewConceptBean) session.getAttribute("newConceptEntity");
            conceptName = request.getParameter("conceptName");
            primitive = request.getParameter("primitive");
            if (primitive == null) {
                primitive = "false";
            }
            conceptBean.setConceptName(conceptName);
            conceptBean.setPrimitive(primitive);
            String namespace = conceptBean.getConceptNamespace();
            if (namespace == null) {
                reportingBean.setReportingHtml(errorMessages.getMessage("classify_3"));
                session.setAttribute("reportingBean", reportingBean);
                status = "classify_fail";
                return (mapping.findForward(status));
            }
            String[] roleMods = request.getParameterValues("some_or_all");
            String[] roleGrps = request.getParameterValues("role_group");
            Vector roles = conceptBean.getRoles();
            RoleBean[] roleCons = new RoleBean[roles.size()];
            roles.copyInto(roleCons);
            if (roleMods != null && roleGrps != null && roleCons != null) {
                if (roleCons.length == roleMods.length && roleCons.length == roleGrps.length) {
                    for (int i = 0; i < roleCons.length; i++) {
                        roleCons[i].setSomeOrAll(roleMods[i]);
                        roleCons[i].setRoleGroup(roleGrps[i]);
                    }
                }
            }
            session.setAttribute("newConceptEntity", conceptBean);
            NewConceptXMLDisplayBean xmlBean = new NewConceptXMLDisplayBean();
            xmlBean.setNewConceptXml(conceptBean);
            String data = BrowserUtilities.encodeUrl("conceptXml") + "=" + BrowserUtilities.encodeUrl(xmlBean.getNewConceptXml()) + "&" + BrowserUtilities.encodeUrl("nameSpace") + "=" + BrowserUtilities.encodeUrl(namespace);
            XMLPropertyHandler configPh = new XMLPropertyHandler("dtsbrowserclassify.xml");
            Properties configProps = configPh.getProps();
            String urlString = configProps.getProperty("url");
            URL url = new URL(urlString);
            URLConnection conn = url.openConnection();
            conn.setDoOutput(true);
            OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
            wr.write(data);
            wr.flush();
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line;
            StringBuffer buf = new StringBuffer();
            while ((line = rd.readLine()) != null) {
                buf.append(line);
            }
            String resultXml = buf.toString();
            wr.close();
            rd.close();
            ClassifiedConceptBean ccBean = new ClassifiedConceptBean();
            ClassifyResultParser parser = new ClassifyResultParser(resultXml);
            ResultBuilder rbuilder = new ResultBuilder();
            rbuilder.setConceptBean(ccBean);
            rbuilder.setParser(parser);
            rbuilder.setNamespace(namespace);
            try {
                rbuilder.buildResult();
                ccBean = rbuilder.getConceptBean();
                ClassifiedConceptDisplayBean displayBean = new ClassifiedConceptDisplayBean();
                displayBean.setNewConceptHtml(ccBean);
                session.setAttribute("classifyResult", ccBean);
                session.setAttribute("classifyDisplayResult", displayBean);
                status = "success";
            } catch (Exception e) {
                Categories.dataServer().error(StackTracePrinter.getStackTrace(e));
                reportingBean.setReportingHtml(errorMessages.getMessage("classify_1") + e.getMessage());
                session.setAttribute("reportingBean", reportingBean);
                status = "classify_fail";
            }
        }
        return (mapping.findForward(status));
    }

    private String getKind(String namespace) {
        String kind = null;
        if (namespace.equalsIgnoreCase("SNOMED CT")) {
            kind = "SNOMED_kind";
        } else if (namespace.equalsIgnoreCase("ICD-9-CM")) {
            kind = "ICD_Kind";
        }
        return kind;
    }
}
