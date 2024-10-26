package org.adempiere.webui.desktop;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.URL;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.logging.Level;
import org.adempiere.webui.EnvWeb;
import org.adempiere.webui.component.Tabpanel;
import org.adempiere.webui.component.ToolBarButton;
import org.adempiere.webui.dashboard.DashboardPanel;
import org.adempiere.webui.dashboard.DashboardRunnable;
import org.adempiere.webui.event.MenuListener;
import org.adempiere.webui.panel.HeaderPanel;
import org.adempiere.webui.panel.SidePanel;
import org.adempiere.webui.util.IServerPushCallback;
import org.adempiere.webui.util.ServerPushTemplate;
import org.adempiere.webui.util.Util;
import org.compiere.model.MMenu;
import org.compiere.util.CLogger;
import org.compiere.util.DB;
import org.compiere.util.Msg;
import org.zkoss.zk.ui.Component;
import org.zkoss.zk.ui.Executions;
import org.zkoss.zk.ui.Page;
import org.zkoss.zk.ui.event.Event;
import org.zkoss.zk.ui.event.EventListener;
import org.zkoss.zk.ui.event.Events;
import org.zkoss.zkex.zul.Borderlayout;
import org.zkoss.zkex.zul.Center;
import org.zkoss.zkex.zul.North;
import org.zkoss.zkex.zul.West;
import org.zkoss.zkmax.zul.Portalchildren;
import org.zkoss.zkmax.zul.Portallayout;
import org.zkoss.zul.Html;
import org.zkoss.zul.Panel;
import org.zkoss.zul.Panelchildren;

/**
 * 
 * Default desktop implementation.
 * @author <a href="mailto:agramdass@gmail.com">Ashley G Ramdass</a>
 * @author <a href="mailto:hengsin@gmail.com">Low Heng Sin</a>
 * @date Mar 2, 2007
 * @version $Revision: 0.10 $
 */
public class DefaultDesktopEnhanced extends TabbedDesktop implements MenuListener, Serializable, EventListener, IServerPushCallback {

    private static final long serialVersionUID = 9056511175189603883L;

    private static final CLogger logger = CLogger.getCLogger(DefaultDesktopEnhanced.class);

    private Center windowArea;

    private Borderlayout layout;

    private Thread dashboardThread;

    private DashboardRunnable dashboardRunnable;

    private int noOfNotice;

    private int noOfRequest;

    private int noOfWorkflow;

    public DefaultDesktopEnhanced() {
        super();
    }

    protected Component doCreatePart(Component parent) {
        SidePanel pnlSide = new SidePanel();
        HeaderPanel pnlHead = new HeaderPanel();
        pnlSide.getMenuPanel().addMenuListener(this);
        layout = new Borderlayout();
        if (parent != null) {
            layout.setParent(parent);
            layout.setWidth("100%");
            layout.setHeight("100%");
            layout.setStyle("position: absolute");
        } else layout.setPage(page);
        dashboardRunnable = new DashboardRunnable(layout.getDesktop(), this);
        North n = new North();
        layout.appendChild(n);
        n.setCollapsible(false);
        pnlHead.setParent(n);
        West w = new West();
        layout.appendChild(w);
        w.setWidth("300px");
        w.setCollapsible(true);
        w.setSplittable(true);
        w.setTitle("Menu");
        w.setFlex(true);
        pnlSide.setParent(w);
        windowArea = new Center();
        windowArea.setParent(layout);
        windowArea.setFlex(true);
        windowContainer.createPart(windowArea);
        createHomeTab();
        return layout;
    }

