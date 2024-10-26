package com.jdkcn;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.criterion.DetachedCriteria;
import org.hibernate.criterion.MatchMode;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import com.jdkcn.dao.CategoryDao;
import com.jdkcn.dao.CommentDao;
import com.jdkcn.dao.EntryDao;
import com.jdkcn.dao.GuestBookDao;
import com.jdkcn.dao.LinkDao;
import com.jdkcn.dao.RequestCounterDao;
import com.jdkcn.dao.RoleDao;
import com.jdkcn.dao.SiteConfigDao;
import com.jdkcn.dao.TagDao;
import com.jdkcn.dao.UserDao;
import com.jdkcn.domain.Category;
import com.jdkcn.domain.Comment;
import com.jdkcn.domain.Entry;
import com.jdkcn.domain.GuestBook;
import com.jdkcn.domain.Link;
import com.jdkcn.domain.Mail;
import com.jdkcn.domain.RequestCounter;
import com.jdkcn.domain.Role;
import com.jdkcn.domain.SiteConfig;
import com.jdkcn.domain.Tag;
import com.jdkcn.domain.User;
import com.jdkcn.exception.InvalidPasswordException;
import com.jdkcn.exception.InvalidUsernameException;
import com.jdkcn.exception.UsernameAlreadyExistException;
import com.jdkcn.service.MailService;
import com.jdkcn.util.Constants;
import com.jdkcn.util.DateUtil;
import com.jdkcn.util.MyblogUtil;
import com.jdkcn.util.PaginationSupport;
import com.jdkcn.util.SHA;
import com.jdkcn.util.TextUtil;
import com.thoughtworks.xstream.XStream;

/**
 * @author <a href="mailto:rory.cn@gmail.com">somebody</a>
 * @since Sep 12, 2006 9:58:37 AM
 * @version $Id: BlogFacadeImpl.java $
 */
public class BlogFacadeImpl implements BlogFacade {

    private Log log = LogFactory.getLog(BlogFacadeImpl.class);

    private EntryDao entryDao;

    private CategoryDao categoryDao;

    private CommentDao commentDao;

    private GuestBookDao guestBookDao;

    private TagDao tagDao;

    private UserDao userDao;

    private MailService mailService;

    private RoleDao roleDao;

    private RequestCounterDao requestCounterDao;

    private LinkDao linkDao;

    private Resource configPath;

    private SiteConfigDao siteConfigDao;

    private Map<String, Object> cache = new HashMap<String, Object>();

    private int last_recent_entry_size = 0;

    public void setSiteConfigDao(SiteConfigDao siteConfigDao) {
        this.siteConfigDao = siteConfigDao;
    }

    public void setLinkDao(LinkDao linkDao) {
        this.linkDao = linkDao;
    }

    public void setConfigPath(Resource configPath) {
        this.configPath = configPath;
    }

    public void setRoleDao(RoleDao roleDao) {
        this.roleDao = roleDao;
    }

    public void setRequestCounterDao(RequestCounterDao requestCounterDao) {
        this.requestCounterDao = requestCounterDao;
    }

    public void setMailService(MailService mailService) {
        this.mailService = mailService;
    }

    public void setCommentDao(CommentDao commentDao) {
        this.commentDao = commentDao;
    }

    public void setGuestBookDao(GuestBookDao guestBookDao) {
        this.guestBookDao = guestBookDao;
    }

    public void setTagDao(TagDao tagDao) {
        this.tagDao = tagDao;
    }

    public void setUserDao(UserDao userDao) {
        this.userDao = userDao;
    }

    public void setEntryDao(EntryDao entryDao) {
        this.entryDao = entryDao;
    }

    public void setCategoryDao(CategoryDao categoryDao) {
        this.categoryDao = categoryDao;
    }

    public void saveOrUpdateRole(Role role) {
        this.roleDao.saveOrUpdate(role);
    }

    public Entry getEntry(String id) {
        return this.entryDao.get(id);
    }

