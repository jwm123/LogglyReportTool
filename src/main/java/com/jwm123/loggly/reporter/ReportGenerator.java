/**
 *    Copyright 2013 jwm123
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package com.jwm123.loggly.reporter;

import org.apache.commons.io.IOUtils;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * com.jwm123.loggly.reporter.ReportGenerator
 *
 * @author jmcentire
 */
public class ReportGenerator {

  private Workbook workbook;

  public ReportGenerator(File reportFile) throws IOException, IllegalArgumentException {
    if(reportFile.exists()) {
      InputStream in = null;
      try {
        in = new FileInputStream(reportFile);
        if (reportFile.getName().endsWith(".xls")) {
          POIFSFileSystem fs = new POIFSFileSystem(in);
          workbook = new HSSFWorkbook(fs);
        } else if (reportFile.getName().endsWith(".xlsx")) {
          workbook = new XSSFWorkbook(in);
        } else {
          throw new IllegalArgumentException("Invalid report filename. " + reportFile.getName());
        }
      } finally {
        IOUtils.closeQuietly(in);
      }
    } else {
      if(reportFile.getName().endsWith(".xls")) {
        workbook = new HSSFWorkbook();
      } else if (reportFile.getName().endsWith(".xlsx")){
        workbook = new XSSFWorkbook();
      } else {
        throw new IllegalArgumentException("Invalid report filename. "+reportFile.getName());
      }
    }
  }

  public byte[] build(List<Map<String, String>> row) throws IOException {
    List<String> colLabels = new ArrayList<String>();
    Sheet reportSheet = workbook.getSheet("Report");
    if(reportSheet == null) {
      reportSheet = workbook.createSheet("Report");
    }
    Row firstRow = reportSheet.getRow(0);
    if(firstRow == null) {
      firstRow = reportSheet.createRow(0);
      int cellNum = 0;
      for(Map<String, String> col : row) {
        for(String key : col.keySet()) {
          Cell cell = firstRow.createCell(cellNum++);
          setCellValue(cell, key);
        }
      }
    }
    for(int i = 0; i < firstRow.getLastCellNum(); i++) {
      Cell cell = firstRow.getCell(i);
      if(cell != null) {
        colLabels.add(cell.getStringCellValue());
      }
    }
    Row newRow = reportSheet.createRow(reportSheet.getLastRowNum() + 1);
    int lastIndex = -1;
    for(Map<String, String> col : row) {
      for(String key : col.keySet()){
        int colNum = -1;
        Cell cell = null;
        if(colLabels.contains(key)) {
          colNum = colLabels.indexOf(key);
          lastIndex = colNum;
        }
        if(colNum == -1) {
          lastIndex++;
          colNum = lastIndex;
          shiftColumns(reportSheet, colNum, key);
          colLabels.add(colNum, key);
        }
        cell = newRow.getCell(colNum);
        if(cell == null) {
          cell = newRow.createCell(colNum);
        }
        setCellValue(cell, col.get(key));
      }
    }
    for(int i = 0; i < firstRow.getLastCellNum(); i++) {
      reportSheet.autoSizeColumn(i);
    }
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    workbook.write(baos);
    return baos.toByteArray();
  }

  private void shiftColumns(Sheet reportSheet, int colNum, String key) {
    boolean firstRow = true;
    for(Row row : reportSheet) {
      for(int i = row.getLastCellNum()+1; i >= colNum; i--) {
        Cell nextCell = row.getCell(i+1);
        Cell oldCell = row.getCell(i);
        if(oldCell != null) {
          if(nextCell == null) {
            nextCell = row.createCell(i+1);
          }
          setCellValue(nextCell, oldCell.getStringCellValue());
          if(firstRow && i == colNum) {
            setCellValue(oldCell, key);
            firstRow = false;
          } else {
            setCellValue(oldCell, "");
          }
        } else if(firstRow && i == colNum) {
          oldCell = row.createCell(i);
          setCellValue(oldCell, key);
          firstRow = false;
        }
      }
    }
  }

  private void setCellValue(Cell cellToSet, String value) {
    if(value.matches("\\d*")) {
      cellToSet.setCellType(Cell.CELL_TYPE_NUMERIC);
      cellToSet.setCellValue(new Integer(value));
    } else if (value.startsWith("=")) {
      cellToSet.setCellType(Cell.CELL_TYPE_FORMULA);
      cellToSet.setCellValue(value);
    } else {
      cellToSet.setCellValue(value);
    }
  }
}
