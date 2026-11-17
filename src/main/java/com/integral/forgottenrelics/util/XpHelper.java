package com.integral.forgottenrelics.util;

import net.minecraft.world.entity.player.Player;

/**
 * Botania's {@code ExperienceHelper} (used by the original XP Tome) on the current vanilla levelling curve: a
 * player's experience is handled as one total of points, and level/progress are recomputed from it, so draining and
 * adding never drift the way repeated float progress updates do.
 */
public final class XpHelper {
    private XpHelper() {
    }

    /** Points needed to go from {@code level} to {@code level + 1} (vanilla {@code getXpNeededForNextLevel}). */
    public static int barCap(int level) {
        if (level >= 30) {
            return 112 + (level - 30) * 9;
        }
        return level >= 15 ? 37 + (level - 15) * 5 : 7 + level * 2;
    }

    /** Total points to reach the start of {@code level}. */
    public static int experienceForLevel(int level) {
        int total = 0;
        for (int i = 0; i < level; i++) {
            total += barCap(i);
        }
        return total;
    }

    /** Original {@code getLevelForExperience}. */
    public static int levelForExperience(int experience) {
        int level = 0;
        while (experience >= barCap(level)) {
            experience -= barCap(level);
            level++;
        }
        return level;
    }

    /** Original {@code getPlayerXP}: total points from level and bar progress. */
    public static int playerXp(Player player) {
        return experienceForLevel(player.experienceLevel) + Math.round(player.experienceProgress * barCap(player.experienceLevel));
    }

    /** Sets the player's experience to an exact total (level, bar and score total). */
    public static void setPlayerXp(Player player, int total) {
        total = Math.max(0, total);
        int level = levelForExperience(total);
        player.experienceLevel = level;
        player.experienceProgress = (total - experienceForLevel(level)) / (float) barCap(level);
        player.totalExperience = total;
    }

    /** Original {@code addPlayerXP}. */
    public static void addPlayerXp(Player player, int amount) {
        setPlayerXp(player, playerXp(player) + amount);
    }

    /** Original {@code drainPlayerXP}. */
    public static void drainPlayerXp(Player player, int amount) {
        addPlayerXp(player, -amount);
    }
}
