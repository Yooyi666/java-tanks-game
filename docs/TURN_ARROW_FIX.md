# Active turn arrow — black, offset, persistent until input

This patch customises the player-turn indicator to match the requested UX. The previous yellow, 2-second indicator is replaced by a small all-black downward arrow with a 30-pixel gap below its tip and the approximate turret tip.

## Behaviour

- The arrow appears above the living player whose turn is active, and does **not** time out while the player waits.
- Its triangle tip is positioned at `tank.y - 55`, with a short vertical stem immediately above it. Both are black.
- The indicator disappears on the first accepted movement / turret / power key, the Space key to fire, or a successful power-up purchase. Unrecognised keys or unaffordable purchases do not hide it.
- The arrow stays hidden for the remainder of that turn; it reappears for the next player's turn after the previous projectile resolves.
- It is suppressed during projectile flight, level completion and game-over.

## Files changed

- `src/main/java/Tanks/App.java` — arrow style and lifecycle only.
- `src/test/java/Tanks/Stage6RenderPipelineTest.java` — updated black-arrow rendering assertions, persistent state checks and new interaction tests.

## Validation

The Java 8-language-level isolated simulation passed **155 tests** and no failures. That simulation uses stubbed Processing and JUnit implementations and **does not replace** a real Gradle / Processing window run on Windows.

From the project root, run:

```powershell
gradle clean test --rerun-tasks
gradle run
```

Expected JUnit count: **155**. The earlier user-confirmed **147/147 tests and 93.20% JaCoCo** are historical baseline values; this revision's JaCoCo coverage has not yet been measured on Windows.

If a test fails, inspect `build/reports/tests/test/index.html` or `build/test-results/test/TEST-*.xml` for the exact assertion.
