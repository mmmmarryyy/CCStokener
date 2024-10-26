package daPackage;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.LinkedList;
import daDTO.GBDTO;
import daDTO.GBPageDTO;
import daDTO.UserPageDTO;

/**
 * @author Francesco Carello, David Detzler, Timo Pauli
 *         Fachhochschule-Kaiserslautern 2006 da-com daPackage this class
 *         provides all methods for crawling the community.
 * 
 */
public class DaCrawler {

    public static final String userAgent = "da-crawler 1.5 -- www.fh-kl.de --";

    public static final String userPageUrl = "http://www.gesichterparty.de/index.php?modul=viewprofile&user_id=";

    public static final String guestBookUrl = "http://www.gesichterparty.de/index.php?modul=userguestbook&user_id=";

    /**
	 * @param userID,
	 *            the user page to be crawled
	 * @return a UserPageDTO which contains the user page-string and the userid
	 */
    public static UserPageDTO getUserPage(int userID) {
        String line;
        StringBuffer readBuffer = new StringBuffer();
        UserPageDTO pageDTO = new UserPageDTO();
        pageDTO.setId(userID);
        try {
            URL url = new URL(userPageUrl + userID);
            HttpURLConnection connect = (HttpURLConnection) url.openConnection();
            connect.addRequestProperty("User-Agent", userAgent);
            System.out.println("DaCrawler::getUserPage:userid-->" + userID);
            BufferedReader input = new BufferedReader(new InputStreamReader(connect.getInputStream()));
            while ((line = input.readLine()) != null) {
                readBuffer.append(line);
            }
            input.close();
            connect.disconnect();
            pageDTO.setThepage(readBuffer.toString());
            return pageDTO;
        } catch (MalformedURLException mue) {
            System.err.println("crawlUserPage::Bad URL-->" + mue);
            return null;
        } catch (IOException ioe) {
            System.err.println("DaCrawler::crawlUserPage::IOException-->" + ioe);
            return null;
        }
    }

    /**
	 * @param id,
	 *            the owners userid
	 * @return a GBDTO containing the guestbook and the userid
	 */
    public static GBDTO getGuestBook(int id) {
        GBDTO guestBook = new GBDTO(id);
        GBPageDTO pageDTO = null;
        Collection<String> pageColl = new LinkedList<String>();
        int page = 0;
        boolean hasMore = true;
        try {
            while (hasMore) {
                guestBook.setCurrentPageNumber(page);
                pageDTO = getSingleGBPage(guestBook.getId(), guestBook.getCurrentPageNumber());
                pageColl.add(pageDTO.getThePage());
                System.out.println(Thread.currentThread().getName() + " crawlGuestBook-->crawlPage " + page + " ID " + id);
                page++;
                hasMore = pageDTO.hasMorePages();
            }
        } catch (Exception e) {
            System.err.println("DaCrawler::getGuestBook::unknown error-->" + e);
            if (pageColl.size() > 0) {
                guestBook.setInterrupted(true);
                guestBook.setTheGuestBook(pageColl);
                return guestBook;
            }
            e.printStackTrace();
            return null;
        }
        guestBook.setTheGuestBook(pageColl);
        return guestBook;
    }

    /**
	 * @param guestBook,
	 *            which needs to be finished
	 * @return the complete guestbook
	 */
    public static GBDTO getInterruptedGuestBook(GBDTO guestBook) {
        GBPageDTO pageDTO = null;
        Collection<String> pageColl = guestBook.getTheGuestBook();
        int page = guestBook.getCurrentPageNumber();
        int oldSize = pageColl.size();
        guestBook.setInterrupted(false);
        boolean hasMore = true;
        try {
            while (hasMore) {
                guestBook.setCurrentPageNumber(page);
                pageDTO = getSingleGBPage(guestBook.getId(), guestBook.getCurrentPageNumber());
                pageColl.add(pageDTO.getThePage());
                System.out.println("DaCrawler::getInterruptedGuestBook:" + "crawlInterruptedGuestBook-->crawlPage " + page + " ID " + guestBook.getId() + "Size " + pageColl.size());
                page++;
                hasMore = pageDTO.hasMorePages();
            }
        } catch (Exception e) {
            System.err.println("DaCrawler::getInterruptedGuestBook::unknown error-->" + e);
            if (pageColl.size() == oldSize) {
                guestBook.setInterrupted(false);
                return guestBook;
            } else {
                if (pageColl.size() > oldSize) {
                    guestBook.setInterrupted(true);
                    guestBook.setTheGuestBook(pageColl);
                    pageColl = null;
                    return guestBook;
                }
            }
            e.printStackTrace();
            return null;
        }
        guestBook.setTheGuestBook(pageColl);
        guestBook.setInterrupted(false);
        pageColl = null;
        return guestBook;
    }

    /**
	 * @param id,
	 *            the owners userid
	 * @param pageNumber
	 * @return a GBPageDTO containing the guest book page
	 */
    public static GBPageDTO getSingleGBPage(int id, int pageNumber) {
        String site = null;
        GBPageDTO pageDTO = new GBPageDTO(id, pageNumber);
        try {
            String line;
            StringBuffer readBuffer = new StringBuffer();
            URL url = new URL(guestBookUrl + id + "&seite=" + pageNumber);
            HttpURLConnection connect = (HttpURLConnection) url.openConnection();
            connect.addRequestProperty("User-Agent", userAgent);
            BufferedReader input = new BufferedReader(new InputStreamReader(connect.getInputStream()));
            while ((line = input.readLine()) != null) {
                readBuffer.append("\n" + line);
            }
            input.close();
            connect.disconnect();
            site = readBuffer.toString();
        } catch (MalformedURLException mue) {
            System.err.println("getSingleGBPage::Bad URL-->" + mue);
            return null;
        } catch (IOException ioe) {
            System.err.println("getSingleGBPage::IOError-->" + ioe);
            return null;
        }
        if ((!site.contains("<td style=\"text-align:left;\" width=\"35\">")) || (site.contains("gp_negativ.png"))) {
            pageDTO.setMorePages(false);
        } else {
            pageDTO.setMorePages(true);
        }
        pageDTO.setThePage(site);
        return pageDTO;
    }
}
