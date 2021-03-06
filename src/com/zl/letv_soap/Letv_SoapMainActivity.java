package com.zl.letv_soap;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.zkc.Service.CaptureService;
import com.zkc.barcodescan.R;
import com.zkc.barcodescan.activity.ActivityBarcodeSetting;
import com.zkc.barcodescan.activity.ActivityQrcodeSetting;
import com.zkc.barcodescan.activity.WiFiStateBroadcastReceiver;
import com.zkc.beep.ServiceBeepManager;
import com.zl.common_logcatutils.LogcatFileManager;
import com.zl.common_utils.FileUtils;
import com.zl.common_utils.StringTransUtils;
import com.zl.letv_dbsetting.Letv_BatchDBSheetBuilder;
import com.zl.letv_dbsetting.Letv_BatchHandleActivity;
import com.zl.letv_dbsetting.Letv_ConfigActivity;
import com.zl.letv_dbsetting.Letv_DBHandleActivity;
import com.zl.letv_dbsetting.Letv_DBSheetBuilder;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Message;
import android.serialport.api.SerialPort;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.ViewConfiguration;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

public class Letv_SoapMainActivity extends Activity {
	private static final String TAG = "MainActivity";
	private ScanBroadcastReceiver scanBroadcastReceiver;
	private WiFiStateBroadcastReceiver wsb;
	private Button btnOpen, btnEdit;
	public static TextView et_code;
	private Button emptyBtn;
	private Button btnBatchList,btnIsComplete,btnOutBound,btnRevocation;
	private ImageView resultview;
	private TextView detail,netstate,tvbatch,tvcount,tvbatchcountlimit,tvbatchsum;
	private Letv_DBSheetBuilder dbsb;
	private Letv_BatchDBSheetBuilder batchbuilder;
	private String filename = "outbound.xls";//自定义批次号，即存储出库数据的Excel文件名称
	private String batch = "";
	private List<String> batchlist = new ArrayList<String>();
	private boolean isbatched = false;
	private int batchcountlimit = 0;
	private final String FLAG_OUTBOUND = "isoutbound";
	private final String FLAG_REVOCATION = "isrevocation";
	private String Flag = "";
	private String isComplete = "NO";
	private static final String currentbatchpath = Environment.getExternalStorageDirectory()+File.separator+"ScanResultData"+File.separator+"Config"+File.separator+"letv_currentbatch.dat";
	
	HandlerThread thread = new HandlerThread("MyHandlerThread"); 
//	thread.start();//创建个HandlerThread并启动它 
	private Handler mhandler;
	
	private String codeScan = "";

	List<Map<String, String>> listData = new ArrayList<Map<String, String>>();
	
	private static final String Properfile = Environment.getExternalStorageDirectory()+ File.separator+"ScanResultData"+File.separator+"Config"+File.separator+"letv_config.dat";//联系人列表
	private String[ ] receiver = new String[ ]{ };
	
	public static ServiceBeepManager OKbeepManager;//播放OK声音
	public static ServiceBeepManager NGbeepManager;//播放NG声音
	
	private ProgressDialog mydialog;
	private SoundPool soundPool;//声音播放缓冲
	
	private boolean IS_ALLLOCKED = false;//全局锁定标志
	
	private Map<String,String> scanresultmap = new HashMap<String,String>();//通过外网连接WebService查询得到的结
	private Letv_DBUtils dbUtils = new Letv_DBUtils();
	
	//------------------popupWindow代替menu-----------------------------
	private List<String> mpwlist = new ArrayList<String>();
	
