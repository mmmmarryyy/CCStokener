package com.evver.cardplatform;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.Properties;

/**
 * This class represents a basic card, with a name, suit, and value.  
 * The class also contains image url's for both enabled and disabled states.
 * As a core security constraint of the Evver Games platform, this class
 * is immutable.
 * 
 * @author ronald
 *
 */
public class Card implements Serializable {

    static final long serialVersionUID = 1L;

    public static final String SUIT_KEY = "suit";

    public static final String NAME_KEY = "name";

    public static final String VALUE_KEY = "value";

    public static final String ENABLED_KEY = "enabledfront";

    public static final String DISABLED_KEY = "disabledfront";

    public static final String MINI_ENABLED_KEY = "minienabledfront";

    public static final String MINI_DISABLED_KEY = "minidisabledfront";

    private String imgsURL;

    private String baseURL;

    private String enabledURL;

    private String disabledURL;

    private String miniEnabledURL;

    private String miniDisabledURL;

    private Long cardID;

    private String suit;

    private String name;

    private Integer value;

    /**
	 * Constructs a card using the passed id
	 */
    public Card(Long id, String baseURL, String imgsURL) {
        if (id != null && baseURL != null && imgsURL != null) {
            Properties props = getResourceProperties(id, baseURL);
            this.value = Integer.parseInt(props.getProperty(VALUE_KEY));
            this.suit = props.getProperty(SUIT_KEY);
            this.name = props.getProperty(NAME_KEY);
            this.baseURL = baseURL;
            this.imgsURL = imgsURL;
            this.cardID = id;
            this.enabledURL = imgsURL + props.getProperty(ENABLED_KEY);
            this.disabledURL = imgsURL + props.getProperty(DISABLED_KEY);
            this.miniEnabledURL = imgsURL + props.getProperty(MINI_ENABLED_KEY);
            this.miniDisabledURL = imgsURL + props.getProperty(MINI_DISABLED_KEY);
        }
    }

    /**
	 * Gets the card name
	 * @return card name
	 */
    public String getName() {
        return this.name;
    }

    /**
	 * Gets the card value
	 * @return card value
	 */
    public Integer getValue() {
        return this.value;
    }

    /**
	 * Gets the card suit
	 * @return card suit
	 */
    public String getSuit() {
        return this.suit;
    }

    /**
	 * Gets the card id
	 * @return the card id
	 */
    public Long getCardId() {
        return this.cardID;
    }

    /***
	 * Gets the base resource URL for this card
	 * @return the resource URL for this card
	 */
    public String getBaseURL() {
        return this.baseURL;
    }

    /**
	 * Gets the images URL for this card
	 * @return the images URL for this card
	 */
    public String getImagesURL() {
        return this.imgsURL;
    }

    /**
	 * Gets the full size enabled image URL for this card
	 * @return the enabled image URL for this card
	 */
    public String getEnabledImageURL() {
        return getEnabledImageURL(true);
    }

    /**
	 * Gets the enabled image URL for this card
	 * @param fullsize get the full size image URL
	 * @return the enabled image URL for this card
	 */
    public String getEnabledImageURL(boolean fullsize) {
        if (fullsize) return this.enabledURL; else return this.miniEnabledURL;
    }

    /**
	 * Gets the full size disabled image URL for this card
	 * @return the disabled image URL for this card
	 */
    public String getDisabledImageURL() {
        return getDisabledImageURL(true);
    }

    /**
	 * Gets the disabled image URL for this card
	 * @param fullsize get the full size image URL
	 * @return the disabled image URL for this card
	 */
    public String getDisabledImageURL(boolean fullsize) {
        if (fullsize) return this.disabledURL; else return this.miniDisabledURL;
    }

    /**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
    public boolean equals(Object obj) {
        if (obj instanceof Card) return ((Card) obj).cardID.equals(cardID); else return false;
    }

    /**
	 * @see java.lang.Object#hashCode()
	 */
    public int hashCode() {
        if (getCardId() != null) return getCardId().hashCode(); else return super.hashCode();
    }

    /**
	 * @see java.lang.Object#toString()
	 */
    public String toString() {
        return "Card[" + getSuit() + ", " + getValue() + "]";
    }

    /**
	 * Gets the resource properties for this card
	 * @param id the card id
	 * @param baseURL the base resource URL
	 * @return the properties for this card
	 */
    protected final Properties getResourceProperties(Long id, String baseURL) {
        try {
            URL url = getClass().getResource(baseURL + id + ".properties");
            if (url == null) {
                url = new URL(baseURL + id + ".properties");
            }
            Properties props = new Properties();
            InputStream is = url.openStream();
            props.load(is);
            is.close();
            return props;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
