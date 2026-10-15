package com.arena.pekaclient.feature;

import java.util.Collections;

/** Pure state tests for identity isolation, temporary data gaps, expiry and party-change pruning. */
public final class LeapClassCacheTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        retainsAcrossTemporaryGaps();
        expiresRatherThanKeepingStaleClassesForever();
        identitiesNeverShareClasses();
        freshClassReplacesOldObservation();
        strongerClassSourceWinsConflicts();
        partyChangesPruneAbsentIdentities();
        System.out.println();
        System.out.println("leap class cache: passed " + passed + ", failed " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void retainsAcrossTemporaryGaps() {
        LeapClassCache cache = new LeapClassCache(1000L, 100L);
        LeapClassCache.Resolution detected = cache.resolve("uuid:alice", LeapClass.ARCHER, "scoreboard", 100L);
        LeapClassCache.Resolution gap = cache.resolve("uuid:alice", null, "none", 101L);
        check("first observation accepted", detected.value == LeapClass.ARCHER && detected.changed);
        check("brief data gap retains class", gap.value == LeapClass.ARCHER && !gap.changed);
        check("retention is explicit", gap.reason.contains("temporarily retained"));
    }

    private static void expiresRatherThanKeepingStaleClassesForever() {
        LeapClassCache cache = new LeapClassCache(100L, 100L);
        cache.resolve("uuid:alice", LeapClass.MAGE, "scoreboard", 1000L);
        LeapClassCache.Resolution expired = cache.resolve("uuid:alice", null, "none", 1101L);
        check("expired class becomes unknown", expired.value == null && expired.changed);
        check("expiry explains clearing", expired.reason.contains("expired"));
        check("expired cache entry removed", cache.size() == 0);
    }

    private static void identitiesNeverShareClasses() {
        LeapClassCache cache = new LeapClassCache(1000L, 100L);
        cache.resolve("uuid:alice", LeapClass.HEALER, "scoreboard", 1L);
        LeapClassCache.Resolution other = cache.resolve("uuid:bob", null, "none", 2L);
        check("other identity stays unknown", other.value == null);
        check("same visible slot does not reuse previous player", cache.resolve(
                "uuid:replacement", null, "none", 3L).value == null);
    }

    private static void freshClassReplacesOldObservation() {
        LeapClassCache cache = new LeapClassCache(1000L, 100L);
        cache.resolve("name:player", LeapClass.ARCHER, "tab", 10L);
        LeapClassCache.Resolution replacement = cache.resolve("name:player", LeapClass.BERSERK, "scoreboard", 20L);
        check("fresh scoreboard class wins", replacement.value == LeapClass.BERSERK);
        check("replacement is reported", replacement.changed && replacement.reason.contains("replaced Archer"));
    }

    private static void strongerClassSourceWinsConflicts() {
        LeapClassCache cache = new LeapClassCache(100L, 100L);
        cache.resolve("name:player", LeapClass.ARCHER, "scoreboard", 0L);
        LeapClassCache.Resolution staleTab = cache.resolve("name:player", LeapClass.BERSERK, "tab", 10L);
        check("recent scoreboard beats a conflicting tab refresh", staleTab.value == LeapClass.ARCHER);
        check("lower-priority conflict is explained", staleTab.reason.contains("lower-priority tab")
                && staleTab.reason.contains("scoreboard class"));
        LeapClassCache.Resolution expiredBoard = cache.resolve("name:player", LeapClass.BERSERK, "tab", 101L);
        check("tab can replace an expired scoreboard observation", expiredBoard.value == LeapClass.BERSERK
                && expiredBoard.changed);
    }

    private static void partyChangesPruneAbsentIdentities() {
        LeapClassCache cache = new LeapClassCache(1000L, 100L);
        cache.resolve("name:leaving", LeapClass.TANK, "scoreboard", 0L);
        cache.retainCurrentPlayers(Collections.<String>emptyList(), 10L);
        check("short roster update grace", cache.size() == 1);
        cache.retainCurrentPlayers(Collections.<String>emptyList(), 109L);
        check("not pruned before grace", cache.size() == 1);
        cache.retainCurrentPlayers(Collections.<String>emptyList(), 110L);
        check("leaving teammate is pruned", cache.size() == 0);

        cache.resolve("name:staying", LeapClass.ARCHER, "scoreboard", 200L);
        cache.retainCurrentPlayers(Collections.<String>emptyList(), 210L);
        cache.retainCurrentPlayers(Collections.singleton("name:staying"), 250L);
        cache.retainCurrentPlayers(Collections.<String>emptyList(), 260L);
        cache.retainCurrentPlayers(Collections.<String>emptyList(), 359L);
        check("reappearing identity resets grace", cache.size() == 1);
        cache.retainCurrentPlayers(Collections.<String>emptyList(), 360L);
        check("new absence starts a fresh grace", cache.size() == 0);
    }

    private static void check(String name, boolean ok) {
        if (ok) {
            passed++;
        } else {
            failed++;
            System.out.println("FAIL " + name);
        }
    }
}