    public Entry plusHits(String entryId) {
        Entry entry = this.entryDao.get(entryId);
        if (entry.getHits() == null) {
            entry.setHits(1);
        } else {
            entry.setHits(entry.getHits() + 1);
        }
        this.entryDao.saveOrUpdate(entry);
        return entry;
    }

    public Entry plusCommentSize(String entryId) {
        Entry entry = this.entryDao.get(entryId);
        if (entry.getCommentSize() == null) {
            entry.setCommentSize(1);
        } else {
            entry.setCommentSize(entry.getCommentSize() + 1);
        }
        this.entryDao.saveOrUpdate(entry);
        return entry;
    }

    public Entry getNextEntry(String id) {
        return this.entryDao.getNextEntry(id);
    }

    public Entry getPreviousEntry(String id) {
        return this.entryDao.getPreviousEntry(id);
    }

    public Entry getEntryByName(String name) {
        return this.entryDao.getEntryByName(name);
    }

    @SuppressWarnings("unchecked")
    public List<Entry> getRecentEntries(int size) {
        if (size == last_recent_entry_size) {
            if (cache.get(Constants.RECENT_ENTRIES_CACHE_KEY) != null) {
                if (log.isDebugEnabled()) {
                    log.debug("[myblog]:loading cached recent entries....");
                }
                return (List<Entry>) cache.get(Constants.RECENT_ENTRIES_CACHE_KEY);
            }
        } else {
            last_recent_entry_size = size;
        }
        List<Entry> entries = this.entryDao.getRecentEntries(size);
        cache.put(Constants.RECENT_ENTRIES_CACHE_KEY, entries);
        return entries;
    }

    public void saveOrUpdateEntry(Entry entry) {
        this.entryDao.saveOrUpdate(entry);
        clearRecentEntriesCache();
        clearMonthListCache();
        clearHotTagsCache();
    }

    public void saveOrUpdatePage(Entry entry) {
        this.entryDao.saveOrUpdate(entry);
        clearPagesCache();
    }

    public void saveOrUpdateCategory(Category category) {
        this.categoryDao.saveOrUpdate(category);
        clearCategoriesCache();
    }

    public void removeCategory(String id) {
        Category category = this.categoryDao.get(id);
        this.categoryDao.remove(category);
        clearCategoriesCache();
    }

    public Category getCategory(String id) {
        return categoryDao.get(id);
    }

    public Category getCategoryByName(String name) {
        return this.categoryDao.getCategoryByName(name);
    }

    @SuppressWarnings("unchecked")
    public List<Category> getCategories() {
        if (cache.get(Constants.CATEGORIES_CACHE_KEY) != null) {
            if (log.isDebugEnabled()) {
                log.debug("[myblog]:loading cached categories...");
            }
            return (List<Category>) cache.get(Constants.CATEGORIES_CACHE_KEY);
        } else {
            List<Category> categories = this.categoryDao.getCategories();
            cache.put(Constants.CATEGORIES_CACHE_KEY, categories);
            return categories;
        }
    }

    public PaginationSupport<Entry> getEntryPage(Entry entry, int pageSize, int startIndex, String order, Boolean isDesc) {
        DetachedCriteria criteria = DetachedCriteria.forClass(Entry.class);
        if (StringUtils.isNotBlank(entry.getTitle()) && StringUtils.isNotBlank(entry.getContent())) {
            criteria.add(Restrictions.or(Restrictions.ilike("title", entry.getTitle(), MatchMode.ANYWHERE), Restrictions.ilike("content", entry.getContent(), MatchMode.ANYWHERE)));
        } else if (StringUtils.isNotBlank(entry.getTitle())) {
            criteria.add(Restrictions.ilike("title", entry.getTitle(), MatchMode.ANYWHERE));
        } else if (StringUtils.isNotBlank(entry.getContent())) {
            criteria.add(Restrictions.ilike("content", entry.getContent()));
        }
        if (StringUtils.isNotBlank(entry.getEntryStatus())) {
            criteria.add(Restrictions.eq("entryStatus", entry.getEntryStatus()));
        }
        if (StringUtils.isNotBlank(entry.getType())) {
            criteria.add(Restrictions.eq("type", entry.getType()));
        }
        if (StringUtils.isNotBlank(order)) {
            if (isDesc == null || !isDesc) {
                criteria.addOrder(Order.asc(order));
            } else {
                criteria.addOrder(Order.desc(order));
            }
        } else {
            criteria.addOrder(Order.desc("postTime"));
        }
        return this.entryDao.findPageByCriteria(criteria, pageSize, startIndex);
    }

