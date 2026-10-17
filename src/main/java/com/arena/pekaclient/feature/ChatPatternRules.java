package com.arena.pekaclient.feature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Compiled, user-editable classification rules for Chat QoL.
 *
 * Rules are compiled only when the configuration changes, never while an incoming message is being rendered. An
 * invalid expression is deliberately ignored rather than making chat unusable. The simple target names are kept
 * separate from Minecraft classes so the editor and headless tests can use exactly the same parser.
 */
public final class ChatPatternRules {

    public static final String CHAT_1 = "CHAT1";
    public static final String CHAT_2 = "CHAT2";
    public static final int NO_MATCH = -1;

    /** One persisted editor rule. The pattern is matched against clean, colour-free message text. */
    public static final class Rule {
        public final String target;
        public final String expression;

        public Rule(String target, String expression) {
            this.target = normalizeTarget(target);
            this.expression = expression == null ? "" : expression.trim();
        }

        public boolean isUsable() {
            return expression.length() > 0 && expression.length() <= 256;
        }
    }

    private final Pattern[] patterns;
    private final int[] targets;
    private final List<Rule> accepted;

    /** Compiles a bounded snapshot of rules. The caller may safely mutate its source list afterwards. */
    public ChatPatternRules(List<Rule> source) {
        List<Pattern> compiled = new ArrayList<Pattern>();
        List<Integer> kinds = new ArrayList<Integer>();
        List<Rule> good = new ArrayList<Rule>();
        if (source != null) {
            int limit = Math.min(16, source.size());
            for (int i = 0; i < limit; i++) {
                Rule rule = source.get(i);
                if (rule == null || !rule.isUsable()) {
                    continue;
                }
                try {
                    Pattern pattern = Pattern.compile(rule.expression, Pattern.CASE_INSENSITIVE);
                    compiled.add(pattern);
                    kinds.add(CHAT_1.equals(rule.target) ? ChatFilterRules.PLAYER : ChatFilterRules.SYSTEM);
                    good.add(new Rule(rule.target, rule.expression));
                } catch (PatternSyntaxException ignored) {
                    // Bad editable input is inert. It is still preserved by the settings model for correction.
                }
            }
        }
        patterns = compiled.toArray(new Pattern[compiled.size()]);
        targets = new int[kinds.size()];
        for (int i = 0; i < targets.length; i++) {
            targets[i] = kinds.get(i);
        }
        accepted = Collections.unmodifiableList(good);
    }

    /** Returns PLAYER or SYSTEM for the first matching rule, otherwise {@link #NO_MATCH}. */
    public int classify(String cleanText) {
        String value = cleanText == null ? "" : cleanText;
        for (int i = 0; i < patterns.length; i++) {
            if (patterns[i].matcher(value).matches()) {
                return targets[i];
            }
        }
        return NO_MATCH;
    }

    /** True only when this exact expression currently compiles; used by the editor's Test Pattern action. */
    public static boolean isValid(String expression) {
        if (expression == null || expression.trim().length() == 0 || expression.trim().length() > 256) {
            return false;
        }
        try {
            Pattern.compile(expression.trim(), Pattern.CASE_INSENSITIVE);
            return true;
        } catch (PatternSyntaxException ignored) {
            return false;
        }
    }

    public List<Rule> acceptedRules() {
        return accepted;
    }

    public static String normalizeTarget(String target) {
        return CHAT_2.equalsIgnoreCase(target) ? CHAT_2 : CHAT_1;
    }
}
