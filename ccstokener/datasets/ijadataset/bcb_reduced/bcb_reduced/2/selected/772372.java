package leeon.mobile.BBSBrowser.sjtu;

import java.util.Iterator;
import java.util.List;
import leeon.mobile.BBSBrowser.IDocAction;
import leeon.mobile.BBSBrowser.NetworkException;
import leeon.mobile.BBSBrowser.actions.HttpConfig;
import leeon.mobile.BBSBrowser.models.BoardObject;
import leeon.mobile.BBSBrowser.models.DocObject;
import leeon.mobile.BBSBrowser.utils.HTTPUtil;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;

public class BBSSjtuDocAction implements IDocAction {

    @Override
    public List<DocObject> topTenDocList() throws NetworkException {
        return null;
    }

    /**
	 * 处理最后一页的重复数据
	 * @throws NetworkException 
	 */
    List<DocObject> dealFirstPage(List<DocObject> list, int startFromIndex, int total, boolean isTitle) throws NetworkException {
        if (total <= SjtuHttpConfig.BBS_PAGE_SIZE) return list;
        Iterator<DocObject> i = list.iterator();
        int stickyCount = 0;
        while (i.hasNext() && startFromIndex-- > 0) {
            DocObject o = i.next();
            if (o.isSticky()) {
                stickyCount++;
                continue;
            }
            i.remove();
        }
        if (list.size() < SjtuHttpConfig.BBS_PAGE_SIZE && list.size() > 0) {
            List<DocObject> l = boardDoc(list.get(0).getBoard(), isTitle, list.size(), stickyCount);
            l.addAll(list);
            return l;
        }
        return list;
    }

    /**
	 * 计算分页
	 */
    static int calculatePage(int total) {
        return (int) (total / SjtuHttpConfig.BBS_PAGE_SIZE) + (total % SjtuHttpConfig.BBS_PAGE_SIZE > 0 ? 1 : 0);
    }

    @Override
    public List<DocObject> boardDoc(BoardObject board, boolean isTitle) throws NetworkException {
        return boardDoc(board, isTitle, 0, 0);
    }

    @Override
    public List<DocObject> boardDoc(BoardObject board, boolean isTitle, int totalCount, int stickyCount) throws NetworkException {
        String url = SjtuHttpConfig.BBS_URL + (isTitle ? SjtuHttpConfig.BBS_DOC_TITLE : SjtuHttpConfig.BBS_DOC) + board.getName();
        if (totalCount != 0) {
            int currentFetchedPage = calculatePage(totalCount - stickyCount);
            int totalPage = calculatePage(board.getTotal());
            int willFetchedPage = totalPage - currentFetchedPage - 1;
            url += "&" + SjtuHttpConfig.BBS_DOC_PAGE_PARAM_NAME + "=" + willFetchedPage;
        }
        String html = getUrlHtmlContent(url);
        if (totalCount != 0) return BBSSjtuBodyParseHelper.parseBoardDoc(board, html, isTitle); else {
            board.setTotal(0);
            return dealFirstPage(BBSSjtuBodyParseHelper.parseBoardDoc(board, html, isTitle), SjtuHttpConfig.BBS_PAGE_SIZE - board.getTotal() % SjtuHttpConfig.BBS_PAGE_SIZE, board.getTotal(), isTitle);
        }
    }

    @Override
    public List<DocObject> docContent(DocObject doc, boolean isTitle) throws NetworkException {
        String url = SjtuHttpConfig.BBS_URL + (isTitle ? SjtuHttpConfig.BBS_CON_TITLE : SjtuHttpConfig.BBS_CON) + doc.getBoard().getName();
        url += "&" + (isTitle ? SjtuHttpConfig.BBS_CON_TITLE_PARAM_NAME : SjtuHttpConfig.BBS_CON_FILE_PARAM_NAME) + "=" + doc.getId();
        url += isTitle ? SjtuHttpConfig.BBS_CON_TITLE_SHOWALL : "";
        String html = getUrlHtmlContent(url);
        return BBSSjtuBodyParseHelper.parseDocContent(doc, html, isTitle);
    }

    @Override
    public List<DocObject> docContent(DocObject doc, boolean isTitle, String tag) throws NetworkException {
        return null;
    }

    @Override
    public List<DocObject> docContent(DocObject doc, boolean isTitle, DocObject gdoc) throws NetworkException {
        return null;
    }

    static String getUrlHtmlContent(String url) throws NetworkException {
        HttpClient client = HttpConfig.newInstance();
        HttpGet get = new HttpGet(url);
        get.addHeader("User-Agent", "Java/1.6.0_20");
        try {
            HttpResponse response = client.execute(get);
            HttpEntity entity = response.getEntity();
            return HTTPUtil.toString(entity, BBSSjtuBodyParseHelper.BBS_CHARSET);
        } catch (Exception e) {
            throw new NetworkException(e);
        }
    }

    public static void main(String[] args) throws Exception {
        BoardObject b = new BoardObject("PPPerson", "PPPerson", "美丽人物");
        DocObject d = new DocObject("1323448339", null, null, null, null, b);
        List<DocObject> list = new BBSSjtuDocAction().docContent(d, true);
        System.out.println(list);
    }
}