    public PaginationSupport<Entry> getEntryPageByCategoryId(String categoryId, int pageSize, int startIndex, String order, Boolean isDesc) {
        DetachedCriteria criteria = DetachedCriteria.forClass(Entry.class);
        if (StringUtils.isNotBlank(order)) {
            if (isDesc == null || !isDesc) {
                criteria.addOrder(Order.asc(order));
            } else {
                criteria.addOrder(Order.desc(order));
            }
        } else {
            criteria.addOrder(Order.desc("postTime"));
        }
        criteria.add(Restrictions.eq("entryStatus", Entry.EntryStatus.PUBLISH));
        criteria.add(Restrictions.eq("type", Entry.Type.POST));
        criteria.createCriteria("categories").add(Restrictions.eq("id", categoryId));
        return this.entryDao.findPageByCriteria(criteria, pageSize, startIndex);
    }

    public PaginationSupport<Entry> getEntryPageByTagId(String tagId, int pageSize, int startIndex, String order, Boolean isDesc) {
        DetachedCriteria criteria = DetachedCriteria.forClass(Entry.class);
        if (StringUtils.isNotBlank(order)) {
            if (isDesc == null || !isDesc) {
                criteria.addOrder(Order.asc(order));
            } else {
                criteria.addOrder(Order.desc(order));
            }
        } else {
            criteria.addOrder(Order.desc("postTime"));
        }
        criteria.add(Restrictions.eq("entryStatus", Entry.EntryStatus.PUBLISH));
        criteria.add(Restrictions.eq("type", Entry.Type.POST));
        criteria.createCriteria("tags").add(Restrictions.eq("id", tagId));
        return this.entryDao.findPageByCriteria(criteria, pageSize, startIndex);
    }

    public PaginationSupport<Entry> getEntryPageByTagName(String tagName, int pageSize, int startIndex, String order, Boolean isDesc) {
        DetachedCriteria criteria = DetachedCriteria.forClass(Entry.class);
        if (StringUtils.isNotBlank(order)) {
            if (isDesc == null || !isDesc) {
                criteria.addOrder(Order.asc(order));
            } else {
                criteria.addOrder(Order.desc(order));
            }
        } else {
            criteria.addOrder(Order.desc("postTime"));
        }
        criteria.add(Restrictions.eq("entryStatus", Entry.EntryStatus.PUBLISH));
        criteria.add(Restrictions.eq("type", Entry.Type.POST));
        criteria.createCriteria("tags").add(Restrictions.eq("name", tagName));
        return this.entryDao.findPageByCriteria(criteria, pageSize, startIndex);
    }

    public PaginationSupport<Entry> getEntryPageByCategoryName(String categoryName, int pageSize, int startIndex, String order, Boolean isDesc) {
        DetachedCriteria criteria = DetachedCriteria.forClass(Entry.class);
        if (StringUtils.isNotBlank(order)) {
            if (isDesc == null || !isDesc) {
                criteria.addOrder(Order.asc(order));
            } else {
                criteria.addOrder(Order.desc(order));
            }
        } else {
            criteria.addOrder(Order.desc("postTime"));
        }
        criteria.add(Restrictions.eq("entryStatus", Entry.EntryStatus.PUBLISH));
        criteria.add(Restrictions.eq("type", Entry.Type.POST));
        criteria.createCriteria("categories").add(Restrictions.eq("name", categoryName));
        return this.entryDao.findPageByCriteria(criteria, pageSize, startIndex);
    }

