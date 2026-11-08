# לודו קואלה – איטרציה 1

גרסת איטרציה 1 של משחק ה-Ludo של קבוצת Koala: משחק מלא ברמה הקלה ל-2–4 שחקנים, לפי סעיף 5.2.1 ב-SDP.

## הרצה

- נדרשת Java 17 ומעלה.
- ה-JAR לא נשמר בגיט. בונים אותו ב-Eclipse: File → Export → Java → Runnable JAR file, עם Launch configuration של `LudoApplication`, ושומרים בשם `LudoKoala.jar`.
- הרצה ב-Windows: לחיצה כפולה על `LudoKoala.jar`, או `java -jar LudoKoala.jar` משורת הפקודה.
- המשחק לא משתמש בקבצים חיצוניים או בנתיבים מקומיים, ולכן רץ מכל תיקייה (NFR-07).

## Eclipse

1. File → Import → General → Existing Projects into Workspace, ולבחור את התיקייה.
2. להריץ את `Controller.LudoApplication` כ-Java Application.

הקובץ `.settings/org.eclipse.core.resources.prefs` מגדיר קידוד UTF-8. לא למחוק אותו ולא להכניס אותו ל-`.gitignore`, אחרת הטקסט בעברית ייראה משובש ב-Eclipse על Windows.

## מה כלול באיטרציה 1

| סיפור משתמש (SDP 5.2.1) | דרישות SRS |
|---|---|
| 1 – הגדרת והתחלת משחק | FR-01 עד FR-06 |
| 2 – לוח המשחק (רמה קלה) | FR-07 עד FR-11, FR-14 |
| 3 – קובייה ותורות | FR-15 עד FR-20 |
| 4 – תנועת כלים | FR-23 עד FR-27, FR-31 |
| 5 – אכילה והגנה | FR-28, FR-29 |
| 6 – חסימות Double Block | FR-30 עד FR-32 |
| 7 – סיום, דירוג ושעון | FR-45 עד FR-49 |

לא כלול, לפי ה-SDP: רמות בינונית וקשה, קוביות Boost ו-UltraBoost, שאלות, הפתעות (איטרציה 2), וכן היסטוריית משחקים ב-CSV ואשף ניהול השאלות (איטרציה 3). הכפתורים שלהם מופיעים במסך הפתיחה כלא פעילים.

## ארכיטקטורה

- **Model** – כל חוקי המשחק והנתונים. לא תלוי ב-Swing או ב-AWT.
- **View** – המסכים, הלוח והחלונות הקופצים. אין בו לוגיקת משחק.
- **Controller** – מקבל את פעולות המשתמש, מפעיל את המודל ומחליף מסכים.

תבניות עיצוב:
- **Observer** – `GameListener`: מסך המשחק נרשם ל-`GameEngine` ומתעדכן אחרי כל שינוי, כך שהמודל לא מכיר את התצוגה.
- **Facade** – `GameEngine`: נקודת כניסה אחת לחוקים (`rollDice`, `movePiece`), והבקר לא צריך להכיר את מחלקות החוקים.
- **Factory** – `BoardFactory`: יוצר לוח לפי רמת קושי. ברמות הבינונית והקשה יתווסף שם רק אופן הבנייה.

כל הפרמטרים שמשתנים בין רמות נמצאים ב-`Difficulty`, והחוקים המשותפים לכל הרמות נמצאים ב-`GameRules` (NFR-05).

## חלוקת הקבצים לפי משימות ה-SDP

החלוקה מאפשרת לכל אחד להכניס ל-GitHub את החלק שלו ב-Pull Request נפרד (SDP 5.3.4, תנאי 2).

| חבר צוות | קבצים | דרישות |
|---|---|---|
| דור | `Controller/*`, `View/*`, קבצי Eclipse | 3.4.1–3.4.2, FR-01–05, FR-15, FR-23, FR-48–49 (צד התצוגה) |
| עומר | `Difficulty`, `GameRules`, `SetupValidator`, `Player`, `PlayerColor`, `Piece`, `Position`, `Cell`, `CellType`, `Board`, `BoardFactory`, `InteractionRules` | FR-01–11, FR-14, FR-28–30, FR-32 |
| גל | `Dice`, `DiceRoll`, `TurnManager`, `RankingManager`, `GameClock`, `GameResult`, `GameListener`, `GameEngine` (זרימת התור) | FR-15–20, FR-45–48 |
| דיאנה | `MoveValidator`, `Move`, `GameState`, `GameEngine` (ביצוע מהלך ואכילה) | FR-23–27, FR-31 |
