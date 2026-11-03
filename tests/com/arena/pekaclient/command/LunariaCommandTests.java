package com.arena.pekaclient.command;

/** Exercises the same command object registered with Forge for both names. */
public final class LunariaCommandTests {
    private static void check(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        SvCommand command = new SvCommand();
        check("lunaria".equals(command.getCommandName()), "canonical command");
        check(command.getCommandAliases().contains("velor"), "legacy alias");
        check(command.getCommandUsage(null).contains("/velor"), "help mentions alias");
        String[] routes = {"itemanimation", "ia", "itemdebug", "lockdebug", "espdebug", "espdump", "chateditor", "chatdebug", "layoutdebug"};
        for (String route : routes) {
            check(ItemDebugCommand.isFeatureRoute(route), "route dispatch: " + route);
            check(command.addTabCompletionOptions(null, new String[]{route}, null).contains(route), "completion: " + route);
        }
        check(command.addTabCompletionOptions(null, new String[]{"dungeon", "de"}, null).contains("debug"), "dungeon completion");
        System.out.println("LunariaCommandTests passed");
    }
}
