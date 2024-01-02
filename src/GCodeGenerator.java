import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

public class GCodeGenerator extends JFrame {

    private ArrayList<Row> rows;
    private JTextField millTextField;
    private JTextField freqTextField;
    private JTextField feedTextField;
    private JTextField stepDownTextField;
    private JCheckBox mirrorCheckBox;

    public GCodeGenerator() {
        rows = new ArrayList<>();

        setTitle("Фрезерование отверстий относительно верха заготовки");
        setSize(650, 730);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(null);

        JPanel panel = new JPanel();
        panel.setBounds(0, 0, 650, 730);
        panel.setLayout(null);
        add(panel);

        JPanel canvasPanel = new JPanel();
        canvasPanel.setBounds(0, 0, 500, 480);
        canvasPanel.setLayout(null);
        panel.add(canvasPanel);

        JPanel settingsPanel = new JPanel();
        settingsPanel.setBounds(0, 480, 650, 250);
        settingsPanel.setLayout(null);
        panel.add(settingsPanel);

        JLabel millLabel = new JLabel("Диаметр фрезы");
        millLabel.setBounds(10, 10, 150, 25);
        settingsPanel.add(millLabel);

        millTextField = new JTextField();
        millTextField.setBounds(10, 40, 150, 25);
        settingsPanel.add(millTextField);

        JLabel freqLabel = new JLabel("Шпиндель об/мин");
        freqLabel.setBounds(170, 10, 150, 25);
        settingsPanel.add(freqLabel);

        freqTextField = new JTextField();
        freqTextField.setBounds(170, 40, 150, 25);
        settingsPanel.add(freqTextField);

        JLabel feedLabel = new JLabel("Подача мм/мин");
        feedLabel.setBounds(330, 10, 150, 25);
        settingsPanel.add(feedLabel);

        feedTextField = new JTextField();
        feedTextField.setBounds(330, 40, 150, 25);
        settingsPanel.add(feedTextField);

        JLabel stepDownLabel = new JLabel("Шаг по Z");
        stepDownLabel.setBounds(490, 10, 150, 25);
        settingsPanel.add(stepDownLabel);

        stepDownTextField = new JTextField();
        stepDownTextField.setBounds(490, 40, 150, 25);
        settingsPanel.add(stepDownTextField);

        mirrorCheckBox = new JCheckBox("Зеркально");
        mirrorCheckBox.setBounds(10, 80, 150, 25);
        settingsPanel.add(mirrorCheckBox);

        JButton generateButton = new JButton("Сформировать G-code");
        generateButton.setBounds(490, 80, 150, 25);
        settingsPanel.add(generateButton);

        JLabel xLabel = new JLabel("X");
        xLabel.setBounds(10, 120, 150, 25);
        settingsPanel.add(xLabel);

        JLabel yLabel = new JLabel("Y\n(паз Y1;Y2)");
        yLabel.setBounds(170, 120, 150, 25);
        settingsPanel.add(yLabel);

        JLabel dLabel = new JLabel("Диаметр отверстия\n(ширина паза)");
        dLabel.setBounds(330, 120, 150, 25);
        settingsPanel.add(dLabel);

        JLabel depthLabel = new JLabel("Глубина");
        depthLabel.setBounds(490, 120, 150, 25);
        settingsPanel.add(depthLabel);

        JPanel rowsPanel = new JPanel();
        rowsPanel.setBounds(10, 150, 630, 70);
        rowsPanel.setLayout(null);
        settingsPanel.add(rowsPanel);

        JScrollPane scrollPane = new JScrollPane(rowsPanel);
        scrollPane.setBounds(10, 150, 630, 70);
        settingsPanel.add(scrollPane);

        generateButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                generateGCode();
            }
        });

        setVisible(true);
    }

    private void generateGCode() {
        double mill = Double.parseDouble(millTextField.getText());
        int freq = Integer.parseInt(freqTextField.getText());
        int feed = Integer.parseInt(feedTextField.getText());
        double stepDown = Double.parseDouble(stepDownTextField.getText());
        boolean mirror = mirrorCheckBox.isSelected();

        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter("temp_java.txt"));

            writer.write("G0G40G49G80\n");
            writer.write("G21\n");
            writer.write("G17G90\n");
            writer.write("G" + (mirror ? "55" : "54") + "\n");
            writer.write("G0 Z150.0\n");
            writer.write("M3 S" + freq + "\n");

            for (Row row : rows) {
                String x = row.getXField().getText();
                String y = row.getYField().getText();
                String d = row.getDField().getText();
                String depth = row.getDepthField().getText();

                if (!x.isEmpty() && !y.isEmpty() && !d.isEmpty() && !depth.isEmpty()) {
                    double xValue = Double.parseDouble(x);
                    String[] yValues = y.split(";");
                    double dValue = Double.parseDouble(d);
                    double depthValue = Double.parseDouble(depth);

                    if (dValue < mill) {
                        JOptionPane.showMessageDialog(null, "Диаметр отверстия меньше диаметра фрезы",
                                "Некорректные данные", JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    if (row.isSlot()) {
                        double y1 = Double.parseDouble(yValues[0]);
                        double y2 = Double.parseDouble(yValues[1]);

                        if (y2 > row.getMaxY()) {
                            row.setMaxY(y2);
                        }

                        if (mirror) {
                            xValue -= dValue;
                        }

                        writer.write(slotting(xValue, y1, y2, dValue, depthValue, mill, feed, stepDown));
                    } else {
                        writer.write("'moving\n");
                        writer.write("G0 X" + xValue + " Y" + y + "\n");
                        writer.write("Z5.0\n");

                        if (dValue == mill) {
                            writer.write(drilling(depthValue, feed, stepDown));
                        } else {
                            writer.write(milling(xValue, y, dValue, depthValue, mill, feed, stepDown));
                        }
                    }

                    writer.write("G0 Z10.0\n\n");
                }
            }

            writer.write("'end\n");
            writer.write("G0 Z150.0\n");
            writer.write("M5\n");
            writer.write("Y" + (getMaxY() + 100.0) + "\n");
            writer.write("M30");

            writer.close();

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setDialogTitle("Сохранить G-код");
            fileChooser.setFileFilter(new FileNameExtensionFilter("NC-Studio code", "u00"));
            int userSelection = fileChooser.showSaveDialog(this);

            if (userSelection == JFileChooser.APPROVE_OPTION) {
                String filePath = fileChooser.getSelectedFile().getAbsolutePath();
                if (!filePath.toLowerCase().endsWith(".u00")) {
                    filePath += ".u00";
                }

                BufferedWriter gCodeWriter = new BufferedWriter(new FileWriter(filePath));
                gCodeWriter.write(readTempFile());
                gCodeWriter.close();

                JOptionPane.showMessageDialog(this, "G-код успешно записан в файл:\n" + filePath, "Успешно",
                        JOptionPane.INFORMATION_MESSAGE);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String readTempFile() throws IOException {
        StringBuilder content = new StringBuilder();
        java.io.FileReader fileReader = new java.io.FileReader("temp_java.txt");
        BufferedReader bufferedReader = new BufferedReader(fileReader);

        String line;
        while ((line = bufferedReader.readLine()) != null) {
            content.append(line).append("\n");
        }

        bufferedReader.close();
        return content.toString();
    }

    private String slotting(double x, double y1, double y2, double width, double depth, double mill, int feed,
            double stepDown) {
        String line = "'moving\n";
        line += "G1 Z-" + depth + " F" + feed + "\n";
        line += "Y" + (y2 - (mill / 2)) + "\n";
        line += "X" + (x + width - (mill / 2)) + "\n";
        line += "Y" + (y1 + (mill / 2)) + "\n";
        line += "X" + (x + (mill / 2)) + "\n";
        return line;
    }

    private String drilling(double depth, int feed, double stepDown) {
        String line = "'drilling\n";
        line += "G1 Z-" + depth + " F" + feed + "\n";
        return line;
    }

    private String milling(double x, String y, double width, double depth, double mill, int feed, double stepDown) {
        String line = "'milling\n";
        line += "G1 Z-" + depth + " F" + feed + "\n";
        line += "Y" + y + "\n"; // Not sure what to put here, as the original code is missing the implementation
                                // for the "circular" method
        return line;
    }

    private double getMaxY() {
        double maxY = 0.0;
        for (Row row : rows) {
            if (row.getMaxY() > maxY) {
                maxY = row.getMaxY();
            }
        }
        return maxY;
    }

    private class Row {
        private JTextField xField;
        private JTextField yField;
        private JTextField dField;
        private JTextField depthField;
        private boolean slot;
        private double maxY;

        public Row() {
            xField = new JTextField();
            yField = new JTextField();
            dField = new JTextField();
            depthField = new JTextField();
            slot = false;
            maxY = 0.0;
        }

        public JTextField getXField() {
            return xField;
        }

        public JTextField getYField() {
            return yField;
        }

        public JTextField getDField() {
            return dField;
        }

        public JTextField getDepthField() {
            return depthField;
        }

        public boolean isSlot() {
            return slot;
        }

        public double getMaxY() {
            return maxY;
        }

        public void setMaxY(double maxY) {
            this.maxY = maxY;
        }
    }

    public static void main(String[] args) {
        new GCodeGenerator();
    }

}