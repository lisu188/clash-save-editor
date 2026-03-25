# Developer guide for reverse-engineering integration

## Where format truth lives
1. Kotlin model annotations (`@ClashSimpleProperty`, `@ClashAggregateProperty`) define byte layout.
2. Object-level `isValid()` defines materialization/termination behavior.
3. Reverse-engineering docs under `docs/reverse-engineering/` define confidence and evidence boundaries.

## Update workflow (safe path)
1. Add/adjust field offset + length in model class.
2. Keep uncertain names neutral unless confidence is high.
3. Add/adjust tests for round-trip and preservation behavior.
4. Update `save-format.md`, `unknown-fields.md`, and `final-status.md` in the same change.

## Naming rules
- Use exact same term in model, tests, UI column, and docs.
- Prefer `unknown*`/`reserved*` style for low-confidence data.
- Keep deprecated aliases only when needed for compatibility.

## Guardrails
- Do not modify bytes outside the intended field window.
- Do not add semantics based on a single guess from decompiled code.
- Do not claim checksum/integrity support unless implementation exists and is tested.
