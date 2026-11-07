# Client Inventory Interactions

Sort, merge and move-all buttons for chests and other container screens. **Client-side only**: it works on any server, with or without the mod, because every button just sends the same clicks you would make by hand.

## Buttons

Small buttons appear on the title line above the container slots and above your inventory:

| Button | What it does |
|---|---|
| **Sort Container** | Merges partial stacks and orders the container. Right-click to change the sort order. |
| **Take All** | Moves everything from the container into your inventory. |
| **Sort Inventory** | Sorts your main inventory (never the hotbar). Right-click to change the sort order. |
| **Move All** | Moves everything from your inventory into the container. |
| **Merge** | Moves only the items the container already holds, like a quick-stack. |

Merge and Move All skip your hotbar unless you hold **Ctrl** while clicking or turn on *Include Hotbar*.

Sort orders: **Name**, **Mod**, **Item ID** and **Count** (most plentiful first).

## Hotkeys and middle-click

- **Middle-click** a slot to sort that side (the container or your inventory). On by default; it is skipped on filled slots in creative so cloning still works.
- Key bindings for **Sort**, **Cycle Sort Order**, **Merge**, **Move All** and **Take All** are under *Controls → Client Inventory Interactions*. They start unbound so they don't clash with other mods, and they're ignored while you type in a search box.

## Sophisticated Storage and Backpacks

Sophisticated screens have their own buttons, so by default this mod stays out of them. Turn on **Replace Sophisticated Buttons** to show these buttons there instead and hide Sophisticated's sort, sort-by and transfer buttons. Sorting a Sophisticated storage then runs Sophisticated's own server-side sort (stack upgrades, no-sort and memory slots all keep working), using this mod's sort order.

Larger-than-64 slots elsewhere are supported too, as long as the slots report their real capacity.

## Configuration

Open it from the mods list (Forge) or Mod Menu (Fabric). Every button can be turned off, and each option explains itself when you hover it. Settings are saved in `config/clientinvinteractions.json`.

## Good to know

- Sorting is done with ordinary clicks, so very large containers send many clicks at once. Normal servers are fine with that; a strict anti-cheat plugin might not be.
- Containers with filtered or unusual slots can't always be sorted client-side; the mod tells you instead of guessing.
- *Debug Logging* (in the config) logs every click the mod sends and writes the full cause of network disconnects to the log, handy for bug reports.

## Versions

Minecraft 1.20.1 for Forge and Fabric (Fabric needs Fabric API; Mod Menu is optional).

## Credits

Not affiliated with Sophisticated Storage/Backpacks (by P3pp3rF1y) or Sophisticated Inventory Interactions, which inspired the button set. This mod contains none of their code or assets; it only calls Sophisticated's own sort at runtime when that toggle is on. Its own code is MIT-licensed.
