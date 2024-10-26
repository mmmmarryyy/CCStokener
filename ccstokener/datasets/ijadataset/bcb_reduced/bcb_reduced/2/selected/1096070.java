package org.dreamhost.ide.welcome.ui;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JToolTip;
import org.dreamhost.ide.welcome.content.BundleSupport;
import org.dreamhost.ide.welcome.content.Constants;
import org.dreamhost.ide.welcome.content.RSSFeed;
import org.dreamhost.ide.welcome.content.RSSFeedReaderPanel;
import org.dreamhost.ide.welcome.content.Utils;
import org.dreamhost.ide.welcome.content.WebLink;
import org.openide.awt.Mnemonics;
import org.openide.awt.StatusDisplayer;
import org.openide.util.ImageUtilities;

/**
 *
 * @author S. Aubrecht
 */
class DemoPanel extends RSSFeedReaderPanel {

    public DemoPanel() {
        super(BundleSupport.getURL("Demo"));
    }

    @Override
    protected JComponent buildContent(String url, boolean showProxyButton) {
        JPanel res = new JPanel(new GridBagLayout());
        res.setOpaque(false);
        DemoRSSFeed feed = new DemoRSSFeed(url);
        res.add(feed, new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
        res.add(buildBottomContent(), new GridBagConstraints(0, 1, 1, 1, 0.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        res.add(new JLabel(), new GridBagConstraints(0, 2, 1, 1, 0.0, 1.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
        return res;
    }

    protected JComponent buildBottomContent() {
        WebLink allBlogs = new WebLink("AllDemos", false);
        BundleSupport.setAccessibilityProperties(allBlogs, "AllDemos");
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        panel.add(allBlogs, new GridBagConstraints(1, 0, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHEAST, GridBagConstraints.HORIZONTAL, new Insets(5, 5, 0, 5), 0, 0));
        panel.add(new JLabel(), new GridBagConstraints(0, 0, 1, 1, 1.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));
        return panel;
    }

    class DemoRSSFeed extends RSSFeed {

        public DemoRSSFeed(String url) {
            super(url, false);
        }

        @Override
        protected Component createFeedItemComponent(FeedItem item) {
            JPanel panel = new JPanel(new GridBagLayout());
            panel.setOpaque(false);
            int row = 0;
            if (item.isValid()) {
                WebLink linkButton = new WebLink(item.title, item.link, false);
                linkButton.getAccessibleContext().setAccessibleName(BundleSupport.getAccessibilityName("WebLink", item.title));
                linkButton.getAccessibleContext().setAccessibleDescription(BundleSupport.getAccessibilityDescription("WebLink", item.link));
                linkButton.setFont(BUTTON_FONT);
                panel.add(linkButton, new GridBagConstraints(0, row++, 1, 1, 0.0, 0.0, GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 5, 2, TEXT_INSETS_RIGHT), 0, 0));
                if (item.enclosureUrl != null) {
                    panel.add(new ImageLabel(item.link, getImage(item.enclosureUrl), item.description), new GridBagConstraints(0, row++, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(10, 5, 5, 5), 0, 0));
                } else {
                    JLabel label = new JLabel(BundleSupport.getLabel("NoScreenShot"));
                    label.setHorizontalAlignment(JLabel.CENTER);
                    label.setVerticalAlignment(JLabel.CENTER);
                    panel.add(label, new GridBagConstraints(0, row++, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(0, TEXT_INSETS_LEFT + 5, 0, TEXT_INSETS_RIGHT), 0, 0));
                }
            } else {
                panel.add(new JLabel(BundleSupport.getLabel("ErrLoadingFeed")), new GridBagConstraints(0, row++, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 10, 10, 5), 0, 0));
                JButton button = new JButton();
                Mnemonics.setLocalizedText(button, BundleSupport.getLabel("Reload"));
                button.setOpaque(false);
                button.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        lastReload = 0;
                        reload();
                    }
                });
                panel.add(button, new GridBagConstraints(0, row++, 1, 1, 0.0, 0.0, GridBagConstraints.CENTER, GridBagConstraints.NONE, new Insets(5, 10, 10, 5), 0, 0));
            }
            return panel;
        }

        @Override
        protected int getMaxItemCount() {
            return 1;
        }

        ImageIcon getImage(String urlString) {
            URL url = null;
            try {
                url = new URL(urlString);
            } catch (MalformedURLException mfuE) {
                mfuE.printStackTrace();
            }
            ImageIcon image = null;
            if (isContentCached()) {
                ObjectInputStream input = null;
                try {
                    input = new ObjectInputStream(new FileInputStream(getCacheFilePath()));
                    image = (ImageIcon) input.readObject();
                    Logger.getLogger(DemoPanel.class.getName()).log(Level.FINE, "Demo image loaded from: " + getCacheFilePath());
                } catch (Exception e) {
                    image = null;
                } finally {
                    if (null != input) try {
                        input.close();
                    } catch (IOException e) {
                    }
                }
            }
            if (null == image) {
                ObjectOutputStream output = null;
                try {
                    URLConnection conn = url.openConnection();
                    boolean defCache = conn.getDefaultUseCaches();
                    conn.setDefaultUseCaches(true);
                    image = new ImageIcon(url);
                    conn.setDefaultUseCaches(defCache);
                    output = new ObjectOutputStream(new FileOutputStream(getCacheFilePath()));
                    output.writeObject(image);
                } catch (Exception e) {
                    Logger.getLogger(DemoPanel.class.getName()).log(Level.FINE, "Error while caching Welcome Page demo image", e);
                    image = ImageUtilities.loadImageIcon(Constants.BROKEN_IMAGE, false);
                } finally {
                    if (null != output) {
                        try {
                            output.close();
                        } catch (IOException e) {
                        }
                    }
                }
            }
            return image;
        }

        private File getCacheFilePath() throws IOException {
            File cacheStore = Utils.getCacheStore();
            cacheStore = new File(cacheStore, "demoimage");
            cacheStore.getParentFile().mkdirs();
            cacheStore.createNewFile();
            return cacheStore;
        }
    }

