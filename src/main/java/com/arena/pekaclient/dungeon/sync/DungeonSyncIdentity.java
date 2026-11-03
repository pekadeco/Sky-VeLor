package com.arena.pekaclient.dungeon.sync;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Collection;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Pure helpers for generating the same temporary dungeon-session identifier on each client. */
public final class DungeonSyncIdentity {
    private static final Charset UTF8 = Charset.forName("UTF-8");
    private static final Pattern MC_COLOUR = Pattern.compile("(?i)\\u00a7[0-9A-FK-OR]");
    private static final Pattern SERVER_ID = Pattern.compile("(?i)(?:^|[^a-z0-9])(sbdung-\\d+)(?:$|[^a-z0-9])");
    private static final Pattern SERVER_ID_VALUE = Pattern.compile("(?i)sbdung-\\d+");
    private static final Pattern FLOOR = Pattern.compile(
            "(?i)(?:\\b(M[1-7]|F[1-7])\\b|master\\s*mode\\s*([1-7])|catacombs\\s*\\(\\s*F([1-7])\\s*\\))");
    private static final Pattern USERNAME = Pattern.compile("[A-Za-z0-9_]{3,16}");
    private static final int MIN_PARTY_SIZE = 2;
    private static final Pattern PARTY_SIZE = Pattern.compile("(?i)\\bparty\\s*\\(\\s*(\\d+)\\s*\\)");
    private static final Pattern SYNTHETIC_TAB_LABEL = Pattern.compile(
            "(?i)^\\s*(?:profile|ultimate|revive\\s+stones|crypts|server\\s+info|player\\s+stats|"
                    + "opened\\s+rooms|completed\\s+rooms|secrets\\s+found|gems|area|server|time|deaths?|"
                    + "damage\\s+dealt|healing\\s+done|milestones?|pet(?:\\s+sitter)?|bank|purse|interest|keys)\\s*:");

    private DungeonSyncIdentity() { }

    public static boolean isDungeonStartMessage(String raw) {
        return "dungeon starts in 1 second.".equals(stripFormatting(raw).trim().toLowerCase(Locale.ROOT));
    }

    /**
     * Fakepixel may deliver the exact countdown line as ordinary chat or as a system message.
     * Only these two visible-chat types are accepted; action-bar messages and unrelated chat
     * cannot start a synchronization session.
     */
    public static boolean isDungeonStartMessage(byte chatType, String raw) {
        return (chatType == 0 || chatType == 1) && isDungeonStartMessage(raw);
    }

    /**
     * Extracts the declared party size from Tab List header/row text. A value of zero means the
     * custom Tab List did not expose a reliable Party (n) label.
     */
    public static int findPartySize(Iterable<String> sources) {
        if (sources == null) return 0;
        int found = 0;
        for (String source : sources) {
            Matcher matcher = PARTY_SIZE.matcher(stripFormatting(source));
            while (matcher.find()) {
                int candidate;
                try { candidate = Integer.parseInt(matcher.group(1)); }
                catch (NumberFormatException ignored) { return -1; }
                if (candidate < 1 || candidate > 5) return candidate;
                if (found != 0 && found != candidate) return -1;
                found = candidate;
            }
        }
        return found;
    }

    /**
     * A profile row is considered a rendered player row only when the profile's own exact
     * account name appears as a complete username token in the row's visible text. This filters
     * most Fakepixel's synthetic Tab List rows (for example, "Ultimate: READY") without deriving
     * a username from rank or class decoration.
     */
    public static boolean isVisibleProfileRow(String formattedRow, String profileName) {
        if (!isValidUsername(profileName) || formattedRow == null || formattedRow.trim().isEmpty()) return false;
        String row = stripFormatting(formattedRow).trim().toLowerCase(Locale.ROOT);
        if (SYNTHETIC_TAB_LABEL.matcher(row).find() || PARTY_SIZE.matcher(row).find()) return false;
        String name = profileName.trim().toLowerCase(Locale.ROOT);
        int from = 0;
        while ((from = row.indexOf(name, from)) >= 0) {
            int end = from + name.length();
            boolean left = from == 0 || !isUsernameCharacter(row.charAt(from - 1));
            boolean right = end == row.length() || !isUsernameCharacter(row.charAt(end));
            if (left && right) return true;
            from = end;
        }
        return false;
    }

