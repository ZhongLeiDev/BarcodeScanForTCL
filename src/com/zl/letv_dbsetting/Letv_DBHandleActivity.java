package com.zl.letv_dbsetting;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.zkc.barcodescan.R;
import com.zl.common_showlist.QueryResult;
import com.zl.common_showlist.QueryResultAdapter;
import com.zl.common_utils.ExcelUtils;
import com.zl.common_utils.StringTransUtils;
import com.zl.letv_soap.Letv_DBUtils;
import com.zl.letv_dbsetting.Letv_ConfigActivity;

public class Letv_DBHandleActivity extends Activity{
	private Letv_DBHelper mDbHelper;
	private ListView lv;
	private TextView edtstart,edtstop,date,tvshowdetail;
	private Button btnquery,btnclear;
	private List<QueryResult> queryresultlist = new ArrayList<QueryResult>();
	private QueryResultAdapter ada;
	private boolean isstart = false;
	private boolean isqueryandrebuild = false;
	private File file;
	private String excelpath = Letv_DBSheetBuilder.getSDPath() + File.separator+"ScanResultData"+File.separator+"Query"+File.separator+"letv_query.xls";
	private String[] title = {"Outbound_time", "SERIAL_NUMBER", "MAC"};//���Ӵ洢Excel���ͷ
	private ArrayList<ArrayList<String>> mylist = new ArrayList<ArrayList<String>>();//���ݴ洢�б�
	private static final String Properfile = Environment.getExternalStorageDirectory()+ File.separator+"ScanResultData"+File.separator+"Config"+File.separator+"letv_config.dat";//��ϵ���б�
	private String[ ] receiver = new String[ ]{ };
	private String batchname = "L123";
	private ProgressDialog mydialog;
	
	private Letv_BatchDBSheetBuilder batchbuilder;
	private String[ ] batchunlock = new String[]{};
	
	private Letv_DBUtils dbUtils = new Letv_DBUtils();
	
	private Handler mhandler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			if(msg.what == 0x01){
				ada.notifyDataSetChanged();
				tvshowdetail.setText("���ݿ��ʼ����ѯ�ɹ���ֻ��ʾǰ100�����ݣ�");
				tvshowdetail.setTextColor(Color.BLACK);
			}else if(msg.what == 0x02){
				ada.notifyDataSetChanged();
				tvshowdetail.setText("�Ѳ�ѯ��"+String.valueOf(msg.obj)+"�����ݣ�");
				tvshowdetail.setTextColor(Color.BLACK);
				
				if(isqueryandrebuild){//�����ݴ洢�ŵ����߳̽��У���ʵЧ�������
					if(saveQueryDataToExcel(mylist)){//Excel���ɳɹ�
						Message msg1 = Message.obtain(mhandler);
						msg1.what = 0x03;
						msg1.sendToTarget();
					}else{
						Message msg1 = Message.obtain(mhandler);
						msg1.what = 0x04;
						msg1.sendToTarget();
					}
					}
				
			}else if(msg.what == 0x03){
				tvshowdetail.append("Excel���ɳɹ���");
				tvshowdetail.setTextColor(Color.GREEN);
			}else if(msg.what == 0x04){
				tvshowdetail.append("Excel����ʧ�ܣ�");
				tvshowdetail.setTextColor(Color.RED);
			}else if(msg.what == 0x05){
				tvshowdetail.setText(batchname+"���δ洢�ɹ���");
				tvshowdetail.setTextColor(Color.GREEN);
				mydialog.cancel();
			}else if(msg.what == 0x06){
				tvshowdetail.setText(batchname+"���δ洢ʧ�ܣ�");
				tvshowdetail.setTextColor(Color.RED);
				mydialog.cancel();
			}else if(msg.what == 0x07){
				tvshowdetail.setText(batchname+"���β�ѯ���Ϊ�գ�");
				tvshowdetail.setTextColor(Color.RED);
				mydialog.cancel();
			}else if(msg.what == 0x10){
				tvshowdetail.setText("��������˳����ɣ�");
				tvshowdetail.setTextColor(Color.GREEN);
				mydialog.cancel();
			}else if(msg.what == 0x11){
				tvshowdetail.setText("Զ�����ݿ����ʧ�ܣ�");
				tvshowdetail.setTextColor(Color.RED);
				mydialog.cancel();
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.database_layout);
		mDbHelper = new Letv_DBHelper(this);
		mDbHelper.open();//�����ݿ�
		
