package tony;

import java.awt.Dimension;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import resources.ResourceAnchor;
import tony.db.DBUtil;
import com.example.possessed.BuyHelpPanel;
import com.example.possessed.Gift;
import com.example.possessed.PossessedApplication;
import com.example.possessed.SerializableRunnable;
import com.example.possessed.UnableToCompleteDeal;
import com.vaadin.Application;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.PopupView;
import com.vaadin.ui.VerticalLayout;

public class PossessedUtils {

    public static final double[] MORTGAGERATES = new double[] { 0d, 0.0001d, 0.0001d, 0.0002d, 0.0002d, 0.0005d, 0.0025d, 0.0100d, 0.0500d, 0.1000d };

    public static final long mortgageCap = 2000000000;

    public static final double PRICEINCREASE = 1.2d;

    private static long sales = 0;

    public static long mortgage(long picuid, String user, int mortgageOption) throws UnableToCompleteDeal {
        Connection conn = DBUtil.getConnection();
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            conn.setAutoCommit(false);
            prep = conn.prepareStatement("select * from player where 1=0 for update");
            prep.executeQuery();
            DBUtil.close(prep);
            prep = conn.prepareStatement("select ownerid,mortgaged,auctionflag,price,tokens,proexpirydate from picture" + " join player on player.userid=picture.ownerid" + " where picture.uid=? for update");
            prep.setLong(1, picuid);
            rs = prep.executeQuery();
            if (!rs.next()) throw new UnableToCompleteDeal("Picture not found.");
            if (!rs.getString(1).equals(user)) {
                throw new UnableToCompleteDeal("Picture has already been sold.");
            }
            if (rs.getBoolean(2)) {
                throw new UnableToCompleteDeal("Picture already Mortgaged.");
            }
            if (rs.getBoolean(3)) {
                throw new UnableToCompleteDeal("Picture in Auction.");
            }
            long price = rs.getLong(4);
            Date proExpiryDate = rs.getDate(6);
            DBUtil.close(rs);
            DBUtil.close(prep);
            long mortgageAmount = (long) (mortgageOption * price / (PRICEINCREASE * 10));
            long cap = PossessedUtils.mortgageCap;
            if (proExpiryDate != null && proExpiryDate.getTime() > System.currentTimeMillis()) {
                cap = cap * 3 / 2;
            }
            if (mortgageAmount > cap) mortgageAmount = cap;
            prep = conn.prepareStatement("update player set cash=cash+?, debt=debt+? where userid=?");
            prep.setLong(1, mortgageAmount);
            prep.setLong(2, mortgageAmount);
            prep.setString(3, user);
            prep.executeUpdate();
            DBUtil.close(prep);
            prep = conn.prepareStatement("update picture set mortgaged=true, discount=0, mortgageAmount=?, mortgagerate=?, lastPaymentDate=CURRENT_TIMESTAMP() where uid=?");
            prep.setLong(1, mortgageAmount);
            prep.setInt(2, mortgageOption);
            prep.setLong(3, picuid);
            prep.executeUpdate();
            conn.commit();
            return mortgageAmount;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtil.rollBack(conn);
            DBUtil.close(rs);
            DBUtil.close(prep);
            DBUtil.close(conn);
        }
        return 0;
    }

    public static long repayMortgage(long picuid, String user) throws UnableToCompleteDeal {
        Connection conn = DBUtil.getConnection();
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            conn.setAutoCommit(false);
            prep = conn.prepareStatement("select * from player where 1=0 for update");
            prep.executeQuery();
            DBUtil.close(prep);
            prep = conn.prepareStatement("select picture.ownerid,picture.mortgaged,picture.auctionflag,picture.price,picture.MortgageAmount, player.cash from picture  " + "join player on picture.ownerid=player.userid " + "where uid=? for update");
            prep.setLong(1, picuid);
            rs = prep.executeQuery();
            if (!rs.next()) throw new UnableToCompleteDeal("Picture not found.");
            if (!rs.getString(1).equals(user)) {
                throw new UnableToCompleteDeal("Picture has already been sold.");
            }
            if (!rs.getBoolean(2)) {
                throw new UnableToCompleteDeal("Picture is not Mortgaged.");
            }
            if (rs.getBoolean(3)) {
                throw new UnableToCompleteDeal("Picture in Auction.");
            }
            long price = rs.getLong(4);
            long mortgageAmount = rs.getLong(5);
            long playerCash = rs.getLong(6);
            if (playerCash < mortgageAmount) {
                throw new UnableToCompleteDeal("You dont have enough to repay your mortgage.");
            }
            DBUtil.close(rs);
            DBUtil.close(prep);
            prep = conn.prepareStatement("update player set cash=cash-?, debt=debt-? where userid=?");
            prep.setLong(1, mortgageAmount);
            prep.setLong(2, mortgageAmount);
            prep.setString(3, user);
            prep.executeUpdate();
            DBUtil.close(prep);
            prep = conn.prepareStatement("update picture set mortgaged=false,mortgageAmount=0, MORTGAGERATE=0 where uid=?");
            prep.setLong(1, picuid);
            prep.executeUpdate();
            conn.commit();
            return 0;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtil.rollBack(conn);
            DBUtil.close(rs);
            DBUtil.close(prep);
            DBUtil.close(conn);
        }
        return 0;
    }

    public static PopupView createBuyButton(final Long price, final Application app, final Long currentPictureUid, final SerializableRunnable doAfterBuy) {
        PopupView pv = new PopupView(new PopupView.Content() {

            @Override
            public Component getPopupComponent() {
                VerticalLayout vl = new VerticalLayout();
                vl.setSpacing(true);
                vl.addComponent(new Label("Purchase this picture for " + PossessedApplication.numberfmt.format(price) + " ?"));
                HorizontalLayout buttons = new HorizontalLayout();
                Button ok = new Button("Ok");
                Button cancel = new Button("Cancel");
                buttons.addComponent(ok);
                buttons.addComponent(cancel);
                ClickListener lstn = new ClickListener() {

                    @Override
                    public void buttonClick(ClickEvent event) {
                        if (event.getButton().getCaption().equals("Ok")) {
                            try {
                                buy(price, app, currentPictureUid, doAfterBuy);
                            } catch (UnableToCompleteDeal e) {
                            }
                        }
                    }
                };
                ok.addListener(lstn);
                cancel.addListener(lstn);
                vl.addComponent(buttons);
                return null;
            }

            @Override
            public String getMinimizedValueAsHTML() {
                if (price == null) return "Buy"; else return "Buy for " + PossessedApplication.numberfmt.format(price.longValue());
            }
        });
        return pv;
    }

    public static void buy(long buyamountnum, final Application app, final Long currentPictureUid, final Runnable doAfterBuy) throws UnableToCompleteDeal {
        Connection conn = DBUtil.getConnection();
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            conn.setAutoCommit(false);
            BuyPrecheckAmounts amts = buyPrecheck(conn, buyamountnum, app, currentPictureUid);
            if (!amts.isAllowBumping() && buyamountnum > amts.getDiscountPrice()) {
                buyamountnum = amts.getDiscountPrice();
            }
            prep = conn.prepareStatement("update player set cash=cash+?, tokens=tokens+? where userid=?");
            prep.setLong(1, -buyamountnum);
            sales += 1;
            if (sales % 50 == 0) prep.setLong(2, 1); else prep.setLong(2, 0);
            prep.setString(3, (String) app.getUser());
            prep.executeUpdate();
            long profit = buyamountnum - (long) (amts.picturePrice / PRICEINCREASE);
            long wTmp;
            if (profit > 0) {
                prep.setLong(1, (long) (profit * .3));
                prep.setLong(2, 0);
                prep.setString(3, amts.getPictureUploader());
                prep.executeUpdate();
                wTmp = buyamountnum - (long) (profit * .3);
            } else {
                wTmp = buyamountnum;
            }
            wTmp -= amts.mortgagePrice;
            prep.setLong(1, wTmp);
            prep.setLong(2, 0);
            prep.setString(3, amts.getPictureSeller());
            prep.executeUpdate();
            DBUtil.close(prep);
            prep = conn.prepareStatement("update picture set mortgaged=false, mortgageAmount=0, discount=0, price=?, ownerid=?, ownedDate=CURRENT_TIMESTAMP(),insured=? where uid=?");
            prep.setLong(1, (long) (buyamountnum * PRICEINCREASE + 1));
            prep.setString(2, (String) app.getUser());
            prep.setBoolean(3, isProPlayer(conn, amts.getPictureUploader()));
            prep.setLong(4, currentPictureUid);
            prep.executeUpdate();
            DBUtil.close(prep);
            prep = conn.prepareStatement("update pictureHistory set latestsaleflag=false where picid=? and latestsaleflag=true");
            prep.setLong(1, currentPictureUid);
            prep.executeUpdate();
            DBUtil.close(prep);
            prep = conn.prepareStatement("insert into pictureHistory " + "(dealtime,seller,buyer,type,amount,profit,picid,latestsaleflag,uploader) " + "values(?,?,?,'B',?,?,?,true,?)");
            prep.setTimestamp(1, new Timestamp(System.currentTimeMillis()));
            prep.setString(2, amts.getPictureSeller());
            prep.setString(3, (String) app.getUser());
            prep.setLong(4, buyamountnum);
            prep.setLong(5, profit);
            prep.setLong(6, currentPictureUid);
            prep.setString(7, amts.getPictureUploader());
            prep.executeUpdate();
            DBUtil.close(prep);
            prep = conn.prepareStatement("update pictureHistory set recentlyLost=false where " + "seller=? and picid=?");
            prep.setString(1, (String) app.getUser());
            prep.setLong(2, currentPictureUid);
            prep.executeUpdate();
            DBUtil.close(prep);
            prep = conn.prepareStatement("update player set lost_number=" + "(select count(*) from pictureHistory where recentlyLost=true and seller=? and dealtime>player.lost_lastviewed) where player.userid=?");
            prep.setString(1, amts.getPictureSeller());
            prep.setString(2, amts.getPictureSeller());
            prep.executeUpdate();
            DBUtil.close(prep);
            prep = conn.prepareStatement("update player set recentpossessors_number=recentpossessors_number+1 where player.userid=?");
            prep.setString(1, amts.getPictureUploader());
            prep.executeUpdate();
            DBUtil.close(prep);
            prep = conn.prepareStatement("insert into comments(type,key,posted,userid,comment) " + "values (?,?,CURRENT_TIMESTAMP() ,?,?)");
            prep.setString(1, "p");
            prep.setString(2, currentPictureUid + "");
            prep.setString(3, "T");
            prep.setString(4, "&U" + app.getUser() + " Bought this picture for " + PossessedApplication.numberfmt.format(buyamountnum) + " from &U" + amts.getPictureSeller());
            prep.addBatch();
            prep.setString(1, "u");
            prep.setString(2, (String) app.getUser());
            prep.setString(3, "T");
            prep.setString(4, "Bought picture &P" + currentPictureUid + " for " + PossessedApplication.numberfmt.format(buyamountnum) + " from &U" + amts.getPictureSeller());
            prep.addBatch();
            prep.setString(1, "u");
            prep.setString(2, amts.getPictureSeller());
            prep.setString(3, "T");
            prep.setString(4, "Received " + PossessedApplication.numberfmt.format(wTmp) + " from the sale of &P" + currentPictureUid + " to &U" + app.getUser() + " for " + PossessedApplication.numberfmt.format(buyamountnum));
            prep.addBatch();
            if (profit > 0) {
                prep.setString(1, "u");
                prep.setString(2, amts.getPictureUploader());
                prep.setString(3, "T");
                prep.setString(4, "Received " + PossessedApplication.numberfmt.format((long) (profit * .3)) + " commission from the sale of &P" + currentPictureUid + " to &U" + app.getUser() + " for " + PossessedApplication.numberfmt.format(buyamountnum));
                prep.addBatch();
            }
            prep.executeBatch();
            conn.commit();
            doAfterBuy.run();
            ((PossessedApplication) app).setCash(amts.getPlayerCash() - buyamountnum);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtil.rollBack(conn);
            DBUtil.close(rs);
            DBUtil.close(prep);
            DBUtil.close(conn);
        }
    }

    public static void resolvePlayerNames(HashMap<String, String> arg0, Connection conn) {
        Object userids = arg0.keySet().toArray();
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            prep = conn.prepareStatement("SELECT player.userid,player.name FROM player inner join TABLE(userid varchar(20)=?) T on player.userid=t.userid");
            prep.setObject(1, userids);
            rs = prep.executeQuery();
            while (rs.next()) {
                arg0.put(rs.getString(1), rs.getString(2));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtil.close(rs);
            DBUtil.close(prep);
        }
    }

    public static void resolvePictureUrls(HashMap<Long, String> arg0, Connection conn) {
        Object pictureuids = arg0.keySet().toArray();
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            prep = conn.prepareStatement("SELECT picture.uid,picture.thumnailurl FROM picture inner join TABLE(pictureuid long=?) T on picture.uid=t.pictureuid");
            prep.setObject(1, pictureuids);
            rs = prep.executeQuery();
            while (rs.next()) {
                arg0.put(rs.getLong(1), rs.getString(2));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtil.close(rs);
            DBUtil.close(prep);
        }
    }

    private static BidPrecheckAmounts bidPrecheck(Connection conn, long bidAmountNum, Application app, Long currentPictureUid) throws UnableToCompleteDeal {
        PreparedStatement prep = null;
        ResultSet rs = null;
        BidPrecheckAmounts rtn = new BidPrecheckAmounts();
        try {
            prep = conn.prepareStatement("select picture.bidPrice, picture.userid, auctionflag, auctionEndDate, picture.bidplayer,picture.maxBid from picture " + "where uid=? for update");
            prep.setLong(1, currentPictureUid);
            rs = prep.executeQuery();
            if (!rs.next()) throw new RuntimeException("Picture not found");
            rtn.setBidPrice(rs.getLong(1));
            rtn.setUploader(rs.getString(2));
            boolean auctionFlag = rs.getBoolean(3);
            rtn.setAuctionEndDate(rs.getTimestamp(4).getTime());
            rtn.setHighBidder(rs.getString(5));
            rtn.setMaxBid(rs.getLong(6));
            DBUtil.close(rs);
            DBUtil.close(prep);
            if (!auctionFlag || rtn.getAuctionEndDate() < System.currentTimeMillis()) {
                UnableToCompleteDeal exc = new UnableToCompleteDeal("Auction Ended");
                throw exc;
            }
            prep = conn.prepareStatement("select count(*) from blockedplayer where blocker=? and blockee=?");
            prep.setString(1, rtn.getUploader());
            prep.setString(2, (String) app.getUser());
            rs = prep.executeQuery();
            rs.next();
            if (rs.getInt(1) > 0) {
                UnableToCompleteDeal e = new UnableToCompleteDeal("You are blocked from possessing this picture.");
                throw e;
            }
            prep = conn.prepareStatement("select cash from player where userid=? for update");
            prep.setString(1, (String) app.getUser());
            rs = prep.executeQuery();
            if (!rs.next()) throw new RuntimeException("Player not found");
            long cash = rs.getLong(1);
            DBUtil.close(rs);
            DBUtil.close(prep);
            if (bidAmountNum > cash) {
                UnableToCompleteDeal e = new UnableToCompleteDeal("You don't have this much cash.");
                throw e;
            }
            if (rtn.getHighBidder().equals(app.getUser())) {
                if (bidAmountNum < rtn.getBidPrice()) {
                    UnableToCompleteDeal e = new UnableToCompleteDeal("Your max bid must be at least " + PossessedApplication.numberfmt.format(rtn.getBidPrice()));
                    throw e;
                }
            } else {
                if (bidAmountNum <= rtn.getBidPrice()) {
                    UnableToCompleteDeal e = new UnableToCompleteDeal("Must bid at least " + PossessedApplication.numberfmt.format(rtn.getBidPrice() + 1) + " for this picture.");
                    throw e;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtil.close(rs);
            DBUtil.close(prep);
        }
        return rtn;
    }

    public static BuyPrecheckAmounts buyPrecheck(Connection conn, long buyamountnum, Application app, Long currentPictureUid) throws UnableToCompleteDeal {
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            BuyPrecheckAmounts rtn = new BuyPrecheckAmounts();
            prep = conn.prepareStatement("select * from player where 1=0 for update");
            prep.executeQuery();
            DBUtil.close(prep);
            prep = conn.prepareStatement("select picture.price, picture.userid, picture.mortgageAmount, auctionflag, ownerid, picture.discountprice, picture.allowbumping from picture " + "where uid=? for update");
            prep.setLong(1, currentPictureUid);
            rs = prep.executeQuery();
            if (!rs.next()) throw new RuntimeException("Picture not found");
            rtn.setPicturePrice(rs.getLong(1));
            String user = rs.getString(2);
            rtn.setMortgagePrice(rs.getLong(3));
            boolean auctionFlag = rs.getBoolean(4);
            rtn.setPictureSeller(rs.getString(5));
            rtn.setDiscountPrice(rs.getLong(6));
            rtn.setPictureUploader(user);
            rtn.setAllowBumping(rs.getBoolean(7));
            DBUtil.close(rs);
            DBUtil.close(prep);
            if (!rtn.isAllowBumping() && buyamountnum > rtn.getDiscountPrice()) {
                buyamountnum = rtn.getDiscountPrice();
            }
            if (auctionFlag) {
                UnableToCompleteDeal exc = new UnableToCompleteDeal("Picture in Auction");
                exc.setPicturePrice(rtn.getDiscountPrice());
                throw exc;
            }
            if (app.getUser().equals(rtn.getPictureSeller())) {
                UnableToCompleteDeal exc = new UnableToCompleteDeal("You already own this picture.");
                exc.setPicturePrice(rtn.getDiscountPrice());
                throw exc;
            }
            if (app.getUser().equals(rtn.getPictureUploader())) {
                UnableToCompleteDeal exc = new UnableToCompleteDeal("You can't buy your own uploads.");
                exc.setPicturePrice(rtn.getDiscountPrice());
                throw exc;
            }
            prep = conn.prepareStatement("select count(*) from blockedplayer where blocker=? and blockee=?");
            prep.setString(1, user);
            prep.setString(2, (String) app.getUser());
            rs = prep.executeQuery();
            rs.next();
            if (rs.getInt(1) > 0) {
                UnableToCompleteDeal e = new UnableToCompleteDeal("You are blocked from possessing this picture.");
                e.setPicturePrice(rtn.getDiscountPrice());
                throw e;
            }
            DBUtil.close(rs);
            prep.setString(2, user);
            prep.setString(1, (String) app.getUser());
            rs = prep.executeQuery();
            rs.next();
            if (rs.getInt(1) > 0) {
                UnableToCompleteDeal e = new UnableToCompleteDeal("You have blocked this player, so you can't buy their pictures.");
                e.setPicturePrice(rtn.getDiscountPrice());
                throw e;
            }
            DBUtil.close(rs);
            prep = conn.prepareStatement("select cash,confirmBuy from player where userid=? for update");
            prep.setString(1, (String) app.getUser());
            rs = prep.executeQuery();
            if (!rs.next()) throw new RuntimeException("Player not found");
            rtn.setPlayerCash(rs.getLong(1));
            rtn.setConfirmBuyFlag(rs.getBoolean(2));
            DBUtil.close(rs);
            DBUtil.close(prep);
            if (rtn.getDiscountPrice() == 10 && buyamountnum > 10) {
                UnableToCompleteDeal e = new UnableToCompleteDeal("Cant bump on first bid");
                e.setPicturePrice(rtn.getDiscountPrice());
                throw e;
            }
            if (buyamountnum > rtn.getPlayerCash()) {
                UnableToCompleteDeal e = new UnableToCompleteDeal("You don't have this amount of cash.");
                e.setPicturePrice(rtn.getDiscountPrice());
                throw e;
            }
            if (buyamountnum < rtn.getDiscountPrice()) {
                UnableToCompleteDeal e = new UnableToCompleteDeal("Must offer at least " + PossessedApplication.numberfmt.format(rtn.getDiscountPrice()) + " for this picture.");
                e.setPicturePrice(rtn.getDiscountPrice());
                throw e;
            }
            return rtn;
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtil.close(rs);
            DBUtil.close(prep);
        }
        return null;
    }

    public static void bid(long bidAmount, Application app, Long currentPictureUid, SerializableRunnable doAfterBid) throws UnableToCompleteDeal {
        Connection conn = DBUtil.getConnection();
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            conn.setAutoCommit(false);
            BidPrecheckAmounts amts = bidPrecheck(conn, bidAmount, app, currentPictureUid);
            if (amts.getHighBidder().equals(app.getUser())) {
                prep = conn.prepareStatement("update picture set maxbid=? where uid=?");
                prep.setLong(1, bidAmount);
                prep.setLong(2, currentPictureUid);
                prep.executeUpdate();
                DBUtil.close(prep);
                prep = conn.prepareStatement("update player set cash=cash+? where userid=?");
                prep.setLong(1, amts.getMaxBid() - bidAmount);
                prep.setString(2, (String) app.getUser());
                prep.executeUpdate();
                DBUtil.close(prep);
                app.getMainWindow().showNotification("Max Bid Adjusted.");
            } else {
                if (amts.getMaxBid() >= bidAmount) {
                    prep = conn.prepareStatement("update picture set bidPrice=?, numberOfBids=numberOfBids+2 where uid=?");
                    if (amts.getMaxBid() == bidAmount) prep.setLong(1, amts.getMaxBid()); else prep.setLong(1, bidAmount + 1);
                    prep.setLong(2, currentPictureUid);
                    prep.executeUpdate();
                    app.getMainWindow().showNotification("You have been immediately outbid.");
                } else {
                    prep = conn.prepareStatement("update player set cash=cash+? where userid=?");
                    prep.setLong(1, amts.getMaxBid());
                    prep.setString(2, amts.highBidder);
                    prep.executeUpdate();
                    prep.setLong(1, -bidAmount);
                    prep.setString(2, (String) app.getUser());
                    prep.executeUpdate();
                    DBUtil.close(prep);
                    prep = conn.prepareStatement("update picture set bidPrice=?, maxbid=?, bidplayer=? ,numberOfBids=numberOfBids+1 where uid=?");
                    prep.setLong(1, amts.getMaxBid() + 1);
                    prep.setLong(2, bidAmount);
                    prep.setString(3, (String) app.getUser());
                    prep.setLong(4, currentPictureUid);
                    prep.executeUpdate();
                    app.getMainWindow().showNotification("You are now the highest bidder.");
                }
            }
            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtil.rollBack(conn);
            DBUtil.close(prep);
            DBUtil.close(conn);
        }
    }

    public static void setDiscount(long picuid, String user, int discount) throws UnableToCompleteDeal {
        Connection conn = DBUtil.getConnection();
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            conn.setAutoCommit(false);
            prep = conn.prepareStatement("select ownerid,mortgaged,auctionflag,price from picture where uid=? for update");
            prep.setLong(1, picuid);
            rs = prep.executeQuery();
            if (!rs.next()) throw new UnableToCompleteDeal("Picture not found.");
            if (!rs.getString(1).equals(user)) {
                throw new UnableToCompleteDeal("Picture has already been sold.");
            }
            if (rs.getBoolean(2)) {
                throw new UnableToCompleteDeal("Picture already Mortgaged.");
            }
            if (rs.getBoolean(3)) {
                throw new UnableToCompleteDeal("Picture in Auction.");
            }
            DBUtil.close(rs);
            DBUtil.close(prep);
            prep = conn.prepareStatement("update picture set discount=? where uid=?");
            prep.setInt(1, discount);
            prep.setLong(2, picuid);
            prep.executeUpdate();
            conn.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtil.rollBack(conn);
            DBUtil.close(rs);
            DBUtil.close(prep);
            DBUtil.close(conn);
        }
    }

    public static void processSendToAuction(long pictureID, Application app, SerializableRunnable doAfter, int minutes) throws UnableToCompleteDeal {
        Connection conn = DBUtil.getConnection();
        PreparedStatement prep = null;
        try {
            conn.setAutoCommit(false);
            sendToAuctionPrecheck(pictureID, app, conn);
            prep = conn.prepareStatement("update picture set" + " auctionflag=true," + "auctionEndDate=?," + "bidPlayer=ownerid," + "maxbid=0," + "bidprice=0," + "numberOfBids=0" + "" + " where uid=?");
            Calendar c = new GregorianCalendar();
            c.add(Calendar.MINUTE, minutes);
            prep.setTimestamp(1, new Timestamp(c.getTimeInMillis()));
            prep.setLong(2, pictureID);
            prep.executeUpdate();
            conn.commit();
            doAfter.run();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtil.rollBack(conn);
            DBUtil.close(conn);
        }
    }

    public static boolean isProPlayer(Connection conn, String userid) {
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            prep = conn.prepareStatement("select ProExpiryDate from player where userid=?");
            prep.setString(1, userid);
            rs = prep.executeQuery();
            rs.next();
            Date d = rs.getDate(1);
            return d != null && d.getTime() > System.currentTimeMillis();
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtil.close(rs);
            DBUtil.close(prep);
        }
        return true;
    }

    public static void sendToAuctionPrecheck(long pictureID, Application app, Connection conn) throws UnableToCompleteDeal {
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            prep = conn.prepareStatement("select ownerid,AUCTIONFLAG,mortgaged, ownedDate from picture where uid=? for update");
            prep.setLong(1, pictureID);
            rs = prep.executeQuery();
            if (!rs.next()) {
                UnableToCompleteDeal exc = new UnableToCompleteDeal("Picture not Found.");
                throw exc;
            }
            if (!rs.getString(1).equals(app.getUser())) {
                UnableToCompleteDeal exc = new UnableToCompleteDeal("Picture already sold to someone else.");
                throw exc;
            }
            if (rs.getBoolean(2)) {
                UnableToCompleteDeal exc = new UnableToCompleteDeal("Picture already in Auction.");
                throw exc;
            }
            if (rs.getBoolean(3)) {
                UnableToCompleteDeal exc = new UnableToCompleteDeal("Can't auction mortgaged pictures.");
                throw exc;
            }
            Calendar cal = GregorianCalendar.getInstance();
            cal.add(Calendar.DAY_OF_WEEK, -1);
            if (rs.getTimestamp(4).getTime() > cal.getTimeInMillis()) {
                UnableToCompleteDeal exc = new UnableToCompleteDeal("You must hold a picture for 24 hours, before it can be sent to auction.");
                throw exc;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            DBUtil.close(rs);
            DBUtil.close(prep);
        }
    }

    public static void BuyGift(Gift gift, String fromPlayer, String toPlayer) throws UnableToCompleteDeal {
        Connection conn = DBUtil.getConnection();
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            conn.setAutoCommit(false);
            prep = conn.prepareStatement("select cash, tokens, blocker from player left outer join blockedplayer on blockedplayer.blockee=player.userid and blocker=? where userid=? for update");
            prep.setString(1, toPlayer);
            prep.setString(2, fromPlayer);
            rs = prep.executeQuery();
            rs.next();
            if (rs.getString(3) != null) throw new UnableToCompleteDeal("You are blocked from buying this player a gift.");
            if (rs.getLong(1) < gift.getPrice()) throw new UnableToCompleteDeal("You dont have enough cash to buy this gift.");
            if (rs.getLong(2) < gift.getTokens()) throw new UnableToCompleteDeal("You dont have enough tokens to buy this gift.");
            DBUtil.close(rs);
            DBUtil.close(prep);
            prep = conn.prepareStatement("update player set cash=cash-?, tokens=tokens-? where userid=?");
            prep.setLong(1, gift.getPrice());
            prep.setLong(2, gift.getTokens());
            prep.setString(3, fromPlayer);
            prep.executeUpdate();
            if (gift.getName().equals("Give 20 Tokens")) {
                prep.setLong(1, 0);
                prep.setLong(2, -20);
                prep.setString(3, toPlayer);
                prep.executeUpdate();
            }
            if (gift.isTikibarGift()) {
                prep.setLong(1, -gift.getPrice());
                prep.setLong(2, 0);
                prep.setString(3, "1247754377");
                prep.executeUpdate();
                prep.close();
                prep = conn.prepareStatement("insert into comments(type,key,posted,userid,comment) " + "values (?,?,CURRENT_TIMESTAMP() ,?,?)");
                prep.setString(1, "u");
                prep.setString(2, "1247754377");
                prep.setString(3, "T");
                prep.setString(4, "Received " + PossessedApplication.numberfmt.format(gift.getPrice()) + " from the sale of " + gift.getName() + " to &U" + fromPlayer);
                prep.executeUpdate();
            }
            DBUtil.close(prep);
            if (gift.getValue() > 0) {
                prep = conn.prepareStatement("insert into playerGifts " + "(giftName,fromUserid,toUserId,giftValue,giftTime) " + "values(?,?,?,?,current_timestamp)");
                prep.setString(1, gift.getName());
                prep.setString(2, fromPlayer);
                prep.setString(3, toPlayer);
                prep.setLong(4, gift.getValue());
                prep.executeUpdate();
                DBUtil.close(prep);
            }
            prep = conn.prepareStatement("insert into comments(type,key,posted,userid,comment,subtype) " + "values (?,?,CURRENT_TIMESTAMP() ,?,?,'G')");
            prep.setString(1, "U");
            prep.setString(2, toPlayer);
            prep.setString(3, fromPlayer);
            prep.setString(4, gift.getName());
            prep.executeUpdate();
            DBUtil.close(prep);
            prep = conn.prepareStatement("update player set activity_number=activity_number+1 where userid=?");
            prep.setString(1, toPlayer);
            prep.executeUpdate();
            conn.commit();
        } catch (SQLException err) {
            err.printStackTrace();
        } finally {
            DBUtil.rollBack(conn);
            DBUtil.close(rs);
            DBUtil.close(prep);
            DBUtil.close(conn);
        }
    }

    private static class BidPrecheckAmounts {

        private long bidPrice;

        private String highBidder;

        private long maxBid;

        private String uploader;

        private long auctionEndDate;

        public long getAuctionEndDate() {
            return auctionEndDate;
        }

        public void setAuctionEndDate(long auctionEndDate) {
            this.auctionEndDate = auctionEndDate;
        }

        public String getUploader() {
            return uploader;
        }

        public void setUploader(String uploader) {
            this.uploader = uploader;
        }

        public long getBidPrice() {
            return bidPrice;
        }

        public void setBidPrice(long bidPrice) {
            this.bidPrice = bidPrice;
        }

        public String getHighBidder() {
            return highBidder;
        }

        public void setHighBidder(String highBidder) {
            this.highBidder = highBidder;
        }

        public long getMaxBid() {
            return maxBid;
        }

        public void setMaxBid(long maxBid) {
            this.maxBid = maxBid;
        }
    }

    public static class BuyPrecheckAmounts {

        private long picturePrice;

        private long mortgagePrice;

        private String pictureSeller;

        private String pictureUploader;

        private boolean confirmBuyFlag;

        private long playerCash;

        private long discountPrice;

        private boolean allowBumping;

        public boolean isAllowBumping() {
            return allowBumping;
        }

        public void setAllowBumping(boolean allowBumping) {
            this.allowBumping = allowBumping;
        }

        public long getDiscountPrice() {
            return discountPrice;
        }

        public void setDiscountPrice(long discountPrice) {
            this.discountPrice = discountPrice;
        }

        public long getPlayerCash() {
            return playerCash;
        }

        public void setPlayerCash(long playerCash) {
            this.playerCash = playerCash;
        }

        public boolean isConfirmBuyFlag() {
            return confirmBuyFlag;
        }

        public void setConfirmBuyFlag(boolean confirmBuyFlag) {
            this.confirmBuyFlag = confirmBuyFlag;
        }

        public String getPictureSeller() {
            return pictureSeller;
        }

        public void setPictureSeller(String pictureSeller) {
            this.pictureSeller = pictureSeller;
        }

        public String getPictureUploader() {
            return pictureUploader;
        }

        public void setPictureUploader(String pictureUploader) {
            this.pictureUploader = pictureUploader;
        }

        public long getPicturePrice() {
            return picturePrice;
        }

        public void setPicturePrice(long picturePrice) {
            this.picturePrice = picturePrice;
        }

        public long getMortgagePrice() {
            return mortgagePrice;
        }

        public void setMortgagePrice(long mortgagePrice) {
            this.mortgagePrice = mortgagePrice;
        }
    }

    public static Dimension getJPEGDimension(String urls) throws IOException {
        URL url;
        Dimension d = null;
        try {
            url = new URL(urls);
            InputStream fis = url.openStream();
            if (fis.read() != 255 || fis.read() != 216) throw new RuntimeException("SOI (Start Of Image) marker 0xff 0xd8 missing");
            while (fis.read() == 255) {
                int marker = fis.read();
                int len = fis.read() << 8 | fis.read();
                if (marker == 192) {
                    fis.skip(1);
                    int height = fis.read() << 8 | fis.read();
                    int width = fis.read() << 8 | fis.read();
                    d = new Dimension(width, height);
                    break;
                }
                fis.skip(len - 2);
            }
            fis.close();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return d;
    }

    public static String getText(String name) {
        InputStream is = ResourceAnchor.class.getResourceAsStream("friendselector.txt");
        byte[] str = null;
        try {
            str = new byte[is.available()];
            is.read(str);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new String(str);
    }
}
