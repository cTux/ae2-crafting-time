# Wiki source: Data Energistics Trinity CPU

This is source text for the English and Ukrainian addon-support wiki pages.
Publication is pending the final #740 runtime and visual qualification.

## English

On NeoForge 1.21.1, AE2 Crafting Time has an optional adapter for Data
Energistics 3.3.5 Trinity workers. The separate **Data Energistics Trinity**
server option is on by default, and the global **Profiling** option also applies.
Turning either option off drops the current live estimate and pending timing;
turning it back on begins with a fresh job while keeping learned history.

The native Trinity Plan can show a learned TTC beside material rows. The normal
AE2 Status screen uses the active Trinity worker’s estimate. The coordinator
that accepts a request does not create a second sample. Other Data Energistics
machines and provider modes have separate coverage; see [#740](https://github.com/cTux/ae2-crafting-time/issues/740).

## Українська

На NeoForge 1.21.1 у AE2 Crafting Time є необов’язковий адаптер для робочих
CPU Trinity з Data Energistics 3.3.5. Окремий серверний параметр
**Data Energistics Trinity** увімкнено за замовчуванням; загальний параметр
**Profiling** теж діє на нього. Вимкнення скидає поточну оцінку та незавершені
вимірювання. Після повторного ввімкнення профілювання починається з нового
завдання, а вивчена історія зберігається.

Рідний екран плану Trinity може показувати вивчений TTC біля рядків матеріалів.
Звичайний екран Status AE2 використовує оцінку активного робочого CPU Trinity.
CPU, який лише приймає замовлення, не створює другий зразок. Інші машини й
режими Data Energistics мають окремі межі підтримки; дивіться
[#740](https://github.com/cTux/ae2-crafting-time/issues/740).
