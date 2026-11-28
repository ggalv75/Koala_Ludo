# לודו קואלה – איטרציה 2

גרסת איטרציה 2 של משחק ה-Ludo של קבוצת Koala: משחק מלא ל-2–4 שחקנים ברמה הקלה וברמה הבינונית, כולל קוביית Boost, משבצות שאלה והפתעה, לפי סעיף 5.2.1 ב-SDP.

## הרצה

- נדרשת Java 17 ומעלה.
- ה-JAR לא נשמר בגיט. בונים אותו ב-Eclipse: File → Export → Java → Runnable JAR file, עם Launch configuration של `LudoApplication`, ושומרים בשם `LudoKoala.jar`.
- הרצה ב-Windows: לחיצה כפולה על `LudoKoala.jar`, או `java -jar LudoKoala.jar` משורת הפקודה.
- המשחק לא משתמש בקבצים חיצוניים או בנתיבים מקומיים, ולכן רץ מכל תיקייה (NFR-07). מאגר השאלות `questions.json` נמצא בתיקייה `src` ונארז בתוך ה-JAR, והוא נטען מה-classpath.

## Eclipse

1. File → Import → General → Existing Projects into Workspace, ולבחור את התיקייה.
2. להריץ את `Controller.LudoApplication` כ-Java Application.

בדיקות: לחיצה ימנית על התיקייה `test` ← Run As ← JUnit Test (JUnit 5).

הקובץ `.settings/org.eclipse.core.resources.prefs` מגדיר קידוד UTF-8. לא למחוק אותו ולא להכניס אותו ל-`.gitignore`, אחרת הטקסט בעברית ייראה משובש ב-Eclipse על Windows.

## מה כלול

| סיפור משתמש (SDP 5.2.1) | דרישות SRS |
|---|---|
| **איטרציה 1** | |
| 1 – הגדרת והתחלת משחק | FR-01 עד FR-06 |
| 2 – לוח המשחק (רמה קלה) | FR-07 עד FR-11, FR-14 |
| 3 – קובייה ותורות | FR-15 עד FR-20 |
| 4 – תנועת כלים | FR-23 עד FR-27, FR-31 |
| 5 – אכילה והגנה | FR-28, FR-29 |
| 6 – חסימות Double Block | FR-30 עד FR-32 |
| 7 – סיום, דירוג ושעון | FR-45 עד FR-49 |
| **איטרציה 2** | |
| 8 – לוח הרמה הבינונית ומשבצות מיוחדות | FR-07 עד FR-14 |
| 8, 10 – קוביות Boost/UltraBoost ותורות | FR-15 עד FR-22 |
| 9 – מנגנון השאלות | FR-33 עד FR-37 |
| 10 – מנגנון ההפתעות | FR-38 עד FR-44 |

לא כלול עדיין, לפי ה-SDP (איטרציה 3): הרמה הקשה, היסטוריית משחקים ב-CSV ואשף ניהול השאלות. הם מופיעים במסכים כלא פעילים ("בקרוב").

## ארכיטקטורה

- **Model** – כל חוקי המשחק והנתונים. לא תלוי ב-Swing או ב-AWT.
- **View** – המסכים, הלוח והחלונות הקופצים. אין בו לוגיקת משחק.
- **Controller** – מקבל את פעולות המשתמש, מפעיל את המודל ומחליף מסכים.

תבניות עיצוב:
ארבע התבניות של איטרציה 2 (מטלה 5 סעיף 2, תבנית אחת לכל סטודנט) מפורטות בקובץ תבניות העיצוב של הקבוצה:
- **Factory Method** (עומר) – `BoardFactory` היא ה-Creator עם המתודה המופשטת `createLayout()`, ו-`EasyBoardFactory` ו-`MediumBoardFactory` מחזירות את `EasyBoardLayout` או `MediumBoardLayout`. תהליך הבנייה המשותף (`build`) כתוב פעם אחת, כולל פיזור משבצות ההגנה וההפתעה (FR-07 עד FR-14).
- **Observer** (דיאנה) – `GameListener`: מסך המשחק והבקר נרשמים ל-`GameEngine` ומתעדכנים אחרי כל שינוי, כך שהמודל לא מכיר את התצוגה. באיטרציה 2 נוספו האירועים `onQuestionRequested` ו-`onQuestionAnswered` (הבקר פותח את `QuestionDialog`) ו-`onSurpriseDrawn`.
- **Strategy** (דור) – `SurpriseStrategy`: לכל אחת משש ההפתעות (`SurpriseType`) יש מחלקה משלה ב-`SurpriseStrategies`, והמנוע בוחר אותה לפי ההגרלה של `SurpriseSelector` (FR-38 עד FR-44), בלי `switch` על סוג ההפתעה.
- **Template Method** (גל) – `DiceThrow`, מפורט בהמשך.

