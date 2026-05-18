# Mechanik Dash

To jest szkolny prototyp gry 2D inspirowanej Boulder Dash, zrobiony pod Android Studio w Kotlinie i Jetpack Compose.

## Co jest w środku

- bohater: uczeń ZSP
- cel: zebrać dobre oceny i dojść do stypendium
- 8 poziomów:
  1. Sala Sobczaka
  2. Korytarz 3 Piętro
  3. Sala Elektryków
  4. Korytarz 2 Piętro
  5. Aula
  6. Korytarz 1 Piętro (Matusiak — Mini Boss)
  7. Sala Robotyków
  8. Wyjście (Dyrektor — BOSS)
- przeszkody: telefony, książki, kartkówki
- losowe przeszkody 50/50: telefon albo książka
- zbierane: dobre oceny
- zamiennik ziemi: ściany i balony

## Sterowanie

Przyciski strzałek na ekranie.

## Pliki

- `app/src/main/java/com/example/mechanikdash/MainActivity.kt`
- `app/src/main/java/com/example/mechanikdash/game/GameController.kt`
- `app/src/main/java/com/example/mechanikdash/game/LevelRepository.kt`

## Uwaga

Wersja ma prostą grafikę zastępczą z kształtów. To działa od razu, a potem można łatwo podmienić to na własne sprite'y.
