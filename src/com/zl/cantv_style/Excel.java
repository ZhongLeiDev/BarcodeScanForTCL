package com.zl.cantv_style;

import java.io.IOException;
import java.util.UUID;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;

/**
 * Excel������
 * @author Administrator
 *
 */
public class Excel{

	 //�ļ�·��

	 private String path;

	 private String tableName ;

	 private String[] tableCols;

	 //����������

	 private Workbook workbook;

	 public Excel(String path,String tableName,String[] tableCols) throws BiffException,IOException{

	   this.tableName = tableName;

	   this.tableCols = tableCols;

	   this.setPath(path);

	   this.setWorkbook(Workbook.getWorkbook(new java.io.File(path)));

	 }

	 /**

	   * ��ȡ����������

	   * @return ����������

	   */

	 public int getNumberOfSheets(Workbook book){

	   return book == null ? 0 :book.getNumberOfSheets();

	 }

	 /**

	   * ��ȡ������������

	   * @param sheet ������

	   * @return ������������

	   */

	 public int getRows(Sheet sheet){

	   return sheet == null ?  0 : sheet.getRows();

	 }

	 /**

	   * ��ȡ�������

	   * @param sheet ������

	   * @return �������������

	   */

	 public int getColumns(Sheet sheet){

	   return sheet == null ?  0 : sheet.getColumns();

	 }

	 /**

	   * ��ȡÿ�е�Ԫ������

	   * @param sheet ������

	   * @param row ����

	   * @return ÿ�е�Ԫ������

	   */

	 public Cell[] getRows(Sheet sheet,int row){

	   return sheet == null || sheet.getRows() < row ? null : sheet.getRow(row);

	 }

	 /**

	   * ��ȡÿ�е�Ԫ������

	   * @param sheet ������

	   * @param endrow ������

	   * @param endCol ������

	   * @return ÿ�е�Ԫ������

	   */

	 public Cell[][] getCells(Sheet sheet,int endrow,int endcol){

	   return getCells(sheet,0,endrow,0,endcol);

	 }

	 /**

	   * ��ȡÿ�е�Ԫ������

	   * @param sheet ������

	   * @param startrow ����

	   * @param endrow ������

	   * @param startcol ��ʼ��

	   * @param endCol ������

	   * @return ÿ�е�Ԫ������

	   */

	 public Cell[][] getCells(Sheet sheet,int startrow,int endrow,int startcol,int endcol) {

	   Cell[][] cellArray = new Cell[endrow-startrow][endcol-startcol];

	   int maxRow = this.getRows(sheet);

	   int maxCos = this.getColumns(sheet);

	   for(int i = startrow ;i < endrow && i < maxRow ; i++){

	   

	    for(int j = startcol ; j < endcol && j < maxCos ; j++ ){

	   

	     cellArray[i-startrow][j-startcol] = sheet.getCell(j, i);

	    }

	   

	   }  

	   return cellArray;

	 }

	 /**

	   * �õ��е�ֵ

	   * @param sheet

	   * @param col

	   * @param startrow

	   * @param endrow

	   * @return

	   */

	 public Cell[] getColCells(Sheet sheet,int col,int startrow,int endrow){

	  Cell[] cellArray = new Cell[endrow-startrow];

	  int maxRow = this.getRows(sheet);

	  int maxCos = this.getColumns(sheet);

	  if(col <= 0 || col > maxCos || startrow > maxRow || endrow < startrow){

	   return null;

	  }

	  if(startrow < 0){

	   startrow = 0;

	  }

	  for(int i = startrow ;i < endrow && i < maxRow ; i++){

	   cellArray[i-startrow] = sheet.getCell(col,i);

	  }

	  return cellArray;

	}

	/**

	  * �õ��е�ֵ

	  * @param sheet

	  * @param row

	  * @param startcol

	  * @param endcol

	  * @return

	  */

