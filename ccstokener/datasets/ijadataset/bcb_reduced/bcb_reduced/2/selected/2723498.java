package com.liferay.portlet.auction.service.impl;

import java.util.List;
import com.liferay.portal.SystemException;
import com.liferay.portal.PortalException;
import com.liferay.portal.NoSuchUserException;
import com.liferay.portlet.auction.service.base.AuctionEntryLocalServiceBaseImpl;
import com.liferay.portlet.auction.service.AuctionEntryLocalServiceUtil;
import com.liferay.portlet.auction.model.impl.AuctionEntryImpl;
import com.liferay.portlet.auction.*;
import com.liferay.portlet.auction.service.persistence.*;
import com.liferay.portlet.auction.model.*;
import com.liferay.portal.service.ServiceContextUtil;
import com.liferay.portal.model.User;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.theme.PortletDisplay;
import java.util.Calendar;
import java.text.DateFormatSymbols;
import java.util.Locale;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.mail.MailMessage;
import com.liferay.portal.kernel.util.StringUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.FileUtil;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.File;
import java.io.Writer;
import java.io.BufferedReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import javax.mail.internet.InternetAddress;
import com.liferay.portlet.expando.model.ExpandoBridge;

/**
 * <a href="AuctionEntryLocalServiceImpl.java.html"><b><i>View Source</i></b></a>
 *
 * @author Bijan Vakili
 *
 */
public class AuctionEntryLocalServiceImpl extends AuctionEntryLocalServiceBaseImpl {

    public java.util.List<com.liferay.portlet.auction.model.AuctionEntry> findActiveAuctions() throws SystemException, PortalException {
        return AuctionEntryUtil.findByStatus(AuctionEntryImpl.STATUS_STARTED);
    }

    public String getInactiveStatus() {
        return AuctionEntryImpl.STATUS_ENDED;
    }

    public String getActiveStatus() {
        return AuctionEntryImpl.STATUS_STARTED;
    }

    public int countAuctionsByStatus(String status) throws SystemException, PortalException {
        return AuctionEntryUtil.countByStatus(status);
    }

    public java.util.List<com.liferay.portlet.auction.model.AuctionEntry> findByStatus(String status, int start, int end) throws SystemException, PortalException {
        return AuctionEntryUtil.findByStatus(status, start, end);
    }

    public java.util.List<com.liferay.portlet.auction.model.AuctionEntry> findAllAuctions() throws SystemException, PortalException {
        return AuctionEntryUtil.findAll();
    }

    public long getAuctionMaxBid(long aucId) throws NoSuchEntryException, SystemException, PortalException {
        long maxBid = 0l;
        AuctionEntry ae = AuctionEntryUtil.findByAuctionId(aucId);
        maxBid = ae.getMaxBids();
        return maxBid;
    }

    public void updateAuctionMaxBid(long aucId, long maxBid) throws NoSuchEntryException, SystemException, PortalException {
        AuctionEntry ae = AuctionEntryUtil.findByAuctionId(aucId);
        ae.setMaxBids(maxBid);
        auctionEntryPersistence.update(ae, false);
    }

    public boolean isAuctionActive(long aucId) throws NoSuchEntryException, SystemException, PortalException {
        boolean retVal = false;
        AuctionEntry ae = AuctionEntryUtil.findByAuctionId(aucId);
        if (ae != null) {
            if (AuctionEntryImpl.STATUS_STARTED.equals(ae.getStatus() + "")) {
                retVal = true;
            }
        }
        return retVal;
    }

    public void checkAuctions() throws SystemException, PortalException {
        java.util.List<com.liferay.portlet.auction.model.AuctionEntry> actvActns = findActiveAuctions();
        long crrnt = java.util.Calendar.getInstance().getTimeInMillis();
        for (com.liferay.portlet.auction.model.AuctionEntry aA : actvActns) {
            long end = aA.getEndDate();
            if (end < crrnt) {
                aA.setStatus(getInactiveStatus());
                long userid = aA.getOwnerUserId();
                long aucId = aA.getAuctionId();
                auctionEntryPersistence.update(aA, false);
                try {
                    sendEndAuctionEmails(aA);
                } catch (IOException e) {
                }
            }
        }
    }

