package com.pdfsearch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.table.DefaultTableModel;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;

import org.apache.poi.ss.usermodel.*;

public class PdfSearcher {
    private static final Tika TIKA = new Tika();

    /**
     * @param pdfFile    PDF file to analyse.
     * @param searchTerm Research term.
     * @param tableModel Table model to save risuls.
     */
    public static boolean searchInFile(File file, String searchTerm, DefaultTableModel tableModel, 
                                        boolean caseSensitive, boolean wholeWord) {
        
        String extension = getFileExtension(file.getName()).toLowerCase();
        String[] lines;
        
        if (extension.equals("pdf") ||
            extension.equals("doc") || extension.equals("docx") ||   // Word
            extension.equals("ppt") || extension.equals("pptx") ||   // PowerPoint
            extension.equals("odt") || extension.equals("ods")  || extension.equals("odp") // OpenOffice
            ) {
            try {
                String pdfText = TIKA.parseToString(file);
                lines = pdfText.split("\n");
            } catch (TikaException | IOException e) {
                System.err.println("Error reading file: " + file.getName());
                return false;
            }
        } else if (extension.equals("txt") || 
                    extension.equals("md") || 
                    extension.equals("java")|| 
                    extension.equals("xml") ||
                    extension.equals("html") ||
                    extension.equals("log") ||
                    extension.equals("json") ||
                    extension.equals("yaml") ||
                    extension.equals("yml") ||
                    extension.equals("js") ||
                    extension.equals("bib")
                ){
            try {
                lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8).toArray(new String[0]);
            } catch (IOException e) {
                System.err.println("Error reading text file: " + file.getName());
                return false;
            }
        }else if (extension.equals("csv")) { //CSV
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                lines = reader.lines().toArray(String[]::new);
            } catch (IOException e) {
                System.err.println("Error in reading CSV file: " + file.getAbsolutePath());
                return false;
            }
        }else if (extension.equals("xls") || extension.equals("xlsx")) { // XLS, XLSX
            try (FileInputStream fis = new FileInputStream(file)) {
                Workbook workbook = extension.equals("xls") ? new HSSFWorkbook(fis) : new XSSFWorkbook(fis);
                Sheet sheet = workbook.getSheetAt(0);
                lines = new String[sheet.getPhysicalNumberOfRows()];
                for (int i = 0; i < sheet.getPhysicalNumberOfRows(); i++) {
                    Row row = sheet.getRow(i);
                    if (row != null) {
                        StringBuilder line = new StringBuilder();
                        for (Cell cell : row) {
                            line.append(cell.toString()).append(" ");
                        }
                        lines[i] = line.toString();
                    }
                }
            } catch (IOException e) {
                System.err.println("Error in reading Excel file: " + file.getAbsolutePath());
                return false;
            }
        } else {
            return false;
        }

        boolean hasResults = false;

        String patternText = Pattern.quote(searchTerm);
        if (wholeWord) {
            patternText = "\\b" + patternText + "\\b";
        }

        Pattern pattern = caseSensitive
                ? Pattern.compile(patternText)
                : Pattern.compile(patternText, Pattern.CASE_INSENSITIVE);

        for (String line : lines) {
            Matcher matcher = pattern.matcher(line);
            while (matcher.find()) {
                tableModel.addRow(new Object[]{line.trim(), file.getName()});
                hasResults = true;
                break;
            }
        }
        return hasResults;
    }

    private static String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        return (lastDot == -1) ? "" : fileName.substring(lastDot + 1);
    }
}
