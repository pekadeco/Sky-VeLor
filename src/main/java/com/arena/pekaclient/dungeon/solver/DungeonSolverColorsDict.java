package com.arena.pekaclient.dungeon.solver;

/** Metadata-to-colour mapping used by the 1.8.9 stained-glass terminal. */
public enum DungeonSolverColorsDict {
    NONE("none", -1, -1),
    WHITE("white", 0, 15), ORANGE("orange", 1, 14), MAGENTA("magenta", 2, 13),
    LIGHT_BLUE("light blue", 3, 12), YELLOW("yellow", 4, 11), LIME("lime", 5, 10),
    PINK("pink", 6, 9), GRAY("gray", 7, 8), LIGHT_GRAY("silver", 8, 7),
    CYAN("cyan", 9, 6), PURPLE("purple", 10, 5), BLUE("blue", 11, 4),
    BROWN("brown", 12, 3), GREEN("green", 13, 2), RED("red", 14, 1), BLACK("black", 15, 0);

    private final String color;
    private final int main;
    private final int dye;

    DungeonSolverColorsDict(String color, int main, int dye) {
        this.color = color;
        this.main = main;
        this.dye = dye;
    }

    public static DungeonSolverColorsDict getColorFromMain(int metadata) {
        for (DungeonSolverColorsDict value : values()) if (value.main == metadata) return value;
        return NONE;
    }

    public static DungeonSolverColorsDict getColorFromDye(int metadata) {
        for (DungeonSolverColorsDict value : values()) if (value.dye == metadata) return value;
        return NONE;
    }

    @Override
    public String toString() { return color; }
}
