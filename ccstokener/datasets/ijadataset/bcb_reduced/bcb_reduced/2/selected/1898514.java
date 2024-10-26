package net.sourceforge.thinfeeder.model.dao;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import net.sourceforge.thinfeeder.model.Database;
import net.sourceforge.thinfeeder.util.Utils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import de.nava.informa.core.ChannelFormat;
import de.nava.informa.core.ChannelIF;
import de.nava.informa.core.ItemIF;
import de.nava.informa.core.ParseException;
import de.nava.informa.impl.basic.Channel;
import de.nava.informa.impl.basic.ChannelBuilder;
import de.nava.informa.parsers.FeedParser;
import de.nava.informa.utils.ItemComparator;

/**
 * @author fabianofranz@users.sourceforge.net
 * 
 * TODO To change the template for this generated type comment go to Window -
 * Preferences - Java - Code Style - Code Templates
 */
public class DAOChannel {

    private static Log log = LogFactory.getLog(DAOChannel.class);

    public static ChannelIF addChannel(URL url) throws Exception {
        return DAOChannel.addChannel(url.toExternalForm());
    }

    public static ChannelIF addChannel(String url) throws Exception {
        ChannelIF channel = null;
        try {
            channel = FeedParser.parse(new ChannelBuilder(), url);
            if (channel.getLocation() == null) channel.setLocation(new URL(url));
        } catch (ParseException e) {
            InputStream is = new URL(url).openStream();
            InputStreamReader reader = new InputStreamReader(is);
            channel = FeedParser.parse(new ChannelBuilder(), reader);
            reader.close();
            is.close();
            if (channel.getLocation() == null) channel.setLocation(new URL(url));
        }
        DAOChannel.addChannel(channel);
        DAOFavicon.addFaviconForChannel(channel);
        return channel;
    }

    public static void addChannel(ChannelIF channel) throws Exception {
        StringBuffer sql = new StringBuffer();
        sql.append("INSERT INTO CHANNELS ");
        sql.append("(CHANNEL_ID, TITLE, DESCRIPTION, LOCSTRING, SITE, CREATOR, PUBLISHER, LANGUAGE, FORMAT, IMAGE_ID, TEXTINPUT_ID, COPYRIGHT, RATING, CLOUD_ID, GENERATOR, DOCS, TTL, LAST_UPDATED, LAST_BUILD_DATE, PUB_DATE, UPDATE_PERIOD, UPDATE_FREQUENCY, UPDATE_BASE) ");
        sql.append("VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");
        Connection con = Database.getInstance().getConnection();
        PreparedStatement stmt = con.prepareStatement(sql.toString());
        stmt.setLong(1, channel.getId());
        stmt.setString(2, Utils.stripToSafeDatabaseString(channel.getTitle()));
        stmt.setString(3, Utils.stripToSafeDatabaseString(channel.getDescription()));
        stmt.setString(4, channel.getLocation() == null ? null : channel.getLocation().toExternalForm());
        stmt.setBytes(5, channel.getSite() == null ? null : Utils.serialize(channel.getSite()));
        stmt.setString(6, Utils.stripToSafeDatabaseString(channel.getCreator()));
        stmt.setString(7, Utils.stripToSafeDatabaseString(channel.getPublisher()));
        stmt.setString(8, Utils.stripToSafeDatabaseString(channel.getLanguage()));
        stmt.setString(9, channel.getFormat() == null ? null : Utils.stripToSafeDatabaseString(channel.getFormat().toString()));
        if (channel.getImage() != null) {
            DAOImage.addImage(channel.getImage());
            stmt.setLong(10, channel.getImage().getId());
        } else stmt.setObject(10, null);
        if (channel.getTextInput() != null) {
            DAOTextInput.addTextInput(channel.getTextInput());
            stmt.setLong(11, channel.getTextInput().getId());
        } else stmt.setObject(11, null);
        stmt.setString(12, Utils.stripToSafeDatabaseString(channel.getCopyright()));
        stmt.setString(13, Utils.stripToSafeDatabaseString(channel.getRating()));
        stmt.setString(14, null);
        stmt.setString(15, Utils.stripToSafeDatabaseString(channel.getGenerator()));
        stmt.setString(16, Utils.stripToSafeDatabaseString(channel.getDocs()));
        stmt.setInt(17, channel.getTtl());
        stmt.setDate(18, channel.getLastUpdated() == null ? null : new Date(channel.getLastUpdated().getTime()));
        stmt.setDate(19, channel.getLastBuildDate() == null ? null : new Date(channel.getLastBuildDate().getTime()));
        stmt.setDate(20, channel.getPubDate() == null ? null : new Date(channel.getPubDate().getTime()));
        stmt.setString(21, Utils.stripToSafeDatabaseString(channel.getUpdatePeriod()));
        stmt.setInt(22, channel.getUpdateFrequency());
        stmt.setDate(23, channel.getUpdateBase() == null ? null : new Date(channel.getUpdateBase().getTime()));
        stmt.executeUpdate();
        Collection items = channel.getItems();
        int size = 0;
        if (items != null) {
            Iterator i = items.iterator();
            while ((i.hasNext()) && (size < DAOSystem.getSystem().getDefaultMaxItems())) {
                size++;
                ItemIF item = (ItemIF) i.next();
                DAOItem.addItem(item);
            }
        }
        stmt.close();
    }

