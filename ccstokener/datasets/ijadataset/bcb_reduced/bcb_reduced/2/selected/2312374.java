package es.eucm.eadventure.editor.control.controllers.book;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JEditorPane;
import javax.swing.text.BadLocationException;
import es.eucm.eadventure.common.auxiliar.AssetsConstants;
import es.eucm.eadventure.common.auxiliar.File;
import es.eucm.eadventure.common.data.chapter.book.BookPage;
import es.eucm.eadventure.common.gui.TC;
import es.eucm.eadventure.editor.control.Controller;
import es.eucm.eadventure.editor.control.controllers.AssetsController;
import es.eucm.eadventure.editor.control.controllers.Searchable;
import es.eucm.eadventure.editor.control.tools.books.AddBookPageTool;
import es.eucm.eadventure.editor.control.tools.books.ChangeBookPageMarginsTool;
import es.eucm.eadventure.editor.control.tools.books.ChangeBookPageScrollableTool;
import es.eucm.eadventure.editor.control.tools.books.ChangeBookPageTypeTool;
import es.eucm.eadventure.editor.control.tools.books.ChangeBookPageUriTool;
import es.eucm.eadventure.editor.control.tools.books.DeleteBookPageTool;
import es.eucm.eadventure.editor.control.tools.books.MoveBookPageDownTool;
import es.eucm.eadventure.editor.control.tools.books.MoveBookPageUpTool;
import es.eucm.eadventure.editor.gui.assetchooser.AssetChooser;
import es.eucm.eadventure.editor.gui.otherpanels.bookpanels.BookPagePreviewPanel;

public class BookPagesListDataControl extends Searchable {

    /**
     * List of book paragraphs.
     */
    private List<BookPage> bookPagesList;

    private int selectedPage;

    private boolean defaultScrollable;

    private int defaultMargin;

    private int defaultType;

    private BookDataControl dControl;

    /**
     * Constructor.
     * 
     * @param bookParagraphsList
     *            List of book paragraphs
     */
    public BookPagesListDataControl(List<BookPage> bookPagesList, BookDataControl dControl) {
        this.bookPagesList = bookPagesList;
        this.dControl = dControl;
        selectedPage = -1;
        defaultType = BookPage.TYPE_RESOURCE;
        defaultMargin = 0;
        defaultScrollable = false;
    }

    /**
     * Return the list of book paragraph controllers.
     * 
     * @return Book paragraph controllers
     */
    public List<BookPage> getBookPages() {
        return bookPagesList;
    }

    public BookPage addPage() {
        BookPage newBookPage = new BookPage("", defaultType, defaultMargin, defaultScrollable);
        Controller.getInstance().addTool(new AddBookPageTool(bookPagesList, newBookPage, selectedPage));
        selectedPage = bookPagesList.indexOf(newBookPage);
        return newBookPage;
    }

    public boolean deletePage(BookPage page) {
        Controller.getInstance().addTool(new DeleteBookPageTool(bookPagesList, page));
        return true;
    }

    public boolean movePageUp(BookPage page) {
        Controller.getInstance().addTool(new MoveBookPageUpTool(bookPagesList, page));
        return true;
    }

    public boolean movePageDown(BookPage page) {
        Controller.getInstance().addTool(new MoveBookPageDownTool(bookPagesList, page));
        return true;
    }

    public int countAssetReferences(String assetPath) {
        int count = 0;
        for (BookPage bookPage : bookPagesList) if (bookPage.getUri().equals(assetPath) && (bookPage.getType() == BookPage.TYPE_RESOURCE || bookPage.getType() == BookPage.TYPE_IMAGE)) count++;
        return count;
    }

    public void getAssetReferences(List<String> assetPaths, List<Integer> assetTypes) {
        for (BookPage bookPage : bookPagesList) {
            String uri = bookPage.getUri();
            if (uri != null && !uri.equals("") && (bookPage.getType() == BookPage.TYPE_RESOURCE || bookPage.getType() == BookPage.TYPE_IMAGE)) {
                boolean add = true;
                for (String asset : assetPaths) {
                    if (asset.equals(uri)) {
                        add = false;
                        break;
                    }
                }
                if (add) {
                    int last = assetPaths.size();
                    assetPaths.add(last, uri);
                    if (bookPage.getType() == BookPage.TYPE_RESOURCE) assetTypes.add(last, AssetsConstants.CATEGORY_STYLED_TEXT); else if (bookPage.getType() == BookPage.TYPE_IMAGE) assetTypes.add(last, AssetsConstants.CATEGORY_IMAGE);
                }
            }
        }
    }

