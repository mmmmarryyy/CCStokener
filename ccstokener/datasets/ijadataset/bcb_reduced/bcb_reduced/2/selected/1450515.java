package ces.coffice.webmail.mailmodel.mail;

import java.io.*;
import java.net.*;
import java.util.*;
import ces.coffice.webmail.datamodel.vo.MailAccount;
import ces.coffice.webmail.util.*;
import org.htmlparser.Node;
import org.htmlparser.Parser;
import org.htmlparser.lexer.Page;
import org.htmlparser.lexer.nodes.TagNode;
import org.htmlparser.util.NodeIterator;
import org.htmlparser.util.NodeList;

public class MailBodyConvert {

    private long userId;

    private long mailId;

    private String resPath;

    private String currentPath;

    private SendMail mail;

    private ces.coral.log.Logger logger = new ces.coral.log.Logger(MailBodyConvert.class);

    private HashMap signleCid = new HashMap();

    private String inputEncode;

    /**
     * ���캯��ָ���û����ʼ�
     * @param userId long
     * @param mailId long
     */
    public MailBodyConvert(long userId, long mailId) throws Exception {
        this.userId = userId;
        this.mailId = mailId;
        PersonalConfig person = MemoryConstant.getInstance().getPersonalConfig(new Long(userId));
        this.inputEncode = person.getInputEncode();
        this.resPath = person.getUserDir() + "/" + "mailbox" + "/" + mailId + "/" + "res" + "/";
        recursiveDir(new File(resPath));
        String url = person.getUserUrl();
        if (url.charAt(url.length() - 1) == '/') {
            this.currentPath = person.getUserUrl() + "mailbox" + "/" + mailId + "/";
        } else {
            this.currentPath = person.getUserUrl() + "/" + "mailbox" + "/" + mailId + "/";
        }
    }

    /**
     * ������ԴĿ¼
     * @return String
     */
    public String getResPath() {
        if (resPath == null) {
            resPath = MemoryConstant.getInstance().getPersonalConfig(new Long(userId)).getUserDir() + "/" + mailId + "/" + "res" + "/";
        }
        return resPath;
    }

