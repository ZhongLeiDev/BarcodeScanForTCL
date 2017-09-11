package com.zl.upc_style;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.media.AudioManager;
import android.media.SoundPool;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import com.zkc.Service.CaptureService;
import com.zkc.barcodescan.R;
import com.zkc.barcodescan.activity.WiFiStateBroadcastReceiver;
import com.zl.whaley_dbsetting.ConfigActivity;

public class Upc_MainActivity extends Activity{
	
	private static final String currentbatchpath = Environment.getExternalStorageDirectory()+File.separator+"ScanResultData"+File.separator+"Config"+File.separator+"upc_currentbatch.dat";
	private boolean isInitalized = false;//初始化标志位
	private boolean isSNok = false;//SN码扫描成功标志位
	//private boolean isUPCok = false;//UPC码扫描成功标志位
	private boolean isRevocation = false;//撤销标志位
	private boolean isManualInput = false;//手动输入标志位
	private ScanBroadcastReceiver scanBroadcastReceiver;
	private WiFiStateBroadcastReceiver wsb;//WiFi广播
	private String SNnumber = "UNKNOW";//SN码
	private String currentBatch = "UNKNOWBATCH";//当前批次
	private String currentUPC = "UNKNOWUPC";//当前UPC码
	private String staticBatch = "UNKNOW";//标准批次
	private String staticUPC = "UNKNOWUPC";//标准UPC码
	private int localSum = 0;//本机扫描总数
	private int currentSum = 0;//当前扫描总数
	private int SN_length = 0;//SN码长度
	private int UPC_length = 0;//UPC码长度
	private EditText edtSN,edtUPC;
	private TextView tvdetail,tvbatch,tvnumber,tvscantype,tvnetstate,tvlocalnumber;
	private ImageView imgview;
	private Upc_DBUtils upcDBUtil = new Upc_DBUtils();
	private SoundPool soundPool;//声音播放缓冲
	private Map<String,String> saveMap = new HashMap<String,String>();
	
	//------------------popupWindow代替menu-----------------------------
	private List<String> mpwlist = new ArrayList<String>();
	
