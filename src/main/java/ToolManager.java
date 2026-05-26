import javax.swing.*;
import java.awt.*;

public class ToolManager {

    private EmbroideryCanvas canvas;

    public ToolManager(EmbroideryCanvas canvas) {
        this.canvas = canvas;
    }

    public void chooseColor() {
        Color selectedColor = JColorChooser.showDialog(canvas, "Оберіть колір нитки", canvas.getCurrentColor());
        if (selectedColor != null) {
            canvas.setCurrentColor(selectedColor);
        }
    }

    public void duplicateFragment() {
        if (canvas.getDrawnStitches().isEmpty()) {
            JOptionPane.showMessageDialog(canvas, "Полотно порожнє! Немає що дублювати.",
                    "Повідомлення", JOptionPane.WARNING_MESSAGE);
            return;
        }

        JPanel inputPanel = new JPanel(new GridLayout(2, 2, 10, 10));

        JSpinner widthSpinner = new JSpinner(new SpinnerNumberModel(10, 1, canvas.getGridCols(), 1));
        JSpinner heightSpinner = new JSpinner(new SpinnerNumberModel(10, 1, canvas.getGridRows(), 1));

        inputPanel.add(new JLabel("Ширина фрагмента (клітинок):"));
        inputPanel.add(widthSpinner);
        inputPanel.add(new JLabel("Висота фрагмента (клітинок):"));
        inputPanel.add(heightSpinner);

        int result = JOptionPane.showConfirmDialog(canvas, inputPanel,
                "Задайте розміри фрагмента орнаменту", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            canvas.setSelectionWidth((int) widthSpinner.getValue());
            canvas.setSelectionHeight((int) heightSpinner.getValue());
            canvas.setSelectingArea(true);
            canvas.repaint();

            JOptionPane.showMessageDialog(canvas, "Тепер виберіть мишкою місце на полотні для дублювання і клікніть лівою кнопкою.");
        }
    }
}
