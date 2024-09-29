package org.example.tool;

import org.apache.pdfbox.pdmodel.PDDocument;
import technology.tabula.ObjectExtractor;
import technology.tabula.Page;
import technology.tabula.RectangularTextContainer;
import technology.tabula.Table;
import technology.tabula.extractors.BasicExtractionAlgorithm;
import technology.tabula.extractors.SpreadsheetExtractionAlgorithm;

import java.io.File;
import java.util.*;

/**
 * @author Eric.Lee
 * Date:2024/9/29
 */
public class PDFReader {
//    public static void main(String[] args) {
//        String filePath = "F:\\桌面-儒\\ddd\\1110823-6298199-o.pdf";
//        // 處理第一區塊並保存為 Map
//        Map<String, String> firstBlockData = extractFirstBlock(filePath);
//        System.out.println("第一區塊提取結果：");
//        for (Map.Entry<String, String> entry : firstBlockData.entrySet()) {
//            System.out.println(entry.getKey() + ": " + entry.getValue());
//        }
//
//
//        // 提取第三區塊並顯示為表格格式
//        List<Map<String, String>> thirdBlockData = extractThirdBlock(filePath);
//        System.out.println("第三區塊提取結果：");
//        for (Map<String, String> row : thirdBlockData) {
//            System.out.println(row);
//        }
//
//        // 處理篩選出最小的L和R
//        List<Map<String, String>> filteredCurvesData = processCurves(thirdBlockData);
//        // 打印結果
//        System.out.println("篩選後的結果：");
//        for (Map<String, String> row : filteredCurvesData) {
//            System.out.println(row);
//        }
//    }

