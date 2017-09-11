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
	private boolean isInitalized = false;//��ʼ����־λ
	private boolean isSNok = false;//SN��ɨ��ɹ���־λ
	//private boolean isUPCok = false;//UPC��ɨ��ɹ���־λ
	private boolean isRevocation = false;//������־λ
	private boolean isManualInput = false;//�ֶ������־λ
	private ScanBroadcastReceiver scanBroadcastReceiver;
	private WiFiStateBroadcastReceiver wsb;//WiFi�㲥
	private String SNnumber = "UNKNOW";//SN��
	private String currentBatch = "UNKNOWBATCH";//��ǰ����
	private String currentUPC = "UNKNOWUPC";//��ǰUPC��
	private String staticBatch = "UNKNOW";//��׼����
	private String staticUPC = "UNKNOWUPC";//��׼UPC��
	private int localSum = 0;//����ɨ������
	private int currentSum = 0;//��ǰɨ������
	private int SN_length = 0;//SN�볤��
	private int UPC_length = 0;//UPC�볤��
	private EditText edtSN,edtUPC;
	private TextView tvdetail,tvbatch,tvnumber,tvscantype,tvnetstate,tvlocalnumber;
	private ImageView imgview;
	private Upc_DBUtils upcDBUtil = new Upc_DBUtils();
	private SoundPool soundPool;//�������Ż���
	private Map<String,String> saveMap = new HashMap<String,String>();
	
	//------------------popupWindow����menu-----------------------------
	private List<String> mpwlist = new ArrayList<String>();
	
	private Handler mHandler = new Handler(){
		@Override
		public void handleMessage(Message msg){
			if(msg.what == 0x01){//�ظ�ɨ��
				
				soundPool.play(7,1, 1, 0, 0, 1);
				imgview.setImageDrawable(getResources().getDrawable(R.drawable.ng));		
				tvdetail.setTextColor(Color.RED);
				
				String[ ] message = ((String)msg.obj).split(";");
				tvdetail.setText(message[0]);
				if(message.length>1){
				showRepeatedErrorDialog(message[1]);//��ʾ�ظ�ɨ��ʱ��
				}else{
					showErrorDialog();
				}
				
			}else if(msg.what == 0x02){//��ѯʧ��
				
				soundPool.play(2,1, 1, 0, 0, 1);
				imgview.setImageDrawable(getResources().getDrawable(R.drawable.ng));
				
				tvdetail.setTextColor(Color.RED);
				String message = (String)msg.obj;
				tvdetail.setText(message);
				showErrorDialog();
			}else if(msg.what == 0x03){//��ѯ�ɹ�
				
				soundPool.play(1,1, 1, 0, 0, 1);
				imgview.setImageDrawable(getResources().getDrawable(R.drawable.ok));
				if(!isInitalized){
					showBatchMessageDialog();
				}else{
					String message = (String)msg.obj;
					tvdetail.setTextColor(Color.GREEN);
					tvdetail.setText(message);
					if(!isManualInput){//---------------------���ֶ�����״̬---------------------------------------
						isSNok = true;
					}else{//-------------------------------------�ֶ�����״̬----------------------------------------
					showUPCinputDialog();
					isManualInput = false;//���ֶ������־λ��ΪTRUE���ȴ���������SN
				}
				}

			}else if(msg.what == 0x04){//����ɹ�
				
				soundPool.play(10,1, 1, 0, 0, 1);
				imgview.setImageDrawable(getResources().getDrawable(R.drawable.ok));
				
				tvdetail.setTextColor(Color.GREEN);
				String message = (String)msg.obj;
				tvdetail.setText(message);
				isSNok = false;
				
				currentSum ++;//��ǰɨ����+1
				tvnumber.setText(String.valueOf(currentSum));
				
				localSum++;
				tvlocalnumber.setText(String.valueOf(localSum));
				saveConfig();
				
			}else if(msg.what == 0x05){//����ʧ��
				
				soundPool.play(11,1, 1, 0, 0, 1);
				imgview.setImageDrawable(getResources().getDrawable(R.drawable.ng));
				
				isSNok = false;
				
				tvdetail.setTextColor(Color.RED);
				String message = (String)msg.obj;
				tvdetail.setText(message);
				showErrorDialog();
			}else if(msg.what == 0x06){//ɾ�����
				
				String message = (String)msg.obj;
				tvdetail.setText(message);
				if(message.equals("ɾ���ɹ�")){
					
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
			}else if(msg.what == 0x07){//ʧ��
				
				soundPool.play(4,1, 1, 0, 0, 1);
				imgview.setImageDrawable(getResources().getDrawable(R.drawable.ng));
				
				tvdetail.setTextColor(Color.RED);
				tvdetail.setText("UPC�벻����Ҫ��");
				showErrorDialog();
			}else if(msg.what == 0x08){//��ǰ�����Ӧ���������׼���β�ͬ
				
				soundPool.play(4,1, 1, 0, 0, 1);
				imgview.setImageDrawable(getResources().getDrawable(R.drawable.ng));
				
				tvdetail.setTextColor(Color.RED);
				tvdetail.setText("��ǰ�����Ӧ���������׼���β�ͬ��");
				showErrorDialog();
			}else if(msg.what == 0x10){
				tvnetstate.setText("��������״̬��"+(String)msg.obj);
				tvnetstate.setTextColor(Color.RED);
			}else if(msg.what == 0x11){
				tvnetstate.setText("��������״̬��"+(String)msg.obj);
				tvnetstate.setTextColor(Color.YELLOW);
			}else if(msg.what == 0x12){//��ǰSN�볤�����ʼ��SN�볤�Ȳ�ͬ
				
				isSNok = false;//��SNɨ�迪�ش򿪣�����ɨ��
				
				soundPool.play(4,1, 1, 0, 0, 1);
				imgview.setImageDrawable(getResources().getDrawable(R.drawable.ng));
				
				tvdetail.setTextColor(Color.RED);
				tvdetail.setText("SN�벻����Ҫ��");
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
		
		//---------------------------��������س�ʼ��----------------
				soundPool= new SoundPool(3,AudioManager.STREAM_SYSTEM,5);
				soundPool.load(this, R.raw.scansucess,1);//ɨ��ɹ�---------------------1
				soundPool.load(this, R.raw.scanfail, 1);//ɨ��ʧ��------------------------2
				soundPool.load(this, R.raw.ok,1);//��ȷ-----------------------------------3
				soundPool.load(this, R.raw.error, 1);//����--------------------------------4
				soundPool.load(this, R.raw.sucess, 1);//�ɹ�------------------------------5
				soundPool.load(this, R.raw.fail, 1);//ʧ��----------------------------------6
				soundPool.load(this, R.raw.repeat,1);//�ظ�-------------------------------7
				soundPool.load(this, R.raw.scansucess,1);//ɨ��ɹ�----------------------8
				soundPool.load(this, R.raw.scanfail, 1);//ɨ��ʧ��-------------------------9
				soundPool.load(this, R.raw.savesucess, 1);//�洢�ɹ�---------------------10
				soundPool.load(this, R.raw.savefail, 1);//�洢ʧ��-------------------------11
				soundPool.load(this, R.raw.delsucess, 1);//ɾ���ɹ�-----------------------12
				soundPool.load(this, R.raw.delfail, 1);//ɾ��ʧ��---------------------------13
		
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
		
		initBatch();//��ʼ��������Ϣ
		
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
			tvscantype.setText("��ǰΪ����״̬");
		} else if (item.getItemId() == 2) {
			isRevocation = true;
			tvscantype.setTextColor(Color.YELLOW);
			tvscantype.setText("��ǰΪ����״̬");
		} else if (item.getItemId() == 3) {
			showSNinputDialog();
		} 
		return super.onOptionsItemSelected(item);
	}
	----------------------------------------------------------------------------*/
	
	@Override
	protected void onResume() {//��ؽ�
		System.out.println("onResume" + "open");
		Log.v("onResume", "open");
		super.onResume();
		initBatch();
	}
	
	/**
	 * ��ʼ����������
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
			saveConfig();//��config�ļ������ڣ���config�ļ���ʼ��Ϊ sum=0 ;
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
	
	/*----------------------------------��ʾ��-----------------------------------*/
	/**
	 * ��ʼ����Ϣ��ʾ��
	 */
 private void showBatchMessageDialog(){
		
        final TextView tvshow = new TextView(Upc_MainActivity.this); 
        
        tvshow.setText("");
        //tvshow.setGravity(Gravity.CENTER);
        tvshow.setTextSize(18);
    	tvshow.append("	��ǰɨ������Ϊ��"+currentBatch+"\r\n");
    	tvshow.append("	��ǰɨ��UPCΪ��"+currentUPC);
		
		new AlertDialog.Builder(Upc_MainActivity.this).setIcon(android.R.drawable.btn_star)  
	    .setTitle("ȷ��������Ϣ")
	    .setIcon(android.R.drawable.btn_star)
	    .setMessage("��ȷ�ϱ���ɨ���������Ϣ��")
	    .setView(tvshow)
	    .setPositiveButton("ȷ��",new DialogInterface.OnClickListener() {
	                @Override  
	                public void onClick(DialogInterface dialog,int which) {
	                    // TODO Auto-generated method stub  
	                	isInitalized = true;//�������"ȷ��"�����򽫳�ʼ����־��Ϊ"true"
	                	tvbatch.setText(currentBatch);
	                	SN_length = SNnumber.length();
	                	UPC_length = currentUPC.length();
	                	staticUPC = currentUPC;//���ñ�׼UPC
	                	if(!currentBatch.equals(staticBatch)){//���θı�ʱ����������������
	                	Log.i("SNReply","Load[currentBatch:"+currentBatch+";staticBatch:"+staticBatch+"];");
	                	staticBatch = currentBatch;
	                	//currentSum = 0;
	                	//tvnumber.setText(String.valueOf(currentSum));
	                	}
	                	//showLocalSumDialog();
	                }
	            })  
	    .setNegativeButton("ȡ��",new DialogInterface.OnClickListener() {
	                @Override  
	                public void onClick(DialogInterface dialog,int which) {
	                    // TODO Auto-generated method stub  
	                    
	                }  
	            }).show();// show�ܹؼ�
	}
 
 /**
  * ����ɨ��������ʼ��ѡ���
  */
 private void showLocalSumDialog(){
	 new AlertDialog.Builder(Upc_MainActivity.this).setIcon(android.R.drawable.btn_star)  
	    .setTitle("����ɨ������")
	    .setIcon(android.R.drawable.btn_star)
	    .setMessage("�Ƿ񽫱���ɨ���������㣿")
	    .setPositiveButton("ȷ��",new DialogInterface.OnClickListener() {
	                @Override  
	                public void onClick(DialogInterface dialog,int which) { 
	                    // TODO Auto-generated method stub  
	                	localSum = 0;
	                	tvlocalnumber.setText(String.valueOf(localSum));
	                	saveConfig();
	                	
	                }
	            })  
	    .setNegativeButton("ȡ��",new DialogInterface.OnClickListener() {
	                @Override  
	                public void onClick(DialogInterface dialog,int which) {
	                    // TODO Auto-generated method stub  
	                    
	                }  
	            }).show();// show�ܹؼ�
 }
 
 /**
  * ������ʾ��
  */
 private void showErrorDialog(){
		
		new AlertDialog.Builder(Upc_MainActivity.this).setIcon(android.R.drawable.btn_star)  
	    .setTitle("������Ϣ")
	    .setIcon(android.R.drawable.btn_star)
	    .setMessage("ɨ������"+SNnumber+"����")
	    .setPositiveButton("ȷ��",new DialogInterface.OnClickListener() {
	                @Override  
	                public void onClick(DialogInterface dialog,int which) { 
	                    // TODO Auto-generated method stub  
	                	
	                }
	            })  
	    .setNegativeButton("ȡ��",new DialogInterface.OnClickListener() {
	                @Override  
	                public void onClick(DialogInterface dialog,int which) {
	                    // TODO Auto-generated method stub  
	                    
	                }  
	            }).show();// show�ܹؼ�
	}
 
 /**
  * �ظ�ɨ����ʾ��
  */
 private void showRepeatedErrorDialog(String msg){
		
		new AlertDialog.Builder(Upc_MainActivity.this).setIcon(android.R.drawable.btn_star)  
	    .setTitle("�ظ�ɨ��")
	    .setIcon(android.R.drawable.btn_star)
	    .setMessage("Time��"+msg)
	    .setPositiveButton("ȷ��",new DialogInterface.OnClickListener() {
	                @Override  
	                public void onClick(DialogInterface dialog,int which) { 
	                    // TODO Auto-generated method stub  
	                	
	                }
	            })  
	    .setNegativeButton("ȡ��",new DialogInterface.OnClickListener() {
	                @Override  
	                public void onClick(DialogInterface dialog,int which) {
	                    // TODO Auto-generated method stub  
	                    
	                }  
	            }).show();// show�ܹؼ�
	}
 
 /**
  * �ֶ�����SN��Ի���
  */
 private void showSNinputDialog(){
	 
	 	final EditText edtinput = new EditText(Upc_MainActivity.this); 
		
		new AlertDialog.Builder(Upc_MainActivity.this).setIcon(android.R.drawable.btn_star)
	    .setTitle("�ֶ�����")
	    .setIcon(android.R.drawable.btn_star)
	    .setMessage("���ֶ�����SN�룺")
	    .setView(edtinput)
	    .setPositiveButton("ȷ��",new DialogInterface.OnClickListener() {
	                @Override  
	                public void onClick(DialogInterface dialog,int which) { 
	                    // TODO Auto-generated method stub  
	                	String res = edtinput.getEditableText().toString();
	                	if((res!=null)&&(!res.isEmpty())){
	                		SNnumber = res;
	                		edtSN.setText(res);
	                		if(!isRevocation){
	                			new Thread(queryRunnable).start();//��ѯ����
	                			isManualInput = true;//���ֶ������־λ��Ϊtrue
	                			}else{
	                				new Thread(deleteRunnable).start();//ɾ������
	                			}
	                	}else{
	                		Toast.makeText(Upc_MainActivity.this, "����SN��Ϊ�գ�", Toast.LENGTH_SHORT).show();
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
 
 /**
  * �ֶ�����UPC��Ի���
  */
 private void showUPCinputDialog(){
	 
	 	final EditText edtinput = new EditText(Upc_MainActivity.this); 
		
		new AlertDialog.Builder(Upc_MainActivity.this).setIcon(android.R.drawable.btn_star)
	    .setTitle("�ֶ�����")
	    .setIcon(android.R.drawable.btn_star)
	    .setMessage("���ֶ�����UPC�룺")
	    .setView(edtinput)
	    .setPositiveButton("ȷ��",new DialogInterface.OnClickListener() {
	                @Override  
	                public void onClick(DialogInterface dialog,int which) { 
	                    // TODO Auto-generated method stub  
	                	String res = edtinput.getEditableText().toString();
	                	edtUPC.setText(res);
	                	if((res!=null)&&(!res.isEmpty())&&(res.equals(staticUPC))){
	                		currentUPC = res;
	                		new Thread(saveRunnable).start();//�洢�߳�
	                	}else{
	                		Toast.makeText(Upc_MainActivity.this, "����UPC�벻����Ҫ��", Toast.LENGTH_SHORT).show();
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
 
 /**
  * ȷ�ϳ����Ի���
  */
 private void showResumeDialog(){
		
		new AlertDialog.Builder(Upc_MainActivity.this).setIcon(android.R.drawable.btn_star)
	    .setTitle("����ȷ��")
	    .setIcon(android.R.drawable.btn_star)
	    .setMessage("�������������棬���������������")
	    .setNegativeButton("ȡ��",new DialogInterface.OnClickListener() {
	                @Override  
	                public void onClick(DialogInterface dialog,int which) {
	                    // TODO Auto-generated method stub  
	                    
	                }  
	            })
	    .setPositiveButton("ȷ��",new DialogInterface.OnClickListener() {
	                @Override  
	                public void onClick(DialogInterface dialog,int which) { 
	                    // TODO Auto-generated method stub  
	                	
	                	isRevocation = true;
						tvscantype.setTextColor(Color.RED);
						tvscantype.setText("��ǰΪ����״̬");
	                	
	                }
	            }).show();// show�ܹؼ�
	}
 
 /**
  * �˳�ȷ�Ͽ�
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

	/*----------------------------------���߳�-----------------------------------*/
	/**
	 * ��ѯRunnable
	 */
	private Runnable queryRunnable = new Runnable(){

		@Override
		public void run() {
			// TODO Auto-generated method stub
				String rebuild = upcDBUtil.IsUPCStockOutScanBySN(SNnumber);
				Log.i("SNReply", rebuild);
				if (rebuild.indexOf("����:")>-1) //��������
				{
				   if(rebuild.indexOf("�ظ�")>-1)
				  { 
					    Message msg = Message.obtain(mHandler);
						msg.what = 0x01;//�ظ�ɨ��
						msg.obj = rebuild;
						msg.sendToTarget();
				  }
				  else
				   {
				        
					    Message msg = Message.obtain(mHandler);
						msg.what = 0x02;//ʧ��
						msg.obj = rebuild;
						msg.sendToTarget();
				    }
				}
				else
				{
				     String[] arr=rebuild.split("\\|");//java������Ϊ�ָ���ʱ��"\\|"������"|"
				     if (arr.length==4)
				     {
				          currentBatch = arr[0];
				          currentUPC = arr[1];
				          currentSum = Integer.parseInt(arr[3]);//ˢ�µ�ǰ��������
				          if((!isInitalized)||(isInitalized&&(currentBatch.equals(staticBatch)))){//δ��ʼ�������ѳ�ʼ�����ǵ�ǰ�������׼������ͬ
				          Message msg = Message.obtain(mHandler);
						  msg.what = 0x03;//�ɹ�
						  msg.obj = rebuild;
						  msg.sendToTarget();
				          }else if(isInitalized&&(!currentBatch.equals(staticBatch))){//�ѳ�ʼ�����ǵ�ǰ�������׼���β�ͬ
				        	  Message msg = Message.obtain(mHandler);
							  msg.what = 0x08;//��ǰ�����Ӧ���������׼���β�ͬ
							  msg.obj = rebuild;
							  msg.sendToTarget();
				          }
				     }else{
				    	    Message msg = Message.obtain(mHandler);
							msg.what = 0x02;//ʧ��
							msg.obj = rebuild;
							msg.sendToTarget();
				     }
				    
				}
				
			}
		
	};
	
	/**
	 * �洢Runnable
	 */
	private Runnable saveRunnable = new Runnable(){

		@Override
		public void run() {
			// TODO Auto-generated method stub
			if(SNnumber.length() == SN_length){
			String result = upcDBUtil.AddUPCScanOutRecord(SNnumber, currentUPC, "USER001", "HOST001", "IP001", "ADDRESS001", currentBatch);
			Log.i("SNReply", result);
			if(result.indexOf("�ɹ�")>-1){
				Message msg = Message.obtain(mHandler);
				msg.what = 0x04;//����ɹ�
				msg.obj = result;
				msg.sendToTarget();
			}else{
				Message msg = Message.obtain(mHandler);
				msg.what = 0x05;//����ʧ��
				msg.obj = result;
				msg.sendToTarget();
			}
		}else{
			Message msg = Message.obtain(mHandler);
			msg.what = 0x12;//��ǰSN�볤�����ʼ��SN�볤�Ȳ�ͬ,�洢ʧ��
			msg.sendToTarget();
		}
		}
		
	};
	
	/**
	 * ����Runnable
	 */
	private Runnable deleteRunnable = new Runnable(){

		@Override
		public void run() {
			// TODO Auto-generated method stub
			String result = upcDBUtil.DelUPCScanOutRecord(SNnumber, "USER001");
			Log.i("SNReply", result);
			Message msg = Message.obtain(mHandler);
			msg.what = 0x06;//ɾ�����
			msg.obj = result;
			msg.sendToTarget();
		}
		
	};
	
	/*---------------------------------------------�㲥��-----------------------------------------------*/
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
			if(currentUPC.length() == UPC_length&&currentUPC.equals(staticUPC)){//UPC���ȷ���Ҫ�������׼UPC��ͬ
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
	
	/*----------------------------------------�洢 Properties ����--------------------------------------*/
	/**
	 * ���ر�����ɨ���������sum��
	 * @return
	 */
	private Map<String,String> loadConfig(){
		Map<String,String> result = new HashMap<String,String>();
		Properties properties = ConfigActivity.loadConfig(Upc_MainActivity.this, currentbatchpath);
		result.put("sum", (String)properties.get("sum"));
		return result;
	}
	
	/**
	 * �洢������ɨ���������sum��
	 */
	private void saveConfig(){
		Properties properties = new Properties();
		properties.put("sum", String.valueOf(localSum));
		Log.i("SNReply", "currentBatch:"+currentBatch+";currentSum:"+currentSum);
		ConfigActivity.saveConfig(Upc_MainActivity.this, currentbatchpath, properties);
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
						 	isRevocation = false;
							tvscantype.setTextColor(Color.WHITE);
							tvscantype.setText("��ǰΪ����״̬");
						} else if(position == 1) {
							popupWindow.dismiss();//pop��ʧ
							//isRevocation = true;
							tvscantype.setTextColor(Color.YELLOW);
							//tvscantype.setText("��ǰΪ����״̬");
							tvscantype.setText("����ǰ����Ϊ"+localSum);
							
							showLocalSumDialog();//����ѡ��Ի���
							
						} else if(position == 2) {
							popupWindow.dismiss();//pop��ʧ
							showSNinputDialog();
						} else if(position == 3) {
							popupWindow.dismiss();//pop��ʧ
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
		 	mpwlist.add("��Ϊ����״̬");
		 	mpwlist.add("������������");
		 	mpwlist.add("�ֶ���������");
		 	mpwlist.add("��Ϊ����״̬");
	 }

}
