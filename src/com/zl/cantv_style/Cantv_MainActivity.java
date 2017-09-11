package com.zl.cantv_style;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.zkc.Service.CaptureService;
import com.zkc.barcodescan.R;
import com.zkc.beep.ServiceBeepManager;

public class Cantv_MainActivity extends Activity{
	
	/*Cantvɨ���ļ����λ��*/
	private final String path = Environment.getExternalStorageDirectory() + File.separator+"ScanResultData"+File.separator+"Cantv";
	private final String db_base_name = "base_cantv";
	private final String db_compare_name = "compare_cantv";
	private TextView currentfile;//��ǰ���õ�Excel�ĵ�
	private TextView detail;//��ʾ��Ϣ
	private TextView sum;//��Ҫɨ�������
	private TextView scan;//��ɨ�������
	private TextView scancode;//����ֵ��ʾ
	private ImageView imgresult;
	private Button btn;
	private String filename = "unknow";
	private Cantv_BaseDBHelper basedb = Cantv_BaseDBHelper.getInstance(this);
	private Cantv_CompareDBHelper comparedb = Cantv_CompareDBHelper.getInstance(this);
	private List<String> datalist = new ArrayList<String>();
	
	public static ServiceBeepManager OKbeepManager;//����OK����
	public static ServiceBeepManager NGbeepManager;//����NG����
	
	private ProgressDialog mydialog;
	private ProgressDialog exceldialog;
	private SoundPool soundPool;//�������Ż���
	
	private ScanBroadcastReceiver scanBroadcastReceiver;
	
	private String codeScan = "UNKNOW";
	private boolean isLocked = false; 
	
	private static long pointtime = 0;
	
	//------------------popupWindow����menu-----------------------------
	private List<String> mpwlist = new ArrayList<String>();
	
