package com.arena.pekaclient.chat;

/** Fakepixel-style sender fixtures for the stored-message "Message player" menu action. */
public final class ChatPlayerNameTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        fixture("[MVP+] Alex_99: hello", "Alex_99");
        fixture("[VIP] Bob123: anyone selling?", "Bob123");
        fixture("[123] [MVP++] Sky_User: party up", "Sky_User");
        fixture("Party > [MVP+] PartyGuy: ready", "PartyGuy");
        fixture("Party > ✦ [321] User_Name: go", "User_Name");
        fixture("From [VIP] Sender_9: hi there", "Sender_9");
        fixture("To [MVP+] Target12: thanks", "Target12");
        fixture("[Guild] [MVP] GuildMate: welcome", "GuildMate");
        fixture("✦ [456] [MVP++] Star_Player: gg", "Star_Player");
        fixture("From [777] Emoji✦User_2: hello", "User_2");
        fixture("Name_1234: normal unranked line", "Name_1234");
        fixture("[TAG] [777] Numb3r_Name: test", "Numb3r_Name");
        fixture("Party > [VIP+] Player_007: joined", "Player_007");
        fixture("From [MVP] LastOne_16: test", "LastOne_16");
        fixture("[LEVEL 99] PlayerName: level badge text", "PlayerName");
        fixture("You sold this for 1 coin", "");
        fixture("Server: Restarting soon", "");
        fixture("System: You received a reward", "");
        fixture("Auction House: Your auction sold", "");
        fixture("Al: too short", "");
        fixture("ThisNameIsTooLong17: too long", "");
        done();
    }

    private static void fixture(String line, String expected) {
        String actual = ChatPlayerName.extract(line);
        check("sender fixture " + line, expected.equals(actual));
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++; else { failed++; System.err.println("FAIL " + name); }
    }

    private static void done() {
        System.out.println("chat player names: passed " + passed + ", failed " + failed);
        if (failed > 0) throw new AssertionError("chat player-name failures: " + failed);
    }
}
