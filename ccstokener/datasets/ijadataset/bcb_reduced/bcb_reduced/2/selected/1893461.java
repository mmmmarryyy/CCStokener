package org.gbif.portal.harvest.workflow.activity.file;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang.StringUtils;
import org.gbif.portal.util.file.DelimitedFileReader;
import org.gbif.portal.util.workflow.Activity;
import org.gbif.portal.util.workflow.BaseActivity;
import org.gbif.portal.util.workflow.ProcessContext;

/**
 * An activity that will work on a set of Tab Delimitted files, launching the
 * child workflow for each line of each file.
 * 
 * @author trobertson
 */
public class MultipleTabFileIteratorActivity extends BaseActivity implements Activity {

    /**
	 * Context Keys
	 */
    protected String contextKeyFileUrls;

    protected String contextKeyPrefix;

    protected String contextKeySeparator;

    protected String contextKeyQuoteCharacter;

    protected boolean lowerCaseFirstLetterForContext = false;

    /**
	 * @see org.gbif.portal.util.workflow.Activity#execute(org.gbif.portal.util.workflow.ProcessContext)
	 */
    @SuppressWarnings("unchecked")
    public ProcessContext execute(ProcessContext context) throws Exception {
        List<String> urls = (List<String>) context.get(getContextKeyFileUrls(), List.class, true);
        for (String url : urls) {
            long time = System.currentTimeMillis();
            logger.info("Starting url: " + url);
            InputStream is = null;
            if (url.startsWith("http://") || url.startsWith("ftp://")) {
                is = new URL(url).openStream();
            } else {
                is = new FileInputStream(url);
            }
            String separator = (String) context.get(getContextKeySeparator(), String.class, false);
            if (separator == null) {
                separator = "\t";
            }
            String quoteCharacter = (String) context.get(getContextKeyQuoteCharacter(), String.class, false);
            DelimitedFileReader reader = new DelimitedFileReader(is, separator, quoteCharacter, true);
            Set<String> columns = reader.getColumnHeaders();
            while (reader.next()) {
                logger.debug("Starting row: " + reader.getRowNumber());
                for (String column : columns) {
                    String value = StringUtils.trimToNull(reader.get(column));
                    String key = getContextKeyPrefix() + column;
                    if (lowerCaseFirstLetterForContext && key.length() > 1) {
                        key = key.substring(0, 1).toLowerCase() + key.substring(1);
                    } else if (lowerCaseFirstLetterForContext) {
                        key = key.toLowerCase();
                    }
                    context.put(key, value);
                    logger.debug("Added key[" + key + "] value[" + value + "]");
                }
                launchWorkflow(context, null);
            }
            logger.info("Finished url [" + url + "] in " + ((System.currentTimeMillis() + 1 - time) / 1000) + " secs");
        }
        return context;
    }

    /**
	 * @return Returns the contextKeyFileUrls.
	 */
    public String getContextKeyFileUrls() {
        return contextKeyFileUrls;
    }

    /**
	 * @param contextKeyFileUrls The contextKeyFileUrls to set.
	 */
    public void setContextKeyFileUrls(String contextKeyFileUrls) {
        this.contextKeyFileUrls = contextKeyFileUrls;
    }

    /**
	 * @return Returns the contextKeyPrefix.
	 */
    public String getContextKeyPrefix() {
        return contextKeyPrefix;
    }

    /**
	 * @param contextKeyPrefix The contextKeyPrefix to set.
	 */
    public void setContextKeyPrefix(String contextKeyPrefix) {
        this.contextKeyPrefix = contextKeyPrefix;
    }

    /**
	 * @return the contextKeyQuoteCharacter
	 */
    public String getContextKeyQuoteCharacter() {
        return contextKeyQuoteCharacter;
    }

    /**
	 * @param contextKeyQuoteCharacter the contextKeyQuoteCharacter to set
	 */
    public void setContextKeyQuoteCharacter(String contextKeyQuoteCharacter) {
        this.contextKeyQuoteCharacter = contextKeyQuoteCharacter;
    }

    /**
	 * @return the contextKeySeparator
	 */
    public String getContextKeySeparator() {
        return contextKeySeparator;
    }

    /**
	 * @param contextKeySeparator the contextKeySeparator to set
	 */
    public void setContextKeySeparator(String contextKeySeparator) {
        this.contextKeySeparator = contextKeySeparator;
    }

    /**
	 * @return Returns the lowerCaseFirstLetterForContext.
	 */
    public boolean isLowerCaseFirstLetterForContext() {
        return lowerCaseFirstLetterForContext;
    }

    /**
	 * @param lowerCaseFirstLetterForContext The lowerCaseFirstLetterForContext to set.
	 */
    public void setLowerCaseFirstLetterForContext(boolean lowerCaseFirstLetterForContext) {
        this.lowerCaseFirstLetterForContext = lowerCaseFirstLetterForContext;
    }
}