	private Handler myhandler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			if(msg.what == 0x01){
				detail.setText("����Ҫ��");
				detail.setTextColor(Color.GREEN);
				imgresult.setImageDrawable(getResources().getDrawable(R.drawable.ok));
				soundPool.play(2,1, 1, 0, 0, 1);//���Ż�����еڶ�����������beep
				updateScanSum();
			}else if(msg.what == 0x02){
				detail.setText("������������ݿ��У�������Ա����ݿ�ʧ�ܣ�");
				detail.setTextColor(Color.RED);
				imgresult.setImageDrawable(getResources().getDrawable(R.drawable.ng));
				soundPool.play(1,1, 1, 0, 0, 1);//���Ż�����е�һ����������error
				isLocked = true;//����������
				showErrorDialog();
			}else if(msg.what == 0x03){
				detail.setText("�������Ѿ�ɨ����ˣ�");
				detail.setTextColor(Color.RED);
				imgresult.setImageDrawable(getResources().getDrawable(R.drawable.ng));
				soundPool.play(1,1, 1, 0, 0, 1);//���Ż�����е�һ����������error
				isLocked = true;//����������
				showErrorDialog();
			}else if(msg.what == 0x04){
				detail.setText("���벻�������ݿ��У�");
				detail.setTextColor(Color.RED);
				imgresult.setImageDrawable(getResources().getDrawable(R.drawable.ng));
				soundPool.play(1,1, 1, 0, 0, 1);//���Ż�����е�һ����������error
				isLocked = true;//����������
				showErrorDialog();
			}else if(msg.what == 0x05){
				updateSum();
				updateScanSum();
				detail.setText("Excel���ݼ��سɹ���");
				detail.setTextColor(Color.GREEN);
				if(mydialog.isShowing()){
				mydialog.cancel();
				}
			}else if(msg.what == 0x20){
				exceldialog.cancel();
				detail.setText("Excel������ɳɹ���");
				detail.setTextColor(Color.GREEN);
			}else if(msg.what == 0x21){
				exceldialog.cancel();
				detail.setText("Excel�������ʧ��");
				detail.setTextColor(Color.RED);
			}else if(msg.what == 0x22){
				exceldialog.cancel();
				detail.setText("������ɨ����Ϊ�գ�");
				detail.setTextColor(Color.RED);
			}
		}
	};
	
	@Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cantv_layout);
        
        currentfile = (TextView)findViewById(R.id.currentfile);
        detail = (TextView)findViewById(R.id.tvdetail);
        sum = (TextView)findViewById(R.id.tvsum);
        scan = (TextView)findViewById(R.id.tvscan);
        imgresult = (ImageView)findViewById(R.id.imgresult);
        scancode = (TextView)findViewById(R.id.scancode);
        
        btn = (Button)findViewById(R.id.button1);
        
        init();
        
        btn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View view) {
				// TODO Auto-generated method stub
				long deltatime = Math.abs(System.currentTimeMillis()-pointtime);
				Log.i("DELTATIME", "��ť������Button�����ʱ���Ϊ��"+String.valueOf(deltatime));
				if(deltatime>1000){//��Scan���밴ť���µ�ʱ��������1000msʱ�ж�Ϊ��Ч�İ�ť����¼�
				showbuildExcelDialog();
				}
			}
		});
        
    }
	
	/*----------------------------------------------------------------------------
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		menu.add(0, 1, 1, R.string.can4);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(android.view.MenuItem item) {
		if (item.getItemId() == 1) {
			showRebuildDialog();
		}
		return super.onOptionsItemSelected(item);
	}
	--------------------------------------------------------------------------------*/
	
	@Override
	public void onBackPressed() {
		exitActivity();
	}
	
	@Override
	protected void onDestroy() {
		this.unregisterReceiver(scanBroadcastReceiver);
		super.onDestroy();
	}
	
	@Override
	public void onWindowFocusChanged(boolean hasFocus) {//�ý���ı���ȷ��Activity�Ƿ�������
	// TODO Auto-generated method stub
	super.onWindowFocusChanged(hasFocus);
	if (hasFocus) {
	//do something
		//setIconEnable(mymenu,true);  //  ������һ��ʹͼ������ʾ
	}
	}
	
	private void init(){
		basedb.open();//���ݿ⿪��
		comparedb.open();
		 //--------------------------�ȴ���---------------------------
  		mydialog = new ProgressDialog(this);
  		mydialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
  		mydialog.setTitle("�ȴ�Excel�������");
  		mydialog.setMessage("ɨ������Excel���ڼ���...");
  		mydialog.setCancelable(false);
  		
  		exceldialog = new ProgressDialog(this);
  		exceldialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
  		exceldialog.setTitle("�ȴ�Excel�������");
  		exceldialog.setMessage("Excel�ļ���������...");
  		exceldialog.setCancelable(false);
  		//---------------------------��������س�ʼ��----------------
  		soundPool= new SoundPool(3,AudioManager.STREAM_SYSTEM,5);
  		soundPool.load(this,R.raw.error,1);
  		soundPool.load(this, R.raw.beep, 1);
		//--------------------------------------ɨ��㲥ע��------------------------------------------
		Intent newIntent = new Intent(Cantv_MainActivity.this, CaptureService.class);
		newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startService(newIntent);

		scanBroadcastReceiver = new ScanBroadcastReceiver();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction("com.zkc.scancode");
		this.registerReceiver(scanBroadcastReceiver, intentFilter);
		//-------------------------------------ɨ���ļ�����ȡ------------------------------------------
		if((ExcelRead.getFilenames(path)!=null)&&(ExcelRead.getFilenames(path).size()>0)){
		filename = ExcelRead.getFilenames(path).get(0);
		currentfile.setText(filename);
		//Log.i("MainActivity", String.valueOf(datalist.size())+"------>"+datalist.toString());
		}else{
			detail.setTextColor(Color.RED);
			detail.setText("ָ���ļ������ڣ�����Ŀ¼�ļ���");
		}
		//-------------------------------------��ȡɨ������-----------------------------------------------
		updateSum();
		updateScanSum();
	}
	
	private void reloadFile(){
		ContentValues values = new ContentValues();
		basedb.deleteAll(db_base_name);
		comparedb.deleteAll(db_compare_name);
		long starttime1 = System.currentTimeMillis();
		datalist = ExcelRead.getDataFromExcelForCantv(path+File.separator+filename);
		long endtime1 = System.currentTimeMillis()-starttime1;
		Log.i("getDataTimes", "��ȡ������ʱ��"+String.valueOf(endtime1)+"ms");
		int size = datalist.size();
		for(int i=0;i<size;i++){
			values.put("SN", datalist.get(i).toString());
			basedb.insert(db_base_name, values);//ֱ�Ӳ���Ч��̫�ͣ���Ҫ���̳߳ظ���
		}
		Log.i("getDataTimes", "����������ʱ��"+String.valueOf(System.currentTimeMillis()-starttime1-endtime1)+"ms");
	}
	
	private Runnable reloadRunnable = new Runnable(){

		@Override
		public void run() {
			// TODO Auto-generated method stub
			
			reloadFile();
			
			Message msg = Message.obtain(myhandler);
			msg.what = 0x05;
			msg.sendToTarget();
		}
		
	};
	
	@SuppressWarnings("unused")
	private void reloadFileThoughMultithreads(){//���̼߳���
		final ContentValues values = new ContentValues();
		basedb.deleteAll(db_base_name);
		comparedb.deleteAll(db_compare_name);
		datalist = ExcelRead.getDataFromExcelForCantv(path+File.separator+filename);
		long starttime1 = System.currentTimeMillis();
		int size = datalist.size();
		ThreadPoolExecutor tp = new ThreadPoolExecutor(1, 1, 60, TimeUnit.SECONDS, new LinkedBlockingDeque<Runnable>(size));
		for(int i=0;i<size;i++){
			final int m = i;
			tp.execute(new Runnable(){

				@Override
				public void run() {
					// TODO Auto-generated method stub
					values.put("SN", datalist.get(m).toString());
					basedb.insert(db_base_name, values);//ֱ�Ӳ���Ч��̫�ͣ���Ҫ���̳߳ظ�����Ȼ�����ڵ���CPU��˵����û��ʲô���� ��=���أ�=��
				}
				
			});
		}
		tp.shutdown();
		try {
			tp.awaitTermination(1, TimeUnit.DAYS);//�ȴ��߳�ȫ��ִ�����
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Log.i("getDataTimes", "����������ʱ��"+String.valueOf(System.currentTimeMillis()-starttime1)+"ms");
		
		datalist.clear();//�ͷ�datalist�ڴ�
		
		updateSum();
		mydialog.cancel();
		
	}
	
	/**
	 * ����ԱȺ���
	 * @param SN ���Ƚϵ�SN��
	 * @return �ȽϽ��
	 */
	@SuppressWarnings("unused")
	private boolean compareSN(String SN){
		boolean result = false;
		String sql = "select * from base_cantv where SN = '"+SN+"'";
		int a = basedb.exeSql(sql).getCount();
		if(a>0){
			String sql2 = "select * from compare_cantv where SN = '"+SN+"'";
			if(comparedb.exeSql(sql2).getCount() == 0){
			ContentValues con = new ContentValues();
			con.put("SN", SN);
			if(comparedb.insert(db_compare_name, con)>0){
				detail.setText("����Ҫ��");
				detail.setTextColor(Color.GREEN);
				result = true;
			}else{
				detail.setText("������������ݿ��У�������Ա����ݿ�ʧ�ܣ�");
				detail.setTextColor(Color.RED);
			};
			}else{
				detail.setText("�������Ѿ�ɨ����ˣ�");
				detail.setTextColor(Color.RED);
			}
		}else{
			detail.setText("���벻�������ݿ��У�");
			detail.setTextColor(Color.RED);
		}
		return result;
	}
	
	private Runnable compareRunnable = new Runnable(){

		@Override
		public void run() {
			// TODO Auto-generated method stub
			String sql = "select * from base_cantv where SN = '"+codeScan+"'";
			int a = basedb.exeSql(sql).getCount();
			if(a>0){
				String sql2 = "select * from compare_cantv where SN = '"+codeScan+"'";
				if(comparedb.exeSql(sql2).getCount() == 0){
				ContentValues con = new ContentValues();
				con.put("SN", codeScan);
				if(comparedb.insert(db_compare_name, con)>0){
					Message msg = Message.obtain(myhandler);
					msg.what = 0x01;
					msg.sendToTarget();
				}else{
					Message msg = Message.obtain(myhandler);
					msg.what = 0x02;
					msg.sendToTarget();	
				};
				}else{
					Message msg = Message.obtain(myhandler);
					msg.what = 0x03;
					msg.sendToTarget();	
				}
			}else{
				Message msg = Message.obtain(myhandler);
				msg.what = 0x04;
				msg.sendToTarget();	
			}
		}
		
	};
	
	private void exitActivity() {
		new AlertDialog.Builder(this)
				.setIcon(android.R.drawable.ic_dialog_alert)
				.setTitle(R.string.popup_title)
				.setMessage(R.string.popup_message)
				.setPositiveButton(R.string.popup_yes,
						new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog,
									int which) {

								CaptureService.scanGpio.closeScan(); 
								CaptureService.scanGpio.closePower();

								finish();
							}
						}).setNegativeButton(R.string.popup_no, null).show();
	}
	
	class ScanBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			
			pointtime = System.currentTimeMillis();//��ȡɨ�谴������ʱ��ϵͳʱ�䣬ͨ�����㰴����Button���µ�ʱ����������������ɨ�谴��ButtonҲ�ᰴ�µ�BUG
			
			codeScan = intent.getExtras().getString("code");
			
			scancode.setText(codeScan);
			
			if(!isLocked){//δ����ʱ����ɨ��
				new Thread(compareRunnable).start();
			}
			
	}
	}
	
	private void showErrorDialog(){
		new AlertDialog.Builder(Cantv_MainActivity.this).setIcon(android.R.drawable.btn_star)
		.setCancelable(false)//���õ��ȷ��������ĵط���������
	    .setTitle("���󾯱�")
	    .setMessage("ɨ�赽����\n"+codeScan+"\n��������������ɨ��������ã�")
	    .setPositiveButton("ȷ��",new DialogInterface.OnClickListener() {
	                @Override  
	                public void onClick(DialogInterface dialog,int which) { 
	                    // TODO Auto-generated method stub  
	                	isLocked = false;//���ȷ����ť��������;
	                }
	            }).show();
	}
	
	private void showRebuildDialog(){
		new AlertDialog.Builder(Cantv_MainActivity.this).setIcon(android.R.drawable.btn_star)
		.setCancelable(false)//���õ��ȷ��������ĵط���������
	    .setTitle("����ȷ��")
	    .setMessage("�Ƿ����¼���ɨ���ļ���")
	    .setPositiveButton("ȷ��",new DialogInterface.OnClickListener() {
	                @Override  
	                public void onClick(DialogInterface dialog,int which) { 
	                    // TODO Auto-generated method stub  
	                	mydialog.show();//���صȴ���ʾ��
	                	new Thread(reloadRunnable).start();
	                }
	            })
	            .setNegativeButton("ȡ��", new DialogInterface.OnClickListener() {
	                @Override  
	                public void onClick(DialogInterface dialog,int which) { 
	                    // TODO Auto-generated method stub  
	                	
	                }
	            }).show();
	}
	
	private void showbuildExcelDialog(){
		new AlertDialog.Builder(Cantv_MainActivity.this).setIcon(android.R.drawable.btn_star)
		.setCancelable(false)//���õ��ȷ��������ĵط���������
	    .setTitle("Excel�ļ�����")
	    .setMessage("�Ƿ�����Excel�ļ���")
	    .setPositiveButton("ȷ��",new DialogInterface.OnClickListener() {
	                @Override  
	                public void onClick(DialogInterface dialog,int which) { 
	                    // TODO Auto-generated method stub  
	                	exceldialog.show();//���صȴ���ʾ��
	                	new Thread(buildExcelRunnable).start();
	                }
	            })
	            .setNegativeButton("ȡ��", new DialogInterface.OnClickListener() {
	                @Override  
	                public void onClick(DialogInterface dialog,int which) { 
	                    // TODO Auto-generated method stub  
	                	
	                }
	            }).show();
	}
	
	/**
	 * ����ɨ������
	 */
	private void updateScanSum(){
		String sql2 = "select * from compare_cantv";
		int nn = comparedb.exeSql(sql2).getCount();
		scan.setText(String.valueOf(nn));
		scan.setTextColor(Color.RED);
	}
	
	/**
	 * ����������
	 */
	private void updateSum(){
		String sql = "select * from base_cantv";
		int mm = basedb.exeSql(sql).getCount();
		sum.setText(String.valueOf(mm));
		sum.setTextColor(Color.BLUE);
	}
	
	/**
	 * Excel������ɺ���
	 */
	private Runnable buildExcelRunnable = new Runnable(){

		@Override
		public void run() {
			// TODO Auto-generated method stub
			ArrayList<ArrayList<String>>mylist = new ArrayList<ArrayList<String>>();
			Cursor cur = comparedb.exeSql("select * from compare_cantv");
			while (cur.moveToNext()) {
				ArrayList<String> beanList=new ArrayList<String>();
				beanList.add(cur.getString(1));
				mylist.add(beanList);
			}
			if(!mylist.isEmpty()){
				Log.i("MYLISTSIZE", "���ݸ���Ϊ��"+String.valueOf(mylist.size())+"");
			if(comparedb.saveDataToExcel(mylist, "Saved_"+ExcelRead.getFileNameNoEx(filename)+"."+ExcelRead.getExtensionName(filename))){
				Message msg = Message.obtain(myhandler);
				msg.what = 0x20;
				msg.sendToTarget();
			}else{
				Message msg = Message.obtain(myhandler);
				msg.what = 0x21;
				msg.sendToTarget();
			}
			}else{
				Message msg = Message.obtain(myhandler);
				msg.what = 0x22;
				msg.sendToTarget();
			}
			cur.close();
		}
		
	};
	
	/**
	 * Android4.0���Ͽ��ܳ���Menu�˵���Icon����ʾ�����⣬���������÷������ò˵���Iconʹ����ʾ
	 * �˷���Ϊ���ӷ��������ܻ��õ���Ҳ���ܲ����õ�
	 * @param menu
	 * @param enable
	 */
	@SuppressWarnings("unused")
	private void setIconEnable(Menu menu, boolean enable)
    {
        try
        {
            Class<?> clazz = Class.forName("com.android.internal.view.menu.MenuBuilder");
            Method m = clazz.getDeclaredMethod("setOptionalIconsVisible", boolean.class);
            m.setAccessible(true);

            //MenuBuilderʵ��Menu�ӿڣ������˵�ʱ����������menu��ʵ����MenuBuilder����(java�Ķ�̬����)
            m.invoke(menu, enable);

        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }
	
	/**
	 * ��дonKeyDown������������ϵͳĬ�ϵĴ������а����ļ�������
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if (keyCode == KeyEvent.KEYCODE_MENU) {//�˵���
			//Toast.makeText(this, "�˵����Ѱ��£�", Toast.LENGTH_SHORT).show();
			showPopupWindow();
			return false;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	/**
	 * ��PopupWindow����Menu�˵���������ɨ��ǹ��һ��BUG����ɨ��ʱ�˵������Զ�������,�ô��ַ������
	 */
	 private void showPopupWindow( ) {
		 	setList();
	        // һ���Զ���Ĳ��֣���Ϊ��ʾ������
	        View contentView = LayoutInflater.from(this).inflate(
	                R.layout.mypopupwindow, null);
	        final PopupWindow popupWindow = new PopupWindow(contentView,
	                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, true);
	        // ���ð�ť�ĵ���¼�
	        ListView lv = (ListView) contentView.findViewById(R.id.poplist);
	        lv.setAdapter(new ArrayAdapter<String>(this, android.R.layout.simple_expandable_list_item_1,mpwlist));
	        lv.setOnItemClickListener(new OnItemClickListener() {
				@Override
				public void onItemClick(AdapterView<?> parent, View view,
						int position, long id) {
					// TODO Auto-generated method stub
//					 Toast.makeText(Offline_MainActivity.this, "button is pressed",
//		                        Toast.LENGTH_SHORT).show();
					 if (position == 0) {
						 	popupWindow.dismiss();//pop��ʧ
							showRebuildDialog();
						} 
				}
	        });


	        popupWindow.setTouchable(true);

	        popupWindow.setTouchInterceptor(new OnTouchListener() {

	            @Override
	            public boolean onTouch(View v, MotionEvent event) {

	                Log.i("mengdd", "onTouch : ");

	                return false;
	                // �����������true�Ļ���touch�¼���������
	                // ���غ� PopupWindow��onTouchEvent�������ã���������ⲿ�����޷�dismiss
	            }
	        });

	        // ���������PopupWindow�ı����������ǵ���ⲿ������Back�����޷�dismiss����
	        // �Ҿ���������API��һ��bug
	        popupWindow.setBackgroundDrawable(getResources().getDrawable(
	                R.drawable.popupbg));

	        // ���úò���֮����show
	        //popupWindow.showAsDropDown(contentView.findViewById(R.id.poplinearlayout));
	        popupWindow.showAtLocation(contentView, Gravity.BOTTOM, 0, 0);

	    }
	 
	 private void setList(){
		 	mpwlist.clear();
		 	mpwlist.add("�����µ�ɨ���ļ�");
	 }

}
