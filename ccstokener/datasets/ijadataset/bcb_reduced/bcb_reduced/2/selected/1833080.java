package com.aimluck.eip.news;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.List;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.jetspeed.services.logging.JetspeedLogFactoryService;
import org.apache.jetspeed.services.logging.JetspeedLogger;
import org.apache.jetspeed.services.resources.JetspeedResources;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import com.aimluck.commons.field.ALStringField;
import com.aimluck.commons.utils.ALStringUtil;
import com.aimluck.eip.cayenne.om.account.AipoLicense;
import com.aimluck.eip.cayenne.om.security.TurbineUser;
import com.aimluck.eip.common.ALAbstractFormData;
import com.aimluck.eip.common.ALDBErrorException;
import com.aimluck.eip.common.ALEipConstants;
import com.aimluck.eip.common.ALPageNotFoundException;
import com.aimluck.eip.license.util.LicenseUtils;
import com.aimluck.eip.orm.Database;
import com.aimluck.eip.orm.query.SelectQuery;

public class NewsFormData extends ALAbstractFormData {

    /** logger */
    private static final JetspeedLogger logger = JetspeedLogFactoryService.getLogger(NewsFormData.class.getName());

    private ALStringField company_name;

    private ALStringField post_name;

    private ALStringField customer_name;

    private ALStringField email;

    private ALStringField email_confirm;

    private ALStringField content;

    private String aipo_version;

    private String latest_version;

    @Override
    public void initField() {
        company_name = new ALStringField();
        company_name.setFieldName("御社名");
        post_name = new ALStringField();
        post_name.setFieldName("部署名");
        customer_name = new ALStringField();
        customer_name.setFieldName("ご担当者名");
        email = new ALStringField();
        email.setFieldName("メールアドレス");
        email_confirm = new ALStringField();
        email_confirm.setFieldName("メールアドレス（確認用）");
        content = new ALStringField();
        content.setFieldName("お問い合わせ内容");
        aipo_version = "";
        latest_version = "";
        equalsVersion();
    }

    @Override
    protected void setValidator() {
        company_name.limitMaxLength(200);
        post_name.limitMaxLength(200);
        customer_name.setNotNull(true);
        customer_name.limitMaxLength(200);
        email.setNotNull(true);
        email.setCharacterType(ALStringField.TYPE_ASCII);
        email.limitMaxLength(200);
        email_confirm.setNotNull(true);
        email_confirm.setCharacterType(ALStringField.TYPE_ASCII);
        email_confirm.limitMaxLength(200);
        content.setNotNull(true);
        content.limitMaxLength(400);
    }

    @Override
    protected boolean validate(List<String> msgList) {
        if (!company_name.validate(msgList)) {
            company_name.setValue(null);
        }
        if (!post_name.validate(msgList)) {
            post_name.setValue(null);
        }
        if (!customer_name.validate(msgList)) {
            customer_name.setValue(null);
        }
        if (!email.validate(msgList)) {
            email.setValue(null);
        }
        if (!email_confirm.validate(msgList)) {
            email_confirm.setValue(null);
        }
        if (!content.validate(msgList)) {
            content.setValue(null);
        }
        if (!email.toString().equals(email_confirm.toString())) {
            msgList.add("『 <span class='em'>メールアドレス</span> 』と『 <span class='em'>メールアドレス（確認用）</span> 』には同じものを入力してください。");
            email.setValue(null);
            email_confirm.setValue(null);
        } else if (!ALStringUtil.isCellPhoneMailAddress(email.toString())) {
            msgList.add("『 <span class='em'>メールアドレス</span> 』を正しく入力してください。");
        }
        return (msgList.size() == 0);
    }

    @Override
    protected boolean setFormData(RunData rundata, Context context, List<String> msgList) throws ALPageNotFoundException, ALDBErrorException {
        boolean res = super.setFormData(rundata, context, msgList);
        return res;
    }

    @Override
    protected boolean insertFormData(RunData rundata, Context context, List<String> msglist) {
        return false;
    }

    private void equalsVersion() {
        String line = "";
        try {
            String url_str = JetspeedResources.getString("aipo.version_url", "");
            latest_version = aipo_version = JetspeedResources.getString("aipo.version", "");
            URL url = new URL(url_str);
            InputStream is = url.openStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader bur = new BufferedReader(isr);
            if ((line = bur.readLine()) != null) {
                latest_version = line;
            }
            bur.close();
            isr.close();
            is.close();
        } catch (MalformedURLException e) {
            logger.error("[NewsFormData]", e);
        } catch (UnknownHostException e) {
            logger.error("[NewsFormData]", e);
        } catch (SocketException e) {
            logger.error("[NewsFormData]", e);
        } catch (IOException e) {
            logger.error("[NewsFormData]", e);
        }
    }

    public void getLicense(RunData rundata, Context context) {
        AipoLicense al = LicenseUtils.getAipoLicense(rundata, context);
        if (al != null) {
            context.put("Key", al.getLicense());
        }
        SelectQuery<TurbineUser> query = Database.query(TurbineUser.class);
        Expression exp = ExpressionFactory.matchExp(TurbineUser.DISABLED_PROPERTY, "F");
        query.setQualifier(exp);
        List<TurbineUser> list = query.fetchList();
        context.put("UserNum", (String.valueOf(list.size() - 2)));
    }

    @Override
    protected boolean updateFormData(RunData rundata, Context context, List<String> msgList) {
        return false;
    }

    @Override
    protected boolean loadFormData(RunData rundata, Context context, List<String> msgList) {
        return false;
    }

    @Override
    protected boolean deleteFormData(RunData rundata, Context context, List<String> msgList) {
        return false;
    }

    public ALStringField getCompanyName() {
        return company_name;
    }

    public ALStringField getPostName() {
        return post_name;
    }

    public ALStringField getCustomerName() {
        return customer_name;
    }

    public ALStringField getEmail() {
        return email;
    }

    public ALStringField getEmailConfirm() {
        return email_confirm;
    }

    public ALStringField getContent() {
        return content;
    }

    public String getAipoVersion() {
        return aipo_version;
    }

    public String getLatestVersion() {
        return latest_version;
    }

    public Object getNewsHTML() {
        HttpURLConnection conn = null;
        BufferedReader reader = null;
        String body = null;
        try {
            String news_url = JetspeedResources.getString("aipo.news_url", "");
            URL url = new URL(news_url);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            reader = new BufferedReader(new InputStreamReader(conn.getInputStream(), ALEipConstants.DEF_CONTENT_ENCODING));
            StringBuffer sb = new StringBuffer();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\r\n");
            }
            body = sb.toString();
        } catch (Exception ex) {
            logger.error("Exception", ex);
            body = "";
        } finally {
            try {
                reader.close();
                conn.disconnect();
            } catch (Exception e) {
                logger.error("Exception", e);
                return "";
            }
        }
        return body;
    }
}
