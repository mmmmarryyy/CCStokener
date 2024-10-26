package org.blackdog.type;

import java.beans.PropertyVetoException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;
import java.util.Enumeration;
import org.blackdog.type.base.AudioDuration;
import org.blackdog.type.base.Rate;
import org.siberia.type.SibURL;
import org.siberia.type.annotation.bean.Bean;
import org.siberia.type.annotation.bean.BeanProperty;

/**
 *
 * Abstract implementation of an AudioItem
 *
 * @author alexis
 */
@Bean(name = "audio item", internationalizationRef = "org.blackdog.rc.i18n.type.AudioItem", expert = false, hidden = false, preferred = true, propertiesClassLimit = Object.class, methodsClassLimit = Object.class)
public class AudioItem extends SibURL implements CategorizedItem, Playable, TimeBasedItem, RatedItem {

    /** property bytes length */
    public static final String PROPERTY_BYTES_LENGTH = "org.blackdog.type.AudioItem.bytesLength";

    /** audio category */
    @BeanProperty(name = PROPERTY_CATEGORY, internationalizationRef = "org.blackdog.rc.i18n.property.AudioItem_category", expert = false, hidden = false, preferred = true, bound = true, constrained = true, writeMethodName = "setCategory", writeMethodParametersClass = { AudioCategory.class }, readMethodName = "getCategory", readMethodParametersClass = {  })
    private AudioCategory category = AudioCategory.ROCK;

    /** duration */
    @BeanProperty(name = TimeBasedItem.PROPERTY_DURATION, internationalizationRef = "org.blackdog.rc.i18n.property.AudioItem_duration", expert = false, hidden = false, preferred = true, bound = true, constrained = true, writeMethodName = "setDuration", writeMethodParametersClass = { AudioDuration.class }, readMethodName = "getDuration", readMethodParametersClass = {  })
    private AudioDuration duration = null;

    /** duration verification */
    @BeanProperty(name = TimeBasedItem.PROPERTY_DURATION_VERIFIED, internationalizationRef = "org.blackdog.rc.i18n.property.AudioItem_durationVerified", expert = true, hidden = true, preferred = true, bound = true, constrained = true, writeMethodName = "setDurationVerified", writeMethodParametersClass = { boolean.class }, readMethodName = "isDurationVerified", readMethodParametersClass = {  })
    private boolean durationVerified = false;

    /** rate */
    @BeanProperty(name = PROPERTY_RATE, internationalizationRef = "org.blackdog.rc.i18n.property.AudioItem_rate", expert = false, hidden = false, preferred = true, bound = true, constrained = true, writeMethodName = "setRate", writeMethodParametersClass = { Rate.class }, readMethodName = "getRate", readMethodParametersClass = {  })
    private Rate rate = null;

    /** count played */
    @BeanProperty(name = Playable.PROPERTY_PLAYED_COUNT, internationalizationRef = "org.blackdog.rc.i18n.property.AudioItem_playedCount", expert = false, hidden = false, preferred = true, bound = true, constrained = true, writeMethodName = "setCountPlayed", writeMethodParametersClass = { int.class }, readMethodName = "getCountPlayed", readMethodParametersClass = {  })
    private int countPlayed = 0;

    /** last time played */
    @BeanProperty(name = Playable.PROPERTY_DATE_LAST_PLAYED, internationalizationRef = "org.blackdog.rc.i18n.property.AudioItem_lastTimePlayed", expert = false, hidden = false, preferred = true, bound = true, constrained = true, writeMethodName = "setLastTimePlayed", writeMethodParametersClass = { Date.class }, readMethodName = "getLastTimePlayed", readMethodParametersClass = {  })
    private Date lastTimePlayed = null;

    /** date of creation */
    @BeanProperty(name = TimeBasedItem.PROPERTY_CREATION_DATE, internationalizationRef = "org.blackdog.rc.i18n.property.AudioItem_creationDate", expert = false, hidden = false, preferred = true, bound = true, constrained = true, writeMethodName = "setCreationDate", writeMethodParametersClass = { Date.class }, readMethodName = "getCreationDate", readMethodParametersClass = {  })
    private Date creationDate = null;

