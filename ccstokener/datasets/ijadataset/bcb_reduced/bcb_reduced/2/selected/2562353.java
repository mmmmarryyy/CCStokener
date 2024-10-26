package org.wdcode.web.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import org.wdcode.common.exception.CustomRuntimeException;
import org.wdcode.common.util.CommonUtil;

/**
 * URL相关操作
 * @author WD
 * @since JDK6
 * @version 1.0 2010-01-22
 */
public final class UrlUtil {

    /**
	 * 获得URL
	 * @param url
	 * @return
	 */
    public static final InputStream openStream(URL url) {
        try {
            return url.openStream();
        } catch (IOException e) {
            throw new CustomRuntimeException(e);
        }
    }

    /**
	 * 判断url是否为空
	 * @param url URL
	 * @return boolean
	 */
    public static final boolean isEmpty(URL url) {
        return CommonUtil.isEmpty(url);
    }

    /**
	 * 私有构造
	 */
    private UrlUtil() {
    }
}