	private Handler mainhandler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			if(msg.what == 0x01){
				Log.i("RESULT", "连接服务器成功！");
			}else if(msg.what == 0x02){
//				Toast.makeText(MainActivity.this, "Result is OK!", Toast.LENGTH_SHORT).show();
				mhandler.removeCallbacks(startConnectDBWithSoap);
				resultview.setImageDrawable(getResources().getDrawable(R.drawable.ok));
				detail.setText("已获取到唯一的匹配结果！");
				detail.setTextColor(Color.GREEN);
				
				soundPool.play(2,1, 1, 0, 0, 1);//播放缓存池中第二个声音，即beep
				
				int count = (Integer)msg.obj;
				tvcount.setText(String.valueOf(count));
				
			}else if(msg.what == 0x03){
//				Toast.makeText(MainActivity.this, "Result is NG! No Result Contained!", Toast.LENGTH_SHORT).show();
				mhandler.removeCallbacks(startConnectDBWithSoap);
				resultview.setImageDrawable(getResources().getDrawable(R.drawable.ng));
				detail.setText("未获取到匹配结果");
				detail.setTextColor(Color.RED);
				soundPool.play(1,1, 1, 0, 0, 1);//播放缓存池中第一个声音，即error
				
				IS_ALLLOCKED = true;//锁定操作
				showErrorDialog();//弹出错误提示
				
			}else if(msg.what == 0x04){
//				Toast.makeText(MainActivity.this, "Result is NG! Multiple("+String.valueOf(msg.obj)+") Results Contained!", Toast.LENGTH_SHORT).show();
				mhandler.removeCallbacks(startConnectDBWithSoap);
				resultview.setImageDrawable(getResources().getDrawable(R.drawable.ng));
				detail.setText("获取到多个重复的匹配结果");
				detail.setTextColor(Color.RED);
				soundPool.play(1,1, 1, 0, 0, 1);//播放缓存池中第一个声音，即error
				
				IS_ALLLOCKED = true;//锁定操作
				showErrorDialog();//弹出错误提示
				
			}else if(msg.what == 0x05){
//				Toast.makeText(MainActivity.this, "Result is NG! Multiple("+String.valueOf(msg.obj)+") Results Contained!", Toast.LENGTH_SHORT).show();
				detail.setText("连接服务器失败，请检查网络！");
				detail.setTextColor(Color.BLUE);
			}else if(msg.what == 0x06){
				netstate.setText("网络连接状态：已连接");
				netstate.setTextColor(Color.BLACK);
			}else if(msg.what == 0x07){
				netstate.setText("网络连接状态：已断开");
				netstate.setTextColor(Color.RED);
			}else if(msg.what == 0x08){
				detail.setText("扫描结果不符合要求，请重新扫描！");
				resultview.setImageDrawable(getResources().getDrawable(R.drawable.ng));
				detail.setTextColor(Color.RED);
				
				soundPool.play(1,1, 1, 0, 0, 1);//播放缓存池中第一个声音，即error
				
				IS_ALLLOCKED = true;//锁定操作
				showErrorDialog();//弹出错误提示
				
				Toast.makeText(Letv_SoapMainActivity.this, "扫描结果不符合要求，请重新扫描！", Toast.LENGTH_SHORT).show();
			}else if(msg.what == 0x11){
				netstate.setText("网络连接状态："+(String)msg.obj);
				netstate.setTextColor(Color.BLACK);
			}else if(msg.what == 0x10){
				netstate.setText("网络连接状态："+(String)msg.obj);
				netstate.setTextColor(Color.RED);
			}else if(msg.what == 0x12){
				detail.setText("已扫描到预期结果，但文件存储或更新数据库失败");
				resultview.setImageDrawable(getResources().getDrawable(R.drawable.ng));
				detail.setTextColor(Color.BLACK);
				soundPool.play(1,1, 1, 0, 0, 1);//播放缓存池中第一个声音，即error
				IS_ALLLOCKED = true;//锁定操作
				showErrorDialog();//弹出错误提示
			}else if(msg.what == 0x13){
				//detail.setText("此机器SN码已抄录，但MAC未抄录或白平衡检测工位未通过！");
				detail.setText("此机器SN码已抄录，但MAC未抄录！");
				resultview.setImageDrawable(getResources().getDrawable(R.drawable.ng));
				detail.setTextColor(Color.RED);
				soundPool.play(1,1, 1, 0, 0, 1);//播放缓存池中第一个声音，即error
				IS_ALLLOCKED = true;//锁定操作
				showErrorDialog();//弹出错误提示
			}else if(msg.what == 0x14){
				detail.setText("此机器已经过出库扫描");
				resultview.setImageDrawable(getResources().getDrawable(R.drawable.ng));
				detail.setTextColor(Color.YELLOW);
				soundPool.play(1,1, 1, 0, 0, 1);//播放缓存池中第一个声音，即error
				IS_ALLLOCKED = true;//锁定操作
				showErrorDialog();//弹出错误提示
			}else if(msg.what == 0x15){
				String sum = (String)msg.obj;
				tvbatchsum.setText(sum);
				Log.i("BATCHSUM","查询到的总数"+sum);
				mhandler.removeCallbacks(getScanSumWithSoapRunnable);
				//----------------------判断本批次是否扫描完------------------------------------
				if(Integer.parseInt(sum)>=batchcountlimit){
					isbatched = false;
//					resultview.setImageDrawable(getResources().getDrawable(R.drawable.ng));
					detail.setText("本批次已扫描结束，请先建立新的批次信息！");
					detail.setTextColor(Color.RED);
					soundPool.play(1,1, 1, 0, 0, 1);//播放缓存池中第一个声音，即error
					IS_ALLLOCKED = true;//锁定操作
					showErrorDialog();//弹出错误提示
				}else{
//					detail.setText("扫描总数已刷新！");
//					detail.setTextColor(Color.BLACK);
					Log.i("SCAN", "扫描总数已刷新！");
				}
			}else if(msg.what == 0x16){
				if(batchlist.get(1).equals("EMPTY")){
					Toast.makeText(Letv_SoapMainActivity.this, "批次信息为空，请先建立批次存储表", Toast.LENGTH_SHORT).show();
				}else{
					isbatched = true;
					batch = batchlist.get(1);
					filename = batch+".xls";
					batchcountlimit = Integer.parseInt(batchlist.get(2));
					isComplete = batchlist.get(3);//批次完成状
				}
				tvbatch.setText(batch);
				tvbatchcountlimit.setText(""+batchlist.get(2)+"");
//				tvcount.setText(String.valueOf(dbsb.getRowsFromExcel(filename)));//使用读取Excel行数来获取本批次本机已扫描数量，
				//改为根据查询数据库本批次的记录数来进行赋
				int num = dbsb.getScannedSize(batch);
				
				tvcount.setText(String.valueOf(num));
				
				saveBatch();//存储当前批次信息到指定文
				
				mhandler.post(getScanSumWithSoapRunnable);//初始化完成之后开启数量查询线
			}else if(msg.what == 0x17){
				int num = dbsb.getScannedSize(batch);
				tvcount.setText(String.valueOf(num));
				detail.setText("条码已撤销成功");
				resultview.setImageDrawable(getResources().getDrawable(R.drawable.ok));
				detail.setTextColor(Color.GREEN);
				soundPool.play(2,1, 1, 0, 0, 1);//播放缓存池中第二个声音，即beep
			}else if(msg.what == 0x18){
				detail.setText("条码撤销失败");
				resultview.setImageDrawable(getResources().getDrawable(R.drawable.ng));
				detail.setTextColor(Color.RED);
				soundPool.play(1,1, 1, 0, 0, 1);//播放缓存池中第一个声音，即error
				IS_ALLLOCKED = true;//锁定操作
				showErrorDialog();//弹出错误提示
			}else if(msg.what == 0x19){
				detail.setText("此批次已锁定，不能进行操作！");
				resultview.setImageDrawable(getResources().getDrawable(R.drawable.ng));
				detail.setTextColor(Color.RED);
				Toast.makeText(Letv_SoapMainActivity.this,"此批次已锁定，不能进行操作！" , Toast.LENGTH_SHORT).show();
				soundPool.play(1,1, 1, 0, 0, 1);//播放缓存池中第一个声音，即error
				IS_ALLLOCKED = true;//锁定操作
				showErrorDialog();//弹出错误提示
			}else if(msg.what == 0x20){
				mydialog.cancel();
				detail.setText("Excel表格生成成功，本批次已锁定！");
				resultview.setImageDrawable(getResources().getDrawable(R.drawable.ok));
				detail.setTextColor(Color.GREEN);
				Toast.makeText(Letv_SoapMainActivity.this,"Excel表格生成成功，本批次已锁定！" , Toast.LENGTH_SHORT).show();
			}else if(msg.what == 0x21){
				mydialog.cancel();
				isComplete = "NO";
				batchbuilder.updateFlag(batch, isComplete);//失败时将扫描标志位置为�?�NO
				detail.setText("Excel表格生成失败");
				resultview.setImageDrawable(getResources().getDrawable(R.drawable.ng));
				detail.setTextColor(Color.RED);
				Toast.makeText(Letv_SoapMainActivity.this,"Excel表格生成失败，请重新生成" , Toast.LENGTH_SHORT).show();
				soundPool.play(1,1, 1, 0, 0, 1);//播放缓存池中第一个声音，即error
				IS_ALLLOCKED = true;//锁定操作
				showErrorDialog();//弹出错误提示
			}else if(msg.what == 0x22){
				mydialog.cancel();
				isComplete = "NO";
				batchbuilder.updateFlag(batch, isComplete);//失败时将扫描标志位置为�?�NO
				detail.setText("本批次扫描结果为空！");
				resultview.setImageDrawable(getResources().getDrawable(R.drawable.ng));
				detail.setTextColor(Color.RED);
				Toast.makeText(Letv_SoapMainActivity.this,"请扫描之后再进行生成操作" , Toast.LENGTH_SHORT).show();
				soundPool.play(1,1, 1, 0, 0, 1);//播放缓存池中第一个声音，即error
				IS_ALLLOCKED = true;//锁定操作
				showErrorDialog();//弹出错误提示
			} else if(msg.what == 0x23){
				detail.setText("此条码不在本批次内，撤销操作失败");
				resultview.setImageDrawable(getResources().getDrawable(R.drawable.ng));
				detail.setTextColor(Color.RED);
				soundPool.play(1,1, 1, 0, 0, 1);//播放缓存池中第一个声音，即error
				IS_ALLLOCKED = true;//锁定操作
				showErrorDialog();//弹出错误提示
			} else if(msg.what == 0x24){
				detail.setText("本批次扫描结束或批次信息未建!");
				detail.setTextColor(Color.RED);
				resultview.setImageDrawable(getResources().getDrawable(R.drawable.ng));
				Toast.makeText(Letv_SoapMainActivity.this, "本批次扫描结束或批次信息未建立，请先建立批次存储表！", Toast.LENGTH_SHORT).show();
				soundPool.play(1,1, 1, 0, 0, 1);//播放缓存池中第一个声音，即error
				IS_ALLLOCKED = true;//锁定操作
				showErrorDialog();//弹出错误提示
			}
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.new_layout);
		
		LogcatFileManager.getInstance().startLogcatManager(this);//启LogcatFileManager保存错误信息
		
		//--------------------------等待---------------------------
		mydialog = new ProgressDialog(this);
		mydialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
		mydialog.setTitle("等待Excel生成");
		mydialog.setMessage("Excel正在生成...");
		mydialog.setCancelable(true);
		//---------------------------声音缓冲池初始化----------------
		soundPool= new SoundPool(3,AudioManager.STREAM_SYSTEM,5);
		soundPool.load(this,R.raw.error,1);
		soundPool.load(this, R.raw.beep, 1);
