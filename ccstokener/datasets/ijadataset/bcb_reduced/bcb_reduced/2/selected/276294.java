package com.novse.segmentation.core.io;

import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * @author Mac Kwan ͳһ��Դ·���µ���Դ
 */
public class UrlResource implements Resource {

    /**
     * ͳһ��Դ·��
     */
    private URL url = null;

    /**
     * ���ַ���ʽ��ͳһ��Դ·��Ϊ����Ĺ��캯��
     * 
     * @param path
     *            �ַ���ʽ��ͳһ��Դ·��
     */
    public UrlResource(String path) {
        try {
            this.url = new URL(path);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    /**
     * ��ͳһ��Դ·��ʵ��Ϊ����Ĺ��캯��
     * 
     * @param url
     *            ͳһ��Դ·��ʵ��
     */
    public UrlResource(URL url) {
        this.url = url;
    }

    /**
     * ����ָ��ͳһ��Դ·������Դ��������
     */
    public InputStream getInputStream() throws Exception {
        return this.url.openStream();
    }
}
