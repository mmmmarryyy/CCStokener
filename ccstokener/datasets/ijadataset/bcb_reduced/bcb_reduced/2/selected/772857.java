package be.kuleuven.VTKfakbarCWA1.data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Date;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.IOUtils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import be.kuleuven.VTKfakbarCWA1.model.product.Product;
import be.kuleuven.VTKfakbarCWA1.model.product.StockItem;

public class StockDAO {

    private static StockDAO singletonStockDAO;

    private static final String BASEURL = WebserviceAdressContainer.getInstance().getBASEURL();

    private static ProductDAO mProductDAO = ProductDAO.getSingletonStockDAO();

    public static synchronized StockDAO getSingletonStockManager() {
        if (singletonStockDAO == null) {
            singletonStockDAO = new StockDAO();
        }
        return singletonStockDAO;
    }

    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    /**
	 * Method returns the total count of stock that's in the Stock table of the database.
	 * @param product Product who's stock is being asked.
	 * @return true if the field ID of the Product-object is not null.
	 */
    public StockItem searchStock(Product product) {
        if (product != null && product.getID() > 0) {
            try {
                int id = product.getID();
                String url = BASEURL + "StockDAO/getStock?prodtype_id=" + id;
                String json = stringOfUrl(url);
                List<StockItem> stockitems = new ArrayList<StockItem>();
                stockitems = mapStockItemsToList(json);
                StockItem stock = countStock(stockitems, product);
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

    /**
	 * Returns the total amounts of all products in stock. 
	 * @return List of StockItems
	 */
    public List<StockItem> listStock() {
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
    public boolean updateStock(StockItem stockToUpdate, BigDecimal difference) {
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
                    String resultString = stringOfUrl(url);
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

    /**
	 * Method inserts a new StockItem in the database. Date of the StockItem should refer to the day of the stock-delivery.
	 * @param stockItemToAdd
	 * @return true if the StockItem was added.
	 */
    public boolean addStockSimple(StockItem stockItemToAdd) {
        if (stockItemToAdd != null) {
            try {
                String url = BASEURL + "StockDAO/addStock?prodtype_id=" + stockItemToAdd.getProduct().getID() + "&amount=" + stockItemToAdd.getCount() + "&costPrice=" + stockItemToAdd.getPrice();
                String resultString = stringOfUrl(url);
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

    public HashMap<StockItem, BigDecimal> getDeltaDate(Date date, Time time) {
        HashMap<StockItem, BigDecimal> returner = new HashMap<StockItem, BigDecimal>();
        if (date != null && time != null) {
            try {
                String url = BASEURL + "DeltaStock/getDeltaDate?date=" + date + "&time=" + time;
                System.out.println(url);
                String json = stringOfUrl(url);
                returner = mapDeltasToMap(json);
                return returner;
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

    public HashMap<StockItem, BigDecimal> getDeltaProduct(Product p) {
        HashMap<StockItem, BigDecimal> returner = new HashMap<StockItem, BigDecimal>();
        if (p.getID() > 0) {
            try {
                String url = BASEURL + "DeltaStock/getDeltaProduct?prodtype_id=" + p.getID();
                System.out.println(url);
                String json = stringOfUrl(url);
                returner = mapDeltasToMap(json);
                return returner;
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

    /**
	 * Method gets all Delta's from the database. Remark that the date-object in the Stockitem reffers to
	 * the date of the delta and not the date of when the stockdelivery was made 
	 * price is the purchase price of the product.
	 * @return HashMap which contains the stockItem and the amount of leftovers (if positive) or missing items (if negative).
	 */
    public HashMap<StockItem, BigDecimal> listDeltas() {
        try {
            String url = BASEURL + "DeltaStock/listDeltas";
            String json = stringOfUrl(url);
            System.out.println(json);
            return mapDeltasToMap(json);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
	 * Method inserts a shortage or a surplus of a certain StockItem in the database.
	 * Delta is a positive number if there is more in the real stock than according to the program.
	 * Delta is a negative number if there are some items missing from the stock.
	 * Method will also automatically update the list Stock in the Database.
	 * @param item A StockItem of which the stock must be adapted.
	 * @param delta The amount of items that are to missing or that are to much in the real stock.
	 * @param date Date on which the delta was notified to the system.
	 * @param time Time of the day on which the delta was notified to the system.
	 * @return true if the requested parameters item, delta and date are not "null".
	 */
    public boolean addDeltaStock(StockItem item, BigDecimal delta, Date date, Time time) {
        if (item != null && delta != null && date != null) {
            try {
                updateStock(item, delta);
                String url = BASEURL + "DeltaStock/addDeltaStock?prodtype_id=" + item.getProduct().getID() + "&date=" + date + "&time=" + time + "&costPrice=" + item.getPrice() + "&delta=" + delta;
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

    public boolean deleteDeltaStock(int id) {
        if (id > 0) {
            try {
                String url = BASEURL + "DeltaStock/DeleteDeltaStock?id=" + id;
                System.out.println(url);
                String json = stringOfUrl(url);
                return true;
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (JsonParseException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    private boolean deleteStockSimple(StockItem itemToDelete) {
        if (itemToDelete != null) {
            try {
                String url = BASEURL + "StockDAO/deleteStockSimple?id=" + itemToDelete.getID();
                String resultString = stringOfUrl(url);
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

    private boolean deleteStock(StockItem stockItemToDelete) {
        if (stockItemToDelete != null) {
            try {
                String jsonstock = new Gson().toJson(stockItemToDelete);
                HttpClient client = new HttpClient();
                PostMethod method = new PostMethod("http://localhost:9876/" + "StockDAO/deleteStock");
                method.addParameter("stock", jsonstock);
                int returnCode = client.executeMethod(method);
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

    private StockItem searchStockEldest(Product product) {
        if (product != null && product.getID() > 0) {
            try {
                int id = product.getID();
                String url = BASEURL + "StockDAO/getStock?prodtype_id=" + id;
                String json = stringOfUrl(url);
                List<StockItem> stockitems = new ArrayList<StockItem>();
                stockitems = mapStockItemsToList(json);
                if (stockitems.isEmpty() != true) {
                    StockItem stock = getEldestItem(stockitems, product);
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

    private StockItem getEldestItem(List<StockItem> stockitems, Product product) {
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

    /**
	 * Method searches for the most recent Stockitem from the list.
	 * @param stockitems List of StockItems
	 * @param product Product of which the newest StockItem is requested.
	 * @return StockItem of the Product with the most recent Date.
	 */
    private StockItem getNewestItem(List<StockItem> stockitems, Product product) {
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

    private StockItem countStockEldest(List<StockItem> stockitems, Product product) {
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

    /**
	 * Counts the stock of 1 product.
	 * @param stockitems List of different delivery's of 1 product.
	 * @param product To check if the StockItems do match the requested Product.
	 * @return StockItem, with the Date-field of the most recent StockItem and with the total count of the stock as Count-field.
	 */
    private StockItem countStock(List<StockItem> stockitems, Product product) {
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

    private List<StockItem> countTotalStock(String jsonString) {
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

    private List<StockItem> mapStockItemsToList(String jsonstring) {
        StockItem[] stockmapping = new Gson().fromJson(jsonstring.toString(), StockItem[].class);
        List<StockItem> result = new ArrayList<StockItem>();
        result = arrayToList2(stockmapping);
        return result;
    }

    private List<StockItem> arrayToList2(StockItem[] array) {
        List<StockItem> returner = new ArrayList<StockItem>();
        Collections.addAll(returner, array);
        return returner;
    }

    public String stringOfUrl(String addr) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        URL url = new URL(addr);
        IOUtils.copy(url.openStream(), output);
        return output.toString();
    }

    /**
	 * Method converts a json string which represents some elements from the DeltaStock table from the database 
	 * into a HashMap which will contain StockItem-objects and a BigDecimal which represents the amount of delta.
	 * @param json A json string.
	 * @return HashMap with all the information about a shortage or a surplus of stock.
	 */
    private HashMap<StockItem, BigDecimal> mapDeltasToMap(String json) {
        String string2 = "{\"deltas\":" + json + "}";
        JsonParser parser = new JsonParser();
        JsonObject jsonobj = parser.parse(string2).getAsJsonObject();
        JsonArray deltas = jsonobj.getAsJsonArray("deltas");
        HashMap<StockItem, BigDecimal> result = new HashMap<StockItem, BigDecimal>();
        for (JsonElement j : deltas) {
            StockItem stock = new Gson().fromJson(j.toString(), StockItem.class);
            String d = j.getAsJsonObject().get("delta").toString();
            BigDecimal deci = new BigDecimal(d);
            result.put(stock, deci);
        }
        System.out.println(result);
        return result;
    }
}
