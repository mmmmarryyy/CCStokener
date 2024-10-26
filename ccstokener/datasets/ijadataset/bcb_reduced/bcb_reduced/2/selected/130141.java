package org.lnicholls.galleon.apps.menu;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import org.apache.log4j.Logger;
import org.lnicholls.galleon.app.AppFactory;
import org.lnicholls.galleon.database.PersistentValue;
import org.lnicholls.galleon.database.PersistentValueManager;
import org.lnicholls.galleon.server.Server;
import org.lnicholls.galleon.util.NameValue;
import org.lnicholls.galleon.util.Tools;
import org.lnicholls.galleon.widget.DefaultSDOnlyApplication;
import org.lnicholls.galleon.widget.DefaultMenuScreen;
import org.lnicholls.galleon.widget.DefaultOptionsScreen;
import org.lnicholls.galleon.widget.OptionsButton;
import com.tivo.core.ds.TeDict;
import com.tivo.hme.bananas.BEvent;
import com.tivo.hme.bananas.BText;
import com.tivo.hme.bananas.BView;
import com.tivo.hme.interfaces.IContext;
import com.tivo.hme.sdk.HmeEvent;
import com.tivo.hme.sdk.Resource;

public class Menu extends DefaultSDOnlyApplication {

    private static Logger log = Logger.getLogger(Menu.class.getName());

    private static final Runtime runtime = Runtime.getRuntime();

    public static final String TITLE = "Menu";

    private Resource mMenuBackground;

    private Resource mFolderIcon;

    public void init(IContext context) throws Exception {
        super.init(context);
    }

    public void initService() {
        super.initService();
        mMenuBackground = getSkinImage("menu", "background");
        mFolderIcon = getSkinImage("menu", "folder");
        MenuConfiguration menuConfiguration = (MenuConfiguration) ((MenuFactory) getFactory()).getAppContext().getConfiguration();
        mLocationMenuScreen = new LocationMenuScreen(this);
        push(mLocationMenuScreen, TRANSITION_NONE);
        initialize();
    }

    public class LocationMenuScreen extends DefaultMenuScreen {

        public LocationMenuScreen(Menu app) {
            super(app, "Menu");
            if (!Server.getServer().getServerConfiguration().getIPAddress().startsWith("192")) setTitle("galleon.sourceforge.net");
            getBelow().setResource(mMenuBackground);
            setFooter("Press ENTER for options");
            MenuConfiguration menuConfiguration = (MenuConfiguration) ((MenuFactory) getFactory()).getAppContext().getConfiguration();
            List list = Server.getServer().getAppUrls(false);
            Iterator iterator = Server.getServer().getAppUrls(false).iterator();
            while (iterator.hasNext()) {
                NameValue nameValue = (NameValue) iterator.next();
                mMenuList.add(nameValue);
            }
            createMenu();
        }

        public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
            if (isReturn) {
                createMenu();
            }
            return super.handleEnter(arg, isReturn);
        }

        private void createMenu() {
            mMenuList.clear();
            ArrayList list = new ArrayList();
            MenuConfiguration menuConfiguration = (MenuConfiguration) ((MenuFactory) getFactory()).getAppContext().getConfiguration();
            Iterator iterator = Server.getServer().getAppUrls(false).iterator();
            while (iterator.hasNext()) {
                NameValue nameValue = (NameValue) iterator.next();
                list.add(nameValue);
            }
            NameValue apps[] = new NameValue[0];
            apps = (NameValue[]) list.toArray(apps);
            boolean sorted = false;
            PersistentValue persistentValue = PersistentValueManager.loadPersistentValue(Menu.this.getClass().getName() + "." + "sort");
            if (persistentValue != null) {
                sorted = persistentValue.getValue().equals("true");
            }
            if (sorted) {
                Arrays.sort(apps, new Comparator() {

                    public int compare(Object o1, Object o2) {
                        NameValue nameValue1 = (NameValue) o1;
                        NameValue nameValue2 = (NameValue) o2;
                        return nameValue1.getName().compareToIgnoreCase(nameValue2.getName());
                    }
                });
            }
            for (int i = 0; i < apps.length; i++) {
                mMenuList.add(apps[i]);
            }
        }

