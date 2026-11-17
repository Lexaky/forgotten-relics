# PORTING_PLAN.md template

Copy this skeleton and fill every section. Write it in the user's language.

```markdown
# <Old mod> → <Target>: porting plan

Short paragraph: what the document is, how status is tracked, the rule "a point is done only after its check passes".

| What | Where | Version / commit |
|---|---|---|
| Original | repo URL | branch/commit, size, licence |
| Target API | repo URL | commit + version from gradle.properties |
| Target release jar | CurseForge project/file id | contains generated data missing from git |
| Reference port (if any) | URL | what it is really for (often binaries for another base mod) |

Licence note: what may be reused (textures/lang) and what must not be copied (target code/assets).

## 1. Inventory of the original
### 1.1 Aspects / resources  ### 1.2 Items  ### 1.3 Blocks  ### 1.4 Research
### 1.5 Events (exact formulas)  ### 1.6 Wand parts  ### 1.7 Enchantments  ### 1.8 Integrations  ### 1.9 Config

## 2. Target architecture
| Subsystem | How it works | Extensible from an addon? (data / API / own mechanism / no) |

## 3. Mapping old → new
### 3.1 Systems  ### 3.2 Aspects  ### 3.3 Items  (+ research ids of the base mod)

## 4. Ported / adapted / dropped
Three lists with reasons.

## 5. Plan
| # | Milestone | Depends on | Verification criterion |

## 6. Status
Legend ✅ / 🟡 / ⬜. How verification is done (server boot, GameTests, client check).
| Milestone | Status | What was verified (test names) — and **Not verified:** ... |

### What is left
Not ported / not verified / covered only by code review.

### Deviations already in code
One bullet per deviation, with the reason.
```

Rules that kept the original plan useful:
- Every milestone row names concrete test names once they exist.
- Deviations go into the plan the moment they are coded, not at the end.
- Never mark ✅ from compilation or a clean log alone when the point is about behaviour.
