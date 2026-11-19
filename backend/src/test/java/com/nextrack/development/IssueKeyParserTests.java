package com.nextrack.development;

import com.nextrack.development.application.service.IssueKeyParser;
import com.nextrack.development.application.service.IssueKeyParser.ParsedIssueKey;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class IssueKeyParserTests {

    @Test
    @DisplayName("Extracts issue keys from commit message")
    void extractsKeysFromCommitMessage() {
        String msg = "NT-101: Fix authentication bug and close NT-102";
        Set<ParsedIssueKey> keys = IssueKeyParser.extractIssueKeys(msg);

        assertThat(keys).containsExactly(
                new ParsedIssueKey("NT", 101L),
                new ParsedIssueKey("NT", 102L)
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "feature/NT-101-oauth2-login",
            "bugfix/PROJ99-42",
            "NT-1-initial-commit",
            "release/v1.0.0-NT-55"
    })
    @DisplayName("Extracts issue keys from branch names")
    void extractsKeysFromBranchNames(String branchName) {
        Set<ParsedIssueKey> keys = IssueKeyParser.extractIssueKeys(branchName);
        assertThat(keys).isNotEmpty();
    }

    @Test
    @DisplayName("Extracts issue keys from PR title and body")
    void extractsKeysFromPullRequest() {
        String title = "[NT-404] Add custom error page";
        String body = "Resolves NT-404. Also related to BACKEND-12 and NT-405.";

        Set<ParsedIssueKey> titleKeys = IssueKeyParser.extractIssueKeys(title);
        Set<ParsedIssueKey> bodyKeys = IssueKeyParser.extractIssueKeys(body);

        assertThat(titleKeys).extracting(ParsedIssueKey::formattedKey).containsExactly("NT-404");
        assertThat(bodyKeys).extracting(ParsedIssueKey::formattedKey)
                .containsExactly("NT-404", "BACKEND-12", "NT-405");
    }

    @Test
    @DisplayName("Handles null and blank inputs gracefully")
    void handlesNullAndBlank() {
        assertThat(IssueKeyParser.extractIssueKeys(null)).isEmpty();
        assertThat(IssueKeyParser.extractIssueKeys("")).isEmpty();
        assertThat(IssueKeyParser.extractIssueKeys("   ")).isEmpty();
    }

    @Test
    @DisplayName("Ignores non-matching strings and lowercase prefixes")
    void ignoresNonMatchingStrings() {
        String text = "nt-101 is lowercase, 123-45 is numbers, NT- is incomplete, TOOLONGA99999-1 is over 10 chars";
        Set<ParsedIssueKey> keys = IssueKeyParser.extractIssueKeys(text);
        assertThat(keys).isEmpty();
    }
}
