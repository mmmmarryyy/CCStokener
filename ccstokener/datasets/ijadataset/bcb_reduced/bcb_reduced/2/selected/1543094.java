package be.kuleuven.cw.peno3.client;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Date;
import java.sql.Time;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import be.kuleuven.VTKfakbarCWA1.data.WebserviceAdressContainer;
import be.kuleuven.VTKfakbarCWA1.model.product.Product;
import be.kuleuven.VTKfakbarCWA1.model.product.StockItem;
import be.kuleuven.VTKfakbarCWA1.model.product.Type;
import be.kuleuven.VTKfakbarCWA1.model.sales.Merchandise;
import be.kuleuven.VTKfakbarCWA1.model.sales.MerchandiseItem;

public class ProductDAO {

    private static ProductDAO singletonProductDAO;

    private static final String BASEURL = WebserviceAdressContainer.getInstance().getBASEURL();

    public static synchronized ProductDAO getSingletonMerchandiseManager() {
        if (singletonProductDAO == null) {
            singletonProductDAO = new ProductDAO();
        }
        return singletonProductDAO;
    }

    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    public static void main(String[] args) {
        Calendar cal = Calendar.getInstance();
        Date d = new Date(cal.getTimeInMillis());
        Time t = new Time(cal.getTimeInMillis());
        System.out.println(d);
        System.out.println(t);
        Calendar calen = Calendar.getInstance();
        calen.set(d.getYear() + 1900, d.getMonth(), d.getDate(), t.getHours(), t.getMinutes(), t.getSeconds());
        Date da = new Date(calen.getTimeInMillis());
        Time ti = new Time(cal.getTimeInMillis());
    }

    public List<Product> searchProducts(String searchString) {
        List<Product> returner = new ArrayList<Product>();
        if (searchString != null) {
            try {
                String url = BASEURL + "ProductDAO/searchProducts?productName=" + searchString;
                System.out.println(url);
                String json = stringOfUrl(url);
                returner = mapProductsToList(json);
                printProducts(returner);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (JsonParseException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return returner;
    }

    public Product getProduct(Integer id) {
        if (id != null && id > 0) {
            try {
                String url = BASEURL + "ProductDAO/getProduct?id=" + id;
                System.out.println(url);
                String json = stringOfUrl(url);
                Product product = mapProduct(json);
                printProduct(product);
                return product;
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
        List<Product> products = listProducts();
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

    /**
	 * Method inserts a "delta" of a certain StockItem in the database.
	 * Delta is a positive number if there is more in the real stock than according to the program.
	 * Delta is a negative number if there are some items missing from the stock.
	 * Method will also automatically update the list Stock in the Database.
	 * @param item A StockItem of which the stock must be adapted.
	 * @param delta The amount of items that are to missing or that are to much in the real stock.
	 * @param date Date on which the delta was notified to the system.
	 * @param time Time of the day on which the delta was notified to the system.
	 * @return
	 */
    public static boolean addDeltaStock(StockItem item, BigDecimal delta, Date date, Time time) {
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

    /**
	 * Method gets all Delta's from the database. Remark that the date-object in the Stockitem reffers to
	 * the date of the delta and not the date of when the stockdelivery was made 
	 * price is the purchase price of the product.
	 * @return HashMap which contains the stockItem and the amount of leftovers (if positive) or missing items (if negative).
	 */
    public static HashMap<StockItem, BigDecimal> listDeltas() {
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

    private static HashMap<StockItem, BigDecimal> mapDeltasToMap(String json) {
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

    public static HashMap<StockItem, BigDecimal> getDeltaDate(Date date, Time time) {
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

    public static HashMap<StockItem, BigDecimal> getDeltaProduct(Product p) {
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

    public static boolean deleteDeltaStock(int id) {
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
                String wilikweten = method.getResponseBodyAsString();
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

    public static List<Product> listProducts() {
        List<Product> returner = new ArrayList<Product>();
        try {
            String json = stringOfUrl(BASEURL + "ProductDAO/listProducts");
            returner = mapProductsToList(json);
            printProducts(returner);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return returner;
    }

    public boolean updateProduct(Product productToUpdate) {
        if (productToUpdate != null) {
            try {
                String jsonproduct = new Gson().toJson(productToUpdate);
                printProduct(productToUpdate);
                HttpClient client = new HttpClient();
                PostMethod method = new PostMethod(BASEURL + "ProductDAO/updateProduct");
                method.addParameter("product", jsonproduct);
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

    public boolean addProductSimple(Product productToAdd) {
        if (productToAdd != null) {
            try {
                printProduct(productToAdd);
                String url = BASEURL + "ProductDAO/addProduct?productName=" + productToAdd.getName().replace(" ", "%20") + "&type=" + productToAdd.getType().toDatabaseString() + "&quantityPerProduct=" + productToAdd.getQuantity();
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

    public boolean addProduct(Product productToAdd) {
        if (productToAdd != null) {
            try {
                String jsonproduct = new Gson().toJson(productToAdd);
                printProduct(productToAdd);
                HttpClient client = new HttpClient();
                PostMethod method = new PostMethod(BASEURL + "ProductDAO/addProductJson");
                method.addParameter("product", jsonproduct);
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

    public static boolean deleteProduct(Product productToDelete) {
        if (productToDelete != null) {
            try {
                String jsonproduct = new Gson().toJson(productToDelete);
                printProduct(productToDelete);
                HttpClient client = new HttpClient();
                PostMethod method = new PostMethod(BASEURL + "ProductDAO/deleteProduct");
                method.addParameter("product", jsonproduct);
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

    public String streamToString(InputStream stream) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        IOUtils.copy(stream, output);
        return output.toString();
    }

    private static List<Product> arrayToList(Product[] array) {
        List<Product> returner = new ArrayList<Product>();
        Collections.addAll(returner, array);
        return returner;
    }

    private Product mapProduct(String jsonstring) {
        Product[] productmapping = new Gson().fromJson(jsonstring, Product[].class);
        return productmapping[0];
    }

    private static List<Product> mapProductsToList(String jsonstring) {
        Product[] productmapping = new Gson().fromJson(jsonstring.toString(), Product[].class);
        return arrayToList(productmapping);
    }

    private static void printProducts(List<Product> products) {
        for (Product product : products) {
            printProduct(product);
        }
    }

    private static void printProduct(Product productToPrint) {
        System.out.println(productToPrint);
    }

    private static void printStockItem(StockItem s) {
        System.out.println(s);
    }
}
