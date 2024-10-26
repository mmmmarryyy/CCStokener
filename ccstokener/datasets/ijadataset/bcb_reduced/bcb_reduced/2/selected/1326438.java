package net.seismon.seismolinkClient.client;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import javax.xml.ws.Holder;
import net.seismon.seismolinkClient.webservice.DataItemCollectionType;
import net.seismon.seismolinkClient.webservice.DataItemType;
import net.seismon.seismolinkClient.webservice.DataRequestType;
import net.seismon.seismolinkClient.webservice.DataStatusResponseType;
import net.seismon.seismolinkClient.webservice.ErrorType;
import net.seismon.seismolinkClient.webservice.Inventory;
import net.seismon.seismolinkClient.webservice.InventoryRequestType;
import net.seismon.seismolinkClient.webservice.InventoryResponseType;
import net.seismon.seismolinkClient.webservice.InventoryType;
import net.seismon.seismolinkClient.webservice.Network;
import net.seismon.seismolinkClient.webservice.PurgeStatusResponseType;
import net.seismon.seismolinkClient.webservice.RoutedRequestType;
import net.seismon.seismolinkClient.webservice.Seismolink;
import net.seismon.seismolinkClient.webservice.Seismolink_Service;
import net.seismon.seismolinkClient.webservice.StationIdentifierType;
import net.seismon.seismolinkClient.webservice.TemporalBoundsType;
import net.seismon.seismolinkClient.webservice.TimePeriodType;
import net.seismon.seismolinkClient.webservice.TimePositionType;
import net.seismon.seismolinkClient.webservice.UserTokenType;

/**
 * @author stefan
 *
 */
public class SeismolinkClient {

    /**
	 * @param args
	 */
    private Seismolink_Service service;

    private Seismolink binding;

    private UserTokenType userToken;

    public SeismolinkClient(String email) {
        service = new Seismolink_Service();
        binding = service.getSeismolinkSOAP12Binding();
        userToken = new UserTokenType();
        userToken.setEmail(email);
    }

    public List<Network> ml_getInventory(List<Object> stations, String beginTime, String endTime) {
        List<StationIdentifierType> newStations = new ArrayList<StationIdentifierType>();
        for (Object cur : stations) {
            newStations.add((StationIdentifierType) cur);
        }
        return this.getInventory(newStations, beginTime, endTime);
    }

    public List<Network> getInventory(List<StationIdentifierType> stations, String beginTime, String endTime) {
        InventoryRequestType requestParam = new InventoryRequestType();
        requestParam.setUserToken(userToken);
        TemporalBoundsType tempBound = new TemporalBoundsType();
        TimePeriodType timePeriod = new TimePeriodType();
        timePeriod.setFrame("#ISO-8601");
        TimePositionType beginPosition = new TimePositionType();
        beginPosition.setFrame("#ISO-8601");
        beginPosition.getValue().add(beginTime);
        TimePositionType endPosition = new TimePositionType();
        endPosition.setFrame("#ISO-8601");
        endPosition.getValue().add(endTime);
        timePeriod.setBeginPosition(beginPosition);
        timePeriod.setEndPosition(endPosition);
        tempBound.setTimePeriod(timePeriod);
        for (StationIdentifierType cur : stations) {
            cur.setTimeSpan(tempBound);
            requestParam.getStationIdentifierFilter().add(cur);
        }
        InventoryResponseType slResponse = new InventoryResponseType();
        System.out.print("Sending the request...");
        try {
            System.out.println();
            System.out.println(requestParam.toString());
            slResponse = binding.getInventory(requestParam);
        } catch (Exception e) {
            System.out.println("Error after getInventory");
            System.out.println(e.getMessage());
            e.printStackTrace();
            return new ArrayList<Network>();
        }
        System.out.println("done.");
        InventoryType slArclinkInventory = slResponse.getArclinkInventory();
        Inventory inventory = (Inventory) slArclinkInventory.getAny();
        List<Object> inventoryNetworks = (List<Object>) inventory.getAux_DeviceOrSeismometerOrResp_Paz();
        List<Network> networkList = new ArrayList<Network>();
        for (Object next : inventoryNetworks) {
            networkList.add((Network) next);
        }
        return networkList;
    }

