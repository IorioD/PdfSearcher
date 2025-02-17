package com.pdfsearch;

import javax.swing.SwingUtilities;

public class PdfSearchApplication {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            PdfSearchGui gui = new PdfSearchGui();
            gui.createAndShowGui();
        });
    }
}
