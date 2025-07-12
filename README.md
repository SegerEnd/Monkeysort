# Monkeysort 🐒

Een chaotisch, grappig spel over sorteren geschreven in **Kotlin** met **JavaFX**, waarin apen fruit sorteren.

---

## 🧠 Over het spel

**Monkeysort** is een simulatiespel waarin een 2D-raster gevuld met fruit, waar je aapjes aan het werk zet om het grid te sorteren. In eerste instantie gebruiken de apen de meest inefficiënte sorteermethode die er is: **Bogosort** (ook wel bekend als "Monkeysort"). Naarmate je voortgang boekt en munten verzamelt, kun je upgrades kopen zoals slimmere algoritmes en extra apen om je te helpen.

MacOS:

<img width="300" alt="Scherm­afbeelding 2025-07-12 om 10 07 38" src="https://github.com/user-attachments/assets/f5b80b43-143a-4bf5-92ff-35287764fa05" />

---

## 🎮 Gameplay Functionaliteit

- 2D-raster (standaard: **25x25**) gevuld met fruit emoji's
- Start met **Bogosort**: fruit wordt willekeurig gehusseld.
- Na elke game-tick:
  - Controle of het fruit alfabetisch gesorteerd is
  - Detectie van combo's **3-of-meer-op-een-rij** fruitcombinaties (horizontaal of verticaal)
  - Verkijgen van munten voor combo's
- **Upgrade systeem**:
  - 🧠 Ontgrendel betere sorteeralgoritmes (zoals Bubble Sort en Insertion Sort)
  - 🐵 Koop meer apen voor asynchrone sorting en snellere voortgang
- Game-tick loop: elke tick voert een shuffle uit, controleert sorteerstatus en verwerkt combinaties
- Modulaire code, klaar om uit te breiden
- 🎨 UI met JavaFX en animaties

---

## 🛠️ Opbouw & Uitvoeren

Dit project gebruikt **Gradle** voor de build en uitvoering van de applicatie.

### 💻 Vereisten

- Java 17+
- Kotlin
- JavaFX (zou automatisch moeten worden opgehaald via Gradle)
- Mac of Windows

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

### Testen
Voor de geschreven testen met JUnit en TestFX kun je de volgende opdracht gebruiken:

```bash
./gradlew test
```

De testresultaten worden door JaCoCo gegenereerd en opgeslagen in de `build/jacoco/test/html/index.html` directory. Als alles goed word uitgevoerd zou de test coverage boven de 90% zijn.

Test op: 12-juli-2025 - MacBook Pro - Sequoia 15.5

<img width="532" height="245" alt="Scherm­afbeelding 2025-07-12 om 22 49 45" src="https://github.com/user-attachments/assets/81eb9791-276e-4c08-a3f4-40dacfce8687" />

---

### 📦 Project Dependencies
- Kotlin
- JavaFX
- JUnit 5
- TestFX
- JaCoCo
- Gradle

---

## 📁 Structuur

- `src/main/kotlin/com/segerend`: Broncode van het spel
- `src/test/kotlin/com/segerend`: Unit tests met JUnit en TestFX
- `src/main/resources/`: Assets zoals afbeeldingen
- `build.gradle.kts`: Gradle configuratie

---

## Showcase video

https://github.com/user-attachments/assets/e41a6c5f-7622-4c42-a339-757f20780e21

Veel plezier met de apen te laten sorteren! 🐒💥🍇
