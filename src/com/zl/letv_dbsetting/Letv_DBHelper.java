package com.zl.letv_dbsetting;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class Letv_DBHelper extends SQLiteOpenHelper {

	public static final String DB_NAME = "ldm_family_letv"; // DB name
	private Context mcontext;
	private static Letv_DBHelper mDbHelper;
	private SQLiteDatabase db;

	public Letv_DBHelper(Context context) {
		super(context, DB_NAME, null, 13);
		this.mcontext = context;
	}
	
	public synchronized static Letv_DBHelper getInstance(Context context) { //单例模式获取数据库对象
		if (mDbHelper == null) { 
			mDbHelper = new Letv_DBHelper(context); 
		} 
		return mDbHelper; 
		};

	public Letv_DBHelper(Context context, String name, CursorFactory factory, int version) {
		super(context, name, factory, version);

	}

	/**
	 * 用户第一次使用软件时调用的操作，用于获取数据库创建语句（SW）,然后创建数据库
	 */
	@Override
	public void onCreate(SQLiteDatabase db) {
		String sql = "create table if not exists family_bill_letv(id integer primary key,Outbound_time text,SN text,MAC text,Batch text)";
		db.execSQL(sql);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//		db.execSQL("ALTER TABLE family_bill_letv ADD  batch TEXT DEFAULT 'NO';");
	}

	/* 打开数据库,如果已经打开就使用，否则创建 */
	public Letv_DBHelper open() {
		if (null == mDbHelper) {
			mDbHelper = new Letv_DBHelper(mcontext);
		}
		db = mDbHelper.getWritableDatabase();
		return this;
	}

	/* 关闭数据库 */
	@Override
	public void close() {
		db.close();
		mDbHelper.close();
	}

	/**添加数据 */
	public long insert(String tableName, ContentValues values) {
		return db.insert(tableName, null, values);
	}
	
	/**
	 * 删除数据库中指定SN的数据
	 * @param tableName
	 * @param SN
	 * @return
	 */
	public int delete(String tableName,String SN){
		return db.delete(tableName, "SN = ?", new String[]{SN});
	}
	
	/**
	 * 按批次号（batch）批量删除数据
	 * @param tableName 表名称
	 * @param batch 批次名称
	 * @return 影响到的行数
	 */
	public int deleteWithBatch(String tableName,String batch){
		return db.delete(tableName, "Batch = ?", new String[]{batch});
	}
	
	public int deleteAll(String tableName){
		return db.delete(tableName, "1", new String[]{ });
	}

	/**查询数据*/
	public Cursor findList(String tableName, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
		return db.query(tableName, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
	}

	public Cursor exeSql(String sql) {
		return db.rawQuery(sql, null);
	}
}