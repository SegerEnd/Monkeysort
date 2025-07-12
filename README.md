# Monkeysort Simulator 🐒

Een chaotisch, grappig spel geschreven in **Kotlin** met **JavaFX**, waarin apen proberen fruit te sorteren.

---

## 🧠 Over het spel

**Monkeysort Simulator** is een simulatiespel waarin een 2D-raster gevuld met fruit willekeurig wordt gesorteerd door een of meer apen. In eerste instantie gebruiken de apen de meest inefficiënte sorteermethode die er is: **Bogosort** (ook wel bekend als "Monkeysort"). Naarmate je voortgang boekt en munten verzamelt, kun je upgrades kopen zoals slimmere algoritmes en extra apen om je te helpen.

MacOS:

<img width="300" alt="Scherm­afbeelding 2025-07-12 om 10 07 38" src="https://github.com/user-attachments/assets/f5b80b43-143a-4bf5-92ff-35287764fa05" />

---

## 🎮 Gameplay Functionaliteit

- ✅ 2D-raster (standaard: **25x25**) gevuld met fruitemojis zoals: 🍎🍌🍇🍊🍉  
- ✅ Start met **Bogosort**: fruit wordt willekeurig gehusseld tot het alfabetisch is gesorteerd (links naar rechts, boven naar onder).
- ✅ Na elke game-tick:
  - ✔️ Controle of het fruit alfabetisch gesorteerd is
  - ✔️ Detectie van **3-of-meer-op-een-rij** fruitcombinaties (horizontaal of verticaal)
  - ✔️ Toekennen van munten voor combinaties
- ✅ **Upgrade systeem**:
  - 🧠 Ontgrendel betere sorteeralgoritmes (zoals Bubble Sort en Insertion Sort)
  - 🐵 Koop meer apen voor asynchrone sorting en snellere voortgang
- ✅ Game-tick loop: elke tick voert een shuffle uit, controleert sorteerstatus en verwerkt combinaties
- ✅ Modulaire code, klaar om uit te breiden
- 🎨 UI met JavaFX en animaties

---

## 🛠️ Opbouw & Uitvoeren

Dit project gebruikt **Gradle** voor de build en uitvoering van de applicatie.

### 💻 Vereisten

- Java 17+
- Kotlin
- JavaFX (zou automatisch moeten worden opgehaald via Gradle)

---

## Project compileren en uitvoeren

### 🔸 2. App bundelen en uitvoeren:

Met het zelf gemaakte Gradle-commando `runApp` kun je de applicatie **builden en uitvoeren** als een zelfstandige (self-contained) applicatie:

```bash
./gradlew runApp
```

Deze taak maakt een uitvoerbaar bestand afhankelijk van je besturingssysteem:

- **macOS**: `.app`
- **Windows**: `.exe`

Dit is handig om het spel te verspreiden of als eindgebruiker te gebruiken zonder afhankelijkheden.

### 🔸 1. Development run:

De oorspronkelijke command `./gradlew run` is nog steeds beschikbaar voor ontwikkeling, maar pakt de applicatie niet in een self-contained bestand.
Gebruik dit commando om de app te compileren en starten (zonder bundling):

```bash
./gradlew run
```

---

## 📁 Structuur

- `src/main/kotlin/com/segerend`: Broncode van het spel
- `src/test/kotlin/com/segerend`: Unit tests met JUnit en TestFX
- `src/main/resources/`: Assets zoals afbeeldingen
- `build.gradle.kts`: Gradle configuratie

---

## 🚀 Mogelijke toekomstige uitbreidingen

- 🎶 Geluidseffecten en muziek
- 🧩 Nieuwe sorteer-algoritmes
- 🌍 Online leaderboard
- 👾 Pixel-art apen en fruit (nu emoji's)

---

Veel plezier met de apen te laten sorteren! 🐒💥🍇
