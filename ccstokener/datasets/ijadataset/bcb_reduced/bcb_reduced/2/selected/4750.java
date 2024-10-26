package com.junmiao.A.stock.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.nio.client.DefaultHttpAsyncClient;
import org.apache.http.nio.client.HttpAsyncClient;
import org.apache.http.nio.concurrent.FutureCallback;
import org.apache.http.nio.reactor.IOReactorException;
import org.apache.http.util.EntityUtils;
import com.junmiao.A.postProcess.SohuStockPostProcess;
import com.junmiao.A.stockModel.SohuStockBean;

public class ParseSohuStock {

    public static int count = 1;

    public static void parseSohuStock(ArrayList<String> dataSource, final ArrayList<SohuStockBean> sohuStockBeanList) throws IOReactorException, InterruptedException {
        HttpAsyncClient httpclient = new DefaultHttpAsyncClient();
        httpclient.start();
        if (dataSource != null && dataSource.size() > 0) {
            final CountDownLatch latch = new CountDownLatch(dataSource.size());
            for (int i = 0; i < dataSource.size(); i++) {
                final HttpGet request = new HttpGet(dataSource.get(i));
                httpclient.execute(request, new FutureCallback<HttpResponse>() {

                    public void completed(final HttpResponse response) {
                        System.out.println(" Request completed " + count + " " + request.getRequestLine() + " " + response.getStatusLine());
                        try {
                            HttpEntity he = response.getEntity();
                            try {
                                String resp = EntityUtils.toString(he, "gb2312");
                                if (resp != null && resp.length() > 0) {
                                    SohuStockBean shstBean = SohuStockPostProcess.postSohuStockBeanProcess(resp);
                                    sohuStockBeanList.add(shstBean);
                                }
                                count++;
                            } catch (ParseException e) {
                                e.printStackTrace();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            latch.countDown();
                        } catch (RuntimeException re) {
                            latch.countDown();
                        }
                    }

                    public void failed(final Exception ex) {
                        latch.countDown();
                    }

                    public void cancelled() {
                        latch.countDown();
                    }
                });
            }
            latch.await();
            System.out.println("done");
        }
        if (httpclient != null) {
            httpclient.shutdown();
        }
        System.out.println(sohuStockBeanList.size());
    }
}
