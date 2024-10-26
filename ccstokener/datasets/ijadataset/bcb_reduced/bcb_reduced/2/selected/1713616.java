package com.hand.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.hand.utils.HttpUtil;
import com.hand.utils.MsgPrint;
import com.hand.utils.ProtocolContanst;

public class TestServlet extends HttpServlet {

    /**
	 * Constructor of the object.
	 */
    public TestServlet() {
        super();
    }

    /**
	 * Initialization of the servlet. <br>
	 *
	 * @throws ServletException if an error occurs
	 */
    public void init() throws ServletException {
    }

    /**
	 * The doGet method of the servlet. <br>
	 *
	 * This method is called when a form has its tag value method equals to get.
	 * 
	 * @param request the request send by the client to the server
	 * @param response the response send by the server to the client
	 * @throws ServletException if an error occurred
	 * @throws IOException if an error occurred
	 */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        this.doPost(request, response);
    }

    /**
	 * The doPost method of the servlet. <br>
	 *
	 * This method is called when a form has its tag value method equals to post.
	 * 
	 * @param request the request send by the client to the server
	 * @param response the response send by the server to the client
	 * @throws ServletException if an error occurred
	 * @throws IOException if an error occurred
	 */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setContentType("text/plain;charset=UTF-8");
        request.setCharacterEncoding("utf-8");
        HttpURLConnection httpURLConnection = null;
        byte[] result = null;
        try {
            byte[] bytes = HttpUtil.getHttpURLReturnData(request);
            if (-1 == bytes.length || 23 > bytes.length) throw new Exception();
            String userTag = request.getParameter("userTag");
            String isEncrypt = request.getParameter("isEncrypt");
            URL httpurl = new URL(ProtocolContanst.TRANSFERS_URL + userTag + "&isEncrypt=" + isEncrypt);
            httpURLConnection = (HttpURLConnection) httpurl.openConnection();
            httpURLConnection.setDoOutput(true);
            httpURLConnection.setRequestProperty("Content-Length", String.valueOf(bytes.length));
            OutputStream outputStream = httpURLConnection.getOutputStream();
            outputStream.write(bytes);
            outputStream.close();
            MsgPrint.showMsg("接收到字节的长度=" + httpURLConnection.getContentLength());
            if (0 >= httpURLConnection.getContentLength()) {
                throw new Exception();
            }
            InputStream is = httpURLConnection.getInputStream();
            byte[] resultBytes = new byte[httpURLConnection.getContentLength()];
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
            e.printStackTrace();
        }
        ServletOutputStream sos = response.getOutputStream();
        response.setContentLength(result.length);
        sos.write(result);
        sos.flush();
        sos.close();
    }

    /**
	 * Destruction of the servlet. <br>
	 */
    public void destroy() {
        super.destroy();
    }

    private static byte[] getHttpURLReturnData(HttpServletRequest request) throws Exception {
        if (0 >= request.getContentLength()) {
            throw new Exception();
        }
        ServletInputStream sis = request.getInputStream();
        byte[] resultBytes = new byte[request.getContentLength()];
        byte[] tempByte = new byte[1024];
        int length = 0;
        int index = 0;
        while ((length = sis.read(tempByte)) != -1) {
            System.arraycopy(tempByte, 0, resultBytes, index, length);
            index += length;
        }
        sis.close();
        return resultBytes;
    }
}
