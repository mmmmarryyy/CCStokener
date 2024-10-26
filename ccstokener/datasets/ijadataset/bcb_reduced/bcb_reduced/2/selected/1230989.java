package es.aeat.eett.rubik.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.swing.ImageIcon;
import org.apache.commons.logging.LogFactory;

/**
  * <p>
 * en: 'tableRubik' Icons
 * </p>
 * 
 * <p>
 * es: Iconos de 'tableRubik'
 * </p>
 *
 * @author f00992
 */
public class TableIcons {

    private final ImageIcon icPosExpand = new ImageIcon(getClass().getResource("/tableicons/drill-position-expand.gif"));

    private final ImageIcon icPosCollapse = new ImageIcon(getClass().getResource("/tableicons/drill-position-collapse.gif"));

    private final ImageIcon icMemExpand = new ImageIcon(getClass().getResource("/tableicons/drill-member-expand.gif"));

    private final ImageIcon icMemCollapse = new ImageIcon(getClass().getResource("/tableicons/drill-member-collapse.gif"));

    private final ImageIcon icRelaceExpand = new ImageIcon(getClass().getResource("/tableicons/drill-replace-expand.gif"));

    private final ImageIcon icRelaceCollapse = new ImageIcon(getClass().getResource("/tableicons/drill-replace-collapse.gif"));

    private final ImageIcon sort_current_down = new ImageIcon(getClass().getResource("/tableicons/sort-current-down.gif"));

    private final ImageIcon sort_current_up = new ImageIcon(getClass().getResource("/tableicons/sort-current-up.gif"));

    private final ImageIcon sort_current_natural = new ImageIcon(getClass().getResource("/tableicons/sort-natural.gif"));

    private final ImageIcon icDrillThrough = new ImageIcon(getClass().getResource("/tableicons/drill-through.gif"));

    private final ImageIcon sort_current_right = new ImageIcon(getClass().getResource("/tableicons/sort-current-right.gif"));

    private final ImageIcon sort_current_left = new ImageIcon(getClass().getResource("/tableicons/sort-current-left.gif"));

    private Map arrowMap = null;

    private static TableIcons instance = null;

    private TableIcons() {
        super();
        arrowMap = createArrowMap();
    }

    public static TableIcons getInstance() {
        if (instance == null) instance = new TableIcons();
        return instance;
    }

    private Map createArrowMap() {
        Map map = new HashMap();
        Properties props = new Properties();
        URL url = getClass().getResource("arrow.properties");
        InputStream strm = null;
        try {
            strm = url.openStream();
            props.load(strm);
            for (Enumeration e = props.keys(); e.hasMoreElements(); ) {
                String key = (String) e.nextElement();
                ImageIcon icon = new ImageIcon(getClass().getResource(props.getProperty(key)));
                map.put(key, icon);
            }
        } catch (Exception e) {
            LogFactory.getLog(getClass()).warn("failed loading plug-in settings", e);
        } finally {
            if (strm != null) try {
                strm.close();
            } catch (IOException e) {
            }
        }
        return Collections.unmodifiableMap(map);
    }

    /**
	 * @return Returns the icMemCollapse.
	 */
    public ImageIcon getIcMemCollapse() {
        return icMemCollapse;
    }

    /**
	 * @return Returns the icMemExpand.
	 */
    public ImageIcon getIcMemExpand() {
        return icMemExpand;
    }

    /**
	 * @return Returns the icPosCollapse.
	 */
    public ImageIcon getIcPosCollapse() {
        return icPosCollapse;
    }

    /**
	 * @return Returns the icPosExpand.
	 */
    public ImageIcon getIcPosExpand() {
        return icPosExpand;
    }

    /**
	 * @return Returns the icRelaceCollapse.
	 */
    public ImageIcon getIcRelaceCollapse() {
        return icRelaceCollapse;
    }

    /**
	 * @return Returns the icRelaceExpand.
	 */
    public ImageIcon getIcRelaceExpand() {
        return icRelaceExpand;
    }

    /**
	 * @return Returns the sort_current_down.
	 */
    public ImageIcon getSort_current_down() {
        return sort_current_down;
    }

    /**
	 * @return Returns the sort_current_natural.
	 */
    public ImageIcon getSort_current_natural() {
        return sort_current_natural;
    }

    /**
	 * @return Returns the sort_current_up.
	 */
    public ImageIcon getSort_current_up() {
        return sort_current_up;
    }

    /**
     * @return Returns the icDrillThrough.
     */
    public ImageIcon getIcDrillThrough() {
        return icDrillThrough;
    }

    /**
	 * @return Returns the sort_current_left.
	 */
    public ImageIcon getSort_current_left() {
        return sort_current_left;
    }

    /**
	 * @return Returns the sort_current_right.
	 */
    public ImageIcon getSort_current_right() {
        return sort_current_right;
    }

    public ImageIcon getArrow(String name) {
        return (ImageIcon) ((name != null) ? arrowMap.get(name) : null);
    }
}
