package org.cofax.cms;

import java.util.*;
import java.io.*;
import java.net.*;
import javax.servlet.*;
import javax.servlet.http.*;
import org.cofax.*;
import org.cofax.cds.*;

public class CofaxToolsPreview extends HttpServlet {

    private static String listTemplate;

    private static String fileTemplate;

    private static String templatePath;

    private static String templateLoaderClass;

    private static TemplateLoader templateLoader;

    private static int intRdm = 0;

    private static String templateString = "";

    private static ArrayList templatesToDelete = new ArrayList();

    public static void preview(HashMap glossary, HttpServletResponse res) throws ServletException, IOException {
        String templateId = "";
        String pageId = "";
        try {
            Class c = Class.forName(CDSServlet.templateLoaderClass);
            templateLoader = (TemplateLoader) c.newInstance();
            templateLoader.setTemplateRoot(CDSServlet.templatePath);
            templateLoader.setDefaultIndex(CDSServlet.listTemplate);
            templateLoader.setDefaultObject(CDSServlet.fileTemplate);
        } catch (Exception e) {
            System.err.println("Error loading template loader class:");
            e.printStackTrace(System.err);
            boolean errorInitializing = true;
        }
        CofaxPage page = new CofaxPage();
        page.reset();
        PrintWriter out = res.getWriter();
        try {
            pageId = glossary.get("request:pageId").toString();
            templateLoader.setTemplateSearch(CDSServlet.templatePath + "/" + glossary.get("request:templateSearch").toString());
            templateId = templateLoader.choose(glossary.get("request:FILENAME").toString() + ".htm", "", ".htm");
            if (templateId.equals("")) {
                page.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                page.setErrorMsg("Error: Template not found to satisfy request.");
            }
            modifyTemplateToPreview(templateId);
            String suffix = templateId.substring(templateId.lastIndexOf(".") + 1);
            String templatePreview = "articlePreview" + intRdm + "." + suffix;
            String path = templateId.substring(0, templateId.lastIndexOf("/")) + "/" + templatePreview;
            templatesToDelete.add(path);
            templateString = replaceTagsInTemplate(templateString);
            File deletefile = new File(path);
            boolean delete = true;
            if (deletefile.exists()) {
                delete = deletefile.delete();
            }
            if (delete) {
                File newTemplate = new File(path);
                newTemplate.createNewFile();
                PrintWriter fileOutputStream = new PrintWriter(new FileWriter(path, true));
                fileOutputStream.println(templateString);
                fileOutputStream.close();
            } else {
                System.err.println("CofaxToolsPreview : Can not delete " + templatePreview);
            }
            String urlString = glossary.get("request:requestedUrl") + "?template=" + templatePreview + "&ITEMID=" + glossary.get("request:ITEMID");
            URL urlcnx = new URL(urlString);
            InputStream is = urlcnx.openStream();
            BufferedReader bf = new BufferedReader(new InputStreamReader(is));
            String line;
            PrintWriter status = res.getWriter();
            while ((line = bf.readLine()) != null) {
                status.print(line);
            }
            status.close();
            while (templatesToDelete.size() > 0) {
                try {
                    File deletefile2 = new File((String) templatesToDelete.get(0));
                    if (deletefile2.exists()) {
                        delete = deletefile2.delete();
                    }
                } catch (Exception e) {
                    System.err.println("CofaxToolsPreview : Exception:" + e);
                }
                templatesToDelete.remove(0);
            }
        } catch (Exception e) {
            System.err.println("CofaxToolsPreview : Exception:" + e);
        }
    }

