package com.zl.mainactivity;

import java.io.File;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.zkc.barcodescan.R;
import com.zkc.barcodescan.R.id;
import com.zl.cantv_style.Cantv_MainActivity;
import com.zl.letv_soap.Letv_SoapMainActivity;
import com.zl.mitv_soap.Mitv_SoapMainActivity;
import com.zl.offline_style.Offline_MainActivity;
import com.zl.upc_style.Upc_MainActivity;
import com.zl.whaley_soap.SoapMainActivity;

/*Created by ZhongLei 
 * ->update from v1.0 to v2.0 at 2016/12/29
 * add "cantv" scan methods to progress
 * several bugs to be repaired: 
 * (1)How to efficiently insert large amounts of data into a database
 * (2)ignore several broadcast effects from the press operation
 * ->update from v2.0 to v2.1 at 2017/2/8
 * add "upc" scan methods to progress
 * */
public class MenuSelectActivity extends Activity{
	private TextView whaley;
	private TextView letv;
	private TextView offline;
	private TextView cantv;
	private TextView upc;
	private TextView mitv;
	
	//双击返回键退出
	private long firstTime=0;

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if(keyCode==KeyEvent.KEYCODE_BACK && event.getAction()==KeyEvent.ACTION_DOWN){
            if (System.currentTimeMillis()-firstTime>2000){
                Toast.makeText(MenuSelectActivity.this,"再按一次退出程序",Toast.LENGTH_SHORT).show();
                firstTime=System.currentTimeMillis();
            }else{
                finish();
                System.exit(0);//完全退出系统，避免影响其它程序
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_menu);
		
		whaley = (TextView)findViewById(R.id.whaley);
		letv = (TextView)findViewById(R.id.letv);
		offline = (TextView)findViewById(R.id.offline);
		cantv = (TextView)findViewById(R.id.cantv);
		upc = (TextView)findViewById(R.id.bby);
		mitv = (TextView)findViewById(R.id.mitv);
		
		whaley.setOnClickListener(onClick);
		letv.setOnClickListener(onClick);
		offline.setOnClickListener(onClick);
		cantv.setOnClickListener(onClick);
		upc.setOnClickListener(onClick);
		mitv.setOnClickListener(onClick);
		
		initPath();//路径初始化
		
	}
	
	private OnClickListener onClick = new OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			if(v == whaley){
				startActivity(new Intent(MenuSelectActivity.this,SoapMainActivity.class));
			}else if(v == letv){
				startActivity(new Intent(MenuSelectActivity.this,Letv_SoapMainActivity.class));
			}else if(v == offline){
				startActivity(new Intent(MenuSelectActivity.this,Offline_MainActivity.class));
			}else if(v == cantv){
				startActivity(new Intent(MenuSelectActivity.this,Cantv_MainActivity.class));
			}else if(v == upc){
				startActivity(new Intent(MenuSelectActivity.this,Upc_MainActivity.class));
			}else if(v == mitv){
				startActivity(new Intent(MenuSelectActivity.this,Mitv_SoapMainActivity.class));
			}
			
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
		File file4 = new File(Environment.getExternalStorageDirectory() + File.separator+"ScanResultData"+File.separator+"Cantv");
		if(!file4.exists()){
		makeDir(file4);
		}
	}
	
	private void makeDir(File dir){
		if (!dir.getParentFile().exists()) {
			makeDir(dir.getParentFile());
		}
		dir.mkdir();
	}

}