    private static boolean isUsernameCharacter(char value) {
        return value == '_' || Character.isLetterOrDigit(value);
    }

    /** Uses only profile usernames supplied by the current server's Tab List. */
    public static boolean isValidUsername(String name) {
        return name != null && USERNAME.matcher(name.trim()).matches();
    }

    public static int countValidTabNames(Iterable<String> tabNames) {
        return uniqueValidNames(tabNames).size();
    }

    public static List<String> buildTabListRoster(Iterable<String> tabNames, String selfName) {
        return buildTabListRoster(tabNames, Collections.<String>emptyList(), Collections.<String>emptyList(), selfName, 0);
    }

    public static List<String> buildTabListRoster(Iterable<String> tabNames,
                                                   Iterable<String> classMarkedNames,
                                                   String selfName) {
        return buildTabListRoster(tabNames, Collections.<String>emptyList(), classMarkedNames, selfName, 0);
    }

    /**
     * Builds a canonical roster using exact profile names. Prefer a complete set of visible
     * player rows or class-marked rows. Some Fakepixel clients do not expose the local player's
     * class decoration in the Tab List; when every other party member is uniquely class-marked,
     * the local client's own profile name may complete that roster to the declared Party (n) size.
     * This fallback is deliberately limited to exactly n-1 remote profiles and never fills gaps
     * with arbitrary profile names.
     *
     * Identity is based on server-provided usernames only; no UUID lookup or Mojang account
     * authentication is required. Offline-mode (cracked) servers are supported when the server
     * supplies the same account names to every party member's Tab List.
     */
    public static List<String> buildTabListRoster(Iterable<String> tabNames,
                                                   Iterable<String> visiblePlayerNames,
                                                   Iterable<String> classMarkedNames,
                                                   String selfName,
                                                   int expectedPartySize) {
        if (tabNames == null || selfName == null) return Collections.emptyList();
        String localName = selfName.trim();
        if (!USERNAME.matcher(localName).matches()) return Collections.emptyList();

        List<String> profiles = uniqueValidNames(tabNames);
        String actualLocalName = findIgnoreCase(profiles, localName);
        if (actualLocalName == null) return Collections.emptyList();
        int minimumRosterSize = minimumRosterSize();
        if (expectedPartySize < 0 || (expectedPartySize != 0
                && (expectedPartySize < minimumRosterSize || expectedPartySize > 5))) {
            return Collections.emptyList();
        }

        List<String> visiblePlayers = intersectProfiles(visiblePlayerNames, profiles);
        List<String> classMarked = intersectProfiles(classMarkedNames, profiles);
        if (expectedPartySize >= minimumRosterSize && expectedPartySize <= 5) {
            if (visiblePlayers.size() == expectedPartySize && findIgnoreCase(visiblePlayers, actualLocalName) != null) {
                return sortedRoster(visiblePlayers);
            }
            if (classMarked.size() == expectedPartySize && findIgnoreCase(classMarked, actualLocalName) != null) {
                return sortedRoster(classMarked);
            }

            // The local player's own profile is authoritative membership evidence: this client
            // is inside that party's active run. Some Fakepixel tab rows omit the local player's
            // class badge, while the other party members still have owner-bound class badges.
            // Only accept this fallback when exactly n-1 unique, non-local class-marked profiles
            // remain. Never append the local name to an arbitrary incomplete roster.
            if (classMarked.size() == expectedPartySize - 1
                    && findIgnoreCase(classMarked, actualLocalName) == null) {
                List<String> completedRoster = new ArrayList<String>(classMarked);
                completedRoster.add(actualLocalName);
                if (completedRoster.size() == expectedPartySize) return sortedRoster(completedRoster);
            }

            if (profiles.size() == expectedPartySize) return sortedRoster(profiles);
            return Collections.emptyList();
        }

        if (visiblePlayers.size() >= minimumRosterSize && visiblePlayers.size() <= 5
                && findIgnoreCase(visiblePlayers, actualLocalName) != null) return sortedRoster(visiblePlayers);
        if (profiles.size() >= minimumRosterSize && profiles.size() <= 5) return sortedRoster(profiles);
        if (classMarked.size() >= minimumRosterSize && classMarked.size() <= 5
                && findIgnoreCase(classMarked, actualLocalName) != null) return sortedRoster(classMarked);
        return Collections.emptyList();
    }

