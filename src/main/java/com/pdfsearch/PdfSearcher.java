package com.pdfsearch;

import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;

import javax.swing.table.DefaultTableModel;
import java.io.File;
import java.io.IOException;

public class PdfSearcher {
    private static final Tika TIKA = new Tika();

    /**
     * @param pdfFile    PDF file to analyse.
     * @param searchTerm Research term.
     * @param tableModel Table model to save risuls.
     * @return 
     * @throws IOException   Error in opening file.
     * @throws TikaException Error in file analysis.
     */
    public static boolean searchInFile(File pdfFile, String searchTerm, DefaultTableModel tableModel) throws IOException, TikaException {
        String pdfText = TIKA.parseToString(pdfFile);
        String[] lines = pdfText.split("\n");
        boolean hasResults = false;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.toLowerCase().contains(searchTerm.toLowerCase())) {
                tableModel.addRow(new Object[]{/*i + 1,*/ line, pdfFile.getName()});
                hasResults = true; 
            }
        }
        return hasResults;
    }
}