    public PaginationSupport<Entry> getEntryPageByMonth(Date month, int pageSize, int startIndex, String order, Boolean isDesc) {
        DetachedCriteria criteria = DetachedCriteria.forClass(Entry.class);
        if (StringUtils.isNotBlank(order)) {
            if (isDesc == null || !isDesc) {
                criteria.addOrder(Order.asc(order));
            } else {
                criteria.addOrder(Order.desc(order));
            }
        } else {
            criteria.addOrder(Order.desc("postTime"));
        }
        criteria.add(Restrictions.eq("entryStatus", Entry.EntryStatus.PUBLISH));
        criteria.add(Restrictions.eq("type", Entry.Type.POST));
        criteria.add(Restrictions.between("postTime", DateUtil.getMonthStartTime(month), DateUtil.getMonthEndTime(month)));
        return this.entryDao.findPageByCriteria(criteria, pageSize, startIndex);
    }

    public PaginationSupport<Comment> getCommentPageByEntryId(String entryId, int pageSize, int startIndex, String order, Boolean isDesc) {
        DetachedCriteria criteria = DetachedCriteria.forClass(Comment.class);
        criteria.createCriteria("entry").add(Restrictions.eq("id", entryId));
        return this.commentDao.findPageByCriteria(criteria, pageSize, startIndex);
    }

    public Comment getComment(String id) {
        return this.commentDao.get(id);
    }

    public PaginationSupport<Comment> getCommentPage(Comment comment, int pageSize, int startIndex, String order, Boolean isDesc) {
        DetachedCriteria criteria = DetachedCriteria.forClass(Comment.class);
        if (StringUtils.isNotBlank(comment.getPostIP())) {
            criteria.add(Restrictions.eq("postIP", comment.getPostIP()));
        }
        if (StringUtils.isNotBlank(comment.getStatus())) {
            criteria.add(Restrictions.eq("status", comment.getStatus()));
        }
        if (StringUtils.isNotBlank(order)) {
            if (isDesc == null || !isDesc) {
                criteria.addOrder(Order.asc(order));
            } else {
                criteria.addOrder(Order.desc(order));
            }
        } else {
            criteria.addOrder(Order.desc("postTime"));
        }
        return this.commentDao.findPageByCriteria(criteria, pageSize, startIndex);
    }

    public List<Comment> getCommentListByEntryId(String entryId) {
        return this.commentDao.getCommentListByEntryId(entryId);
    }

    public List<Comment> getCommentListByEntryIdAndStatus(String entryId, String status) {
        return this.commentDao.getCommentListByEntryIdAndStatus(entryId, status);
    }

    @SuppressWarnings("unchecked")
    public List<Comment> getRecentComments(int size) {
        if (cache.get(Constants.RECENT_COMMENTS_CACHE_KEY) != null) {
            if (log.isDebugEnabled()) {
                log.debug("[myblog]:loading cached recent comments....");
            }
            return (List<Comment>) cache.get(Constants.RECENT_COMMENTS_CACHE_KEY);
        } else {
            List<Comment> comments = this.commentDao.getRecentComments(size);
            cache.put(Constants.RECENT_COMMENTS_CACHE_KEY, comments);
            return comments;
        }
    }

    public User getUser(String id) {
        return this.userDao.get(id);
    }

    public User login(String username, String password) throws InvalidUsernameException, InvalidPasswordException {
        User user = this.userDao.getUserByUsername(username);
        if (user == null) {
            throw new InvalidUsernameException("Invalid Username!,no user with username:" + username);
        }
        if (!user.getPassword().equals(SHA.hashPassword(password))) {
            throw new InvalidPasswordException("Invalid password!");
        }
        return user;
    }

    public User cookieLogin(String username, String password) throws InvalidUsernameException, InvalidPasswordException {
        User user = this.userDao.getUserByUsername(username);
        if (user == null) {
            throw new InvalidUsernameException("Invalid Username!,no user with username:" + username);
        }
        if (!SHA.hashPassword(user.getPassword()).equals(password)) {
            throw new InvalidPasswordException("Invalid password!");
        }
        return user;
    }

