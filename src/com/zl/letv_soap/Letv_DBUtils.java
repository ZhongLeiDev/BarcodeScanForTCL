package com.zl.letv_soap;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.util.Log;

public class Letv_DBUtils {
	private ArrayList<String> arrayList = new ArrayList<String>();
	private ArrayList<String> brrayList = new ArrayList<String>();
	private Letv_HttpConnSoap Soap = new Letv_HttpConnSoap();
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
	
	/*---------------------------------����Ϊ������ѯ����������ݵĺ���----------------------------------------*/
	public boolean isOffline_Letv(){
		boolean result = false;
		
		return result;
	}
	
	public boolean isWhitebalanceOK_Letv(){
		boolean result = false;
		
		return result;
	}
	
	/**
	 * ���ݸ�����SN���ѯ����������ݵ���Ϣ
	 * @param SN ������SN��
	 * @return ���صİ���������ݵ�Map��ֵ
	 */
	public Map<String,String> getMessageWithSN_Letv(String SN){
		arrayList.clear();
		brrayList.clear();
		rebuild.clear();
		
		arrayList.add("SN");
		brrayList.add(SN);
		
		rebuild = Soap.GetWebServre("QueryLetv", arrayList, brrayList);
		
		return rebuild;
	}
	
	/**
	 * ����SN�뽫���ݿ����Ӷ�Ӧ�ı��ж�Ӧ��¼��Outboundֵ��Ϊstate
	 * @param SN ɨ�赽��SN��
	 * @param state ��Ҫ���õ�״̬
	 * @return �����Ƿ�ɹ����ɹ�����true��ʧ��ѡ��false
	 */
	public boolean updateOutbondState_Letv(String SN,String state){
		boolean result = false;
		
		arrayList.clear();
		brrayList.clear();
		rebuild.clear();
		
		arrayList.add("SN");
		arrayList.add("state");
		brrayList.add(SN);
		brrayList.add(state);
		
		rebuild = Soap.GetWebServre("updateOutbondState_Letv", arrayList, brrayList);
		
		if((rebuild.get("KEYCODE")).equals("OK")){
			result = true;
		}else if((rebuild.get("KEYCODE")).equals("NG")){
			Log.i("updateOutbondState", "update failed!");
		}
		
		return result;
	}
	
	/**
	 * �����ݿ�������OutbondֵΪstateֵ�����е�Outbondֵ��ΪĬ��ֵ(NO)
	 * @param state ��Ҫ���õ�״̬
	 * @return �����Ƿ�ɹ����ɹ�����true��ʧ��ѡ��false
	 */
	public boolean setDefaultOutbondState_Letv(String state){
		boolean result = false;
		
		arrayList.clear();
		brrayList.clear();
		rebuild.clear();
		
		arrayList.add("state");
		brrayList.add(state);
		
		rebuild = Soap.GetWebServre("setDefaultOutbondState_Letv", arrayList, brrayList);
		
		if((rebuild.get("KEYCODE")).equals("OK")){
			result = true;
		}else if((rebuild.get("KEYCODE")).equals("NG")){
			Log.i("updateOutbondState", "update failed!");
		}
		
		return result;
	}
	
	/**
	 * ���ݳ���״̬��ѯ��ǰ������ɨ�������
	 * @param state ����״̬
	 * @return ��ǰ������ɨ�������
	 */
	public int getSumWithOutbondState_Letv(String state){
		int result = -1;
		
		arrayList.clear();
		brrayList.clear();
		rebuild.clear();
		
		arrayList.add("state");
		brrayList.add(state);
		
		rebuild = Soap.GetWebServre("getSumWithOutbondState_Letv ", arrayList, brrayList);
		
		if(rebuild.get("KEYCODE")!=null){
			result = Integer.parseInt(rebuild.get("KEYCODE"));
		}
		
		return result;
	}
}
