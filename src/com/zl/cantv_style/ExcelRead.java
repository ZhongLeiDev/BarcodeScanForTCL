package com.zl.cantv_style;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;

public class ExcelRead{
	

	/**

	  * ��ȡexcel��׼����
	  * @param path Excel�ĵ�·��
	  * @return
	  */

	public static List<String> getDataFromExcel(String path){

	  ArrayList<String> list=new ArrayList<String>();

	  try {

	   Excel excel = new Excel(path,null,null);

	   Workbook workbook = excel.getWorkbook();

	   Sheet sheet = workbook.getSheet(0);

	   int a = excel.getRows(sheet);      //�������

	   int m=excel.getColumns(sheet);  //�������

	   Cell[][] c = excel.getCells(sheet,0,a,0,m);

	  String f1 = null;

      for(int i =0 ; i < c.length;i++){

	    Cell[] obj = c[i];

	    for(int j =0 ;j< obj.length; j++ ){

	      f1=obj[j].getContents().toString();

	     list.add(f1);

	    }

	   }

	  } catch (Exception e) {

	   e.printStackTrace();

	  }

	  return list;

	}
	
	/**

	  * Cantvɨ������Ҫ�Ķ�ȡexcel�ĺ���
	  * @param path Excel�ĵ�·��
	  * @return
	  */

	public static List<String> getDataFromExcelForCantv(String path){

	  ArrayList<String> list=new ArrayList<String>();

	  try {

	   Excel excel = new Excel(path,null,null);

	   Workbook workbook = excel.getWorkbook();

	   for(int num=0;num<workbook.getSheets().length;num++){//��ȡ���й�����
	   
	   Sheet sheet = workbook.getSheet(num);

	   int a = excel.getRows(sheet);      //�������

	   //int m=excel.getColumns(sheet);  //�������
	   int m = 1;//���������Ϊ1

	   Cell[][] c = excel.getCells(sheet,0,a,0,m);

	  String f1 = null;

     for(int i =0 ; i < c.length;i++){

	    Cell[] obj = c[i];

	    for(int j =0 ;j< obj.length; j++ ){

	      f1=obj[j].getContents().toString();

	     list.add(f1);

	    }

	   }
	   }

	  } catch (Exception e) {

	   e.printStackTrace();

	  }

	  return list;

	}
	
	public static List<String> getFilenames(String dir){
		List<String> a = new ArrayList<String>();//�����ʼ��
		File file = new File(dir);
	      if(file.isDirectory()){
	           File [] fileArray = file.listFiles();
	           if(null != fileArray && 0 != fileArray.length){
	                for(int i = 0; i < fileArray.length; i++){
	                	a.add(fileArray[i].getName());
	                }
	           }
	}
	      return a;
	}
	
	/* 
	 * Java�ļ����� ��ȡ�ļ���չ�� 
	 *  
	 */   
	    public static String getExtensionName(String filename) {    
	        if ((filename != null) && (filename.length() > 0)) {    
	            int dot = filename.lastIndexOf('.');    
	            if ((dot >-1) && (dot < (filename.length() - 1))) {    
	                return filename.substring(dot + 1);    
	            }    
	        }    
	        return filename;    
	    }    
	/* 
	 * Java�ļ����� ��ȡ������չ�����ļ��� 
	 */   
	    public static String getFileNameNoEx(String filename) {    
	        if ((filename != null) && (filename.length() > 0)) {    
	            int dot = filename.lastIndexOf('.');    
	            if ((dot >-1) && (dot < (filename.length()))) {    
	                return filename.substring(0, dot);    
	            }    
	        }    
	        return filename;    
	    }    
	
}
