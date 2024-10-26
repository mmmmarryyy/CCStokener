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
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import be.kuleuven.VTKfakbarCWA1.model.product.Product;

public class ProductDAO {

    private static ProductDAO singletonProductDAO;

    private final String BASEURL = WebserviceAdressContainer.getInstance().getBASEURL();

    public static synchronized ProductDAO getSingletonMerchandiseManager() {
        if (singletonProductDAO == null) {
            singletonProductDAO = new ProductDAO();
        }
        return singletonProductDAO;
    }

    public static synchronized ProductDAO getSingletonStockDAO() {
        if (singletonProductDAO == null) {
            singletonProductDAO = new ProductDAO();
        }
        return singletonProductDAO;
    }

    public Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException();
    }

    public List<Product> searchProducts(String searchString) {
        List<Product> returner = new ArrayList<Product>();
        if (searchString != null) {
            try {
                String url = BASEURL + "ProductDAO/searchProducts?productName=" + searchString;
                String json = stringOfUrl(url);
                returner = mapProductsToList(json);
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
                String json = stringOfUrl(url);
                Product product = mapProduct(json);
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

    public List<Product> listProducts() {
        List<Product> returner = new ArrayList<Product>();
        try {
            String json = stringOfUrl(BASEURL + "ProductDAO/listProducts");
            returner = mapProductsToList(json);
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
                HttpClient client = new HttpClient();
                PostMethod method = new PostMethod(BASEURL + "ProductDAO/updateProduct");
                method.addParameter("product", jsonproduct);
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

    public boolean addProductSimple(Product productToAdd) {
        if (productToAdd != null) {
            try {
                String url = BASEURL + "ProductDAO/addProduct?productName=" + productToAdd.getName().replace(" ", "%20") + "&type=" + productToAdd.getType().toDatabaseString() + "&quantityPerProduct=" + productToAdd.getQuantity();
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

    public boolean addProduct(Product productToAdd) {
        if (productToAdd != null) {
            try {
                String jsonproduct = new Gson().toJson(productToAdd);
                HttpClient client = new HttpClient();
                PostMethod method = new PostMethod(BASEURL + "ProductDAO/addProductJson");
                method.addParameter("product", jsonproduct);
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

    public boolean deleteProduct(Product productToDelete) {
        if (productToDelete != null) {
            try {
                String jsonproduct = new Gson().toJson(productToDelete);
                HttpClient client = new HttpClient();
                PostMethod method = new PostMethod(BASEURL + "ProductDAO/deleteProduct");
                method.addParameter("product", jsonproduct);
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

    public boolean deactivateProduct(Product productToDeactivate) {
        if (productToDeactivate != null) {
            try {
                String url = BASEURL + "ProductDAO/deactivateProduct?id=" + productToDeactivate.getID();
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

    private List<Product> arrayToList(Product[] array) {
        List<Product> returner = new ArrayList<Product>();
        Collections.addAll(returner, array);
        return returner;
    }

    private Product mapProduct(String jsonstring) {
        Product[] productmapping = new Gson().fromJson(jsonstring, Product[].class);
        return productmapping[0];
    }

    private List<Product> mapProductsToList(String jsonstring) {
        Product[] productmapping = new Gson().fromJson(jsonstring.toString(), Product[].class);
        return arrayToList(productmapping);
    }

    private void printProducts(List<Product> products) {
        for (Product product : products) {
            printProduct(product);
        }
    }

    private void printProduct(Product productToPrint) {
        System.out.println(productToPrint);
    }
}
