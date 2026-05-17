import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Color;
import java.awt.BasicStroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

public class EmbroideryCanvas extends JPanel {

    private static class Stitch {
        int gridX, gridY;
        Color color;

        public Stitch(int gridX, int gridY, Color color) {
            this.gridX = gridX;
            this.gridY = gridY;
            this.color = color;
        }
    }

    private final int CELL_SIZE = 25;
    private List<Stitch> allStitches = new ArrayList<>();
    private List<Stitch> drawnStitches = new ArrayList<>();
    private Timer timer;

    public EmbroideryCanvas() {
        String[] patternMap = {
                "                                         ",
                "                                         ",
                "                                         ",
                "                    R                    ",
                "                   RRR                   ",
                "             RRR  RR RR  RRR             ",
                "             RR    BBB    RR             ",
                "             R R B BRB B R R             ",
                "                B R R R B                ",
                "               B RRRRRRR B               ",
                "             R  RR BBB RR  R             ",
                "            RBBB RB B BR BBBR            ",
                "           RR BRRRBB BBRR B RR           ",
                "            RBBB RB B BR BBBR            ",
                "              R RR BBB RR  R             ",
                "               B RRRRRRR B               ",
                "                B R R R B                ",
                "             R R B BRB B R R             ",
                "             RR    BBB    RR             ",
                "             RRR  RR RR  RRR             ",
                "                   RRR                   ",
                "                    R                    ",
        };

        parsePattern(patternMap);
        Collections.shuffle(allStitches);
        startEmbroideryAnimation();
    }

    private void parsePattern(String[] map) {
        for (int row = 0; row < map.length; row++) {
            String line = map[row];
            for (int col = 0; col < line.length(); col++) {
                char symbol = line.charAt(col);
                if (symbol == 'R') {
                    allStitches.add(new Stitch(col, row, Color.RED));
                } else if (symbol == 'B') {
                    allStitches.add(new Stitch(col, row, Color.BLACK));
                }
            }
        }
    }

    private void startEmbroideryAnimation() {
        timer = new Timer(30, new ActionListener() {
            int currentIndex = 0;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentIndex< allStitches.size()) {
                    drawnStitches.add(allStitches.get(currentIndex));
                    currentIndex++;
                    repaint();
                } else {
                    timer.stop();
                }
            }
        });
        timer.start();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        g2.setColor(new Color(240, 242, 245));
        g2.fillRect(0, 0, getWidth(), getHeight());

        int gridCols = 41;
        int gridRows = 30;

        int offsetX = (getWidth() - (gridCols * CELL_SIZE)) / 2;
        int offsetY = (getHeight() - (gridRows * CELL_SIZE)) / 2;

        if (offsetX < 0) offsetX = 0;
        if (offsetY < 0) offsetY = 0;

        g2.setColor(Color.WHITE);
        g2.fillRect(offsetX, offsetY, gridCols * CELL_SIZE, gridRows * CELL_SIZE);

        g2.setColor(new Color(215, 215, 215));
        g2.setStroke(new BasicStroke(1));
        g2.drawRect(offsetX, offsetY, gridCols * CELL_SIZE, gridRows * CELL_SIZE);

        g2.setColor(Color.LIGHT_GRAY);

        for (int i = 0; i <= gridRows; i++) {
            g2.drawLine(offsetX, offsetY + i * CELL_SIZE, offsetX + gridCols * CELL_SIZE, offsetY + i * CELL_SIZE);
        }

        for (int j = 0; j <= gridCols; j++) {
            g2.drawLine(offsetX + j * CELL_SIZE, offsetY, offsetX + j * CELL_SIZE, offsetY + gridRows * CELL_SIZE);
        }

        g2.setStroke(new BasicStroke(3));

        for (Stitch stitch : drawnStitches) {
            g2.setColor(stitch.color);
            drawCross(g2, stitch.gridX * CELL_SIZE + offsetX, stitch.gridY * CELL_SIZE + offsetY);
        }
    }

    private void drawCross(Graphics2D g2, int x, int y) {
        int padding = 4;
        g2.drawLine(x + padding, y + padding, x + CELL_SIZE - padding, y + CELL_SIZE - padding);
        g2.drawLine(x + CELL_SIZE - padding, y + padding, x + padding, y + CELL_SIZE - padding);
    }

    public void clearCanvas() {
        if (timer != null && timer.isRunning()) {
            timer.stop();
        }

        allStitches.clear();
        drawnStitches.clear();

        repaint();
    }

    public void saveToPNG() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Зберегти вишивку як PNG");

        FileNameExtensionFilter filter = new  FileNameExtensionFilter("Зображення PNG", "png");
        fileChooser.setFileFilter(filter);

        int userSelection = fileChooser.showSaveDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToSave = fileChooser.getSelectedFile();

            if (!fileToSave.getAbsolutePath().toLowerCase().endsWith(".png")) {
                fileToSave = new File(fileToSave.getAbsoluteFile() + ".png");
            }

            BufferedImage image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
            Graphics2D g2 = image.createGraphics();

            paint(g2);
            g2.dispose();

            try {
                ImageIO.write(image, "png", fileToSave);
                JOptionPane.showMessageDialog(this, "Вишивку успішно збережено!", "Успіх", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Помилка при збережені файлу: " + ex.getMessage(), "Помилка", JOptionPane.ERROR_MESSAGE);
            }
        }
    }


    public void openFromPNG() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Відкрити схему вишивки (PNG)");
        FileNameExtensionFilter filter = new FileNameExtensionFilter("Зображення PNG", "png");
        fileChooser.setFileFilter(filter);

        int userSelection = fileChooser.showOpenDialog(this);

        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File fileToOpen = fileChooser.getSelectedFile();

            try {
                BufferedImage image = ImageIO.read(fileToOpen);

                int gridCols = 41;
                int gridRows = 30;

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
                    JOptionPane.showMessageDialog(this, "Не вдалося розпізнати полотно вишивки на цій картинці.", "Помилка", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                int imgOffsetX = canvasX - 1;
                int imgOffsetY = canvasY - 1;

                if (timer != null && timer.isRunning()) {
                    timer.stop();
                }
                allStitches.clear();
                drawnStitches.clear();

                for (int row = 0; row < gridRows; row++) {
                    for (int col = 0; col < gridCols; col++) {

                        int sampleX = imgOffsetX + col * CELL_SIZE + CELL_SIZE / 2;
                        int sampleY = imgOffsetY + row * CELL_SIZE + CELL_SIZE / 2;

                        if (sampleX < image.getWidth() && sampleY < image.getHeight()) {
                            Color pixelColor = new Color(image.getRGB(sampleX, sampleY));

                            if (pixelColor.getRed() > 200 && pixelColor.getGreen() < 50 && pixelColor.getBlue() < 50) {
                                allStitches.add(new Stitch(col, row, Color.RED));
                            } else if (pixelColor.getRed() < 50 && pixelColor.getGreen() < 50 && pixelColor.getBlue() < 50) {
                                allStitches.add(new Stitch(col, row, Color.BLACK));
                            }
                        }
                    }
                }
                drawnStitches.addAll(allStitches);
                repaint();

                JOptionPane.showMessageDialog(this, "Схему успішно завантажено на полотно!", "Успіх", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Помилка при читанні файлу: " + ex.getMessage(), "Помилка", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