    private static List<String> intersectProfiles(Iterable<String> candidates, List<String> profiles) {
        List<String> result = new ArrayList<String>();
        if (candidates == null) return result;
        for (String candidate : candidates) {
            String actual = findIgnoreCase(profiles, candidate == null ? null : candidate.trim());
            if (actual != null && findIgnoreCase(result, actual) == null) result.add(actual);
        }
        return result;
    }

    private static List<String> sortedRoster(List<String> roster) {
        List<String> sorted = new ArrayList<String>(roster);
        Collections.sort(sorted, String.CASE_INSENSITIVE_ORDER);
        return Collections.unmodifiableList(sorted);
    }

    private static List<String> uniqueValidNames(Iterable<String> names) {
        List<String> unique = new ArrayList<String>();
        if (names == null) return unique;
        for (String rawName : names) {
            if (rawName == null) continue;
            String name = rawName.trim();
            if (!USERNAME.matcher(name).matches() || findIgnoreCase(unique, name) != null) continue;
            unique.add(name);
        }
        return unique;
    }

    private static String findIgnoreCase(List<String> names, String wanted) {
        if (names == null || wanted == null) return null;
        for (String name : names) if (name.equalsIgnoreCase(wanted)) return name;
        return null;
    }

    public static String stripFormatting(String raw) {
        return MC_COLOUR.matcher(raw == null ? "" : raw).replaceAll("");
    }

    public static String findServerId(Iterable<String> sources) {
        if (sources == null) return null;
        for (String source : sources) {
            Matcher matcher = SERVER_ID.matcher(stripFormatting(source).trim());
            if (matcher.find()) return matcher.group(1).toLowerCase(Locale.ROOT);
        }
        return null;
    }

    public static String findFloor(Iterable<String> sources) {
        if (sources == null) return null;
        for (String source : sources) {
            Matcher matcher = FLOOR.matcher(stripFormatting(source));
            if (!matcher.find()) continue;
            if (matcher.group(1) != null) return matcher.group(1).toUpperCase(Locale.ROOT);
            if (matcher.group(2) != null) return "M" + matcher.group(2);
            if (matcher.group(3) != null) return "F" + matcher.group(3);
        }
        return null;
    }

    /**
     * Derives a shared ID from the server instance, floor, and exact party usernames.
     * Names are canonicalized and hashed locally; only this SHA-256 value is sent to the Worker.
     */
    public static String createNamedSessionId(String serverId, String floor, List<String> playerNames) {
        if (serverId == null || !SERVER_ID_VALUE.matcher(serverId.trim()).matches()
                || floor == null || !floor.trim().matches("(?i)(?:M|F)[1-7]")
                || playerNames == null || playerNames.size() < minimumRosterSize() || playerNames.size() > 5) return null;

        List<String> sortedNames = new ArrayList<String>();
        for (String rawName : playerNames) {
            if (rawName == null) return null;
            String name = rawName.trim().toLowerCase(Locale.ROOT);
            if (!USERNAME.matcher(name).matches()) return null;
            if (sortedNames.contains(name)) return null;
            sortedNames.add(name);
        }
        Collections.sort(sortedNames);

        StringBuilder canonical = new StringBuilder("skyvelor-dungeon-sync-v3\n");
        appendPart(canonical, serverId.trim().toLowerCase(Locale.ROOT));
        appendPart(canonical, floor.trim().toUpperCase(Locale.ROOT));
        canonical.append(sortedNames.size()).append('\n');
        for (String name : sortedNames) appendPart(canonical, name);
        return sha256Hex(canonical.toString().getBytes(UTF8));
    }


    private static int minimumRosterSize() {
        return MIN_PARTY_SIZE;
    }

    private static void appendPart(StringBuilder target, String value) {
        target.append(value.length()).append(':').append(value).append('\n');
    }

    private static String sha256Hex(byte[] bytes) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte item : digest) result.append(String.format(Locale.ROOT, "%02x", item & 0xff));
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is required by the Java runtime", exception);
        }
    }
}