    public static List getChannels() throws Exception {
        return getChannels(null);
    }

    public static List getChannels(String orderBy) throws Exception {
        StringBuffer sql = new StringBuffer();
        sql.append("SELECT CHANNEL_ID, TITLE, DESCRIPTION, LOCSTRING, SITE, CREATOR, PUBLISHER, LANGUAGE, FORMAT, IMAGE_ID, TEXTINPUT_ID, COPYRIGHT, RATING, CLOUD_ID, GENERATOR, DOCS, TTL, LAST_UPDATED, LAST_BUILD_DATE, PUB_DATE, UPDATE_PERIOD, UPDATE_FREQUENCY, UPDATE_BASE ");
        sql.append("FROM CHANNELS");
        if (orderBy != null) {
            sql.append(" ORDER BY ");
            sql.append(orderBy);
        }
        List channels = new ArrayList();
        Connection con = Database.getInstance().getConnection();
        ResultSet rs = con.createStatement().executeQuery(sql.toString());
        while (rs.next()) {
            ChannelIF channel = new Channel();
            channel.setId(rs.getLong("CHANNEL_ID"));
            channel.setTitle(rs.getString("TITLE"));
            channel.setDescription(rs.getString("DESCRIPTION"));
            try {
                channel.setLocation(rs.getObject("LOCSTRING") == null ? null : new URL(rs.getString("LOCSTRING")));
            } catch (MalformedURLException e) {
            }
            channel.setSite(rs.getObject("SITE") == null ? null : (URL) Utils.deserialize(rs.getBytes("SITE")));
            channel.setCreator(rs.getString("CREATOR"));
            channel.setPublisher(rs.getString("PUBLISHER"));
            channel.setLanguage(rs.getString("LANGUAGE"));
            channel.setFormat(DAOChannel.getFormat(rs.getString("FORMAT")));
            channel.setImage(rs.getObject("IMAGE_ID") == null ? null : DAOImage.getImage(rs.getLong("IMAGE_ID")));
            channel.setTextInput(rs.getObject("TEXTINPUT_ID") == null ? null : DAOTextInput.getTextInput(rs.getLong("TEXTINPUT_ID")));
            channel.setCopyright(rs.getString("COPYRIGHT"));
            channel.setRating(rs.getString("RATING"));
            channel.setCloud(rs.getObject("CLOUD_ID") == null ? null : DAOCloud.getCloud(rs.getLong("CLOUD_ID")));
            channel.setGenerator(rs.getString("GENERATOR"));
            channel.setDocs(rs.getString("DOCS"));
            channel.setTtl(rs.getInt("TTL"));
            channel.setLastUpdated(rs.getDate("LAST_UPDATED"));
            channel.setLastBuildDate(rs.getDate("LAST_BUILD_DATE"));
            channel.setPubDate(rs.getDate("PUB_DATE"));
            channel.setUpdatePeriod(rs.getString("UPDATE_PERIOD"));
            channel.setUpdateFrequency(rs.getInt("UPDATE_FREQUENCY"));
            channel.setUpdateBase(rs.getDate("UPDATE_BASE"));
            List items = DAOItem.getItemsByTimestampDesc(channel);
            for (int i = 0; i < items.size(); i++) {
                channel.addItem((ItemIF) items.get(i));
            }
            channels.add(channel);
        }
        rs.close();
        return channels;
    }

    public static List getChannelsOrderByTitle() throws Exception {
        return getChannels("TITLE");
    }