//		soundPool.play(1,1, 1, 0, 0, 1);
		
		Flag = FLAG_OUTBOUND;//标志位置“出库
		
		batch = "L123";
		
		batchbuilder =new Letv_BatchDBSheetBuilder(this);//批次存储数据库初始化
		
		initPath();//初始化存储路
		
		checkNetState();//查询网络状
		
		dbsb = new Letv_DBSheetBuilder(this);//初始化数据库操作类对象dbsb
		
		//-------------------------------扫描查询处理-----------------------------------------
		thread.start();
		mhandler = new Handler(thread.getLooper())//使用HandlerThread的looper对象创建Handler，如果使用默认的构�?�方法，很有可能阻塞UI线程 
		{
			@Override
			public void handleMessage(Message msg){
				if(msg.what == 0x01){

				}else if(msg.what == 0x02){
					
				}else if(msg.what == 0x03){
				
				}else if(msg.what == 0x04){
					
				}
			}
		};
		
		

		resultview = (ImageView)findViewById(R.id.resultimg);
		et_code = (TextView) findViewById(R.id.codevalue);
		detail = (TextView)findViewById(R.id.detail);
		netstate = (TextView)findViewById(R.id.netstate);
		tvbatch = (TextView)findViewById(R.id.tvbatch);
		tvcount = (TextView)findViewById(R.id.tvcount);
		tvbatchcountlimit = (TextView)findViewById(R.id.tvbatchcountlimit);
		tvbatchsum = (TextView)findViewById(R.id.tvbatchsum);
		tvbatch.setText(batch);
		
		btnBatchList = (Button)findViewById(R.id.btnbatch);
		btnBatchList.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				String[ ] str = batchbuilder.getBatchList();
				showBatchList(str);
			}
		});
		btnIsComplete = (Button)findViewById(R.id.btncomplete);
		btnIsComplete.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(isComplete.equals("NO")){
				changeFlagDialog();
				}else if(isComplete.equals("YES")){
					Message msg = Message.obtain(mainhandler);
					msg.what = 0x19;
					msg.sendToTarget();
				}
			}
		});
		btnOutBound = (Button)findViewById(R.id.btnoutbound);
		btnOutBound.setTextColor(Color.RED);//初始状态为出库扫描状态
		btnOutBound.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				
				if(isComplete.equals("NO")){
					showOutBoundDialog();
					}else if(isComplete.equals("YES")){
						Message msg = Message.obtain(mainhandler);
						msg.what = 0x19;
						msg.sendToTarget();
					}

			}
		});
		btnRevocation = (Button)findViewById(R.id.btnrevocation);
		btnRevocation.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				
				if(isComplete.equals("NO")){
					showRevocationDialog();
					}else if(isComplete.equals("YES")){
						Message msg = Message.obtain(mainhandler);
						msg.what = 0x19;
						msg.sendToTarget();
					}
			}
		});

		et_code.setText("");
		
		btnEdit = (Button) findViewById(R.id.btnback);
		btnEdit.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				exitActivity();
			}
		});
		
		emptyBtn = (Button) findViewById(R.id.btnclear);
		emptyBtn.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				et_code.setText("");
			}
		});
		
		btnOpen = (Button) findViewById(R.id.btnscan);
		btnOpen.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				SerialPort.CleanBuffer();
				CaptureService.scanGpio.openScan();
				resultview.setImageDrawable(getResources().getDrawable(R.drawable.wait));
				detail.setText("");
			}

		});

		Intent newIntent = new Intent(Letv_SoapMainActivity.this, CaptureService.class);
		newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startService(newIntent);

		getOverflowMenu();

		scanBroadcastReceiver = new ScanBroadcastReceiver();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction("com.zkc.scancode");
		this.registerReceiver(scanBroadcastReceiver, intentFilter);
		
		wsb = new WiFiStateBroadcastReceiver(mainhandler);
		IntentFilter filter=new IntentFilter();
		filter.addAction(WifiManager.RSSI_CHANGED_ACTION);
		filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		this.registerReceiver(wsb,filter); 
		
		new Thread(initBatch).start();
	}
	
	
	private Runnable initBatch = new Runnable(){

		@Override
		public void run() {
			// TODO Auto-generated method stub
			batchlist.clear();
			initPro();
//			batchlist = batchbuilder.getLatestBatch();//获取当前批次信息
			File f = new File(currentbatchpath);
			if(f.exists()){
				Properties properties = Letv_ConfigActivity.loadConfig(Letv_SoapMainActivity.this, currentbatchpath);
				batch = (String)properties.get("batch");
				batchlist = batchbuilder.getCurrentBatch(batch);
			}else{
				
				batchlist = batchbuilder.getLatestBatch();
			
			}
			
			Message msg = Message.obtain(mainhandler);
			msg.what = 0x16;
//			msg.obj = batchlist;
			msg.sendToTarget();
			
		}
		
	};
	
	private Runnable changeBatch = new Runnable(){

		@Override
		public void run() {
			// TODO Auto-generated method stub
			batchlist.clear();
			batchlist = batchbuilder.getLatestBatch();//获取当前批次信息
			Message msg = Message.obtain(mainhandler);
			msg.what = 0x16;
			msg.sendToTarget();
		}
		
	};
	
	
	private void initPath(){
		File file = new File(Environment.getExternalStorageDirectory() + File.separator+"ScanResultData"+File.separator+"Data");
		if(!file.exists()){
		makeDir(file);
		}
		File file1 = new File(Environment.getExternalStorageDirectory() + File.separator+"ScanResultData"+File.separator+"Query");
		if(!file1.exists()){
		makeDir(file1);
		}
		File file2 = new File(Environment.getExternalStorageDirectory() + File.separator+"ScanResultData"+File.separator+"Total");
		if(!file2.exists()){
			makeDir(file2);
		}
		File file3 = new File(Environment.getExternalStorageDirectory() + File.separator+"ScanResultData"+File.separator+"Config");
		if(!file3.exists()){
		makeDir(file3);
		}
	}
	
	private void makeDir(File dir){
		if (!dir.getParentFile().exists()) {
			makeDir(dir.getParentFile());
		}
		dir.mkdir();
	}

	private void getOverflowMenu() {
		try {
			ViewConfiguration config = ViewConfiguration.get(this);
			Field menuKeyField = ViewConfiguration.class
					.getDeclaredField("sHasPermanentMenuKey");
			if (menuKeyField != null) {
				menuKeyField.setAccessible(true);
				menuKeyField.setBoolean(config, false);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*--------------------------------------------------------------------
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		menu.add(0, 1, 1, R.string.action_1d);
		menu.add(0, 2, 2, R.string.action_2d);
		menu.add(0, 3, 3, R.string.sendemail);
		menu.add(0, 4, 4, R.string.rebuildsheet);
		menu.add(0, 5, 5, R.string.localdb);
		menu.add(0, 6, 6, R.string.configuration);
		menu.add(0, 7, 7, R.string.batchshow);
		menu.add(0,8,8,R.string.input);
		return super.onCreateOptionsMenu(menu);

	}

	@Override
	public boolean onOptionsItemSelected(android.view.MenuItem item) {
		if (item.getItemId() == 1) {
			Intent intent = new Intent();
			intent.setClass(Letv_SoapMainActivity.this, ActivityBarcodeSetting.class);
			startActivity(intent);
		} else if (item.getItemId() == 2) {
			Intent intent = new Intent();
			intent.setClass(Letv_SoapMainActivity.this, ActivityQrcodeSetting.class);
			startActivity(intent);
		} else if (item.getItemId() == 3) {
			sendEmail();
		} else if (item.getItemId() == 4) {
			showRebuildDialogWithoutExcel();
		} else if (item.getItemId() == 5) {
			startActivity(new Intent(Letv_SoapMainActivity.this,Letv_DBHandleActivity.class));
		} else if (item.getItemId() == 6) {
			startActivity(new Intent(Letv_SoapMainActivity.this,Letv_ConfigActivity.class));
		} else if (item.getItemId() == 7) {
			startActivity(new Intent(Letv_SoapMainActivity.this,Letv_BatchHandleActivity.class));
		} else if (item.getItemId() == 8) {
			showSNInputDialog();
		}
		return super.onOptionsItemSelected(item);
	}
	---------------------------------------------------------------------------*/
	
	private void sendEmail(){//发送邮件
    	Intent email = new Intent(android.content.Intent.ACTION_SEND);  
    	// 附件
    	 File file = new File(Environment.getExternalStorageDirectory()+ File.separator + "ScanResultData"+ File.separator+filename);
    	 Log.i("FilePath",file.getAbsolutePath());
    	 if(file.exists()){
//    	 File file = new File(Environment.getExternalStorageDirectory().getPath()+ File.separator + "Family"+ File.separator+"bill.xls");  
    	//邮件发送类型：带附件的邮件  
    	email.setType("application/octet-stream");
    	 //邮件接收者（数组，可以是多位接收者）  
    	String[] emailReciver = receiver;
    	  
    	if(receiver.length != 0){
    	String  emailTitle = "出库扫描结果列表";  
    	String emailContent = "附件为出库扫描的结果表格，请查收";  
    	//设置邮件地址  
    	email.putExtra(android.content.Intent.EXTRA_EMAIL, emailReciver);  
    	//设置邮件标题  
    	 email.putExtra(android.content.Intent.EXTRA_SUBJECT, emailTitle);  
    	//设置发送的内容  
    	email.putExtra(android.content.Intent.EXTRA_TEXT, emailContent);  
    	//附件  
    	email.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));  
    	 //调用系统的邮件系  
    	startActivity(Intent.createChooser(email, "请选择邮件发送软件"));
    	}else{
    		Toast.makeText(Letv_SoapMainActivity.this, "收件人未指定", Toast.LENGTH_SHORT).show();
    	}
    	 }else{
    		 Toast.makeText(Letv_SoapMainActivity.this, "指定附件不存在，是否已经进行了扫描？", Toast.LENGTH_SHORT).show();
    	 }
    }

	@Override
	protected void onResume() {//活动重建
		System.out.println("onResume" + "open");
		Log.v("onResume", "open");
		super.onResume();
		
		new Thread(initBatch).start();//刷新显示
		
	}
	
	private void initPro(){
		File pf = new File(Properfile);//初始化properfile文件
		if(pf.exists()){
		Properties properties = Letv_ConfigActivity.loadConfig(this, Properfile);
		String str = (String)properties.get("members");
		if(!str.equals("L")){//由于序列化时是以“L”为起始位，故当list内容为空时反序列化时依然会存在“L”这个字
		List<Object> templist = StringTransUtils.StringToList(str);
		receiver= new String[templist.size()];
		for(int i=0;i<templist.size();i++){
			receiver[i]=(String)templist.get(i);
		}
		}
		}
	}

	@Override
	public void onBackPressed() {
		exitActivity();
	}

	@Override
	protected void onDestroy() {
		this.unregisterReceiver(scanBroadcastReceiver);
		this.unregisterReceiver(wsb);
		LogcatFileManager.getInstance().stopLogcatManager();//关闭LogcatFileManager
//		dbsb.closeDB();//单例对象，不要关闭数据库操作
//		batchbuilder.closeDB();
		super.onDestroy();
	}

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
	
	@SuppressWarnings("unused")
	private void showRebuildDialog(){
		
		LayoutInflater factory = LayoutInflater.from(this);  
        final View textEntryView = factory.inflate(R.layout.choose_view, null);
        final EditText edtbatch = (EditText) textEntryView.findViewById(R.id.new_batch);  
        final EditText edtsum = (EditText)textEntryView.findViewById(R.id.sum); 
		
		new AlertDialog.Builder(Letv_SoapMainActivity.this).setIcon(android.R.drawable.btn_star)  
	    .setTitle("批次")
	    .setIcon(android.R.drawable.btn_star)
//	    .setMessage("输入新的批次名：")
	    .setView(textEntryView)
//	    .setMessage("请输入本批次机器总量")
//	    .setView(batchsize)
	    .setPositiveButton("确定",new DialogInterface.OnClickListener() {
	                @Override  
	                public void onClick(DialogInterface dialog,int which) { 
	                    // TODO Auto-generated method stub  
	                	
	                	String batchname = edtbatch.getText().toString();
	                	String batchsum = edtsum.getText().toString();
	                	
	                	if((!batchname.isEmpty())&&(isNumber(batchsum))&&(Integer.parseInt(batchsum)>0)&&(!batchname.equals("EMPTY"))&&(!batchname.equals("NO"))){
	                	
	                	String oldpath = Environment.getExternalStorageDirectory()+File.separator+"ScanResultData"+File.separator+filename;
//	                	String newname = new SimpleDateFormat("yyyy-MM-dd HH-mm-ss",Locale.CHINA).format(new Date())+".xls";
	                	String newname = filename;
	                	String newpath = Environment.getExternalStorageDirectory()+File.separator+"ScanResultData"+File.separator+"Data"+File.separator+newname ;
	                    File oldfile = new File(oldpath);
	                    File newfile = new File(newpath);
	                    if(!oldfile.exists()){
	                    	Toast.makeText(Letv_SoapMainActivity.this, "记录表格不存在，请先进行扫描", Toast.LENGTH_SHORT).show();
	                    }else{
	                    if(!newfile.exists()){
	                    	try {
								if(batchbuilder.saveBatch(batchname, batchsum)){
									newfile.createNewFile();
									FileUtils.copyFile(oldpath, newpath);
									FileUtils.deleteFile(oldpath);
									Toast.makeText(Letv_SoapMainActivity.this, "原表格已备份，新记录表已创建", Toast.LENGTH_SHORT).show();
//									new Thread(initBatch).start();
								}else{
									Toast.makeText(Letv_SoapMainActivity.this, "数据库存储失败或批次号已存在，请重新创建", Toast.LENGTH_SHORT).show();	
								}
							}catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
								Toast.makeText(Letv_SoapMainActivity.this, "原表格备份失败，请重新创建新记录表！", Toast.LENGTH_SHORT).show();
							}
	                    }else{
							if(batchbuilder.saveBatch(batchname, batchsum)){
								FileUtils.copyFile(oldpath, newpath);
								FileUtils.deleteFile(oldpath);
								Toast.makeText(Letv_SoapMainActivity.this, "原表格已备份，新记录表已创建", Toast.LENGTH_SHORT).show();
							}else{
								Toast.makeText(Letv_SoapMainActivity.this, "数据库存储失败，请重新创建！", Toast.LENGTH_SHORT).show();	
							}
	                    }
	                    }
	                }else{
	                	Toast.makeText(Letv_SoapMainActivity.this, "输入参数不符合要求，请重新输入！", Toast.LENGTH_SHORT).show();
	                }
	                }
	            })  
	    .setNegativeButton("取消",new DialogInterface.OnClickListener() {
	                @Override  
	                public void onClick(DialogInterface dialog,int which) {
	                    // TODO Auto-generated method stub  
	                    
	                }  
	            }).show();
	}
	