    public static HashMap initglossary(String code, DataStore db, HttpServletRequest req, boolean articleSection) {
        HashMap ht = new HashMap();
        String tag, pathInfo;
        if (articleSection) {
            ht.put("ITEMID", code);
            tag = CofaxToolsDbUtils.fillTag(db, "getArticleByItemID");
        } else {
            ht.put("MAPPINGCODE", code);
            tag = CofaxToolsDbUtils.fillTag(db, "getSectionByMappingCode");
        }
        ht = CofaxToolsDbUtils.getNameValuePackageHash(db, ht, tag);
        HashMap glossary = new HashMap();
        glossary.put("request:SECTION", ht.get("SECTION"));
        glossary.put("request:SECTIONNAME", ht.get("SECTIONNAME"));
        glossary.put("request:PUBNAME", ht.get("PUBNAME"));
        if (articleSection) glossary.put("request:ITEMID", code);
        if (articleSection) glossary.put("request:FILENAME", ht.get("FILENAME"));
        Object tmp = req.getHeader("host");
        String hostName;
        if (tmp != null) {
            hostName = (String) tmp;
        } else {
            hostName = req.getServerName();
        }
        pathInfo = "/" + (String) glossary.get("request:PUBNAME") + "/" + (String) glossary.get("request:SECTIONNAME") + "/";
        if (articleSection) pathInfo = pathInfo + (String) glossary.get("request:FILENAME") + ".htm";
        String RequestURI = CofaxToolsServlet.aliasPath + pathInfo;
        String requestedUrl = req.getScheme() + "://" + hostName + RequestURI;
        glossary.put("request:pageId", pathInfo);
        glossary.put("request:requestedUrl", requestedUrl);
        glossary.put("request:pathInfo", pathInfo);
        glossary.put("request:templateSearch", (String) glossary.get("request:PUBNAME") + "/" + (String) glossary.get("request:SECTIONNAME"));
        return (glossary);
    }

    public static HashMap initglossary(String itemID, DataStore db, HttpServletRequest req) {
        return initglossary(itemID, db, req, true);
    }

    private static void modifyTemplateToPreview(String templateId) {
        CofaxPage template = templateLoader.load(templateId);
        templateString = template.toString();
        intRdm = (int) java.lang.Math.rint((double) java.lang.Math.random() * 100);
        String path = templateId.substring(0, templateId.lastIndexOf("/")) + "/";
        int done = 0;
        while (templateString.substring(done).indexOf("<%@ include file=") > -1) {
            String templateStringWork = templateString.substring(done);
            int i = templateStringWork.indexOf("<%@ include file=") + 18;
            templateStringWork = templateStringWork.substring(i);
            String subTemplate = templateStringWork.substring(0, templateStringWork.indexOf("\""));
            String suffix = subTemplate.substring(subTemplate.lastIndexOf(".") + 1);
            String subTemplateToReplace = subTemplate.substring(0, subTemplate.lastIndexOf(".")) + intRdm + "." + suffix;
            templateString = CofaxUtil.replace(templateString, "<%@ include file=\"" + subTemplate + "\"", "<%@ include file=\"" + subTemplateToReplace + "\"");
            saveRdmTemplate(intRdm, path + subTemplate);
            done += i;
        }
    }

    private static void saveRdmTemplate(int intRdm, String templatePath) {
        try {
            String suffix = templatePath.substring(templatePath.lastIndexOf(".") + 1);
            String filename = templatePath.substring(templatePath.lastIndexOf("/") + 1, templatePath.lastIndexOf("."));
            String templatePreview = filename + intRdm + "." + suffix;
            String path = templatePath.substring(0, templatePath.lastIndexOf("/")) + "/" + templatePreview;
            templatesToDelete.add(path);
            String templateString2 = replaceTagsInTemplateFile(templatePath);
            File deletefile = new File(path);
            boolean delete = true;
            if (deletefile.exists()) {
                delete = deletefile.delete();
            }
            if (delete) {
                File newTemplate = new File(path);
                newTemplate.createNewFile();
                PrintWriter fileOutputStream = new PrintWriter(new FileWriter(path, true));
                fileOutputStream.println(templateString2);
                fileOutputStream.close();
            } else {
                System.err.println("CofaxToolsPreview : Can not delete " + templatePreview);
            }
        } catch (Exception e) {
            System.err.println("CofaxToolsPreview : Error while processing template '" + templatePath + "' : " + e);
        }
    }

    private static String replaceTagsInTemplateFile(String templatePath) {
        CofaxPage template2 = templateLoader.load(templatePath);
        String templateString2 = template2.toString();
        return replaceTagsInTemplate(templateString2);
    }

    private static String replaceTagsInTemplate(String templateString) {
        String templateString2 = templateString;
        try {
            templateString2 = CofaxUtil.replace(templateString2, "getArticle", "getArticleTemp");
            templateString2 = CofaxUtil.replace(templateString2, "getSection", "getSectionTemp");
            templateString2 = CofaxUtil.replace(templateString2, "getRelatedLinks", "getRelatedLinksTemp");
        } catch (Exception e) {
            System.err.println("CofaxToolsPreview : replaceTagsInTemplate : '" + templatePath + "' : " + e);
        }
        return templateString2;
    }
}
