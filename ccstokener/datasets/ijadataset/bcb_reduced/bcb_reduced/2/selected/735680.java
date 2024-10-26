package edu.indiana.cs.classes.b534;

import java.net.*;
import java.io.*;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Calendar;
import org.apache.axis2.*;
import edu.indiana.cs.classes.b534.MyAuctionServiceExceptionException0;
import edu.indiana.cs.classes.b534.MyAuctionServiceStub;
import edu.indiana.cs.classes.b534.MyAuctionServiceStub.CategoryType;
import edu.indiana.cs.classes.b534.MyAuctionServiceStub.Item;
import edu.indiana.cs.classes.b534.MyAuctionServiceStub.ItemStatusType;
import edu.indiana.cs.classes.b534.MyAuctionServiceStub.ItemType;
import edu.indiana.cs.classes.b534.MyAuctionServiceStub.Response;
import edu.indiana.cs.classes.b534.MyAuctionServiceStub.ServerTime;

public class Seller implements Runnable {

    int SellerID = 0;

    public Seller(int currentSeller) {
        SellerID = currentSeller;
    }

    public ArrayList<String> getFileList(String url) throws IOException {
        URL pic_url = new URL(url);
        ArrayList<String> links = new ArrayList<String>();
        String urlBase = "http://cs.indiana.edu/~echintha/b534/photos/";
        URLConnection pic_c = pic_url.openConnection();
        BufferedReader in = new BufferedReader(new InputStreamReader(pic_c.getInputStream()));
        String inputLine;
        while ((inputLine = in.readLine()) != null) {
            if (inputLine.isEmpty()) {
                continue;
            }
            links.add(urlBase + inputLine);
        }
        in.close();
        return links;
    }

    @Override
    public void run() {
        try {
            try {
                ArrayList<String> result = new ArrayList<String>();
                result = getFileList("http://cs.indiana.edu/~echintha/b534/photos/index.txt");
                for (String string : result) {
                    System.out.println(string);
                }
            } catch (IOException e2) {
                e2.printStackTrace();
            }
            MyAuctionServiceStub stub;
            String uid;
            stub = new MyAuctionServiceStub("http://localhost:8080/axis2/services/MyAuctionService");
            MyAuctionServiceStub.UserDetails aUser = new MyAuctionServiceStub.UserDetails();
            aUser.setId("2342");
            aUser.setAddress("address");
            aUser.setEmail("test@test");
            aUser.setName("user name");
            aUser.setAccountBalance(100.00);
            aUser.setAddress("bloomington");
            aUser.setItemsOnSale(null);
            Response idRes;
            idRes = stub.register(aUser);
            System.out.println("Client gets id:" + idRes.getId());
            uid = idRes.getId();
            int nItem = 10;
            while (nItem > 0) {
                nItem--;
                try {
                    Thread.sleep(10000);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                ItemType itemSpec = new ItemType();
                itemSpec.setBuyItNowPrice(10);
                itemSpec.setCategory(CategoryType.Books);
                itemSpec.setId(idRes.getId());
                itemSpec.setCurrentPrice(5);
                itemSpec.setDescription("The Bibble");
                itemSpec.setName("The Bibble");
                itemSpec.setSellerId(uid);
                ServerTime stime;
                try {
                    stime = stub.getServerTime();
                    Calendar c = stime.getServerTime();
                    c.add(Calendar.MINUTE, 3);
                    itemSpec.setEndTime(c);
                    itemSpec.setStatus(ItemStatusType.OnAuction);
                    Item sellItem = new Item();
                    sellItem.setItem(itemSpec);
                    stub.sell(sellItem);
                } catch (RemoteException e) {
                    e.printStackTrace();
                } catch (MyAuctionServiceExceptionException0 e) {
                    e.printStackTrace();
                }
                System.out.println("New item was added");
            }
        } catch (AxisFault e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (MyAuctionServiceExceptionException0 e) {
            e.printStackTrace();
        }
    }
}
