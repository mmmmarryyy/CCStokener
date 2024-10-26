package org.npsnet.v.gui;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.swing.*;
import org.npsnet.v.services.gui.ContentPanel;
import org.npsnet.v.services.gui.ContentPanelContext;
import org.npsnet.v.services.gui.ScrollableContentPanelContext;
import org.npsnet.v.services.gui.UnsupportedContextException;

/**
 * The content panel class for image content.
 *
 * @author Andrzej Kapolka
 */
public class ImageContentPanel extends ContentPanel {

    /**
     * The owner of this panel.
     */
    private StandardContentPanelProvider owner;

    /**
     * The image to display.
     */
    private Image image;

    /**
     * Whether or not the image is still loading.
     */
    private boolean loading;

    /**
     * The image panel component.
     */
    private JPanel imagePanel;

    /**
     * The scroll pane component.
     */
    private JScrollPane scrollPane;

    /**
     * Represents a context of this content panel.
     */
    public class ImageContentPanelContext extends ScrollableContentPanelContext {

        /**
         * Constructor.
         *
         * @param pSource the originating content panel
         * @param pURL the URL
         * @param pHorizontalScrollBarValue the horizontal scroll bar value
         * @param pHorizontalScrollBarExtent the horizontal scroll bar extent
         * @param pHorizontalScrollBarMinimum the horizontal scroll bar minimum
         * @param pHorizontalScrollBarMaximum the horizontal scroll bar maximum
         * @param pVerticalScrollBarValue the vertical scroll bar value
         * @param pVerticalScrollBarExtent the vertical scroll bar extent
         * @param pVerticalScrollBarMinimum the vertical scroll bar minimum
         * @param pVerticalScrollBarMaximum the vertical scroll bar maximum
         */
        public ImageContentPanelContext(ContentPanel pSource, URL pURL, int pHorizontalScrollBarValue, int pHorizontalScrollBarExtent, int pHorizontalScrollBarMinimum, int pHorizontalScrollBarMaximum, int pVerticalScrollBarValue, int pVerticalScrollBarExtent, int pVerticalScrollBarMinimum, int pVerticalScrollBarMaximum) {
            super(pSource, pURL, pHorizontalScrollBarValue, pHorizontalScrollBarExtent, pHorizontalScrollBarMinimum, pHorizontalScrollBarMaximum, pVerticalScrollBarValue, pVerticalScrollBarExtent, pVerticalScrollBarMinimum, pVerticalScrollBarMaximum);
        }
    }

    /**
     * Constructor.
     *
     * @param pOwner the standard content panel provider that spawned
     * this panel
     */
    public ImageContentPanel(StandardContentPanelProvider pOwner) {
        super(new BorderLayout());
        owner = pOwner;
        imagePanel = new JPanel() {

            public void paintComponent(Graphics g) {
                super.paintComponent(g);
                if (image != null) {
                    g.drawImage(image, 0, 0, ImageContentPanel.this);
                }
            }

            public Dimension getPreferredSize() {
                if (image != null) {
                    return new Dimension(image.getWidth(this), image.getHeight(this));
                } else {
                    return super.getPreferredSize();
                }
            }

            public boolean imageUpdate(Image img, int flags, int x, int y, int width, int height) {
                if ((flags & ImageObserver.HEIGHT) != 0) {
                    if (ImageContentPanel.super.getContext() instanceof ScrollableContentPanelContext) {
                        ScrollableContentPanelContext scpc = (ScrollableContentPanelContext) ImageContentPanel.super.getContext();
                        scrollPane.getHorizontalScrollBar().setValues(scpc.getHorizontalScrollBarValue(), scpc.getHorizontalScrollBarExtent(), scpc.getHorizontalScrollBarMinimum(), scpc.getHorizontalScrollBarMaximum());
                        scrollPane.getVerticalScrollBar().setValues(scpc.getVerticalScrollBarValue(), scpc.getVerticalScrollBarExtent(), scpc.getVerticalScrollBarMinimum(), scpc.getVerticalScrollBarMaximum());
                    }
                    scrollPane.doLayout();
                }
                return super.imageUpdate(img, flags, x, y, width, height);
            }
        };
        imagePanel.setBackground(Color.WHITE);
        scrollPane = new JScrollPane(imagePanel);
        add(scrollPane, BorderLayout.CENTER);
    }

    /**
     * Sets the context of this content panel, notifying all listeners that
     * the context has changed.
     *
     * @param newContext the new context
     * @exception UnsupportedContextException if the type of the
     * specified context is unsupported
     */
    public void setContext(ContentPanelContext newContext) throws UnsupportedContextException {
        try {
            URLConnection urlc = newContext.getURL().openConnection();
            String type = urlc.getContentType();
            if (type == null || !type.startsWith("image")) {
                throw new UnsupportedContextException();
            }
        } catch (IOException ioe) {
            throw new UnsupportedContextException();
        }
        image = getToolkit().createImage(newContext.getURL());
        loading = true;
        if (getHost() != null) {
            getHost().contentPanelStartedLoading(this);
        }
        getToolkit().prepareImage(image, -1, -1, this);
        imagePanel.repaint();
        scrollPane.doLayout();
        super.setContext(newContext);
    }

    /**
     * Returns this content panel's current context.
     *
     * @return the current context
     */
    public ContentPanelContext getContext() {
        return new ImageContentPanelContext(this, super.getContext().getURL(), scrollPane.getHorizontalScrollBar().getValue(), scrollPane.getHorizontalScrollBar().getModel().getExtent(), scrollPane.getHorizontalScrollBar().getMinimum(), scrollPane.getHorizontalScrollBar().getMaximum(), scrollPane.getVerticalScrollBar().getValue(), scrollPane.getVerticalScrollBar().getModel().getExtent(), scrollPane.getVerticalScrollBar().getMinimum(), scrollPane.getVerticalScrollBar().getMaximum());
    }

    /**
     * Checks whether this content panel is loading content in another
     * thread.  Default implementation simply returns <code>false</code>.
     *
     * @return <code>true</code> if this content panel is loading content
     * in another thread, <code>false</code> otherwise
     */
    public boolean isLoading() {
        return loading;
    }

    /**
     * Causes this content panel to stop loading content.  Default
     * implementation does nothing.
     */
    public void stopLoading() {
        loading = false;
    }

    /**
     * Provides information concerning an image being loaded.
     *
     * @param img the image being loaded
     * @param flags a set of flags indicating the image's status
     * @param x the x coordinate
     * @param y the y coordinate
     * @param width the width
     * @param height the height
     * @return <code>true</code> to continue loading, <code>false</code>
     * otherwise
     */
    public boolean imageUpdate(Image img, int flags, int x, int y, int width, int height) {
        imagePanel.imageUpdate(img, flags, x, y, width, height);
        if ((flags & ImageObserver.ALLBITS) != 0) {
            loading = false;
        }
        if (!loading && getHost() != null) {
            getHost().contentPanelStoppedLoading(this);
        }
        return loading;
    }
}
