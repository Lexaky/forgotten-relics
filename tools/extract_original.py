"""Extract research entries and infusion recipes from the original Forgotten Relics source.

Reads reference/original/.../research/RelicsResearchRegistry.java and writes
tools/data/original_research.json - the single source of truth the data generators
reuse (keys, parents, aspects, components, instability, warp).

Run: python -I tools/extract_original.py
"""
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "reference/original/src/main/java/com/integral/forgottenrelics/research/RelicsResearchRegistry.java"
ASPECTS_SRC = SRC.with_name("RelicsAspectRegistry.java")
OUT = ROOT / "tools/data/original_research.json"


def split_args(s):
    """Split a Java argument list on top-level commas."""
    out, depth, cur = [], 0, []
    for ch in s:
        if ch in "([{":
            depth += 1
        elif ch in ")]}":
            depth -= 1
        if ch == "," and depth == 0:
            out.append("".join(cur).strip())
            cur = []
        else:
            cur.append(ch)
    if "".join(cur).strip():
        out.append("".join(cur).strip())
    return out


def matching(s, i):
    """Index of the bracket closing the one at s[i]."""
    pairs = {"(": ")", "[": "]", "{": "}"}
    open_ch, close_ch, depth = s[i], pairs[s[i]], 0
    for j in range(i, len(s)):
        if s[j] == open_ch:
            depth += 1
        elif s[j] == close_ch:
            depth -= 1
            if depth == 0:
                return j
    raise ValueError("unbalanced")


def aspects(s):
    return {m.group(1): int(m.group(2)) for m in re.finditer(r"\.add\(Aspect\.(\w+),\s*(\d+)\)", s)}


def strings(s):
    return re.findall(r'"([^"]*)"', s)


def main():
    text = SRC.read_text(encoding="utf-8")
    text = re.sub(r"//[^\n]*", "", text)

    # local ItemStack variables -> original expression
    stacks = {m.group(1): m.group(2).strip() for m in
              re.finditer(r"ItemStack (\w+)\s*=\s*\(?(new ItemStack\([^;]*?\))\)?;", text)}

    recipes = {}
    for m in re.finditer(r'recipes\.put\("(\w+)",\s*ThaumcraftApi\.addInfusionCraftingRecipe\(', text):
        start = m.end() - 1
        args = split_args(text[start + 1:matching(text, start)])
        comps = re.findall(r"\w+", args[5].split("{", 1)[1].rsplit("}", 1)[0])
        recipes[m.group(1)] = {
            "research": strings(args[0])[0],
            "output": args[1],
            "instability": int(args[2]),
            "aspects": aspects(args[3]),
            "center": args[4],
            "components": comps,
        }

    research = []
    for m in re.finditer(r"new ForgottenRelicsResearchItem\(", text):
        start = m.end() - 1
        end = matching(text, start)
        args = split_args(text[start + 1:end])
        chain_end = text.index("registerResearchItem()", end)
        chain = text[end + 1:chain_end]
        entry = {
            "key": strings(args[0])[0],
            "aspects": aspects(args[2]),
            "col": int(args[3]), "row": int(args[4]), "complexity": int(args[5]),
            "icon": args[6],
            "flags": sorted(set(re.findall(r"\.set(Special|Round|Hidden|Concealed|Secondary|Stub|AutoUnlock)\(", chain))),
            "parents": [],
            "parents_hidden": [],
            "item_triggers": [],
            "pages": [],
        }
        for name, field in (("setParents", "parents"), ("setParentsHidden", "parents_hidden"),
                            ("setItemTriggers", "item_triggers")):
            for pm in re.finditer(r"\." + name + r"\(", chain):
                ps = pm.end() - 1
                inner = chain[ps + 1:matching(chain, ps)]
                entry[field] += strings(inner) if field != "item_triggers" else re.findall(r"\w+", inner)
        pm = re.search(r"\.setPages\(", chain)
        if pm:
            ps = pm.end() - 1
            for page in re.finditer(r'new ResearchPage\(([^()]*(?:\([^()]*\))?[^()]*)\)', chain[ps:matching(chain, ps)]):
                body = page.group(1).strip()
                rm = re.search(r'recipes\.get\("(\w+)"\)', body)
                entry["pages"].append({"recipe": rm.group(1)} if rm else {"text": strings(body)[0] if strings(body) else body})
        research.append(entry)

    warp_research = {m.group(1): int(m.group(2)) for m in
                     re.finditer(r'addWarpToResearch\("(\w+)",\s*(\d+)\)', text)}
    warp_item = [{"stack": m.group(1).strip(), "warp": int(m.group(2))} for m in
                 re.finditer(r"addWarpToItem\(([^;]+?),\s*(\d+)\);", text)]

    # Relic item aspects (RelicsAspectRegistry.registerItemAspectsLast): stack variable -> Main.itemX -> aspects
    asp_text = re.sub(r"//[^\n]*", "", ASPECTS_SRC.read_text(encoding="utf-8"))
    asp_text = asp_text[asp_text.index("registerItemAspectsLast"):]
    var_item = {m.group(1): m.group(2) for m in
                 re.finditer(r"ItemStack (\w+)\s*=\s*new ItemStack\(Main\.(\w+)", asp_text)}
    item_aspects = {}
    for m in re.finditer(r"registerObjectTag\((\w+),\s*new AspectList\(\)((?:\s*\.add\(Aspect\.\w+,\s*\d+\))+)\)", asp_text):
        if m.group(1) in var_item:
            item_aspects[var_item[m.group(1)]] = aspects(m.group(2))

    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps({"stacks": stacks, "research": research, "recipes": recipes,
                               "warp_research": warp_research, "warp_item": warp_item,
                               "item_aspects": item_aspects},
                              indent=1, ensure_ascii=False), encoding="utf-8")
    print(f"research={len(research)} recipes={len(recipes)} warp_research={len(warp_research)} "
          f"warp_item={len(warp_item)} stacks={len(stacks)} item_aspects={len(item_aspects)} -> {OUT.relative_to(ROOT)}")


if __name__ == "__main__":
    sys.exit(main())
