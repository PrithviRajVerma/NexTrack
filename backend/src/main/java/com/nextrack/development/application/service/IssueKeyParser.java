package com.nextrack.development.application.service;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class IssueKeyParser {

    private static final Pattern ISSUE_KEY_PATTERN = Pattern.compile("\\b([A-Z][A-Z0-9]{1,9})-(\\d+)\\b");

    private IssueKeyParser() {}

    public record ParsedIssueKey(String projectKey, long issueNumber, String formattedKey) {
        public ParsedIssueKey(String projectKey, long issueNumber) {
            this(projectKey.toUpperCase(), issueNumber, projectKey.toUpperCase() + "-" + issueNumber);
        }
    }

    /**
     * Extracts uppercase issue keys conforming to \b([A-Z][A-Z0-9]{1,9})-(\d+)\b
     * from branch names, commit messages, and PR titles/descriptions.
     */
    public static Set<ParsedIssueKey> extractIssueKeys(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptySet();
        }

        Set<ParsedIssueKey> keys = new LinkedHashSet<>();
        Matcher matcher = ISSUE_KEY_PATTERN.matcher(text);
        while (matcher.find()) {
            try {
                String projectKey = matcher.group(1).toUpperCase();
                long issueNumber = Long.parseLong(matcher.group(2));
                keys.add(new ParsedIssueKey(projectKey, issueNumber));
            } catch (NumberFormatException ignored) {
                // Protect against extreme values that overflow long
            }
        }
        return Collections.unmodifiableSet(keys);
    }
}