    public void createAuction(long userId, long auctionId, long googcmId, long reservePrice, String description, String category, int days, String status, String title, byte[] imgBytes, ServiceContext serviceContext) throws SystemException, PortalException {
        long version = AuctionEntryImpl.DEFAULT_VERSION;
        this.createAuction("", userId, auctionId, googcmId, reservePrice, description, category, days, status, title, imgBytes, version, serviceContext);
    }

    public void createAuction(String uuid, long userId, long auctionId, long googcmId, long reservePrice, String description, String category, int days, String status, String title, byte[] imgBytes, long version, ServiceContext serviceContext) throws PortalException, SystemException {
        User user = userPersistence.findByPrimaryKey(userId);
        Calendar createDate = Calendar.getInstance();
        createDate.setTimeZone(user.getTimeZone());
        Calendar endDate = Calendar.getInstance();
        endDate.setTimeZone(user.getTimeZone());
        endDate.add(Calendar.DAY_OF_MONTH, days);
        validate(googcmId, reservePrice, description, category, days, status, title, imgBytes);
        long id = counterLocalService.increment();
        uuid = "" + counterLocalService.increment();
        long auctionEntryPrimKey = counterLocalService.increment();
        long imageId = counterLocalService.increment();
        auctionId = counterLocalService.increment();
        AuctionEntry aucEnt = auctionEntryPersistence.fetchByAuctionId(auctionId, false);
        if (aucEnt == null) {
            aucEnt = auctionEntryPersistence.create(auctionEntryPrimKey);
            aucEnt.setAuctionId(auctionId);
        }
        aucEnt.setUuid("" + uuid);
        aucEnt.setId(id);
        aucEnt.setAuctionId(auctionId);
        aucEnt.setImageId(imageId);
        aucEnt.setGoogcmId(googcmId);
        aucEnt.setCreateDate(createDate.getTimeInMillis());
        aucEnt.setEndDate(endDate.getTimeInMillis());
        aucEnt.setOwnerUserId(userId);
        aucEnt.setReservePrice(reservePrice);
        aucEnt.setDescription(description);
        aucEnt.setCategory(category);
        aucEnt.setStatus(getActiveStatus());
        aucEnt.setTitle(title);
        aucEnt.setMaxBids(0);
        aucEnt.setNumBids(0);
        auctionEntryPersistence.update(aucEnt, false);
        ExpandoBridge expandoBridge = aucEnt.getExpandoBridge();
        expandoBridge.setAttributes(serviceContext);
        saveImages(imageId, imgBytes);
        try {
            sendEmail(aucEnt);
        } catch (IOException e) {
        }
    }

    protected void saveImages(long imageId, byte[] imgBytes) throws PortalException, SystemException {
        if ((imgBytes != null) & (imgBytes.length > 0)) imageLocalService.updateImage(imageId, imgBytes);
    }

    protected void sendEmail(AuctionEntry aucEnt) throws IOException, PortalException, SystemException {
        User user = userPersistence.findByPrimaryKey(aucEnt.getOwnerUserId());
        String fromName = user.getFullName();
        String fromAddress = user.getEmailAddress();
        String toName = user.getFullName();
        String toAddress = user.getEmailAddress();
        String subject = "Auction of " + aucEnt.getTitle() + " Started";
        Calendar endDate = Calendar.getInstance();
        long endDateMillis = aucEnt.getEndDate();
        endDate.setTimeInMillis(endDateMillis);
        DateFormatSymbols dateFormatSymbols = new DateFormatSymbols();
        String[] wkdays = dateFormatSymbols.getWeekdays();
        String day = wkdays[endDate.get(Calendar.DAY_OF_WEEK)];
        String body = "Auction will continue until " + day + ".";
        InternetAddress from = new InternetAddress(fromAddress, fromName);
        InternetAddress to = new InternetAddress(toAddress, toName);
        MailMessage message = new MailMessage(from, to, subject, body, true);
        mailService.sendEmail(message);
    }

