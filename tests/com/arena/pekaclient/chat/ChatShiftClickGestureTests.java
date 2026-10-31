package com.arena.pekaclient.chat;

/** Regression cases for Shift-left-click copy gesture capture and release matching. */
public final class ChatShiftClickGestureTests {
    private static int passed;
    private static int failed;

    public static void main(String[] args) {
        sameMessageCopies();
        releaseOutsideOrOnDifferentMessageIsConsumed();
        otherMouseButtonsDoNotFinishLeftGesture();
        resetClearsStaleGestures();
        System.out.println("chat shift-click gesture: passed " + passed + ", failed " + failed);
        if (failed > 0) System.exit(1);
    }

    private static void sameMessageCopies() {
        ChatShiftClickGesture gesture = new ChatShiftClickGesture();
        check("left shift press arms a real message", gesture.begin("msg-12") && gesture.pending());
        check("left release on same message requests copy", gesture.release(0, "msg-12") == ChatShiftClickGesture.COPY);
        check("successful gesture is consumed once", !gesture.pending()
                && gesture.release(0, "msg-12") == ChatShiftClickGesture.PASS);
    }

    private static void releaseOutsideOrOnDifferentMessageIsConsumed() {
        ChatShiftClickGesture outside = new ChatShiftClickGesture();
        outside.begin("msg-12");
        check("release outside message is consumed without copying", outside.release(0, null) == ChatShiftClickGesture.CONSUME
                && !outside.pending());

        ChatShiftClickGesture other = new ChatShiftClickGesture();
        other.begin("msg-12");
        check("release over a different message cannot copy its contents",
                other.release(0, "msg-13") == ChatShiftClickGesture.CONSUME && !other.pending());
    }

    private static void otherMouseButtonsDoNotFinishLeftGesture() {
        ChatShiftClickGesture gesture = new ChatShiftClickGesture();
        gesture.begin("msg-12");
        check("right-button release does not consume left gesture", gesture.release(1, "msg-12")
                == ChatShiftClickGesture.PASS && gesture.pending());
        check("matching left release still completes afterward", gesture.release(0, "msg-12")
                == ChatShiftClickGesture.COPY);
    }

    private static void resetClearsStaleGestures() {
        ChatShiftClickGesture gesture = new ChatShiftClickGesture();
        gesture.begin("msg-12");
        gesture.reset();
        check("screen-close reset prevents stale copy", !gesture.pending()
                && gesture.release(0, "msg-12") == ChatShiftClickGesture.PASS);
        check("empty group IDs cannot arm copy", !gesture.begin("") && !gesture.pending());
    }

    private static void check(String name, boolean ok) {
        if (ok) passed++;
        else { failed++; System.err.println("FAIL " + name); }
    }
}