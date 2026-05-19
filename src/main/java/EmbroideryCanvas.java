import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.imageio.ImageIO;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.AudioInputStream;

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
    private final int gridCols = 41;
    private final int gridRows = 29;
    private List<Stitch> allStitches = new ArrayList<>();
    private List<Stitch> drawnStitches = new ArrayList<>();
    private List<Integer> undoHistory = new ArrayList<>();
    private Timer timer;

    private Color currentColor = Color.RED;
    private String symmetryMode = "Без симетрії";

    public boolean isSelectingArea = false;    // Чи ми зараз у режимі вибору області для дублювання?
    private int selectionWidth = 0;     // Задана ширина фрагмента
    private int selectionHeight = 0;    // Задана висота фрагмента
    private int currentMouseGridX = 0;  // Поточна координата миші на сітці
    private int currentMouseGridY = 0;

    private Clip audioClip;

    public void toggleMusic() {
        if (audioClip != null && audioClip.isRunning()) {
            audioClip.stop();
            return;
        }

        if (audioClip != null) {
            audioClip.loop(Clip.LOOP_CONTINUOUSLY);
            audioClip.start();
            return;
        }
        try {
            File musicFile = new File("leleka.wav");
            if (!musicFile.exists()) {
                JOptionPane.showMessageDialog(this, "Файл leleka.wav не знайдено!",
                        "Помилка", JOptionPane.ERROR_MESSAGE);
                return;
            }
            AudioInputStream audioStream = AudioSystem.getAudioInputStream(musicFile);
            audioClip = AudioSystem.getClip();
            audioClip.open(audioStream);

            audioClip.loop(Clip.LOOP_CONTINUOUSLY);
            audioClip.start();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Помилка відтворення музики: " + e.getMessage(),
                    "Помилка", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void chooseColor() {
        Color selectedColor =  JColorChooser.showDialog(this, "Оберіть колір нитки", currentColor);
        if (selectedColor != null) {
            currentColor = selectedColor;
        }
    }

    public void setSymmetryMode(String symmetryMode) {
        this.symmetryMode = symmetryMode;
    }

    public EmbroideryCanvas() {
        String[] patternMap = {
                "                                         ",
                "                                         ",
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

        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleMouseClick(e); //Використовую Moise Pressed, а не MouseClicked, бо це візуально швидше
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                if (isSelectingArea) {
                    int offsetX = (getWidth() - (gridCols * CELL_SIZE)) / 2;
                    int offsetY = (getHeight() - (gridRows * CELL_SIZE)) / 2;
                    if (offsetX < 0) offsetX = 0;
                    if (offsetY < 0) offsetY = 0;

                    currentMouseGridX = (e.getX() - offsetX) /  CELL_SIZE;
                    currentMouseGridY = (e.getY() - offsetY) /  CELL_SIZE;

                    repaint();
                }
            }
        });
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

        if (isSelectingArea) {
            // Рахуємо піксельні координати верхнього лівого кута рамки
            int framePixelX = offsetX + currentMouseGridX * CELL_SIZE;
            int framePixelY = offsetY + currentMouseGridY * CELL_SIZE;

            g2.setColor(new Color(0, 0, 255, 64));  // Напівпрозорий синій колір
            g2.fillRect(framePixelX, framePixelY, selectionWidth *  CELL_SIZE, selectionHeight * CELL_SIZE);

            g2.setColor(Color.BLUE);
            g2.setStroke(new BasicStroke(2));
            g2.drawRect(framePixelX, framePixelY, selectionWidth * CELL_SIZE, selectionHeight * CELL_SIZE);
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
        undoHistory.clear();

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
                JOptionPane.showMessageDialog(this, "Вишивку успішно збережено!",
                        "Успіх", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Помилка при збережені файлу: "
                        + ex.getMessage(), "Помилка", JOptionPane.ERROR_MESSAGE);
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
                int gridRows = 29;

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
                    JOptionPane.showMessageDialog(this, "Не вдалося розпізнати полотно вишивки на цій картинці.",
                            "Помилка", JOptionPane.ERROR_MESSAGE);
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

                            if (pixelColor.getRed() < 245 || pixelColor.getGreen() < 245 || pixelColor.getBlue() < 245) {
                                allStitches.add(new Stitch(col, row, pixelColor));
                            }
                        }
                    }
                }
                drawnStitches.addAll(allStitches);
                repaint();

                JOptionPane.showMessageDialog(this, "Схему успішно завантажено на полотно!",
                        "Успіх", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Помилка при читанні файлу: "
                        + ex.getMessage(), "Помилка", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleMouseClick(MouseEvent e) {

        int offsetX = (getWidth() - (gridCols * CELL_SIZE)) / 2;
        int offsetY = (getHeight() - (gridRows * CELL_SIZE)) / 2;

        if (offsetX < 0) offsetX = 0;
        if (offsetY < 0) offsetY = 0;

        int col = (e.getX() - offsetX) / CELL_SIZE;
        int row = (e.getY() - offsetY) / CELL_SIZE;

        if (col >= 0 && col < gridCols && row >= 0 && row < gridRows) {

            // СТАН 1: РЕЖИМ ВИБОРУ ОБЛАСТІ ДЛЯ ДУБЛЮВАННЯ
            if (isSelectingArea) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    List<Stitch> baseFragment = new ArrayList<>();
                    for (Stitch s : allStitches) {
                        if (s.gridX >= currentMouseGridX && s.gridX < currentMouseGridX + selectionWidth &&
                            s.gridY >= currentMouseGridY && s.gridY < currentMouseGridY + selectionHeight) {
                            baseFragment.add(s);
                        }
                    }

                    if (baseFragment.isEmpty()) {
                        JOptionPane.showMessageDialog(this, "У вибраній зоні немає хрестиків для копіювання!",
                                "Помилка", JOptionPane.ERROR_MESSAGE);
                        return;
                    }

                    drawnStitches.clear();
                    undoHistory.clear();
                    int stepX = selectionWidth;
                    int stepY = selectionHeight;

                    for (int offestY = 0; offestY < gridRows; offestY += stepY) {
                        for (int  offestX = 0; offestX < gridCols; offestX += stepX) {

                            for (Stitch s : baseFragment) {
                                int newX = s.gridX - currentMouseGridX +  offestX;
                                int newY = s.gridY - currentMouseGridY +  offestY;

                                if (newX < gridCols && newY < gridRows) {
                                    addSingleStich(newX, newY, s.color);

                                    int mirroredNewY = (gridRows - 1) - newY;
                                    int mirroredNewX = (gridCols - 1) - newX;

                                    if (symmetryMode.equals("По горизонталі") || symmetryMode.equals("Чотиристороння")) {
                                        addSingleStich(newX, mirroredNewY, s.color);
                                    }
                                    if (symmetryMode.equals("По вертикалі") || symmetryMode.equals("Чотиристороння")) {
                                        addSingleStich(mirroredNewX, newY, s.color);
                                    }
                                    if (symmetryMode.equals("Чотиристороння")) {
                                        addSingleStich(mirroredNewX, mirroredNewY, s.color);
                                    }
                                }
                            }
                        }
                    }

                    isSelectingArea = false;
                    repaint();
                    JOptionPane.showMessageDialog(this, "Орнамент успішно продубльовано!");
                    return;
                } else if (e.getButton() == MouseEvent.BUTTON3) {
                    // Якщо правий клік - відміняємо вибір
                    isSelectingArea = false;
                    repaint();
                    return;
                }
            }

            // СТАН 2: РЕЖИМ СИМЕТРІЇ
            int mirroredRow = (gridRows - 1) - row;
            int mirroredCol = (gridCols - 1) - col;

            if (e.getButton() == MouseEvent.BUTTON1) {
                int countBefore = drawnStitches.size();

                addSingleStich(col, row, currentColor);

                if (symmetryMode.equals("По горизонталі") || symmetryMode.equals("Чотиристороння")) {
                    addSingleStich(col, mirroredRow, currentColor);
                }
                if (symmetryMode.equals("По вертикалі") || symmetryMode.equals("Чотиристороння")) {
                    addSingleStich(mirroredCol, row, currentColor);
                }
                if (symmetryMode.equals("Чотиристороння")) {
                    addSingleStich(mirroredCol, mirroredRow, currentColor);
                }
                int added = drawnStitches.size() - countBefore;
                if (added > 0) {
                    undoHistory.add(added);
                }
            } else if (e.getButton() == MouseEvent.BUTTON3) {
                removeSingleStich(col, row);

                if (symmetryMode.equals("По горизонталі") || symmetryMode.equals("Чотиристороння")) {
                    removeSingleStich(col, mirroredRow);
                }
                if (symmetryMode.equals("По вертикалі") || symmetryMode.equals("Чотиристороння")) {
                    removeSingleStich(mirroredCol, row);
                }
                if (symmetryMode.equals("Чотиристороння")) {
                    removeSingleStich(mirroredCol, mirroredRow);
                }
            }
            repaint();
        }
    }

    private void addSingleStich(int col, int row, Color color) {
        drawnStitches.removeIf(stitch -> stitch.gridX == col && stitch.gridY == row);
        drawnStitches.add(new Stitch(col, row, color));
    }

    private void removeSingleStich(int col, int row) {
        drawnStitches.removeIf(stitch -> stitch.gridX == col && stitch.gridY == row);
    }

    public void duplicateFragment() {
        if (drawnStitches.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Полотно порожнє! Немає що дублювати.",
                    "Повідомлення", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 10, 10));

        JSpinner widthSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 41, 1));
        JSpinner heightSpinner = new JSpinner(new SpinnerNumberModel(10, 1, 29, 1));

        inputPanel.add(new JLabel("Ширина фрагмента (клітинок):"));
        inputPanel.add(widthSpinner);
        inputPanel.add(new JLabel("Висота фргмента (клітинок):"));
        inputPanel.add(heightSpinner);

        int result = JOptionPane.showConfirmDialog(this, inputPanel,
                "Задайте розміри фрагмента орнаменту", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            selectionWidth = (int) widthSpinner.getValue();
            selectionHeight = (int) heightSpinner.getValue();

            isSelectingArea = true;
            repaint();

            JOptionPane.showMessageDialog(this, "Тепер виберіть мишкою місце на полотні для дублювання і клікніть лівою кнопкою.");
        }
    }

    public void undo() {
        if (undoHistory.isEmpty() || drawnStitches.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Немає дій для скасування!",
                    "Помилка", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int lastActionCount = undoHistory.remove(undoHistory.size() - 1);

        for (int i = 0; i < lastActionCount; i++) {
            if (!drawnStitches.isEmpty()) {
                drawnStitches.remove(drawnStitches.size() - 1);
            }
        }
        repaint();
    }
}