תבניות נוספות בקוד:
- **Facade** – `GameEngine`: נקודת כניסה אחת לחוקים (`rollDice`, `movePiece`), והבקר לא צריך להכיר את מחלקות החוקים.
- **Facade** – `SoundManager`: המסכים קוראים רק ל-`play(...)` ולמתגי המוזיקה והצלילים, ולא מכירים את Java Sound.
- **Null Object** – `GameSounds.silent()`: מימוש שקט של אותו ממשק. משמש בבדיקות ובכל מקום שאין בו צורך בסאונד, כך שהקוד לא בודק `null`.
- **Template Method** (גל) – `DiceThrow`: כל הטלה עוברת אותו רצף, שמוגדר פעם אחת במתודה `final perform()`: קובייה רגילה, קוביית מכפיל, חישוב צעדים (FR-16, FR-17) וחוק ה-6 (FR-19, FR-20). תתי-המחלקות משנות רק את השלבים שמשתנים:
  - `StepDieThrow` – רמה קלה, בלי קוביית מכפיל.
  - `MultiplierDieThrow` – Boost ברמה הבינונית, UltraBoost ברמה הקשה.
  - `SurpriseUltraBoostThrow` – ההטלה המיוחדת מההפתעה (FR-21). דורסת את ה-hook `countsTowardSixStreak()`, כך שההטלה לא נספרת ברצף ה-6 ולא מתבטלת.

## מוזיקה וצלילים

- מוזיקת רקע בלופ, וצלילים להטלת קובייה, לכל משבצת שהכלי עובר ולסיום המשחק.
- כפתור "מוזיקה" במסך הפתיחה ובמסך המשחק, וכפתור "צלילים" במסך המשחק. ההגדרה משותפת לכל המסכים בזמן ההרצה.
- כל הצלילים נוצרים בקוד (`SoundSynthesizer`), בלי קבצי שמע: אין נתיבים חיצוניים (NFR-07) ואין זכויות יוצרים על הקלטות.
- ההשמעה רצה ב-thread נפרד ולא מקפיאה את הממשק. במחשב בלי התקן שמע המשחק פשוט רץ בשקט (NFR-01).
- הסאונד שייך לשכבת ה-View: המודל לא יודע עליו. הבקר מעביר את `GameSounds` למסכים, והבדיקות משתמשות במימוש השקט כדי לא להשמיע מוזיקה בזמן הרצת JUnit.

כל הפרמטרים שמשתנים בין רמות נמצאים ב-`Difficulty`, והחוקים המשותפים לכל הרמות נמצאים ב-`GameRules` (NFR-05). גם קוביית המכפיל של כל רמה מוגדרת שם (`multiplierDie`): בקלה `NONE`, בבינונית `BOOST`, בקשה `ULTRA_BOOST`.

## איטרציה 2 – פירוט לפי חלקים

### קוביות ותורות (גל)

| דרישה | איפה בקוד |
|---|---|
| FR-15 – קוביות Boost ו-UltraBoost | `MultiplierDie`, `Dice.rollMultiplier` |
| FR-16, FR-17 – חישוב צעדים: מכפלה במסלול, קובייה רגילה בלבד במסלול הבית | `DiceRoll.trackSteps/homePathSteps`, `MoveValidator.stepsFor` |
| FR-19, FR-20 – חוק ה-6 לפי הקובייה הרגילה בלבד | `DiceRoll.isSix`, `TurnManager.afterRoll` |
| FR-21, FR-44 – הטלת UltraBoost מיוחדת | `TurnManager.grantSpecialRoll`, `SurpriseUltraBoostThrow` |
| FR-25 – הפתעה בשרשרת שייכת לבעל הכלי | `TurnState.deferSpecialRoll`, דחייה ב-`TurnManager.passTurn` |
| FR-22, FR-43 – הפסד התור הבא | `TurnManager.loseNextTurn`, דילוג ב-`TurnManager.passTurn` |

**ממשק למנגנון ההפתעות:** ההפתעות משנות תורות רק דרך שתי מתודות: `TurnManager.grantSpecialRoll(state, player)` ו-`TurnManager.loseNextTurn(state, player)`. בשתיהן `player` הוא בעל הכלי שנחת על ההפתעה, גם כשהכלי הגיע אליה בשרשרת (למשל כלי יריב שהוזז לאחור ב-FR-40). קוראים להן בזמן הנחיתה על המשבצת, לפני שהמהלך מסתיים. את שאר ההחלטות (מתי ההטלה המיוחדת מגיעה, ביטול ההטלה הנוספת, דילוג על התור) `GameEngine` ו-`TurnManager` כבר מבצעים.

