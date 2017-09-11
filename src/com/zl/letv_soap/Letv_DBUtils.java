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
	
	/*---------------------------------以下为外网查询乐视相关数据的函数----------------------------------------*/
	public boolean isOffline_Letv(){
		boolean result = false;
		
		return result;
	}
	
	public boolean isWhitebalanceOK_Letv(){
		boolean result = false;
		
		return result;
	}
	
	/**
	 * 根据给出的SN码查询乐视相关数据的信息
	 * @param SN 给出的SN码
	 * @return 返回的包含相关数据的Map数值
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
	 * 根据SN码将数据库乐视对应的表中对应记录的Outbound值置为state
	 * @param SN 扫描到的SN码
	 * @param state 需要设置的状态
	 * @return 操作是否成功，成功返回true，失败选择false
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
	 * 将数据库中所有Outbond值为state值的项中的Outbond值置为默认值(NO)
	 * @param state 需要设置的状态
	 * @return 操作是否成功，成功返回true，失败选择false
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
	 * 根据出库状态查询当前批次已扫描的数量
	 * @param state 出库状态
	 * @return 当前批次已扫描的数量
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