    public User getUserByUsername(String username) {
        return this.userDao.getUserByUsername(username);
    }

    public void removeComment(String id) {
        Comment comment = this.commentDao.get(id);
        Entry entry = comment.getEntry();
        if (entry.getCommentSize() > 0) {
            entry.setCommentSize(entry.getCommentSize() - 1);
        }
        this.entryDao.saveOrUpdate(entry);
        this.commentDao.remove(this.commentDao.get(id));
        clearRecentCommentsCache();
    }

    public void removeEntry(String id) {
        this.entryDao.remove(this.entryDao.get(id));
        this.commentDao.removeCommentsByEntryId(id);
        clearRecentCommentsCache();
        clearRecentEntriesCache();
        clearMonthListCache();
    }

    public void removePage(String id) {
        this.entryDao.remove(this.entryDao.get(id));
        clearPagesCache();
    }

    public void removeUser(String id) {
        this.userDao.remove(this.userDao.get(id));
    }

    public void saveOrUpdateComment(Comment comment) {
        Assert.notNull(comment.getEntry(), "comment 's target is required");
        this.commentDao.saveOrUpdate(comment);
        clearRecentCommentsCache();
    }

    public void saveOrUpdateUser(User user) {
        if (StringUtils.isBlank(user.getNickname())) {
            user.setNickname(user.getUsername());
        }
        user.setPassword(SHA.hashPassword(user.getPassword()));
        if (log.isDebugEnabled()) {
            log.debug("encrypt password to:" + SHA.hashPassword(user.getPassword()));
        }
        this.userDao.saveOrUpdate(user);
    }

    public void updateUserWithoutEncryptPassword(User user) {
        if (StringUtils.isBlank(user.getNickname())) {
            user.setNickname(user.getUsername());
        }
        this.userDao.saveOrUpdate(user);
    }

    public PaginationSupport<User> getUserPage(User user, int pageSize, int startIndex, String order, Boolean isDesc) {
        DetachedCriteria criteria = DetachedCriteria.forClass(User.class);
        if (StringUtils.isNotBlank(user.getUsername())) {
            criteria.add(Restrictions.eq("username", user.getUsername()));
        }
        if (StringUtils.isNotBlank(user.getNickname())) {
            criteria.add(Restrictions.ilike("nickname", user.getNickname()));
        }
        return this.userDao.findPageByCriteria(criteria, pageSize, startIndex);
    }

    public GuestBook getGuestBook(String id) {
        return this.guestBookDao.get(id);
    }

    public void removeGuestBook(String id) {
        this.guestBookDao.remove(this.guestBookDao.get(id));
    }

    public void removeTag(String id) {
        this.tagDao.remove(this.tagDao.get(id));
    }

    public void saveOrUpdateGuestBook(GuestBook guestBook) {
        this.guestBookDao.saveOrUpdate(guestBook);
    }

    public void saveOrUpdateTag(Tag tag) {
        this.tagDao.saveOrUpdate(tag);
    }

    public List<Tag> getTags() {
        return this.tagDao.getTags();
    }

    public Tag getTag(String id) {
        return this.tagDao.get(id);
    }

    public Tag getTagByName(String name) {
        return this.tagDao.getTagByName(name);
    }

    public User register(User user) throws UsernameAlreadyExistException {
        this.userDao.saveOrUpdate(user);
        Mail mail = new Mail();
        this.mailService.send(mail);
        return user;
    }

    public void saveOrUpdateRequestCounter(RequestCounter requestCounter) {
        this.requestCounterDao.saveOrUpdate(requestCounter);
    }

    public void sendMail(Mail mail) {
        this.mailService.send(mail);
    }

