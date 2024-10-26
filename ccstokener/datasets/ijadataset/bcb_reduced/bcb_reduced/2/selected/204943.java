package be.kuleuven.cw.peno3.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Date;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.IOUtils;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import be.kuleuven.VTKfakbarCWA1.data.WebserviceAdressContainer;
import be.kuleuven.VTKfakbarCWA1.model.product.Product;
import be.kuleuven.VTKfakbarCWA1.model.product.StockItem;

public class StockDAO {

    private static StockDAO singletonStockDAO;

    private static final String BASEURL = WebserviceAdressContainer.getInstance().getBASEURL();

    private static ProductDAO mProductDAO;

    public static synchronized StockDAO getSingletonStockManager() {
        if (singletonStockDAO == null) {
            singletonStockDAO = new StockDAO();
        }
        return singletonStockDAO;
    }

    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    public static StockItem searchStock(Product product) {
        if (product != null && product.getID() > 0) {
            try {
                int id = product.getID();
                String url = BASEURL + "StockDAO/getStock?prodtype_id=" + id;
                System.out.println(url);
                String json = stringOfUrl(url);
                System.out.println(json);
                List<StockItem> stockitems = new ArrayList<StockItem>();
                stockitems = mapStockItemsToList(json);
                StockItem stock = countStock(stockitems, product);
                printStockItem(stock);
                return stock;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (JsonParseException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private static StockItem searchStockEldest(Product product) {
        if (product != null && product.getID() > 0) {
            try {
                int id = product.getID();
                String url = BASEURL + "StockDAO/getStock?prodtype_id=" + id;
                System.out.println(url);
                String json = stringOfUrl(url);
                System.out.println(json);
                List<StockItem> stockitems = new ArrayList<StockItem>();
                stockitems = mapStockItemsToList(json);
                if (stockitems.isEmpty() != true) {
                    StockItem stock = getEldestItem(stockitems, product);
                    printStockItem(stock);
                    return stock;
                }
                return null;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (JsonParseException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    private static StockItem countStockEldest(List<StockItem> stockitems, Product product) {
        BigDecimal total = new BigDecimal("0");
        for (StockItem stockitem : stockitems) {
            String s = stockitem.getProduct().getName();
            if (s.equals(product.getName()) == true) {
                total = total.add(stockitem.getCount());
            }
        }
        StockItem result = getEldestItem(stockitems, product);
        if (result.getDate() != null) {
            result.setCount(total);
        }
        return result;
    }

    private static StockItem countStock(List<StockItem> stockitems, Product product) {
        BigDecimal total = new BigDecimal("0");
        for (StockItem stockitem : stockitems) {
            String s = stockitem.getProduct().getName();
            if (s.equals(product.getName()) == true) {
                total = total.add(stockitem.getCount());
            }
        }
        StockItem result = getNewestItem(stockitems, product);
        if (result.getDate() != null) {
            result.setCount(total);
        }
        return result;
    }

    private static StockItem getEldestItem(List<StockItem> stockitems, Product product) {
        StockItem eldest = stockitems.get(0);
        for (StockItem item : stockitems) {
            if ((item.getProduct().getName().equals(product.getName())) && (eldest.getDate() == null)) {
                eldest = item;
            } else if (item.getProduct().getName().equals(product.getName())) {
                Date n = eldest.getDate();
                Date i = item.getDate();
                if (n.getYear() > i.getYear()) {
                    eldest = item;
                } else if (n.getYear() == i.getYear()) {
                    if (n.getMonth() > i.getMonth()) {
                        eldest = item;
                    } else if (n.getMonth() == i.getMonth() && n.getDate() > i.getDate()) {
                        eldest = item;
                    }
                }
            }
        }
        return eldest;
    }

    private static StockItem getNewestItem(List<StockItem> stockitems, Product product) {
        StockItem newest = new StockItem();
        for (StockItem item : stockitems) {
            if ((item.getProduct().getName().equals(product.getName())) && (newest.getDate() == null)) {
                newest = item;
            } else if (item.getProduct().getName().equals(product.getName())) {
                Date n = newest.getDate();
                Date i = item.getDate();
                if (n.getYear() < i.getYear()) {
                    newest = item;
                } else if (n.getYear() == i.getYear()) {
                    if (n.getMonth() < i.getMonth()) {
                        newest = item;
                    } else if (n.getMonth() == i.getMonth() && n.getDate() < i.getDate()) {
                        newest = item;
                    }
                }
            }
        }
        return newest;
    }

    private static List<StockItem> countTotalStock(String jsonString) {
        List<StockItem> stockitems = new ArrayList<StockItem>();
        List<Product> products = mProductDAO.listProducts();
        stockitems = mapStockItemsToList(jsonString);
        List<StockItem> result = new ArrayList<StockItem>();
        for (Product product : products) {
            StockItem itemToAdd = countStock(stockitems, product);
            if (itemToAdd.getCount() != null) {
                result.add(itemToAdd);
            }
        }
        return result;
    }

    private static List<StockItem> mapStockItemsToList(String jsonstring) {
        StockItem[] stockmapping = new Gson().fromJson(jsonstring.toString(), StockItem[].class);
        List<StockItem> result = new ArrayList<StockItem>();
        result = arrayToList2(stockmapping);
        return result;
    }

    private static List<StockItem> arrayToList2(StockItem[] array) {
        List<StockItem> returner = new ArrayList<StockItem>();
        Collections.addAll(returner, array);
        return returner;
    }

    public static List<StockItem> listStock() {
        List<StockItem> returner = new ArrayList<StockItem>();
        try {
            String json = stringOfUrl(BASEURL + "StockDAO/listStock");
            returner = countTotalStock(json);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return returner;
    }

    /**
	 * Method updates the Stock table in the database. This happens when some product is sold, 
	 * or when there are some Stockitems missing from the stock or when there are more stockitems 
	 * in the real stock than according to the system.
	 * @param stockToUpdate Stockitem of which the number of items in stock needs to be updated.
	 * @param difference The number of items that needs to be added (positive) or removed (negative) from the database
	 * @return returns true if the stock is updated
	 */
    public static boolean updateStock(StockItem stockToUpdate, BigDecimal difference) {
        if (stockToUpdate != null) {
            try {
                BigDecimal newAmount = new BigDecimal("0");
                StockItem item = new StockItem();
                if (difference.signum() < 0) {
                    BigDecimal invDiffer = difference.negate();
                    item = searchStockEldest(stockToUpdate.getProduct());
                    if (item != null) {
                        while (item.getCount().compareTo(invDiffer) <= 0) {
                            invDiffer = invDiffer.subtract(item.getCount());
                            deleteStock(item);
                            item = searchStockEldest(stockToUpdate.getProduct());
                        }
                        newAmount = item.getCount().subtract(invDiffer);
                    } else {
                        System.out.println("There is no such stockItem in stock.");
                    }
                } else {
                    item = searchStock(stockToUpdate.getProduct());
                    newAmount = item.getCount().add(difference);
                }
                if (item != null) {
                    String url = BASEURL + "StockDAO/updateStockID?id=" + item.getID() + "&newAmount=" + newAmount;
                    System.out.println(url);
                    String resultString = stringOfUrl(url);
                    System.out.println(resultString);
                    return true;
                }
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (HttpException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static boolean addStockSimple(StockItem stockItemToAdd) {
        if (stockItemToAdd != null) {
            try {
                String url = BASEURL + "StockDAO/addStock?prodtype_id=" + stockItemToAdd.getProduct().getID() + "&amount=" + stockItemToAdd.getCount() + "&costPrice=" + stockItemToAdd.getPrice();
                System.out.println(url);
                String resultString = stringOfUrl(url);
                System.out.println(resultString);
                return true;
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (HttpException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private static boolean deleteStockSimple(StockItem itemToDelete) {
        if (itemToDelete != null) {
            try {
                String url = BASEURL + "StockDAO/deleteStockSimple?id=" + itemToDelete.getID();
                System.out.println(url);
                String resultString = stringOfUrl(url);
                System.out.println(resultString);
                return true;
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (HttpException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private static boolean deleteStock(StockItem stockItemToDelete) {
        if (stockItemToDelete != null) {
            try {
                String jsonstock = new Gson().toJson(stockItemToDelete);
                HttpClient client = new HttpClient();
                PostMethod method = new PostMethod("http://localhost:9876/" + "StockDAO/deleteStock");
                method.addParameter("stock", jsonstock);
                int returnCode = client.executeMethod(method);
                System.out.println(method.getResponseBodyAsString());
                return true;
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            } catch (HttpException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static String stringOfUrl(String addr) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        URL url = new URL(addr);
        IOUtils.copy(url.openStream(), output);
        return output.toString();
    }

    private static void printStockItem(StockItem s) {
        System.out.println(s);
    }
}
