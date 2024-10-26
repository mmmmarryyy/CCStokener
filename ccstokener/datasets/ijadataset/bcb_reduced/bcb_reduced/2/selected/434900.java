package org.chartsy.main.welcome;

import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.Random;
import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.event.MouseInputAdapter;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.client.CookieStore;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.chartsy.main.managers.ProxyManager;
import org.chartsy.main.util.DesktopUtil;
import org.chartsy.main.util.NotifyUtil;
import org.openide.awt.StatusDisplayer;
import org.openide.util.NbBundle;

/**
 *
 * @author Viorel
 */
public class BottomContent extends JPanel {

    private String imageURL;

    private String bannerURL;

    private ImageIcon bannerImg;

    private JLabel bannerLbl;

    private MouseInputAdapter mouseInputAdapter;

    private CookieStore cookieStore = new BasicCookieStore();

    public BottomContent() {
        initComponents();
    }

    private void initComponents() {
        setLayout(new FlowLayout(FlowLayout.CENTER));
        setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
        setOpaque(false);
        initBanner();
        mouseInputAdapter = new BannerMouseHandler();
        bannerLbl = new JLabel();
        bannerLbl.setOpaque(false);
        bannerLbl.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        bannerLbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        bannerLbl.setHorizontalAlignment(SwingConstants.CENTER);
        bannerLbl.setVerticalAlignment(SwingConstants.CENTER);
        bannerLbl.setIcon(bannerImg);
        bannerLbl.addMouseListener(mouseInputAdapter);
        bannerLbl.addMouseMotionListener(mouseInputAdapter);
        add(bannerLbl);
    }

    private void initBanner() {
        for (int k = 0; k < 3; k++) {
            if (bannerImg == null) {
                int i = getRandomId();
                imageURL = NbBundle.getMessage(BottomContent.class, "URL_BannerImageLink", Integer.toString(i));
                bannerURL = NbBundle.getMessage(BottomContent.class, "URL_BannerLink", Integer.toString(i));
                HttpContext context = new BasicHttpContext();
                context.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
                HttpGet method = new HttpGet(imageURL);
                try {
                    HttpResponse response = ProxyManager.httpClient.execute(method, context);
                    HttpEntity entity = response.getEntity();
                    if (entity != null) {
                        bannerImg = new ImageIcon(ImageIO.read(entity.getContent()));
                        EntityUtils.consume(entity);
                    }
                } catch (IOException ex) {
                    bannerImg = null;
                } finally {
                    method.abort();
                }
            } else {
                break;
            }
        }
        if (bannerImg == null) {
            NotifyUtil.error("Banner Error", "Application could not get banner image. Please check your internet connection.", false);
        }
    }

    private int getRandomId() {
        Random random = new Random();
        int n = 1000;
        int rand = random.nextInt(n);
        return Math.abs(rand);
    }

    private class BannerMouseHandler extends MouseInputAdapter {

        public void mousePressed(MouseEvent e) {
            bannerLbl.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            HttpContext context = new BasicHttpContext();
            context.setAttribute(ClientContext.COOKIE_STORE, cookieStore);
            HttpGet method = new HttpGet(bannerURL);
            try {
                HttpResponse response = ProxyManager.httpClient.execute(method, context);
                HttpEntity entity = response.getEntity();
                HttpHost host = (HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
                HttpUriRequest request = (HttpUriRequest) context.getAttribute(ExecutionContext.HTTP_REQUEST);
                String targetURL = host.toURI() + request.getURI();
                DesktopUtil.browseAndWarn(targetURL, bannerLbl);
                EntityUtils.consume(entity);
            } catch (Exception ex) {
                NotifyUtil.error("Banner Error", "Could not open the default web browser.", ex, false);
            } finally {
                method.abort();
            }
            bannerLbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        public void mouseEntered(MouseEvent e) {
            StatusDisplayer.getDefault().setStatusText(bannerURL);
        }

        public void mouseExited(MouseEvent e) {
            StatusDisplayer.getDefault().setStatusText("");
        }
    }
}
