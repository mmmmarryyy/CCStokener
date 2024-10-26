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
import java.util.Collections;
import java.util.List;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.IOUtils;
import be.kuleuven.VTKfakbarCWA1.data.WebserviceAdressContainer;
import be.kuleuven.VTKfakbarCWA1.model.management.User;
import be.kuleuven.VTKfakbarCWA1.model.product.Product;
import be.kuleuven.VTKfakbarCWA1.model.product.StockItem;
import be.kuleuven.VTKfakbarCWA1.model.product.Type;
import be.kuleuven.VTKfakbarCWA1.model.sales.Merchandise;
import be.kuleuven.VTKfakbarCWA1.model.sales.MerchandiseItem;
import be.kuleuven.VTKfakbarCWA1.model.sales.Transaction;
import be.kuleuven.VTKfakbarCWA1.model.sales.TransactionItem;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

public class TransactionDAO {

    private static final String BASEURL = WebserviceAdressContainer.getInstance().getBASEURL();

    private static UserDAO userdao = new UserDAO();

    private static MerchandiseDAO merchdao = new MerchandiseDAO();

    public static void main(String[] args) {
        User user = userdao.getUser(1);
        Merchandise merch1 = merchdao.getMerchandise(1);
        Merchandise merch2 = merchdao.getMerchandise(2);
        Merchandise merch3 = merchdao.getMerchandise(3);
        TransactionItem item1 = new TransactionItem();
        TransactionItem item2 = new TransactionItem();
        TransactionItem item3 = new TransactionItem();
        item1.setMerchandise(merch1);
        item1.setCount(2);
        item2.setMerchandise(merch2);
        item2.setCount(3);
        item3.setMerchandise(merch3);
        item3.setCount(4);
        Transaction transToAdd = new Transaction();
        transToAdd.setTotalPrice(new BigDecimal(12));
        transToAdd.setTransactionUser(user);
        List<TransactionItem> trItems = new ArrayList<TransactionItem>();
        trItems.add(item1);
        trItems.add(item2);
        trItems.add(item3);
        transToAdd.setOrder(trItems);
        Date date = new Date(110, 11, 14);
        Time time = new Time(23, 6, 15);
        transToAdd.setTransactionDate(date);
        transToAdd.setTransactionTime(time);
        addTransaction(transToAdd);
    }

    public static Transaction getTransaction(Integer id) {
        if (id != null && id > 0) {
            try {
                String url = BASEURL + "TransactionDAO/getTransaction?id=" + id;
                String json = stringOfUrl(url);
                Transaction transaction = mapTransaction(json);
                List<TransactionItem> order = getTransactionItemsForTransaction(transaction);
                for (TransactionItem item : order) {
                    Merchandise merch = item.getMerchandise();
                    merch.setComponents(getMerchandiseItemsForMerchandise(merch));
                }
                transaction.setOrder(order);
                return transaction;
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

    public static List<Transaction> listTransactions() {
        List<Transaction> returner = new ArrayList<Transaction>();
        try {
            String json = stringOfUrl(BASEURL + "TransactionDAO/listTransactions");
            List<Transaction> transactions = mapTransactionsToList(json);
            List<TransactionItem> order = new ArrayList<TransactionItem>();
            for (Transaction transaction : transactions) {
                order = getTransactionItemsForTransaction(transaction);
                for (TransactionItem item : order) {
                    Merchandise merch = item.getMerchandise();
                    merch.setComponents(getMerchandiseItemsForMerchandise(merch));
                }
                transaction.setOrder(order);
                returner.add(transaction);
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return returner;
    }

    private static List<TransactionItem> getTransactionItemsForTransaction(Transaction transaction) {
        List<TransactionItem> returner = new ArrayList<TransactionItem>();
        try {
            String url = BASEURL + "TransactionDAO/getTransactionItems?transactionId=" + transaction.getId();
            String json = stringOfUrl(url);
            returner = mapTransactionItemsToList(json);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (JsonParseException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return returner;
    }

    private static List<MerchandiseItem> getMerchandiseItemsForMerchandise(Merchandise merchandise) {
        return merchdao.getMerchandiseItemsForMerchandise(merchandise);
    }

    public static boolean addTransaction(Transaction transactionToAdd) {
        if (transactionToAdd != null) {
            try {
                String jsontransaction = new Gson().toJson(transactionToAdd);
                HttpClient client = new HttpClient();
                PostMethod method = new PostMethod(BASEURL + "TransactionDAO/addTransaction");
                method.addParameter("transaction", jsontransaction);
                int returnCode = client.executeMethod(method);
                if (returnCode != 200) {
                    return false;
                }
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

    public static boolean deleteTransaction(Transaction transactionToDelete) {
        if (transactionToDelete != null) {
            try {
                String jsontransaction = new Gson().toJson(transactionToDelete);
                HttpClient client = new HttpClient();
                PostMethod method = new PostMethod(BASEURL + "TransactionDAO/deleteTransaction");
                method.addParameter("transaction", jsontransaction);
                int returnCode = client.executeMethod(method);
                if (returnCode != 200) {
                    return false;
                }
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

    private static List<Transaction> transactionArrayToList(Transaction[] array) {
        List<Transaction> returner = new ArrayList<Transaction>();
        Collections.addAll(returner, array);
        return returner;
    }

    private static List<TransactionItem> transactionItemArrayToList(TransactionItem[] array) {
        List<TransactionItem> returner = new ArrayList<TransactionItem>();
        Collections.addAll(returner, array);
        return returner;
    }

    private static Transaction mapTransaction(String jsonstring) {
        Transaction[] transactionmapping = new Gson().fromJson(jsonstring, Transaction[].class);
        return transactionmapping[0];
    }

    private static List<Transaction> mapTransactionsToList(String jsonstring) {
        Transaction[] transactionmapping = new Gson().fromJson(jsonstring.toString(), Transaction[].class);
        return transactionArrayToList(transactionmapping);
    }

    private static List<TransactionItem> mapTransactionItemsToList(String jsonstring) {
        TransactionItem[] transactionItemMapping = new Gson().fromJson(jsonstring.toString(), TransactionItem[].class);
        return transactionItemArrayToList(transactionItemMapping);
    }

    private void printTransactions(List<Transaction> transactions) {
        for (Transaction transaction : transactions) {
            printTransaction(transaction);
        }
    }

    private void printTransaction(Transaction transaction) {
        System.out.println(transaction);
    }
}