	private Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			if(msg.what == 0x01){//重复扫描
				
				soundPool.play(7,1, 1, 0, 0, 1);
				imgview.setImageDrawable(getResources().getDrawable(R.drawable.ng));		
				tvdetail.setTextColor(Color.RED);
				
				String[ ] message = ((String)msg.obj).split(";");
				tvdetail.setText(message[0]);
				if(message.length>1){
				showRepeatedErrorDialog(message[1]);//显示重复扫描时间
				}else{
					showErrorDialog();
				}
				
			}else if(msg.what == 0x02){//查询失败
				
				soundPool.play(2,1, 1, 0, 0, 1);
				imgview.setImageDrawable(getResources().getDrawable(R.drawable.ng));
				
				tvdetail.setTextColor(Color.RED);
				String message = (String)msg.obj;
				tvdetail.setText(message);
				showErrorDialog();
			}else if(msg.what == 0x03){//查询成功
				
				soundPool.play(1,1, 1, 0, 0, 1);
				imgview.setImageDrawable(getResources().getDrawable(R.drawable.ok));
				if(!isInitalized){
					showBatchMessageDialog();
				}else{
					String message = (String)msg.obj;
					tvdetail.setTextColor(Color.GREEN);
					tvdetail.setText(message);
					if(!isManualInput){//---------------------非手动输入状态---------------------------------------
						isSNok = true;
					}else{//-------------------------------------手动输入状态----------------------------------------
					showUPCinputDialog();
					isManualInput = false;//将手动输入标志位设为TRUE，等待重新输入SN
				}
				}

			}else if(msg.what == 0x04){//保存成功
				
				soundPool.play(10,1, 1, 0, 0, 1);
				imgview.setImageDrawable(getResources().getDrawable(R.drawable.ok));
				
				tvdetail.setTextColor(Color.GREEN);
				String message = (String)msg.obj;
				tvdetail.setText(message);
				isSNok = false;
				
				currentSum ++;//当前扫描数+1
				tvnumber.setText(String.valueOf(currentSum));
				
				localSum++;
				tvlocalnumber.setText(String.valueOf(localSum));
				saveConfig();
				
			}else if(msg.what == 0x05){//保存失败
				
				soundPool.play(11,1, 1, 0, 0, 1);
				imgview.setImageDrawable(getResources().getDrawable(R.drawable.ng));
				
				isSNok = false;
				
				tvdetail.setTextColor(Color.RED);
				String message = (String)msg.obj;
				tvdetail.setText(message);
				showErrorDialog();
			}else if(msg.what == 0x06){//删除结果
				
				String message = (String)msg.obj;
				tvdetail.setText(message);
				if(message.equals("删除成功")){
					
					currentSum -- ;
					tvnumber.setText(String.valueOf(currentSum));
					
					tvdetail.setTextColor(Color.GREEN);
					soundPool.play(12,1, 1, 0, 0, 1);
					imgview.setImageDrawable(getResources().getDrawable(R.drawable.ok));	
					
					localSum--;
					tvlocalnumber.setText(String.valueOf(localSum));
					saveConfig();
					
				}else{
					tvdetail.setTextColor(Color.RED);
					soundPool.play(13,1, 1, 0, 0, 1);
					imgview.setImageDrawable(getResources().getDrawable(R.drawable.ng));	
				}
			}else if(msg.what == 0x07){//失败
				
				soundPool.play(4,1, 1, 0, 0, 1);
				imgview.setImageDrawable(getResources().getDrawable(R.drawable.ng));
				
				tvdetail.setTextColor(Color.RED);
				tvdetail.setText("UPC码不符合要求！");
				showErrorDialog();
			}else if(msg.what == 0x08){//当前条码对应的批次与标准批次不同
				
				soundPool.play(4,1, 1, 0, 0, 1);
				imgview.setImageDrawable(getResources().getDrawable(R.drawable.ng));
				
				tvdetail.setTextColor(Color.RED);
				tvdetail.setText("当前条码对应的批次与标准批次不同！");
				showErrorDialog();
			}else if(msg.what == 0x10){
				tvnetstate.setText("网络连接状态："+(String)msg.obj);
				tvnetstate.setTextColor(Color.RED);
			}else if(msg.what == 0x11){
				tvnetstate.setText("网络连接状态："+(String)msg.obj);
				tvnetstate.setTextColor(Color.YELLOW);
			}else if(msg.what == 0x12){//当前SN码长度与初始化SN码长度不同
				
				isSNok = false;//将SN扫描开关打开，重新扫描
				
				soundPool.play(4,1, 1, 0, 0, 1);
				imgview.setImageDrawable(getResources().getDrawable(R.drawable.ng));
				
				tvdetail.setTextColor(Color.RED);
				tvdetail.setText("SN码不符合要求！");
				showErrorDialog();
				
			}else if(msg.what == 0x13){
				//showLocalSumDialog();
			}
		}
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.upc_layout);
		
		Intent newIntent = new Intent(Upc_MainActivity.this, CaptureService.class);
		newIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		startService(newIntent);
		
		wsb = new WiFiStateBroadcastReceiver(mHandler);
		IntentFilter filter=new IntentFilter();
		filter.addAction(WifiManager.RSSI_CHANGED_ACTION);
		filter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
		filter.addAction(WifiManager.WIFI_STATE_CHANGED_ACTION);
		this.registerReceiver(wsb,filter); 
		
		scanBroadcastReceiver = new ScanBroadcastReceiver();
		IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction("com.zkc.scancode");
		this.registerReceiver(scanBroadcastReceiver, intentFilter);
		
		//---------------------------声音缓冲池初始化----------------
				soundPool= new SoundPool(3,AudioManager.STREAM_SYSTEM,5);
				soundPool.load(this, R.raw.scansucess,1);//扫描成功---------------------1
				soundPool.load(this, R.raw.scanfail, 1);//扫描失败------------------------2
				soundPool.load(this, R.raw.ok,1);//正确-----------------------------------3
				soundPool.load(this, R.raw.error, 1);//错误--------------------------------4
				soundPool.load(this, R.raw.sucess, 1);//成功------------------------------5
				soundPool.load(this, R.raw.fail, 1);//失败----------------------------------6
				soundPool.load(this, R.raw.repeat,1);//重复-------------------------------7
				soundPool.load(this, R.raw.scansucess,1);//扫描成功----------------------8
				soundPool.load(this, R.raw.scanfail, 1);//扫描失败-------------------------9
				soundPool.load(this, R.raw.savesucess, 1);//存储成功---------------------10
				soundPool.load(this, R.raw.savefail, 1);//存储失败-------------------------11
				soundPool.load(this, R.raw.delsucess, 1);//删除成功-----------------------12
				soundPool.load(this, R.raw.delfail, 1);//删除失败---------------------------13
		
		edtSN = (EditText)findViewById(R.id.sn);
		edtUPC = (EditText)findViewById(R.id.upc);
		edtSN.setFocusable(false);
		edtUPC.setFocusable(false);
		tvnetstate = (TextView)findViewById(R.id.netstate);
		tvdetail = (TextView)findViewById(R.id.detail);
		tvbatch = (TextView)findViewById(R.id.batch);
		tvnumber = (TextView)findViewById(R.id.number);
		tvscantype = (TextView)findViewById(R.id.scantype);
		imgview = (ImageView)findViewById(R.id.imageView1);
		tvlocalnumber = (TextView)findViewById(R.id.localnumber);
		
		initBatch();//初始化批次信息
		
	}
	/*-----------------------------------------------------------------
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// TODO Auto-generated method stub
		menu.add(0, 1, 1, R.string.itemoutbound);
		menu.add(0, 2, 2, R.string.itemrecovation);
		menu.add(0, 3, 3, R.string.itemmanulinput);
		return super.onCreateOptionsMenu(menu);

	}

	@Override
	public boolean onOptionsItemSelected(android.view.MenuItem item) {
		if (item.getItemId() == 1) {
			isRevocation = false;
			tvscantype.setTextColor(Color.WHITE);
			tvscantype.setText("当前为出库状态");
		} else if (item.getItemId() == 2) {
			isRevocation = true;
			tvscantype.setTextColor(Color.YELLOW);
			tvscantype.setText("当前为撤销状态");
		} else if (item.getItemId() == 3) {
			showSNinputDialog();
		} 
		return super.onOptionsItemSelected(item);
	}
	----------------------------------------------------------------------------*/
	
	@Override
	protected void onResume() {//活动重建
		System.out.println("onResume" + "open");
		Log.v("onResume", "open");
		super.onResume();
		initBatch();
	}
	
	/**
	 * 初始化批次数据
	 */
	private void initBatch(){
		File f = new File(currentbatchpath);
		if(f.exists()){
			saveMap = loadConfig();
			String number = saveMap.get("sum");
		if(number!=null){
				tvlocalnumber.setText(number);
				localSum = Integer.parseInt(number);
			}
		}else{
			saveConfig();//若config文件不存在，则将config文件初始化为 sum=0 ;
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
		super.onDestroy();
		saveConfig();
	}
	
	/*----------------------------------提示框-----------------------------------*/
	/**
	 * 初始化信息提示框
	 */
 private void showBatchMessageDialog(){
		
        final TextView tvshow = new TextView(Upc_MainActivity.this); 
        
        tvshow.setText("");
        //tvshow.setGravity(Gravity.CENTER);
        tvshow.setTextSize(18);
    	tvshow.append("	当前扫描批次为："+currentBatch+"\r\n");
    	tvshow.append("	当前扫描UPC为："+currentUPC);
		
		new AlertDialog.Builder(Upc_MainActivity.this).setIcon(android.R.drawable.btn_star)  
	    .setTitle("确认批次信息")
	    .setIcon(android.R.drawable.btn_star)
	    .setMessage("请确认本次扫描的批次信息：")
	    .setView(tvshow)
	    .setPositiveButton("确定",new DialogInterface.OnClickListener() {
	                @Override  
	                public void onClick(DialogInterface dialog,int which) {
	                    // TODO Auto-generated method stub  
	                	isInitalized = true;//若点击了"确认"键，则将初始化标志置为"true"
	                	tvbatch.setText(currentBatch);
	                	SN_length = SNnumber.length();
	                	UPC_length = currentUPC.length();
	                	staticUPC = currentUPC;//设置标准UPC
	                	if(!currentBatch.equals(staticBatch)){//批次改变时进行批次数量重置
	                	Log.i("SNReply","Load[currentBatch:"+currentBatch+";staticBatch:"+staticBatch+"];");
	                	staticBatch = currentBatch;
	                	//currentSum = 0;
	                	//tvnumber.setText(String.valueOf(currentSum));
	                	}
	                	//showLocalSumDialog();
	                }
	            })  
	    .setNegativeButton("取消",new DialogInterface.OnClickListener() {
	                @Override  
	                public void onClick(DialogInterface dialog,int which) {
	                    // TODO Auto-generated method stub  
	                    
	                }  
	            }).show();// show很关键
	}
 
 /**
  * 本机扫描数量初始化选择框
  */
 private void showLocalSumDialog(){
	 new AlertDialog.Builder(Upc_MainActivity.this).setIcon(android.R.drawable.btn_star)  
	    .setTitle("本机扫描数量")
	    .setIcon(android.R.drawable.btn_star)
	    .setMessage("是否将本机扫描数量清零？")
	    .setPositiveButton("确定",new DialogInterface.OnClickListener() {
	                @Override  
	                public void onClick(DialogInterface dialog,int which) { 
	                    // TODO Auto-generated method stub  
	                	localSum = 0;
	                	tvlocalnumber.setText(String.valueOf(localSum));
	                	saveConfig();
	                	
	                }
	            })  
	    .setNegativeButton("取消",new DialogInterface.OnClickListener() {
	                @Override  
	                public void onClick(DialogInterface dialog,int which) {
	                    // TODO Auto-generated method stub  
	                    
	                }  
	            }).show();// show很关键
 }
 
 /**
  * 错误提示框
  */
 private void showErrorDialog(){
		
		new AlertDialog.Builder(Upc_MainActivity.this).setIcon(android.R.drawable.btn_star)  
	    .setTitle("错误信息")
	    .setIcon(android.R.drawable.btn_star)
	    .setMessage("扫描条码"+SNnumber+"出错！")
	    .setPositiveButton("确定",new DialogInterface.OnClickListener() {
	                @Override  
	                public void onClick(DialogInterface dialog,int which) { 
	                    // TODO Auto-generated method stub  
	                	
	                }
	            })  
	    .setNegativeButton("取消",new DialogInterface.OnClickListener() {
	                @Override  
	                public void onClick(DialogInterface dialog,int which) {
	                    // TODO Auto-generated method stub  
	                    
	                }  
	            }).show();// show很关键
	}
 
 /**
  * 重复扫描提示框
  */
 private void showRepeatedErrorDialog(String msg){
		
		new AlertDialog.Builder(Upc_MainActivity.this).setIcon(android.R.drawable.btn_star)  
	    .setTitle("重复扫描")
	    .setIcon(android.R.drawable.btn_star)
	    .setMessage("Time："+msg)
	    .setPositiveButton("确定",new DialogInterface.OnClickListener() {
	                @Override  
	                public void onClick(DialogInterface dialog,int which) { 
	                    // TODO Auto-generated method stub  
	                	
	                }
	            })  
	    .setNegativeButton("取消",new DialogInterface.OnClickListener() {
	                @Override  
	                public void onClick(DialogInterface dialog,int which) {
	                    // TODO Auto-generated method stub  
	                    
	                }  
	            }).show();// show很关键
	}
 
 /**
  * 手动输入SN码对话框
  */
 private void showSNinputDialog(){
	 
	 	final EditText edtinput = new EditText(Upc_MainActivity.this); 
		
		new AlertDialog.Builder(Upc_MainActivity.this).setIcon(android.R.drawable.btn_star)
	    .setTitle("手动输入")
	    .setIcon(android.R.drawable.btn_star)
	    .setMessage("请手动输入SN码：")
	    .setView(edtinput)
	    .setPositiveButton("确定",new DialogInterface.OnClickListener() {
	                @Override  
	                public void onClick(DialogInterface dialog,int which) { 
	                    // TODO Auto-generated method stub  
	                	String res = edtinput.getEditableText().toString();
	                	if((res!=null)&&(!res.isEmpty())){
	                		SNnumber = res;
	                		edtSN.setText(res);
	                		if(!isRevocation){
	                			new Thread(queryRunnable).start();//查询操作
	                			isManualInput = true;//将手动输入标志位置为true
	                			}else{
	                				new Thread(deleteRunnable).start();//删除操作
	                			}
	                	}else{
	                		Toast.makeText(Upc_MainActivity.this, "输入SN码为空！", Toast.LENGTH_SHORT).show();
	                	}
	                	
	                }
	            })  
	    .setNegativeButton("取消",new DialogInterface.OnClickListener() {
	                @Override  
	                public void onClick(DialogInterface dialog,int which) {
	                    // TODO Auto-generated method stub  
	                    
	                }  
	            }).show();// show很关键
	}
 
 /**
  * 手动输入UPC码对话框
  */
 private void showUPCinputDialog(){
	 
	 	final EditText edtinput = new EditText(Upc_MainActivity.this); 
		
		new AlertDialog.Builder(Upc_MainActivity.this).setIcon(android.R.drawable.btn_star)
	    .setTitle("手动输入")
	    .setIcon(android.R.drawable.btn_star)
	    .setMessage("请手动输入UPC码：")
	    .setView(edtinput)
	    .setPositiveButton("确定",new DialogInterface.OnClickListener() {
	                @Override  
	                public void onClick(DialogInterface dialog,int which) { 
	                    // TODO Auto-generated method stub  
	                	String res = edtinput.getEditableText().toString();
	                	edtUPC.setText(res);
	                	if((res!=null)&&(!res.isEmpty())&&(res.equals(staticUPC))){
	                		currentUPC = res;
	                		new Thread(saveRunnable).start();//存储线程
	                	}else{
	                		Toast.makeText(Upc_MainActivity.this, "输入UPC码不符合要求！", Toast.LENGTH_SHORT).show();
	                	}
	                	
	                }
	            })  
	    .setNegativeButton("取消",new DialogInterface.OnClickListener() {
	                @Override  
	                public void onClick(DialogInterface dialog,int which) {
	                    // TODO Auto-generated method stub  
	                    
	                }  
	            }).show();// show很关键
	}
 
 /**
  * 确认撤销对话框
  */
 private void showResumeDialog(){
		
		new AlertDialog.Builder(Upc_MainActivity.this).setIcon(android.R.drawable.btn_star)
	    .setTitle("撤销确认")
	    .setIcon(android.R.drawable.btn_star)
	    .setMessage("撤销操作不可逆，请谨慎操作！！！")
	    .setNegativeButton("取消",new DialogInterface.OnClickListener() {
	                @Override  
	                public void onClick(DialogInterface dialog,int which) {
	                    // TODO Auto-generated method stub  
	                    
	                }  
	            })
	    .setPositiveButton("确定",new DialogInterface.OnClickListener() {
	                @Override  
	                public void onClick(DialogInterface dialog,int which) { 
	                    // TODO Auto-generated method stub  
	                	
	                	isRevocation = true;
						tvscantype.setTextColor(Color.RED);
						tvscantype.setText("当前为撤销状态");
	                	
	                }
	            }).show();// show很关键
	}
 
 /**
  * 退出确认框
  */
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

	/*----------------------------------子线程-----------------------------------*/
	/**
	 * 查询Runnable
	 */
	private Runnable queryRunnable = new Runnable(){

		@Override
		public void run() {
			// TODO Auto-generated method stub
				String rebuild = upcDBUtil.IsUPCStockOutScanBySN(SNnumber);
				Log.i("SNReply", rebuild);
				if (rebuild.indexOf("错误:")>-1) //包含错误
				{
				   if(rebuild.indexOf("重复")>-1)
				  { 
					    Message msg = Message.obtain(mHandler);
						msg.what = 0x01;//重复扫描
						msg.obj = rebuild;
						msg.sendToTarget();
				  }
				  else
				   {
				        
					    Message msg = Message.obtain(mHandler);
						msg.what = 0x02;//失败
						msg.obj = rebuild;
						msg.sendToTarget();
				    }
				}
				else
				{
				     String[] arr=rebuild.split("\\|");//java以竖线为分隔符时是"\\|"而不是"|"
				     if (arr.length==4)
				     {
				          currentBatch = arr[0];
				          currentUPC = arr[1];
				          currentSum = Integer.parseInt(arr[3]);//刷新当前批次数量
				          if((!isInitalized)||(isInitalized&&(currentBatch.equals(staticBatch)))){//未初始化或者已初始化但是当前批次与标准批次相同
				          Message msg = Message.obtain(mHandler);
						  msg.what = 0x03;//成功
						  msg.obj = rebuild;
						  msg.sendToTarget();
				          }else if(isInitalized&&(!currentBatch.equals(staticBatch))){//已初始化但是当前批次与标准批次不同
				        	  Message msg = Message.obtain(mHandler);
							  msg.what = 0x08;//当前条码对应的批次与标准批次不同
							  msg.obj = rebuild;
							  msg.sendToTarget();
				          }
				     }else{
				    	    Message msg = Message.obtain(mHandler);
							msg.what = 0x02;//失败
							msg.obj = rebuild;
							msg.sendToTarget();
				     }
				    
				}
				
			}
		
	};
	
	/**
	 * 存储Runnable
	 */
	private Runnable saveRunnable = new Runnable(){

		@Override
		public void run() {
			// TODO Auto-generated method stub
			if(SNnumber.length() == SN_length){
			String result = upcDBUtil.AddUPCScanOutRecord(SNnumber, currentUPC, "USER001", "HOST001", "IP001", "ADDRESS001", currentBatch);
			Log.i("SNReply", result);
			if(result.indexOf("成功")>-1){
				Message msg = Message.obtain(mHandler);
				msg.what = 0x04;//保存成功
				msg.obj = result;
				msg.sendToTarget();
			}else{
				Message msg = Message.obtain(mHandler);
				msg.what = 0x05;//保存失败
				msg.obj = result;
				msg.sendToTarget();
			}
		}else{
			Message msg = Message.obtain(mHandler);
			msg.what = 0x12;//当前SN码长度与初始化SN码长度不同,存储失败
			msg.sendToTarget();
		}
		}
		
	};
	
	/**
	 * 撤销Runnable
	 */
	private Runnable deleteRunnable = new Runnable(){

		@Override
		public void run() {
			// TODO Auto-generated method stub
			String result = upcDBUtil.DelUPCScanOutRecord(SNnumber, "USER001");
			Log.i("SNReply", result);
			Message msg = Message.obtain(mHandler);
			msg.what = 0x06;//删除结果
			msg.obj = result;
			msg.sendToTarget();
		}
		
	};
	
	/*---------------------------------------------广播类-----------------------------------------------*/
	class ScanBroadcastReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
	    // TODO Auto-generated method stub
		String code = intent.getExtras().getString("code");
		if(!isSNok){
			SNnumber = code;
			edtSN.setText(SNnumber);
			//isSNok = true;
			if(!isRevocation){
			new Thread(queryRunnable).start();
			}else{
				new Thread(deleteRunnable).start();
				isSNok = false;
			}
		}else if(isSNok){
			Log.i("SNReply", "currentUPC:"+currentUPC+";staticUPC:"+staticUPC);
			currentUPC = code;
			edtUPC.setText(currentUPC);
			if(currentUPC.length() == UPC_length&&currentUPC.equals(staticUPC)){//UPC长度符合要求且与标准UPC相同
			new Thread(saveRunnable).start();
			//isUPCok = true;
			}else{
				Message msg = Message.obtain(mHandler);
				msg.what = 0x07;
				msg.sendToTarget();
			}
		}
		
		}
	}
	
	/*----------------------------------------存储 Properties 函数--------------------------------------*/
	/**
	 * 加载本机已扫描的数量（sum）
	 * @return
	 */
	private Map<String,String> loadConfig(){
		Map<String,String> result = new HashMap<String,String>();
		Properties properties = ConfigActivity.loadConfig(Upc_MainActivity.this, currentbatchpath);
		result.put("sum", (String)properties.get("sum"));
		return result;
	}
	
	/**
	 * 存储本机已扫描的数量（sum）
	 */
	private void saveConfig(){
		Properties properties = new Properties();
		properties.put("sum", String.valueOf(localSum));
		Log.i("SNReply", "currentBatch:"+currentBatch+";currentSum:"+currentSum);
		ConfigActivity.saveConfig(Upc_MainActivity.this, currentbatchpath, properties);
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
						 	isRevocation = false;
							tvscantype.setTextColor(Color.WHITE);
							tvscantype.setText("当前为出库状态");
						} else if(position == 1) {
							popupWindow.dismiss();//pop消失
							//isRevocation = true;
							tvscantype.setTextColor(Color.YELLOW);
							//tvscantype.setText("当前为撤销状态");
							tvscantype.setText("清零前数量为"+localSum);
							
							showLocalSumDialog();//清零选择对话框
							
						} else if(position == 2) {
							popupWindow.dismiss();//pop消失
							showSNinputDialog();
						} else if(position == 3) {
							popupWindow.dismiss();//pop消失
							showResumeDialog();
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
		 	mpwlist.add("设为出库状态");
		 	mpwlist.add("本机计数清零");
		 	mpwlist.add("手动输入条码");
		 	mpwlist.add("设为撤销状态");
	 }

}
