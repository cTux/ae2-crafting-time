# AE2 Crafting Time Guide Book Specification

Issue: [#144](https://github.com/cTux/ae2-crafting-time/issues/144)

Status: revised proposal, 2026-09-02; not shipped behavior. The current issue
predates the requested book and recipe. Its proposed replacement is below for
approval before updating GitHub.

## Goal and scope

Give players a craftable book that opens a short mod introduction and a useful
first chapter without leaving Minecraft.

This first delivery replaces the earlier seven-page completion requirement.
Detailed chapters on estimates, screens, diagnostics, controls/configuration,
integrations, and troubleshooting remain future scope. Do not create empty
pages or advertise those chapters as available.

## Book and recipe

- Name: **AE2 Crafting Time Guide**; Ukrainian: **Посібник AE2 Crafting Time**.
- Appearance: the ordinary Minecraft book model and texture, without a new
  cover, tint, or enchantment glint. Resource-pack changes to the vanilla book
  apply naturally. Vanilla books and other guides keep their own behavior.
- Shapeless recipe: one ordinary book + one uncharged Certus Quartz Crystal +
  one clock produces one guide book. Any arrangement works in both the 2×2
  inventory grid and a crafting table.
- Consume one of each input per craft. The clock is consumed, not returned.
  No extra ingredient, quartz dust, charged crystal, Nether Quartz, written
  book, or enchanted book substitutes for the specified inputs.
- Discover the recipe through the vanilla recipe book when acquiring an
  ordinary book. Manual crafting also works before discovery under vanilla's
  normal unrestricted-crafting rules.
- Using the held book opens this mod's guide. Support the main hand and the
  off hand when Minecraft routes the use action there. Repeated opening
  neither consumes nor damages the book.
- First opening reaches the introduction; later openings may follow
  GuideME's native reading-history behavior. Home/navigation reaches both pages.
- Correct guide identity survives moving, dropping, storing, saving, and
  reconnecting to a dedicated server.
- No automatic starter-book grant or new creative tab is required.

## Content ready for implementation

Exactly two pages: `index.md` and `getting-started.md`. Both are visible in
navigation and searchable, and each links to the other. Use the copy below;
the design defines frontmatter and links. Do not put dependency IDs or Java
details inside player pages.

### English introduction — AE2 Crafting Time

AE2 Crafting Time shows estimates for your AE2 autocrafting jobs and helps you
notice delays. It learns from the output your network actually produces, so
new crafts may not have an estimate yet. It does not make machines craft faster.

Open Crafting Plan before starting a job, or Crafting Status while it runs, to
see the available time estimates. TTC means “time to craft.” These times are
estimates, not deadlines: your machines, shared workload, and server performance
can change the result.

Start with **Chapter 1: Your first estimate**.

### English chapter — Chapter 1: Your first estimate

#### Make and open the guide

Combine a book, an uncharged Certus Quartz Crystal, and a clock in any order in
your crafting grid. You get this guide, and all three ingredients are used up.
Hold the guide and use it to open these pages. You can read it as often as you like.

#### Let your network learn

1. Use an AE2 network that can already autocraft an item. This mod observes
   your existing setup; the book does not configure a network for you.
2. Request a small craft through an AE2 crafting terminal. Before confirming,
   look at Crafting Plan. Missing estimates or “No data yet” mean there is no
   usable timing history for that output yet, not that the recipe is broken.
3. Start the job and let the network produce its output. Open Crafting Status
   to follow progress and the available estimates. Learning happens as real
   outputs arrive; you do not need to keep this book open.
4. Request the same output again. Once usable samples exist, its estimate uses
   your network's measured production speed. There is no fixed number of
   crafts that guarantees an accurate estimate.

#### Read the estimate

A row's time describes the requested crafting amount for that output, not
just one item. Early estimates can be uncertain, and shared machines or a
changed setup can affect the result. A delay warning is a reason to inspect
the craft, not proof that a particular machine is broken.

The mod learns on the server, including in singleplayer, and saves its timing
history with the world. Reading this guide does not start crafts, clear samples,
or change your network.

### Ukrainian introduction — AE2 Crafting Time

AE2 Crafting Time показує приблизний час автокрафтів AE2 і допомагає помічати
затримки. Мод навчається на результатах роботи вашої мережі, тому для нових
крафтів оцінки часу ще може не бути. Він не прискорює роботу машин.

Відкрийте план крафту перед запуском завдання або стан крафту під час його
виконання, щоб побачити доступні оцінки часу. TTC означає «час виготовлення».
Це приблизний прогноз, а не точний термін: результат залежить від машин,
спільного навантаження та швидкодії сервера.

Почніть із **розділу 1: Ваша перша оцінка часу**.

### Ukrainian chapter — Розділ 1: Ваша перша оцінка часу

#### Створіть і відкрийте посібник

Покладіть книгу, незаряджений кристал кварцу Цертус і годинник у сітку крафту
в будь-якому порядку. Ви отримаєте цей посібник, а всі три складники буде
витрачено. Візьміть посібник у руку й скористайтеся ним, щоб відкрити ці
сторінки. Читайте його скільки завгодно.

#### Дайте мережі навчитися

1. Скористайтеся мережею AE2, яка вже вміє автоматично виготовляти предмет.
   Мод спостерігає за наявною системою; книга не налаштовує мережу за вас.
2. Замовте невелику кількість предмета через термінал крафту AE2. Перед
   підтвердженням перегляньте план крафту. Якщо оцінки немає або показано,
   що даних ще немає, для цього результату поки немає придатної історії
   часу виготовлення. Це не означає, що рецепт зламаний.
3. Запустіть завдання й дочекайтеся, поки мережа почне отримувати результат.
   Відкрийте стан крафту, щоб стежити за поступом і доступними оцінками часу.
   Мод навчається, коли надходять виготовлені ресурси; тримати книгу
   відкритою не потрібно.
4. Замовте той самий результат ще раз. Коли з'являться придатні вимірювання,
   оцінка спиратиметься на фактичну швидкість вашої мережі. Немає фіксованої
   кількості крафтів, яка гарантує точний прогноз.

#### Як читати оцінку

Час у рядку стосується замовленої кількості цього ресурсу для виготовлення,
а не лише одного предмета. Перші оцінки можуть бути неточними, а спільні
машини чи зміни в системі можуть вплинути на результат. Попередження про
затримку — привід перевірити крафт, а не доказ несправності певної машини.

Мод навчається на сервері, зокрема й в одиночній грі, та зберігає історію
часу виготовлення разом зі світом. Читання посібника не запускає крафтів,
не очищує вимірювань і не змінює мережу.

## Compatibility and boundaries

| Release target | This delivery |
| --- | --- |
| 1.21.1 NeoForge | Book, recipe, introduction, chapter one; GuideME required on client and server |
| 26.1.2 NeoForge | Same player behavior; GuideME required on client and server |
| 1.20.1 Forge | Excluded, retaining issue #144's target scope |
| 1.20.1 Fabric | Excluded, retaining issue #144's target scope |

Standalone GuideME has 1.20.1 releases. This exclusion is a scope decision,
not a claim that a backport is impossible. Do not add a second implementation
using AE2's old internal guidebook.

English is the default; Ukrainian is complete; other languages use GuideME's
native fallback. Both modern targets work offline after installation.
Missing GuideME produces a loader dependency error, not a broken recipe.

No custom screen, key binding, Java item, packet, profiler change, config,
custom world data, web export, Ponder scene, or optional-mod integration.
Do not hijack AE2 item-hover guide targets in this first delivery.

## Acceptance criteria

| ID | Observable result |
| --- | --- |
| A1 | Both grids produce one correctly named guide from exactly the specified inputs in any position. Incorrect inputs fail; discovery and consumption work. |
| A2 | Inventory, held, and dropped result looks like a vanilla book. Ordinary books and AE2's own guide remain unchanged. |
| A3 | Main-hand and eligible off-hand use opens our guide repeatedly without consumption; save/reload and multiplayer preserve identity. |
| A4 | Both pages contain the supplied copy, with working reciprocal links, navigation, search, and no unfinished-chapter links. |
| A5 | Both languages have matching page paths and link/navigation structure, with translated text and names; both themes remain readable. |
| A6 | Both modern JARs contain the guide, target-correct recipe/unlock data, and required metadata; dedicated servers load and serve the recipe. |
| A7 | Neither old JAR contains a GuideME recipe, advancement, or required dependency; both 1.20.1 clients still start. |
| A8 | Resource and targeted game checks pass without new runtime Java or profiler changes. |

## Proposed issue update — exact text for approval

Title: `[Feature]: Add a craftable GuideME book and first chapter`

Body:

```markdown
## Goal

Add a craftable AE2 Crafting Time guide book with a short mod introduction and chapter one: Your first estimate.

## Behavior

- Shapeless: one ordinary book + one uncharged Certus Quartz Crystal + one clock = one guide book. Consume all three inputs; support inventory and crafting-table grids.
- Use the vanilla book appearance. Holding and using the book opens our guide without consuming it.
- Include complete English and Ukrainian introduction and chapter-one pages, with navigation, search, and links between them.
- Explain what the mod does, how to learn the first useful timing samples, and why TTC is an estimate.

## Technical scope

Reuse GuideME's generic book with our guide ID, title, and the vanilla book model. Use data-driven pages and version-specific vanilla recipes. Require GuideME on both sides for 1.21.1 NeoForge and 26.1.2 NeoForge. Keep 1.20.1 Forge/Fabric outside this delivery.

## Acceptance

- Correct and incorrect ingredients, both grids, discovery, and consumption behave as specified.
- Book appearance and opening work; identity survives storage, save/reload, and multiplayer.
- Both pages work in both languages and themes, with no broken navigation or links.
- Modern packages and dedicated servers load correctly; old targets gain no GuideME recipe or dependency.
- Resource validation and targeted game checks pass.

## Later work

The earlier full-guide outline remains future scope: detailed estimates, screens, diagnostics, controls/configuration, integrations, and troubleshooting. No empty chapters, custom UI, new Java item, or profiler changes in this delivery.

## Risks and planning

The oldest supported AE2 1.21.1 artifact does not require GuideME itself; declare it explicitly. Recipe ingredient formats differ between modern targets.

- docs/guideme-guide/spec.md
- docs/guideme-guide/technical-design.md
- docs/guideme-guide/implementation-plan.md
```

See [technical-design.md](technical-design.md) and
[implementation-plan.md](implementation-plan.md).
