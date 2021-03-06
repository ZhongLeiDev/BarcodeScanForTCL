package com.zl.mitv_dbsetting;

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

public class Mitv_DBSheetBuilder {
	private Context contex;
	private File file;
	private String[] title = {"出货时间", "整机SN", "有线网MAC","生产日期","栈板号"};
	private Mitv_DBHelper mDbHelper;
	private ArrayList<ArrayList<String>> mylist = new ArrayList<ArrayList<String>>();
	
	public Mitv_DBSheetBuilder(Context ctx){
		this.contex = ctx;
//		mDbHelper = new DBHelper(contex);
		mDbHelper = Mitv_DBHelper.getInstance(contex);//单例模式
		mDbHelper.open();//打开数据库
	}
	
	public void closeDB(){//关闭数据库
		mDbHelper.close();
	}
	
	/**
	 * 将从远程服务器上获取的结果先存储在本地服务器，再存储在本地Excel文件
	 * @param rs 从远程服务器获取的查询结果
	 * @param filename 存储的目的Excel文件的文件名
	 * @return 返回存储结果，存储成功则返回true，失败则返回false
	 */
	public boolean saveData(ResultSet rs,String filename){
		boolean result = false;
		try{
		ContentValues values = new ContentValues();
		values.put("Outbound_time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.CHINA).format(new Date()));
		values.put("SN", rs.getString("SN"));
		values.put("MAC", rs.getString("MAC"));
		Cursor mCrusor = mDbHelper.exeSql("select * from family_bill_mitv where SN = '"+rs.getString("SN")+"'");
		if(mCrusor.getCount()==0){//如果查询结果为空，即数据库内还未插入此条信息时插入，避免重复插入
		long insert = mDbHelper.insert("family_bill_mitv", values);
		if(insert>0){
			result = saveDataToExcelFroomDB(rs,filename);
		}
		}else{
			Log.i("INSERTDATA", "数据库中已存在此条记录！");
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
	 * 本地数据库存储操作，不需要进行本地Excel操作
	 * @param rs 直连数据库查询到的结果
	 * @param batch 批次信息
	 * @return 是否存储成功
	 */
	public boolean saveDataWithBatch(ResultSet rs,String batch){
		boolean result = false;
		try{
		ContentValues values = new ContentValues();
		values.put("Outbound_time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.CHINA).format(new Date()));
		values.put("SN", rs.getString("SN"));
		values.put("MAC", rs.getString("MAC"));
		values.put("batch", batch);//写入批次号
		Cursor mCrusor = mDbHelper.exeSql("select * from family_bill_mitv where SN = '"+rs.getString("SN")+"'");
		if(mCrusor.getCount()==0){//如果查询结果为空，即数据库内还未插入此条信息时插入，避免重复插入
		long insert = mDbHelper.insert("family_bill_mitv", values);
		if(insert>0){
//			result = saveDataToExcelFroomDB(rs,filename);
			result = true;//本地数据库操作时不需要进行Excel本地存储
		}
		}else{
			Log.i("INSERTDATA", "数据库中已存在此条记录！");
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
	 * 将外网通过WebService查询到的结果存储到本地数据库
	 * @param map 查询到的结果
	 * @param batch 批次号
	 * @return
	 */
	public boolean saveDataWithBatch_Map(Map<String,String> map,String batch){
		boolean result = false;
		ContentValues values = new ContentValues();
		values.put("Outbound_time", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.CHINA).format(new Date()));
		values.put("SN", map.get("TVSN").replace(" ", ""));//清除空格;
		values.put("MAC", map.get("ETHERNETMAC").replace(" ", ""));
		values.put("Production_time", map.get("TESTDATE"));
		values.put("batch", batch);//写入批次号
		values.put("XMPaperCard", map.get("XMPAPERCARD").replace(" ", ""));//写入栈板号
		Cursor mCrusor = mDbHelper.exeSql("select * from family_bill_mitv where SN = '"+map.get("TVSN").replace(" ", "")+"'");
		if(mCrusor.getCount()==0){//如果查询结果为空，即数据库内还未插入此条信息时插入，避免重复插入
		long insert = mDbHelper.insert("family_bill_mitv", values);
		if(insert>0){
//			result = saveDataToExcelFroomDB(rs,filename);
			result = true;//本地数据库操作时不需要进行Excel本地存储
		}
		}else{
			Log.i("INSERTDATA", "数据库中已存在此条记录！");
			result = false;
		}
		mCrusor.close();
		return result;
	}
	
	/**
	 * 按SN码进行删除
	 * @param str 指定的SN码
	 * @return
	 */
	public int deleteFromDB(String SN){
		return mDbHelper.delete("family_bill_mitv", SN);
	}
	
	/**
	 * 对数据库进行SQL操作
	 * @param sql
	 * @return
	 */
	public Cursor exeSQL(String sql){
		return mDbHelper.exeSql(sql);
	}
	
	/**
	 * 按批次号进行本地数据库批量删除
	 * @param batch 批次号
	 * @return 受影响的行数
	 */
	public int deleteFromDBWithBatch(String batch){
		return mDbHelper.deleteWithBatch("family_bill_mitv", batch);
	}
	
	/**
	 * 将远程服务器查询出来的结果存储在本地Excel文件
	 * @param rs 远程服务器的查询结果
	 * @param filename 存储目的文件的文件名称
	 * @return 是否存储成功，存储成功则返回true，失败则返回false
	 */
	public boolean saveDataToExcelFroomDB(ResultSet rs,String filename){
		boolean isSaved = false;
		
		mylist.clear();
		ArrayList<String> beanList=new ArrayList<String>();
		try{
		beanList.add(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss",Locale.CHINA).format(new Date()));
		beanList.add(rs.getString("SN"));
		beanList.add(rs.getString("MAC"));
		beanList.add(rs.getString("Production_time"));
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
				Log.i("DELETEROW", "删除分表数据成功！");
				}else{
					Log.i("DELETEROW", "删除分表数据失败！");
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
		if(f.exists()){//如果目标文件已存在则先删除文件再创建，避免在原Excel表格之后添加数据
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
	 * 获取指定Excel表格数据行数
	 * @param filename
	 * @return 返回行数，若读取失败则返回-10
	 */
	public int getRowsFromExcel(String filename){
//		ExcelUtils.initExcel(getSDPath() + File.separator+"ScanResultData"+File.separator+filename, title);
		File f = new File(getSDPath() + File.separator+"ScanResultData"+File.separator+filename);
		if(f.exists()){
		return ExcelUtils.getExcelRows(getSDPath() + File.separator+"ScanResultData"+File.separator+filename)-1;//这里的“-1”是减去Excel头“title”的数据行
		}else{
			return 0;
		}
		}
	
	/**
	 * 删除总表里的最后一行
	 * @return
	 */
	public boolean deleteLatestRowFromTotalData(){//删除总表里的最后一行
		return ExcelUtils.deleteLatestRow(getSDPath() + File.separator+"ScanResultData"+File.separator+"Total"+File.separator+"total.xls");
	}
	
	public int getScannedSize(String batchname){
		Log.i("BATCHNAME", batchname);
		Cursor mCursor = mDbHelper.exeSql("select * from family_bill_mitv where batch =  '"+batchname+"'");
		return mCursor.getCount();
	}
	
	/**
	 * 删除分表里的最后一行
	 * @param filename
	 * @return
	 */
	public boolean deleteLatestRowFromData(String filename){//删除分表里的最后一行
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