    public boolean ml_getWaveform(List<Object> stations, String beginTime, String endTime, String filename) {
        List<StationIdentifierType> newStations = new ArrayList<StationIdentifierType>();
        for (Object cur : stations) {
            newStations.add((StationIdentifierType) cur);
        }
        return this.getWaveform(newStations, beginTime, endTime, filename);
    }

    public boolean getWaveform(List<StationIdentifierType> stations, String beginTime, String endTime, String filename) {
        boolean isError = false;
        DataRequestType dataRequest = new DataRequestType();
        dataRequest.setUserToken(userToken);
        TemporalBoundsType tempBound = new TemporalBoundsType();
        TimePeriodType timePeriod = new TimePeriodType();
        timePeriod.setFrame("#ISO-8601");
        TimePositionType beginPosition = new TimePositionType();
        beginPosition.setFrame("#ISO-8601");
        beginPosition.getValue().add(beginTime);
        TimePositionType endPosition = new TimePositionType();
        endPosition.setFrame("#ISO-8601");
        endPosition.getValue().add(endTime);
        timePeriod.setBeginPosition(beginPosition);
        timePeriod.setEndPosition(endPosition);
        tempBound.setTimePeriod(timePeriod);
        for (StationIdentifierType cur : stations) {
            cur.setTimeSpan(tempBound);
            dataRequest.getStationIdentifierFilter().add(cur);
            dataRequest.setDataFormat("MSEED");
        }
        System.out.print("Sending the data request...");
        DataStatusResponseType slDataResponse = binding.dataRequest(dataRequest);
        System.out.println("done.");
        System.out.print("Checking the status of the data...");
        List<String> requestId = new ArrayList<String>();
        requestId.add(slDataResponse.getRoutedRequest().get(0).getId());
        Holder<List<RoutedRequestType>> routedRequestHolder = new Holder<List<RoutedRequestType>>(slDataResponse.getRoutedRequest());
        Holder<ErrorType> errorHolder = new Holder<ErrorType>(slDataResponse.getError());
        while (!routedRequestHolder.value.get(0).getReadyFlag().equals("true")) {
            binding.checkStatus(userToken, requestId, routedRequestHolder, errorHolder);
        }
        System.out.println("done.");
        System.out.println("Retrieving the data...");
        if (routedRequestHolder.value.get(0).getStatusDescription().matches(".*Status: OK.*")) {
            List<DataItemCollectionType> dataItemCollectionList = new ArrayList<DataItemCollectionType>();
            dataItemCollectionList.add(new DataItemCollectionType());
            dataItemCollectionList.get(0).getDataItem().add(new DataItemType());
            Holder<List<DataItemCollectionType>> dataSet = new Holder<List<DataItemCollectionType>>();
            Holder<List<DataItemType>> dataItem = new Holder<List<DataItemType>>(dataItemCollectionList.get(0).getDataItem());
            binding.dataRetrieve(userToken, requestId, dataItem, dataSet, errorHolder);
            System.out.print("\tDownloading the data...");
            try {
                URL url = new URL(dataItem.value.get(0).getDownloadToken().getDownloadURL());
                URLConnection con = url.openConnection();
                BufferedInputStream in = new BufferedInputStream(con.getInputStream());
                FileOutputStream out = new FileOutputStream(filename);
                int i = 0;
                byte[] bytesIn = new byte[1024];
                while ((i = in.read(bytesIn)) >= 0) {
                    out.write(bytesIn, 0, i);
                }
                out.close();
                in.close();
            } catch (Exception e) {
                System.out.println("Error connecting to ftp.");
                System.out.println(e.getMessage());
                isError = true;
            }
            System.out.println("done.");
        } else {
            System.out.println("Status not ok: " + routedRequestHolder.value.get(0).getStatusDescription());
            isError = true;
        }
        System.out.println("done.");
        System.out.print("Purging the data...");
        List<PurgeStatusResponseType> purgeStatusResponseList = new ArrayList<PurgeStatusResponseType>();
        purgeStatusResponseList.add(new PurgeStatusResponseType());
        Holder<List<PurgeStatusResponseType>> purgeStatusResponseHolder = new Holder<List<PurgeStatusResponseType>>(purgeStatusResponseList);
        binding.purgeData(userToken, requestId, purgeStatusResponseHolder, errorHolder);
        System.out.println("done.");
        return isError;
    }
}