    public SiteConfig getDatabaseSiteConfig() {
        if (cache.get(Constants.SITECONFIG_CACHE_KEY) != null) {
            if (log.isDebugEnabled()) {
                log.debug("[myblog]:loading cached siteconfig....");
            }
            return (SiteConfig) cache.get(Constants.SITECONFIG_CACHE_KEY);
        }
        DetachedCriteria criteria = DetachedCriteria.forClass(SiteConfig.class);
        List<SiteConfig> result = this.siteConfigDao.getListByCriteria(criteria);
        if (result.isEmpty()) {
            return null;
        }
        SiteConfig siteConfig = result.get(0);
        cache.put(Constants.SITECONFIG_CACHE_KEY, siteConfig);
        return siteConfig;
    }

    public synchronized SiteConfig getSiteConfig() {
        if (cache.get(Constants.SITECONFIG_CACHE_KEY) != null) {
            if (log.isDebugEnabled()) {
                log.debug("[myblog]:loading cached siteconfig....");
            }
            return (SiteConfig) cache.get(Constants.SITECONFIG_CACHE_KEY);
        }
        SiteConfig siteConfig = new SiteConfig();
        XStream xStream = new XStream();
        xStream.alias("siteConfig", SiteConfig.class);
        BufferedReader reader;
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(configPath.getFile()), "UTF-8"));
            siteConfig = (SiteConfig) xStream.fromXML(reader);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (siteConfig.getLimitLength() == null) {
            siteConfig.setLimitLength(500);
        }
        if (StringUtils.isBlank(siteConfig.getTheme())) {
            siteConfig.setTheme("default");
        }
        cache.put(Constants.SITECONFIG_CACHE_KEY, siteConfig);
        return siteConfig;
    }

    public synchronized void saveSiteConfig(SiteConfig siteConfig) {
        XStream xStream = new XStream();
        xStream.alias("siteConfig", SiteConfig.class);
        StringBuffer sb = new StringBuffer();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append(xStream.toXML(siteConfig));
        try {
            Writer out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(configPath.getFile()), "UTF-8"));
            out.write(sb.toString());
            out.flush();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        clearSiteConfigCache();
    }

    public void saveOrUpdateSiteConfig(SiteConfig siteConfig) {
        this.siteConfigDao.saveOrUpdate(siteConfig);
        clearSiteConfigCache();
    }

    public void saveOrUpdateLink(Link link) {
        this.linkDao.saveOrUpdate(link);
        clearLinksCache();
        clearRecommendLinksCache();
    }

    public void removeLink(String id) {
        this.linkDao.remove(getLink(id));
        clearLinksCache();
        clearRecommendLinksCache();
    }

    public Link getLink(String id) {
        return this.linkDao.get(id);
    }

    @SuppressWarnings("unchecked")
    public List<Link> getLinks() {
        if (cache.get(Constants.LINKS_CACHE_KEY) != null) {
            if (log.isDebugEnabled()) {
                log.debug("[myblog]:loading cached links.");
            }
            return (List<Link>) cache.get(Constants.LINKS_CACHE_KEY);
        }
        List<Link> links = this.linkDao.getLinks();
        cache.put(Constants.LINKS_CACHE_KEY, links);
        return links;
    }

    @SuppressWarnings("unchecked")
    public List<Link> getRecommendLinks() {
        if (cache.get(Constants.RECOMMEND_LINKS_CACHE_KEY) != null) {
            if (log.isDebugEnabled()) {
                log.debug("[myblog]:loading cached recommend links.");
            }
            return (List<Link>) cache.get(Constants.RECOMMEND_LINKS_CACHE_KEY);
        }
        List<Link> links = this.linkDao.getRecommendLinks();
        cache.put(Constants.RECOMMEND_LINKS_CACHE_KEY, links);
        return null;
    }

    public void sendTrackback(String entryId, List<String> trackbackURLs) {
        Entry entry = getEntry(entryId);
        if (entry == null) {
            return;
        }
        String title = entry.getTitle();
        String excerpt = StringUtils.left(MyblogUtil.removeHTML(entry.getContent()), 255);
        String url = getSiteConfig().getSiteURL() + "/entry/";
        if (StringUtils.isNotBlank(entry.getName())) {
            url += entry.getName() + ".html";
        } else {
            url += "id/" + entry.getId() + ".html";
        }
        String blogName = getSiteConfig().getSiteName();
        StringBuffer sb = new StringBuffer();
        try {
            sb.append(URLEncoder.encode("title", "UTF-8")).append("=").append(URLEncoder.encode(title, "UTF-8"));
            sb.append("&").append(URLEncoder.encode("excerpt", "UTF-8")).append("=").append(URLEncoder.encode(excerpt, "UTF-8"));
            sb.append("&").append(URLEncoder.encode("url", "UTF-8")).append("=").append(URLEncoder.encode(url, "UTF-8"));
            sb.append("&").append(URLEncoder.encode("blog_name", "UTF-8")).append("=").append(URLEncoder.encode(blogName, "UTF-8"));
            for (String trackURL : trackbackURLs) {
                URL tburl = new URL(trackURL);
                HttpURLConnection conn = (HttpURLConnection) tburl.openConnection();
                conn.setDoOutput(true);
                OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
                BufferedReader rd = null;
                try {
                    wr.write(sb.toString());
                    wr.flush();
                    boolean inputAvailable = false;
                    try {
                        rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                        inputAvailable = true;
                    } catch (Throwable e) {
                        log.error("connection error", e);
                    }
                    if (inputAvailable) {
                        String line;
                        StringBuffer resultBuff = new StringBuffer();
                        while ((line = rd.readLine()) != null) {
                            resultBuff.append(TextUtil.escapeHTML(line, true));
                            resultBuff.append("<br />");
                        }
                        log.info("trackback ok:" + resultBuff);
                    }
                    if (conn.getResponseCode() > 399) {
                        log.info("trackback error with errorCode:" + conn.getResponseCode());
                    } else {
                        log.debug("trackback ok with code:" + conn.getResponseCode());
                    }
                } finally {
                    if (wr != null) wr.close();
                    if (rd != null) rd.close();
                }
            }
        } catch (IOException ex) {
            log.error("trackback error:", ex);
        }
    }

    public void approveComment(String commentId) {
        Comment comment = getComment(commentId);
        if (!Comment.Status.APPROVED.equals(comment.getStatus())) {
            comment.setStatus(Comment.Status.APPROVED);
            this.commentDao.saveOrUpdate(comment);
        }
        clearRecentCommentsCache();
    }

    public void againstComment(String commentId) {
        Comment comment = getComment(commentId);
        if (!Comment.Status.WAIT_FOR_APPROVE.equals(comment.getStatus())) {
            comment.setStatus(Comment.Status.WAIT_FOR_APPROVE);
            this.commentDao.saveOrUpdate(comment);
        }
        clearRecentCommentsCache();
    }

    public List<Entry> getRelatedEntries(Entry entry, int size) {
        return this.entryDao.getRelatedEntries(entry, size);
    }

    public List<RequestCounter> getRequestList(Entry entry, int size) {
        return this.requestCounterDao.getRequestList(entry, size);
    }

    public List<RequestCounter> getRequestListByUri(String uri, int size) {
        return this.requestCounterDao.getRequestListByUri(uri, size);
    }

    public List<Entry> getPages(String entryStatus) {
        return this.entryDao.getPages(entryStatus);
    }

    @SuppressWarnings("unchecked")
    public List<Entry> getPublishedPages() {
        if (cache.get(Constants.PAGES_CACHE_KEY) != null) {
            if (log.isDebugEnabled()) {
                log.debug("[myblog]:loading cached published pages..");
            }
            return (List<Entry>) cache.get(Constants.PAGES_CACHE_KEY);
        }
        List<Entry> pages = getPages(Entry.EntryStatus.PUBLISH);
        cache.put(Constants.PAGES_CACHE_KEY, pages);
        return pages;
    }

    /**
	 * clear the recent entries cache.
	 * 
	 * @author <a href="mailto:rory.cn@gmail.com">somebody</a>
	 */
    private void clearRecentEntriesCache() {
        cache.put(Constants.RECENT_ENTRIES_CACHE_KEY, null);
        if (log.isDebugEnabled()) {
            log.debug("[myblog]:clear cached recent entries....");
        }
    }

    /**
	 * clear the pages cache.
	 * 
	 * @author <a href="mailto:rory.cn@gmail.com">somebody</a>
	 */
    private void clearPagesCache() {
        cache.put(Constants.PAGES_CACHE_KEY, null);
        if (log.isDebugEnabled()) {
            log.debug("[myblog]:clear cached pages....");
        }
    }

    /**
	 * clear the categories cache.
	 * 
	 * @author <a href="mailto:rory.cn@gmail.com">somebody</a>
	 */
    private void clearCategoriesCache() {
        cache.put(Constants.CATEGORIES_CACHE_KEY, null);
        if (log.isDebugEnabled()) {
            log.debug("[myblog]:clear cached categories...");
        }
    }

    /**
	 * clear the recent comments cache.
	 * 
	 * @author <a href="mailto:rory.cn@gmail.com">somebody</a>
	 */
    private void clearRecentCommentsCache() {
        cache.put(Constants.RECENT_COMMENTS_CACHE_KEY, null);
        if (log.isDebugEnabled()) {
            log.debug("[myblog]:clear cached recent comments...");
        }
    }

    /**
	 * clear the siteconfig cache.
	 * 
	 * @author <a href="mailto:rory.cn@gmail.com">somebody</a>
	 */
    private void clearSiteConfigCache() {
        cache.put(Constants.SITECONFIG_CACHE_KEY, null);
        if (log.isDebugEnabled()) {
            log.debug("[myblog]:clear cached siteconfig...");
        }
    }

    /**
	 * clear links cache.
	 * 
	 * @author <a href="mailto:rory.cn@gmail.com">somebody</a>
	 */
    private void clearLinksCache() {
        cache.put(Constants.LINKS_CACHE_KEY, null);
        if (log.isDebugEnabled()) {
            log.debug("[myblog]:clear cached links...");
        }
    }

    /**
	 * clear recommend links cache.
	 * 
	 * @author <a href="mailto:rory.cn@gmail.com">somebody</a>
	 */
    private void clearRecommendLinksCache() {
        cache.put(Constants.RECOMMEND_LINKS_CACHE_KEY, null);
        if (log.isDebugEnabled()) {
            log.debug("[myblog]:clear recommend cached links...");
        }
    }

    public List<String> getSubscribeEntryCommentEmails(String entryId) {
        return this.commentDao.getSubscribeEntryCommentEmails(entryId);
    }

    @SuppressWarnings("unchecked")
    public List<Date> getMonthList() {
        if (cache.get(Constants.ARCHIVE_MONTH_LIST_CACHE_KEY) != null) {
            log.debug("[myblog]:loading cached month list..");
            return (List<Date>) cache.get(Constants.ARCHIVE_MONTH_LIST_CACHE_KEY);
        }
        List<Date> monthList = this.entryDao.getMonthList();
        return monthList;
    }

    /**
	 * clear the month list cache..
	 * 
	 * @author <a href="mailto:rory.cn@gmail.com">somebody</a>
	 */
    private void clearMonthListCache() {
        cache.put(Constants.ARCHIVE_MONTH_LIST_CACHE_KEY, null);
        log.debug("[myblog]:clear month list cache...");
    }

    @SuppressWarnings("unchecked")
    public List<Tag> getHotTags(int size) {
        if (cache.get(Constants.HOT_TAGS_CACHE_KEY) != null) {
            log.debug("[myblog]:loading cached hot tags...");
            return (List<Tag>) cache.get(Constants.HOT_TAGS_CACHE_KEY);
        }
        List<Tag> tags = this.tagDao.getHotTags(size);
        cache.put(Constants.HOT_TAGS_CACHE_KEY, tags);
        return tags;
    }

    private void clearHotTagsCache() {
        cache.put(Constants.HOT_TAGS_CACHE_KEY, null);
        log.debug("[myblog]:clean hot tags cache..");
    }
}
