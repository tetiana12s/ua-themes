import javax.swing.*;
import javax.swing.SwingUtilities;
import java.awt.*;

public class Main {

    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("Робота Солтис Тетяни");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            JPanel toolbar = new JPanel();
            toolbar.setBackground(Color.lightGray);
            toolbar.add(new JButton("Зберегти"));
            toolbar.add(new JButton("Очистити"));

            // Створюємо наше полотно для малювання
            EmbroideryCanvas canvas  = new EmbroideryCanvas();

            frame.add(toolbar, BorderLayout.NORTH);
            frame.add(canvas, BorderLayout.CENTER);
            frame.setSize(1120, 825);
            frame.setLocationRelativeTo(null);  // Центрування вікна
            frame.setVisible(true);
        });
    }
}
