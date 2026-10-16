package com.arena.pekaclient.feature;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

/** Pure identity tests for rank/class-formatted Spirit Leap labels. */
public final class LeapIdentityTests {

    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        canonicalNamesAreStrict();
        displayPrefixesAreNotIdentity();
        ambiguousDisplayNamesAreRejected();
        matchingDoesNotUseSubstrings();
        uuidKeysAreStableAndDistinct();
        System.out.println();
        System.out.println("leap identity: passed " + passed + ", failed " + failed);
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void canonicalNamesAreStrict() {
        check("plain username", "Dipanshu_ff".equals(LeapIdentity.canonicalUsername(" Dipanshu_ff ")));
        check("formatted name is not an identity", LeapIdentity.canonicalUsername("[VIP] Dipanshu_ff") == null);
        check("too long", LeapIdentity.canonicalUsername("abcdefghijklmnopq") == null);
        check("control characters removed", "Player_7".equals(LeapIdentity.canonicalUsername("Player_\u00007")));
    }

    private static void displayPrefixesAreNotIdentity() {
        check("class and rank prefixes", "Chimpanzee_7".equals(LeapIdentity.usernameFromDisplay(
                "\u00a7b[B] \u00a76[MVP+] \u00a7fChimpanzee_7", Collections.<String>emptyList())));
        check("rank and decorative suffix resolve through tab identity", "Dipanshu_ff".equals(
                LeapIdentity.usernameFromDisplay("[VIP] Dipanshu_ff 42", Arrays.asList("Dipanshu_ff", "AdityaARJ"))));
        check("suffix rank token", "AdityaARJ".equals(LeapIdentity.usernameFromDisplay(
                "AdityaARJ [MVP+]", Arrays.asList("AdityaARJ"))));
        check("duplicate known name is deduplicated", "Player_1".equals(LeapIdentity.usernameFromDisplay(
                "[B] Player_1", Arrays.asList("Player_1", "player_1"))));
    }

    private static void ambiguousDisplayNamesAreRejected() {
        check("two unverified tokens", LeapIdentity.usernameFromDisplay("Alpha Beta", null) == null);
        check("two known username tokens", LeapIdentity.usernameFromDisplay("Alpha Beta",
                Arrays.asList("Alpha", "Beta")) == null);
        check("rank token alone is not identity", LeapIdentity.usernameFromDisplay("[MVP+]", null) == null);
    }

    private static void matchingDoesNotUseSubstrings() {
        check("exact names", LeapIdentity.sameUsername("Player_2", "player_2"));
        check("not substring", !LeapIdentity.sameUsername("Player_22", "Player_2"));
        check("whole token match", LeapIdentity.containsUsername("[B] [VIP] Player_2", "Player_2"));
        check("prefix collision rejected", !LeapIdentity.containsUsername("[B] Player_22", "Player_2"));
        check("rank is not a username match", !LeapIdentity.containsUsername("[VIP] Player_2", "VIP"));
    }

    private static void uuidKeysAreStableAndDistinct() {
        UUID first = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID second = UUID.fromString("00000000-0000-0000-0000-000000000002");
        check("uuid preferred", LeapIdentity.identityKey(first, "Player")
                .equals("uuid:00000000-0000-0000-0000-000000000001"));
        check("different UUIDs are isolated", !LeapIdentity.identityKey(first, "Player")
                .equals(LeapIdentity.identityKey(second, "Player")));
        check("username fallback", "name:player".equals(LeapIdentity.identityKey(null, "Player")));
        check("formatted fallback rejected", LeapIdentity.identityKey(null, "[VIP] Player") == null);
        check("uuid survives a tab-list gap for same username", LeapIdentity.identityKey(null, "Player",
                "uuid:00000000-0000-0000-0000-000000000001", "player")
                .equals("uuid:00000000-0000-0000-0000-000000000001"));
        check("changed username cannot inherit prior uuid", LeapIdentity.identityKey(null, "OtherPlayer",
                "uuid:00000000-0000-0000-0000-000000000001", "Player").equals("name:otherplayer"));
        check("new uuid replaces prior uuid", LeapIdentity.identityKey(UUID.fromString(
                "00000000-0000-0000-0000-000000000002"), "Player",
                "uuid:00000000-0000-0000-0000-000000000001", "Player")
                .equals("uuid:00000000-0000-0000-0000-000000000002"));
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