    public void deleteAssetReferences(String assetPath) {
        ArrayList<BookPage> toRemove = new ArrayList<BookPage>();
        for (BookPage bookPage : bookPagesList) if (bookPage.getUri().equals(assetPath) && (bookPage.getType() == BookPage.TYPE_RESOURCE || bookPage.getType() == BookPage.TYPE_IMAGE)) toRemove.add(bookPage);
        for (BookPage bookPage : toRemove) bookPagesList.remove(bookPage);
    }

    public BookPage changeCurrentPage(int page) {
        BookPage currentPage = null;
        if (page >= 0 && page < bookPagesList.size()) {
            currentPage = bookPagesList.get(page);
            this.selectedPage = page;
        }
        return currentPage;
    }

    public boolean editStyledTextAssetPath() {
        String selectedAsset = null;
        if (selectedPage < 0 || selectedPage >= bookPagesList.size()) return false;
        AssetChooser chooser = AssetsController.getAssetChooser(AssetsConstants.CATEGORY_STYLED_TEXT, AssetsController.FILTER_NONE);
        int option = chooser.showAssetChooser(Controller.getInstance().peekWindow());
        if (option == AssetChooser.ASSET_FROM_ZIP) {
            selectedAsset = chooser.getSelectedAsset();
        } else if (option == AssetChooser.ASSET_FROM_OUTSIDE) {
            boolean added = AssetsController.addSingleAsset(AssetsConstants.CATEGORY_STYLED_TEXT, chooser.getSelectedFile().getAbsolutePath());
            String filePath = chooser.getSelectedFile().getAbsolutePath();
            String filesFolderPath = filePath.substring(0, filePath.lastIndexOf(".")) + "_files";
            File filesFolder = new File(filesFolderPath);
            if (filesFolder.exists() && filesFolder.isDirectory()) {
                added &= AssetsController.addSingleAsset(AssetsConstants.CATEGORY_STYLED_TEXT, filesFolderPath);
            }
            if (added) {
                selectedAsset = chooser.getSelectedFile().getName();
            }
        }
        if (selectedAsset != null) {
            String[] assetFilenames = AssetsController.getAssetFilenames(AssetsConstants.CATEGORY_STYLED_TEXT);
            String[] assetPaths = AssetsController.getAssetsList(AssetsConstants.CATEGORY_STYLED_TEXT);
            int assetIndex = -1;
            for (int i = 0; i < assetFilenames.length; i++) if (assetFilenames[i].equals(selectedAsset)) assetIndex = i;
            Controller.getInstance().addTool(new ChangeBookPageUriTool(bookPagesList.get(selectedPage), assetPaths[assetIndex]));
            return true;
        } else return false;
    }

    public boolean editImageAssetPath() {
        String selectedAsset = null;
        if (selectedPage < 0 || selectedPage >= bookPagesList.size()) return false;
        AssetChooser chooser = AssetsController.getAssetChooser(AssetsConstants.CATEGORY_IMAGE, AssetsController.FILTER_NONE);
        int option = chooser.showAssetChooser(Controller.getInstance().peekWindow());
        if (option == AssetChooser.ASSET_FROM_ZIP) {
            selectedAsset = chooser.getSelectedAsset();
        } else if (option == AssetChooser.ASSET_FROM_OUTSIDE) {
            boolean added = AssetsController.addSingleAsset(AssetsConstants.CATEGORY_IMAGE, chooser.getSelectedFile().getAbsolutePath());
            String filePath = chooser.getSelectedFile().getAbsolutePath();
            String filesFolderPath = filePath.substring(0, filePath.lastIndexOf(".")) + "_files";
            File filesFolder = new File(filesFolderPath);
            if (filesFolder.exists() && filesFolder.isDirectory()) {
                added &= AssetsController.addSingleAsset(AssetsConstants.CATEGORY_IMAGE, filesFolderPath);
            }
            if (added) {
                selectedAsset = chooser.getSelectedFile().getName();
            }
        }
        if (selectedAsset != null) {
            String[] assetFilenames = AssetsController.getAssetFilenames(AssetsConstants.CATEGORY_IMAGE);
            String[] assetPaths = AssetsController.getAssetsList(AssetsConstants.CATEGORY_IMAGE);
            int assetIndex = -1;
            for (int i = 0; i < assetFilenames.length; i++) if (assetFilenames[i].equals(selectedAsset)) assetIndex = i;
            Controller.getInstance().addTool(new ChangeBookPageUriTool(bookPagesList.get(selectedPage), assetPaths[assetIndex]));
            return true;
        } else return false;
    }

