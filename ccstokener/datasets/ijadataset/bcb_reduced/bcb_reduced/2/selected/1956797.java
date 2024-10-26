package com.miracleas.bitcoin_spinner_lib;

import java.net.HttpURLConnection;
import java.net.URL;
import android.os.Handler;
import com.bccapi.core.StreamReader;

/**
 * This class lets you obtain USD -> BTC exchange rates from MtGox.
 */
public class Ticker {

    private static final int MTGOX_RETRIES = 3;

    private static final long MTGOX_CACHE_INTERVAL = Consts.MINUTE_IN_NANOSECONDS * 5;

    private static Double _mtgoxInUsd;

    private static long _lastMtgoxInUsd;

    private static boolean _blockerOn;

    public interface BtcToUsdCallbackHandler {

        public void handleBtcToUsdCallback(Double usdValue);
    }

    private static class BtcToUsdRequester implements Runnable {

        long _satoshis;

        Handler _handler;

        BtcToUsdCallbackHandler _callBackhandler;

        public BtcToUsdRequester(long satoshis, Handler handler, BtcToUsdCallbackHandler callbackHandler) {
            _satoshis = satoshis;
            _handler = handler;
            _callBackhandler = callbackHandler;
        }

        @Override
        public void run() {
            if (_blockerOn) {
                return;
            }
            try {
                _blockerOn = true;
                for (int i = 0; i < MTGOX_RETRIES; i++) {
                    Double mtgox = getMtGoxUsdBuy();
                    if (mtgox != null) {
                        doCallback(mtgox);
                        return;
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        doCallback(null);
                        return;
                    }
                }
                doCallback(null);
            } finally {
                _blockerOn = false;
            }
        }

        private Double getMtGoxUsdBuy() {
            try {
                URL url = new URL("https://mtgox.com/api/0/data/ticker.php");
                HttpURLConnection connection;
                connection = (HttpURLConnection) url.openConnection();
                connection.setReadTimeout(60000);
                connection.setRequestMethod("GET");
                connection.connect();
                int status = connection.getResponseCode();
                if (status != 200) {
                    return null;
                }
                String ticker = StreamReader.readFully(connection.getInputStream());
                int buyIndex = ticker.indexOf("\"buy\":");
                int sellIndex = ticker.indexOf(",\"sell\"");
                if (buyIndex == -1 || sellIndex == -1 || buyIndex >= sellIndex) {
                    return null;
                }
                String buyString = ticker.substring(buyIndex + 6, sellIndex);
                return Double.parseDouble(buyString);
            } catch (Exception e) {
                return null;
            }
        }

        private synchronized void doCallback(Double value) {
            _mtgoxInUsd = value;
            final Double btc = new Double(_satoshis) / Consts.SATOSHIS_PER_BITCOIN;
            final Double usd;
            if (_mtgoxInUsd != null) {
                usd = btc * _mtgoxInUsd;
            } else {
                usd = null;
            }
            _handler.post(new Runnable() {

                @Override
                public void run() {
                    _callBackhandler.handleBtcToUsdCallback(usd);
                }
            });
        }
    }

    /**
	 * Request a number of Bitcoins measured in satoshis to be converted into USD at the current MtGox buy rate.
	 * 
	 * @param satoshis
	 *            The number of Bitcoins in satoshis
	 * @param callback
	 *            The {@link BtcToUsdCallbackHandler} to make a call back to once the conversion is done.
	 */
    public static void requestBtcToUsd(long satoshis, BtcToUsdCallbackHandler callback) {
        if (_mtgoxInUsd == null || _lastMtgoxInUsd + MTGOX_CACHE_INTERVAL < System.nanoTime()) {
            _lastMtgoxInUsd = System.nanoTime();
            final Handler handler = new Handler();
            Thread t = new Thread(new BtcToUsdRequester(satoshis, handler, callback));
            t.start();
        } else {
            new BtcToUsdRequester(satoshis, new Handler(), callback).doCallback(_mtgoxInUsd);
        }
    }
}
