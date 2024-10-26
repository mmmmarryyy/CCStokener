package ch.unibe.im2.inkanno;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;
import ch.unibe.eindermu.utils.Aspect;
import ch.unibe.eindermu.utils.Config;
import ch.unibe.eindermu.utils.Observable;
import ch.unibe.eindermu.utils.Observer;
import ch.unibe.eindermu.utils.StringMap;
import ch.unibe.im2.inkanno.exporter.FactoryException;
import ch.unibe.im2.inkanno.gui.GUI;
import ch.unibe.im2.inkanno.gui.color.Colorizer;
import ch.unibe.im2.inkanno.gui.color.ColorizerCallback;
import ch.unibe.im2.inkanno.gui.color.NullColorizer;

/**
 * ColorizerManager manages different colorizers, which can be added in additional
 * jars. There you have to create the file "colorizer_implementation.properties" in the packages
 * "ch.unibe.im2.inkanno.gui.color" in this file you can specify a list of classes which implement the "Colorizer" interface.
 * This list must be in the following format:
 * <code>
 * colorizer.whatevername=package.of.class.ClassImplementingColorizerInterface
 * colorizer.anothername=package.of.class.AnotherClassImplementingTheColorizerInterface
 * </code>
 * The Colorizer interface selects then one of this colorizers as the default colorizer. This can be changed by the 
 * command line option --colorizer.
 * The selected colorizer will the be used to choose the color of the displayed strokes. 
 * @author emanuel
 *
 */
public class DrawPropertyManager extends Hashtable<String, Object> implements Observable {

    public class ColorizerActionListener implements ActionListener {

        private ColorizerCallback cb;

        public ColorizerActionListener(ColorizerCallback cb) {
            this.cb = cb;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            DrawPropertyManager.this.setColorizer(cb.getColorizerForSelection());
        }
    }

    public static final Aspect EVENT_NEW_COLORIZER = new Aspect() {
    };

    public static final Aspect EVENT_DRAW_PROPERTY_CHANGED = new Aspect() {
    };

    public static final String IS_TRACE_GROUP_VISIBLE = "is_trace_group_visible";

    private static DrawPropertyManager cm;

    private List<Colorizer> colorizers;

    private Colorizer currentColorizer;

    private StringMap<Object> properties;

    private Map<Aspect, List<Observer>> observers;

    /**
     * Returns the singleton of the Colorizer manager
     * @return
     */
    public static DrawPropertyManager getInstance() {
        if (cm == null) {
            cm = new DrawPropertyManager();
        }
        return cm;
    }

    /**
     * Only one instance allowed, use getInstance()
     */
    private DrawPropertyManager() {
        properties = new StringMap<Object>();
        observers = new HashMap<Aspect, List<Observer>>();
        setProperty(IS_TRACE_GROUP_VISIBLE, true);
        loadAvailableColorizer();
        GUI.getInstance().getDocumentManager().registerFor(DocumentManager.ON_DOCUMENT_SWITCH, new Observer() {

            @Override
            public void notifyFor(Aspect event, Object subject) {
                if (DrawPropertyManager.this.currentColorizer != null) {
                    DrawPropertyManager.this.currentColorizer.initialize((Document) subject);
                }
            }
        });
    }

    private void loadAvailableColorizer() {
        List<Colorizer> l = new ArrayList<Colorizer>();
        try {
            Enumeration<URL> en = ClassLoader.getSystemClassLoader().getResources("ch/unibe/im2/inkanno/plugins/colorizer_implementation.properties");
            while (en.hasMoreElements()) {
                Properties p = new Properties();
                URL url = en.nextElement();
                p.load(url.openStream());
                for (Object objstr : p.keySet()) {
                    String str = (String) objstr;
                    if (str.equals("colorizer") || str.startsWith("colorizer.")) {
                        if (p.getProperty(str) != null) {
                            l.add(createColorizerPlugin(p.getProperty(str)));
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (FactoryException e) {
            e.printStackTrace();
        }
        colorizers = l;
        if (colorizers.size() == 0) {
            setColorizer(new NullColorizer());
        } else if (Config.getMain().get("colorizer") != null && !Config.getMain().get("colorizer").isEmpty()) {
            String selection = Config.getMain().get("colorizer");
            for (Colorizer col : colorizers) {
                if (col.isResponsible(selection)) {
                    setColorizer(col);
                    break;
                }
            }
        }
        if (currentColorizer == null) {
            setColorizer(new NullColorizer());
        }
    }

    private Colorizer createColorizerPlugin(String name) throws FactoryException {
        Class<?> c = null;
        try {
            c = Class.forName(name);
        } catch (ClassNotFoundException e1) {
            throw new FactoryException("Class '" + name + "' could not be found.");
        }
        if (c != null) {
            Colorizer x = null;
            try {
                Method m = c.getDeclaredMethod("factory", new Class[] {});
                x = (Colorizer) m.invoke(null, new Object[] {});
            } catch (IllegalAccessException e) {
                throw new FactoryException("Class '" + name + "' is not valid.");
            } catch (SecurityException e) {
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
            return x;
        } else {
            throw new FactoryException("Class '" + name + "' could not be found.");
        }
    }

    public Colorizer getCurrentColorizer() {
        return currentColorizer;
    }

    public void setColorizer(Colorizer colorizer) {
        Colorizer oldColorizer = currentColorizer;
        currentColorizer = colorizer;
        if (!colorizers.contains(colorizer)) {
            colorizers.add(colorizer);
        }
        if (GUI.getInstance().getDocumentManager().hasCurrentDocument()) {
            colorizer.initialize(GUI.getInstance().getDocumentManager().getCurrentDocument());
            this.notifyObserver(EVENT_NEW_COLORIZER);
        }
    }

    public List<Colorizer> getColorizers() {
        return colorizers;
    }

    public boolean getBProperty(String name) {
        return (Boolean) get(name);
    }

    public double getDProperty(String name) {
        return (Double) get(name);
    }

    public int getIProperty(String name) {
        return (Integer) get(name);
    }

    public String getSProperty(String name) {
        return (String) get(name);
    }

    public <S> S setProperty(String name, S value) {
        put(name, value);
        notifyPropertyChange(name);
        return value;
    }

    public static class PropertyChangeEventAspect extends Aspect {

        private String propertyName;

        public PropertyChangeEventAspect(String propertyName) {
            this.propertyName = propertyName;
        }

        public int hashCode() {
            return propertyName.hashCode();
        }

        public boolean equals(Object other) {
            return this.hashCode() == other.hashCode();
        }
    }

    @Override
    public void registerFor(Aspect event, Observer o) {
        if (!observers.containsKey(event)) {
            observers.put(event, new LinkedList<Observer>());
        }
        observers.get(event).add(o);
    }

    private void notifyPropertyChange(String property) {
        notifyObserver(new PropertyChangeEventAspect(property));
    }

    private void notifyObserver(Aspect event) {
        if (observers.containsKey(event)) {
            for (Observer o : observers.get(event)) {
                o.notifyFor(event, this);
            }
        }
    }

    public Vector<ColorizerCallback> getColorizerCallbacks() {
        Vector<ColorizerCallback> cbs = new Vector<ColorizerCallback>();
        for (Colorizer c : getColorizers()) {
            cbs.addAll(c.getCallbacks());
        }
        return cbs;
    }

    public boolean isSelectedColorizer(Colorizer colorizer) {
        return currentColorizer == colorizer;
    }
}
