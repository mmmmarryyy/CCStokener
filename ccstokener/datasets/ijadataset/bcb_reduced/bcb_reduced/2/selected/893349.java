package com.peterhi.classroom;

import java.io.File;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.CoolItem;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.Widget;
import com.peterhi.runtime.Util;
import com.peterhi.runtime.Util.Property;

public class ToolKit {

    private static Map images = new HashMap();

    public static Cursor TAB_LEFT = createCursor(UIImageManager.THIS.getTabLeft());

    public static Cursor TAB_RIGHT = createCursor(UIImageManager.THIS.getTabRight());

    public static Cursor TAB_UP = createCursor(UIImageManager.THIS.getTabUp());

    public static Cursor TAB_DOWN = createCursor(UIImageManager.THIS.getTabDown());

    public static Cursor TAB_CENTER = createCursor(UIImageManager.THIS.getTabCenter());

    public static Cursor SPLIT_TOP = createCursor(UIImageManager.THIS.getSplitTop());

    public static Cursor SPLIT_BOTTOM = createCursor(UIImageManager.THIS.getSplitBottom());

    public static Cursor SPLIT_LEFT = createCursor(UIImageManager.THIS.getSplitLeft());

    public static Cursor SPLIT_RIGHT = createCursor(UIImageManager.THIS.getSplitRight());

    public static Cursor AUTOHIDE_TOP = createCursor(UIImageManager.THIS.getAutoHideTop());

    public static Cursor AUTOHIDE_BOTTOM = createCursor(UIImageManager.THIS.getAutoHideBottom());

    public static Cursor AUTOHIDE_LEFT = createCursor(UIImageManager.THIS.getAutoHideLeft());

    public static Cursor AUTOHIDE_RIGHT = createCursor(UIImageManager.THIS.getAutoHideRight());

    public static Cursor AUTOHIDE_CENTER = createCursor(UIImageManager.THIS.getAutoHideCenter());

    public static Cursor FLOAT_ON = createCursor(UIImageManager.THIS.getFloatOn());

    public static Cursor DOCK_TOP = createCursor(UIImageManager.THIS.getDockTop());

    public static Cursor DOCK_BOTTOM = createCursor(UIImageManager.THIS.getDockBottom());

    public static Cursor DOCK_LEFT = createCursor(UIImageManager.THIS.getDockLeft());

    public static Cursor DOCK_RIGHT = createCursor(UIImageManager.THIS.getDockRight());

    public static Cursor DOCK_CENTER = createCursor(UIImageManager.THIS.getDockCenter());

    private static Cursor createCursor(Image img) {
        return createCursor(img, (img.getBounds().x + img.getBounds().width) / 2, (img.getBounds().y + img.getBounds().height / 2));
    }

    private static Cursor createCursor(Image img, int x, int y) {
        return new Cursor(Display.getDefault(), img.getImageData(), x, y);
    }

    private static class DelegateListener implements Listener {

        private Object target;

        private String method;

        DelegateListener(Object target, String method) {
            this.target = target;
            this.method = method;
        }

        public void handleEvent(Event e) {
            Util.invoke(target, method, new Object[] { e });
        }
    }

    private static class ImageManagerInvocationHandler implements InvocationHandler {

        private boolean disposed;

        private Class interfaceType;

        private String base;

        public ImageManagerInvocationHandler(Class interfaceType, String base) {
            this.interfaceType = interfaceType;
            this.base = base;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (isIsDisposedMethod(method)) {
                return new Boolean(disposed);
            } else if (isDisposeMethod(method)) {
                for (Iterator itor = images.entrySet().iterator(); itor.hasNext(); ) {
                    Map.Entry e = (Map.Entry) itor.next();
                    Image value = (Image) e.getValue();
                    dispose(value);
                }
                disposed = true;
                return null;
            } else if (Util.isGetter(method) && Image.class.equals(method.getReturnType())) {
                Property property = Util.getProperty(method);
                URL url = interfaceType.getResource(base + File.separator + property.getName() + ".png");
                Image image = (Image) images.get(url);
                if (image == null) {
                    image = new Image(Display.getCurrent(), url.openStream());
                    images.put(url, image);
                }
                return image;
            } else {
                return Util.defaultHandleObjectInvoke(interfaceType, this, proxy, method, args);
            }
        }
    }

