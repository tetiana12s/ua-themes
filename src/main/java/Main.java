import javax.swing.*;
import javax.swing.SwingUtilities;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

/**
 * Головний інфраструктурний клас програми.
 * Відповідає за ініціалізацію головного вікна додатка (JFrame), побудову
 * верхньої панелі інструментів (JToolBar) та глобальну реєстрацію гарячих клавіш.
 * * @author Солтис Тетяна
 */
public class Main {

    /**
     * Точка входу в програму. Запускає інтерфейс Swing в асинхронному
     * безпечному потоці обробки подій.
     */
    public static void main(String[] args) {

        SwingUtilities.invokeLater(() -> {
            // Створення та базове налаштування головного контейнера (вікна)
            JFrame frame = new JFrame("Редактор орнаменту - Солтис Тетяни");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new BorderLayout());

            // Створюємо полотно для малювання
            EmbroideryCanvas canvas  = new EmbroideryCanvas();

            FileManager fileManager = new FileManager(canvas);
            ToolManager toolManager = new ToolManager(canvas);

            // --- НАЛАШТУВАННЯ ПАНЕЛІ ІНСТРУМЕНТІВ (JToolBar) ---
            JToolBar toolbar = new JToolBar();
            toolbar.setFloatable(false);
            toolbar.setFocusable(false);
            toolbar.setBackground(new Color(235, 237, 240));
            toolbar.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));

            // Кнопка Експорту в PNG
            JButton saveButton = new JButton("Зберегти");
            saveButton.setFocusable(false);
            saveButton.setToolTipText("Зберегти вишивку як зображення PNG (Ctrl + S)");
            saveButton.addActionListener(e -> fileManager.saveToPNG());
            toolbar.add(saveButton);

            // Кнопка Імпорту з PNG
            JButton openButton = new JButton("Відкрити");
            openButton.setFocusable(false);
            openButton.addActionListener(e -> {fileManager.openFromPNG();});
            toolbar.add(openButton);

            toolbar.addSeparator(new Dimension(15, 0));

            // Кнопка активації інструменту "Піпетка"
            JButton pickerButton = new JButton("Піпетка");
            pickerButton.setFocusable(false);
            pickerButton.setToolTipText("Узяти колір з полотна (Клавіша E");
            pickerButton.addActionListener(e -> {
                canvas.activeColorPickerMode();
                JOptionPane.showMessageDialog(frame, "Режим піпетки активовано! Клацніть на потрібний колір на полотні.");
            });
            toolbar.add(pickerButton);

            // Кнопка виклику колірної палітри
            JButton colorButton = new JButton("Палітра");
            colorButton.setFocusable(false);
            colorButton.setToolTipText("Обрати колір нитки (Клавіша P)");
            colorButton.addActionListener(e -> toolManager.chooseColor());
            toolbar.add(colorButton);

            // Кнопка активації інструменту "Заливка"
            JButton fillButton = new JButton("Заливка");
            fillButton.setFocusable(false);
            fillButton.setToolTipText("Залити суміжну область обраним кольором (Клавіша F");
            fillButton.addActionListener(e -> {
                canvas.activeBucketFillMode();
                JOptionPane.showMessageDialog(frame, "Режим заливки активовано! Клацніть на область полотна.");
            });
            toolbar.add(fillButton);

            // Кнопка виклику дублювання фрагмента
            JButton duplicateButton = new JButton("Дублювати");
            duplicateButton.setFocusable(false);
            duplicateButton.addActionListener(e -> toolManager.duplicateFragment());
            toolbar.add(duplicateButton);

            toolbar.addSeparator(new Dimension(15, 0));

            // Налаштування випадаючого списку системної геометричної симетрії
            JLabel symmetryLabel = new JLabel("Симетрія: ");
            symmetryLabel.setFont(new Font("Arial", Font.PLAIN, 12));
            toolbar.add(symmetryLabel);

            String[] symmetryOptions = {"Без симетрії", "По горизонталі", "По вертикалі", "Чотиристороння"};
            JComboBox<String> symmetryComboBox = new JComboBox<>(symmetryOptions);
            symmetryComboBox.setMaximumSize(new Dimension(150, 30));
            symmetryComboBox.setFocusable(false);

            symmetryComboBox.addActionListener(e -> {
                String celectedMode = (String) symmetryComboBox.getSelectedItem();
                canvas.setSymmetryMode(celectedMode);
            });
            toolbar.add(symmetryComboBox);

            // Пружина динамічного відступу компонентів Swing
            toolbar.add(Box.createHorizontalGlue());

            // Кнопка керування фоновою аудіодоріжкою
            JButton musicButton = new JButton("Музика");
            musicButton.setFocusable(false);
            musicButton.setToolTipText("Увімкнути або вимкнути музику (клавіша M)");
            musicButton.addActionListener(e -> canvas.toggleMusic());
            toolbar.add(musicButton);

