package com.arena.pekaclient.feature.mining;

public final class MiningAbilityCooldownTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        MiningAbilityCooldown.AbilityUse pickobulus = MiningAbilityCooldown.parseUseMessage("You used your Pickobulus Pickaxe Ability!");
        check("Pickobulus is recognized", pickobulus != null && "Pickobulus".equals(pickobulus.name));
        check("Pickobulus uses a nominal 60 second cooldown", pickobulus != null && pickobulus.cooldownSeconds == 60);
        MiningAbilityCooldown.AbilityUse speed = MiningAbilityCooldown.parseUseMessage("§aYou used your Mining Speed Boost Pickaxe Ability!§r");
        check("formatted Mining Speed Boost is recognized", speed != null && speed.name.equals("Mining Speed Boost"));
        check("Mining Speed Boost uses a nominal 120 second cooldown", speed != null && speed.cooldownSeconds == 120);
        check("unknown ability cooldown is not guessed", MiningAbilityCooldown.parseUseMessage("You used your Example Pickaxe Ability!") == null);
        check("unrelated chat is ignored", MiningAbilityCooldown.parseUseMessage("You broke a block") == null);
        check("null messages are ignored", MiningAbilityCooldown.parseUseMessage(null) == null);
        check("remaining cooldown rounds up", MiningAbilityCooldown.remainingSeconds(2501L, 2000L) == 1L);
        check("remaining cooldown rounds up across one second", MiningAbilityCooldown.remainingSeconds(3001L, 2000L) == 2L);
        check("elapsed cooldown is ready", MiningAbilityCooldown.remainingSeconds(2000L, 2000L) == 0L);
        check("inactive connection never shows stale cooldown", "Not tracked".equals(
                MiningAbilityCooldown.statusText(1000L, false, "Pickobulus", 5000L)));
        check("unknown prior ability shows not tracked", "Not tracked".equals(
                MiningAbilityCooldown.statusText(1000L, true, null, 0L)));
        check("ready status has no remaining time", "Ready".equals(
                MiningAbilityCooldown.statusText(6000L, true, "Pickobulus", 5000L)));
        check("cooldown status shows ability and seconds", "Pickobulus - 4s".equals(
                MiningAbilityCooldown.statusText(1000L, true, "Pickobulus", 5000L)));
        finish();
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++;
        else { failed++; System.out.println("FAIL " + name); }
    }

    private static void finish() {
        System.out.println("mining ability cooldown: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }
}
