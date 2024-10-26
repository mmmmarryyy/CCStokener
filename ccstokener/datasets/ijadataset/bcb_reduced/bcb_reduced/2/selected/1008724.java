package sun.awt;

import java.awt.*;
import java.awt.im.InputMethodHighlight;
import java.io.*;
import java.net.URL;
import java.util.Collections;
import java.util.Map;
import sun.awt.image.ByteArrayImageSource;
import sun.awt.image.FileImageSource;
import sun.awt.image.URLImageSource;
import sun.awt.im.InputMethod;
import sun.misc.SoftCache;

public abstract class SunToolkit extends Toolkit {

    private static final String POST_EVENT_QUEUE_KEY = "PostEventQueue";

    public SunToolkit() {
        EventQueue theEventQueue;
        String eqName = Toolkit.getProperty("AWT.EventQueueClass", "java.awt.EventQueue");
        try {
            theEventQueue = (EventQueue) Class.forName(eqName).newInstance();
        } catch (Exception e) {
            System.err.println("Failed loading " + eqName + ": " + e);
            theEventQueue = new EventQueue();
        }
        AppContext appContext = AppContext.getAppContext();
        appContext.put(AppContext.EVENT_QUEUE_KEY, theEventQueue);
        PostEventQueue postEventQueue = new PostEventQueue(theEventQueue);
        appContext.put(POST_EVENT_QUEUE_KEY, postEventQueue);
    }

    public static AppContext createNewAppContext() {
        return createNewAppContext(Thread.currentThread().getThreadGroup());
    }

    static AppContext createNewAppContext(ThreadGroup threadGroup) {
        EventQueue eventQueue;
        String eqName = Toolkit.getProperty("AWT.EventQueueClass", "java.awt.EventQueue");
        try {
            eventQueue = (EventQueue) Class.forName(eqName).newInstance();
        } catch (Exception e) {
            System.err.println("Failed loading " + eqName + ": " + e);
            eventQueue = new EventQueue();
        }
        AppContext appContext = new AppContext(threadGroup);
        appContext.put(AppContext.EVENT_QUEUE_KEY, eventQueue);
        PostEventQueue postEventQueue = new PostEventQueue(eventQueue);
        appContext.put(POST_EVENT_QUEUE_KEY, postEventQueue);
        return appContext;
    }

    private static final Map appContextMap = Collections.synchronizedMap(new IdentityWeakHashMap());

    public static AppContext targetToAppContext(Object target) {
        if (target != null && !GraphicsEnvironment.isHeadless()) {
            return (AppContext) appContextMap.get(target);
        }
        return null;
    }

    public static void insertTargetMapping(Object target, AppContext appContext) {
        if (!GraphicsEnvironment.isHeadless()) {
            appContextMap.put(target, appContext);
        }
    }

    protected EventQueue getSystemEventQueueImpl() {
        AppContext appContext = AppContext.getAppContext();
        EventQueue theEventQueue = (EventQueue) appContext.get(AppContext.EVENT_QUEUE_KEY);
        return theEventQueue;
    }

    public static void postEvent(AppContext appContext, AWTEvent event) {
        PostEventQueue postEventQueue = (PostEventQueue) appContext.get(POST_EVENT_QUEUE_KEY);
        if (postEventQueue != null) {
            postEventQueue.postEvent(event);
        }
    }

    public Dimension getScreenSize() {
        return new Dimension(getScreenWidth(), getScreenHeight());
    }

    public abstract String getDefaultCharacterEncoding();

    protected abstract int getScreenWidth();

    protected abstract int getScreenHeight();

    static SoftCache imgCache = new SoftCache();