    public static String getActionString(String text, char mnemonic, int accelerator, boolean ellipsis) {
        int index = text.toLowerCase().indexOf(mnemonic);
        if (index < 0) {
            text += "(&" + Character.toUpperCase(mnemonic) + ")";
        } else {
            text = text.substring(0, index) + "&" + text.substring(index);
        }
        if (ellipsis) {
            text += "...";
        }
        return text;
    }

    public static void setSize(CoolItem ci, Point sz) {
        ci.setMinimumSize(sz);
        ci.setPreferredSize(sz);
        ci.setSize(sz);
    }

    public static GridLayout createGridLayout(int numColumns, boolean makeColumnsEqualWidth) {
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = numColumns;
        gridLayout.makeColumnsEqualWidth = makeColumnsEqualWidth;
        return gridLayout;
    }

    public static GridLayout createGridLayout(int marginWidth, int marginHeight, int horizontalSpacing, int verticalSpacing) {
        return createGridLayout(1, marginWidth, marginHeight, horizontalSpacing, verticalSpacing);
    }

    public static GridLayout createGridLayout(int numColumns, int marginWidth, int marginHeight, int horizontalSpacing, int verticalSpacing) {
        GridLayout gridLayout = new GridLayout();
        gridLayout.numColumns = numColumns;
        gridLayout.horizontalSpacing = horizontalSpacing;
        gridLayout.verticalSpacing = verticalSpacing;
        gridLayout.marginWidth = marginWidth;
        gridLayout.marginHeight = marginHeight;
        return gridLayout;
    }

    public static RowLayout createRowLayout(int type, int spacing, int marginLeft, int marginRight, int marginTop, int marginBottom) {
        RowLayout rowLayout = new RowLayout();
        rowLayout.type = type;
        rowLayout.spacing = spacing;
        rowLayout.marginLeft = marginLeft;
        rowLayout.marginRight = marginRight;
        rowLayout.marginTop = marginTop;
        rowLayout.marginBottom = marginBottom;
        return rowLayout;
    }

    public static FillLayout createFillLayout(int marginWidth, int marginHeight) {
        FillLayout fillLayout = new FillLayout();
        fillLayout.marginWidth = marginWidth;
        fillLayout.marginHeight = marginHeight;
        return fillLayout;
    }

    public static GridData createGridData(int horizontalAlignment, int verticalAlignment, boolean grabExcessHorizontalSpace, boolean grabExcessVerticalSpace) {
        return createGridData(horizontalAlignment, verticalAlignment, grabExcessHorizontalSpace, grabExcessVerticalSpace, 1, 1);
    }

    public static GridData createGridData(int horizontalAlignment, int verticalAlignment, boolean grabExcessHorizontalSpace, boolean grabExcessVerticalSpace, int horizontalSpan, int verticalSpan) {
        GridData gridData = new GridData();
        gridData.horizontalAlignment = horizontalAlignment;
        gridData.verticalAlignment = verticalAlignment;
        gridData.grabExcessHorizontalSpace = grabExcessHorizontalSpace;
        gridData.grabExcessVerticalSpace = grabExcessVerticalSpace;
        gridData.horizontalSpan = horizontalSpan;
        gridData.verticalSpan = verticalSpan;
        return gridData;
    }

    public static GridData createGridData(int horizontalAlignment, int verticalAlignment, boolean grabExcessHorizontalSpace, boolean grabExcessVerticalSpace, int horizontalSpan, int verticalSpan, int widthHint, int heightHint) {
        GridData gridData = new GridData();
        gridData.horizontalAlignment = horizontalAlignment;
        gridData.verticalAlignment = verticalAlignment;
        gridData.grabExcessHorizontalSpace = grabExcessHorizontalSpace;
        gridData.grabExcessVerticalSpace = grabExcessVerticalSpace;
        gridData.horizontalSpan = horizontalSpan;
        gridData.verticalSpan = verticalSpan;
        gridData.widthHint = widthHint;
        gridData.heightHint = heightHint;
        return gridData;
    }

    public static void listen(Display display, int type, Object target, String method) {
        display.addFilter(type, new DelegateListener(target, method));
    }

    public static void listen(Widget widget, int type, Object target, String method) {
        widget.addListener(type, new DelegateListener(target, method));
    }

    public static Shell getOwnerShell(Event e) {
        if (e == null) {
            throw new NullPointerException();
        }
        Shell sh = null;
        if (e.widget instanceof Control) {
            sh = getShellFromControl((Control) e.widget);
        }
        if (sh == null && e.widget instanceof Item) {
            sh = getShellFromItem((Item) e.widget);
        }
        if (sh == null && e.item instanceof Control) {
            sh = getShellFromControl((Control) e.item);
        }
        if (sh == null && e.item instanceof Item) {
            sh = getShellFromItem((Item) e.item);
        }
        return sh;
    }