private void showRebuildDialogWithoutExcel(){
		
		LayoutInflater factory = LayoutInflater.from(this); 
        final View textEntryView = factory.inflate(R.layout.choose_view, null);
        final EditText edtbatch = (EditText) textEntryView.findViewById(R.id.new_batch);  
        final EditText edtsum = (EditText)textEntryView.findViewById(R.id.sum); 
		
		new AlertDialog.Builder(Letv_SoapMainActivity.this).setIcon(android.R.drawable.btn_star)  
	    .setTitle("批次")
	    .setIcon(android.R.drawable.btn_star)
	    .setView(textEntryView)
	    .setPositiveButton("确定",new DialogInterface.OnClickListener() {
	                @Override  
	                public void onClick(DialogInterface dialog,int which) { 
	                    // TODO Auto-generated method stub  
	                	
	                	String batchname = edtbatch.getText().toString();
	                	String batchsum = edtsum.getText().toString();
	                	
	                	if((isInputCorrect(batchname))&&(!batchname.isEmpty())&&(!batchsum.isEmpty())&&(isNumber(batchsum))&&(Integer.parseInt(batchsum)>0)&&(!batchname.equals("EMPTY"))&&(!batchname.equals("NO"))){
	                	
	                		if(batchbuilder.saveBatch(batchname, batchsum)){
	                			Toast.makeText(Letv_SoapMainActivity.this, "数据库写入成功，新批次已建立", Toast.LENGTH_SHORT).show();
	                			new Thread(changeBatch).start();
	                		}else{
	                			Toast.makeText(Letv_SoapMainActivity.this, "数据库写入失败，新批次未建立", Toast.LENGTH_SHORT).show();
	                		}
	               
	                }else{
	                	Toast.makeText(Letv_SoapMainActivity.this, "输入参数不符合要求(不能包含除大小写字母及数字以外的字符)，请重新输入", Toast.LENGTH_SHORT).show();
	                }
	                }
	            })  
	    .setNegativeButton("取消",new DialogInterface.OnClickListener() {
	                @Override  
	                public void onClick(DialogInterface dialog,int which) {
	                    // TODO Auto-generated method stub  
	                    
	                }  
	            }).show();
	}
	
	public void showBatchList(final String[ ] str){//弹出批次列表
		new AlertDialog.Builder(Letv_SoapMainActivity.this).setTitle("选择批次") 
		.setSingleChoiceItems(str, 0, new DialogInterface.OnClickListener() {
		 @Override
		public void onClick(DialogInterface dialog, int item) {
		        Toast.makeText(getApplicationContext(), str[item], Toast.LENGTH_SHORT).show(); 
		        batch = str[item];
		        batchlist =  batchbuilder.getCurrentBatch(batch);
		        
		        Message msg = Message.obtain(mainhandler);
		    	msg.what = 0x16;
				msg.sendToTarget();
		        
		        dialog.cancel(); 
		  } 
		}).show();//显示对话 
	}
	
