package quotegetter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;

public class quoteGetter {
	public quoteGetter(){

	}

	private void getQuote(int tickerID, String tickerName, Calendar calend) {
		try {
			URL url = new URL("http://195.128.78.52/export9.out");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setDoOutput(true);
			DataOutputStream outStream = new DataOutputStream( conn.getOutputStream() );

			// формируем запрос
			// непонятный параметр. наверно типа защита от автозапросов :)
			String requestText = "d=d";
			// выбираем акции ММВБ
			requestText += "&market=1";
			// выбираем тикер
			requestText += "&em="+tickerID;
			// дата, месяц (начиная с 0), год даты начала запроса
			requestText += "&df="+calend.get(Calendar.DAY_OF_MONTH);
			requestText += "&mf="+calend.get(Calendar.MONTH);
			requestText += "&my="+calend.get(Calendar.YEAR);
			// дата, месяц (начиная с 0), год даты конца запроса
			requestText += "&dt="+calend.get(Calendar.DAY_OF_MONTH);
			requestText += "&mt="+calend.get(Calendar.MONTH);
			requestText += "&yt="+calend.get(Calendar.YEAR);
			// периодичность 1 час
			requestText += "&p=7";
			// имя и расширение получаемого файла
			requestText += "&f=myfile&e=.txt";
			// идентификатор тикера
			requestText += "&cn="+tickerName;
			// формат даты (дд/мм/гг) и формат времени (чч:мм)
			requestText += "&dtf=4&tmf=4";
			// выбираем показ дату начала свечи
			requestText += "&MSOR=0";
			// разделитель полей запятая
			requestText += "&sep=1";
			// разделитель разрядов пустой
			requestText += "&sep2=1";
			// формат записи в файл: все подряд
			requestText += "&datf=1";
			// не добавлять заголовок файла
			requestText += "&at=0";
			// заполнить периоды без сделок
			requestText += "&fsp=1";

			outStream.writeBytes(requestText);
			outStream.flush();

			// Get the response
			StringBuffer answer = new StringBuffer();
			BufferedReader reader = new BufferedReader(new InputStreamReader(
					conn.getInputStream()));
			String line;
			while ((line = reader.readLine()) != null) {
				answer.append(line);
				answer.append("\n");
			}
			outStream.close();
			reader.close();

			try {
				//Создаем файл для скидывания информации с флагом append
				FileWriter fstream = new FileWriter("D:\\logs\\quoteGetter\\import_"+calend.get(Calendar.DAY_OF_MONTH)+"_"+(calend.get(Calendar.MONTH) + 1)+"_"+calend.get(Calendar.YEAR)+".log", true);
				BufferedWriter out = new BufferedWriter(fstream);
				out.write(answer.toString());
				out.close();
			} catch (Exception e) {// Catch exception if any
				//System.out.println("Error: " + e.getMessage());
			}
		} catch (IOException ex) {
			//System.out.println("catched:" + ex.toString());
		}
	}

	public void getQuotes(){
		String[] tickers = {"AVAZ","AFLT","PKBA","MMBM","GAZP","GMKN","SIBN","KMAZ","CMST",
				"KSGR","LKOH","MVID","MGNT","PLZL","ROSB","HYDR","SBER","SNGS","CTLK","YKEN"};
		int[] tickerIDs = {39,29,17375,15914,16842,795,2,15544,20707,75094,8,19737,17086,17123,
				17273,20266,3,4,16052,15903};
		Calendar calend = Calendar.getInstance();
		calend.add(Calendar.DATE, -1);
		for (int i = 0; i < 20; ++i){
			getQuote(tickerIDs[i], tickers[i], calend);
		}

		DBConnector dbConn = new DBConnector();
		dbConn.importFile("D:\\logs\\quoteGetter\\import_"+calend.get(Calendar.DAY_OF_MONTH)+"_"+(calend.get(Calendar.MONTH) + 1)+"_"+calend.get(Calendar.YEAR)+".log");
	}
}