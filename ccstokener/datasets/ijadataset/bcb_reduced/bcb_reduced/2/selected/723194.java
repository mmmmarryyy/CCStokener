package be.kuleuven.VTKfakbarCWA1.data;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.io.IOUtils;
import be.kuleuven.VTKfakbarCWA1.model.sales.Merchandise;
import be.kuleuven.VTKfakbarCWA1.model.sales.MerchandiseItem;
import be.kuleuven.VTKfakbarCWA1.model.sales.Transaction;
import be.kuleuven.VTKfakbarCWA1.model.sales.TransactionItem;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;

public class TransactionDAO {

    private final String BASEURL = WebserviceAdressContainer.getInstance().getBASEURL();

    private final MerchandiseDAO merchDao = MerchandiseDAO.getSingletonMerchandiseDAO();

    public Transaction getTransaction(Integer id) {
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

    public List<Transaction> listTransactions() {
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

    private List<TransactionItem> getTransactionItemsForTransaction(Transaction transaction) {
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

    private List<MerchandiseItem> getMerchandiseItemsForMerchandise(Merchandise merchandise) {
        return merchDao.getMerchandiseItemsForMerchandise(merchandise);
    }

    public boolean addTransaction(Transaction transactionToAdd) {
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

    public boolean deleteTransaction(Transaction transactionToDelete) {
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

    public String stringOfUrl(String addr) throws IOException {
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

    private List<Transaction> transactionArrayToList(Transaction[] array) {
        List<Transaction> returner = new ArrayList<Transaction>();
        Collections.addAll(returner, array);
        return returner;
    }

    private List<TransactionItem> transactionItemArrayToList(TransactionItem[] array) {
        List<TransactionItem> returner = new ArrayList<TransactionItem>();
        Collections.addAll(returner, array);
        return returner;
    }

    private Transaction mapTransaction(String jsonstring) {
        Transaction[] transactionmapping = new Gson().fromJson(jsonstring, Transaction[].class);
        return transactionmapping[0];
    }

    private List<Transaction> mapTransactionsToList(String jsonstring) {
        Transaction[] transactionmapping = new Gson().fromJson(jsonstring.toString(), Transaction[].class);
        return transactionArrayToList(transactionmapping);
    }

    private List<TransactionItem> mapTransactionItemsToList(String jsonstring) {
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