private void showSNInputDialog(){
		
        final EditText edtinput = new EditText(Letv_SoapMainActivity.this); 
		
		new AlertDialog.Builder(Letv_SoapMainActivity.this).setIcon(android.R.drawable.btn_star)  
	    .setTitle("SN码输入")
	    .setIcon(android.R.drawable.btn_star)
	    .setMessage("手动输入SN码：")
	    .setView(edtinput)
//	    .setMessage("请输入本批次机器总量")
//	    .setView(batchsize)
	    .setPositiveButton("确定",new DialogInterface.OnClickListener() {
	                @Override  
	                public void onClick(DialogInterface dialog,int which) { 
	                    // TODO Auto-generated method stub  
	                	
	                String sn = edtinput.getText().toString();
	                	
	                	if((!sn.isEmpty())&&(sn.length()==19)&&isSuited(sn)){
	                	
	                		codeScan = sn;//将手动输入的值设置为codeScan
	                		et_code.setText(sn);//显示输入的码
	                		if(Flag.equals(FLAG_OUTBOUND)){
	                			mhandler.post(startConnectDBWithSoap);//出库操作
	                			}else if(Flag.equals(FLAG_REVOCATION)){
	                				mhandler.post(startRevocationWithSoap);//撤销操作
	                			}
	            			mhandler.post(getScanSumWithSoapRunnable);//数量查询线程
	                	
	                }else{
	                	Toast.makeText(Letv_SoapMainActivity.this, "输入参数不符合要求，请重新输入！", Toast.LENGTH_SHORT).show();
	                }
	                }
	            })  
	    .setNegativeButton("取消",new DialogInterface.OnClickListener() {
	                @Override  
	                public void onClick(DialogInterface dialog,int which) {
	                    // TODO Auto-generated method stub  
	                    
	                }  
	            }).show();// show很关   
	}

