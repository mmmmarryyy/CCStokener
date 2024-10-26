package org.t2framework.lucy.config.stax;

import java.io.BufferedInputStream;
import java.net.URL;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.EndElement;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import org.t2framework.commons.util.Assertion;
import org.t2framework.commons.util.Logger;
import org.t2framework.commons.util.ResourceUtil;
import org.t2framework.commons.util.StAXUtil;
import org.t2framework.commons.util.StreamUtil;
import org.t2framework.commons.util.URLUtil;
import org.t2framework.lucy.Lucy;
import org.t2framework.lucy.exception.TagHandlerNotFoundException;

/**
 * 
 * <#if locale="en">
 * <p>
 * A xml parser using StAX.
 * </p>
 * <#else>
 * <p>
 * StAXを使ったXMLパーサです．
 * </p>
 * </#if>
 * 
 * @author shot
 * 
 */
public class StAXParser {

    protected static Logger logger = Logger.getLogger(StAXParser.class);

    protected final XmlEventHandlerRule rule;

    protected final XMLInputFactory factory;

    protected final LucyEventFilter filter = new LucyEventFilter();

    public StAXParser(XmlEventHandlerRule rule) {
        this.rule = Assertion.notNull(rule);
        this.factory = XMLInputFactory.newInstance();
        factory.setXMLResolver(new LucyXmlResolver());
    }

    /**
	 * 
	 * <#if locale="en">
	 * <p>
	 * 
	 * </p>
	 * <#else>
	 * <p>
	 * XMLをパースして、適切なハンドラを呼びます．
	 * </p>
	 * </#if>
	 * 
	 * @param lucy
	 * @param path
	 */
    public void parse(final Lucy lucy, final String path) {
        BufferedInputStream bis = getBufferedInputStream(path);
        XMLEventReaderDelegate reader = null;
        try {
            XMLEventReader org = StAXUtil.createXMLEventReader(factory, bis);
            XMLEventReader filteredReader = StAXUtil.createFilteredReader(factory, org, filter);
            reader = new XMLEventReaderDelegate(filteredReader);
            XmlEventContext context = new XmlEventContext(lucy);
            context.startHandle(path);
            for (; reader.hasNext(); ) {
                XMLEvent event = reader.nextEvent();
                context.setCurrentEvent(event);
                if (event.isStartElement()) {
                    StartElement se = event.asStartElement();
                    Attributes attributes = new Attributes(se);
                    String tagname = se.getName().getLocalPart();
                    XmlEventHandler handler = getHandler(tagname);
                    handler.start(context, attributes);
                    continue;
                } else if (event.isEndElement()) {
                    EndElement ee = event.asEndElement();
                    String tagname = ee.getName().getLocalPart();
                    XmlEventHandler handler = getHandler(tagname);
                    String body = context.popBody();
                    handler.end(context, body);
                    continue;
                } else if (event.isCharacters()) {
                    Characters c = event.asCharacters();
                    context.pushBody(c.getData());
                    continue;
                }
            }
            context.endHandle();
        } catch (RuntimeException e) {
            logger.debug(e.getMessage());
            throw e;
        } catch (Throwable t) {
            logger.debug(t.getMessage());
            throw new IllegalStateException(t);
        } finally {
            if (reader != null) {
                reader.close();
            }
            if (bis != null) {
                StreamUtil.close(bis);
            }
        }
    }

    protected XmlEventHandler getHandler(String tagname) {
        XmlEventHandler handler = rule.getHandler(tagname);
        if (handler == null) {
            throw new TagHandlerNotFoundException(tagname);
        }
        return handler;
    }

    protected BufferedInputStream getBufferedInputStream(String path) {
        URL url = ResourceUtil.getResource(path);
        return new BufferedInputStream(URLUtil.openStream(url));
    }
}
