package com.zl.letv_dbsetting;

import java.io.File;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.util.Log;

import com.zl.common_utils.ExcelUtils;

public class Letv_DBSheetBuilder {
	private Context contex;
	private File file;
	private String[] title = {"Outbound_time", "SN", "MAC"};
	private Letv_DBHelper mDbHelper;
	private ArrayList<ArrayList<String>> mylist = new ArrayList<ArrayList<String>>();
	
	public Letv_DBSheetBuilder(Context ctx){
		this.contex = ctx;
//		mDbHelper = new DBHelper(contex);
		mDbHelper = Letv_DBHelper.getInstance(contex);//����ģʽ
		mDbHelper.open();//�����ݿ�
	}
	
	public void closeDB(){//�ر����ݿ�
		mDbHelper.close();
	}
	
	/**
	 * ����Զ�̷������ϻ�ȡ�Ľ���ȴ洢�ڱ��ط��������ٴ洢�ڱ���Excel�ļ�
	 * @param rs ��Զ�̷�������ȡ�Ĳ�ѯ���
	 * @param filename �洢��Ŀ��Excel�ļ����ļ���
	 * @return ���ش洢������洢�ɹ��򷵻�true��ʧ���򷵻�false
	 */
	public boolean saveData(ResultSet rs,String filename){
		boolean result = false;
		try{
		ContentValues values = new ContentValues();
		values.put("Outbound_time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.CHINA).format(new Date()));
		values.put("SN", rs.getString("SN"));
		values.put("MAC", rs.getString("MAC"));
		Cursor mCrusor = mDbHelper.exeSql("select * from family_bill_letv where SN = '"+rs.getString("SN")+"'");
		if(mCrusor.getCount()==0){//�����ѯ���Ϊ�գ������ݿ��ڻ�δ���������Ϣʱ���룬�����ظ�����
		long insert = mDbHelper.insert("family_bill_letv", values);
		if(insert>0){
			result = saveDataToExcelFroomDB(rs,filename);
		}
		}else{
			Log.i("INSERTDATA", "���ݿ����Ѵ��ڴ�����¼��");
			result = true;
		}
		mCrusor.close();
		}catch(SQLException e){
			e.printStackTrace();
			result = false;
		}
		return result;
	}
	
	/**
	 * �������ݿ�洢����������Ҫ���б���Excel����
	 * @param rs ֱ�����ݿ��ѯ���Ľ��
	 * @param batch ������Ϣ
	 * @return �Ƿ�洢�ɹ�
	 */
	public boolean saveDataWithBatch(ResultSet rs,String batch){
		boolean result = false;
		try{
		ContentValues values = new ContentValues();
		values.put("Outbound_time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.CHINA).format(new Date()));
		values.put("SN", rs.getString("SN"));
		values.put("MAC", rs.getString("MAC"));
		values.put("batch", batch);//д�����κ�
		Cursor mCrusor = mDbHelper.exeSql("select * from family_bill_letv where SN = '"+rs.getString("SN")+"'");
		if(mCrusor.getCount()==0){//�����ѯ���Ϊ�գ������ݿ��ڻ�δ���������Ϣʱ���룬�����ظ�����
		long insert = mDbHelper.insert("family_bill_letv", values);
		if(insert>0){
//			result = saveDataToExcelFroomDB(rs,filename);
			result = true;//�������ݿ����ʱ����Ҫ����Excel���ش洢
		}
		}else{
			Log.i("INSERTDATA", "���ݿ����Ѵ��ڴ�����¼��");
			result = true;
		}
		mCrusor.close();
		}catch(SQLException e){
			e.printStackTrace();
			result = false;
		}
		return result;
	}
	
	/**
	 * ������ͨ��WebService��ѯ���Ľ���洢���������ݿ�
	 * @param map ��ѯ���Ľ��
	 * @param batch ���κ�
	 * @return
	 */
	public boolean saveDataWithBatch_Map(Map<String,String> map,String batch){
		boolean result = false;
		ContentValues values = new ContentValues();
		values.put("Outbound_time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.CHINA).format(new Date()));
		values.put("SN", map.get("SN").replace(" ", ""));//����ո�;
		values.put("MAC", map.get("MAC").replace(" ", ""));
		values.put("batch", batch);//д�����κ�
		Cursor mCrusor = mDbHelper.exeSql("select * from family_bill_letv where SN = '"+map.get("SN").replace(" ", "")+"'");
		if(mCrusor.getCount()==0){//�����ѯ���Ϊ�գ������ݿ��ڻ�δ���������Ϣʱ���룬�����ظ�����
		long insert = mDbHelper.insert("family_bill_letv", values);
		if(insert>0){
//			result = saveDataToExcelFroomDB(rs,filename);
			result = true;//�������ݿ����ʱ����Ҫ����Excel���ش洢
		}
		}else{
			Log.i("INSERTDATA", "���ݿ����Ѵ��ڴ�����¼��");
			result = true;
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
		return mDbHelper.delete("family_bill_letv", SN);
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
		return mDbHelper.deleteWithBatch("family_bill_letv", batch);
	}
	
	/**
	 * ��Զ�̷�������ѯ�����Ľ���洢�ڱ���Excel�ļ�
	 * @param rs Զ�̷������Ĳ�ѯ���
	 * @param filename �洢Ŀ���ļ����ļ�����
	 * @return �Ƿ�洢�ɹ����洢�ɹ��򷵻�true��ʧ���򷵻�false
	 */
	public boolean saveDataToExcelFroomDB(ResultSet rs,String filename){
		boolean isSaved = false;
		
		mylist.clear();
		ArrayList<String> beanList=new ArrayList<String>();
		try{
		beanList.add(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.CHINA).format(new Date()));
		beanList.add(rs.getString("SN"));
		beanList.add(rs.getString("MAC"));
		mylist.add(beanList);
		}catch(SQLException e){
			e.printStackTrace();
			isSaved = false;
		}
		if(!mylist.isEmpty()){
			boolean save1 = saveDataToExcel(mylist,filename);
			boolean save2 = saveTotalDataToExcel(mylist);
			isSaved = save1&&save2;
			if((!save1)&&save2){
				deleteLatestRowFromTotalData();
			}else if(save1&&(!save2)){
				if(deleteLatestRowFromData(filename)){
				Log.i("DELETEROW", "ɾ���ֱ����ݳɹ���");
				}else{
					Log.i("DELETEROW", "ɾ���ֱ�����ʧ�ܣ�");
				}
			}else if((!save1)&&(!save2)){
				deleteLatestRowFromTotalData();
				deleteLatestRowFromData(filename);
			}
		}
		
		return isSaved;
	}
	
	public boolean saveDataToExcel(ArrayList<ArrayList<String>> list,String filename) {
		file = new File(getSDPath() + File.separator+"ScanResultData");
		makeDir(file);
		File f = new File(file.toString() + File.separator+filename);
		if(f.exists()){//���Ŀ���ļ��Ѵ�������ɾ���ļ��ٴ�����������ԭExcel����֮����������
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
	 * ��ȡָ��Excel������������
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
	
	public int getScannedSize(String batchname){
		Cursor mCursor = mDbHelper.exeSql("select * from family_bill_letv where batch =  '"+batchname+"'");
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