private void changeFlagDialog(){
	
	new AlertDialog.Builder(Letv_SoapMainActivity.this).setIcon(android.R.drawable.btn_star)  
    .setTitle("批次扫描状态")
    .setIcon(android.R.drawable.btn_star)
    .setMessage("确定本批次已扫描完成")
    .setPositiveButton("确定",new DialogInterface.OnClickListener() {
                @Override  
                public void onClick(DialogInterface dialog,int which) { 
                    // TODO Auto-generated method stub  
                	isComplete = "YES";
                	if((batchbuilder.updateFlag(batch, isComplete))>0){
                		mydialog.show();
                		new Thread(buildExcel).start();//启Excel生成线程
                	}else{
                		Toast.makeText(Letv_SoapMainActivity.this, "批次信息更新失败", Toast.LENGTH_SHORT).show();
                	}
                }
            })  
    .setNegativeButton("取消",new DialogInterface.OnClickListener() {
                @Override  
                public void onClick(DialogInterface dialog,int which) {
                    // TODO Auto-generated method stub  
                    
                }  
            }).show();   
}

private void showOutBoundDialog(){
	
	new AlertDialog.Builder(Letv_SoapMainActivity.this).setIcon(android.R.drawable.btn_star)  
    .setTitle("出库状态转换")
    .setIcon(android.R.drawable.btn_star)
    .setMessage("确定将扫描状态设置为出库状态？")
    .setPositiveButton("确定",new DialogInterface.OnClickListener() {
                @Override  
                public void onClick(DialogInterface dialog,int which) { 
                    // TODO Auto-generated method stub  
                Flag = FLAG_OUTBOUND;
                btnOutBound.setTextColor(Color.RED);
                btnRevocation.setTextColor(Color.BLACK);
                Toast.makeText(Letv_SoapMainActivity.this, "已将扫描状态设置为出库操作状态！", Toast.LENGTH_SHORT).show();
                }
            })  
    .setNegativeButton("取消",new DialogInterface.OnClickListener() {
                @Override  
                public void onClick(DialogInterface dialog,int which) {
                    // TODO Auto-generated method stub  
                    
                }  
            }).show();// show很关   
}

private void showRevocationDialog(){
	
	new AlertDialog.Builder(Letv_SoapMainActivity.this).setIcon(android.R.drawable.btn_star)  
    .setTitle("入库状态转换")
    .setMessage("确定将扫描状态设置为撤销操作状态？")
    .setPositiveButton("确定",new DialogInterface.OnClickListener() {
                @Override  
                public void onClick(DialogInterface dialog,int which) { 
                    // TODO Auto-generated method stub  
                	Flag = FLAG_REVOCATION;
                	btnOutBound.setTextColor(Color.BLACK);
                    btnRevocation.setTextColor(Color.RED);
                    Toast.makeText(Letv_SoapMainActivity.this, "已将扫描状态设置为撤销操作状态！", Toast.LENGTH_SHORT).show();
                }
            })  
    .setNegativeButton("取消",new DialogInterface.OnClickListener() {
                @Override  
                public void onClick(DialogInterface dialog,int which) {
                    // TODO Auto-generated method stub  
                    
                }  
            }).show();  
}

private void showErrorDialog(){
	new AlertDialog.Builder(Letv_SoapMainActivity.this).setIcon(android.R.drawable.btn_star)
	.setCancelable(false)//设置点击确定键以外的地方不起作用
    .setTitle("错误警报")
    .setMessage("扫描到条码\n"+codeScan+"\n发生错误，请重新扫描或检查设置！")
    .setPositiveButton("确定",new DialogInterface.OnClickListener() {
                @Override  
                public void onClick(DialogInterface dialog,int which) { 
                    // TODO Auto-generated method stub  
                	IS_ALLLOCKED = false;
                }
            }).show();
}
	
