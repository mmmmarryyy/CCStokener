package com.pinae.nala.xb.xml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import com.pinae.nala.xb.NoSuchPathException;

/**
 * ���ļ�����Ϊ�����
 * 
 * @author ��ع��
 * 
 */
public class XMLReader {

    /**
	 * ���ļ�����Ϊ�����
	 * 
	 * @param filename
	 *            ��Ҫ��ȡ���ļ����
	 * @return �ļ����������
	 * @throws NoSuchPathException
	 *             �����ļ�ʱ���޷�����·������쳣
	 * @throws FileNotFoundException
	 */
    public static InputStream getFileStream(String filename) throws NoSuchPathException {
        try {
            return new FileInputStream(filename);
        } catch (FileNotFoundException e) {
            throw new NoSuchPathException(e.getMessage());
        }
    }

    /**
	 * ��URL��Դ����Ϊ�����
	 * 
	 * @param path
	 *            URL��Դ
	 * @return ��Դ���������
	 * @throws NoSuchPathException
	 *             ����URL����Դʱ���޷�����·������쳣
	 * @throws IOException
	 *             ��дURL��Դ����쳣
	 */
    public static InputStream getURLStream(String path) throws NoSuchPathException, IOException {
        URL url = null;
        try {
            url = new URL(path);
        } catch (MalformedURLException e) {
            throw new NoSuchPathException(e.getMessage());
        }
        if (url != null) {
            return url.openStream();
        } else {
            return null;
        }
    }
}