    protected void sendEndAuctionEmails(AuctionEntry aucEnt) throws IOException, PortalException, SystemException {
        long numBids = aucEnt.getNumBids();
        long maxBid = aucEnt.getMaxBids();
        long aucId = aucEnt.getAuctionId();
        BidEntry be = BidEntryUtil.findByAuctionId_BidAmount(maxBid, aucId);
        long bidderId = be.getUserId();
        long bidId = be.getBidId();
        User sllr = userPersistence.findByPrimaryKey(aucEnt.getOwnerUserId());
        User byr = userPersistence.findByPrimaryKey(bidderId);
        String sllrName = sllr.getFullName();
        String sllrAddress = sllr.getEmailAddress();
        String byrName = byr.getFullName();
        String byrAddress = byr.getEmailAddress();
        String form = "<form action='https://checkout.google.com/api/checkout/v2/checkoutForm/Merchant/731745742367936' id='BB_BuyButtonForm' method='post' name='BB_BuyButtonForm' target='_top'>";
        form += "    <input name='item_name_1' type='hidden' value='" + aucEnt.getTitle() + ".00'/>";
        form += "    <input name='item_description_1' type='hidden' value='" + aucEnt.getDescription() + "'/>";
        form += "    <input name='item_quantity_1' type='hidden' value='1'/>";
        form += "    <input name='item_price_1' type='hidden' value='" + maxBid + "'/>";
        form += "    <input name='item_currency_1' type='hidden' value='USD'/>";
        form += "    <input name='_charset_' type='hidden' value='utf-8'/>";
        form += "    <input alt='' src='https://checkout.google.com/buttons/buy.gif?merchant_id=731745742367936&amp;w=117&amp;h=48&amp;style=white&amp;variant=text&amp;loc=en_US' type='image'/>";
        form += "</form>";
        String sllrSubject = "Auction #" + aucEnt.getAuctionId() + " Ended ";
        String byrSubject = "You Won Auction #" + aucEnt.getAuctionId();
        String sllrBody = "The auction ended. The buyer, " + byrName + " can be contacted at " + byrAddress + ". " + byrName + " will make a payment for amount of $" + maxBid + " by using the following form:<BR/>" + form;
        String byrBody = "You have won the auction. Please promptly make the payment by using the following form:<BR/>" + form;
        System.out.println("EMAIL BODY: " + sllrBody);
        System.out.println("EMAIL BODY: " + byrBody);
        InternetAddress byrIA = new InternetAddress(byrAddress, byrName);
        InternetAddress sllrIA = new InternetAddress(sllrAddress, sllrName);
        MailMessage sllrMessage = new MailMessage(sllrIA, sllrIA, sllrSubject, sllrBody, true);
        MailMessage byrMessage = new MailMessage(byrIA, byrIA, byrSubject, byrBody, true);
        mailService.sendEmail(sllrMessage);
        mailService.sendEmail(byrMessage);
    }

    protected void validate(long googcmId, long reservePrice, String description, String category, int days, String status, String title, byte[] imgBytes) throws PortalException, SystemException {
        if (Validator.isNull(description)) throw new AuctionDescriptionException(); else if (Validator.isNull(title)) throw new AuctionTitleException(); else if (Validator.isNull(category)) throw new CategoryIdException();
        if (googcmId < 1000000000l | googcmId > 999999999999999l) throw new AuctionGoogCMIdException();
        long imgMaxSize = 1048576l;
        if ((imgBytes == null) || (imgBytes.length > ((int) imgMaxSize))) throw new AuctionImageSizeException();
        if (days != 3 & days != 7 & days != 10) throw new AuctionEndeDateException();
        if ((reservePrice < 0) || (reservePrice > 10000)) throw new AuctionReservePriceException();
        try {
            URL url = new URL("https://checkout.google.com/api/checkout/v2/checkoutForm/Merchant/" + googcmId);
            URLConnection conn = url.openConnection();
            BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            boolean sellerExists = true;
            String line;
            while ((line = rd.readLine()) != null) {
                if (line.contains("" + googcmId)) {
                    throw new AuctionGoogCMAccountException();
                }
            }
            rd.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
