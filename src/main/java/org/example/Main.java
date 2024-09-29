package org.example;

import org.example.tool.PDFReader;
import org.example.tool.PDFToExcelConverter;

/**
 * @author Eric.Lee
 * Date:2024/9/29
 */
public class Main {
    public static void main(String[] args) {
        PDFReader pdfReader = new PDFReader();
        PDFToExcelConverter pdfToExcelConverter = new PDFToExcelConverter();

        MainProcess process = new MainProcess(pdfReader, pdfToExcelConverter);
        process.startConvert("F:\\桌面-儒\\ddd\\", "output-file.xlsx");
    }
}