package com.kdev.qq.utils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import sun.util.logging.resources.logging;

/**
 * @author kick.smiles@gmail.com <br />
 *         Date : Dec 8, 2011
 * 
 */
@SuppressWarnings("unused")
public class ExpressUtils {

    private static final Log log = LogFactory.getLog(ExpressUtils.class);

    private static final long serialVersionUID = 1L;

    private final String EX_KEY = "gmbd9ksf96412ac3";

    private String number;

    private String expNumber;

    private String json;

    private final String Shentong = "Shentong";

    private final String LBWL = "Longbanwuliu";

    private final String TT = "Tiantian";

    private final String XF = "Xinfengwuliu";

    private final String Yibangwuliu = "Yibangwuliu";

    private final String Yuantong = "Yuantong";

    private final String Yunda = "Yunda";

    private final String Zhaijisong = "Zhaijisong";

    private final String zhongtong = "zhongtong";

    /**
	 * expNumber 快递名称ID number 快递编号
	 * 
	 * @param expNumber
	 * @param number
	 */
    public String getExp(String expinfo) {
        System.out.println(expinfo);
        try {
            String re = "";
            if (!"".equals(expinfo) && expinfo.split(" ").length == 3) {
                expNumber = expinfo.split(" ")[1].trim();
                number = expinfo.split(" ")[2].trim();
            } else {
                return "[FormatNumber error..] 格式错误";
            }
            StringBuffer sb = new StringBuffer("http://www.kuaidi100.com/api?");
            sb.append("id=").append(EX_KEY).append("&");
            sb.append("com=").append(expNumber).append("&");
            sb.append("nu=").append(number.trim()).append("&");
            sb.append("show=").append("3").append("&muti=1");
            URL url = new URL(sb.toString());
            HttpURLConnection connect = (HttpURLConnection) url.openConnection();
            InputStream is = connect.getInputStream();
            ByteArrayOutputStream outStream = new ByteArrayOutputStream();
            byte[] buff = new byte[256];
            int rc = 0;
            while ((rc = is.read(buff, 0, 256)) > 0) {
                outStream.write(buff, 0, rc);
            }
            byte[] b = outStream.toByteArray();
            outStream.close();
            is.close();
            connect.disconnect();
            String str = "";
            json = str = new String(b, "utf8");
            log.info("快递查询 res : " + json);
            return json;
        } catch (Exception e) {
            log.info("快递查询查询异常.... ");
            return "";
        }
    }

    public static void main(String[] args) {
    }
}
