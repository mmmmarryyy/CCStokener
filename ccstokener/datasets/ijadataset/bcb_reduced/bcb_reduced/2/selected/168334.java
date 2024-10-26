package com.yxl.ws.client.util;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import org.codehaus.xfire.client.Client;
import org.codehaus.xfire.transport.http.HttpTransport;

public class XfireClientUtil {

    /**
	 * Description: 调用webservice
	 * 
	 * @param wsdl
	 *            访问路径
	 * @param method
	 *            要调用的方法
	 * @param parms
	 *            传入的参数
	 * @return 服务器返回信息 Copyright 深圳太极软件公司
	 * @author 杨雪令
	 */
    public Object[] connService(String wsdl, String method, Object[] parms) {
        Object[] results = null;
        try {
            URL url = new URL(wsdl);
            HttpURLConnection httpConnection;
            httpConnection = (HttpURLConnection) url.openConnection();
            httpConnection.connect();
            Client client = new Client(httpConnection.getInputStream(), null);
            results = client.invoke(method, parms);
        } catch (MalformedURLException e) {
            System.out.println("service访问路径有误");
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("连接service失败，可能是服务器已经关闭");
            e.printStackTrace();
        } catch (Exception e) {
            String[] strError = { e.getMessage() };
            if (results == null) results = strError;
            System.out.println(e.getMessage());
        }
        return results;
    }

    /**
	 * @param args
	 */
    public static void main(String[] args) {
        XfireClientUtil client = new XfireClientUtil();
        Object[] rs = null;
        String wsdl = "http://127.0.0.1/services/SyncService?wsdl";
        String str1 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "<root>" + "<record>" + "<userName>傻B2[webservice]</userName>" + "<userCode>100089</userCode>" + "<personCode>sb</personCode>" + "<password>123</password>" + "<caID></caID>" + "<duty>科长</duty>" + "<sex>1</sex>" + "<mobile>13812345678</mobile>" + "<email>zhangs@gzs.com</email>" + "<orgCode>098321</orgCode>" + "</record>" + "</root>";
        String str2 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "<root>" + "<record>" + "<id>2010558545505</id>" + "<orgCode>117040115</orgCode>" + "<abbrName>市测试局[service测试]</abbrName>" + "<name>厦门市测试局</name>" + "<xzqhCode>430300</xzqhCode>" + "<remark>备注</remark>" + "<sortNO>2</sortNO>" + "<sign>dgsgsj-</sign>" + "</record>" + "</root>";
        String str3 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + "<root>" + "<record>" + "<id>10004</id>" + "<name>南山区</name>" + "<fullName>深圳市南山区</fullName>" + "<sign>szlhq</sign>" + "<xzqhID>440304</xzqhID>" + "<parentID>440300</parentID>" + "<remark>---</remark>" + "<layer>3</layer>" + "<sortNO>1</sortNO>" + "</record>" + "</root>";
        rs = client.connService("http://127.0.0.1/xfire+spring_1/soap/TestWS?wsdl", "sayHelloWorld", new Object[] { "god" });
        System.out.println(rs[0]);
    }
}