private Runnable buildExcel = new Runnable(){

	@Override
	public void run() {
		// TODO Auto-generated method stub
		ArrayList<ArrayList<String>>mylist = new ArrayList<ArrayList<String>>();
		Cursor cur = dbsb.exeSQL("select * from family_bill_letv where batch = '"+batch+"'");
		while (cur.moveToNext()) {
			ArrayList<String> beanList=new ArrayList<String>();
			beanList.add(cur.getString(1));
			beanList.add(cur.getString(2));
			beanList.add(cur.getString(3));
			mylist.add(beanList);
		}
		if(!mylist.isEmpty()){
			Log.i("MYLISTSIZE", "数据个数为："+String.valueOf(mylist.size())+"");
		if(dbsb.saveDataToExcel(mylist, filename.replace("\n", "a"))){//若批次名称中包含"\n"换行符，则替换为"a",文件命名不能包含换行符（这是个曾经出现的BUG）
			Message msg = Message.obtain(mainhandler);
			msg.what = 0x20;
			msg.sendToTarget();
		}else{
			Message msg = Message.obtain(mainhandler);
			msg.what = 0x21;
			msg.sendToTarget();
		}
		}else{
			Message msg = Message.obtain(mainhandler);
			msg.what = 0x22;
			msg.sendToTarget();
		}
		cur.close();
	}
	
};

