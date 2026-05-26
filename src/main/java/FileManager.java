import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

public class FileManager {
    private EmbroideryCanvas canvas; // Посилання на наше полотно

    public FileManager(EmbroideryCanvas canvas) {
        this.canvas = canvas;
    }

    /** Експортує поточне растрове зображення полотна Swing у графічний файл PNG. */
    public void saveToPNG() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Зберегти вишивку як PNG");

        FileNameExtensionFilter filter = new  FileNameExtensionFilter("Зображення PNG", "png");
        fileChooser.setFileFilter(filter);

        int userSelection = fileChooser.showSaveDialog(canvas);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();

            if (!fileToSave.getAbsolutePath().toLowerCase().endsWith(".png")) {
                fileToSave = new File(fileToSave.getAbsoluteFile() + ".png");
            }

            BufferedImage image = new BufferedImage(canvas.getWidth(), canvas.getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = image.createGraphics();

            canvas.paint(g2);  // Рендеринг полотна у буфер картинки
            g2.dispose();

            try {
                ImageIO.write(image, "png", fileToSave);
                JOptionPane.showMessageDialog(canvas, "Вишивку успішно збережено!",
                        "Успіх", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(canvas, "Помилка при збережені файлу: "
                        + ex.getMessage(), "Помилка", JOptionPane.ERROR_MESSAGE);
            }
        }
    }


    /** Імпортує схему вишивки з PNG за допомогою алгоритму реверс-інжинірингу пікселів. */
    public void openFromPNG() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Відкрити схему вишивки (PNG)");
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Зображення PNG", "png");
        fileChooser.setFileFilter(filter);

        int userSelection = fileChooser.showOpenDialog(canvas);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToOpen = fileChooser.getSelectedFile();

            try {
                BufferedImage image = ImageIO.read(fileToOpen);

                int canvasX = -1;
                int canvasY = -1;

                for (int y = 0; y < image.getHeight(); y++) {
                    for (int x = 0; x < image.getWidth(); x++) {
                        int rgb = image.getRGB(x, y) & 0x00FFFFFF;
                        if (rgb == 0xFFFFFF) {
                            canvasX = x;
                            canvasY = y;
                            break;
                        }
                    }
                    if (canvasX != -1) break;
                }

                if (canvasX == -1 || canvasY == -1) {
                    JOptionPane.showMessageDialog(canvas, "Не вдалося розпізнати полотно вишивки на цій картинці.",
                            "Помилка", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                int imgOffsetX = canvasX - 1;
                int imgOffsetY = canvasY - 1;

                if (canvas.getTimer() != null && canvas.getTimer().isRunning()) {
                    canvas.getTimer().stop();
                }

                canvas.getAllStitches().clear();
                canvas.getDrawnStitches().clear();

                for (int row = 0; row < canvas.getGridRows(); row++) {
                    for (int col = 0; col < canvas.getGridCols(); col++) {

                        int sampleX = imgOffsetX + col * canvas.getCellSize() + canvas.getCellSize() / 2;
                        int sampleY = imgOffsetY + row * canvas.getCellSize() + canvas.getCellSize() / 2;

                        if (sampleX < image.getWidth() && sampleY < image.getHeight()) {
                            Color pixelColor = new Color(image.getRGB(sampleX, sampleY));
                            if (pixelColor.getRed() < 245 || pixelColor.getGreen() < 245 || pixelColor.getBlue() < 245) {
                                canvas.getAllStitches().add(new Stitch(col, row, pixelColor));
                            }
                        }
                    }
                }
                canvas.getDrawnStitches().addAll(canvas.getAllStitches());
                canvas.repaint();

                JOptionPane.showMessageDialog(canvas, "Схему успішно завантажено на полотно!",
                        "Успіх", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(canvas, "Помилка при читанні файлу: "
                        + ex.getMessage(), "Помилка", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