    public boolean editURL(String newURL) {
        if (selectedPage >= 0 && selectedPage < bookPagesList.size() && bookPagesList.get(selectedPage).getType() == BookPage.TYPE_URL) {
            Controller.getInstance().addTool(new ChangeBookPageUriTool(bookPagesList.get(selectedPage), newURL));
            return true;
        }
        return false;
    }

    public boolean setType(int newType) {
        boolean typeSet = false;
        if (selectedPage >= 0 && selectedPage < bookPagesList.size() && newType != bookPagesList.get(selectedPage).getType()) {
            Controller.getInstance().addTool(new ChangeBookPageTypeTool(bookPagesList.get(selectedPage), newType));
            typeSet = true;
        }
        return typeSet;
    }

    public void setMargins(int newMargin, int newMarginTop, int newMarginBottom, int newMarginEnd) {
        if (selectedPage >= 0 && selectedPage < bookPagesList.size()) {
            Controller.getInstance().addTool(new ChangeBookPageMarginsTool(bookPagesList.get(selectedPage), newMargin, newMarginTop, newMarginBottom, newMarginEnd));
        }
    }

    public void setScrollable(boolean scrollable) {
        if (selectedPage >= 0 && selectedPage < bookPagesList.size()) {
            Controller.getInstance().addTool(new ChangeBookPageScrollableTool(bookPagesList.get(selectedPage), scrollable));
        }
    }

    public boolean isValidPage(BookPage page) {
        boolean isValid = false;
        try {
            if (page.getType() == BookPage.TYPE_RESOURCE) {
                BookPagePreviewPanel panel = new BookPagePreviewPanel(dControl, true);
                panel.setCurrentBookPage(page);
                isValid = !page.getUri().equals("") && panel.isValid();
            } else if (page.getType() == BookPage.TYPE_URL) {
                URL url = new URL(page.getUri());
                url.openStream().close();
                isValid = true;
            } else if (page.getType() == BookPage.TYPE_IMAGE) {
                if (page.getUri().length() > 0) isValid = true;
            }
        } catch (Exception e) {
            isValid = false;
        }
        return isValid;
    }

    public boolean isValidPage(int page) {
        return isValidPage(bookPagesList.get(page));
    }

    public boolean isValid(String currentPath, List<String> incidences) {
        boolean valid = true;
        for (int i = 0; i < bookPagesList.size(); i++) {
            String bookParagraphPath = currentPath + " >> " + TC.get("Element.BookPage") + " #" + (i + 1);
            boolean isPageValid = true;
            if (bookPagesList.get(i).getType() == BookPage.TYPE_RESOURCE && bookPagesList.get(i).getUri().length() == 0) {
                isPageValid = false;
                if (incidences != null) incidences.add(bookParagraphPath + " >> " + TC.get("Operation.AdventureConsistencyErrorBookPage"));
            }
            valid &= isPageValid;
        }
        return valid;
    }

    public BookPage getSelectedPage() {
        if (selectedPage >= 0 && selectedPage < bookPagesList.size()) return bookPagesList.get(selectedPage); else return null;
    }

    public int getIndexSelectedPage() {
        return selectedPage;
    }

    public BookPage getLastPage() {
        return bookPagesList.get(bookPagesList.size() - 1);
    }

    @Override
    public void recursiveSearch() {
        for (int i = 0; i < bookPagesList.size(); i++) {
            if (bookPagesList.get(i).getType() != BookPage.TYPE_URL) {
                JEditorPane editor = new JEditorPane();
                try {
                    editor.setPage(AssetsController.getResourceAsURLFromZip(bookPagesList.get(i).getUri()));
                    check(editor.getDocument().getText(0, editor.getDocument().getLength()), TC.get("Search.HTMLBookPage"));
                } catch (MalformedURLException e1) {
                } catch (IOException e1) {
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected List<Searchable> getPath(Searchable dataControl) {
        if (dataControl == this) {
            List<Searchable> path = new ArrayList<Searchable>();
            path.add(this);
            return path;
        }
        return null;
    }
}