    private static class ImageLabel extends JLabel implements Constants, MouseListener {

        private String url;

        private boolean visited = false;

        public ImageLabel(String url, ImageIcon img, String description) {
            super(new MaxSizeImageIcon(img.getImage()));
            this.url = url;
            if (null != description) setToolTipText("<html>" + description);
            setOpaque(false);
            setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
            addMouseListener(this);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        public void mouseClicked(MouseEvent e) {
            if (!e.isPopupTrigger()) {
                visited = true;
                Utils.showURL(url);
                mouseEntered(null);
            }
        }

        public void mousePressed(MouseEvent e) {
        }

        public void mouseReleased(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
            Color borderColor = Utils.getColor(visited ? VISITED_LINK_COLOR : MOUSE_OVER_LINK_COLOR);
            setBorder(BorderFactory.createLineBorder(borderColor, 1));
            StatusDisplayer.getDefault().setStatusText(url);
        }

        public void mouseExited(MouseEvent e) {
            setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
            StatusDisplayer.getDefault().setStatusText("");
        }

        @Override
        public JToolTip createToolTip() {
            JToolTip tip = super.createToolTip();
            JLabel lbl = new JLabel(getToolTipText());
            Dimension preferredSize = lbl.getPreferredSize();
            FontMetrics fm = tip.getFontMetrics(tip.getFont());
            int lines = preferredSize.width / 500;
            if (preferredSize.width % 500 > 0) lines++;
            preferredSize.height = Math.min(lines * fm.getHeight() + 10, 300);
            preferredSize.width = 500;
            tip.setPreferredSize(preferredSize);
            return tip;
        }
    }

    private static class MaxSizeImageIcon implements Icon, Constants {

        private static final int MAX_IMAGE_WIDTH = 202;

        private static final int MAX_IMAGE_HEIGHT = 142;

        private Image content;

        private Image frame;

        public MaxSizeImageIcon(Image content) {
            this.content = content;
            frame = ImageUtilities.loadImage(IMAGE_PICTURE_FRAME);
        }

        public void paintIcon(Component c, Graphics g, int x, int y) {
            try {
                int imgX = x;
                int imgY = y;
                if (content.getWidth(null) > MAX_IMAGE_WIDTH) imgX += (content.getWidth(null) - MAX_IMAGE_WIDTH) / 2;
                if (content.getHeight(null) > MAX_IMAGE_HEIGHT) imgY += (content.getHeight(null) - MAX_IMAGE_HEIGHT) / 2;
                g.drawImage(content, x, y, x + Math.min(MAX_IMAGE_WIDTH, content.getWidth(null)), y + Math.min(MAX_IMAGE_HEIGHT, content.getHeight(null)), imgX, imgY, imgX + Math.min(MAX_IMAGE_WIDTH, content.getWidth(null)), imgY + Math.min(MAX_IMAGE_HEIGHT, content.getHeight(null)), null);
                g.drawImage(frame, x, y, c);
            } catch (ThreadDeath td) {
                throw td;
            } catch (Throwable e) {
                Logger.getLogger(DemoPanel.class.getName()).log(Level.FINE, "Error while painting demo image.", e);
            }
        }

        public int getIconWidth() {
            return MAX_IMAGE_WIDTH;
        }

        public int getIconHeight() {
            return MAX_IMAGE_HEIGHT;
        }
    }
}
