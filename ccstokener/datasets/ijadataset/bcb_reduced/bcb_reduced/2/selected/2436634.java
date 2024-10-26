package com.vscorp.ui.swing;

import java.awt.AWTError;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;

/** 
 * Collection of static convenience methods for Swing
 * See com.vscorp.ui.swing.VSBackgroundTask for an example.
 *
 * @see com.vscorp.ui.swing.VSBackgroundTask
 *
 * @author Daniel A. Syrstad
 */
public class VSSwingUtil {

    private static Font toolbarFont;

    /** Allows invocation of a method on a class like SwingUtilities.invokeLater()
    * except that it doesn't require a Runnable inner class and the complications
    * which go with it. The method identified by aMethodName will be invoked
    * from the Swing event thread.
    *
    * @param aTargetObj the object on which to invoke aMethodName
    * @param aParameter a parameter to be passed to aMethodName, may be null
    * @param aMethodName the method on aTargetObj to invoke
    */
    public static void invokeLater(Object aTargetObj, Object aParameter, String aMethodName) {
        SwingUtilities.invokeLater(new VSInvokeLaterHelper(aTargetObj, aParameter, aMethodName));
    }

    /** Finds the aMethodName on aTargetObj or any of its super classes.
	 * Classes are searched from their leaf backwards. Throws AWTError 
	 * (unchecked) if the method cannot be found.
	 */
    public static Method resolveMethod(Object aTargetObj, String aMethodName, Class[] someParameters) {
        Class clazz = aTargetObj.getClass();
        Exception noMethodException = null;
        for (; clazz != null; clazz = clazz.getSuperclass()) {
            try {
                Method method = clazz.getDeclaredMethod(aMethodName, someParameters);
                method.setAccessible(true);
                return method;
            } catch (NoSuchMethodException e) {
                if (noMethodException == null) noMethodException = e;
            } catch (Exception e) {
                throw new AWTError("Method exception:" + e);
            }
        }
        throw new AWTError("Method exception:" + noMethodException);
    }

    /** Invokes the specified method like Method.invoke. Exceptions thrown by the method
	 * are remapped to AWTError (unchecked exception). If aMethod is null, nothing
	 * happens.
	 */
    public static void invokeMethod(Method aMethod, Object aTargetObj, Object[] someParameters) {
        if (aMethod == null) return;
        try {
            aMethod.invoke(aTargetObj, someParameters);
        } catch (InvocationTargetException e) {
            String msg = e.getTargetException().toString() + '\n';
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            e.printStackTrace(printWriter);
            msg += stringWriter.toString();
            printWriter.close();
            throw new AWTError(msg);
        } catch (Exception e) {
            throw new AWTError("Method exception:" + e);
        }
    }

    /** Find the requested resource (file) like Class.getResource(), but it works
	 * for both applets and applications. Returns null if it cannot find the resource.
	 */
    public static URL getResource(Class _class, String resource) {
        URL url = _class.getResource(resource);
        if (url == null) {
        }
        return url;
    }

    /** Open the requested resource (file) like Class.getResourceAsStream(), but it works
	 * for both applets and applications. Throws an IOException if it cannot find the resource.
	 */
    public static InputStream getResourceAsStream(Class _class, String resource) throws IOException {
        URL url = VSSwingUtil.getResource(_class, resource);
        if (url == null) throw new IOException("Cannot find resource " + resource);
        try {
            return url.openStream();
        } catch (IOException e) {
            throw new IOException(e + "Cannot open resource " + url);
        }
    }

    /** Get a ImageIcon resource.
	 */
    public static ImageIcon getImageIconResource(Class _class, String resource) {
        byte[] buffer = null;
        try {
            InputStream ins = _class.getResourceAsStream(resource);
            if (ins == null) {
                System.err.println(_class.getName() + "/" + resource + " not found.");
                return null;
            }
            BufferedInputStream in = new BufferedInputStream(ins);
            ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
            buffer = new byte[1024];
            int n;
            while ((n = in.read(buffer)) > 0) out.write(buffer, 0, n);
            in.close();
            out.flush();
            buffer = out.toByteArray();
            if (buffer.length == 0) {
                System.err.println(resource + " is zero-length");
                return null;
            }
        } catch (IOException e) {
            System.err.println(e.toString());
            return null;
        }
        return new ImageIcon(buffer);
    }

    public static JButton addToolBarButton(JComponent toolbar, Class _class, String image_resrc, String label, String tooltip) {
        JButton button;
        ImageIcon icon = VSSwingUtil.getImageIconResource(_class, image_resrc);
        if (label == null) button = new JButton(icon); else button = new JButton(label, icon);
        if (toolbarFont == null) {
            Font f = button.getFont();
            toolbarFont = new Font(f.getName(), f.getStyle(), 10);
        }
        button.setFont(toolbarFont);
        button.setMargin(new Insets(1, 1, 1, 1));
        button.setVerticalTextPosition(AbstractButton.BOTTOM);
        button.setHorizontalTextPosition(AbstractButton.CENTER);
        button.setToolTipText(tooltip);
        button.setFocusPainted(false);
        toolbar.add(button);
        return button;
    }

    /** Make the size of all toolbar buttons uniform.
	 */
    public static void resizeToolBarButtons(JComponent toolbar) {
        Component[] components = toolbar.getComponents();
        int len = components.length;
        Dimension max_dim = new Dimension(0, 0);
        for (int i = 0; i < len; i++) {
            if (!(components[i] instanceof JButton)) continue;
            Dimension size = components[i].getPreferredSize();
            if (size.width > max_dim.width) max_dim.width = size.width;
            if (size.height > max_dim.height) max_dim.height = size.height;
        }
        for (int i = 0; i < len; i++) {
            if (!(components[i] instanceof JButton)) continue;
            JComponent jcomponent = (JComponent) components[i];
            jcomponent.setPreferredSize(max_dim);
            jcomponent.setMinimumSize(max_dim);
            jcomponent.setMaximumSize(max_dim);
            jcomponent.setSize(max_dim);
        }
    }

    /** Creates a new JPanel with a BoxLayout. axis is BosLayout.{X|Y}_AXIS.
	 */
    public static JPanel createBoxPanel(int axis) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, axis));
        return panel;
    }
}
