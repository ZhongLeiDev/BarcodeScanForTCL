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
		super(context, DB_NAME, null, 13);//���ݿ�汾����11-->12����ʼ��ʱ�����onUpgrade����
		this.mcontext = context;
	}
	
	public synchronized static Offline_BatchDBHelper getInstance(Context context) { //����ģʽ��ȡ���ݿ����
		if (mBatchDbHelper == null) { 
			mBatchDbHelper = new Offline_BatchDBHelper(context); 
		}
		return mBatchDbHelper; 
		};

	public Offline_BatchDBHelper(Context context, String name, CursorFactory factory, int version) {
		super(context, name, factory, version);

	}

	/**
	 * �û���һ��ʹ�����ʱ���õĲ��������ڻ�ȡ���ݿⴴ����䣨SW��,Ȼ�󴴽����ݿ�
	 */
	@Override
	public void onCreate(SQLiteDatabase db) {
//		String sql = "create table if not exists batch_list_offline(id integer primary key,create_time text,batch_name text,batch_size text)";
		/*---------------����������Ϣ�����������У�create_time�����ݱ���ʱ�䣩��batch_name���������ƣ���-------------------------------
		-----------------batch_size�������λ�����������scan_flag���Ƿ��Ѿ�ɨ����ɣ�---------------------------------------------------------------*/
		String sql = "create table if not exists batch_list_offline(id integer primary key,create_time text,batch_name text,batch_size text,scan_flag text)";
		db.execSQL(sql);
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
//		db.execSQL("ALTER TABLE batch_list_offline ADD  scan_flag TEXT DEFAULT 'NO';");
	}

	/* �����ݿ�,����Ѿ��򿪾�ʹ�ã����򴴽� */
	public Offline_BatchDBHelper open() {
		if (null == mBatchDbHelper) {
			mBatchDbHelper = new Offline_BatchDBHelper(mcontext);
		}
		db = mBatchDbHelper.getWritableDatabase();
		return this;
	}

	/* �ر����ݿ� */
	@Override
	public void close() {
		db.close();
		mBatchDbHelper.close();
	}

	/**������� */
	public long insert(String tableName, ContentValues values) {
		return db.insert(tableName, null, values);
	}
	
	/**
	 * ɾ�����ݿ���ָ��batch_name(���κ�)������
	 * @param tableName
	 * @param batch_name
	 * @return
	 */
	public int delete(String tableName,String batch_name){
		return db.delete(tableName, "batch_name = ?", new String[]{batch_name});
	}
	
	public int deleteAll(String tableName){//ɾ�����ݿ�����������
		return db.delete(tableName, "1", new String[]{ });
	}
	
	public int deleteUnFinished( ){//ɾ�����ݿ������г���״̬Ϊ��NO��������
//		return db.delete("batch_list_offline", "scan_flag = ?", new String[]{"NO"});
		return 1;
	}

	public int updateSum(String batchname,String sum){//�������ݿ��ڵ�ɨ������
		ContentValues values = new ContentValues();
		values.put("batch_size",sum);
		return db.update("batch_list_offline", values, "batch_name=? ", new String[]{batchname});
	}
	
	public int updateFlag(String batchname,String flag){//�������ݿ��ڵ�ɨ����ɱ�־λ
		ContentValues values = new ContentValues();
		values.put("scan_flag",flag);
		return db.update("batch_list_offline", values, "batch_name=? ", new String[]{batchname});
	}
	
	/**��ѯ����*/
	public Cursor findList(String tableName, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy, String limit) {
		return db.query(tableName, columns, selection, selectionArgs, groupBy, having, orderBy, limit);
	}

	public Cursor exeSql(String sql) {
		return db.rawQuery(sql, null);
	}
}
