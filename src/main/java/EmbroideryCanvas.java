import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.io.File;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.AudioInputStream;

/**
 * Інтерактивне графічне полотно (JPanel) для проєктування схем української вишивки хрестиком.
 * Відповідає за рендеринг графіки (сітки, хрестиків), обробку кліків миші та
 * виконання базових алгоритмів малювання (симетрія, заливка, дублювання).
 */
public class EmbroideryCanvas extends JPanel {

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

    /**
     * Встановлює режим геометричної симетрії для малювання.
     * @param symmetryMode Назва режиму (напр. "По горизонталі", "Чотиристороння").
     */
    public void setSymmetryMode(String symmetryMode) {
        this.symmetryMode = symmetryMode;
    }


    /**
     * Керує відтворенням фонової музики.
     * Якщо музика грає — зупиняє, інакше — запускає по колу (LOOP_CONTINUOUSLY).
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

    /**
     * Розбирає текстову матрицю на об'єкти Stitch.
     * @param map Масив рядків, де 'R' - червоний, 'B' - чорний.
     */
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

    /**
     * Запускає таймер Swing для ефекту поступової появи хрестиків при старті програми.
     */
    private void startEmbroideryAnimation() {
        timer = new Timer(30, new ActionListener() {
            int currentIndex = 0;

            @Override
            public void actionPerformed(ActionEvent e) {
                if (currentIndex < allStitches.size()) {
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
     * Головний метод рендерингу компонента Swing.
     * Відмальовує фон, сітку, всі збережені хрестики та рамки інструментів.
     * @param g Графічний контекст (Graphics).
     */
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
        g2.drawRect(offsetX, offsetY, gridCols * CELL_SIZE, gridRows * CELL_SIZE);

        g2.setColor(Color.LIGHT_GRAY);
        g2.setStroke(new BasicStroke(1));
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
            int framePixelX = offsetX + currentMouseGridX * CELL_SIZE;
            int framePixelY = offsetY + currentMouseGridY * CELL_SIZE;

            g2.setColor(new Color(0, 0, 255, 64));
            g2.fillRect(framePixelX, framePixelY, selectionWidth * CELL_SIZE, selectionHeight * CELL_SIZE);

            g2.setColor(Color.BLUE);
            g2.setStroke(new BasicStroke(2));
            g2.drawRect(framePixelX, framePixelY, selectionWidth * CELL_SIZE, selectionHeight * CELL_SIZE);
        }

        if (showKeyboardCursor) {
            int cursorPixelX = offsetX + keyboardGridX * CELL_SIZE;
            int cursorPixelY = offsetY + keyboardGridY * CELL_SIZE;

            g2.setColor(Color.DARK_GRAY);
            g2.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER,
                    10.0f, new float[]{4.0f}, 0.0f));
            g2.drawRect(cursorPixelX, cursorPixelY, CELL_SIZE, CELL_SIZE);
        }
    }

    /**
     * Допоміжний метод для малювання двох перехресних ліній (хрестика).
     */
    private void drawCross(Graphics2D g2, int x, int y) {
        int padding = 4;
        g2.drawLine(x + padding, y + padding, x + CELL_SIZE - padding, y + CELL_SIZE - padding);
        g2.drawLine(x + CELL_SIZE - padding, y + padding, x + padding, y + CELL_SIZE - padding);
    }

    /**
     * Повністю очищає полотно від усіх графічних елементів та історії скасувань.
     */
    public void clearCanvas() {
        if (timer != null && timer.isRunning()) {
            timer.stop();
        }
        allStitches.clear();
        drawnStitches.clear();
        undoHistory.clear();
        repaint();
    }


    /**
     * Центральний диспетчер логіки кліків миші.
     * Розраховує координати клітинки та викликає відповідну дію залежно від активного інструменту.
     */
    private void handleMouseClick(MouseEvent e) {
        int offsetX = (getWidth() - (gridCols * CELL_SIZE)) / 2;
        int offsetY = (getHeight() - (gridRows * CELL_SIZE)) / 2;
        if (offsetX < 0) offsetX = 0;
        if (offsetY < 0) offsetY = 0;

        int col = (e.getX() - offsetX) / CELL_SIZE;
        int row = (e.getY() - offsetY) / CELL_SIZE;

        if (col >= 0 && col < gridCols && row >= 0 && row < gridRows) {

            if (isColorPickerMode && e.getButton() == MouseEvent.BUTTON1) {
                Color pickedColor = getStitchColorAt(col, row);
                if (pickedColor != null) {
                    currentColor = pickedColor;
                }
                isColorPickerMode = false;
                repaint();
                return;
            }

            if (isBucketFillMode && e.getButton() == MouseEvent.BUTTON1) {
                Color targetColor = getStitchColorAt(col, row);
                int countBefore = drawnStitches.size();
                floodFill(col, row, targetColor, currentColor);

                int added = Math.abs(drawnStitches.size() - countBefore);
                if (added > 0) undoHistory.add(added);

                isBucketFillMode = false;
                repaint();
                return;
            }

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
                        for (int offestX = 0; offestX < gridCols; offestX += stepX) {
                            for (Stitch s : baseFragment) {
                                int newX = s.gridX - currentMouseGridX + offestX;
                                int newY = s.gridY - currentMouseGridY + offestY;

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
                    isSelectingArea = false;
                    repaint();
                    return;
                }
            }

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
                if (added > 0) undoHistory.add(added);
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


    /**
     * Додає один хрестик у список відмальованих (видаляючи старий, якщо він там був).
     */
    private void addSingleStich(int col, int row, Color color) {
        drawnStitches.removeIf(stitch -> stitch.gridX == col && stitch.gridY == row);
        drawnStitches.add(new Stitch(col, row, color));
    }


    /**
     * Видаляє хрестик із заданих координат.
     */
    private void removeSingleStich(int col, int row) {
        drawnStitches.removeIf(stitch -> stitch.gridX == col && stitch.gridY == row);
    }

    /**
     * Метод скасування останньої дії (Ctrl+Z).
     * Використовує масив історії (undoHistory), щоб дізнатися, скільки хрестиків було додано за останній клік.
     */
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


    /**
     * Рухає віртуальний клавіатурний курсор по сітці.
     */
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

    /**
     * Створює хрестик у місці знаходження клавіатурного курсора (імітація кліку).
     */
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
        if (added > 0) undoHistory.add(added);
        repaint();
    }

    /**
     * Рекурсивний алгоритм Flood Fill (Заливка).
     * Заповнює всі суміжні клітинки однакового кольору новим кольором.
     * * @param col Поточна колонка
     * @param row Поточний рядок
     * @param targetColor Колір фону або хрестика, на який клікнули
     * @param replacementColor Новий колір нитки
     */
    public void floodFill(int col, int row, Color targetColor, Color replacementColor) {
        if (col < 0 || col >= gridCols || row < 0 || row >= gridRows) return;

        Color currentColorAtCell = getStitchColorAt(col, row);

        if (!colorsMatch(currentColorAtCell, targetColor) ||
                colorsMatch(currentColorAtCell, replacementColor)) return;

        if (replacementColor == null) {
            removeSingleStich(col, row);
        } else {
            addSingleStich(col, row, replacementColor);
        }

        floodFill(col + 1, row, targetColor, replacementColor);
        floodFill(col - 1, row, targetColor, replacementColor);
        floodFill(col, row + 1, targetColor, replacementColor);
        floodFill(col, row - 1, targetColor, replacementColor);
    }

    /**
     * Шукає колір хрестика за координатами сітки.
     * @return Колір хрестика або null, якщо клітинка порожня.
     */
    Color getStitchColorAt(int col, int row) {
        return drawnStitches.stream()
                .filter(s -> s.gridX == col && s.gridY == row)
                .map(s -> s.color)
                .findFirst().orElse(null);
    }

    /**
     * Перевіряє ідентичність двох кольорів (з урахуванням можливих null значень фону).
     */
    boolean colorsMatch(Color c1, Color c2) {
        if (c1 == null && c2 == null) return true;
        if (c1 == null || c2 == null) return false;
        return c1.getRGB() == c2.getRGB();
    }

    // --- Гетери та сетери ---
    public int getCellSize() { return CELL_SIZE; }
    public int getGridCols() { return gridCols; }
    public int getGridRows() { return gridRows; }
    public Timer getTimer() { return timer; }
    public List<Stitch> getAllStitches() { return allStitches; }
    public List<Stitch> getDrawnStitches() { return drawnStitches; }

    public Color getCurrentColor() { return currentColor; }
    public void setCurrentColor(Color color) { this.currentColor = color; }

    public void setSelectionWidth(int w) { this.selectionWidth = w; }
    public void setSelectionHeight(int h) { this.selectionHeight = h; }
    public void setSelectingArea(boolean b) { this.isSelectingArea = b; }
}