# Player documentation draft

Future copy for the behavior in the [specification](spec.md). Do not describe
this as available until implementation and verification are complete.

## English

### Throughput

Crafting Time shortens large throughput numbers in hover details: `~1.23M items/s`
means about 1.23 million items per second. The suffixes are k (thousand), M
(million), B (billion), T (trillion), P (quadrillion) and E (quintillion).
Even larger rates use scientific notation. Small positive rates below the
display precision show `<0.01` instead of zero.

These are learned rates for your AE2 network. Compact formatting changes how
they look, not how Crafting Time calculates them. Sample durations keep their
existing per-unit format, and AE2's own quantity lines keep their full values.

### Details and reset

Ctrl+click an eligible crafted row to publish its full throughput values to
server chat. The message includes rates per tick and per second without compact
suffixes or two-decimal rounding. Full values show the precision stored by the
profiler; they are still measurements, not exact promises about future crafts.
Everyone on the server can see this message. Existing chat settings and cooldown
still apply. Ctrl+Alt+click keeps its separate reset action.

### Configuration

**Options > Client > Displays > Compact hover numbers** starts on. Turn it off
to restore the older two-decimal throughput display. This setting is separate
from **Compact crafting amounts**. It does not change chat output. Choose Done
to save or Cancel to discard edits.

## Українською

### Пропускна здатність

Crafting Time скорочує великі значення пропускної здатності в підказках:
`~1.23M` означає приблизно 1,23 мільйона. Позначення k, M, B, T, P та E
відповідають тисячам, мільйонам, мільярдам, трильйонам, квадрильйонам і
квінтильйонам. Ще більші значення показуються в науковому записі. Додатні
значення, менші за точність відображення, показуються як `<0.01`, а не нуль.

Це швидкість, виміряна для вашої мережі AE2. Скорочення змінює лише вигляд
чисел, а не розрахунки. Тривалість зразків зберігає формат на одиницю ресурсу,
а власні рядки кількості AE2 показують повні значення.

### Подробиці та скидання

Натисніть Ctrl і клацніть відповідний рядок крафту, щоб опублікувати повні
значення пропускної здатності в чаті сервера. Повідомлення містить швидкість
за такт і за секунду без скорочень та округлення до двох десяткових знаків.
Це значення з точністю, яку зберігає профайлер, а не гарантія швидкості майбутніх
крафтів. Повідомлення бачать усі гравці сервера. Налаштування чату й обмеження
частоти повідомлень діють як раніше. Ctrl+Alt+клацання окремо скидає історію.

### Налаштування

Параметр **Скорочувати числа в підказках** у розділі налаштувань клієнта для
відображення типово ввімкнений. Вимкніть його, щоб повернути попередній формат
пропускної здатності з двома десятковими знаками. Він не залежить від компактного
показу кількості крафту та не змінює повідомлення в чаті. Підтвердьте зміни,
щоб зберегти їх, або скасуйте редагування.