            // Кнопка покрокового скасування дій малювання
            JButton undoButton = new JButton("Назад");
            undoButton.setFocusable(false);
            undoButton.setToolTipText("Скасувати останню дію (Ctrl + Z)");
            undoButton.addActionListener(e -> canvas.undo());
            toolbar.add(undoButton);

            // Кнопка повного очищення робочої сітки
            JButton clearButton = new JButton("Очистити");
            clearButton.setFocusable(false);
            clearButton.setToolTipText("Очистити полотно (Клавіша C)");
            clearButton.addActionListener(e -> canvas.clearCanvas());
            toolbar.add(clearButton);

            // --- ГЛОБАЛЬНА СИСТЕМА ГАРЯЧИХ КЛАВІШ ---
            JComponent rootPane = frame.getRootPane();

            // Ctrl + Z (Скасувати попередній крок)
            rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke
                    (java.awt.event.KeyEvent.VK_Z, java.awt.event.InputEvent.CTRL_DOWN_MASK), "undoAction");
            rootPane.getActionMap().put("undoAction", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    canvas.undo();
                }
            });

            // Ctrl + S (Зберегти орнамент)
            rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_DOWN_MASK), "saveAction");
            rootPane.getActionMap().put("saveAction", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    fileManager.saveToPNG();
                }
            });

            // Клавіша M (Увімкнути/вимкнути музику)
            rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .put(KeyStroke.getKeyStroke(KeyEvent.VK_M, 0), "musicAction");
            rootPane.getActionMap().put("musicAction", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    canvas.toggleMusic();
                }
            });

            // Клавіша C (Очистити полотно)
            rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, 0), "clearAction");
            rootPane.getActionMap().put("clearAction", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    int response = JOptionPane.showConfirmDialog(frame,
                            "Ви впевнені, що хочете повністю очистити полотно?",
                            "Підтвердження", JOptionPane.YES_NO_OPTION);
                        if (response == JOptionPane.YES_OPTION) {
                            canvas.clearCanvas();
                        }
                }
            });

            // Клавіша ESC (Вимкнути режим рамки при дублюванні)
            rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0), "cancelSelection");
            rootPane.getActionMap().put("cancelSelection", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    canvas.cancelActiveModes();
                    canvas.repaint();
                }
            });

            // Клавіша P (Увімкнути палітру)
            rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_P, 0), "paletteAction");
            rootPane.getActionMap().put("paletteAction", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    toolManager.chooseColor();
                }
            });

            // Клавіша F (Увімкнути заливку)
            rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F, 0), "fillAction");
            rootPane.getActionMap().put("fillAction", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    canvas.activeBucketFillMode();
                    JOptionPane.showMessageDialog(frame, "Режим заливки активовано! Клацніть на область полотна.");
                }
            });

            // Рух стрілками та малювання пробілом

            // Стрілочка Вгору
            rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_UP, 0), "moveUp");
            rootPane.getActionMap().put("moveUp", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) { canvas.moveKeyboardCursor(0, -1); }
            });

            // Стрілка Вниз
            rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_DOWN, 0), "moveDown");
            rootPane.getActionMap().put("moveDown", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) { canvas.moveKeyboardCursor(0, 1); }
            });

            // Стрілка Ліворуч
            rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_LEFT, 0), "moveLeft");
            rootPane.getActionMap().put("moveLeft", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) { canvas.moveKeyboardCursor(-1, 0); }
            });

            // Стрілка Праворуч
            rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_RIGHT, 0), "moveRight");
            rootPane.getActionMap().put("moveRight", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) { canvas.moveKeyboardCursor(1, 0); }
            });

            // Пробіл
            rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_SPACE, 0), "spaceDraw");
            rootPane.getActionMap().put("spaceDraw", new AbstractAction() {
                @Override
                public void actionPerformed(ActionEvent e) { canvas.drawStitchAtKeyboardCursor(); }
            });

            // Клавіша E (Увімкнути піпетку)
            rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                    .put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_E, 0), "pickerAction");
            rootPane.getActionMap().put("pickerAction", new AbstractAction() {
                @Override
                public void actionPerformed(java.awt.event.ActionEvent e) {
                    canvas.activeColorPickerMode();
                    JOptionPane.showMessageDialog(frame, "Режим піпетки активовано! Клікніть на потрібний колір на сітці.");
                }
            });

            // --- ВІЗУАЛІЗАЦІЯ ВІКНА---
            frame.add(toolbar, BorderLayout.NORTH);
            frame.add(canvas, BorderLayout.CENTER);
            frame.setSize(1120, 825);
            frame.setLocationRelativeTo(null);  // Центрування вікна
            frame.setVisible(true);
        });
    }
}
