package com.hand.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.hand.utils.HttpUtil;
import com.hand.utils.MsgPrint;
import com.hand.utils.ProtocolContanst;

public class HCSMobileApp extends HttpServlet {

    /**
	 * 初始化servlet方法 <br>
	 * 
	 * @throws 将抛出ServletException
	 */
    public void init() throws ServletException {
        MsgPrint.showMsg("=====初始化servlet方法=====");
    }

    /**
	 * servlet的doPost方法. <br>
	 * 
	 * 接收客户端发送的Post请求.
	 * 
	 * @param request
	 *            客户端发送请求到服务器，通过该参数可以获取到客户端请求的资源（如请求数据、消
	 *            息头 内容等）
	 * @param response
	 *            服务器响应客户端，将服务器操作结果响应客户端
	 * @throws 将抛出ServletException
	 * @throws 将抛出IO流异常
	 */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/plain;charset=UTF-8");
        request.setCharacterEncoding("utf-8");
        HttpURLConnection httpConn = null;
        byte[] result = null;
        try {
            byte[] bytes = HttpUtil.getHttpURLReturnData(request);
            if (-1 == bytes.length || 23 > bytes.length) throw new Exception();
            MsgPrint.showMsg("========byte length" + bytes.length);
            String userTag = request.getParameter("userTag");
            String isEncrypt = request.getParameter("isEncrypt");
            URL httpurl = new URL(ProtocolContanst.TRANSFERS_URL + userTag + "&isEncrypt=" + isEncrypt);
            httpConn = (HttpURLConnection) httpurl.openConnection();
            httpConn.setDoOutput(true);
            httpConn.setRequestProperty("Content-Length", String.valueOf(bytes.length));
            OutputStream outputStream = httpConn.getOutputStream();
            outputStream.write(bytes);
            outputStream.close();
            InputStream is = httpConn.getInputStream();
            if (0 >= httpConn.getContentLength()) {
                throw new Exception();
            }
            byte[] resultBytes = new byte[httpConn.getContentLength()];
            byte[] tempByte = new byte[1024];
            int length = 0;
            int index = 0;
            while ((length = is.read(tempByte)) != -1) {
                System.arraycopy(tempByte, 0, resultBytes, index, length);
                index += length;
            }
            is.close();
            result = resultBytes;
        } catch (Exception e) {
        }
        ServletOutputStream sos = response.getOutputStream();
        if (null != result) {
            response.setContentLength(result.length);
            sos.write(result);
        } else {
            response.setContentLength(26);
            sos.write(new byte[] { 48, 48, 55, -23, 3, 56, 49, 54, 57, 55, 49, 51, 54, 72, 71, 52, 48, 1, 3, 3, 48, 48, 48, 48, 48, 48 });
        }
        sos.flush();
        sos.close();
    }

    /**
	 * servlet的doGet方法. <br>
	 * 
	 * 接收客户端发送的get方式请求.本方法将所有实现交与doPost方法进行处理
	 * 
	 * @param request
	 *            客户端发送请求到服务器，通过该参数可以获取到客户端请求的资源（如请求数据、消 息头内容等）
	 * @param response
	 *            服务器响应客户端，将服务器操作结果响应客户端
	 * @throws 将抛出ServletException
	 * @throws 将抛出IO流异常
	 */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.doPost(request, response);
    }

    /**
	 * 销毁servlet方法. <br>
	 */
    public void destroy() {
        super.destroy();
    }
}
