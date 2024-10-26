package cn.myapps.core.macro.runner;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collection;
import com.ibm.db2.jcc.a.c;
import cn.myapps.base.action.ParamsTable;
import cn.myapps.core.dynaform.activity.ejb.Activity;
import cn.myapps.core.dynaform.document.ejb.Document;
import cn.myapps.core.dynaform.macro.CurrDocJsUtil;
import cn.myapps.core.dynaform.macro.FactoryJsUtil;
import cn.myapps.core.dynaform.macro.HTMLJsUtil;
import cn.myapps.core.dynaform.macro.PrintJsUtil;
import cn.myapps.core.dynaform.macro.Tools;
import cn.myapps.core.dynaform.macro.WebJsUtil;
import cn.myapps.core.user.action.WebUser;
import cn.myapps.util.ProcessFactory;
import cn.myapps.util.ftp.FTPUpload;
import cn.myapps.util.mail.EmailUtil;
import cn.myapps.util.message.MessageUtil;

public abstract class AbstractRunner implements IRunner {

    private static final String _BASELIB_FILENAME = "baselib.js";

    private static String _BASE_LIB_JS = null;

    private String sessionid;

    protected HTMLJsUtil _htmlJsUtil;

    protected String applicationId;

    public abstract Object run(String label, String js) throws Exception;

    /**
	 * 读取baselib.js文件中的function
	 * 
	 * @return 读取文件后的内容
	 */
    public String readBaseLib() throws Exception {
        if (_BASE_LIB_JS == null) {
            StringBuffer js = new StringBuffer();
            try {
                URL url = AbstractRunner.class.getResource(_BASELIB_FILENAME);
                if (url != null) {
                    InputStream is = url.openStream();
                    InputStreamReader reader = new InputStreamReader(is);
                    BufferedReader bfReader = new BufferedReader(reader);
                    String tmp = null;
                    do {
                        tmp = bfReader.readLine();
                        if (tmp != null) {
                            js.append(tmp).append('\n');
                        }
                    } while (tmp != null);
                    bfReader.close();
                    reader.close();
                    is.close();
                }
            } catch (Exception e) {
                e.printStackTrace();
                throw e;
            }
            _BASE_LIB_JS = js.toString();
        }
        return _BASE_LIB_JS;
    }

    /**
	 * 注册一些公共工具类,使在调用时可以直接使用如($EMAIL,调用类方法时需要直接$EMAIL.方法名)
	 * 
	 * @param currdoc
	 *            文档对象
	 * @param params
	 *            参数
	 * @param user
	 *            web用户
	 * @param errors
	 *            错误信息
	 */
    public void initBSFManager(Document currdoc, ParamsTable params, WebUser user, Collection errors) throws Exception {
        if (params != null && params.getSessionid() != null) {
            this.sessionid = params.getSessionid();
        }
        if (this.applicationId != null && this.applicationId.trim().length() > 0) {
            declareBean("$EMAIL", new EmailUtil(this.applicationId), EmailUtil.class);
        } else {
            declareBean("$EMAIL", new EmailUtil(), EmailUtil.class);
        }
        if (this.applicationId != null && this.applicationId.trim().length() > 0) {
            declareBean("$MESSAGE", new MessageUtil(this.applicationId), MessageUtil.class);
        } else {
            declareBean("$MESSAGE", new MessageUtil(), MessageUtil.class);
        }
        declareBean("$FTP", new FTPUpload(), FTPUpload.class);
        if (currdoc != null) {
            currdoc.set_params(params);
        }
        if (currdoc != null) declareBean("$CURRDOC", new CurrDocJsUtil(currdoc), CurrDocJsUtil.class);
        declareBean("$PRINTER", new PrintJsUtil(), PrintJsUtil.class);
        if (currdoc != null && params != null && user != null) declareBean("$WEB", new WebJsUtil(currdoc, params, user, errors), WebJsUtil.class);
        declareBean("$TOOLS", new Tools(), Tools.class);
        declareBean("$BEANFACTORY", new FactoryJsUtil(), FactoryJsUtil.class);
        declareBean("$PROCESSFACTORY", ProcessFactory.getInstance(), ProcessFactory.class);
        if (_htmlJsUtil == null) _htmlJsUtil = new HTMLJsUtil();
        declareBean("$HTML", _htmlJsUtil, HTMLJsUtil.class);
    }

    public void registerActivity(Activity activity) throws Exception {
        declareBean("$ACTIVITY", activity, Activity.class);
    }

    /**
	 * 声明对象,注册对象
	 * 
	 * @param registName
	 *            注册对象的别名
	 * @param bean
	 *            注册类
	 * @param clazz
	 *            注册类的Class
	 */
    public abstract void declareBean(String registName, Object bean, Class clazz) throws Exception;

    /**
	 * 释放已声明对象
	 * 
	 * @param registName
	 *            注册对象的别名
	 */
    public abstract void undeclareBean(String registName) throws Exception;

    /**
	 * 获取应用标识
	 * 
	 * @return 应用标识
	 */
    public String getApplicationId() {
        return applicationId;
    }

    /**
	 * 设置应用标识
	 */
    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    /**
	 * 获取htmlJsUtil的解析类
	 * 
	 * @return 解析类
	 */
    public HTMLJsUtil get_htmlJsUtil() {
        return _htmlJsUtil;
    }

    /**
	 * 设置htmlJsUtil的解析类
	 * 
	 * @param htmlJsUtil的解析类
	 */
    public void set_htmlJsUtil(HTMLJsUtil jsUtil) {
        _htmlJsUtil = jsUtil;
    }
}
