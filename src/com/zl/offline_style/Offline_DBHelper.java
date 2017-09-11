package com.zl.offline_style;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteOpenHelper;

public class Offline_DBHelper extends SQLiteOpenHelper {

	public static final String DB_NAME = "ldm_family_offline"; // DB name
	private Context mcontext;
	private static Offline_DBHelper mDbHelper;
	private SQLiteDatabase db;

	public Offline_DBHelper(Context context) {
		super(context, DB_NAME, null, 13);
		this.mcontext = context;
	}
	
	public synchronized static Offline_DBHelper getInstance(Context context) { //����ģʽ��ȡ���ݿ����
		if (mDbHelper == null) { 
			mDbHelper = new Offline_DBHelper(context); 
		}
		return mDbHelper; 
		};

	public Offline_DBHelper(Context context, String name, CursorFactory factory, int version) {
		super(context, name, factory, version);

	}

	/**
	 * �û���һ��ʹ�����ʱ���õĲ��������ڻ�ȡ���ݿⴴ����䣨SW��,Ȼ�󴴽����ݿ�
	 */
	@Override
	public void onCreate(SQLiteDatabase db) {
		String sql = "create table if not exists family_bill_offline(id integer primary key,Outbound_time text,SN text,MAC text,Batch text)";
		db.execSQL(sql);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//		db.execSQL("ALTER TABLE family_bill_offline ADD  batch TEXT DEFAULT 'NO';");
	}

	/* �����ݿ�,����Ѿ��򿪾�ʹ�ã����򴴽� */
	public Offline_DBHelper open() {
		if (null == mDbHelper) {
			mDbHelper = new Offline_DBHelper(mcontext);
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
	 * �����κţ�batch������ɾ������
	 * @param tableName ������
	 * @param batch ��������
	 * @return Ӱ�쵽������
	 */
	public int deleteWithBatch(String tableName,String batch){
		return db.delete(tableName, "Batch = ?", new String[]{batch});
	}
	
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
}