    /** audio bytes length */
    @BeanProperty(name = PROPERTY_BYTES_LENGTH, internationalizationRef = "org.blackdog.rc.i18n.property.AudioItem_bytesLength", expert = true, hidden = false, preferred = true, bound = false, constrained = false, writeMethodName = "setAudioBytesLength", writeMethodParametersClass = { long.class }, readMethodName = "getAudioBytesLength", readMethodParametersClass = {  })
    private long audioBytesLength = -1;

    /** Creates a new instance of AudioItem */
    public AudioItem() {
    }

    /** set the value of the audio item	
     *	@param url an url expressed as a string
     */
    private void setFilePath(String url) throws PropertyVetoException, MalformedURLException {
        this.setValue(new URL(url));
    }

    /** get the value of the audio item	
     *	@return an url expressed as a string
     */
    private String getFilePath() {
        URL url = this.getValue();
        if (url == null) {
            return null;
        } else {
            return url.toString();
        }
    }

    /** return the category of the item
     *  @return an Item of AudioCategory enumeration
     */
    public AudioCategory getCategory() {
        return this.category;
    }

    /** initialize the category of the item
     *  @param category an Item of AudioCategory enumeration
     *
     *  @throws PropertyVetoException
     */
    public void setCategory(AudioCategory category) throws PropertyVetoException {
        AudioCategory cat = category;
        if (cat == null) {
            cat = AudioCategory.ROCK;
        }
        boolean equals = true;
        if (cat == null) {
            if (this.getCategory() != null) {
                equals = false;
            }
        } else {
            equals = cat.equals(this.getCategory());
        }
        if (!equals) {
            this.fireVetoableChange(PROPERTY_CATEGORY, this.getCategory(), cat);
            this.checkReadOnlyProperty(PROPERTY_CATEGORY, this.getCategory(), cat);
            AudioCategory oldCategory = this.getCategory();
            this.category = cat;
            this.firePropertyChange(PROPERTY_CATEGORY, oldCategory, this.getCategory());
        }
    }

    /** return the category of the item
     *  @return the code of an AudioCategory or null if undefined
     */
    protected String getCategoryValue() {
        String result = null;
        AudioCategory cat = this.getCategory();
        if (cat != null) {
            result = cat.name();
        }
        return result;
    }

    /** initialize the category of the item
     *  @param categoryCode the code of an AudioCategory
     *
     *  @throws PropertyVetoException
     */
    protected void setCategoryValue(String categoryCode) throws PropertyVetoException {
        AudioCategory cat = null;
        try {
            cat = AudioCategory.valueOf(categoryCode);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        }
        this.setCategory(cat);
    }

    /** return the rate of this item
     *	@return a Rate or null if the rate is undefined
     */
    public Rate getRate() {
        return this.rate;
    }

    /** initialize the rate of this item
     *	@param rate a Rate or null to indicate that the rate of this item is undefined
     *
     *	@exception PropertyVetoException if the value change is not accepted
     */
    public void setRate(Rate rate) throws PropertyVetoException {
        boolean equals = true;
        if (rate == null) {
            if (this.getRate() != null) {
                equals = false;
            }
        } else {
            equals = rate.equals(this.getRate());
        }
        if (!equals) {
            this.fireVetoableChange(PROPERTY_RATE, this.getRate(), rate);
            this.checkReadOnlyProperty(PROPERTY_RATE, this.getRate(), rate);
            Rate oldRate = this.getRate();
            this.rate = rate;
            this.firePropertyChange(PROPERTY_RATE, oldRate, this.getRate());
        }
    }