    public static ChannelIF getChannel(long id) throws Exception {
        StringBuffer sql = new StringBuffer();
        sql.append("SELECT CHANNEL_ID, TITLE, DESCRIPTION, LOCSTRING, SITE, CREATOR, PUBLISHER, LANGUAGE, FORMAT, IMAGE_ID, TEXTINPUT_ID, COPYRIGHT, RATING, CLOUD_ID, GENERATOR, DOCS, TTL, LAST_UPDATED, LAST_BUILD_DATE, PUB_DATE, UPDATE_PERIOD, UPDATE_FREQUENCY, UPDATE_BASE ");
        sql.append("FROM CHANNELS ");
        sql.append("WHERE CHANNEL_ID = ?");
        Connection con = Database.getInstance().getConnection();
        PreparedStatement stmt = con.prepareStatement(sql.toString());
        stmt.setLong(1, id);
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            ChannelIF channel = new Channel();
            channel.setId(rs.getLong("CHANNEL_ID"));
            channel.setTitle(rs.getString("TITLE"));
            channel.setDescription(rs.getString("DESCRIPTION"));
            try {
                channel.setLocation(rs.getObject("LOCSTRING") == null ? null : new URL(rs.getString("LOCSTRING")));
            } catch (MalformedURLException e) {
            }
            channel.setSite(rs.getObject("SITE") == null ? null : (URL) Utils.deserialize(rs.getBytes("SITE")));
            channel.setCreator(rs.getString("CREATOR"));
            channel.setPublisher(rs.getString("PUBLISHER"));
            channel.setLanguage(rs.getString("LANGUAGE"));
            channel.setFormat(DAOChannel.getFormat(rs.getString("FORMAT")));
            channel.setImage(rs.getObject("IMAGE_ID") == null ? null : DAOImage.getImage(rs.getLong("IMAGE_ID")));
            channel.setTextInput(rs.getObject("TEXTINPUT_ID") == null ? null : DAOTextInput.getTextInput(rs.getLong("TEXTINPUT_ID")));
            channel.setCopyright(rs.getString("COPYRIGHT"));
            channel.setRating(rs.getString("RATING"));
            channel.setCloud(rs.getObject("CLOUD_ID") == null ? null : DAOCloud.getCloud(rs.getLong("CLOUD_ID")));
            channel.setGenerator(rs.getString("GENERATOR"));
            channel.setDocs(rs.getString("DOCS"));
            channel.setTtl(rs.getInt("TTL"));
            channel.setLastUpdated(rs.getDate("LAST_UPDATED"));
            channel.setLastBuildDate(rs.getDate("LAST_BUILD_DATE"));
            channel.setPubDate(rs.getDate("PUB_DATE"));
            channel.setUpdatePeriod(rs.getString("UPDATE_PERIOD"));
            channel.setUpdateFrequency(rs.getInt("UPDATE_FREQUENCY"));
            channel.setUpdateBase(rs.getDate("UPDATE_BASE"));
            List items = DAOItem.getItemsByTimestampDesc(channel);
            for (int i = 0; i < items.size(); i++) {
                channel.addItem((ItemIF) items.get(i));
            }
            rs.close();
            stmt.close();
            return channel;
        } else {
            rs.close();
            stmt.close();
            return null;
        }
    }

    public static void updateChannel(ChannelIF channel) throws Exception {
        StringBuffer sql = new StringBuffer();
        sql.append("UPDATE CHANNELS SET TITLE = ?, DESCRIPTION = ?, LOCSTRING = ?, SITE = ?, CREATOR = ?, PUBLISHER = ?, LANGUAGE = ?, FORMAT = ?, IMAGE_ID = ?, TEXTINPUT_ID = ?, COPYRIGHT = ?, RATING = ?, CLOUD_ID = ?, GENERATOR = ?, DOCS = ?, TTL = ?, LAST_UPDATED = ?, LAST_BUILD_DATE = ?, PUB_DATE = ?, UPDATE_PERIOD = ?, UPDATE_FREQUENCY = ?, UPDATE_BASE = ? ");
        sql.append("WHERE CHANNEL_ID = ?");
        Connection con = Database.getInstance().getConnection();
        PreparedStatement stmt = con.prepareStatement(sql.toString());
        stmt.setString(1, Utils.stripToSafeDatabaseString(channel.getTitle()));
        stmt.setString(2, Utils.stripToSafeDatabaseString(channel.getDescription()));
        stmt.setString(3, channel.getLocation() == null ? null : channel.getLocation().toExternalForm());
        stmt.setBytes(4, channel.getSite() == null ? null : Utils.serialize(channel.getSite()));
        stmt.setString(5, Utils.stripToSafeDatabaseString(channel.getCreator()));
        stmt.setString(6, Utils.stripToSafeDatabaseString(channel.getPublisher()));
        stmt.setString(7, Utils.stripToSafeDatabaseString(channel.getLanguage()));
        stmt.setString(8, channel.getFormat() == null ? null : Utils.stripToSafeDatabaseString(channel.getFormat().toString()));
        if (channel.getImage() != null) {
            stmt.setLong(9, channel.getImage().getId());
        } else stmt.setObject(9, null);
        if (channel.getTextInput() != null) {
            stmt.setLong(10, channel.getTextInput().getId());
        } else stmt.setObject(10, null);
        stmt.setString(11, Utils.stripToSafeDatabaseString(channel.getCopyright()));
        stmt.setString(12, Utils.stripToSafeDatabaseString(channel.getRating()));
        stmt.setString(13, channel.getCloud() == null ? null : Utils.stripToSafeDatabaseString(channel.getCloud().toString()));
        stmt.setString(14, Utils.stripToSafeDatabaseString(channel.getGenerator()));
        stmt.setString(15, Utils.stripToSafeDatabaseString(channel.getDocs()));
        stmt.setInt(16, channel.getTtl());
        stmt.setDate(17, channel.getLastUpdated() == null ? null : new Date(channel.getLastUpdated().getTime()));
        stmt.setDate(18, channel.getLastBuildDate() == null ? null : new Date(channel.getLastBuildDate().getTime()));
        stmt.setDate(19, channel.getPubDate() == null ? null : new Date(channel.getPubDate().getTime()));
        stmt.setString(20, Utils.stripToSafeDatabaseString(channel.getUpdatePeriod()));
        stmt.setInt(21, channel.getUpdateFrequency());
        stmt.setDate(22, channel.getUpdateBase() == null ? null : new Date(channel.getUpdateBase().getTime()));
        stmt.setLong(23, channel.getId());
        stmt.executeUpdate();
        stmt.close();
    }

    public static int updateChannelItems(ChannelIF channel, int limit) throws Throwable {
        int itemsRefreshed = 0;
        int itemsDeleted = 0;
        ChannelIF newChannel = null;
        try {
            newChannel = FeedParser.parse(new ChannelBuilder(), channel.getLocation());
        } catch (ParseException e) {
            InputStream is = channel.getLocation().openStream();
            InputStreamReader reader = new InputStreamReader(is);
            newChannel = FeedParser.parse(new ChannelBuilder(), reader);
            reader.close();
            is.close();
        }
        Collection newItems = newChannel.getItems();
        Iterator i = newItems.iterator();
        while (i.hasNext()) {
            ItemIF item = (ItemIF) i.next();
            if (!contains(channel, item)) {
                item.setChannel(channel);
                DAOItem.addItem(item);
                itemsRefreshed++;
                if (log.isInfoEnabled()) log.info("Item added: " + item.toString());
            }
        }
        try {
            if (itemsRefreshed > 0) itemsDeleted += DAOChannel.deleteOlderItems(channel, limit);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return itemsRefreshed > limit ? limit : itemsRefreshed;
    }

    public static int deleteOlderItems(ChannelIF channel, int limit) throws Exception {
        StringBuffer sql = new StringBuffer();
        sql.append("DELETE FROM ITEMS WHERE ITEM_ID NOT IN ( ");
        sql.append("SELECT TOP ");
        sql.append(limit);
        sql.append(" ITEM_ID FROM ITEMS WHERE CHANNEL_ID = ? ORDER BY TIMESTAMP DESC");
        sql.append(") AND CHANNEL_ID = ?");
        Connection con = Database.getInstance().getConnection();
        PreparedStatement stmt = con.prepareStatement(sql.toString());
        stmt.setLong(1, channel.getId());
        stmt.setLong(2, channel.getId());
        int deleted = stmt.executeUpdate();
        stmt.close();
        return deleted;
    }

    public static int updateChannelItems(ChannelIF channel) throws Throwable {
        return DAOChannel.updateChannelItems(channel, DAOSystem.getSystem().getDefaultMaxItems());
    }

    public static ChannelFormat getFormat(String strFormat) {
        if (strFormat.equals(ChannelFormat.RSS_0_90.toString())) return ChannelFormat.RSS_0_90; else if (strFormat.equals(ChannelFormat.RSS_0_91.toString())) return ChannelFormat.RSS_0_91; else if (strFormat.equals(ChannelFormat.RSS_0_92.toString())) return ChannelFormat.RSS_0_92; else if (strFormat.equals(ChannelFormat.RSS_0_93.toString())) return ChannelFormat.RSS_0_93; else if (strFormat.equals(ChannelFormat.RSS_0_94.toString())) return ChannelFormat.RSS_0_94; else if (strFormat.equals(ChannelFormat.RSS_1_0.toString())) return ChannelFormat.RSS_1_0; else if (strFormat.equals(ChannelFormat.RSS_2_0.toString())) return ChannelFormat.RSS_2_0;
        return ChannelFormat.UNKNOWN_CHANNEL_FORMAT;
    }

    private static boolean contains(ChannelIF channel, ItemIF item) {
        String title = Utils.stripToSafeDatabaseString(item.getTitle());
        Object[] items = channel.getItems().toArray();
        Arrays.sort(items, new ItemComparator(true));
        for (int i = 0; i < items.length; i++) {
            boolean equalDates = false;
            boolean equalTitles = false;
            ItemIF toCompare = (ItemIF) items[i];
            java.util.Date date = item.getDate();
            java.util.Date foundDate = item.getFound();
            equalTitles = toCompare.getTitle().equals(title);
            if (equalTitles) return true;
        }
        return false;
    }

    public static void removeAllItems(ChannelIF channel) throws Exception {
        StringBuffer sql = new StringBuffer();
        sql.append("DELETE FROM ITEMS ");
        sql.append("WHERE CHANNEL_ID = ?");
        Connection con = Database.getInstance().getConnection();
        PreparedStatement stmt = con.prepareStatement(sql.toString());
        stmt.setLong(1, channel.getId());
        stmt.executeUpdate();
        stmt.close();
    }

    public static void removeFavicon(ChannelIF channel) throws Exception {
        StringBuffer sql = new StringBuffer();
        sql.append("SELECT FILENAME FROM FAVICONS ");
        sql.append("WHERE CHANNEL_ID = ?");
        Connection con = Database.getInstance().getConnection();
        PreparedStatement stmt = con.prepareStatement(sql.toString());
        stmt.setLong(1, channel.getId());
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            String favicon = rs.getString("FILENAME");
            try {
                File file = new File(favicon);
                file.delete();
            } catch (Throwable t) {
            }
        }
        rs.close();
        stmt.close();
        sql = new StringBuffer();
        sql.append("DELETE FROM FAVICONS ");
        sql.append("WHERE CHANNEL_ID = ?");
        con = Database.getInstance().getConnection();
        stmt = con.prepareStatement(sql.toString());
        stmt.setLong(1, channel.getId());
        stmt.executeUpdate();
        stmt.close();
    }

    public static void removeChannel(ChannelIF channel) throws Exception {
        DAOChannel.removeAllItems(channel);
        DAOChannel.removeFavicon(channel);
        StringBuffer sql = new StringBuffer();
        sql.append("DELETE FROM CHANNELS ");
        sql.append("WHERE CHANNEL_ID = ?");
        Connection con = Database.getInstance().getConnection();
        PreparedStatement stmt = con.prepareStatement(sql.toString());
        stmt.setLong(1, channel.getId());
        stmt.executeUpdate();
        stmt.close();
        if (channel.getImage() != null) DAOImage.removeImage(channel.getImage());
        if (channel.getTextInput() != null) DAOTextInput.removeTextInput(channel.getTextInput());
    }

    public static boolean hasUnreadItems(ChannelIF channel) throws Exception {
        String sql = "SELECT ITEM_ID FROM ITEMS WHERE CHANNEL_ID = ? AND UNREAD = ?";
        Connection con = Database.getInstance().getConnection();
        PreparedStatement stmt = con.prepareStatement(sql);
        stmt.setLong(1, channel.getId());
        stmt.setInt(2, 1);
        ResultSet rs = stmt.executeQuery();
        boolean r = rs.next();
        rs.close();
        stmt.close();
        return r;
    }

    public static boolean existsForSite(String url) throws Exception {
        if (url == null) return false;
        String sql = "SELECT CHANNEL_ID FROM CHANNELS WHERE LOCSTRING = ?";
        Connection con = Database.getInstance().getConnection();
        PreparedStatement stmt = con.prepareStatement(sql);
        stmt.setString(1, url);
        ResultSet rs = stmt.executeQuery();
        boolean r = rs.next();
        rs.close();
        stmt.close();
        return r;
    }
}
