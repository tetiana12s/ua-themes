import javax.swing.*;
import javax.swing.SwingUtilities;
import java.awt.*;

public class Main {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Робота Солтис Тетяни");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            // Створюємо полотно для малювання
            EmbroideryCanvas canvas  = new EmbroideryCanvas();

            JToolBar toolbar = new JToolBar();
            toolbar.setFloatable(false);
            toolbar.setBackground(new Color(235, 237, 240));
            toolbar.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

            JButton saveButton = new JButton("Зберегти");
            saveButton.addActionListener(e -> canvas.saveToPNG());
            toolbar.add(saveButton);

            JButton openButton = new JButton("Відкрити");
            openButton.addActionListener(e -> {canvas.openFromPNG();});
            toolbar.add(openButton);

            toolbar.addSeparator(new Dimension(15, 0));

            JButton colorButton = new JButton("Палітра");
            colorButton.addActionListener(e -> canvas.chooseColor());
            toolbar.add(colorButton);

            JButton duplicateButton = new JButton("Дублювати");
            duplicateButton.addActionListener(e -> canvas.duplicateFragment());
            toolbar.add(duplicateButton);

            toolbar.addSeparator(new Dimension(15, 0));

            JLabel symmetryLabel = new JLabel("Симетрія: ");
            symmetryLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            toolbar.add(symmetryLabel);

            String[] symmetryOptions = {"Без симетрії", "По горизонталі", "По вертикалі", "Чотиристороння"};
            JComboBox<String> symmetryComboBox = new JComboBox<>(symmetryOptions);
            symmetryComboBox.setMaximumSize(new Dimension(150, 30));

            symmetryComboBox.addActionListener(e -> {
                String celectedMode = (String) symmetryComboBox.getSelectedItem();
                canvas.setSymmetryMode(celectedMode);
            });
            toolbar.add(symmetryComboBox);

            toolbar.add(Box.createHorizontalGlue());

            JButton clearButton = new JButton("Очистити");
            clearButton.addActionListener(e -> canvas.clearCanvas());
            toolbar.add(clearButton);

            frame.add(toolbar, BorderLayout.NORTH);
            frame.add(canvas, BorderLayout.CENTER);
            frame.setSize(1120, 825);
            frame.setLocationRelativeTo(null);  // Центрування вікна
            frame.setVisible(true);
        });
    }
}
