package com.zl.upc_style;

import java.util.ArrayList;

import android.util.Log;

public class Upc_DBUtils {
	private ArrayList<String> arrayList = new ArrayList<String>();
	private ArrayList<String> brrayList = new ArrayList<String>();
	private Upc_HttpConnSoap Soap = new Upc_HttpConnSoap();
			
	/**
	 * 通过SN码获取整条记录的数据信息
	 * @param strSN
	 * @return
	 */
	public String IsUPCStockOutScanBySN(String strSN){
		String result = "EMPTY";
		arrayList.clear();
		brrayList.clear();
		
		arrayList.add("strSN");
		brrayList.add(strSN);
		
		String[] param = Soap.GetWebServre("IsUPCStockOutScanBySN", arrayList, brrayList).split("<");
		result = param[5].split(">")[1];
		
		return result;
		
	}
	
	/**
	 * 将扫描信息存储到远程数据库
	 * @param strSN  SN码                                                              
	 * @param strUPC UPC码
	 * @param strEntryNo 操作人用户名
	 * @param strEntryHost 操作设备名称
	 * @param strEntryIP 操作设备IP
	 * @param strAddress 地址
	 * @param strBatchNo 批次码
	 * @return
	 */
	public String AddUPCScanOutRecord(String strSN, String strUPC, String strEntryNo, String strEntryHost, String strEntryIP, String strAddress, String strBatchNo){
		String result = "EMPTY";
		arrayList.clear();
		brrayList.clear();
		
		arrayList.add("strSN");
		arrayList.add("strUPC");
		arrayList.add("strEntryNo");
		arrayList.add("strEntryHost");
		arrayList.add("strEntryIP");
		arrayList.add("strResult");//strResult or strAddress ?
		arrayList.add("strBatchNo");
		brrayList.add(strSN);
		brrayList.add(strUPC);
		brrayList.add(strEntryNo);
		brrayList.add(strEntryHost);
		brrayList.add(strEntryIP);
		brrayList.add(strAddress);
		brrayList.add(strBatchNo);
		
		String[] param = Soap.GetWebServre("AddUPCScanOutRecord", arrayList, brrayList).split("<");
		result = param[5].split(">")[1];
		
		return result;
		
	}
	
	/**
	 * 撤销指定的 SN 所标识的数据记录
	 * @param strSN SN码
	 * @param strEntryNo 操作人用户名
	 * @return
	 */
	public String DelUPCScanOutRecord(String strSN, String strEntryNo){
		String result = "EMPTY";
		arrayList.clear();
		brrayList.clear();
		
		arrayList.add("strSN");
		arrayList.add("strEntryNo");
		brrayList.add(strSN);
		brrayList.add(strEntryNo);
		
		String[] param = Soap.GetWebServre("DelUPCScanOutRecord", arrayList, brrayList).split("<");
		result = param[5].split(">")[1];
		Log.i("DelUPCScanOutRecord", result);	
		
		return result;
		
	}

}