**החלטות שלנו שלא כתובות בקבצי הקורס:**
- אם כלי של יריב מקבל בשרשרת הטלת UltraBoost, אי אפשר לתת לו אותה "מיד" באמצע תור של שחקן אחר. לכן היא הופכת להטלה הראשונה בתור הבא שלו, והפסד תור מבטל אותה (FR-22).
- אם המהלך שהגיע למשבצת ההפתעה בוצע עם 6, ההטלה הנוספת מה-6 נשמרת ומגיעה אחרי ההטלה המיוחדת (לפי FR-19, הזכאות להטלה נוספת נשמרת).

### לוח הרמה הבינונית (עומר)

| דרישה | איפה בקוד |
|---|---|
| FR-07 – לוח 17×17 עם המסלול הנוסף | `MediumBoardLayout`, `MediumBoardFactory` |
| FR-10, FR-11 – 8 חצים ו-8 משבצות הגנה | `Difficulty.MEDIUM`, `BoardFactory.build` |
| FR-13, FR-14 – 2 משבצות הפתעה מוגנות במיקום אקראי, בלי חפיפות | `BoardFactory.build` |

### מנגנון השאלות (דיאנה)

| דרישה | איפה בקוד |
|---|---|
| FR-12, FR-14 – 6 משבצות שאלה, 2 מכל רמה, במיקום אקראי | `QuestionSquarePlacer` |
| FR-33, FR-34 – זיהוי רמה לפי צבע והגרלת שאלה מתאימה | `QuestionLevel`, `QuestionRepository.findRandom` |
| FR-35, FR-36 – הצגת השאלה ובדיקת התשובה | `QuestionDialog`, `QuestionService` |
| FR-37 – 1/2/3 צעדים קדימה או אחורה לפי הרמה | `QuestionMovementRules` |

מאגר השאלות (`src/questions.json`) כולל 30 שאלות: 10 בכל רמה.

### מנגנון ההפתעות (דור)

| דרישה | איפה בקוד |
|---|---|
| FR-38 – הגרלה 10/10/10/30/30/10 | `SurpriseSelector` |
| FR-39 עד FR-44 – שש ההפתעות | `SurpriseStrategies`, `SurpriseMovement` |
| FR-25 – תגובות שרשרת בין שאלות והפתעות | `GameEngine` |

## חלוקת הקבצים לפי משימות ה-SDP

החלוקה מאפשרת לכל אחד להכניס ל-GitHub את החלק שלו ב-Pull Request נפרד (SDP 5.3.4, תנאי 2).

| חבר צוות | קבצים | דרישות |
|---|---|---|
| דור | `Controller/*`, `View/*`, קבצי Eclipse. איטרציה 2: `SurpriseStrategy`, `SurpriseStrategies`, `SurpriseSelector`, `SurpriseType`, `SurpriseMovement`, `SurpriseResolution`, `SurpriseEvent` | 3.4.1–3.4.2, FR-01–05, FR-15, FR-23, FR-48–49 (צד התצוגה), FR-38–44 |
| עומר | `Difficulty`, `GameRules`, `SetupValidator`, `Player`, `PlayerColor`, `Piece`, `Position`, `Cell`, `CellType`, `Board`, `BoardFactory`, `InteractionRules`. איטרציה 2: `BoardLayout`, `EasyBoardFactory`, `EasyBoardLayout`, `MediumBoardFactory`, `MediumBoardLayout` | FR-01–14, FR-28–30, FR-32 |
| גל | `Dice`, `DiceRoll`, `TurnManager`, `RankingManager`, `GameClock`, `GameResult`, `GameListener`, `GameEngine` (זרימת התור). איטרציה 2: `MultiplierDie`, `DiceThrow` ותתי-המחלקות שלה, `ThrowResult`, `TurnState` | FR-15–22, FR-45–48 |
| דיאנה | `MoveValidator`, `Move`, `GameState`, `GameEngine` (ביצוע מהלך ואכילה). איטרציה 2: `Question`, `QuestionLevel`, `QuestionRepository`, `QuestionService`, `QuestionMovementRules`, `QuestionSquarePlacer`, `View/QuestionDialog`, `questions.json` | FR-23–27, FR-31, FR-33–37 |
