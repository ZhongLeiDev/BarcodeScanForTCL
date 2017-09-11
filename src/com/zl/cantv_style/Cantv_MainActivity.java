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
	
	/*Cantv扫描文件存放位置*/
	private final String path = Environment.getExternalStorageDirectory() + File.separator+"ScanResultData"+File.separator+"Cantv";
	private final String db_base_name = "base_cantv";
	private final String db_compare_name = "compare_cantv";
	private TextView currentfile;//当前调用的Excel文档
	private TextView detail;//提示信息
	private TextView sum;//需要扫描的总数
	private TextView scan;//已扫描的总数
	private TextView scancode;//条码值显示
	private ImageView imgresult;
	private Button btn;
	private String filename = "unknow";
	private Cantv_BaseDBHelper basedb = Cantv_BaseDBHelper.getInstance(this);
	private Cantv_CompareDBHelper comparedb = Cantv_CompareDBHelper.getInstance(this);
	private List<String> datalist = new ArrayList<String>();
	
	public static ServiceBeepManager OKbeepManager;//播放OK声音
	public static ServiceBeepManager NGbeepManager;//播放NG声音
	
	private ProgressDialog mydialog;
	private ProgressDialog exceldialog;
	private SoundPool soundPool;//声音播放缓冲
	
	private ScanBroadcastReceiver scanBroadcastReceiver;
	
	private String codeScan = "UNKNOW";
	private boolean isLocked = false; 
	
	private static long pointtime = 0;
	
	//------------------popupWindow代替menu-----------------------------
	private List<String> mpwlist = new ArrayList<String>();
	
	private Handler myhandler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			if(msg.what == 0x01){
				detail.setText("符合要求！");
				detail.setTextColor(Color.GREEN);
				imgresult.setImageDrawable(getResources().getDrawable(R.drawable.ok));
				soundPool.play(2,1, 1, 0, 0, 1);//播放缓存池中第二个声音，即beep
				updateScanSum();
			}else if(msg.what == 0x02){
				detail.setText("条码存在于数据库中，但插入对比数据库失败！");
				detail.setTextColor(Color.RED);
				imgresult.setImageDrawable(getResources().getDrawable(R.drawable.ng));
				soundPool.play(1,1, 1, 0, 0, 1);//播放缓存池中第一个声音，即error
				isLocked = true;//出错则锁定
				showErrorDialog();
			}else if(msg.what == 0x03){
				detail.setText("此条码已经扫描过了！");
				detail.setTextColor(Color.RED);
				imgresult.setImageDrawable(getResources().getDrawable(R.drawable.ng));
				soundPool.play(1,1, 1, 0, 0, 1);//播放缓存池中第一个声音，即error
				isLocked = true;//出错则锁定
				showErrorDialog();
			}else if(msg.what == 0x04){
				detail.setText("条码不存在数据库中！");
				detail.setTextColor(Color.RED);
				imgresult.setImageDrawable(getResources().getDrawable(R.drawable.ng));
				soundPool.play(1,1, 1, 0, 0, 1);//播放缓存池中第一个声音，即error
				isLocked = true;//出错则锁定
				showErrorDialog();
			}else if(msg.what == 0x05){
				updateSum();
				updateScanSum();
				detail.setText("Excel数据加载成功！");
				detail.setTextColor(Color.GREEN);
				if(mydialog.isShowing()){
				mydialog.cancel();
				}
			}else if(msg.what == 0x20){
				exceldialog.cancel();
				detail.setText("Excel表格生成成功！");
				detail.setTextColor(Color.GREEN);
			}else if(msg.what == 0x21){
				exceldialog.cancel();
				detail.setText("Excel表格生成失败");
				detail.setTextColor(Color.RED);
			}else if(msg.what == 0x22){
				exceldialog.cancel();
				detail.setText("本批次扫描结果为空！");
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
				Log.i("DELTATIME", "按钮按下与Button点击的时间差为："+String.valueOf(deltatime));
				if(deltatime>1000){//当Scan键与按钮按下的时间间隔大于1000ms时判定为有效的按钮点击事件
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
	public void onWindowFocusChanged(boolean hasFocus) {//用焦点改变来确认Activity是否加载完毕
	// TODO Auto-generated method stub
	super.onWindowFocusChanged(hasFocus);
	if (hasFocus) {
	//do something
		//setIconEnable(mymenu,true);  //  就是这一句使图标能显示
	}
	}
	
	private void init(){
		basedb.open();//数据库开启
		comparedb.open();
		 //--------------------------等待框---------------------------
  		mydialog = new ProgressDialog(this);
  		mydialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
  		mydialog.setTitle("等待Excel加载完成");
  		mydialog.setMessage("扫描数据Excel正在加载...");
  		mydialog.setCancelable(false);
  		
  		exceldialog = new ProgressDialog(this);
  		exceldialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
  		exceldialog.setTitle("等待Excel生成完成");
  		exceldialog.setMessage("Excel文件正在生成...");
  		exceldialog.setCancelable(false);
  		//---------------------------声音缓冲池初始化----------------
  		soundPool= new SoundPool(3,AudioManager.STREAM_SYSTEM,5);
  		soundPool.load(this,R.raw.error,1);
  		soundPool.load(this, R.raw.beep, 1);
		//--------------------------------------扫描广播注册------------------------------------------
		Intent newIntent = new Intent(Cantv_MainActivity.this, CaptureService.class);
		newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startService(newIntent);

		scanBroadcastReceiver = new ScanBroadcastReceiver();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction("com.zkc.scancode");
		this.registerReceiver(scanBroadcastReceiver, intentFilter);
		//-------------------------------------扫描文件名获取------------------------------------------
		if((ExcelRead.getFilenames(path)!=null)&&(ExcelRead.getFilenames(path).size()>0)){
		filename = ExcelRead.getFilenames(path).get(0);
		currentfile.setText(filename);
		//Log.i("MainActivity", String.valueOf(datalist.size())+"------>"+datalist.toString());
		}else{
			detail.setTextColor(Color.RED);
			detail.setText("指定文件不存在，请检查目录文件！");
		}
		//-------------------------------------获取扫描数量-----------------------------------------------
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
		Log.i("getDataTimes", "获取数据用时："+String.valueOf(endtime1)+"ms");
		int size = datalist.size();
		for(int i=0;i<size;i++){
			values.put("SN", datalist.get(i).toString());
			basedb.insert(db_base_name, values);//直接插入效率太低，需要用线程池改良
		}
		Log.i("getDataTimes", "插入数据用时："+String.valueOf(System.currentTimeMillis()-starttime1-endtime1)+"ms");
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
	private void reloadFileThoughMultithreads(){//多线程加载
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
					basedb.insert(db_base_name, values);//直接插入效率太低，需要用线程池改良，然而对于单核CPU来说，并没有什么作用 （=￣ω￣=）
				}
				
			});
		}
		tp.shutdown();
		try {
			tp.awaitTermination(1, TimeUnit.DAYS);//等待线程全部执行完毕
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Log.i("getDataTimes", "插入数据用时："+String.valueOf(System.currentTimeMillis()-starttime1)+"ms");
		
		datalist.clear();//释放datalist内存
		
		updateSum();
		mydialog.cancel();
		
	}
	
	/**
	 * 条码对比函数
	 * @param SN 待比较的SN码
	 * @return 比较结果
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
				detail.setText("符合要求！");
				detail.setTextColor(Color.GREEN);
				result = true;
			}else{
				detail.setText("条码存在于数据库中，但插入对比数据库失败！");
				detail.setTextColor(Color.RED);
			};
			}else{
				detail.setText("此条码已经扫描过了！");
				detail.setTextColor(Color.RED);
			}
		}else{
			detail.setText("条码不存在数据库中！");
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
			
			pointtime = System.currentTimeMillis();//获取扫描按键按下时的系统时间，通过计算按键与Button按下的时间差来解决后续按下扫描按键Button也会按下的BUG
			
			codeScan = intent.getExtras().getString("code");
			
			scancode.setText(codeScan);
			
			if(!isLocked){//未锁定时开启扫描
				new Thread(compareRunnable).start();
			}
			
	}
	}
	
	private void showErrorDialog(){
		new AlertDialog.Builder(Cantv_MainActivity.this).setIcon(android.R.drawable.btn_star)
		.setCancelable(false)//设置点击确定键以外的地方不起作用
	    .setTitle("错误警报")
	    .setMessage("扫描到条码\n"+codeScan+"\n发生错误，请重新扫描或检查设置！")
	    .setPositiveButton("确定",new DialogInterface.OnClickListener() {
	                @Override  
	                public void onClick(DialogInterface dialog,int which) { 
	                    // TODO Auto-generated method stub  
	                	isLocked = false;//点击确定按钮后解除锁定;
	                }
	            }).show();
	}
	
	private void showRebuildDialog(){
		new AlertDialog.Builder(Cantv_MainActivity.this).setIcon(android.R.drawable.btn_star)
		.setCancelable(false)//设置点击确定键以外的地方不起作用
	    .setTitle("重载确认")
	    .setMessage("是否重新加载扫描文件？")
	    .setPositiveButton("确定",new DialogInterface.OnClickListener() {
	                @Override  
	                public void onClick(DialogInterface dialog,int which) { 
	                    // TODO Auto-generated method stub  
	                	mydialog.show();//加载等待提示框
	                	new Thread(reloadRunnable).start();
	                }
	            })
	            .setNegativeButton("取消", new DialogInterface.OnClickListener() {
	                @Override  
	                public void onClick(DialogInterface dialog,int which) { 
	                    // TODO Auto-generated method stub  
	                	
	                }
	            }).show();
	}
	
	private void showbuildExcelDialog(){
		new AlertDialog.Builder(Cantv_MainActivity.this).setIcon(android.R.drawable.btn_star)
		.setCancelable(false)//设置点击确定键以外的地方不起作用
	    .setTitle("Excel文件生成")
	    .setMessage("是否生成Excel文件？")
	    .setPositiveButton("确定",new DialogInterface.OnClickListener() {
	                @Override  
	                public void onClick(DialogInterface dialog,int which) { 
	                    // TODO Auto-generated method stub  
	                	exceldialog.show();//加载等待提示框
	                	new Thread(buildExcelRunnable).start();
	                }
	            })
	            .setNegativeButton("取消", new DialogInterface.OnClickListener() {
	                @Override  
	                public void onClick(DialogInterface dialog,int which) { 
	                    // TODO Auto-generated method stub  
	                	
	                }
	            }).show();
	}
	
	/**
	 * 更新扫描数量
	 */
	private void updateScanSum(){
		String sql2 = "select * from compare_cantv";
		int nn = comparedb.exeSql(sql2).getCount();
		scan.setText(String.valueOf(nn));
		scan.setTextColor(Color.RED);
	}
	
	/**
	 * 更新总数量
	 */
	private void updateSum(){
		String sql = "select * from base_cantv";
		int mm = basedb.exeSql(sql).getCount();
		sum.setText(String.valueOf(mm));
		sum.setTextColor(Color.BLUE);
	}
	
	/**
	 * Excel表格生成函数
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
				Log.i("MYLISTSIZE", "数据个数为："+String.valueOf(mylist.size())+"");
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
	 * Android4.0以上可能出现Menu菜单的Icon不显示的问题，在这里利用反射设置菜单的Icon使其显示
	 * 此方法为附加方法，可能会用到，也可能不会用到
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

            //MenuBuilder实现Menu接口，创建菜单时，传进来的menu其实就是MenuBuilder对象(java的多态特征)
            m.invoke(menu, enable);

        } catch (Exception e)
        {
            e.printStackTrace();
        }
    }
	
	/**
	 * 重写onKeyDown方法可以拦截系统默认的处理，进行按键的监听处理
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		// TODO Auto-generated method stub
		if (keyCode == KeyEvent.KEYCODE_MENU) {//菜单键
			//Toast.makeText(this, "菜单键已按下！", Toast.LENGTH_SHORT).show();
			showPopupWindow();
			return false;
		}
		return super.onKeyDown(keyCode, event);
	}
	
	/**
	 * 以PopupWindow代替Menu菜单栏，这是扫描枪的一个BUG（在扫描时菜单栏会自动弹出）,用此种方法解决
	 */
	 private void showPopupWindow( ) {
		 	setList();
	        // 一个自定义的布局，作为显示的内容
	        View contentView = LayoutInflater.from(this).inflate(
	                R.layout.mypopupwindow, null);
	        final PopupWindow popupWindow = new PopupWindow(contentView,
	                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT, true);
	        // 设置按钮的点击事件
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
						 	popupWindow.dismiss();//pop消失
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
	                // 这里如果返回true的话，touch事件将被拦截
	                // 拦截后 PopupWindow的onTouchEvent不被调用，这样点击外部区域无法dismiss
	            }
	        });

	        // 如果不设置PopupWindow的背景，无论是点击外部区域还是Back键都无法dismiss弹框
	        // 我觉得这里是API的一个bug
	        popupWindow.setBackgroundDrawable(getResources().getDrawable(
	                R.drawable.popupbg));

	        // 设置好参数之后再show
	        //popupWindow.showAsDropDown(contentView.findViewById(R.id.poplinearlayout));
	        popupWindow.showAtLocation(contentView, Gravity.BOTTOM, 0, 0);

	    }
	 
	 private void setList(){
		 	mpwlist.clear();
		 	mpwlist.add("加载新的扫描文件");
	 }

}