    /** initialize the rate of this item
     *	@param value an Integer
     *
     *	@exception PropertyVetoException if the value change is not accepted
     */
    protected void setRateValue(Integer value) throws PropertyVetoException {
        Rate rate = null;
        if (value != null) {
            rate = new Rate();
            try {
                rate.setRateValue(value.intValue());
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
        this.setRate(rate);
    }

    /** return the value of the rate of this item
     *	@return an Integer or null if the rate is not initailized
     */
    protected Integer getRateValue() {
        Integer result = null;
        Rate rate = this.getRate();
        if (rate != null) {
            result = rate.getRateValue();
        }
        return result;
    }

    /** set the duration for this item
     *	@param duration an AudioDuration
     */
    public void setDuration(AudioDuration duration) throws PropertyVetoException {
        boolean equals = true;
        if (duration == null) {
            if (this.getDuration() != null) {
                equals = false;
            }
        } else {
            equals = duration.equals(this.getDuration());
        }
        if (!equals) {
            this.fireVetoableChange(PROPERTY_DURATION, this.getDuration(), duration);
            this.checkReadOnlyProperty(PROPERTY_DURATION, this.getDuration(), duration);
            AudioDuration oldDate = this.getDuration();
            this.duration = duration;
            this.firePropertyChange(PROPERTY_DURATION, oldDate, this.getDuration());
        }
    }

    /** return the duration for this item
     *	@return an AudioDuration or null if the duration is not specified
     */
    public AudioDuration getDuration() {
        return this.duration;
    }

    /** indicate if the duration is verified
     *	@param verified true if the duration is verified
     */
    public void setDurationVerified(boolean verified) throws PropertyVetoException {
        if (verified != this.isDurationVerified()) {
            this.fireVetoableChange(PROPERTY_DURATION_VERIFIED, this.isDurationVerified(), verified);
            this.checkReadOnlyProperty(PROPERTY_DURATION_VERIFIED, this.isDurationVerified(), verified);
            this.durationVerified = verified;
            this.firePropertyChange(PROPERTY_DURATION_VERIFIED, !verified, verified);
        }
    }

    /** return the duration for this item
     *	@return an AudioDuration or null if the duration is not specified
     */
    protected long getDurationValue() {
        if (this.getDuration() == null) {
            return -1;
        } else {
            return this.getDuration().getTimeInMilli();
        }
    }

    /** indicate if the duration is verified
     *	@param verified true if the duration is verified
     */
    protected void setDurationValue(long millis) throws PropertyVetoException {
        this.setDuration(new AudioDuration(millis));
    }

    /** indicate if the duration is verified
     *	@return true if the duration is verified
     */
    public boolean isDurationVerified() {
        return this.durationVerified;
    }

    /** return the number of play for this item
     *	@return an integer that represents the number of play for this item
     */
    public int getCountPlayed() {
        return this.countPlayed;
    }

    /** initialize the number of play for this item
     *	@param playCount an integer that represents the number of play for this item
     */
    public void setCountPlayed(int playCount) throws PropertyVetoException {
        if (playCount != this.getCountPlayed()) {
            this.fireVetoableChange(PROPERTY_PLAYED_COUNT, this.getCountPlayed(), playCount);
            this.checkReadOnlyProperty(PROPERTY_PLAYED_COUNT, this.getCountPlayed(), playCount);
            int oldPlayCount = this.getCountPlayed();
            this.countPlayed = playCount;
            this.firePropertyChange(PROPERTY_PLAYED_COUNT, oldPlayCount, this.getCountPlayed());
        }
    }

    /** return a Date that represents the last time the item was wanted to be played
     *	@return a Date
     */
    public Date getLastTimePlayed() {
        return this.lastTimePlayed;
    }

    /** initialize the Date that represents the last time the item was wanted to be played
     *	@param date a Date
     */
    public void setLastTimePlayed(Date date) throws PropertyVetoException {
        boolean equals = false;
        if (date == null) {
            if (this.getLastTimePlayed() == null) {
                equals = true;
            }
        } else {
            equals = date.equals(this.getLastTimePlayed());
        }
        if (!equals) {
            this.fireVetoableChange(PROPERTY_DATE_LAST_PLAYED, this.getLastTimePlayed(), date);
            this.checkReadOnlyProperty(PROPERTY_DATE_LAST_PLAYED, this.getLastTimePlayed(), date);
            Date oldLastTimePlayed = this.getLastTimePlayed();
            this.lastTimePlayed = date;
            this.firePropertyChange(PROPERTY_DATE_LAST_PLAYED, oldLastTimePlayed, this.getLastTimePlayed());
        }
    }

    /** return a Date that represents the date of creation of this item
     *	@return a Date
     */
    public Date getCreationDate() {
        return this.creationDate;
    }

    /** initialize the Date that represents the date when the item was created
     *	@param date a Date
     */
    public void setCreationDate(Date date) throws PropertyVetoException {
        boolean equals = false;
        if (date == null) {
            if (this.getCreationDate() == null) {
                equals = true;
            }
        } else {
            equals = date.equals(this.getCreationDate());
        }
        if (!equals) {
            this.fireVetoableChange(PROPERTY_CREATION_DATE, this.getCreationDate(), date);
            this.checkReadOnlyProperty(PROPERTY_CREATION_DATE, this.getCreationDate(), date);
            Date oldCreationDate = this.getCreationDate();
            this.creationDate = date;
            this.firePropertyChange(PROPERTY_CREATION_DATE, oldCreationDate, this.getCreationDate());
        }
    }

    public long getAudioBytesLength() {
        return audioBytesLength;
    }

    public void setAudioBytesLength(long audioBytesLength) {
        this.audioBytesLength = audioBytesLength;
    }

    public boolean equals(Object t) {
        boolean retValue = false;
        if (t != null && t.getClass().equals(this.getClass())) {
            URL a = this.getURL();
            URL b = ((AudioItem) t).getURL();
            if (a == null) {
                if (b == null) {
                    retValue = true;
                }
            } else {
                if (b != null) {
                    try {
                        URI uriA = a.toURI();
                        if (uriA != null) {
                            retValue = uriA.equals(b.toURI());
                        }
                    } catch (URISyntaxException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
        return retValue;
    }

    /** return an InputStream
     *  @return an InputStream
     *
     *  @exception IOException if the creation failed
     */
    public InputStream createInputStream() throws IOException {
        InputStream stream = null;
        URL url = this.getValue();
        if (url != null) {
            stream = url.openStream();
        }
        return stream;
    }

    /** return the number of bytes that represents the playable
     *	@return a Long or -1 if this cannot be found
     */
    public long getBytesLength() {
        long result = -1;
        URL url = this.getValue();
        if (url != null) {
            String protocol = url.getProtocol();
            if ("file".equals(protocol)) {
                try {
                    URI uri = url.toURI();
                    if (uri != null) {
                        File f = new File(uri);
                        if (f.exists()) {
                            result = f.length();
                        }
                    }
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    /** return the simple name of the item
     *  @return a String that does not contains '.' (example : 'mp3', 'ogg', etc..)
     */
    private String getSimpleName() {
        String result = null;
        URL url = this.getValue();
        if (url != null) {
            String file = url.getFile();
            if (file != null) {
                int lastSlashIndex = file.lastIndexOf(File.separator);
                if (lastSlashIndex != -1) {
                    result = file.substring(lastSlashIndex + 1);
                }
            }
        }
        return result;
    }

    /** return the extension of the item
     *  @return a String that does not contains '.' (example : 'mp3', 'ogg', etc..)
     */
    public String getExtension() {
        String result = null;
        String simpleName = this.getSimpleName();
        if (simpleName != null) {
            int lastPointIndex = simpleName.lastIndexOf('.');
            if (lastPointIndex != -1) {
                result = simpleName.substring(lastPointIndex + 1);
            }
        }
        return result;
    }

    /** return the name of the playable item
     *  @return the name
     */
    public String getPlayableName() {
        String result = null;
        String simpleName = this.getSimpleName();
        if (simpleName != null) {
            int lastPointIndex = simpleName.lastIndexOf('.');
            if (lastPointIndex != -1) {
                result = simpleName.substring(0, lastPointIndex);
            }
        }
        return result;
    }
}
