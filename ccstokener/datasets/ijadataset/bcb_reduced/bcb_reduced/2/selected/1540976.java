package pms.whq;

import java.awt.Font;
import java.awt.FontFormatException;
import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.imageio.*;

/**
 *
 * @author psiegel
 */
public class Settings {

    public static final String MONSTER_DIR = "MonsterDir";

    public static final String EVENT_DIR = "EventDir";

    public static final String TABLE_DIR = "TableDir";

    public static final String RULES_DIR = "RulesDir";

    public static final String IMG_DIR = "ImageDir";

    public static final String MONSTER_IMG_DIR = "MonsterImageDir";

    public static final String EVENT_IMG_DIR = "EventImageDir";

    public static final String CARD_WIDTH = "CardWidth";

    public static final String CARD_HEIGHT = "CardHeight";

    public static final String FONT_DIR = "FontDir";

    public static final String SIMULATE_DECK = "SimulateDeck";

    public static final String PARTY_SIZE = "PartySize";

    public static final String AUTO_CLOSE_CARDS = "AutoCloseCards";

    public static final String EVENT_PROBABILITY = "EventPropability";

    protected static Properties mSettings;

    protected static Map mFonts;

    static {
        mSettings = new Properties();
        mSettings.setProperty(MONSTER_DIR, "data/xml/monsters/");
        mSettings.setProperty(EVENT_DIR, "data/xml/events/");
        mSettings.setProperty(TABLE_DIR, "data/xml/tables/");
        mSettings.setProperty(RULES_DIR, "data/xml/rules/");
        mSettings.setProperty(IMG_DIR, "data/graphics/");
        mSettings.setProperty(MONSTER_IMG_DIR, "data/graphics/monsters/");
        mSettings.setProperty(EVENT_IMG_DIR, "data/graphics/events/");
        mSettings.setProperty(CARD_WIDTH, "240");
        mSettings.setProperty(CARD_HEIGHT, "370");
        mSettings.setProperty(FONT_DIR, "data/fonts/");
        mSettings.setProperty(PARTY_SIZE, "4");
        mSettings.setProperty(EVENT_PROBABILITY, "37");
        mSettings.setProperty(AUTO_CLOSE_CARDS, "true");
        mFonts = new HashMap();
    }

    /** Creates a new instance of EventDeckSettings */
    public Settings() {
    }

    public static void load() {
        File file = new File("settings.cfg");
        try {
            mSettings.load(new FileInputStream(file));
        } catch (FileNotFoundException fnfe) {
        } catch (IOException ioex) {
        }
    }

    public static void save() {
        File file = new File("settings.cfg");
        try {
            mSettings.store(new FileOutputStream(file), "EventDeck Settings");
        } catch (FileNotFoundException fnfe) {
        } catch (IOException ioex) {
        }
    }

    public static String getSetting(String setting) {
        return mSettings.getProperty(setting);
    }

    public static int getSettingAsInt(String setting) {
        int value = 0;
        String sValue = getSetting(setting);
        try {
            value = Integer.parseInt(sValue);
        } catch (NumberFormatException nfe) {
        }
        return value;
    }

    public static boolean getSettingAsBool(String setting) {
        return Boolean.parseBoolean(getSetting(setting));
    }

    public static void setSetting(String setting, String value) {
        mSettings.setProperty(setting, value);
    }

    public static BufferedImage loadImage(String path) {
        BufferedImage img = null;
        String s = new String();
        URL bgURL = s.getClass().getResource("/" + path);
        if (bgURL == null) {
            File file = new File(path);
            try {
                bgURL = file.toURL();
            } catch (MalformedURLException mue) {
            }
        }
        if (bgURL != null) {
            try {
                img = ImageIO.read(bgURL);
            } catch (IOException ioex) {
            }
        }
        return img;
    }

    public static Font getFont(String name, int style, int size) {
        Font base = null;
        if (mFonts.containsKey(name)) {
            base = (Font) mFonts.get(name);
        } else {
            String path = getSetting(FONT_DIR) + name + ".ttf";
            URL url = path.getClass().getResource("/" + path);
            if (url == null) {
                File file = new File(path);
                try {
                    url = file.toURL();
                } catch (MalformedURLException mue) {
                }
            }
            if (url != null) {
                try {
                    base = Font.createFont(Font.TRUETYPE_FONT, url.openStream());
                } catch (IOException ioex) {
                } catch (FontFormatException ffe) {
                }
            }
        }
        if (base != null) {
            mFonts.put(name, base);
            return base.deriveFont(style, size);
        }
        return null;
    }
}