    public static Rectangle createRectangle(Point location, Point size) {
        return new Rectangle(location.x, location.y, size.x, size.y);
    }

    public static Rectangle createRectangle(int x, int y, Point size) {
        return new Rectangle(x, y, size.x, size.y);
    }

    public static int getRight(Rectangle r) {
        return r.x + r.width;
    }

    public static int getBottom(Rectangle r) {
        return r.y + r.height;
    }

    public static int getCenterX(Rectangle r) {
        return r.x + r.width / 2;
    }

    public static int getCenterY(Rectangle r) {
        return r.y + r.height / 2;
    }

    public static Rectangle cutLeftInHalf(Rectangle r) {
        return cutLeft(r, 0.5f);
    }

    public static Rectangle cutLeft(Rectangle r, float ratio) {
        if (ratio < 0 || ratio > 1) {
            throw new IllegalArgumentException();
        }
        int extent = (int) (r.width * ratio);
        return cutLeft(r, extent);
    }

    public static Rectangle cutLeft(Rectangle r, int extent) {
        if (extent < 0 || extent > r.width) {
            throw new IllegalArgumentException();
        }
        return new Rectangle(r.x, r.y, extent, r.height);
    }

    public static Rectangle cutRightInHalf(Rectangle r) {
        return cutRight(r, 0.5f);
    }

    public static Rectangle cutRight(Rectangle r, float ratio) {
        if (ratio < 0 || ratio > 1) {
            throw new IllegalArgumentException();
        }
        int extent = (int) (r.width * ratio);
        return cutRight(r, extent);
    }

    public static Rectangle cutRight(Rectangle r, int extent) {
        if (extent < 0 || extent > r.width) {
            throw new IllegalArgumentException();
        }
        return new Rectangle(r.x + r.width - extent, r.y, extent, r.height);
    }

    public static Rectangle cutTopInHalf(Rectangle r) {
        return cutTop(r, 0.5f);
    }

    public static Rectangle cutTop(Rectangle r, float ratio) {
        if (ratio < 0 || ratio > 1) {
            throw new IllegalArgumentException();
        }
        int extent = (int) (r.height * ratio);
        return cutTop(r, extent);
    }

    public static Rectangle cutTop(Rectangle r, int extent) {
        if (extent < 0 || extent > r.height) {
            throw new IllegalArgumentException();
        }
        return new Rectangle(r.x, r.y, r.width, extent);
    }

    public static Rectangle cutBottomInHalf(Rectangle r) {
        return cutBottom(r, 0.5f);
    }

    public static Rectangle cutBottom(Rectangle r, float ratio) {
        if (ratio < 0 || ratio > 1) {
            throw new IllegalArgumentException();
        }
        int extent = (int) (r.height * ratio);
        return cutBottom(r, extent);
    }

    public static Rectangle cutBottom(Rectangle r, int extent) {
        if (extent < 0 || extent > r.height) {
            throw new IllegalArgumentException();
        }
        return new Rectangle(r.x, r.y + r.height - extent, r.width, extent);
    }

    private static Shell getShellFromControl(Control c) {
        return c.getShell();
    }

    private static Shell getShellFromItem(Item i) {
        if (i instanceof MenuItem) {
            MenuItem mi = (MenuItem) i;
            return mi.getParent().getShell();
        } else if (i instanceof ToolItem) {
            ToolItem ti = (ToolItem) i;
            return ti.getParent().getShell();
        } else if (i instanceof CoolItem) {
            CoolItem ci = (CoolItem) i;
            return ci.getParent().getShell();
        } else if (i instanceof TabItem) {
            TabItem ti = (TabItem) i;
            return ti.getParent().getShell();
        } else if (i instanceof CTabItem) {
            CTabItem ti = (CTabItem) i;
            return ti.getParent().getShell();
        } else {
            return null;
        }
    }

    public static Object[] get(Composite c, Class type) {
        List l = new ArrayList();
        getRecursively(c, type, l);
        return l.toArray();
    }

