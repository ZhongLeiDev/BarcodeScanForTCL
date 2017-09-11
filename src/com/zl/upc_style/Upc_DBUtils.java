package com.zl.upc_style;

import java.util.ArrayList;

import android.util.Log;

public class Upc_DBUtils {
	private ArrayList<String> arrayList = new ArrayList<String>();
	private ArrayList<String> brrayList = new ArrayList<String>();
	private Upc_HttpConnSoap Soap = new Upc_HttpConnSoap();
			
	/**
	 * ͨ��SN���ȡ������¼��������Ϣ
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
	 * ��ɨ����Ϣ�洢��Զ�����ݿ�
	 * @param strSN  SN��                                                              
	 * @param strUPC UPC��
	 * @param strEntryNo �������û���
	 * @param strEntryHost �����豸����
	 * @param strEntryIP �����豸IP
	 * @param strAddress ��ַ
	 * @param strBatchNo ������
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
	 * ����ָ���� SN ����ʶ�����ݼ�¼
	 * @param strSN SN��
	 * @param strEntryNo �������û���
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