/**
 * 外网通过WebService连接数据库所使用的线程
 */
	private Runnable startConnectDBWithSoap = new Runnable(){
    	@Override
		public void run(){
    		
    		Log.i("SoapStart","开启外网连接数据库线程！");
    		
    		String outbond = "";
    		String whitebalancestate = "";
    		String mac = "";		
				
					 scanresultmap = dbUtils.getMessageWithSN_Letv(codeScan);//获取外网查询到的结果
					 
					 Log.i("ScanResultMap", scanresultmap.toString());
					 
					 if(scanresultmap.get("KEYCODE")==null){
					 
					 whitebalancestate = scanresultmap.get("WHITEBALANCE");//白平衡检测状态
					 mac = scanresultmap.get("MAC");
					 outbond = scanresultmap.get("OQC");
					 
					 if(mac == null){//判断字符串是否为空
						 mac = "qwq";
					 }
					 if(whitebalancestate == null){
						 whitebalancestate = "qwq";
					 }
					 if(outbond == null){
						 outbond = "qwq";
					 }
					 
					 Log.i("Meassage","mac is："+mac+";whitebalancestate is："+whitebalancestate+";outbond is："+outbond );
					 
					 if(mac.length()==12){
				     //if((mac.length()==12)&&(whitebalancestate.equals("YES"))){
						 
						 if(outbond.equals("NO")){
					 
					 boolean isupdate = false;
					 boolean isok = dbsb.saveDataWithBatch_Map(scanresultmap, batch);
					 
					 if(isok){//本地存储成功之后再进行远程更新，防止远程更新之后不能进行本地存储操作
						 
						 isupdate = dbUtils.updateOutbondState_Letv(codeScan, batch);//远程更新数据库记录
					 
					 }
					 
					 if(isok&&isupdate){
						 
						 int count = dbsb.getScannedSize(batch);
						 
	    	          Message msg1 = Message.obtain(mainhandler);
	    	          msg1.what = 0x02;
	    	          msg1.obj = count;
	    	          msg1.sendToTarget();
	    	          
					 }
					 else{
						 dbsb.deleteFromDB(scanresultmap.get("SN"));//文件存储失败则删除刚刚插入数据库中的记录
						 Log.i("SAVEDATA", "已扫描到预期结果，但文件存储失败或更新数据库失败");//判定为NG，需要进行Message信息传送
						 Message msg1 = Message.obtain(mainhandler);
		    	          msg1.what = 0x012;
		    	          msg1.sendToTarget();
						 }
					 
						 }else{// else if(out.equals("YES"))
							 dbsb.saveDataWithBatch_Map(scanresultmap, batch);//保险措施，应用于远程数据库已更新但是本地数据库未更新的情况，但是逻辑上应该不存在这种状况，暂时还没发现逻辑漏洞
							 Message msg = Message.obtain(mainhandler);
							 msg.what = 0x14;//检测是否已经过出库扫描
							 msg.sendToTarget();
						 }
					 
				 }else{
					 Message msg = Message.obtain(mainhandler);
					 msg.what = 0x13;//检测MAC是否已抄录，白平衡检测工位是否已经检测OK
					 msg.sendToTarget();
				 }
    	}else if(scanresultmap.get("KEYCODE").equals("N")){//乐视SN查询失败时返回值为 "N" 而不是 "NO"
    		 Message msg1 = Message.obtain(mainhandler);
			 msg1.what = 0x03;
			 msg1.sendToTarget();
    	}
    		
    		}
    	};
    	
    	/**
    	 * 进行撤销操作使用的线程
    	 */
    	private Runnable startRevocationWithSoap = new Runnable(){//启动撤销操作
        	@Override
    		public void run(){
        		
        		Cursor cur = dbsb.exeSQL("select * from family_bill_letv where SN = '"+codeScan+"' and batch = '"+batch+"'");
        		
        		if(cur.getCount()>0){
        		
				    				dbUtils.updateOutbondState_Letv(codeScan, "NO");
				    				 
				    				 dbsb.deleteFromDB(codeScan);
				    				 
				    					 Message msg = Message.obtain(mainhandler);
				    					 msg.what = 0x17;
				    					 msg.sendToTarget();
    			
        		}else{
        			Message msg = Message.obtain(mainhandler);
        			msg.what = 0x23;//撤消操作的机器不在本批次之内
        			msg.sendToTarget();
        		}
        		
//        		}
        		}
        	};
    	
        /**
         * 获取批次机器总量的线程
         */
    	private Runnable getScanSumWithSoapRunnable = new Runnable(){

			@Override
			public void run(){
				// TODO Auto-generated method stub
	    		
				int count = dbUtils.getSumWithOutbondState_Letv(batch);
				 
				 Message msg = Message.obtain(mainhandler);
				 msg.what = 0x15;
				 msg.obj = String.valueOf(count);
				 msg.sendToTarget();
	    		
			}
    		
    	};
    	
    	/**
         * 查当前网络是否可用
         * 
         * @param context
         * @return
         */
        public boolean isNetworkAvailable(Activity activity)
        {
            Context context = activity.getApplicationContext();
            // 获取手机有连接管理对象（包括对wi-fi,net等连接的管理
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            
            if (connectivityManager == null)
            {
                return false;
            }
            else
            {
                // 获取NetworkInfo对象
                NetworkInfo[] networkInfo = connectivityManager.getAllNetworkInfo();
                
                if (networkInfo != null && networkInfo.length > 0)
                {
                    for (int i = 0; i < networkInfo.length; i++)
                    {
                        System.out.println(i + "===状===" + networkInfo[i].getState());
                        System.out.println(i + "===类型===" + networkInfo[i].getTypeName());
                        // 判断当前网络状态是否为连接状
                        if (networkInfo[i].getState() == NetworkInfo.State.CONNECTED)
                        {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
        
        private void checkNetState(){
        	boolean netstate = isNetworkAvailable(Letv_SoapMainActivity.this);
            if(netstate){
          	  Message msg = Message.obtain(mainhandler);
          	  msg.what = 0x06;
          	  msg.sendToTarget();
            }else{
          	  Message msg = Message.obtain(mainhandler);
          	  msg.what = 0x07;
          	  msg.sendToTarget();
            }
        }
        
        @SuppressWarnings("unused")
		private Runnable task =new Runnable() {
            @Override
			public void run() {
                // TODOAuto-generated method stub  
                   //要执行的代码  
                  boolean netstate = isNetworkAvailable(Letv_SoapMainActivity.this);
                  if(netstate){
                	  Message msg = Message.obtain(mainhandler);
                	  msg.what = 0x06;
                	  msg.sendToTarget();
                  }else{
                	  Message msg = Message.obtain(mainhandler);
                	  msg.what = 0x07;
                	  msg.sendToTarget();
                  }
                  mainhandler.postDelayed(this,3000);//设置延迟时间，此处是3  
            }     
         };  
        

	class ScanBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			// TODO Auto-generated method stub
			
			if(!IS_ALLLOCKED){//当未全局锁定时
			
			if(isComplete.equals("NO")){
			if(isbatched){
			String text = intent.getExtras().getString("code");
			Log.i(TAG, "MyBroadcastReceiver code:" + text);
			et_code.setText(text);
			if(isSuited(text)&&(text.length()==19)){//乐视条码位数为19位
			codeScan = text;
			if(Flag.equals(FLAG_OUTBOUND)){
			mhandler.post(startConnectDBWithSoap);
			}else if(Flag.equals(FLAG_REVOCATION)){
				mhandler.post(startRevocationWithSoap);
			}
			mhandler.post(getScanSumWithSoapRunnable);//数量查询线程
			}else{
				Message msg = Message.obtain(mainhandler);
				msg.what = 0x08;
				msg.sendToTarget();
			}
		}else{
			Message msg = Message.obtain(mainhandler);
			msg.what = 0x24;
			msg.sendToTarget();
		}
		}else if(isComplete.equals("YES")){
			Message msg = Message.obtain(mainhandler);
			msg.what = 0x19;
			msg.sendToTarget();
		}
		}else{//当全锁定时，拉响警报
			soundPool.play(1,1, 1, 0, 0, 1);//拉响警报
		}
		}
	}
	
	public static boolean isSuited(String str) {//判断字符串是否全部为大写字母或数
		  String regex = "^[0-9A-Z]+$";
		  return str.matches(regex);
		 }
	
	public static boolean isInputCorrect(String str){
		String regex = "^[0-9a-zA-Z]+$";
		return str.matches(regex);
	}
	
	public static boolean isNumber(String str){
		Pattern p = Pattern.compile("[0-9]*"); 
	     Matcher m = p.matcher(str); 
	     if(m.matches() ){
//	      Toast.makeText(Main.this,"输入的是数字", Toast.LENGTH_SHORT).show();
	    	 return true;
	      }else{
	    	  return false;
	      }
	}
	
	private void saveBatch(){
		Properties properties = new Properties();
		properties.put("batch", batch);
		Letv_ConfigActivity.saveConfig(Letv_SoapMainActivity.this, currentbatchpath, properties);
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
		} else if (keyCode == 135) {//135为"SCAN"键键值
			Log.i("KEYCODE","KEY_SCAN");
			lockButton();
			return true;
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
							Intent intent = new Intent();
							intent.setClass(Letv_SoapMainActivity.this, ActivityBarcodeSetting.class);
							startActivity(intent);
						} else if (position == 1) {
							popupWindow.dismiss();//pop消失
							Intent intent = new Intent();
							intent.setClass(Letv_SoapMainActivity.this, ActivityQrcodeSetting.class);
							startActivity(intent);
						} else if (position == 2) {
							popupWindow.dismiss();//pop消失
							sendEmail();
						} else if (position == 3) {
							popupWindow.dismiss();//pop消失
							showRebuildDialogWithoutExcel();
						} else if (position == 4) {
							popupWindow.dismiss();//pop消失
							startActivity(new Intent(Letv_SoapMainActivity.this,Letv_DBHandleActivity.class));
						} else if (position == 5) {
							popupWindow.dismiss();//pop消失
							startActivity(new Intent(Letv_SoapMainActivity.this,Letv_ConfigActivity.class));
						} else if (position == 6) {
							popupWindow.dismiss();//pop消失
							startActivity(new Intent(Letv_SoapMainActivity.this,Letv_BatchHandleActivity.class));
						} else if (position == 7) {
							popupWindow.dismiss();//pop消失
							showSNInputDialog();
						} else if (position == 8) {
							popupWindow.dismiss();//pop消失
							unlockButton();
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
		 	mpwlist.add("一维头设置");
			mpwlist.add("二维头设置");
			mpwlist.add("发送邮件");
			mpwlist.add("新建批次");
			mpwlist.add("本地数据库");
			mpwlist.add("全局设置");
			mpwlist.add("批次管理");
			mpwlist.add("手动输入");
			mpwlist.add("释放按钮");
	 }
	 
	 /**
	  * 锁定所有Button，防止自动获取焦点
	  */
	 private void lockButton(){
		 btnBatchList.setEnabled(false);
		 btnBatchList.setText("锁定");
		 btnIsComplete.setEnabled(false);
		 btnIsComplete.setText("锁定");
		 btnOutBound.setEnabled(false);
		 btnOutBound.setText("锁定");
		 btnRevocation.setEnabled(false);
		 btnRevocation.setText("锁定");
	 }
	 
	 /**
	  * 解除所有Button的锁定状态
	  */
	 private void unlockButton(){
		 btnBatchList.setEnabled(true);
		 btnBatchList.setText("批次");
		 btnIsComplete.setEnabled(true);
		 btnIsComplete.setText("完成");
		 btnOutBound.setEnabled(true);
		 btnOutBound.setText("出库");
		 btnRevocation.setEnabled(true);
		 btnRevocation.setText("撤销");
	 }
	
}
