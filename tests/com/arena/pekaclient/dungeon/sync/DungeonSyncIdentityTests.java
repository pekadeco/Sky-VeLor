package com.arena.pekaclient.dungeon.sync;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class DungeonSyncIdentityTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        check("exact start message is accepted", DungeonSyncIdentity.isDungeonStartMessage("Dungeon starts in 1 second."));
        check("formatted start message is accepted", DungeonSyncIdentity.isDungeonStartMessage("\u00a7eDungeon starts in 1 second."));
        check("similar chat text is rejected", !DungeonSyncIdentity.isDungeonStartMessage("Dungeon starts in 2 seconds."));
        check("exact countdown starts sync when delivered as ordinary chat", DungeonSyncIdentity.isDungeonStartMessage((byte) 0, "Dungeon starts in 1 second."));
        check("exact countdown starts sync when delivered as system text", DungeonSyncIdentity.isDungeonStartMessage((byte) 1, "\u00a7aDungeon starts in 1 second."));
        check("action-bar messages cannot start sync", !DungeonSyncIdentity.isDungeonStartMessage((byte) 2, "Dungeon starts in 1 second."));
        check("unrelated ordinary chat cannot start sync", !DungeonSyncIdentity.isDungeonStartMessage((byte) 0, "Dungeon starts in 2 seconds."));
        check("server ID is normalized", "sbdung-12".equals(DungeonSyncIdentity.findServerId(Arrays.asList("server: SBdung-12"))));
        check("server ID can be parsed from tab-list header/footer text",
                "sbdung-8".equals(DungeonSyncIdentity.findServerId(Arrays.asList("Welcome", "Server: sbdung-8", "Fakepixel"))));
        check("server ID is not inferred from unrelated host", DungeonSyncIdentity.findServerId(Arrays.asList("play.example.net")) == null);
        check("master floor is normalized", "M7".equals(DungeonSyncIdentity.findFloor(Arrays.asList("Master Mode 7"))));
        check("normal floor is normalized", "F7".equals(DungeonSyncIdentity.findFloor(Arrays.asList("The Catacombs (F7)"))));
        check("party size is parsed from the custom Tab List header", DungeonSyncIdentity.findPartySize(
                Arrays.asList("You are playing on MCG.FAKEPIXEL.HF", "\u00a7bParty (2)", "Server Info")) == 2);
        check("party size ignores unrelated row text", DungeonSyncIdentity.findPartySize(
                Arrays.asList("Profile: Mango", "Ultimate: READY", "Revive Stones: 0")) == 0);
        check("conflicting party headers are rejected", DungeonSyncIdentity.findPartySize(
                Arrays.asList("Party (2)", "Party (3)")) == -1);
        check("Party (1) is recognized as a single-player run and can be rejected by stable roster policy",
                DungeonSyncIdentity.findPartySize(Arrays.asList("Party (1)")) == 1
                        && DungeonSyncIdentity.buildTabListRoster(Arrays.asList("ii8we_", "OtherName"),
                                Arrays.asList("ii8we_"), Collections.<String>emptyList(), "ii8we_", 1).isEmpty());
        check("unsupported party size remains an explicit rejection", DungeonSyncIdentity.buildTabListRoster(
                Arrays.asList("ii8we_", "whiteshadow", "FakeRow"), Arrays.asList("ii8we_", "whiteshadow"),
                Arrays.asList("ii8we_", "whiteshadow"), "ii8we_", 6).isEmpty());
        check("a displayed profile row matches its own account name", DungeonSyncIdentity.isVisibleProfileRow(
                "\u00a78[\u00a7b238\u00a78] \u00a7b[MVP+\u00a7b] whiteshadow", "whiteshadow"));
        check("a profile name must match as a complete token", !DungeonSyncIdentity.isVisibleProfileRow(
                "[MVP+] ii8we_extra", "ii8we_"));
        check("synthetic tab rows are not mistaken for player rows", !DungeonSyncIdentity.isVisibleProfileRow(
                "Ultimate: READY", "FakeUltimateRow"));
        check("synthetic tab labels are excluded even when their internal profile is a valid username",
                !DungeonSyncIdentity.isVisibleProfileRow("Ultimate: READY", "Ultimate"));
        check("profile summary rows are not mistaken for dungeon party members",
                !DungeonSyncIdentity.isVisibleProfileRow("Profile: Mango", "Mango"));

        List<String> tabRoster = DungeonSyncIdentity.buildTabListRoster(
                Arrays.asList("ii8we_", "whiteshadow", "peropigeon", "II8WE_", "not a username"), "ii8we_");
        check("session roster uses only unique valid Tab List profile names", tabRoster.size() == 3
                && tabRoster.contains("ii8we_") && tabRoster.contains("whiteshadow") && tabRoster.contains("peropigeon"));
        check("tab roster order is deterministic", tabRoster.equals(Arrays.asList("ii8we_", "peropigeon", "whiteshadow")));
        check("tab roster requires the local player", DungeonSyncIdentity.buildTabListRoster(
                Arrays.asList("whiteshadow", "peropigeon"), "ii8we_").isEmpty());
        check("stable mode rejects a declared Party (1) roster",
                DungeonSyncIdentity.buildTabListRoster(Arrays.asList("ii8we_", "Profile", "Ultimate"),
                        Collections.<String>emptyList(), Collections.<String>emptyList(), "ii8we_", 1).isEmpty());
        check("Party (1) does not let synthetic profile rows join the roster",
                DungeonSyncIdentity.buildTabListRoster(Arrays.asList("ii8we_", "Profile", "Ultimate", "Mango"),
                        Arrays.asList("Mango"), Arrays.asList("Mango"), "ii8we_", 1).isEmpty());
        check("tab roster rejects more than five valid profiles", DungeonSyncIdentity.buildTabListRoster(
                Arrays.asList("AlphaOne", "BetaTwo", "GammaThree", "DeltaFour", "Epsilon5", "ii8we_"), "ii8we_").isEmpty());
        List<String> largerTab = Arrays.asList("ii8we_", "whiteshadow", "peropigeon", "LobbyOne", "LobbyTwo", "LobbyThree", "LobbyFour");
        List<String> classRows = Arrays.asList("ii8we_", "whiteshadow", "peropigeon");
        check("large Tab List selects only class-marked dungeon profiles", DungeonSyncIdentity.buildTabListRoster(
                largerTab, classRows, "ii8we_").equals(Arrays.asList("ii8we_", "peropigeon", "whiteshadow")));
        check("filtered roster fails closed when local class row is absent", DungeonSyncIdentity.buildTabListRoster(
                largerTab, Arrays.asList("whiteshadow", "peropigeon"), "ii8we_").isEmpty());
        check("small Tab List uses exact profile names without requiring class decorations", DungeonSyncIdentity.buildTabListRoster(
                Arrays.asList("ii8we_", "whiteshadow", "peropigeon"), Collections.singletonList("whiteshadow"), "ii8we_")
                .equals(Arrays.asList("ii8we_", "peropigeon", "whiteshadow")));

        List<String> fakepixelProfiles = Arrays.asList("ii8we_", "whiteshadow", "SyntheticRowOne", "SyntheticRowTwo",
                "SyntheticRowThree", "SyntheticRowFour");
        List<String> displayedPartyPlayers = Arrays.asList("whiteshadow", "ii8we_");
        List<String> firstClientRoster = DungeonSyncIdentity.buildTabListRoster(fakepixelProfiles,
                displayedPartyPlayers, Collections.singletonList("whiteshadow"), "ii8we_", 2);
        List<String> secondClientRoster = DungeonSyncIdentity.buildTabListRoster(fakepixelProfiles,
                displayedPartyPlayers, Collections.singletonList("whiteshadow"), "whiteshadow", 2);
        check("two-player party roster uses visible profile rows when class decoration is missing", 
                firstClientRoster.equals(Arrays.asList("ii8we_", "whiteshadow")));
        check("both clients derive the same roster regardless of which local class badge is absent",
                firstClientRoster.equals(secondClientRoster));
        check("both clients derive an identical session fingerprint from those exact profile names",
                DungeonSyncIdentity.createNamedSessionId("sbdung-8", "F7", firstClientRoster).equals(
                        DungeonSyncIdentity.createNamedSessionId("sbdung-8", "F7", secondClientRoster)));
        check("incomplete class-marked roster is not patched with a client-specific local name",
                DungeonSyncIdentity.buildTabListRoster(fakepixelProfiles, Collections.<String>emptyList(),
                        Collections.singletonList("whiteshadow"), "whiteshadow", 2).isEmpty());
        List<String> clientAFromRemoteBadge = DungeonSyncIdentity.buildTabListRoster(fakepixelProfiles,
                fakepixelProfiles, Collections.singletonList("whiteshadow"), "ii8we_", 2);
        List<String> clientBFromRemoteBadge = DungeonSyncIdentity.buildTabListRoster(fakepixelProfiles,
                fakepixelProfiles, Collections.singletonList("ii8we_"), "whiteshadow", 2);
        check("declared two-player party can use local profile plus its one class-marked teammate",
                clientAFromRemoteBadge.equals(Arrays.asList("ii8we_", "whiteshadow")));
        check("both clients build the same roster when each local class badge is omitted",
                clientAFromRemoteBadge.equals(clientBFromRemoteBadge));
        check("both clients hash the same session when each sees only the teammate class badge",
                DungeonSyncIdentity.createNamedSessionId("sbdung-3", "F5", clientAFromRemoteBadge).equals(
                        DungeonSyncIdentity.createNamedSessionId("sbdung-3", "F5", clientBFromRemoteBadge)));
        List<String> offlineModeClientA = DungeonSyncIdentity.buildTabListRoster(
                Arrays.asList("ii8we_", "whiteshadow", "ProfileRow", "Ultimate"),
                Arrays.asList("ii8we_", "whiteshadow"), Collections.singletonList("whiteshadow"), "ii8we_", 2);
        List<String> offlineModeClientB = DungeonSyncIdentity.buildTabListRoster(
                Arrays.asList("ii8we_", "whiteshadow", "ProfileRow", "Ultimate"),
                Arrays.asList("ii8we_", "whiteshadow"), Collections.singletonList("ii8we_"), "whiteshadow", 2);
        check("offline-mode roster uses the usernames provided by the server without UUIDs",
                offlineModeClientA.equals(Arrays.asList("ii8we_", "whiteshadow"))
                        && offlineModeClientB.equals(offlineModeClientA));
        check("offline-mode clients derive the same hash from identical server, floor and usernames",
                DungeonSyncIdentity.createNamedSessionId("sbdung-3", "F5", offlineModeClientA).equals(
                        DungeonSyncIdentity.createNamedSessionId("sbdung-3", "F5", offlineModeClientB)));
        check("offline-mode roster matching remains case-insensitive",
                DungeonSyncIdentity.buildTabListRoster(
                        Arrays.asList("II8WE_", "WHITESHADOW", "ProfileRow"),
                        Arrays.asList("ii8we_", "whiteshadow"), Collections.singletonList("whiteshadow"), "iI8wE_", 2)
                        .equals(Arrays.asList("II8WE_", "WHITESHADOW")));
        check("local plus teammate fallback refuses too many class-marked profiles",
                DungeonSyncIdentity.buildTabListRoster(fakepixelProfiles, fakepixelProfiles,
                        Arrays.asList("whiteshadow", "SyntheticRowOne"), "ii8we_", 2).isEmpty());
        check("declared party size prevents selecting the wrong number of displayed profiles",
                DungeonSyncIdentity.buildTabListRoster(fakepixelProfiles, displayedPartyPlayers,
                        displayedPartyPlayers, "ii8we_", 3).isEmpty());
        check("declared party size uses exact names if profile collection itself matches it",
                DungeonSyncIdentity.buildTabListRoster(Arrays.asList("ii8we_", "whiteshadow"),
                        Collections.<String>emptyList(), Collections.<String>emptyList(), "ii8we_", 2)
                        .equals(Arrays.asList("ii8we_", "whiteshadow")));

        List<String> partyA = Arrays.asList("whiteshadow", "ii8we_", "peropigeon");
        List<String> partyB = Arrays.asList("peropigeon", "WHITESHADOW", "II8WE_");
        String first = DungeonSyncIdentity.createNamedSessionId("sbdung-8", "M7", partyA);
        String reordered = DungeonSyncIdentity.createNamedSessionId("SBDUNG-8", "m7", partyB);
        check("session ID is independent of roster order and name casing", first != null && first.equals(reordered));
        check("session ID is a 64-character SHA-256 value", first != null && first.matches("[0-9a-f]{64}"));
        check("different server produces a different session ID",
                !first.equals(DungeonSyncIdentity.createNamedSessionId("sbdung-9", "M7", partyA)));
        check("different floor produces a different session ID",
                !first.equals(DungeonSyncIdentity.createNamedSessionId("sbdung-8", "F7", partyA)));
        check("exact usernames distinguish players with the same first letter",
                !DungeonSyncIdentity.createNamedSessionId("sbdung-8", "M7", Arrays.asList("AlexOne", "Raven"))
                        .equals(DungeonSyncIdentity.createNamedSessionId("sbdung-8", "M7", Arrays.asList("AlexTwo", "Raven"))));
        check("party membership changes the session ID",
                !first.equals(DungeonSyncIdentity.createNamedSessionId("sbdung-8", "M7", Arrays.asList("whiteshadow", "ii8we_", "otherperson"))));
        check("numeric Minecraft usernames are valid", DungeonSyncIdentity.createNamedSessionId(
                "sbdung-8", "M7", Arrays.asList("300405", "whiteshadow")) != null);
        check("missing server ID fails closed", DungeonSyncIdentity.createNamedSessionId(null, "M7", partyA) == null);
        check("invalid server ID fails closed", DungeonSyncIdentity.createNamedSessionId("play.example.net", "M7", partyA) == null);
        check("missing floor fails closed", DungeonSyncIdentity.createNamedSessionId("sbdung-8", null, partyA) == null);
        check("invalid floor fails closed", DungeonSyncIdentity.createNamedSessionId("sbdung-8", "M8", partyA) == null);
        check("stable mode rejects a single-player session fingerprint",
                DungeonSyncIdentity.createNamedSessionId("sbdung-8", "M7",
                        Collections.singletonList("whiteshadow")) == null);
        check("stable mode accepts the minimum two-player session fingerprint",
                DungeonSyncIdentity.createNamedSessionId("sbdung-8", "M7",
                        Arrays.asList("whiteshadow", "ii8we_")) != null);
        check("more than five players is rejected", DungeonSyncIdentity.createNamedSessionId(
                "sbdung-8", "M7", Arrays.asList("AlphaOne", "BetaTwo", "GammaThree", "DeltaFour", "Epsilon5", "ZetaSix")) == null);
        check("duplicate names are rejected", DungeonSyncIdentity.createNamedSessionId(
                "sbdung-8", "M7", Arrays.asList("whiteshadow", "WHITESHADOW")) == null);
        check("invalid username is rejected", DungeonSyncIdentity.createNamedSessionId(
                "sbdung-8", "M7", Arrays.asList("whiteshadow", "bad username")) == null);
        check("empty roster is rejected", DungeonSyncIdentity.createNamedSessionId(
                "sbdung-8", "M7", Collections.<String>emptyList()) == null);

        System.out.println("DungeonSyncIdentityTests: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static void check(String label, boolean okay) {
        if (okay) passed++;
        else { failed++; System.out.println("FAIL: " + label); }
    }
}