    private static void getRecursively(Composite c, Class type, List l) {
        if (c == null) {
            return;
        } else if (c.getClass().equals(type)) {
            l.add(c);
        }
        Control[] cs = c.getChildren();
        for (int i = 0; i < cs.length; i++) {
            if (cs[i] instanceof Composite) {
                getRecursively((Composite) cs[i], type, l);
            }
        }
    }

    public static Rectangle toDisplay(Control c, Rectangle r) {
        Point pt = c.toDisplay(r.x, r.y);
        return new Rectangle(pt.x, pt.y, r.width, r.height);
    }

    public static Rectangle toControl(Control c, Rectangle r) {
        Point pt = c.toControl(r.x, r.y);
        return new Rectangle(pt.x, pt.y, r.width, r.height);
    }

    public static Object getImageManager(Class interfaceType, String base) {
        return getImageManager(interfaceType.getClassLoader(), interfaceType, base);
    }

    public static Object getImageManager(ClassLoader interfaceLoader, Class interfaceType, String base) {
        if (!ImageManager.class.isAssignableFrom(interfaceType)) {
            throw new IllegalArgumentException(interfaceType + " must extent " + ImageManager.class);
        }
        Object o = Proxy.newProxyInstance(interfaceLoader, new Class[] { interfaceType }, new ImageManagerInvocationHandler(interfaceType, base));
        return (ImageManager) o;
    }

    public static void insert(Composite parent, Control child, int index, Composite temp) {
        Control[] children = parent.getChildren();
        if (index < 0 || index > children.length) {
            throw new IndexOutOfBoundsException();
        }
        for (int i = index; i < children.length; i++) {
            children[i].setParent(temp);
        }
        child.setParent(parent);
        for (int i = index; i < children.length; i++) {
            children[i].setParent(parent);
        }
        parent.layout();
    }

    public static void replace(Control old, Control new0, Composite temp) {
        Composite dstParent = old.getParent();
        Control[] children = dstParent.getChildren();
        int index = -1;
        for (int i = 0; i < children.length; i++) {
            if (children[i].equals(old)) {
                index = i;
                break;
            }
        }
        if (index < 0) {
            throw new IllegalArgumentException();
        }
        for (int i = index; i < children.length; i++) {
            children[i].setParent(temp);
        }
        new0.setParent(dstParent);
        index++;
        for (int i = index; i < children.length; i++) {
            children[i].setParent(dstParent);
        }
        dstParent.layout();
    }

    public static int indexOf(Composite parent, Control child) {
        Control[] children = parent.getChildren();
        for (int i = 0; i < children.length; i++) {
            if (children[i].equals(child)) {
                return i;
            }
        }
        return -1;
    }

    public static boolean isDisposed(Object o) {
        try {
            if (o == null) {
                return true;
            }
            Class type = o.getClass();
            Method isDisposed = type.getMethod("isDisposed");
            Object anObject = isDisposed.invoke(o, (Object[]) null);
            if (anObject == null || !(anObject instanceof Boolean)) {
                throw new NoSuchMethodException("Incorrect method signature, expecting 'boolean isDisposed()'.");
            }
            Boolean aBoolean = (Boolean) anObject;
            return aBoolean.booleanValue();
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static void dispose(Object o) {
        try {
            if (o == null) {
                return;
            }
            Class type = o.getClass();
            if (!isDisposed(o)) {
                Method dispose = type.getMethod("dispose", (Class[]) null);
                dispose.invoke(o, (Object[]) null);
            }
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public static boolean isMac() {
        return System.getProperty("os.name").toLowerCase().contains("mac");
    }

    private static boolean isIsDisposedMethod(Method method) {
        if (!method.getName().equals("isDisposed")) {
            return false;
        }
        if (!boolean.class.equals(method.getReturnType())) {
            return false;
        }
        if (!Util.isArrayEmpty(method.getParameterTypes())) {
            return false;
        }
        return true;
    }

    private static boolean isDisposeMethod(Method method) {
        if (!method.getName().equals("dispose")) {
            return false;
        }
        if (!void.class.equals(method.getReturnType())) {
            return false;
        }
        if (!Util.isArrayEmpty(method.getParameterTypes())) {
            return false;
        }
        return true;
    }

    public static void center(Shell shell) {
        Rectangle parentBounds = shell.getParent() == null ? shell.getDisplay().getClientArea() : shell.getBounds();
        Rectangle bounds = shell.getBounds();
        shell.setLocation(parentBounds.x + (parentBounds.width + bounds.width) / 2, parentBounds.y + (parentBounds.height + bounds.height) / 2);
    }
}
