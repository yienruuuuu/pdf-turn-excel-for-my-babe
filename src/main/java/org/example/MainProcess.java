package org.example;

import org.example.tool.PDFReader;
import org.example.tool.PDFToExcelConverter;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Eric.Lee
 * Date:2024/9/29
 */
@Component
public class MainProcess {
    private final PDFReader pdfReader;
    private final PDFToExcelConverter pdfToExcelConverter;

    public MainProcess(PDFReader pdfReader, PDFToExcelConverter pdfToExcelConverter) {
        this.pdfReader = pdfReader;
        this.pdfToExcelConverter = pdfToExcelConverter;
    }

    public void startConvert(String folderPath, String outputFilePath) {
        File folder = new File(folderPath);
        File[] pdfFiles = folder.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));

        if (pdfFiles == null || pdfFiles.length == 0) {
            System.out.println("資料夾中沒有找到 PDF 文件。");
            return;
        }

        // 用來保存所有 PDF 的數據
        List<Map<String, String>> allFirstBlockData = new ArrayList<>();
        List<Map<String, String>> allThirdBlockData = new ArrayList<>();

        for (File pdfFile : pdfFiles) {
            String filePath = pdfFile.getAbsolutePath();

            // 提取第一區塊
            Map<String, String> firstBlockData = pdfReader.extractFirstBlock(filePath);
            allFirstBlockData.add(firstBlockData);
            firstBlockData.get("ID no");
            System.out.println(firstBlockData.get("ID no"));

            // 提取第三區塊
            List<Map<String, String>> thirdBlockData = pdfReader.extractThirdBlock(filePath,firstBlockData.get("ID no"));
            System.out.println("第三區塊提取結果：");
            for (Map<String, String> row : thirdBlockData) {
                System.out.println(row);
            }

            // 處理篩選出最小的L和R
            List<Map<String, String>> filteredCurvesData = pdfReader.processCurves(thirdBlockData);
            // 打印結果
            System.out.println("篩選後的結果：");
            for (Map<String, String> row : filteredCurvesData) {
                System.out.println(row);
            }

            // 保存到總列表中
            allThirdBlockData.addAll(filteredCurvesData);
        }

        // 將提取的所有 PDF 的數據轉換為一個 Excel
        pdfToExcelConverter.convertToExcel(allFirstBlockData, allThirdBlockData, outputFilePath);
    }
}