		batchbuilder = new Letv_BatchDBSheetBuilder(this);
		batchunlock = batchbuilder.getBatchList();
		
		//--------------------------�ȴ���---------------------------
				mydialog = new ProgressDialog(this);
				mydialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
				mydialog.setTitle("�ȴ��������");
				mydialog.setMessage("���ڴ������Ժ�...");
				mydialog.setCancelable(true);
		
		lv = (ListView)findViewById(R.id.listView1);
		edtstart = (TextView)findViewById(R.id.start);
		edtstop = (TextView)findViewById(R.id.stop);
		tvshowdetail = (TextView)findViewById(R.id.tvshowdetail);
		btnquery = (Button)findViewById(R.id.query);
		btnclear = (Button)findViewById(R.id.clear);
		edtstart.setOnClickListener(click);
		edtstop.setOnClickListener(click);
		btnquery.setOnClickListener(click);
		btnclear.setOnClickListener(click);
		
//		btnquery.setVisibility(View.GONE);//���ز�ѯ��ť����ʾ��հ�ť
		
		queryresultlist.add(new QueryResult("����ʱ��", "SN��"));
		ada = new QueryResultAdapter(this, queryresultlist);
		lv.setAdapter(ada);
		
		isstart = true;
		new Thread(queryRunnable).start();
		
