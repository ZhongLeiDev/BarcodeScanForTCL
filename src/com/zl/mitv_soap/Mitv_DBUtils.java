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
	
	/*---------------------------------以下为外网查询小米相关数据的函数----------------------------------------*/
	public boolean isOffline_Mitv(){
		boolean result = false;
		
		return result;
	}
	
	public boolean isWhitebalanceOK_Mitv(){
		boolean result = false;
		
		return result;
	}
	
	/**
	 * 根据给出的SN码查询小米相关数据的信息
	 * @param SN 给出的SN码
	 * @return 返回的包含相关数据的Map数值
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
	 * 根据SN码将数据库小米对应的表中对应记录的Outbound值置为state
	 * @param SN 扫描到的SN码
	 * @param state 需要设置的状态
	 * @return 操作是否成功，成功返回true，失败选择false
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
	 * 将数据库中所有Outbond值为state值的项中的Outbond值置为默认值(NO)
	 * @param state 需要设置的状态
	 * @return 操作是否成功，成功返回true，失败选择false
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
	 * 根据出库状态查询当前批次已扫描的数量
	 * @param state 出库状态
	 * @return 当前批次已扫描的数量
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
	 * 根据 tvsn 获取对应的栈板号
	 * @param tvsn
	 * @return 操作是否成功，成功返回栈板号 , 失败返回 NG
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
