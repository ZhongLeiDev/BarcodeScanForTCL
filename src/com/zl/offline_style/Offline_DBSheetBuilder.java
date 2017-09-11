package com.zl.offline_style;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.util.Log;

import com.zl.common_utils.ExcelUtils;

public class Offline_DBSheetBuilder {
	private Context contex;
	private File file;
	private String[] title = {"Outbound_time", "SN"};
	private Offline_DBHelper mDbHelper;
	public Offline_DBSheetBuilder(Context ctx){
		this.contex = ctx;
//		mDbHelper = new DBHelper(contex);
		mDbHelper = Offline_DBHelper.getInstance(contex);//����ģʽ
		mDbHelper.open();//�����ݿ�
	}
	
	public void closeDB(){//�ر����ݿ�
		mDbHelper.close();
	}
	
	/**
	 * �������ݿ�洢����������Ҫ���б���Excel����
	 * @param rs ֱ�����ݿ��ѯ���Ľ��
	 * @param batch ������Ϣ
	 * @return �Ƿ�洢�ɹ�
	 */
	public boolean saveDataWithBatch(String SN,String batch){
		boolean result = false;
		ContentValues values = new ContentValues();
		values.put("Outbound_time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.CHINA).format(new Date()));
		values.put("SN", SN);
		values.put("batch", batch);//д�����κ�
		Cursor mCrusor = mDbHelper.exeSql("select * from family_bill_offline where SN = '"+SN+"'");
		if(mCrusor.getCount()==0){//�����ѯ���Ϊ�գ������ݿ��ڻ�δ���������Ϣʱ���룬�����ظ�����
		long insert = mDbHelper.insert("family_bill_offline", values);
		if(insert>0){
			result = true;//�������ݿ����ʱ����Ҫ����Excel���ش洢
		}
		}else{
			Log.i("INSERTDATA", "���ݿ����Ѵ��ڴ�����¼��");
			result = false;//���ݿ����Ѵ��ڴ�����¼
		}
		mCrusor.close();
		return result;
	}
	
	
	/**
	 * ��SN�����ɾ��
	 * @param str ָ����SN��
	 * @return
	 */
	public int deleteFromDB(String SN){
		return mDbHelper.delete("family_bill_offline", SN);
	}
	
	/**
	 * �����ݿ����SQL����
	 * @param sql
	 * @return
	 */
	public Cursor exeSQL(String sql){
		return mDbHelper.exeSql(sql);
	}
	
	/**
	 * �����κŽ��б������ݿ�����ɾ��
	 * @param batch ���κ�
	 * @return ��Ӱ�������
	 */
	public int deleteFromDBWithBatch(String batch){
		return mDbHelper.deleteWithBatch("family_bill_offline", batch);
	}
	
	
	public boolean saveDataToExcel(ArrayList<ArrayList<String>> list,String filename) {
		file = new File(getSDPath() + File.separator+"ScanResultData");
		makeDir(file);
		File f = new File(file.toString() + File.separator+filename);
		if(f.exists()){//���Ŀ���ļ��Ѵ�������ɾ���ļ��ٴ�����������ԭExcel���֮���������
			f.delete();
		}
		ExcelUtils.initExcel(file.toString() + File.separator+filename, title);
//		ExcelUtils.writeObjListToExcel(getBillData(), getSDPath() + "/Family/bill.xls", this);
		boolean result = ExcelUtils.writeObjListToExcel(list, getSDPath() + File.separator+"ScanResultData"+File.separator+filename, contex);
		return result;
	}
	
	public boolean saveTotalDataToExcel(ArrayList<ArrayList<String>> list) {
		file = new File(getSDPath() + File.separator+"ScanResultData"+File.separator+"Total");
		makeDir(file);
		ExcelUtils.initExcel(file.toString() + File.separator+"total.xls", title);
//		ExcelUtils.writeObjListToExcel(getBillData(), getSDPath() + "/Family/bill.xls", this);
		boolean result = ExcelUtils.writeObjListToExcel(list, getSDPath() + File.separator+"ScanResultData"+File.separator+"Total"+File.separator+"total.xls", contex);
		return result;
	}
	
	/**
	 * ��ȡָ��Excel�����������
	 * @param filename
	 * @return ��������������ȡʧ���򷵻�-10
	 */
	public int getRowsFromExcel(String filename){
//		ExcelUtils.initExcel(getSDPath() + File.separator+"ScanResultData"+File.separator+filename, title);
		File f = new File(getSDPath() + File.separator+"ScanResultData"+File.separator+filename);
		if(f.exists()){
		return ExcelUtils.getExcelRows(getSDPath() + File.separator+"ScanResultData"+File.separator+filename)-1;//����ġ�-1���Ǽ�ȥExcelͷ��title����������
		}else{
			return 0;
		}
		}
	
	/**
	 * ɾ���ܱ�������һ��
	 * @return
	 */
	public boolean deleteLatestRowFromTotalData(){//ɾ���ܱ�������һ��
		return ExcelUtils.deleteLatestRow(getSDPath() + File.separator+"ScanResultData"+File.separator+"Total"+File.separator+"total.xls");
	}
	
	/**
	 * �������κŲ�ѯ���ݿ��з�������������������
	 * @param batchname
	 * @return
	 */
	public int getScannedSize(String batchname){
		Cursor mCursor = mDbHelper.exeSql("select * from family_bill_offline where batch =  '"+batchname+"'");
		return mCursor.getCount();
	}
	
	/**
	 * ɾ���ֱ�������һ��
	 * @param filename
	 * @return
	 */
	public boolean deleteLatestRowFromData(String filename){//ɾ���ֱ�������һ��
		return ExcelUtils.deleteLatestRow( getSDPath() + File.separator+"ScanResultData"+File.separator+filename);
	}
	
	public static void makeDir(File dir) {
		if (!dir.getParentFile().exists()) {
			makeDir(dir.getParentFile());
		}
		dir.mkdir();
	}

	public static String getSDPath() {
		File sdDir = null;
		boolean sdCardExist = Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED);
		if (sdCardExist) {
			sdDir = Environment.getExternalStorageDirectory();
		}
		String dir = sdDir.toString();
		return dir;

	}
	

}
