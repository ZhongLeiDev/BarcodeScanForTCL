package com.zl.mitv_soap;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.zl.serviceurl.ServiceUrls;

import android.util.Log;

public class Mitv_HttpConnSoap {
	public Map<String,String> GetWebServre(String methodName, ArrayList<String> Parameters, ArrayList<String> ParValues) {
		Map<String,String> Values = new HashMap<String,String>();
		
		String ServerUrl = ServiceUrls.CMP_SERVICE_URL;
		
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

			Values = inputStream2List(inStream);
			
			return Values;

		} catch (Exception e) {
			e.printStackTrace();
			System.out.print("2221");
			return Values;
		}
	}
	
	/**
	 * 将获取的数据进行解析
	 * @param in 获取的输入流数据
	 * @return 返回Map数据类型，当数据解析值为状态值（OK/NG）或数量时，返回Map<"KEYCODE",result>,result即为返回的状态或数量
	 */
	public Map<String,String> inputStream2List(InputStream in)throws IOException{
		Map<String,String> Values = new HashMap<String,String>();
		Values.clear();
		StringBuffer out = new StringBuffer();
		String s1 = "";
		byte[] b = new byte[4096];//最大支持的数据块为4M（在此例中），如果大于4M的话需要对"out"进行分析而不是"s1"
		for (int n; (n = in.read(b)) != -1;) {
			s1 = new String(b, 0, n);
			out.append(s1);
		}
		String[ ] params = s1.split("<");//以“<“为标志位(XML文件)拆分，标志要以获得的输入流来决定
		String temp;
		if(params.length>0){
			String[ ] build = params[0].split(";");
			if(build.length>1){//当返回字符串为状态值（OK或NG）或数量时，build[ ]数组长度为1,再向下做判断
			for(int i=0;i<build.length;i++){
				temp = build[i].split(":")[0];
				Values.put(temp, build[i].replace(temp+":", ""));
			}
			}else{
				Values.put("KEYCODE", params[0]);
			}
		}
		
		return Values;
	}

	public ArrayList<String> inputStreamtovaluelist(InputStream in, String MonthsName) throws IOException {
		StringBuffer out = new StringBuffer();
		String s1 = "";
		byte[] b = new byte[4096];
		ArrayList<String> Values = new ArrayList<String>();
		Values.clear();

		for (int n; (n = in.read(b)) != -1;) {
			s1 = new String(b, 0, n);
			out.append(s1);
		}

		System.out.println(out);
		String[] s13 = s1.split("><");
		String ifString = MonthsName + "Result";
		String TS = "";
		String vs = "";

		Boolean getValueBoolean = false;
		for (int i = 0; i < s13.length; i++) {
			TS = s13[i];
			System.out.println(TS);
			int j, k, l;
			j = TS.indexOf(ifString);
			k = TS.lastIndexOf(ifString);

			if (j >= 0) {
				System.out.println(j);
				if (getValueBoolean == false) {
					getValueBoolean = true;
				} else {

				}

				if ((j >= 0) && (k > j)) {
					System.out.println("FFF" + TS.lastIndexOf("/" + ifString));
					//System.out.println(TS);
					l = ifString.length() + 1;
					vs = TS.substring(j + l, k - 2);
					//System.out.println("fff"+vs);
					Values.add(vs);
					System.out.println("退出" + vs);
					getValueBoolean = false;
					return Values;
				}

			}
			if (TS.lastIndexOf("/" + ifString) >= 0) {
				getValueBoolean = false;
				return Values;
			}
			if ((getValueBoolean) && (TS.lastIndexOf("/" + ifString) < 0) && (j < 0)) {
				k = TS.length();
				//System.out.println(TS);
				vs = TS.substring(7, k - 8);
				//System.out.println("f"+vs);
				Values.add(vs);
			}

		}

		return Values;
	}

}