		File pf = new File(Properfile);//��ʼ��properfile�ļ�
		if(pf.exists()){
		Properties properties = Letv_ConfigActivity.loadConfig(this, Properfile);
		String str = (String)properties.get("members");
		if(!str.equals("L")){//�������л�ʱ���ԡ�L��Ϊ��ʼλ���ʵ�list����Ϊ��ʱ�����л�ʱ��Ȼ����ڡ�L������ַ�
		List<Object> templist = StringTransUtils.StringToList(str);
		receiver = new String[templist.size()];
		for(int i=0;i<templist.size();i++){
			receiver[i]=(String)templist.get(i);
		}
		}
		}
	
	}
	
	private OnClickListener click = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if(v == btnquery){
				if((btnquery.getText().toString().equals("��ѯ������洢"))||(btnquery.getText().toString().equals("����ѯ���"))){
				if((!edtstart.getText().toString().equals("��ʼʱ��"))&&(!edtstop.getText().toString().equals("����ʱ��"))){
					if((edtstart.getText().toString()+" 00:00:00").compareTo(edtstop.getText().toString()+" 23:59:59")<0){//�ж�starttime�Ƿ�С��stoptime
						new Thread(queryRunnable).start();
					}else{
						Log.i("TIMECOMPARE", "��ʼʱ����ڻ���ڽ���ʱ�䣡");
						tvshowdetail.setText("��ʼʱ����ڻ���ڽ���ʱ�䣡");
						tvshowdetail.setTextColor(Color.RED);
					}
				}else{
					Log.i("TIMEEDIT", "��д��ʱ��β�������");
					tvshowdetail.setText("��д��ʱ��β�������");
					tvshowdetail.setTextColor(Color.RED);
				}
				}else if(btnquery.getText().toString().equals("�����ʼ�")){
					sendEmail();
				}else if(btnquery.getText().toString().equals("���β�ѯ���洢")){
					showBatchQueryDialog();
				}else if(btnquery.getText().toString().equals("���γ���")){
					showRevocationDialog();
				}
			}else if(v == btnclear){
				//-------------------------------->���û����أ����������<-------------------------------------
//				int number = mDbHelper.deleteAll("family_bill_letv");
//				Toast.makeText(DatabaseHandle.this, "ɾ�����ݣ�"+number, Toast.LENGTH_SHORT).show();
			}else if(v == edtstart){
				date = edtstart;
				getDialog().show();
			}else if(v == edtstop){
				date = edtstop;
				getDialog().show(); 
			}
			
		}
	};
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {//�˵�ѡ��
		// TODO Auto-generated method stub
		menu.add(0, 1, 1, R.string.query_1);
		menu.add(0, 2, 2, R.string.query_2);
		menu.add(0, 3, 3, R.string.query_3);
		menu.add(0, 4, 4, R.string.query_4);
		menu.add(0, 5, 5, R.string.query_5);
		return super.onCreateOptionsMenu(menu);

	}
	
	@Override
	public boolean onOptionsItemSelected(android.view.MenuItem item) {
		if (item.getItemId() == 1) {
			isqueryandrebuild = false;
			btnquery.setText("����ѯ���");
		} else if (item.getItemId() == 2) {
		isqueryandrebuild = true;
		btnquery.setText("��ѯ������洢");
		initExcelFile();
		} else if (item.getItemId() == 3) {
			btnquery.setText("�����ʼ�");
		} else if (item.getItemId() == 4) {
			btnquery.setText("���β�ѯ���洢");
		} else if (item.getItemId() == 5) {
			btnquery.setText("���γ���");
		}
		return super.onOptionsItemSelected(item);
	}
	
	private void sendEmail(){//�����ʼ�
    	Intent email = new Intent(android.content.Intent.ACTION_SEND);  
    	// ����    
    	 File file = new File(excelpath);
    	 Log.i("FilePath",file.getAbsolutePath());
    	 if(file.exists()){
    	//�ʼ��������ͣ����������ʼ�  
    	email.setType("application/octet-stream");
    	 //�ʼ������ߣ����飬�����Ƕ�λ�����ߣ�  
//    	String[] emailReciver = new String[]{"zhonglei@tcl.com"};
    	String[] emailReciver = receiver;
    	
    	if(receiver.length != 0){
    	  
    	String  emailTitle = "����ɨ�����б�";  
    	String emailContent = "����Ϊ����ɨ��Ľ���������ա�";  
    	//�����ʼ���ַ  
    	email.putExtra(android.content.Intent.EXTRA_EMAIL, emailReciver);  
    	//�����ʼ�����  
    	 email.putExtra(android.content.Intent.EXTRA_SUBJECT, emailTitle);  
    	//���÷��͵�����  
    	email.putExtra(android.content.Intent.EXTRA_TEXT, emailContent);  
    	//����  
    	email.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));  
    	 //����ϵͳ���ʼ�ϵͳ  
    	startActivity(Intent.createChooser(email, "��ѡ���ʼ��������"));
    	}else{
    		Toast.makeText(Letv_DBHandleActivity.this, "�ռ���δָ����", Toast.LENGTH_SHORT).show();
    	}
    	 }else{
    		 Toast.makeText(Letv_DBHandleActivity.this, "ָ�����������ڣ����Ȳ�ѯ���ɱ����ٷ��ͣ�", Toast.LENGTH_SHORT).show();
    	 }
    }
	
	private Runnable queryRunnable = new Runnable(){

		@Override
		public void run() {
			// TODO Auto-generated method stub
			queryresultlist.clear();
			queryresultlist.add(new QueryResult("����ʱ��", "SN��"));
			if(isstart){
				Cursor cur = mDbHelper.exeSql("select * from family_bill_letv limit 100");
				while (cur.moveToNext()) {
					QueryResult qr = new QueryResult(cur.getString(1), cur.getString(2));
					queryresultlist.add(qr);
				}
				
				isstart = false;
				cur.close();
//				ada.notifyDataSetChanged();
				Message msg = Message.obtain(mhandler);
				msg.what = 0x01;
				msg.sendToTarget();
			}else{
				if(isqueryandrebuild){
				mylist.clear();//��մ洢�б�
				}
				Cursor cur = mDbHelper.exeSql("select * from family_bill_letv where Outbound_time > '"+edtstart.getText().toString()+" 00:00:00"+"' and Outbound_time < '"+edtstop.getText().toString()+" 23:59:59"+"'");
				int number = cur.getCount();
				while (cur.moveToNext()) {
					QueryResult qr = new QueryResult(cur.getString(1), cur.getString(2));
					queryresultlist.add(qr);
					if(isqueryandrebuild){
					setDataToCell(cur);
					}
				}
				cur.close();
//				ada.notifyDataSetChanged();
				Message msg = Message.obtain(mhandler);
				msg.what = 0x02;
				msg.obj = number;
				msg.sendToTarget();
				Log.i("QUERYRESULT", "�ѻ�ȡ��ѯ�����");
//				if(isqueryandrebuild){
//				if(saveQueryDataToExcel(mylist)){//Excel���ɳɹ�
//					Message msg1 = Message.obtain(mhandler);
//					msg1.what = 0x03;
//					msg1.sendToTarget();
//				}else{
//					Message msg1 = Message.obtain(mhandler);
//					msg1.what = 0x04;
//					msg1.sendToTarget();
//				}
//				}
			}
			
			Log.i("QueryResultList", queryresultlist.toString());
			
		}
		
	};
	
	private Runnable batchQuery = new Runnable(){

		@Override
		public void run() {
			// TODO Auto-generated method stub
			Cursor cur = mDbHelper.exeSql("select * from family_bill_letv where batch = '"+batchname+"'");
			int count = cur.getCount();
			if(count>0){
				mylist.clear();
			while(cur.moveToNext()){
				setDataToCell(cur);
			}
			if(saveBatchDataToExcel(mylist)){
				Message msg = Message.obtain(mhandler);
				msg.what = 0x05;//���δ洢�ɹ�
				msg.obj = count;
				msg.sendToTarget();
			}else{
				Message msg = Message.obtain(mhandler);
				msg.what = 0x06;//���δ洢ʧ��
				msg.sendToTarget();
			}
			}else{
				Message msg = Message.obtain(mhandler);
				msg.what = 0x07;//���β�ѯ���Ϊ��
				msg.sendToTarget();
			}
			cur.close();
		}
		
	};
	
	/**
	 * �������γ�������
	 */
	/*private Runnable batchRevocation = new Runnable(){

		@Override
		public void run() {
			// TODO Auto-generated method stub
			Connection cn = null;//��ʼ��cnΪ��ֵ
    		boolean isConnected = false;
    		try {
				//---------------------------------ʹ��jtds����-------------------------------------------------------------
				Class.forName("net.sourceforge.jtds.jdbc.Driver");
				cn = DriverManager.getConnection( "jdbc:jtds:sqlserver://10.126.8.6:1433/rm_db", "user01", "tcl123" );
				
				//���ӳɹ�ʱ�������ʾ
	    		Log.i("CONNECT_STATE", "���������ӳɹ���");
	    		Message msg = Message.obtain(mhandler);
	    		msg.what = 0x08;//���������ӳɹ�
	    		msg.sendToTarget();
	    		
	    		isConnected = true;
				
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				Message msg = Message.obtain(mhandler);
				msg.what = 0x09;//Զ�����ݿ�����ʧ��
				msg.sendToTarget();
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    		
    		if(isConnected){
    		String sqlupdate = "update LETV_SN_MAC set outbound = 'NO' where  OQC = '"+batchname+"'";//���³����־λYES
    		JtdsStatement st;
    		
			try {
				 st=(JtdsStatement) cn.createStatement(ResultSet.TYPE_SCROLL_SENSITIVE,ResultSet.CONCUR_READ_ONLY);//�ɹ��������
				 st.executeUpdate(sqlupdate);//���³����¼
				 
				 mDbHelper.deleteWithBatch("family_bill_letv", batchname);
				 
					 Message msg = Message.obtain(mhandler);
					 msg.what = 0x10;//Զ�����ݿ�����ɹ�
					 msg.sendToTarget();
				
				if(st != null){
						st.close();
					}if(cn != null){
						cn.close();
					}
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				 Message msg = Message.obtain(mhandler);
				 msg.what = 0x11;//Զ�����ݿ����ʧ��
				 msg.sendToTarget();
			}
    		
    		}
		}
		
	};*/
	
	/**
	 * �������γ���
	 */
	private Runnable batchRevocationWAN = new Runnable(){

		@Override
		public void run() {
			// TODO Auto-generated method stub
			if(dbUtils.setDefaultOutbondState_Letv(batchname)){
				 mDbHelper.deleteWithBatch("family_bill_letv", batchname);
				 Message msg = Message.obtain(mhandler);
				 msg.what = 0x10;//Զ�����ݿ�����ɹ�
				 msg.sendToTarget();
			}else{
				 Message msg = Message.obtain(mhandler);
				 msg.what = 0x11;//Զ�����ݿ����ʧ��
				 msg.sendToTarget();
			}
			
		}
		
		
	};
	
	private void showBatchQueryDialog(){
		
		final EditText edtinput = new EditText(Letv_DBHandleActivity.this); 
		
		new AlertDialog.Builder(Letv_DBHandleActivity.this).setIcon(android.R.drawable.btn_star)  
	    .setTitle("�����β�ѯ������Excel��")
	    .setIcon(android.R.drawable.btn_star)
	    .setMessage("�ֶ��������κţ�")
	    .setView(edtinput)
	    .setPositiveButton("ȷ��",new DialogInterface.OnClickListener() {
	                @Override  
	                public void onClick(DialogInterface dialog,int which) { 
	                    // TODO Auto-generated method stub  
	                	 String batch = edtinput.getText().toString();
	                	 if(!batch.isEmpty()){
	                		 batchname = batch;
	                		 if(isUnlockedAndSuite(batch)){
	                		 mydialog.show();
	                		 new Thread(batchQuery).start();
	                		 }else{
	                			 Toast.makeText(Letv_DBHandleActivity.this, "���������������򲻴��ڣ�", Toast.LENGTH_SHORT).show();
	                		 }
	                	 }else{
	                		 Toast.makeText(Letv_DBHandleActivity.this, "���������κţ�", Toast.LENGTH_SHORT).show();
	                	 }
	                
	                }
	            })  
	    .setNegativeButton("ȡ��",new DialogInterface.OnClickListener() {
	                @Override  
	                public void onClick(DialogInterface dialog,int which) {
	                    // TODO Auto-generated method stub  
	                    
	                }  
	            }).show();// show�ܹؼ�   
	}
	
