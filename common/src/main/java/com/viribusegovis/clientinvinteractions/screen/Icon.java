package com.viribusegovis.clientinvinteractions.screen;

/** 7x7 pixel-art glyphs drawn with fills, so the mod ships no textures. '#' is a lit pixel. */
enum Icon {
    SORT(
            "#######",
            ".......",
            "#####..",
            ".......",
            "###....",
            ".......",
            "#......"),
    MERGE_UP(
            "#######",
            ".......",
            "...#...",
            "..###..",
            ".#.#.#.",
            "...#...",
            "...#..."),
    MOVE_ALL_UP(
            "...#...",
            "..#.#..",
            ".#...#.",
            "...#...",
            "..#.#..",
            ".#...#.",
            "......."),
    TAKE_ALL_DOWN(
            ".......",
            ".#...#.",
            "..#.#..",
            "...#...",
            ".#...#.",
            "..#.#..",
            "...#...");

    static final int SIZE = 7;

    private final String[] rows;

    Icon(String... rows) {
        this.rows = rows;
    }

    boolean isLit(int x, int y) {
        return rows[y].charAt(x) == '#';
    }
}
