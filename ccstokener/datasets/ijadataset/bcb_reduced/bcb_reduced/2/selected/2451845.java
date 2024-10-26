package net.simpleframework.web.page.component.ui.portal.module;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;
import net.simpleframework.core.AbstractXmlDocument;
import net.simpleframework.web.page.IForward;
import net.simpleframework.web.page.TextForward;
import net.simpleframework.web.page.UrlForward;
import net.simpleframework.web.page.component.ComponentParameter;
import net.simpleframework.web.page.component.ui.portal.PageletBean;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;

/**
 * 这是一个开源的软件，请在LGPLv3下合法使用、修改或重新发布。
 * 
 * @author 陈侃(cknet@126.com, 13910090885)
 *         http://code.google.com/p/simpleframework/
 *         http://www.simpleframework.net
 */
public class WeatherModuleHandle extends AbstractPortalModuleHandle {

    public WeatherModuleHandle(final PageletBean pagelet) {
        super(pagelet);
    }

    private static String[] defaultOptions = new String[] { "_weather_code=CHXX0008" };

    @Override
    protected String[] getDefaultOptions() {
        return defaultOptions;
    }

    @Override
    public IForward getPageletOptionContent(final ComponentParameter compParameter) {
        return new UrlForward(getResourceHomePath() + "/jsp/module/weather_option.jsp");
    }

    @Override
    public IForward getPageletContent(final ComponentParameter compParameter) {
        return new TextForward(output(compParameter));
    }

    public String output(final ComponentParameter compParameter) {
        InputStream inputStream;
        try {
            final URL url = new URL("http://xml.weather.yahoo.com/forecastrss?p=" + getPagelet().getOptionProperty("_weather_code") + "&u=c");
            inputStream = url.openStream();
        } catch (final IOException e) {
            return e.getMessage();
        }
        final StringBuilder sb = new StringBuilder();
        new AbstractXmlDocument(inputStream) {

            @Override
            protected void init() throws Exception {
                final Element root = getRoot();
                final Namespace ns = root.getNamespaceForPrefix("yweather");
                final Element channel = root.element("channel");
                final String link = channel.elementText("link");
                final Element item = channel.element("item");
                Element ele = item.element(QName.get("condition", ns));
                if (ele == null) {
                    sb.append("ERROR");
                    return;
                }
                final String imgPath = getPagelet().getColumnBean().getPortalBean().getCssResourceHomePath(compParameter) + "/images/yahoo/";
                String text, image;
                Date date = new SimpleDateFormat(YahooWeatherUtils.RFC822_MASKS[1], Locale.US).parse(ele.attributeValue("date"));
                final int temp = Integer.parseInt(ele.attributeValue("temp"));
                int code = Integer.valueOf(ele.attributeValue("code")).intValue();
                if (code == 3200) {
                    text = YahooWeatherUtils.yahooTexts[YahooWeatherUtils.yahooTexts.length - 1];
                    image = imgPath + "3200.gif";
                } else {
                    text = YahooWeatherUtils.yahooTexts[code];
                    image = imgPath + code + ".gif";
                }
                sb.append("<div style=\"line-height: normal;\"><a target=\"_blank\" href=\"").append(link).append("\"><img src=\"");
                sb.append(image).append("\" /></a>");
                sb.append(YahooWeatherUtils.formatHour(date)).append(" - ");
                sb.append(text).append(" - ").append(temp).append("℃").append("<br>");
                final Iterator<?> it = item.elementIterator(QName.get("forecast", ns));
                while (it.hasNext()) {
                    ele = (Element) it.next();
                    date = new SimpleDateFormat("dd MMM yyyy", Locale.US).parse(ele.attributeValue("date"));
                    final int low = Integer.parseInt(ele.attributeValue("low"));
                    final int high = Integer.parseInt(ele.attributeValue("high"));
                    code = Integer.valueOf(ele.attributeValue("code")).intValue();
                    if (code == 3200) {
                        text = YahooWeatherUtils.yahooTexts[YahooWeatherUtils.yahooTexts.length - 1];
                        image = imgPath + "3200.gif";
                    } else {
                        text = YahooWeatherUtils.yahooTexts[code];
                        image = imgPath + code + ".gif";
                    }
                    sb.append(YahooWeatherUtils.formatWeek(date)).append(" ( ");
                    sb.append(text).append(". ");
                    sb.append(low).append("℃~").append(high).append("℃");
                    sb.append(" )<br>");
                }
                sb.append("</div>");
            }
        };
        return sb.toString();
    }

    @Override
    public OptionWindowUI getPageletOptionUI(final ComponentParameter compParameter) {
        return new OptionWindowUI(getPagelet()) {

            @Override
            public int getHeight() {
                return 150;
            }
        };
    }
}
