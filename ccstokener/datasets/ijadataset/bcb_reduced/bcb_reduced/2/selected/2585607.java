package traitmap.proxy;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import javax.servlet.http.HttpServletResponse;

/**
 * �擾�������ʂ����̃��X�|���X�ɕԂ����������܂��B
 * 
 * @author Konta
 * @version 1.0
 */
public final class Proxy {

    /** �o�b�t�@�T�C�Y */
    public final int BUF_SIZE = 16384;

    String m_url;

    /**
	 * �R���X�g���N�^
	 *
	 */
    public Proxy() {
        clear();
    }

    /**
	 * �R���X�g���N�^
	 * @param url
	 */
    public Proxy(String url) {
        clear();
        m_url = url;
    }

    /**
	 * ����
	 *
	 */
    public void clear() {
        m_url = "";
    }

    /**
	 * ���X�|���X�Ɍ��ʂ��o�͂��܂��B
	 * @param res HttpServletResponse
	 * @throws MalformedURLException
	 * @throws IOException
	 */
    public void write(HttpServletResponse res) throws MalformedURLException, IOException {
        if (m_url.equals("")) {
            return;
        }
        URL url = new URL(m_url);
        URLConnection con = url.openConnection();
        con.setUseCaches(false);
        BufferedInputStream in = new BufferedInputStream(con.getInputStream(), BUF_SIZE);
        BufferedOutputStream out = new BufferedOutputStream(res.getOutputStream());
        byte[] buf = new byte[BUF_SIZE];
        int size = 0;
        String contentType = con.getContentType();
        if (contentType != null) {
            res.setContentType(con.getContentType());
        }
        while ((size = in.read(buf)) > 0) {
            out.write(buf, 0, size);
        }
        out.flush();
        out.close();
        in.close();
    }

    /**
	 * URL���擾���܂��B
	 * @return URL
	 */
    public String getUrl() {
        return m_url;
    }

    /**
	 * URL��ݒ肵�܂��B
	 * @param string
	 */
    public void setUrl(String string) {
        m_url = string;
    }
}