private void showRevocationDialog(){
		
		final EditText edtinput = new EditText(Letv_DBHandleActivity.this); 
		
		new AlertDialog.Builder(Letv_DBHandleActivity.this).setIcon(android.R.drawable.btn_star)  
	    .setTitle("�����γ�������")
	    .setIcon(android.R.drawable.btn_star)
	    .setMessage("�ֶ��������κţ�")
	    .setView(edtinput)
	    .setPositiveButton("ȷ��",new DialogInterface.OnClickListener() {
	                @Override  
	                public void onClick(DialogInterface dialog,int which) { 
	                    // TODO Auto-generated method stub  
	                	String batch = edtinput.getText().toString();
	                	if(!batch.isEmpty()){
	                	batchname = batch;
	                	if(isUnlockedAndSuite(batch)){
	                	mydialog.show();
	                	new Thread(batchRevocationWAN).start();
	                	}else{
	                		Toast.makeText(Letv_DBHandleActivity.this, "���������������򲻴��ڣ�", Toast.LENGTH_SHORT).show();
	                	}
	                	}else{
	                		 Toast.makeText(Letv_DBHandleActivity.this, "���������κţ�", Toast.LENGTH_SHORT).show();
	                	}
	                }
	            })  
	    .setNegativeButton("ȡ��",new DialogInterface.OnClickListener() {
	                @Override  
	                public void onClick(DialogInterface dialog,int which) {
	                    // TODO Auto-generated method stub  
	                    
	                }  
	            }).show();// show�ܹؼ�   
	}
	
	private void setDataToCell(Cursor cur){
		ArrayList<String> beanList=new ArrayList<String>();
		beanList.add(cur.getString(1));
		beanList.add(cur.getString(2));
		beanList.add(cur.getString(3));
//		beanList.add(cur.getString(4));//���κţ���ʱ����Ҫ
		mylist.add(beanList);
	}
	
	public boolean saveQueryDataToExcel(ArrayList<ArrayList<String>> list) {
		file = new File(Letv_DBSheetBuilder.getSDPath() + File.separator+"ScanResultData"+File.separator+"Query");
		Letv_DBSheetBuilder.makeDir(file);
		ExcelUtils.initExcel(file.toString() + File.separator+"query.xls", title);
		boolean result = ExcelUtils.writeObjListToExcel(list, excelpath, getApplicationContext());
		return result;
	}
	
	public boolean saveBatchDataToExcel(ArrayList<ArrayList<String>> list) {
		file = new File(Letv_DBSheetBuilder.getSDPath() + File.separator+"ScanResultData"+File.separator+"Query");
		Letv_DBSheetBuilder.makeDir(file);
		String fpath = file.toString() + File.separator+batchname.replace("\n", "a")+".xls";//�����κ��д��ڵĻس����滻Ϊa,�������ֵ�һ��BUG�������Ѿ�����
		File file = new File(fpath);
		if(file.exists()){//���ļ��Ѵ��ڣ�����ɾ����Ȼ�����½��ļ����������ļ������������
			file.delete();
		}
		ExcelUtils.initExcel(fpath, title);
		boolean result = ExcelUtils.writeObjListToExcel(list, fpath, getApplicationContext());
		return result;
	}
	
	private void initExcelFile(){
		File f = new File(excelpath);
		if(f.exists()){
			f.delete();
		}
	}
	
	private final DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {
	    @Override
	    public void onDateSet(DatePicker view, int year, int monthOfYear,
	            int dayOfMonth) {
	        int mYear = year;
	        String mm;
	        String dd;

	        int mMonth = monthOfYear + 1;
	        mm = String.valueOf(mMonth);
	        if (mm.length() < 2)
	            mm = "0" + mm;

	        int mDay = dayOfMonth;
	        dd = String.valueOf(mDay);
	        if (dd.length() < 2)
	            dd = "0" + dd;

	        date.setText(String.valueOf(mYear) + "-" + mm + "-" + dd);
	    }
	};

	@Override
	protected Dialog onCreateDialog(int id) {
	     final Calendar c = Calendar.getInstance();
	     int mYear = c.get(Calendar.YEAR);
	     int mMonth = c.get(Calendar.MONTH);
	     int mDay = c.get(Calendar.DAY_OF_MONTH);

	    switch (id) {
	    case 0:
	        return new DatePickerDialog(this, mDateSetListener, mYear, mMonth,
	                mDay);
	    }
	    return null;
	}
	
	private Dialog getDialog(){
		 final Calendar c = Calendar.getInstance();
	     int mYear = c.get(Calendar.YEAR);
	     int mMonth = c.get(Calendar.MONTH);
	     int mDay = c.get(Calendar.DAY_OF_MONTH);
	     
         return new DatePickerDialog(this, mDateSetListener, mYear, mMonth,
	                mDay);
	
	}
	
	private boolean isUnlockedAndSuite(String str){//�ж��Ƿ�Ϊ���������β��������Ѵ���
		boolean result =false;
		for(int i = 0;i<batchunlock.length;i++){
			if(batchunlock[i].equals(str)){
				result = true;
			}
		}
		return result;
	}
	

}
