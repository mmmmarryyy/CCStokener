package com.evver.evvercards;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

/**
 * This class represents a basic card, with a name, suit, value, locked and 
 * shown flags.  The values of this class may be null, which is interpreted
 * to mean that the particular values of this card are not readable by a user.   
 * 
 * @author ronald
 *
 */
public class Card implements Serializable {

    static final long serialVersionUID = 1L;

    public static final String NAME_KEY = "name";

    public static final String SUIT_KEY = "suit";

    public static final String VALUE_KEY = "value";

    public static final String SHOWN_KEY = "shown";

    public static final String LOCKED_KEY = "locked";

    public static final String BASE_URL = "baseurl";

    public static final String PROP_ORIGIN = "origin";

    public static final String PROP_DRAWPILE = "draw";

    public static final String PROP_DISCARDPILE = "discard";

    private Long cardID;

    private String name;

    private String suit;

    private Integer value;

    private Boolean shown;

    private Boolean locked;

    @SuppressWarnings("unchecked")
    private Map props;

    /**
	 * Constructs a card with null values
	 */
    public Card() {
    }

    /**
	 * Constructs a card with identical values as the passed card
	 * @param card the card to duplicate
	 */
    public Card(Card card) {
        if (card != null) {
            this.cardID = card.cardID;
            this.locked = card.locked;
            this.shown = card.shown;
            this.value = card.value;
            this.suit = card.suit;
            this.name = card.name;
            this.props = card.props;
        }
    }

    /**
	 * Constructs a card using properties form a URL
	 * @param id the card ID
	 * @param baseURL the base properties URL
	 */
    @SuppressWarnings("unchecked")
    public Card(Long id, String baseURL) {
        if (id != null && baseURL != null) {
            this.props = getResourceProperties(id, baseURL);
            this.locked = Boolean.parseBoolean((String) props.get(LOCKED_KEY));
            this.shown = Boolean.parseBoolean((String) props.get(SHOWN_KEY));
            this.value = Integer.parseInt((String) props.get(VALUE_KEY));
            this.suit = (String) props.get(SUIT_KEY);
            this.name = (String) props.get(NAME_KEY);
            this.cardID = id;
            this.props.put(BASE_URL, baseURL);
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
	 * Sets the name of the card
	 * @param name the name to set
	 */
    public void setName(String name) {
        this.name = name;
    }

    /**
	 * Gets the card value
	 * @return card value
	 */
    public Integer getValue() {
        return this.value;
    }

    /**
	 * Sets the value of the card
	 * @param value the value to set
	 */
    public void setValue(Integer value) {
        this.value = value;
    }

    /**
	 * Gets the card suit
	 * @return card suit
	 */
    public String getSuit() {
        return this.suit;
    }

    /**
	 * Sets the suit of this card
	 * @param suit the suit to set
	 */
    public void setSuit(String suit) {
        this.suit = suit;
    }

    /**
	 * Gets the card id
	 * @return the card id
	 */
    public Long getCardId() {
        return this.cardID;
    }

    /**
	 * Sets the ID of this card
	 * @param id the ID to set
	 */
    public void setCardId(Long id) {
        this.cardID = id;
    }

    /**
	 * Gets if this card is shown
	 * @return the card is shown
	 */
    public Boolean getShown() {
        return this.shown;
    }

    /**
	 * Sets the card as shown
	 * @param shown the flag to set
	 */
    public void setShown(Boolean shown) {
        this.shown = shown;
    }

    /**
	 * Gets if this card is locked
	 * @return the card is locked
	 */
    public Boolean getLocked() {
        return this.locked;
    }

    /**
	 * Sets this card as locked
	 * @param locked the flag to set
	 */
    public void setLocked(Boolean locked) {
        this.locked = locked;
    }

    /**
	 * Gets the card properties
	 * @return the card properties
	 */
    @SuppressWarnings("unchecked")
    public Map getProperties() {
        return this.props;
    }

    /**
	 * Sets the card properties
	 * @param props the properties to set
	 */
    @SuppressWarnings("unchecked")
    public void setProperties(Map props) {
        this.props = props;
    }

    /**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
    public boolean equals(Object obj) {
        if (obj instanceof Card && ((Card) obj).cardID != null) return ((Card) obj).cardID.equals(cardID); else return false;
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