	public Cell[] getRowCells(Sheet sheet,int row,int startcol,int endcol){

	  Cell[] cellArray = new Cell[endcol-startcol];

	  int maxRow = this.getRows(sheet);

	  int maxCos = this.getColumns(sheet);

	  if(row <= 0 || row > maxRow || startcol > maxCos || endcol < startcol){

	   return null;

	  }

	  if(startcol < 0){

	   startcol = 0;

	  }

	  for(int i = startcol ;i < startcol && i < maxCos ; i++){

	   cellArray[i-startcol] = sheet.getCell(i,row);

	  }

	  return cellArray;

	}

	  

	/**

	  * �������ID

	  * @return

	  */

	public static String getStrRandomId(){

	  String uuid = UUID.randomUUID().toString().replace("-","");  

	  return uuid;

	}

	/**

	  * ��װSQL���(��չ�������ݿ���������ֶε����)

	  * @param sheet ������

	  * @param startrow ��ʼ��

	  * @param endrow ������

	  * @param startcol ��ʼ��

	  * @param endcol ������

	  * @return SQL�������

	  */

	public Object[] constrctCellsSql(Sheet sheet,int startrow,int endrow,int startcol,int endcol,String payTime){

	  Cell[][] cellArray = getCells(sheet, startrow, endrow,startcol,endcol);

	  java.util.ArrayList<String> list = new java.util.ArrayList<String>();

	  StringBuffer bf = new StringBuffer("INSERT INTO " + tableName+"(ID,");

	  for(int i = 0 ; tableCols != null &&  i < tableCols.length ; i++){

	   if(i != tableCols.length -1)

	    bf.append(tableCols[i]).append(",");

	   else

	    bf.append(tableCols[i]).append("");

	  

	  }

	  bf.append(",PAY_TIME) VALUES ");

	  for(int i = 0;i< cellArray.length;i++){

	   //�ڵ�һ��ǰ�Ӹ��������

	   StringBuffer sqlBuffer = new StringBuffer();

	   sqlBuffer.append(bf.toString()+"('"+getStrRandomId()+"',");

	   Cell[] cell = cellArray[i];

	   if(tableCols != null && cell != null &&  tableCols.length != cell.length)

	    continue;

	   for(int j = 0 ; j < cell.length; j++){

	    String tmp = "";

	    if(cell[j] != null && cell[j].getContents() != null){

	     tmp = cell[j].getContents();

	    }

	    if(j != cell.length -1 )

	     sqlBuffer.append("'").append(tmp).append("',");

	    else

	     sqlBuffer.append("'").append(tmp).append("'");    

	   }

	   //����ʱ���ֶ�

	   sqlBuffer.append(",").append("to_date('"+payTime+"','YYYY-MM-DD HH24:MI:SS')");

	   sqlBuffer.append(")");

	   list.add(sqlBuffer.toString());

	   System.out.println(sqlBuffer.toString());

	  }

	  System.out.println(list);

	  return list.toArray();

	}

	/**

	  * ��ȡExcel�ļ�·��

	  * @return Excel�ļ�·��

	  */

	public String getPath(){

	  return this.path;

	}

	/**

	  * ����Excel�ļ�·��

	  * @param path Excel�ļ�·��

	  */

	public void setPath(String path){

	  this.path = path;

	}

	/**

	  * ��ȡ����������

	  */

	public Workbook getWorkbook(){

	  return this.workbook;

	}

	/**

	  * ���ù���������

	  * @param workbook ����������

	  */

	public void setWorkbook(Workbook workbook){

	  this.workbook = workbook;

	}
	
	/*���Ժ���
	public static void main(String[] args){

		  try {

		   File fileWrite = new File("c:/testWrite.xls");

		           fileWrite.createNewFile();

		          OutputStream os = new FileOutputStream(fileWrite);

		          Excel.writeExcel(os);

		         } catch (IOException e) {

		           // TODO Auto-generated catch block

		    e.printStackTrace();

		   }

		}*/
	
}