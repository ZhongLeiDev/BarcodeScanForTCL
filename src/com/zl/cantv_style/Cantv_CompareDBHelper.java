package com.zl.cantv_style;

import java.io.File;
import java.util.ArrayList;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Environment;

import com.zl.common_utils.ExcelUtils;

public class Cantv_CompareDBHelper extends SQLiteOpenHelper{
	public static final String DB_NAME = "ldm_family_cantvcompare"; // DB name
	private Context mcontext;
	private static Cantv_CompareDBHelper mDbHelper;
	private SQLiteDatabase db;
	private String[] title = {"SN"};
	
	public Cantv_CompareDBHelper(Context context) {
		super(context, DB_NAME, null, 13);
		this.mcontext = context;
	}
	
	public synchronized static Cantv_CompareDBHelper getInstance(Context context) { //����ģʽ��ȡ���ݿ����
		if (mDbHelper == null) { 
			mDbHelper = new Cantv_CompareDBHelper(context); 
		}
		return mDbHelper; 
		};

	public Cantv_CompareDBHelper(Context context, String name, CursorFactory factory, int version) {
		super(context, name, factory, version);

	}

	/**
	 * �û���һ��ʹ�����ʱ���õĲ��������ڻ�ȡ���ݿⴴ����䣨SW��,Ȼ�󴴽����ݿ�
	 */
	@Override
	public void onCreate(SQLiteDatabase db) {
		String sql = "create table if not exists compare_cantv(id integer primary key,SN text)";//�����Ƚ����ݿ�
		db.execSQL(sql);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//		db.execSQL("ALTER TABLE family_bill_letv ADD  batch TEXT DEFAULT 'NO';");
	}

	/* �����ݿ�,����Ѿ��򿪾�ʹ�ã����򴴽� */
	public Cantv_CompareDBHelper open() {
		if (null == mDbHelper) {
			mDbHelper = new Cantv_CompareDBHelper(mcontext);
		}
		db = mDbHelper.getWritableDatabase();
		return this;
	}

	/* �ر����ݿ� */
	@Override
	public void close() {
		db.close();
		mDbHelper.close();
	}

	/**������� */
	public long insert(String tableName, ContentValues values) {
		return db.insert(tableName, null, values);
	}
	
	/**
	 * ɾ�����ݿ���ָ��SN������
	 * @param tableName
	 * @param SN
	 * @return
	 */
	public int delete(String tableName,String SN){
		return db.delete(tableName, "SN = ?", new String[]{SN});
	}
	
	/**
	 * ɾ�����ݿ���ָ�����е���������
	 * @param tableName
	 * @return
	 */
	public int deleteAll(String tableName){
		return db.delete(tableName, "1", new String[]{ });
	}

	/**��ѯ����*/
	public Cursor findList(String tableName, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
		return db.query(tableName, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
	}

	public Cursor exeSql(String sql) {
		return db.rawQuery(sql, null);
	}
	
	public boolean saveDataToExcel(ArrayList<ArrayList<String>> list,String filename) {
		String path = Environment.getExternalStorageDirectory()+File.separator;
		File file = new File(path+"ScanResultData");
		makeDir(file);
		File f = new File(file.toString() + File.separator+filename);
		if(f.exists()){//���Ŀ���ļ��Ѵ�������ɾ���ļ��ٴ�����������ԭExcel���֮���������
			f.delete();
		}
		ExcelUtils.initExcel(file.toString() + File.separator+filename, title);
		boolean result = ExcelUtils.writeObjListToExcel(list, path + File.separator+"ScanResultData"+File.separator+filename,mcontext);
		return result;
	}
	
	public static void makeDir(File dir) {
		if (!dir.getParentFile().exists()) {
			makeDir(dir.getParentFile());
		}
		dir.mkdir();
	}

}
