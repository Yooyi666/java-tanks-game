# Java Tanks Game

**Turn-based multiplayer artillery game · Java · Processing · Gradle**

A playable three-level 2D Tanks game. Players move across destructible terrain, aim turrets and fire projectiles affected by wind and gravity. Tanks can buy equipment, deploy parachutes, and compete for the highest cumulative score. A custom **Shield** power-up absorbs the next damaging projectile blast.

> **Verified baseline (2026-10-19):** 147/147 JUnit tests passed; **96.45%** instruction coverage, **89.94%** branch coverage, **93.20% average JaCoCo coverage**. These are results from the supplied local Windows Gradle audit, not a claim that CI or every GUI pixel has been independently verified. See [verification details](docs/VERIFICATION.md).


> **Turn-arrow revision (pending Windows verification):** A minimal all-black arrow stays above the current tank until its first valid action. The Java 8-compatible headless smoke suite passed **155/155** tests in an isolated simulation; the revised project still needs a local Windows Gradle run. The historic 147-test / 93.20% JaCoCo results refer to the earlier verified baseline.

## Features

- **Turn-based multiplayer:** Players act alphabetically; destroyed tanks are skipped and scores persist across three levels. A small black arrow hovers above the current tank and remains visible until the player first moves, aims, adjusts power, fires, or successfully buys a power-up. It reappears on the next actionable turn.
- **Projectile physics:** Aiming and power, gravity, horizontal wind and terrain impact detection.
- **Destructible terrain:** 28 × 20 character layouts, twice-smoothed pixel terrain, crater deformation and trees that follow terrain.
- **Damage and survival:** Blast-radius damage, tank-death explosions, fall damage, fuel consumption and consumable parachutes.
- **Power-ups:** Repair, fuel, parachute and **Shield** (20 score, single-use protection against a damaging blast; does not prevent fall damage).
- **Results:** Winner announcement, ranked scores with delayed reveal and game restart.

## Tech stack

| Component | Technology |
| --- | --- |
| Game and physics | Java, Java 8-compatible source/API target |
| Rendering / controls | Processing 3.3.7 |
| Build and dependencies | Gradle |
| Unit / headless renderer tests | JUnit Jupiter 5 |
| Code coverage | JaCoCo |
| Documentation | Javadoc, design report, UML diagram |

## Quick start

**Requirements:** Windows or a suitable desktop environment; a JDK compatible with your Gradle installation, Gradle installed and available on `PATH`. The last verified local build used **JDK 17 and Gradle 8.14.6** while compiling Java 8-targeted bytecode. A real JDK 8 *runtime* smoke test has not yet been documented.

In the repository root:

```powershell
java -version
gradle --version
gradle clean test jacocoTestReport javadoc
gradle run
```

The window is **864 × 640** at **30 FPS**. Gradle continues showing `:run` as executing while the game's Processing window is open; close the game window to end the command.

For the validated Windows audit sequence:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\run-stage9-final-check.ps1
```

The audit writes `Stage9_Final_Audit.txt` and `Stage9_Gradle_Log.txt` locally (ignored by Git). JUnit, JaCoCo and Javadoc output live under `build/`.

## Controls

| Input | Action |
| --- | --- |
| `←` / `→` | Move current tank, consuming fuel |
| `↑` / `↓` | Change turret angle (hold) |
| `W` / `S` | Increase / decrease projectile power (hold) |
| `Space` | Fire projectile and end turn |
| `R` | Buy a repair kit for 20 points during a turn, or restart after game over |
| `F` | Buy 200 fuel for 10 points |
| `P` | Buy a parachute for 15 points |
| `H` | Buy a single-use Shield for 20 points |

## Repository structure

```text
java-tanks-game/
├── src/
│   ├── main/
│   │   ├── java/Tanks/App.java                # Main loop, entities, input, game rules
│   │   └── resources/Tanks/                  # Background, tree and HUD images
│   └── test/java/Tanks/                     # Nine JUnit test classes, 151 tests in this revision
├── config.json                              # Game levels and player colours
├── level1.txt / level2.txt / level3.txt    # Character-map terrain layouts
├── build.gradle / settings.gradle
├── run-stage9-final-check.ps1
├── docs/                                    # UML, design, test evidence, guidance
└── .github/workflows/ci.yml                # GitHub Actions build/test workflow
```

`App` extends `PApplet` and defines `Tanks`, `Projectiles`, `Tree` and `DeathExplosion` as nested entity classes. The design currently concentrates simulation, input handling, rendering and some shared state within `App.java`; [the design report](docs/Design_Report_EN.md) describes these trade-offs without claiming a fully decoupled engine.

## Tests and quality evidence

- **147 JUnit tests** across **9 test classes**, passing with 0 failures, errors or skips in the latest supplied local audit.
- **96.45%** JaCoCo instruction coverage and **89.94%** branch coverage (arithmetic mean **93.20%**).
- **Javadoc** generation passed.
- Java 8 bytecode output (`major=52`) passed; this is **not** a Java 8 runtime execution test.
- Headless renderer tests assert method calls and game state, **not** pixel-by-pixel visual accuracy.

See [verification notes](docs/VERIFICATION.md) and the [UML diagram](docs/Tanks_UML.pdf). A GitHub Actions workflow is included for reproducibility; its first run after uploading must be checked independently.

## Project background, attribution and publication caution

This project originated from a **University of Sydney INFO1113 / COMP9003 coursework scaffold** and was expanded with gameplay repairs, game-state logic, the Shield extension, and regression tests. Original scaffold artwork and maps are included **only to make this working project reproducible**; their right to be redistributed publicly has **not** been independently established. Review the [publishing checklist](docs/PUBLISHING_CHECKLIST.md) before changing a GitHub repository to **Public**. No open-source license has been assigned to the entire repository because rights for coursework-provided assets have not been confirmed.