    /**
     * ������Դ�ļ���������Դ�ļ�·��
     * @param htmlCode String
     * @throws Exception
     * @return String
     */
    private String saveRes(URL url) throws Exception {
        if (url == null) return "";
        File file = new File(this.getResPath() + url.getFile());
        int i = 0;
        while (file.exists()) {
            file = new File(this.getResPath() + "res" + i + url.getFile());
            i++;
        }
        file.createNewFile();
        BufferedInputStream input = new BufferedInputStream(url.openStream());
        if (input == null) return "";
        BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(file));
        int read;
        while ((read = input.available()) != -1) {
            output.write(read);
            output.flush();
        }
        if (output != null) output.close();
        if (input != null) input.close();
        return "res" + "/" + file.getName();
    }

    /**
     * ת��html������Ե�ַΪָ����ʽ�ַ�
     */
    public String convertPath(String htmlCode, String formatedStr) throws Exception {
        if (htmlCode == null) return "";
        htmlCode = new String(htmlCode.getBytes(this.inputEncode), Page.DEFAULT_CHARSET);
        String htmlContent = "";
        Parser parser = Parser.createParser(htmlCode);
        parser.registerScanners();
        parser.setEncoding(this.inputEncode);
        NodeIterator nodeIterator = parser.elements();
        while (nodeIterator.hasMoreNodes()) {
            Node node = nodeIterator.nextNode();
            this.formatedPath(node, formatedStr);
            if (node != null) htmlContent += node.toHtml();
        }
        return htmlContent;
    }

    /**
     * ת��nodeΪĿ��node
     * @param node Node
     * @param formatedStr String
     * @return String
     */
    private Node formatedPath(Node node, String formatedStr) throws Exception {
        NodeList list = node.getChildren();
        if (list != null) {
            for (int i = 0; i < list.size(); i++) {
                formatedPath(list.elementAt(i), formatedStr);
            }
        }
        TagNode tagNode = null;
        try {
            tagNode = (TagNode) node;
        } catch (Exception ex) {
            return node;
        }
        String strTagName = "";
        strTagName = tagNode.getTagName();
        dealwithNode(tagNode, "background", formatedStr);
        dealwithNode(tagNode, "src", formatedStr);
        if (strTagName.toLowerCase().equals("link")) {
            dealwithNode(tagNode, "href", formatedStr);
        }
        if (strTagName.toLowerCase().equals("base")) {
            tagNode.removeAttribute("href");
        }
        if (strTagName.toLowerCase().equals("param")) {
            if (tagNode.getAttribute("name") != null && tagNode.getAttribute("name").toLowerCase().trim().equals("movie")) {
                dealwithNode(tagNode, "value", formatedStr);
            }
        }
        return tagNode;
    }

    /**
     * ����node���
     */
    private String dealwithNode(TagNode tagNode, String attributeName, String formatedStr) throws Exception {
        String attributeValue = "";
        attributeValue = tagNode.getAttribute(attributeName);
        String strFileTempName = null;
        if (attributeValue != null) {
            if (formatedStr.indexOf("saveres") >= 0) {
                if (attributeValue != null && attributeValue.indexOf(this.getCurrentPath()) != -1) {
                    attributeValue = ces.coral.lang.StringUtil.replaceAll(attributeValue, this.getCurrentPath(), "");
                    ;
                } else {
                    String path = this.saveRes(resPath, attributeValue);
                    if (path != null && path.length() > 0) {
                        attributeValue = "res" + "/" + path;
                    }
                }
                tagNode.setAttribute(attributeName, attributeValue);
            }
            if (formatedStr.indexOf("getabsolutepath") >= 0) {
                attributeValue = this.convertToAsolutePath(currentPath, attributeValue);
                tagNode.setAttribute(attributeName, attributeValue);
            }
            if (formatedStr.indexOf("cidtomail") >= 0) {
                if (attributeValue.indexOf("res/") != 0) return "";
                File file = new File(this.getResPath() + getFileName(attributeValue));
                if (file != null && file.isFile()) {
                    String cid = getCID(file.getName());
                    attributeValue = "cid:" + cid;
                    tagNode.setAttribute(attributeName, attributeValue);
                    if (this.signleCid.get(cid) == null) {
                        mail.setRelatedAttach(file, cid);
                        this.signleCid.put(cid, cid);
                    }
                }
            }
        }
        return strFileTempName;
    }

    private String getCID(String filename) {
        if (filename == null) filename = "gyb";
        return new ces.coral.encrypt.MD5().getMD5ofStr(filename) + filename.replace('.', 'g');
    }

    /**
     * ȡ�ļ��� ��ͬ��URLӦ��һ��
     * @param filename String
     * @return String
     */
    private String getSingleFileName(URL url) {
        if (url == null) return "";
        return (new ces.coral.encrypt.MD5().getMD5ofStr(url.getHost() + url.getFile())).substring(0, 5) + this.getFileName(url.getFile());
    }

    /**
     * ����Ե�ַת�ɾ�Ե�ַ
     */
    public String convertToAsolutePath(String currentPath, String value) throws Exception {
        currentPath = this.formatUrl(currentPath);
        if (value == null) return "";
        value = value.trim();
        if (value.indexOf("http:") == 0) {
            return value;
        }
        if (value.indexOf("/") == 0) {
            return this.getRootPath(currentPath) + value.substring(1, value.length() - 1);
        }
        String returnV = currentPath;
        while (value.indexOf("./") == 0) {
            value = value.substring(2, value.length());
        }
        if (value.indexOf("../") == 0) {
            while (value.indexOf("../") == 0) {
                value = value.substring(3, value.length());
                returnV = this.getParentPath(returnV);
            }
            return returnV + value;
        }
        return currentPath + value;
    }

    /**
     * ������Դ ������Դ���
     * @param path String
     * @return String
     */
    private String saveRes(String path, String url1) throws Exception {
        URL url = null;
        try {
            url = new URL(url1);
        } catch (MalformedURLException ex) {
            logger.debug("ץȡ��Դʧ��:" + url1);
        }
        if (url == null) return "";
        BufferedInputStream input = null;
        try {
            input = new BufferedInputStream(url.openStream());
        } catch (IOException ex1) {
            ex1.printStackTrace();
        }
        if (input == null) return "";
        File file = new File(this.getResPath() + this.getSingleFileName(url));
        if (file.exists()) return file.getName();
        try {
            file.createNewFile();
        } catch (IOException ex2) {
            System.out.println(file.getPath() + "   " + file.getName());
            ex2.printStackTrace();
        }
        BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(file));
        int read;
        while ((read = input.read()) != -1) {
            output.write(read);
            output.flush();
        }
        if (output != null) output.close();
        if (input != null) input.close();
        return file.getName();
    }

    /**
     * �õ���Ŀ¼
     * @param path String
     * @return String
     */
    private String getRootPath(String path) {
        if (path == null) return "";
        path = this.formatUrl(path);
        int p1 = path.indexOf("//");
        if (p1 < 0) return path;
        int p2 = path.indexOf("/", p1 + 2);
        if (p2 < 0) return path;
        return path.substring(0, p2 + 1);
    }

    /**
     *  ���ظ�Ŀ¼ ���� http://www.sina.com.cn/mail/mymail/
     * @param path String
     * @throws Exception
     * @return String
     */
    private String getParentPath(String path) throws Exception {
        if (path == null) return "";
        path = formatUrl(path);
        path = path.substring(0, path.length() - 1);
        int p1 = path.lastIndexOf("/");
        if (p1 < 0) return path;
        return path.substring(0, p1 + 1);
    }

    /**
     * ��ʽ��url
     * @param path String
     * @return String
     */
    private String formatUrl(String path) {
        if (path == null) path = "http://";
        if (path.charAt(path.length() - 1) != '/') path = path.concat("/");
        if (path.length() > 7) {
            String temp = ces.coral.lang.StringUtil.replaceAll(path.substring(7, path.length()), "//", "/");
            path = path.substring(0, 7) + temp;
        }
        return path;
    }

    /**
     * �����ļ���
     * @param args String[]
     * @throws Exception
     */
    private String getFileName(String filename) {
        if (filename == null) return null;
        int p1;
        if ((p1 = filename.lastIndexOf("/")) != -1) {
            int p2 = filename.indexOf("?", p1);
            if (p2 != -1 && p2 > p1) {
                return filename.substring(p1 + 1, p2);
            }
            return filename.substring(p1 + 1, filename.length());
        }
        int p2 = filename.indexOf("?", p1);
        if (p2 != -1) return filename.substring(p2);
        return filename;
    }

    public static final void main(String[] args) throws Exception {
        MailAccount mailAccount = new MailAccount();
        mailAccount.setReceiveServer("pop3.163.com");
        mailAccount.setSendServer("POP3");
        mailAccount.setProtocol("pop3");
        WebMail mail = new WebMail(mailAccount, true);
        mail.connect("gybing_2004", "gybing");
        Receive receive = mail.createReceive();
        if (receive.nextMessage()) System.out.println(receive.getMessageSubject());
        System.out.println(receive.getMessageContent());
    }

    public String getCurrentPath() {
        return currentPath;
    }

    public void setCurrentPath(String currentPath) {
        this.currentPath = currentPath;
    }

    /**
     * �жϵ�ǰ�ļ��ļ����Ƿ���ڣ������ڣ������ļ���
     * @param directory File
     * @throws Exception
     */
    private void recursiveDir(File directory) throws Exception {
        if (!directory.exists()) recursiveDir(directory.getParentFile()); else return;
        directory.mkdir();
    }

    public SendMail getMail() {
        return mail;
    }

    public void setMail(SendMail mail) {
        this.mail = mail;
    }
}
