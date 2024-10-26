package net.solosky.maplefetion.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import net.solosky.maplefetion.FetionClient;
import net.solosky.maplefetion.FetionConfig;
import net.solosky.maplefetion.bean.User;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

/**
 *
 * 区域配置，部分配置和区域有关
 *
 *
 * @author solosky <solosky772@qq.com>
 *
 */
public class LocaleSetting {

    /**
	 * 配置的Dom文档
	 */
    private Document document;

    /**
	 * 配置是否被加载
	 */
    private boolean isLoaded;

    /**
	 * 默认的构造函数
	 */
    public LocaleSetting() {
        this.isLoaded = false;
        this.document = null;
    }

    /**
	 * 返回一个节点的文本
	 * @param path		路径
	 * @return
	 */
    public String getNodeText(String path) {
        if (this.document != null) {
            Element el = XMLHelper.find(this.document.getRootElement(), path);
            return el != null ? el.getText() : null;
        } else {
            return null;
        }
    }

    /**
	 * 获取自适应系统配置
	 * @param user
	 * @return
	 * @throws IOException
	 * @throws JDOMException
	 */
    public void load(User user) throws IOException, JDOMException {
        URL url = new URL(FetionConfig.getString("server.nav-system-uri"));
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setDoOutput(true);
        conn.addRequestProperty("User-Agent", "IIC2.0/PC " + FetionClient.PROTOCOL_VERSION);
        String accountType = null;
        if (user.getMobile() > 0) {
            accountType = "mobile-no=\"" + user.getMobile() + "\"";
        } else if (user.getFetionId() > 0) {
            accountType = "sid=\"" + user.getFetionId() + "\"";
        } else if (user.getEmail() != null) {
            accountType = "email=\"" + user.getEmail() + "\"";
        } else {
            throw new IllegalStateException("Invalid CMCC mobile number, FetionId or Registered Email.");
        }
        String content = "<config><user " + accountType + " /><client type=\"PC\" version=\"" + FetionClient.PROTOCOL_VERSION + "\" platform=\"W5.1\" /><servers version=\"0\" /><service-no version=\"0\" /><parameters version=\"0\" /><hints version=\"0\" /><http-applications version=\"0\" /><client-config version=\"0\" /><services version=\"0\" /></config>";
        OutputStream out = conn.getOutputStream();
        out.write(content.getBytes());
        out.flush();
        SAXBuilder builder = new SAXBuilder();
        this.document = builder.build(conn.getInputStream());
        this.isLoaded = true;
    }

    /**
	 * 检查是否是有效的配置，比如获取的配置里面没有SIPC服务器地址，说明用户输入的账号无效
	 * @return
	 */
    public boolean isValid() {
        return this.getNodeText("/config/servers/sipc-proxy") != null;
    }

    /***
	 * 从一个流中加载配置
	 * @param in
	 * @throws IOException 
	 * @throws JDOMException 
	 */
    public void load(InputStream in) throws JDOMException, IOException {
        SAXBuilder builder = new SAXBuilder();
        this.document = builder.build(in);
        this.isLoaded = true;
    }

    /**
	 * 是否被加载
	 * @return the isLoaded
	 */
    public boolean isLoaded() {
        return isLoaded;
    }
}
