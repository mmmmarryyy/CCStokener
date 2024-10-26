package GUI.composites;

import java.io.InputStream;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Iterator;
import java.util.List;
import javax.xml.datatype.Duration;
import org.apache.log4j.Logger;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.ImageLoader;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import GUI.CompositeLoaderDialog;
import GUI.ListenerScriptColumn;
import GUI.cache.ImageCache;
import GUI.notifier.NotifierDialog;
import bean.Prodotto;
import com.ebay.soap.eBLBaseComponents.AmountType;
import com.ebay.soap.eBLBaseComponents.ItemType;
import com.ebay.soap.eBLBaseComponents.PictureDetailsType;
import core.ApplicationCore;

/**
 * Pannello di visualizzazione delle ASTE Ebay
 * Mostra una tabella preceduta da una serie di filtri di visualizzazione
 * 
 * @author pegoraro
 * 
 *  [SVN] Last commit: $Author: donacot $ 
 *  [SVN] URL: $URL: https://donatoguy.svn.sourceforge.net/svnroot/donatoguy/src/GUI/composites/CercaProdottoComposite.java $
 * 
 * @version [SVN] $Rev: 194 $
 * @since 0.1.132
 *
 */
public class CercaAstaComposite extends Composite {

    private static Table table;

    private Text textCodice;

    private Text textTitolo;

    private Text textLocation;

    private Text textMarca;

    private Text textModello;

    private Text textCategoria;

    private static Logger log = Logger.getLogger(OpzioniComposite.class.getName());

    private static Image icon = ImageCache.getImage("080587-glossy-black-comment-bubble-icon-business-magnifying-glass-ps.png");

    private static Image iconBrowseTo = ImageCache.getImage("080545-glossy-black-comment-bubble-icon-business-globe.png");

    private CompositeLoaderDialog load;

