package com.zl.cantv_style;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;

public class ExcelRead{
	

	/**

	  * 读取excel标准函数
	  * @param path Excel文档路径
	  * @return
	  */

	public static List<String> getDataFromExcel(String path){

	  ArrayList<String> list=new ArrayList<String>();

	  try {

	   Excel excel = new Excel(path,null,null);

	   Workbook workbook = excel.getWorkbook();

	   Sheet sheet = workbook.getSheet(0);

	   int a = excel.getRows(sheet);      //最大行数

	   int m=excel.getColumns(sheet);  //最大列数

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

	  * Cantv扫描所需要的读取excel的函数
	  * @param path Excel文档路径
	  * @return
	  */

	public static List<String> getDataFromExcelForCantv(String path){

	  ArrayList<String> list=new ArrayList<String>();

	  try {

	   Excel excel = new Excel(path,null,null);

	   Workbook workbook = excel.getWorkbook();

	   for(int num=0;num<workbook.getSheets().length;num++){//获取所有工作表
	   
	   Sheet sheet = workbook.getSheet(num);

	   int a = excel.getRows(sheet);      //最大行数

	   //int m=excel.getColumns(sheet);  //最大列数
	   int m = 1;//最大列数设为1

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
		List<String> a = new ArrayList<String>();//数组初始化
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
	 * Java文件操作 获取文件扩展名 
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
	 * Java文件操作 获取不带扩展名的文件名 
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
