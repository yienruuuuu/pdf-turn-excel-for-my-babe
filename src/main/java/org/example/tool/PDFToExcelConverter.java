package org.example.tool;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.util.List;
import java.util.Map;

/**
 * @author Eric.Lee
 * Date:2024/9/29
 */
public class PDFToExcelConverter {


    // 將提取的數據轉換成 Excel 格式
    public void convertToExcel(List<Map<String, String>> allFirstBlockData, List<Map<String, String>> allThirdBlockData, String outputFilePath) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Data");

        // 創建標題行
        String[] headers = {"ID no", "性別(男1女2)", "年齡", "哪一邊(右=1左=2)", "N1", "P1", "N1P1 Amp"};
        Row headerRow = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
        }

        int rowNum = 1;

        // 創建數據行，這裡需要同時處理來自不同 PDF 的數據
        for (int i = 0; i < allFirstBlockData.size(); i++) {
            Map<String, String> firstBlockData = allFirstBlockData.get(i);

            // 對於每個ID，我們篩選對應的最小L和R
            Map<String, String> rData = null;
            Map<String, String> lData = null;

            // 根據每個ID篩選出L和R數據
            for (Map<String, String> thirdBlockRow : allThirdBlockData) {
                // 避免每次都遍歷所有的表格行，這裡可以加上對應ID的檢查
                if (thirdBlockRow.get("ID no").equals(firstBlockData.get("ID no"))) {
                    if (thirdBlockRow.get("Curve").contains("R")) {
                        rData = thirdBlockRow;
                    } else if (thirdBlockRow.get("Curve").contains("L")) {
                        lData = thirdBlockRow;
                    }
                }
            }

            // 寫入右耳數據（R）
            if (rData != null) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(firstBlockData.get("ID no"));
                row.createCell(1).setCellValue(firstBlockData.get("Sex").contains("F") ? "2" : "1");
                row.createCell(2).setCellValue(firstBlockData.get("Age"));
                row.createCell(3).setCellValue("1");  // 右耳
                row.createCell(4).setCellValue(rData.getOrDefault("N1", "NR"));
                row.createCell(5).setCellValue(rData.getOrDefault("P1", "NR"));
                row.createCell(6).setCellValue(rData.getOrDefault("N1-P1 Amp", "NR"));
            }

            // 寫入左耳數據（L）
            if (lData != null) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(firstBlockData.get("ID no"));
                row.createCell(1).setCellValue(firstBlockData.get("Sex").contains("F") ? "2" : "1");
                row.createCell(2).setCellValue(firstBlockData.get("Age"));
                row.createCell(3).setCellValue("2");  // 左耳
                row.createCell(4).setCellValue(lData.getOrDefault("N1", "NR"));
                row.createCell(5).setCellValue(lData.getOrDefault("P1", "NR"));
                row.createCell(6).setCellValue(lData.getOrDefault("N1-P1 Amp", "NR"));
            }
        }

        // 寫入Excel文件
        try (FileOutputStream fileOut = new FileOutputStream(outputFilePath)) {
            workbook.write(fileOut);
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            workbook.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}