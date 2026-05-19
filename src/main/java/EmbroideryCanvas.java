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

/**
 * Інтерактивне графічне полотно (JPanel) для проєктування схем української вишивки хрестиком.
 * Забезпечує рендеринг сітки, двокнопкове малювання мишею, симетрію,
 * а також роботу інструментів дублювання, заливки та піпетки.
 */
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

    // --- Геометричні константи полотна ---
    private final int CELL_SIZE = 25;
    private final int gridCols = 41;
    private final int gridRows = 29;

    // --- Сховища даних (Об'єктний граф вишивки) ---
    private List<Stitch> allStitches = new ArrayList<>();
    private List<Stitch> drawnStitches = new ArrayList<>();
    private List<Integer> undoHistory = new ArrayList<>();
    private Timer timer;

    // --- Системні параметри малювання ---
    private Color currentColor = Color.RED;
    private String symmetryMode = "Без симетрії";

    // --- Прапорці станів інструментів ---
    private boolean isSelectingArea = false;    // Чи ми зараз у режимі вибору області для дублювання?
    private boolean isBucketFillMode = false;    // Чи активний режим заливки
    private boolean isColorPickerMode = false;
    private boolean showKeyboardCursor = false;

    private int selectionWidth = 0;     // Задана ширина фрагмента
    private int selectionHeight = 0;    // Задана висота фрагмента
    private int currentMouseGridX = 0;  // Поточна координата миші на сітці
    private int currentMouseGridY = 0;

    // --- Параметри графічного курсора клавіатури ---
    private int keyboardGridX = 0;
    private int keyboardGridY = 0;

    private Clip audioClip;

    /**
     * Конструктор полотна. Ініціалізує початкову схему, запускає стартову
     * анімацію та реєструє низькорівневі слухачі подій миші.
     */
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
        Collections.shuffle(allStitches);   // Перемішування елементів для ефекту "живого" вишивання
        startEmbroideryAnimation();

        // Подієво-орієнтований слухач кліків миші
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                handleMouseClick(e); //Використовую Moise Pressed, а не MouseClicked, бо це візуально швидше
            }
        });

        // Слухач руху миші для плавного відтворення синьої рамки вибору фрагмента
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

    /** Вмикає режим заливки суміжних областей та скидає інші режими. */
    public void activeBucketFillMode() {
        this.isBucketFillMode = true;
        this.isColorPickerMode = false;
        this.isSelectingArea = false;
    }

    /** Вмикає режим піпетки та скидає інші режими. */
    public void activeColorPickerMode() {
        this.isColorPickerMode = true;
        this.isSelectingArea = false;
        this.isBucketFillMode = false;
    }

    /** Скасовує дію поточних активних режимів вибору, заливки чи піпетки. */
    public void cancelActiveModes() {
        this.isSelectingArea = false;
        this.isBucketFillMode = false;
        this.isColorPickerMode = false;
        repaint();
    }

    public boolean isSelectingArea() {
        return isSelectingArea;
    }

    public void setSymmetryMode(String symmetryMode) {
        this.symmetryMode = symmetryMode;
    }

    // ==========================================
    //         ФУНКЦІОНАЛЬНІ МЕТОДИ СИСТЕМИ
    // ==========================================

    /**
     * Перемикає стан відтворення музики leleka.wav.
     */
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

    /** Розгортає діалогове вікно JColorChooser для вибору поточного кольору нитки. */
    public void chooseColor() {
        Color selectedColor =  JColorChooser.showDialog(this, "Оберіть колір нитки", currentColor);
        if (selectedColor != null) {
            currentColor = selectedColor;
        }
    }

    /** Парсить текстову матрицю символів у об'єкти типу Stitch. */
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

    /** Запускає асинхронний таймер для поступового відтворення початкової схеми. */
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

    /**
     * Графічний цикл відтворення Swing.
     * Відповідає за малювання фону, білої тканини, ліній сітки, хрестиків та сервісних рамок.
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        g2.setColor(new Color(240, 242, 245));
        g2.fillRect(0, 0, getWidth(), getHeight());

        // Розрахунок центрованого розташування білої тканини на екрані
        int offsetX = (getWidth() - (gridCols * CELL_SIZE)) / 2;
        int offsetY = (getHeight() - (gridRows * CELL_SIZE)) / 2;
        if (offsetX < 0) offsetX = 0;
        if (offsetY < 0) offsetY = 0;

        g2.setColor(Color.WHITE);
        g2.drawRect(offsetX, offsetY, gridCols * CELL_SIZE, gridRows * CELL_SIZE);

        // Малювання сітки клітинок полотна
        g2.setColor(Color.LIGHT_GRAY);
        g2.setStroke(new BasicStroke(1));
        for (int i = 0; i <= gridRows; i++) {
            g2.drawLine(offsetX, offsetY + i * CELL_SIZE, offsetX + gridCols * CELL_SIZE, offsetY + i * CELL_SIZE);
        }

        for (int j = 0; j <= gridCols; j++) {
            g2.drawLine(offsetX + j * CELL_SIZE, offsetY, offsetX + j * CELL_SIZE, offsetY + gridRows * CELL_SIZE);
        }

        // Рендеринг об'єктного графа нанесених хрестиків
        g2.setStroke(new BasicStroke(3));
        for (Stitch stitch : drawnStitches) {
            g2.setColor(stitch.color);
            drawCross(g2, stitch.gridX * CELL_SIZE + offsetX, stitch.gridY * CELL_SIZE + offsetY);
        }

        // Рендеринг напівпрозорої синьої рамки у режимі дублювання фрагмента орнаменту
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

        // Рендеринг темного пунктирного курсора клавіатури
        if (showKeyboardCursor) {
            int cursorPixelX = offsetX + keyboardGridX * CELL_SIZE;
            int cursorPixelY = offsetY + keyboardGridY * CELL_SIZE;

            g2.setColor(Color.DARK_GRAY);
            g2.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10.0f, new float[]{4.0f}, 0.0f));
            g2.drawRect(cursorPixelX, cursorPixelY, CELL_SIZE, CELL_SIZE);
        }
    }

    /** Допоміжний метод для нанесення графічних ліній хрестика в межах клітинки. */
    private void drawCross(Graphics2D g2, int x, int y) {
        int padding = 4;
        g2.drawLine(x + padding, y + padding, x + CELL_SIZE - padding, y + CELL_SIZE - padding);
        g2.drawLine(x + CELL_SIZE - padding, y + padding, x + padding, y + CELL_SIZE - padding);
    }

    /** Повністю очищає полотно від усіх графічних об'єктів та скидає буфери. */
    public void clearCanvas() {
        if (timer != null && timer.isRunning()) {
            timer.stop();
        }

        allStitches.clear();
        drawnStitches.clear();
        undoHistory.clear();

        repaint();
    }

    /** Експортує поточне растрове зображення полотна Swing у графічний файл PNG. */
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

            paint(g2);  // Рендеринг полотна у буфер картинки
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


    /** Імпортує схему вишивки з PNG за допомогою алгоритму реверс-інжинірингу пікселів. */
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

                // Пошук лівого верхнього кута білого полотна на картинці
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

                // Матричний покроковий аналіз кольорів у центрі клітинок
                for (int row = 0; row < gridRows; row++) {
                    for (int col = 0; col < gridCols; col++) {

                        int sampleX = imgOffsetX + col * CELL_SIZE + CELL_SIZE / 2;
                        int sampleY = imgOffsetY + row * CELL_SIZE + CELL_SIZE / 2;

                        if (sampleX < image.getWidth() && sampleY < image.getHeight()) {
                            Color pixelColor = new Color(image.getRGB(sampleX, sampleY));
                            // Якщо колір відмінний від білого фону сітки — створюємо об'єкт хрестика
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

    /**
     * Центральний диспетчер подій кліків миші. Керує активацією інструментів:
     * заливки, піпетки, вибору області, а також звичайним двокнопковим малюванням з симетрією.
     */
    private void handleMouseClick(MouseEvent e) {

        int offsetX = (getWidth() - (gridCols * CELL_SIZE)) / 2;
        int offsetY = (getHeight() - (gridRows * CELL_SIZE)) / 2;

        if (offsetX < 0) offsetX = 0;
        if (offsetY < 0) offsetY = 0;

        int col = (e.getX() - offsetX) / CELL_SIZE;
        int row = (e.getY() - offsetY) / CELL_SIZE;

        if (col >= 0 && col < gridCols && row >= 0 && row < gridRows) {

            // --- РЕЖИМ ПІПЕТКИ (ВЗЯТТЯ КОЛЬОРУ З СІТКИ) ---
            if (isColorPickerMode && e.getButton() == MouseEvent.BUTTON1) {
                Color pickedColor = getStitchColorAt(col, row);

                if(pickedColor != null) {
                    currentColor = pickedColor;
                }
                isColorPickerMode = false;
                repaint();
                return;
            }

            // --- РЕЖИМ ЗАЛИВКИ FLOOD FILL ---
            if (isBucketFillMode && e.getButton() == MouseEvent.BUTTON1) {
                Color targetColor = getStitchColorAt(col, row);

                // Фіксуємо кількість хрестиків до заливки для історії скасування (Undo)
                int countBefore = drawnStitches.size();
                floodFill(col, row, targetColor, currentColor);

                int added = Math.abs(drawnStitches.size() - countBefore);
                if  (added > 0) {
                    undoHistory.add(added);
                }

                isBucketFillMode = false;
                repaint();
                return;
            }

            // --- РЕЖИМ АВТОМАТИЧНОГО ДУБЛЮВАННЯ ФРАГМЕНТА ---
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

            // --- ЗВИЧАЙНИЙ РЕЖИМ МАЛЮВАННЯ З СИМЕТРІЄЮ ---
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

    /** Безпечно додає один хрестик на полотно, примусово видаляючи старий об'єкт з цих координат. */
    private void addSingleStich(int col, int row, Color color) {
        drawnStitches.removeIf(stitch -> stitch.gridX == col && stitch.gridY == row);
        drawnStitches.add(new Stitch(col, row, color));
    }

    /** Видаляє об'єкт хрестика з вказаних координат. */
    private void removeSingleStich(int col, int row) {
        drawnStitches.removeIf(stitch -> stitch.gridX == col && stitch.gridY == row);
    }

    /** Ініціалізує лічильники JSpinner та переводить додаток у режим вибору рамки копіювання. */
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

    /** Менеджер скасування дій. Видаляє з кінця списку кількість елементів останнього кроку. */
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

    /** Переміщує графічний курсор клавіатури, затискаючи його в межах робочої матриці. */
    public void moveKeyboardCursor(int dx, int dy) {
        showKeyboardCursor = true;

        keyboardGridX += dx;
        keyboardGridY += dy;

        if (keyboardGridX < 0) keyboardGridX = 0;
        if (keyboardGridX >= gridCols) keyboardGridX = gridCols - 1;
        if (keyboardGridY < 0) keyboardGridY = 0;
        if (keyboardGridY >= gridRows) keyboardGridY = gridRows - 1;

        repaint();
    }

    /** Імітує нанесення хрестика у позиції клавіатурного курсора з урахуванням активної симетрії. */
    public void drawStitchAtKeyboardCursor() {
        if (!showKeyboardCursor) return;

        int countBefore = drawnStitches.size();

        int mirroredRow = (gridRows - 1) - keyboardGridY;
        int mirroredCol = (gridCols - 1) - keyboardGridX;

        addSingleStich(keyboardGridX, keyboardGridY, currentColor);

        if (symmetryMode.equals("По горизонталі") || symmetryMode.equals("Чотиристороння")) {
            addSingleStich(keyboardGridX, mirroredRow, currentColor);
        }
        if (symmetryMode.equals("По вертикалі") || symmetryMode.equals("Чотиристороння")) {
            addSingleStich(mirroredCol, keyboardGridY, currentColor);
        }
        if (symmetryMode.equals("Чотиристороння")) {
            addSingleStich(mirroredCol, mirroredRow, currentColor);
        }

        int added = drawnStitches.size() - countBefore;
        if (added > 0) {
            undoHistory.add(added);
        }
        repaint();
    }

    /**
     * Рекурсивний алгоритм глибинного пошуку для автоматичної
     * заливки колірних областей на полотні.
     */
    public void floodFill(int col, int row, Color targetColor, Color replacementColor) {
        // Перевірка меж сітки
        if (col < 0 || col >= gridCols || row < 0 || row >= gridRows) return;

        Color currentColorAtCell = getStitchColorAt(col,row);

        if (!colorsMatch(currentColorAtCell, targetColor) ||
                colorsMatch(currentColorAtCell, replacementColor)) return;

        if (replacementColor == null) {
            removeSingleStich(col, row);
        } else {
            addSingleStich(col, row, replacementColor);
        }

        // Рекурсивно викликаємо для 4-х сусідніх клітинок
        floodFill(col + 1, row, targetColor, replacementColor);
        floodFill(col - 1, row, targetColor, replacementColor);
        floodFill(col, row + 1, targetColor, replacementColor);
        floodFill(col, row - 1, targetColor, replacementColor);
    }

    /** Допоміжний метод лінійного пошуку об'єктивного кольору хрестика в координатах сітки. */
    private Color getStitchColorAt(int col, int row) {
        return drawnStitches.stream()
                .filter(s -> s.gridX == col && s.gridY == row)
                .map(s -> s.color)
                .findFirst().orElse(null);
    }

    /** Порівнює колірні структури з урахуванням потенційних null-об'єктів фону. */
    private boolean colorsMatch(Color c1, Color c2) {
        if (c1 == null && c2 == null) return true;
        if (c1 == null || c2 == null) return false;
        return c1.getRGB() == c2.getRGB();
    }
}
