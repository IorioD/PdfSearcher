package com.pdfsearch;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

public class PdfSearchGui {
    private static final String DEFAULT_PATH = "C:/Users";
    private static final String[] COLUMN_NAMES = {/*"Riga",*/ "Occurrence", "File"};
    private volatile boolean running = false;

    public void createAndShowGui() {
        JDialog dialog = new JDialog();
        dialog.setTitle("PDF Searcher");
        dialog.setSize(1000, 800);
        dialog.setLocationRelativeTo(null);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setIconImage(new ImageIcon("icon.png").getImage());

        // Componenti principali
        JTextField searchField = new JTextField();
        addContextMenu(searchField);
        
        JFileChooser fileChooser = createFileChooser();
        DefaultTableModel tableModel = createTableModel();
        JTable resultTable = new JTable(tableModel);
        JLabel fileCountLabel = new JLabel("Scanned file: 0");
        JLabel entryCountLabel = new JLabel("File with results: 0 | Entry found: 0");

        // Layout principale
        dialog.setLayout(new BorderLayout());
        dialog.add(createTopPanel(searchField, fileChooser), BorderLayout.NORTH);
        dialog.add(createResultPane(resultTable), BorderLayout.CENTER);
        dialog.add(createButtonPanel(dialog, searchField, fileChooser, tableModel, fileCountLabel, entryCountLabel), BorderLayout.SOUTH);

        resultTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openSelectedFile(resultTable, fileChooser);
                }
            }
        });
        resultTable.setDefaultRenderer(Object.class, new FileHighlightRenderer());
        dialog.setVisible(true);
    }

    private JFileChooser createFileChooser() {
        JFileChooser chooser = new JFileChooser();
        chooser.setControlButtonsAreShown(false);
        chooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        chooser.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("PDF Files", "pdf"));

        File initialDirectory = new File(DEFAULT_PATH);
        if (initialDirectory.exists() && initialDirectory.isDirectory()) {
            chooser.setCurrentDirectory(initialDirectory);
        }else{
            initialDirectory = new File(System.getProperty("user.home"),"Desktop");
            chooser.setCurrentDirectory(initialDirectory);
        }
        return chooser;
    }

    private DefaultTableModel createTableModel() {
        return new DefaultTableModel(COLUMN_NAMES, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }

    private JPanel createTopPanel(JTextField searchField, JFileChooser fileChooser) {
        JPanel searchPanel = new JPanel(new BorderLayout());
        searchPanel.add(new JLabel("Insert the search input:"), BorderLayout.NORTH);
        searchPanel.add(searchField, BorderLayout.CENTER);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(searchPanel, BorderLayout.NORTH);
        topPanel.add(fileChooser, BorderLayout.CENTER);

        return topPanel;
    }

    private JScrollPane createResultPane(JTable resultTable) {
        return new JScrollPane(resultTable);
    }

    private JPanel createButtonPanel(JDialog dialog, JTextField searchField, JFileChooser fileChooser,
                                     DefaultTableModel tableModel, JLabel fileCountLabel, JLabel entryCountLabel) {
        JPanel buttonPanel = new JPanel(new BorderLayout());
        JPanel buttons = new JPanel();
        JButton searchButton = new JButton("Search");
        JButton closeButton = new JButton("Close");
        JButton stopButton = new JButton("Stop");
        stopButton.setEnabled(false);

        searchField.addActionListener(e -> searchButton.doClick());
        buttons.add(searchButton);
        buttons.add(closeButton);
        buttons.add(stopButton);

        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        fileCountLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        entryCountLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        infoPanel.add(fileCountLabel);
        infoPanel.add(entryCountLabel);

        JProgressBar progressBar = new JProgressBar();
        progressBar.setStringPainted(true); // Mostra il testo del progresso
        progressBar.setVisible(false); // Nascondi finché non inizia la ricerca

        buttonPanel.add(buttons, BorderLayout.CENTER);
        buttonPanel.add(infoPanel, BorderLayout.NORTH);
        buttonPanel.add(progressBar, BorderLayout.SOUTH);

        searchButton.addActionListener(e -> performSearchWithProgress(dialog, searchField, fileChooser, 
                                                                        tableModel, fileCountLabel,
                                                                         entryCountLabel, progressBar, stopButton));
        closeButton.addActionListener(e -> dialog.dispose());

        return buttonPanel;
    }

    private void addContextMenu(JTextField textField) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem cutItem = new JMenuItem(new AbstractAction("Cut") {
            @Override
            public void actionPerformed(ActionEvent e) {
                textField.cut();
            }
        });
        JMenuItem copyItem = new JMenuItem(new AbstractAction("Copy") {
            @Override
            public void actionPerformed(ActionEvent e) {
                textField.copy();
            }
        });
        JMenuItem pasteItem = new JMenuItem(new AbstractAction("Paste") {
            @Override
            public void actionPerformed(ActionEvent e) {
                textField.paste();
            }
        });

        menu.add(cutItem);
        menu.add(copyItem);
        menu.add(pasteItem);

        textField.setComponentPopupMenu(menu);
    }

    private static class FileHighlightRenderer extends DefaultTableCellRenderer {
        private final Color[] colors = {new Color(230, 240, 255), new Color(255, 230, 240)};
        private final java.util.Map<String, Integer> fileColorMap = new java.util.HashMap<>();
        private int currentColorIndex = 0;

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            
            if (!isSelected) {
                String fileName = (String) table.getValueAt(row, 1); // Indice della colonna del file (indici partono da 0)
                fileColorMap.putIfAbsent(fileName, currentColorIndex);
                currentColorIndex = (fileColorMap.size() % colors.length);
                component.setBackground(colors[fileColorMap.get(fileName)]);
            } else {
                component.setBackground(table.getSelectionBackground());
            }
            return component;
        }
    }

    private void performSearchWithProgress(JDialog dialog, JTextField searchField, JFileChooser fileChooser,
                                           DefaultTableModel tableModel, JLabel fileCountLabel, 
                                           JLabel entryCountLabel, JProgressBar progressBar, JButton stopButton) {
        String searchTerm = searchField.getText().trim();
        if (searchTerm.isEmpty()) {
            JOptionPane.showMessageDialog(dialog, "Please insert a search input.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        File selectedFileOrDirectory = fileChooser.getSelectedFile();
        if (selectedFileOrDirectory == null) {
            JOptionPane.showMessageDialog(dialog, "Please select a valid file or directory.", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        progressBar.setVisible(true);
        progressBar.setIndeterminate(true);
        stopButton.setEnabled(true);
        running = true;

        SwingWorker<Void, Integer> worker = new SwingWorker<>() {
            private int fileCount = 0;
            private int entryCount = 0;
            private int matchingFileCount = 0;

            @Override
            protected Void doInBackground() throws Exception {
                tableModel.setRowCount(0);

                if (selectedFileOrDirectory.isDirectory()) {
                    File[] pdfFiles = selectedFileOrDirectory.listFiles((dir, name) -> name.toLowerCase().endsWith(".pdf"));
                    if (pdfFiles == null || pdfFiles.length == 0) {
                        JOptionPane.showMessageDialog(dialog, "No PDF file found in the selected directory.", "Error", JOptionPane.ERROR_MESSAGE);
                        return null;
                    }
                    fileCount = pdfFiles.length;

                    progressBar.setIndeterminate(false);
                    progressBar.setMaximum(fileCount);

                    for (int i = 0; i < pdfFiles.length; i++) {
                        if (!running) break;
                        if (PdfSearcher.searchInFile(pdfFiles[i], searchTerm, tableModel)) {
                            matchingFileCount++;
                        }
                        entryCount = tableModel.getRowCount();
                        publish(i + 1); //aggiorna progresso
                    }
                } else if (selectedFileOrDirectory.isFile() && selectedFileOrDirectory.getName().toLowerCase().endsWith(".pdf")) {
                    fileCount = 1;
                    
                    progressBar.setIndeterminate(false);
                    progressBar.setMaximum(1);

                    if (!running) return null;
                    if (PdfSearcher.searchInFile(selectedFileOrDirectory, searchTerm, tableModel)) {
                        matchingFileCount++;
                    }
                    entryCount = tableModel.getRowCount();
                    publish(1); //aggiorna progresso
                }
                return null;
            }

            @Override
            protected void process(java.util.List<Integer> chunks) {
                for (Integer progress : chunks) {
                    progressBar.setValue(progress);
                }
            }

            @Override
            protected void done() {
                progressBar.setVisible(false);
                stopButton.setEnabled(false);
                fileCountLabel.setText("Scanned file: " + fileCount);
                entryCountLabel.setText("File with results: " + matchingFileCount + " | Entry found: " + entryCount);

                if (entryCount == 0) {
                    JOptionPane.showMessageDialog(dialog, "Search term \"" + searchTerm + "\" not found.", "Risults", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        };
        
        stopButton.addActionListener(e -> {
            running = false;
            stopButton.setEnabled(false);
        });

        worker.execute();
    }

    private void openSelectedFile(JTable resultTable, JFileChooser fileChooser) {
        int selectedRow = resultTable.getSelectedRow();
        if (selectedRow != -1) {
            String fileName = (String) resultTable.getValueAt(selectedRow, 1); // indice col
            File selectedFileOrDirectory = fileChooser.getSelectedFile();
            File pdfFile;

            if(selectedFileOrDirectory.isDirectory()){
                pdfFile=new File(selectedFileOrDirectory, fileName);
            }else{
                pdfFile = selectedFileOrDirectory;
            }

            try {
                if (Desktop.isDesktopSupported() && pdfFile.exists()) {
                    Desktop.getDesktop().open(pdfFile);
                } else {
                    String errorMessage = "<html><body style='width: 300px;'>Unable to open file: " + pdfFile.getAbsolutePath() + "</body></html>";
                    JOptionPane.showMessageDialog(null, errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (IOException e) {
                String errorMessage = "<html><body style='width: 300px;'>Error opening file: " + pdfFile.getAbsolutePath() + "</body></html>";
                JOptionPane.showMessageDialog(null, errorMessage, "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