    /**
	 * Crea il composite di ricerca
	 * 
	 * @param parent
	 * @param style
	 */
    public CercaAstaComposite(final Composite parent, int style, final ApplicationCore core) {
        super(parent, style);
        setLayout(new FormLayout());
        load = new CompositeLoaderDialog(core, parent.getShell(), style);
        final CompositeLoaderDialog loader = load;
        final Group filtriDiRicercaGroup = new Group(this, SWT.NONE);
        filtriDiRicercaGroup.setLayout(new FormLayout());
        final FormData fd_filtriDiRicercaGroup = new FormData();
        fd_filtriDiRicercaGroup.bottom = new FormAttachment(0, 165);
        fd_filtriDiRicercaGroup.right = new FormAttachment(100, -10);
        fd_filtriDiRicercaGroup.top = new FormAttachment(0, 0);
        fd_filtriDiRicercaGroup.left = new FormAttachment(0, 10);
        filtriDiRicercaGroup.setLayoutData(fd_filtriDiRicercaGroup);
        filtriDiRicercaGroup.setText("Filtri di Ricerca");
        table = new Table(this, SWT.BORDER);
        final FormData fd_table = new FormData();
        fd_table.top = new FormAttachment(filtriDiRicercaGroup, 6);
        fd_table.right = new FormAttachment(filtriDiRicercaGroup, 0, SWT.RIGHT);
        fd_table.bottom = new FormAttachment(100, -10);
        fd_table.left = new FormAttachment(filtriDiRicercaGroup, 0, SWT.LEFT);
        table.setLayoutData(fd_table);
        table.setLinesVisible(true);
        table.setHeaderVisible(true);
        textCodice = new Text(filtriDiRicercaGroup, SWT.BORDER);
        final FormData fd_text = new FormData();
        fd_text.bottom = new FormAttachment(0, 27);
        fd_text.top = new FormAttachment(0, 6);
        fd_text.right = new FormAttachment(0, 200);
        fd_text.left = new FormAttachment(0, 91);
        textCodice.setText("<Mostra Tutti>");
        textCodice.setLayoutData(fd_text);
        Label lblCodice = new Label(filtriDiRicercaGroup, SWT.NONE);
        final FormData fd_lblCodice = new FormData();
        fd_lblCodice.bottom = new FormAttachment(0, 24);
        fd_lblCodice.top = new FormAttachment(0, 9);
        fd_lblCodice.right = new FormAttachment(0, 72);
        fd_lblCodice.left = new FormAttachment(0, 17);
        lblCodice.setLayoutData(fd_lblCodice);
        lblCodice.setText("Codice");
        Label lblBarcode = new Label(filtriDiRicercaGroup, SWT.NONE);
        final FormData fd_lblBarcode = new FormData();
        fd_lblBarcode.bottom = new FormAttachment(0, 21);
        fd_lblBarcode.top = new FormAttachment(0, 6);
        fd_lblBarcode.right = new FormAttachment(0, 303);
        fd_lblBarcode.left = new FormAttachment(0, 248);
        lblBarcode.setLayoutData(fd_lblBarcode);
        lblBarcode.setText("Barcode");
        textTitolo = new Text(filtriDiRicercaGroup, SWT.BORDER);
        final FormData fd_text_1 = new FormData();
        fd_text_1.right = new FormAttachment(100, -86);
        fd_text_1.bottom = new FormAttachment(0, 27);
        fd_text_1.top = new FormAttachment(0, 6);
        fd_text_1.left = new FormAttachment(0, 309);
        textTitolo.setLayoutData(fd_text_1);
        textLocation = new Text(filtriDiRicercaGroup, SWT.BORDER);
        final FormData fd_text_2 = new FormData();
        fd_text_2.right = new FormAttachment(100, -86);
        fd_text_2.bottom = new FormAttachment(0, 54);
        fd_text_2.top = new FormAttachment(0, 33);
        fd_text_2.left = new FormAttachment(0, 91);
        textLocation.setText("<Mostra Tutti>");
        textLocation.setLayoutData(fd_text_2);
        Label lblDescrizione = new Label(filtriDiRicercaGroup, SWT.NONE);
        final FormData fd_lblDescrizione = new FormData();
        fd_lblDescrizione.bottom = new FormAttachment(0, 51);
        fd_lblDescrizione.top = new FormAttachment(0, 36);
        fd_lblDescrizione.right = new FormAttachment(0, 85);
        fd_lblDescrizione.left = new FormAttachment(0, 17);
        lblDescrizione.setLayoutData(fd_lblDescrizione);
        lblDescrizione.setText("Descrizione");
        Label lblFornitore = new Label(filtriDiRicercaGroup, SWT.NONE);
        final FormData fd_lblFornitore = new FormData();
        fd_lblFornitore.bottom = new FormAttachment(0, 78);
        fd_lblFornitore.top = new FormAttachment(0, 63);
        fd_lblFornitore.right = new FormAttachment(0, 72);
        fd_lblFornitore.left = new FormAttachment(0, 17);
        lblFornitore.setLayoutData(fd_lblFornitore);
        lblFornitore.setText("Fornitore");
        Combo combo = new Combo(filtriDiRicercaGroup, SWT.NONE);
        final FormData fd_combo = new FormData();
        fd_combo.right = new FormAttachment(100, -234);
        fd_combo.bottom = new FormAttachment(0, 81);
        fd_combo.top = new FormAttachment(0, 60);
        fd_combo.left = new FormAttachment(0, 91);
        combo.setLayoutData(fd_combo);
        Label lblMarca = new Label(filtriDiRicercaGroup, SWT.NONE);
        final FormData fd_lblMarca = new FormData();
        fd_lblMarca.bottom = new FormAttachment(0, 104);
        fd_lblMarca.top = new FormAttachment(0, 89);
        fd_lblMarca.right = new FormAttachment(0, 72);
        fd_lblMarca.left = new FormAttachment(0, 17);
        lblMarca.setLayoutData(fd_lblMarca);
        lblMarca.setText("Marca");
        textMarca = new Text(filtriDiRicercaGroup, SWT.BORDER);
        final FormData fd_text_3 = new FormData();
        fd_text_3.bottom = new FormAttachment(0, 110);
        fd_text_3.top = new FormAttachment(0, 89);
        fd_text_3.right = new FormAttachment(0, 200);
        fd_text_3.left = new FormAttachment(0, 91);
        textMarca.setText("<Mostra Tutti>");
        textMarca.setLayoutData(fd_text_3);
        textCategoria = new Text(filtriDiRicercaGroup, SWT.BORDER);
        final FormData fd_text_8 = new FormData();
        fd_text_8.right = new FormAttachment(100, -86);
        fd_text_8.bottom = new FormAttachment(0, 110);
        fd_text_8.top = new FormAttachment(0, 89);
        fd_text_8.left = new FormAttachment(0, 309);
        textCategoria.setLayoutData(fd_text_8);
        Label lblFamcat = new Label(filtriDiRicercaGroup, SWT.NONE);
        final FormData fd_lblFamcat = new FormData();
        fd_lblFamcat.bottom = new FormAttachment(0, 104);
        fd_lblFamcat.top = new FormAttachment(0, 89);
        fd_lblFamcat.right = new FormAttachment(0, 303);
        fd_lblFamcat.left = new FormAttachment(0, 248);
        lblFamcat.setLayoutData(fd_lblFamcat);
        lblFamcat.setText("Fam/Cat");
        try {
            List<ItemType> found = core.getEbayHandle().getAste();
            refreshAsteTable(found);
        } catch (Exception e) {
            log.error("Impossibile aggiornare la tabella dei prodotti:", e);
        }
        ModifyListener searcher = new ModifyListener() {

            public void modifyText(ModifyEvent arg0) {
                try {
                    Prodotto pivot = new Prodotto();
                    if (textMarca.getText().compareTo("<Mostra Tutti>") != 0) pivot.setMarca(textMarca.getText());
                    if (textCodice.getText().compareTo("<Mostra Tutti>") != 0) pivot.setCodice(textCodice.getText());
                    if (textModello.getText().compareTo("<Mostra Tutti>") != 0) pivot.setModello(textModello.getText());
                    if (textLocation.getText().compareTo("<Mostra Tutti>") != 0) pivot.setDescrizione(textLocation.getText());
                    log.error("TOIMPLEMENT");
                } catch (Exception e) {
                    log.error("Impossibile aggiornare la tabella dei prodotti:", e);
                }
            }
        };
        textMarca.addModifyListener(searcher);
        textCodice.addModifyListener(searcher);
        textLocation.addModifyListener(searcher);
        table.addListener(SWT.MouseDoubleClick, new Listener() {

            public void handleEvent(Event event) {
                Rectangle clientArea = table.getClientArea();
                Point pt = new Point(event.x, event.y);
                int index = table.getTopIndex();
                while (index < table.getItemCount()) {
                    boolean visible = false;
                    TableItem item = table.getItem(index);
                    ItemType ebayitem = (ItemType) item.getData();
                    for (int i = 0; i < table.getColumnCount(); i++) {
                        Rectangle rect = item.getBounds(i);
                        if (rect.contains(pt)) {
                            if (i == 0) {
                                final Shell prev = new Shell(table.getShell(), SWT.NO_FOCUS | SWT.NO_TRIM);
                                Image biggerprev = loadPreview(ebayitem);
                                prev.setBackgroundImage(biggerprev);
                                prev.setSize(biggerprev.getBounds().width, biggerprev.getBounds().height);
                                prev.open();
                                prev.addListener(SWT.MouseDown, new Listener() {

                                    public void handleEvent(Event event) {
                                        NotifierDialog.fadeOut(prev);
                                    }
                                });
                                return;
                            } else if (i == 1) {
                                Shell brow = new Shell(table.getShell());
                                brow.setLayout(new FillLayout());
                                Browser browser = new Browser(brow, SWT.NONE);
                                browser.setUrl(ebayitem.getListingDetails().getViewItemURL());
                                brow.open();
                                return;
                            }
                            System.out.println("Item " + index + "-" + i);
                        }
                        if (!visible && rect.intersects(clientArea)) {
                            visible = true;
                        }
                    }
                    if (!visible) return;
                    index++;
                }
            }
        });
    }

