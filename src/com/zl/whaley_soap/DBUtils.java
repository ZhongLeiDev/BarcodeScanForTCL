package com.zl.whaley_soap;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.util.Log;

public class DBUtils {
	private ArrayList<String> arrayList = new ArrayList<String>();
	private ArrayList<String> brrayList = new ArrayList<String>();
	private HttpConnSoap Soap = new HttpConnSoap();
	private Map<String,String> rebuild = new HashMap<String,String>();

	public static Connection getConnection() {
		Connection con = null;
		try {
			//Class.forName("org.gjt.mm.mysql.Driver");
			//con=DriverManager.getConnection("jdbc:mysql://192.168.0.106:3306/test?useUnicode=true&characterEncoding=UTF-8","root","initial");  		    
		} catch (Exception e) {
			//e.printStackTrace();
		}
		return con;
	}
	
	/*------------------------------------���º���Ϊ������ѯ΢��������ݵĺ���----------------------------------*/
	/**
	 * ����SN���ѯ΢������
	 * @return SN���Ӧ��һ������
	 */
	public Map<String,String> getMessageWithSN(String SN){
		arrayList.clear();
		brrayList.clear();
		rebuild.clear();
		
		arrayList.add("SN");
		brrayList.add(SN);
		
		rebuild = Soap.GetWebServre("QureryWhaley", arrayList, brrayList);
		
		return rebuild;
	}
	
	/**
	 * ���ݳ���״̬��ѯ��ǰ������ɨ�������
	 * @param state ����״̬
	 * @return ��ǰ������ɨ�������
	 */
	public int getSumWithOutbondState(String state){
		int result = -1;
		
		arrayList.clear();
		brrayList.clear();
		rebuild.clear();
		
		arrayList.add("state");
		brrayList.add(state);
		
		rebuild = Soap.GetWebServre("getSumWithOutbondState ", arrayList, brrayList);
		
		if(rebuild.get("KEY")!=null){
			result = Integer.parseInt(rebuild.get("KEY"));
		}
		
		return result;
	}
	
	/**
	 * ����SN�뽫���ݿ��ж�Ӧ��¼��Outboundֵ��Ϊstate
	 * @param SN ɨ�赽��SN��
	 * @param state ��Ҫ���õ�״̬
	 * @return �����Ƿ�ɹ����ɹ�����true��ʧ��ѡ��false
	 */
	public boolean updateOutbondState(String SN,String state){
		boolean result = false;
		
		arrayList.clear();
		brrayList.clear();
		rebuild.clear();
		
		arrayList.add("SN");
		arrayList.add("state");
		brrayList.add(SN);
		brrayList.add(state);
		
		rebuild = Soap.GetWebServre("updateOutbondState ", arrayList, brrayList);
		
		if((rebuild.get("KEY")).equals("OK")){
			result = true;
		}else if((rebuild.get("KEY")).equals("NG")){
			Log.i("updateOutbondState", "update failed!");
		}
		
		return result;
	}

	/**
	 * �����ݿ�������OutbondֵΪstateֵ�����е�Outbondֵ��ΪĬ��ֵ(NO)
	 * @param state ��Ҫ���õ�״̬
	 * @return �����Ƿ�ɹ����ɹ�����true��ʧ��ѡ��false
	 */
	public boolean setDefaultOutbondState(String state) {
		// TODO Auto-generated method stub
		boolean result = false;
		
		arrayList.clear();
		brrayList.clear();
		rebuild.clear();
		
		arrayList.add("state");
		brrayList.add(state);
		
		rebuild = Soap.GetWebServre("setDefaultOutbondState", arrayList, brrayList);
		
		if((rebuild.get("KEYCODE")).equals("OK")){
			result = true;
		}else if((rebuild.get("KEYCODE")).equals("NG")){
			Log.i("updateOutbondState", "update failed!");
		}
		
		return result;
	}
	
}