    private void createHomeTab() {
        Tabpanel homeTab = new Tabpanel();
        windowContainer.addWindow(homeTab, Msg.getMsg(EnvWeb.getCtx(), "Home").replaceAll("&", ""), false);
        Portallayout portalLayout = new Portallayout();
        portalLayout.setWidth("100%");
        portalLayout.setHeight("100%");
        portalLayout.setStyle("position: absolute; overflow: auto");
        homeTab.appendChild(portalLayout);
        Portalchildren portalchildren = null;
        int currentColumnNo = 0;
        String sql = "SELECT COUNT(DISTINCT COLUMNNO) " + "FROM PA_DASHBOARDCONTENT " + "WHERE (AD_CLIENT_ID=0 OR AD_CLIENT_ID=?) AND ISACTIVE='Y'";
        int noOfCols = DB.getSQLValue(null, sql, EnvWeb.getCtx().getAD_Client_ID());
        int width = noOfCols <= 0 ? 100 : 100 / noOfCols;
        sql = "SELECT x.*, m.AD_MENU_ID " + "FROM PA_DASHBOARDCONTENT x " + "LEFT OUTER JOIN AD_MENU m ON x.AD_WINDOW_ID=m.AD_WINDOW_ID " + "WHERE (x.AD_CLIENT_ID=0 OR x.AD_CLIENT_ID=?) AND x.ISACTIVE='Y' " + "ORDER BY x.COLUMNNO, x.AD_CLIENT_ID, x.LINE ";
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try {
            pstmt = DB.prepareStatement(sql, null);
            pstmt.setInt(1, EnvWeb.getCtx().getAD_Client_ID());
            rs = pstmt.executeQuery();
            while (rs.next()) {
                int columnNo = rs.getInt("ColumnNo");
                if (portalchildren == null || currentColumnNo != columnNo) {
                    portalchildren = new Portalchildren();
                    portalLayout.appendChild(portalchildren);
                    portalchildren.setWidth(width + "%");
                    portalchildren.setStyle("padding: 5px");
                    currentColumnNo = columnNo;
                }
                Panel panel = new Panel();
                panel.setStyle("margin-bottom:10px");
                panel.setTitle(rs.getString("Name"));
                String description = rs.getString("Description");
                if (description != null) panel.setTooltiptext(description);
                String collapsible = rs.getString("IsCollapsible");
                panel.setCollapsible(collapsible.equals("Y"));
                panel.setBorder("normal");
                portalchildren.appendChild(panel);
                Panelchildren content = new Panelchildren();
                panel.appendChild(content);
                boolean panelEmpty = true;
                String htmlContent = rs.getString("HTML");
                if (htmlContent != null) {
                    StringBuffer result = new StringBuffer("<html><head>");
                    URL url = getClass().getClassLoader().getResource("org/compiere/images/PAPanel.css");
                    InputStreamReader ins;
                    try {
                        ins = new InputStreamReader(url.openStream());
                        BufferedReader bufferedReader = new BufferedReader(ins);
                        String cssLine;
                        while ((cssLine = bufferedReader.readLine()) != null) result.append(cssLine + "\n");
                    } catch (IOException e1) {
                        logger.log(Level.SEVERE, e1.getLocalizedMessage(), e1);
                    }
                    result.append("</head><body><div class=\"content\">\n");
                    result.append(stripHtml(htmlContent, false) + "<br>\n");
                    result.append("</div>\n</body>\n</html>\n</html>");
                    Html html = new Html();
                    html.setContent(result.toString());
                    content.appendChild(html);
                    panelEmpty = false;
                }
                int AD_Window_ID = rs.getInt("AD_Window_ID");
                if (AD_Window_ID > 0) {
                    int AD_Menu_ID = rs.getInt("AD_Menu_ID");
                    ToolBarButton btn = new ToolBarButton(String.valueOf(AD_Menu_ID));
                    MMenu menu = new MMenu(EnvWeb.getCtx(), AD_Menu_ID, null);
                    btn.setLabel(menu.getName());
                    btn.addEventListener(Events.ON_CLICK, this);
                    content.appendChild(btn);
                    panelEmpty = false;
                }
                int PA_Goal_ID = rs.getInt("PA_Goal_ID");
                if (PA_Goal_ID > 0) {
                    StringBuffer result = new StringBuffer("<html><head>");
                    URL url = getClass().getClassLoader().getResource("org/compiere/images/PAPanel.css");
                    InputStreamReader ins;
                    try {
                        ins = new InputStreamReader(url.openStream());
                        BufferedReader bufferedReader = new BufferedReader(ins);
                        String cssLine;
                        while ((cssLine = bufferedReader.readLine()) != null) result.append(cssLine + "\n");
                    } catch (IOException e1) {
                        logger.log(Level.SEVERE, e1.getLocalizedMessage(), e1);
                    }
                    result.append("</head><body><div class=\"content\">\n");
                    result.append(renderGoals(PA_Goal_ID, content));
                    result.append("</div>\n</body>\n</html>\n</html>");
                    Html html = new Html();
                    html.setContent(result.toString());
                    content.appendChild(html);
                    panelEmpty = false;
                }
                String url = rs.getString("ZulFilePath");
                if (url != null) {
                    try {
                        Component component = Executions.createComponents(url, content, null);
                        if (component != null) {
                            if (component instanceof DashboardPanel) {
                                DashboardPanel dashboardPanel = (DashboardPanel) component;
                                if (!dashboardPanel.getChildren().isEmpty()) {
                                    content.appendChild(dashboardPanel);
                                    dashboardRunnable.add(dashboardPanel);
                                    panelEmpty = false;
                                }
                            } else {
                                content.appendChild(component);
                                panelEmpty = false;
                            }
                        }
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Failed to create components. zul=" + url, e);
                    }
                }
                if (panelEmpty) panel.detach();
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to create dashboard content", e);
        } finally {
            Util.closeCursor(pstmt, rs);
        }
        registerWindow(homeTab);
        if (!portalLayout.getDesktop().isServerPushEnabled()) portalLayout.getDesktop().enableServerPush(true);
        dashboardRunnable.refreshDashboard();
        dashboardThread = new Thread(dashboardRunnable, "UpdateInfo");
        dashboardThread.setDaemon(true);
        dashboardThread.start();
    }

    public void onEvent(Event event) {
        Component comp = event.getTarget();
        String eventName = event.getName();
        if (eventName.equals(Events.ON_CLICK)) {
            if (comp instanceof ToolBarButton) {
                ToolBarButton btn = (ToolBarButton) comp;
                int menuId = 0;
                try {
                    menuId = Integer.valueOf(btn.getName());
                } catch (Exception e) {
                }
                if (menuId > 0) onMenuSelected(menuId);
            }
        }
    }

    public void onServerPush(ServerPushTemplate template) {
        template.execute(this);
    }

    /**
	 * 
	 * @param page
	 */
    public void setPage(Page page) {
        if (this.page != page) {
            layout.setPage(page);
            this.page = page;
        }
    }

    /**
	 * Get the root component
	 * @return Component
	 */
    public Component getComponent() {
        return layout;
    }

    public void logout() {
        if (dashboardThread != null && dashboardThread.isAlive()) {
            dashboardRunnable.stop();
            dashboardThread.interrupt();
        }
    }

    public void updateUI() {
        int total = noOfNotice + noOfRequest + noOfWorkflow;
        windowContainer.setTabTitle(0, "Home (" + total + ")", "Notice : " + noOfNotice + ", Request : " + noOfRequest + ", Workflow Activities : " + noOfWorkflow);
    }
}