    static synchronized Image getImageFromHash(Toolkit tk, URL url) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            try {
                java.security.Permission perm = url.openConnection().getPermission();
                if (perm != null) {
                    try {
                        sm.checkPermission(perm);
                    } catch (SecurityException se) {
                        if ((perm instanceof java.io.FilePermission) && perm.getActions().indexOf("read") != -1) {
                            sm.checkRead(perm.getName());
                        } else if ((perm instanceof java.net.SocketPermission) && perm.getActions().indexOf("connect") != -1) {
                            sm.checkConnect(url.getHost(), url.getPort());
                        } else {
                            throw se;
                        }
                    }
                }
            } catch (java.io.IOException ioe) {
                sm.checkConnect(url.getHost(), url.getPort());
            }
        }
        Image img = (Image) imgCache.get(url);
        if (img == null) {
            try {
                img = tk.createImage(new URLImageSource(url));
                imgCache.put(url, img);
            } catch (Exception e) {
            }
        }
        return img;
    }

    static synchronized Image getImageFromHash(Toolkit tk, String filename) {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkRead(filename);
        }
        Image img = (Image) imgCache.get(filename);
        if (img == null) {
            try {
                img = tk.createImage(new FileImageSource(filename));
                imgCache.put(filename, img);
            } catch (Exception e) {
            }
        }
        return img;
    }

    public Image getImage(String filename) {
        return getImageFromHash(this, filename);
    }

    public Image getImage(URL url) {
        return getImageFromHash(this, url);
    }

    public Image createImage(String filename) {
        SecurityManager security = System.getSecurityManager();
        if (security != null) {
            security.checkRead(filename);
        }
        return createImage(new FileImageSource(filename));
    }

    public Image createImage(URL url) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            try {
                java.security.Permission perm = url.openConnection().getPermission();
                if (perm != null) {
                    try {
                        sm.checkPermission(perm);
                    } catch (SecurityException se) {
                        if ((perm instanceof java.io.FilePermission) && perm.getActions().indexOf("read") != -1) {
                            sm.checkRead(perm.getName());
                        } else if ((perm instanceof java.net.SocketPermission) && perm.getActions().indexOf("connect") != -1) {
                            sm.checkConnect(url.getHost(), url.getPort());
                        } else {
                            throw se;
                        }
                    }
                }
            } catch (java.io.IOException ioe) {
                sm.checkConnect(url.getHost(), url.getPort());
            }
        }
        return createImage(new URLImageSource(url));
    }

    public Image createImage(byte[] data, int offset, int length) {
        return createImage(new ByteArrayImageSource(data, offset, length));
    }

    /**
     * Returns whether enableInputMethods should be set to true for peered
     * TextComponent instances on this platform. False by default.
     */
    public boolean enableInputMethodsForTextComponent() {
        return false;
    }

    /**
     *  Show the specified window in a multi-vm environment
     */
    public void activate(Window window) {
        return;
    }

    /**
     *  Hide the specified window in a multi-vm environment
     */
    public void deactivate(Window window) {
        return;
    }
}

class PostEventQueue extends Thread {

    private static int threadNum = 0;

    private EventQueueItem queueHead = null;

    private EventQueueItem queueTail = null;

    private boolean keepGoing = true;

    private final EventQueue eventQueue;

    PostEventQueue(EventQueue eq) {
        super("SunToolkit.PostEventQueue-" + threadNum);
        synchronized (PostEventQueue.class) {
            threadNum++;
        }
        eventQueue = eq;
        start();
    }

    public void run() {
        while (keepGoing && !isInterrupted()) {
            try {
                EventQueueItem item;
                synchronized (this) {
                    while (keepGoing && (queueHead == null)) {
                        notifyAll();
                        wait();
                    }
                    if (!keepGoing) break;
                    item = queueHead;
                }
                eventQueue.postEvent(item.event);
                synchronized (this) {
                    queueHead = queueHead.next;
                    if (queueHead == null) queueTail = null;
                }
            } catch (InterruptedException e) {
                keepGoing = false;
                synchronized (this) {
                    notifyAll();
                }
            }
        }
    }

    synchronized void postEvent(AWTEvent event) {
        EventQueueItem item = new EventQueueItem(event);
        if (queueHead == null) {
            queueHead = queueTail = item;
            notifyAll();
        } else {
            queueTail.next = item;
            queueTail = item;
        }
    }

    void flush() {
        if (Thread.currentThread() == this) {
            return;
        }
        synchronized (this) {
            if (queueHead != null) {
                try {
                    wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }

    synchronized void quitRunning() {
        keepGoing = false;
        notifyAll();
    }
}

class EventQueueItem {

    AWTEvent event;

    EventQueueItem next;

    EventQueueItem(AWTEvent evt) {
        event = evt;
    }
}