    // 提取第一區塊：使用 BasicExtractionAlgorithm (Stream 模式)
    public Map<String, String> extractFirstBlock(String filePath) {
        Map<String, String> extractedData = new HashMap<>();
        try {
            // 使用 Tabula 加載 PDF 文件
            PDDocument document = PDDocument.load(new File(filePath));
            ObjectExtractor extractor = new ObjectExtractor(document);

            // 提取第1頁
            Page page = extractor.extract(1);

            // 使用 BasicExtractionAlgorithm 提取表格 (Stream 模式)
            BasicExtractionAlgorithm algorithm = new BasicExtractionAlgorithm();
            List<Table> tables = algorithm.extract(page);

            // 準備欄位名稱清單
            String[] fields = {"Last name", "Sex", "ID no", "First name(s)", "Age", "Date of test", "Address", "Day of birth", "Tel", "Examiner", "E-Mail"};

            for (Table table : tables) {
                List<List<RectangularTextContainer>> rows = table.getRows();
                for (List<RectangularTextContainer> row : rows) {
                    StringBuilder rowText = new StringBuilder();
                    for (RectangularTextContainer cell : row) {
                        String cellText = cell.getText().trim();
                        // 將每行數據拼接起來，然後去掉多餘的 `|` 符號
                        rowText.append(cellText).append(" ");
                    }
                    String cleanRow = rowText.toString().replaceAll("\\|", "").trim(); // 去掉多餘的分隔符

                    // 根據欄位名稱清單匹配對應的數據
                    for (String field : fields) {
                        // 查找每個欄位
                        if (cleanRow.contains(field) && !extractedData.containsKey(field)) {
                            // 提取欄位名之後到下一個欄位之前的內容
                            String value = extractValue(cleanRow, field, fields);
                            // 去除資料的冒號、空白字元
                            if (!value.isEmpty()) {
                                extractedData.put(field, value.replace(":", "").trim());  // 移除冒號並去掉空白
                            } else {
                                extractedData.put(field, "N/A");  // 如果沒有內容則填 N/A
                            }
                        }
                    }
                }
            }
            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return extractedData;
    }

    // 從 cleanRow 中提取欄位值
    private static String extractValue(String cleanRow, String field, String[] fields) {
        String value = "";
        int fieldIndex = cleanRow.indexOf(field) + field.length();
        value = cleanRow.substring(fieldIndex).trim();

        // 找下一個欄位名，將其作為結束點
        for (String nextField : fields) {
            if (value.contains(nextField)) {
                value = value.substring(0, value.indexOf(nextField)).trim();
                break;
            }
        }
        return value;
    }

    // 提取第三區塊：使用 SpreadsheetExtractionAlgorithm (Lattice 模式)
    public List<Map<String, String>> extractThirdBlock(String filePath, String idNo) {
        List<Map<String, String>> tableData = new ArrayList<>();

        try {
            // 使用 Tabula 加載 PDF 文件
            PDDocument document = PDDocument.load(new File(filePath));
            ObjectExtractor extractor = new ObjectExtractor(document);

            // 提取第1頁
            Page page = extractor.extract(1);

            // 使用 Lattice 模式提取表格 (SpreadsheetExtractionAlgorithm)
            SpreadsheetExtractionAlgorithm algorithm = new SpreadsheetExtractionAlgorithm();
            List<Table> tables = algorithm.extract(page);

            // 定義表格標題
            String[] headers = {"Curve", "P1", "N1", "P1'", "N1'", "N1-P1 Lat", "N1-P1 Amp", "(LA-SA)/(R+L)"};

            boolean inLatencySection = false;
            for (Table table : tables) {
                List<List<RectangularTextContainer>> rows = table.getRows();
                for (List<RectangularTextContainer> row : rows) {
                    Map<String, String> rowData = new LinkedHashMap<>();
                    List<String> rowValues = new ArrayList<>();
                    for (RectangularTextContainer cell : row) {
                        String text = cell.getText().trim();
                        if (text.equalsIgnoreCase("Latencies (ms)")) {
                            inLatencySection = true; // 進入 Latencies 部分
                        }
                        if (inLatencySection && text.equalsIgnoreCase("Name of used protocol")) {
                            inLatencySection = false; // 結束 Latencies 部分
                        }
                        if (inLatencySection) {
                            rowValues.add(text); // 收集行數據
                        }
                    }

                    // 如果該行有數據且在 Latencies 區間，將其加入 tableData
                    if (inLatencySection && rowValues.size() == headers.length) {
                        for (int i = 0; i < rowValues.size(); i++) {
                            rowData.put(headers[i], rowValues.get(i));
                        }

                        // 添加 `ID no` 到每一行
                        rowData.put("ID no", idNo);

                        // 只將有 `Curve` 值的行加入到結果中
                        if (!rowData.get("Curve").isEmpty()) {
                            tableData.add(rowData);
                        }
                    }
                }
            }

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return tableData;
    }

    // 新增處理方法，用於篩選出最小的R和L
    public List<Map<String, String>> processCurves(List<Map<String, String>> tableData) {
        Map<String, Map<String, String>> curveGroups = new HashMap<>(); // 用來存放最小的 L 和 R

        for (Map<String, String> rowData : tableData) {
            String curve = rowData.get("Curve");
            if (!curve.isEmpty()) {
                String group = curve.startsWith("105 L") ? "L" : curve.startsWith("105 R") ? "R" : null;

                if (group != null) {
                    String currentKey = curve.replaceAll("[^0-9]", "");
                    int currentIndex = currentKey.isEmpty() ? 0 : Integer.parseInt(currentKey);

                    boolean hasValidData = !(rowData.get("N1").isEmpty() && rowData.get("P1").isEmpty() && rowData.get("N1-P1 Amp").isEmpty());

                    if (!curveGroups.containsKey(group)) {
                        if (!hasValidData) {
                            rowData.put("N1", "NR");
                            rowData.put("P1", "NR");
                            rowData.put("N1-P1 Amp", "NR");
                        }
                        curveGroups.put(group, rowData);
                    } else {
                        String existingCurve = curveGroups.get(group).get("Curve");
                        String existingKey = existingCurve.replaceAll("[^0-9]", "");
                        int existingIndex = existingKey.isEmpty() ? 0 : Integer.parseInt(existingKey);

                        if (currentIndex < existingIndex) {
                            if (!hasValidData) {
                                rowData.put("N1", "NR");
                                rowData.put("P1", "NR");
                                rowData.put("N1-P1 Amp", "NR");
                            }
                            curveGroups.put(group, rowData);
                        }
                    }
                }
            }
        }

        return new ArrayList<>(curveGroups.values());
    }

}