    /**
	 * Aggiorna la tabella dei prodotti, svuotandola e 
	 * ri-popolandola con l'input
	 * 
	 * @param prods List dei prodotti da visualizzare
	 */
    public static void refreshAsteTable(List<ItemType> prods) {
        table.removeAll();
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy 'alle' hh:mm");
        Iterator<ItemType> it = prods.iterator();
        TableColumn iconTableColumn = null;
        if (table.getColumnCount() < 2) {
            iconTableColumn = new TableColumn(table, SWT.NONE);
            iconTableColumn.setWidth(36);
            iconTableColumn.setText("");
            iconTableColumn.setToolTipText("Fare doppio click su questa colonna per ingrandire l'anteprima");
            final TableColumn expTableColumn = new TableColumn(table, SWT.NONE);
            expTableColumn.setWidth(36);
            expTableColumn.setText("Link");
            expTableColumn.setToolTipText("Fare doppio click su questa colonna per aprire un browser alla pagina dell'oggetto corrispondente");
            final TableColumn codiceTableColumn = new TableColumn(table, SWT.NONE);
            codiceTableColumn.setWidth(60);
            codiceTableColumn.setText("ItemID");
            codiceTableColumn.addSelectionListener(new ListenerScriptColumn(table, ListenerScriptColumn.STRING_COMPARATOR));
            final TableColumn categoriaTableColumn = new TableColumn(table, SWT.NONE);
            categoriaTableColumn.setWidth(120);
            categoriaTableColumn.setText("Titolo");
            categoriaTableColumn.addSelectionListener(new ListenerScriptColumn(table, ListenerScriptColumn.STRING_COMPARATOR));
            final TableColumn newCategoriaTableColumn = new TableColumn(table, SWT.NONE);
            newCategoriaTableColumn.setWidth(100);
            newCategoriaTableColumn.setText("Categoria");
            newCategoriaTableColumn.addSelectionListener(new ListenerScriptColumn(table, ListenerScriptColumn.STRING_COMPARATOR));
            final TableColumn newColumnTableColumn = new TableColumn(table, SWT.NONE);
            newColumnTableColumn.setWidth(100);
            newColumnTableColumn.setText("Dove si Trova");
            newColumnTableColumn.addSelectionListener(new ListenerScriptColumn(table, ListenerScriptColumn.STRING_COMPARATOR));
            final TableColumn ModelloColumnTableColumn = new TableColumn(table, SWT.NONE);
            ModelloColumnTableColumn.setWidth(100);
            ModelloColumnTableColumn.setText("Tempo Rimasto");
            ModelloColumnTableColumn.addSelectionListener(new ListenerScriptColumn(table, ListenerScriptColumn.STRING_COMPARATOR));
            final TableColumn newColumnTableColumnCat = new TableColumn(table, SWT.NONE);
            newColumnTableColumnCat.setWidth(250);
            newColumnTableColumnCat.setText("Prezzo");
            newColumnTableColumnCat.addSelectionListener(new ListenerScriptColumn(table, ListenerScriptColumn.STRING_COMPARATOR));
            final TableColumn newColumnTableColumnPrezzo = new TableColumn(table, SWT.NONE);
            newColumnTableColumnPrezzo.setWidth(80);
            newColumnTableColumnPrezzo.setText("Link");
            newColumnTableColumnPrezzo.addSelectionListener(new ListenerScriptColumn(table, ListenerScriptColumn.INT_COMPARATOR));
        }
        int rowcnt = 0;
        while (it.hasNext()) {
            ItemType type = (ItemType) it.next();
            int c = 0;
            final TableItem newItemTableItem = new TableItem(table, SWT.BORDER);
            newItemTableItem.setData(type);
            Image preview = loadPreview(type);
            if (preview != null) {
                newItemTableItem.setImage(c++, ImageCache.resize(preview, 32, 32));
            } else newItemTableItem.setImage(c++, icon);
            newItemTableItem.setImage(c++, iconBrowseTo);
            newItemTableItem.setText(c++, type.getItemID());
            newItemTableItem.setText(c++, type.getTitle());
            newItemTableItem.setText(c++, type.getPrimaryCategory().getCategoryName());
            newItemTableItem.setText(c++, type.getLocation());
            Duration timeleft = type.getTimeLeft();
            newItemTableItem.setText(c++, timeleft.getDays() + "g " + timeleft.getHours() + "h");
            AmountType cost = type.getSellingStatus().getCurrentPrice();
            newItemTableItem.setText(c++, cost.getValue() + cost.getCurrencyID().value());
            log.debug("TROVATO:" + type.getCurrency().toString() + type.getTimeLeft());
            rowcnt++;
        }
        if (rowcnt == 0) log.warn("Nessun Asta Trovata!");
    }

    static Image loadPreview(ItemType it) {
        PictureDetailsType pics = it.getPictureDetails();
        String urlname = pics.getGalleryURL();
        try {
            URL url = new URL(urlname);
            log.info("PROXY?" + System.getProperty("http.proxyHost"));
            InputStream stream = url.openStream();
            ImageLoader loader = new ImageLoader();
            ImageData[] gal = loader.load(stream);
            return new Image(Display.getCurrent(), gal[0]);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    protected void checkSubclass() {
    }
}
