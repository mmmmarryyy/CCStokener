package uit.upis.action;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import com.esri.aims.mtier.model.envelope.Envelope;
import com.thoughtworks.xstream.XStream;
import uit.server.model.LayerDef;
import uit.server.model.Model;
import uit.server.model.RequestMapObject;
import uit.server.model.ResponseMapObject;
import uit.server.util.RequestMapHelper;
import uit.server.util.ScaleUtil;
import uit.comm.Constants;
import uit.comm.util.RequestUtil;
import uit.comm.util.StringUtil;
import uit.upis.manager.MapManager;

public class MapAction extends BaseAction {

    private MapManager mapManager;

    /**
	 * @return Returns the mapManager.
	 */
    public MapManager getMapManager() {
        return mapManager;
    }

    /**
	 * @param mapManager The mapManager to set.
	 */
    public void setMapManager(MapManager mapManager) {
        this.mapManager = mapManager;
    }

    /**
	 * ����ȭ��
	 * @param mapping
	 * @param form
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
    public ActionForward main(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        if (isLogin(request)) {
            HttpSession session = request.getSession();
            List layerList = (List) session.getAttribute(Constants.LAYER_LIST);
            Model model = new Model();
            model.setTempLayer(layerList);
            model = mapManager.getMap(model);
            request.setAttribute("model", model);
            request.setAttribute("imagePath", model.getUrl());
            return mapping.findForward("success");
        } else return mapping.findForward("loginForm");
    }

    /**
	 * ��ü����, �̵�, Ȯ��, ���, �ʱ�ȭ, ���ΰ�ħ ��ȸ
	 * �̹��� ������ �����´�. 
	 * @param mapping
	 * @param form
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
    public ActionForward getImageMap(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.debug("MapAction getImageMap()");
        RequestUtil.showRequestParam(request);
        String xml = "";
        try {
            xml = mapManager.doMap(request);
        } catch (Exception e) {
            e.printStackTrace();
        }
        ajaxResponse(response, xml);
        return null;
    }

    /**
	 * ������ȸ
	 * @param mapping
	 * @param form
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
    public ActionForward getIdentify(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.debug("MapAction getIdentify()");
        RequestUtil.showRequestParam(request);
        String x = RequestUtil.getRequest(request, "pointX");
        String y = RequestUtil.getRequest(request, "pointY");
        String sx = RequestUtil.getRequest(request, "screenX");
        String sy = RequestUtil.getRequest(request, "screenY");
        System.out.println(sx + " , " + sy);
        mapManager.identify(Double.parseDouble(x), Double.parseDouble(y));
        return null;
    }

    /**
	 * ����, ���� �� ������ȸ
	 * @param mapping
	 * @param form
	 * @param request
	 * @param response
	 * @return ActionForward
	 * @throws Exception
	 */
    public ActionForward getHistMap(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.debug("MapAction getHistMap()");
        String historyMode = RequestUtil.getRequest(request, "mode", "P");
        int index = RequestUtil.getIntegerRequest(request, "mapIndex", 0);
        RequestMapHelper helper = new RequestMapHelper();
        RequestMapObject req = helper.setRequestMapObject(request);
        System.out.println(index);
        Envelope envelope = helper.getMapHistory(request.getSession(), index);
        ResponseMapObject mapObject = null;
        try {
            System.out.println(index + "request");
            System.out.println(envelope.getMaxX() + " aaa ");
            mapObject = mapManager.doZoom(req.getVisibleLayerList(), req.getWidth(), req.getHeight(), envelope);
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        ajaxResponse(response, helper.generateMapObjToXML(request, mapObject));
        return null;
    }

    /**
	 * ���� ���� ��ȸ
	 * @param mapping
	 * @param form
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
    public ActionForward getPartition(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.debug("MapAction getPartition()");
        return null;
    }

    /**
	 * �������� ���� ��ȸ
	 * @param mapping
	 * @param form
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception
	 */
    public ActionForward getAdminDist(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.debug("MapAction getAdminDist()");
        return null;
    }

    /**
	 * �̹����������� �� ����. 
	 */
    public ActionForward saveImageMap(ActionMapping mapping, ActionForm form, HttpServletRequest request, HttpServletResponse response) throws Exception {
        log.debug("MapAction saveImageMap()");
        String imageURL = RequestUtil.getRequest(request, "imgUrl");
        DataInputStream di = null;
        FileOutputStream fo = null;
        byte[] b = new byte[1];
        URL url = new URL(imageURL);
        URLConnection urlConnection = url.openConnection();
        urlConnection.connect();
        response.setContentType("application/octet-stream");
        response.setHeader("Content-disposition", "attachment;filename=" + "map.png");
        OutputStream outstream = response.getOutputStream();
        byte abyte0[] = new byte[4096];
        try {
            BufferedInputStream instream = new BufferedInputStream(urlConnection.getInputStream());
            int i;
            while ((i = instream.read(abyte0, 0, 4096)) != -1) outstream.write(abyte0, 0, i);
            instream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