        public boolean handleAction(BView view, Object action) {
            if (action.equals("push") || action.equals("play")) {
                load();
                new Thread() {

                    public void run() {
                        try {
                            NameValue value = (NameValue) (mMenuList.get(mMenuList.getFocus()));
                            byte mem[] = new byte[2];
                            mem[0] = (byte) mMenuList.getFocus();
                            mem[1] = (byte) mMenuList.getTop();
                            TeDict params = new TeDict();
                            transitionForward(value.getValue(), params, mem);
                        } catch (Exception ex) {
                            Tools.logException(Menu.class, ex);
                        }
                    }
                }.start();
                return true;
            }
            return super.handleAction(view, action);
        }

        protected void createRow(BView parent, int index) {
            BView icon = new BView(parent, 9, 2, 32, 32);
            NameValue value = (NameValue) mMenuList.get(index);
            try {
                URL url = new URL(value.getValue() + "icon.png");
                URLConnection urlConnection = url.openConnection();
                InputStream inputStream = urlConnection.getInputStream();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                byte[] data = new byte[1024];
                int success = inputStream.read(data);
                while (success != -1) {
                    baos.write(data, 0, success);
                    success = inputStream.read(data);
                }
                baos.close();
                inputStream.close();
                icon.setResource(createImage(baos.toByteArray()));
            } catch (Exception ex) {
                icon.setResource(mFolderIcon);
            }
            BText name = new BText(parent, 50, 4, parent.getWidth() - 40, parent.getHeight() - 4);
            name.setShadow(true);
            name.setFlags(RSRC_HALIGN_LEFT);
            name.setValue(Tools.trim(value.getName(), 40));
        }

        public boolean handleKeyPress(int code, long rawcode) {
            switch(code) {
                case KEY_PLAY:
                    postEvent(new BEvent.Action(this, "play"));
                    return true;
                case KEY_ENTER:
                    getBApp().push(new OptionsScreen((Menu) getBApp()), TRANSITION_LEFT);
            }
            return super.handleKeyPress(code, rawcode);
        }
    }

    public class OptionsScreen extends DefaultOptionsScreen {

        public OptionsScreen(DefaultSDOnlyApplication app) {
            super(app);
            getBelow().setResource(mMenuBackground);
            boolean sorted = false;
            PersistentValue persistentValue = PersistentValueManager.loadPersistentValue(Menu.this.getClass().getName() + "." + "sort");
            if (persistentValue != null) {
                sorted = persistentValue.getValue().equals("true");
            }
            int start = TOP;
            int width = 280;
            int increment = 37;
            int height = 25;
            BText text = new BText(getNormal(), BORDER_LEFT, start, BODY_WIDTH, 30);
            text.setFlags(RSRC_HALIGN_LEFT | RSRC_TEXT_WRAP | RSRC_VALIGN_CENTER);
            text.setFont("default-24-bold.font");
            text.setShadow(true);
            text.setValue("Sort");
            NameValue[] nameValues = new NameValue[] { new NameValue("Yes", "true"), new NameValue("No", "false") };
            mSortedButton = new OptionsButton(getNormal(), BORDER_LEFT + BODY_WIDTH - width, start, width, height, true, nameValues, String.valueOf(sorted));
            setFocusDefault(mSortedButton);
        }

        public boolean handleEnter(java.lang.Object arg, boolean isReturn) {
            getBelow().setResource(mMenuBackground);
            return super.handleEnter(arg, isReturn);
        }

        public boolean handleExit() {
            try {
                DefaultSDOnlyApplication application = (DefaultSDOnlyApplication) getApp();
                if (!application.isDemoMode()) {
                    PersistentValueManager.savePersistentValue(Menu.this.getClass().getName() + "." + "sort", mSortedButton.getValue());
                    Server.getServer().updateApp(((MenuFactory) getFactory()).getAppContext());
                }
            } catch (Exception ex) {
                Tools.logException(Menu.class, ex, "Could not configure menu app");
            }
            return super.handleExit();
        }

        private OptionsButton mSortedButton;
    }

    public static class MenuFactory extends AppFactory {

        public void initialize() {
            MenuConfiguration menuConfiguration = (MenuConfiguration) getAppContext().getConfiguration();
        }
    }

    public boolean handleInitInfo(HmeEvent.InitInfo info) {
        if (mLocationMenuScreen != null) {
            if (getMemento() != null && getMemento().length > 0) {
                int pos = getMemento()[0];
                int top = getMemento()[1];
                mLocationMenuScreen.getMenuList().setTop(top);
                mLocationMenuScreen.getMenuList().setFocus(pos, false);
                mLocationMenuScreen.getMenuList().flush();
            }
        }
        return true;
    }

    private LocationMenuScreen mLocationMenuScreen;
}
