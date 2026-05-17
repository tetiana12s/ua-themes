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

            JPanel toolbar = new JPanel();
            toolbar.setBackground(Color.lightGray);

            JButton saveButton = new JButton("Зберегти");
            saveButton.addActionListener(e -> canvas.saveToPNG());
            toolbar.add(saveButton);

            JButton openButton = new JButton("Відкрити");
            openButton.addActionListener(e -> {canvas.openFromPNG();});
            toolbar.add(openButton);

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
