package com.zl.upc_style;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;

import com.zl.serviceurl.ServiceUrls;

import android.util.Log;

public class Upc_HttpConnSoap {
	public String GetWebServre(String methodName, ArrayList<String> Parameters, ArrayList<String> ParValues) {
		String Values = "NONE";
		
		String ServerUrl = ServiceUrls.BBY_SERVICE_URL;
		
		//String soapAction="http://tempuri.org/LongUserId1";
		String soapAction = "http://tempuri.org/" + methodName;
		//String data = "";
		String soap = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
				+ "<soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">"
				+ "<soap:Body>";
		String tps, vps, ts;
		String mreakString = "";

		mreakString = "<" + methodName + " xmlns=\"http://tempuri.org/\">";
		for (int i = 0; i < Parameters.size(); i++) {
			tps = Parameters.get(i).toString();
			//设置该方法的参数为.net webService中的参数名称
			vps = ParValues.get(i).toString();
			ts = "<" + tps + ">" + vps + "</" + tps + ">";
			mreakString = mreakString + ts;
		}
		mreakString = mreakString + "</" + methodName + ">";
		String soap2 = "</soap:Body>"+"</soap:Envelope>";
		String requestData = soap + mreakString + soap2;
		//System.out.println(requestData);

		try {
			URL url = new URL(ServerUrl);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			
			byte[] bytes = requestData.getBytes("utf-8");
			con.setDoInput(true);
			con.setDoOutput(true);
			con.setUseCaches(false);
			con.setConnectTimeout(6000);// 设置超时时间
			con.setRequestMethod("POST");
			con.setRequestProperty("Content-Type", "text/xml;charset=utf-8");
			con.setRequestProperty("SOAPAction", soapAction);
			con.setRequestProperty("Content-Length", "" + bytes.length);

			
			OutputStream outStream = con.getOutputStream();
			outStream.write(bytes);
			outStream.flush();
			outStream.close();
			Log.i("ResponseErrorCode",String.valueOf(con.getResponseCode()) );
			InputStream inStream = con.getInputStream();
			
			//Log.i("ResponseErrorCode",String.valueOf(con.getResponseCode()) );

			Values = inputStream2String(inStream);
			
			return Values;

		} catch (Exception e) {
			e.printStackTrace();
			System.out.print("2221");
			return Values;
		}
	}
	
	/**
	 * 将 InputStream 转化为 String
	 * @param in
	 * @return
	 * @throws IOException
	 */
	private String inputStream2String(InputStream in) throws IOException {
		// TODO Auto-generated method stub
		
		StringBuffer out = new StringBuffer();
		String s1 = "";
		byte[] b = new byte[4096];//最大支持的数据块为4M（在此例中），如果大于4M的话需要对"out"进行分析而不是"s1"
		for (int n; (n = in.read(b)) != -1;) {
			s1 = new String(b, 0, n);
			out.append(s1);
		}
		
		return s1;
	}
	
}
