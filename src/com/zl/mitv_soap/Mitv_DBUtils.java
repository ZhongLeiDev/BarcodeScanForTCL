package com.zl.mitv_soap;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import android.util.Log;

public class Mitv_DBUtils {
	private ArrayList<String> arrayList = new ArrayList<String>();
	private ArrayList<String> brrayList = new ArrayList<String>();
	private Mitv_HttpConnSoap Soap = new Mitv_HttpConnSoap();
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
	
	/*---------------------------------����Ϊ������ѯС��������ݵĺ���----------------------------------------*/
	public boolean isOffline_Mitv(){
		boolean result = false;
		
		return result;
	}
	
	public boolean isWhitebalanceOK_Mitv(){
		boolean result = false;
		
		return result;
	}
	
	/**
	 * ���ݸ�����SN���ѯС��������ݵ���Ϣ
	 * @param SN ������SN��
	 * @return ���صİ���������ݵ�Map��ֵ
	 */
	public Map<String,String> getMessageWithSN_Mitv(String SN){
		arrayList.clear();
		brrayList.clear();
		rebuild.clear();
		
		arrayList.add("tvsn");
		brrayList.add(SN);
		
		rebuild = Soap.GetWebServre("QueryMitv", arrayList, brrayList);
		
		return rebuild;
	}
	
	/**
	 * ����SN�뽫���ݿ�С�׶�Ӧ�ı��ж�Ӧ��¼��Outboundֵ��Ϊstate
	 * @param SN ɨ�赽��SN��
	 * @param state ��Ҫ���õ�״̬
	 * @return �����Ƿ�ɹ����ɹ�����true��ʧ��ѡ��false
	 */
	public boolean updateOutbondState_Mitv(String SN,String state){
		boolean result = false;
		
		arrayList.clear();
		brrayList.clear();
		rebuild.clear();
		
		arrayList.add("tvsn");
		arrayList.add("state");
		brrayList.add(SN);
		brrayList.add(state);
		
		rebuild = Soap.GetWebServre("updateOutbondState_Mitv", arrayList, brrayList);
		
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
	public boolean setDefaultOutbondState_Mitv(String state){
		boolean result = false;
		
		arrayList.clear();
		brrayList.clear();
		rebuild.clear();
		
		arrayList.add("shipmentstate");
		brrayList.add(state);
		
		rebuild = Soap.GetWebServre("setDefaultOutbondState_Mitv", arrayList, brrayList);
		
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
	public int getSumWithOutbondState_Mitv(String state){
		int result = -1;
		
		arrayList.clear();
		brrayList.clear();
		rebuild.clear();
		
		arrayList.add("shipmentstate");
		brrayList.add(state);
		
		rebuild = Soap.GetWebServre("getSumWithOutBondState_Mitv", arrayList, brrayList);
		
		if(rebuild.get("KEYCODE")!=null){
			result = Integer.parseInt(rebuild.get("KEYCODE"));
		}
		
		return result;
	}
	
	/**
	 * ���� tvsn ��ȡ��Ӧ��ջ���
	 * @param tvsn
	 * @return �����Ƿ�ɹ����ɹ�����ջ��� , ʧ�ܷ��� NG
	 */
	public String getXMPaperCard(String tvsn){
		String result = "EMPTY";
		Map<String,String> map = new HashMap<String,String>();
		
		arrayList.clear();
		brrayList.clear();
		
		arrayList.add("tvsn");
		brrayList.add(tvsn);
		
		map = Soap.GetWebServre("getXMPaperCard", arrayList, brrayList);
		
		if((map.get("KEYCODE")).equals("NG")){
			result = "NG";
		} else {
			result = map.get("KEYCODE");
		}
		
		return result;
	}
	
}
