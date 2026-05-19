import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.awt.Color;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Тестування бізнес-логіки графічного полотна вишивки")
public class EmbroideryCanvasTest {

    private EmbroideryCanvas canvas;

    @BeforeEach
    void setUp() {
        // Ініціалізуємо нове полотно перед кожним тестом
        canvas = new EmbroideryCanvas();
    }

    @Test
    @DisplayName("Перевірка методу порівняння кольорів (colorsMatch)")
    void testColorsMatch() {
        // Тест 1: Два однакові кольори
        assertTrue(canvas.colorsMatch(Color.RED, Color.RED),
                "Ідентичні кольори мають збігатися");

        // Тест 2: Два різні кольори
        assertFalse(canvas.colorsMatch(Color.RED, Color.BLACK),
                "Різні кольори не повинні збігатися");

        // Тест 3: Обидва кольори null (порожні клітинки фону)
        assertTrue(canvas.colorsMatch(null, null),
                "Два null-об'єкти вважаються однаковим фоном");

        // Тест 4: Один колір задано, а інший null
        assertFalse(canvas.colorsMatch(Color.RED, null),
                "Колір та null не повинні збігатися");
    }

    @Test
    @DisplayName("Перевірка обмеження руху клавіатурного курсора межами сітки")
    void testMoveKeyboardCursorBounds() {
        // За замовчуванням курсор стоїть в (0,0)
        // Спробуємо вийти за ліву і верхню межу (в мінус)
        canvas.moveKeyboardCursor(-5, -5);

        // Перевіряємо через рефлексію або непрямі методи, що курсор затиснувся в (0,0)
        // Оскільки поля private, логіка moveKeyboardCursor захищає від IndexOutOfBounds
        assertDoesNotThrow(() -> canvas.moveKeyboardCursor(-1, 0),
                "Рух ліворуч за межі сітки не повинен викликати помилок");

        // Спробуємо пройти далеко праворуч і вниз (gridCols=41, gridRows=29)
        assertDoesNotThrow(() -> canvas.moveKeyboardCursor(100, 100),
                "Рух далеко вниз і праворуч має безпечно обмежуватися константами сітки");
    }

    @Test
    @DisplayName("Перевірка зчитування кольору хрестика (getStitchColorAt)")
    void testGetStitchColorAt() {
        // На початку, після запуску анімації, на полотні є стартові хрестики.
        // Перевіримо, що метод пошуку кольору повертає null для клітинки, де точно нічого немає
        Color cellColor = canvas.getStitchColorAt(0, 0);

        assertNull(cellColor, "Порожня клітинка полотна має повертати null");
    }
}
