package com.zl.offline_style;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class Offline_BatchDBHelper extends SQLiteOpenHelper {

	public static final String DB_NAME = "ldm_batch_offline"; // DB name
	private Context mcontext;
	private static Offline_BatchDBHelper mBatchDbHelper;
	private SQLiteDatabase db;

	public Offline_BatchDBHelper(Context context) {
		super(context, DB_NAME, null, 13);//数据库版本号由11-->12，初始化时会调用onUpgrade方法
		this.mcontext = context;
	}
	
	public synchronized static Offline_BatchDBHelper getInstance(Context context) { //单例模式获取数据库对象
		if (mBatchDbHelper == null) { 
			mBatchDbHelper = new Offline_BatchDBHelper(context); 
		}
		return mBatchDbHelper; 
		};

	public Offline_BatchDBHelper(Context context, String name, CursorFactory factory, int version) {
		super(context, name, factory, version);

	}

	/**
	 * 用户第一次使用软件时调用的操作，用于获取数据库创建语句（SW）,然后创建数据库
	 */
	@Override
	public void onCreate(SQLiteDatabase db) {
//		String sql = "create table if not exists batch_list_offline(id integer primary key,create_time text,batch_name text,batch_size text)";
		/*---------------创建批次信息表，包含的列有：create_time（数据表创建时间）、batch_name（批次名称）、-------------------------------
		-----------------batch_size（本批次机器总量）、scan_flag（是否已经扫描完成）---------------------------------------------------------------*/
		String sql = "create table if not exists batch_list_offline(id integer primary key,create_time text,batch_name text,batch_size text,scan_flag text)";
		db.execSQL(sql);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//		db.execSQL("ALTER TABLE batch_list_offline ADD  scan_flag TEXT DEFAULT 'NO';");
	}

	/* 打开数据库,如果已经打开就使用，否则创建 */
	public Offline_BatchDBHelper open() {
		if (null == mBatchDbHelper) {
			mBatchDbHelper = new Offline_BatchDBHelper(mcontext);
		}
		db = mBatchDbHelper.getWritableDatabase();
		return this;
	}

	/* 关闭数据库 */
	@Override
	public void close() {
		db.close();
		mBatchDbHelper.close();
	}

	/**添加数据 */
	public long insert(String tableName, ContentValues values) {
		return db.insert(tableName, null, values);
	}
	
	/**
	 * 删除数据库中指定batch_name(批次号)的数据
	 * @param tableName
	 * @param batch_name
	 * @return
	 */
	public int delete(String tableName,String batch_name){
		return db.delete(tableName, "batch_name = ?", new String[]{batch_name});
	}
	
	public int deleteAll(String tableName){//删除数据库内所有数据
		return db.delete(tableName, "1", new String[]{ });
	}
	
	public int deleteUnFinished( ){//删除数据库内所有出库状态为“NO”的数据
//		return db.delete("batch_list_offline", "scan_flag = ?", new String[]{"NO"});
		return 1;
	}

	public int updateSum(String batchname,String sum){//更新数据库内的扫描总数
		ContentValues values = new ContentValues();
		values.put("batch_size",sum);
		return db.update("batch_list_offline", values, "batch_name=? ", new String[]{batchname});
	}
	
	public int updateFlag(String batchname,String flag){//更新数据库内的扫描完成标志位
		ContentValues values = new ContentValues();
		values.put("scan_flag",flag);
		return db.update("batch_list_offline", values, "batch_name=? ", new String[]{batchname});
	}
	
	/**查询数据*/
	public Cursor findList(String tableName, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
		return db.query(tableName, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
	}

	public Cursor exeSql(String sql) {
		return db.rawQuery(sql, null);
	}
}
