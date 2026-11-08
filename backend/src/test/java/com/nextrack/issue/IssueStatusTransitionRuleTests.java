package com.nextrack.issue;

import com.nextrack.issue.domain.entity.IssueStatus;
import com.nextrack.issue.domain.workflow.IssueStatusTransitionRule;
import com.nextrack.shared.exception.WorkflowTransitionDeniedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IssueStatusTransitionRuleTests {

    @ParameterizedTest(name = "Permitted: {0} -> {1}")
    @CsvSource({
            "BACKLOG, TO_DO",
            "BACKLOG, ARCHIVED",
            "TO_DO, BACKLOG",
            "TO_DO, IN_PROGRESS",
            "IN_PROGRESS, TO_DO",
            "IN_PROGRESS, IN_REVIEW",
            "IN_REVIEW, IN_PROGRESS",
            "IN_REVIEW, DONE",
            "DONE, IN_REVIEW",
            "DONE, IN_PROGRESS",
            "DONE, ARCHIVED"
    })
    @DisplayName("Permitted transitions must be allowed by FSM rule")
    void shouldAllowPermittedTransitions(IssueStatus from, IssueStatus to) {
        assertThat(IssueStatusTransitionRule.isTransitionAllowed(from, to))
                .as("Transition from %s to %s should be permitted", from, to)
                .isTrue();

        assertThatCode(() -> IssueStatusTransitionRule.validateTransition(from, to))
                .doesNotThrowAnyException();
    }

    @ParameterizedTest(name = "Disallowed: {0} -> {1}")
    @CsvSource({
            "BACKLOG, DONE",
            "BACKLOG, IN_PROGRESS",
            "BACKLOG, IN_REVIEW",
            "TO_DO, DONE",
            "TO_DO, IN_REVIEW",
            "TO_DO, ARCHIVED",
            "IN_PROGRESS, DONE",
            "IN_PROGRESS, BACKLOG",
            "IN_PROGRESS, ARCHIVED",
            "IN_REVIEW, TO_DO",
            "IN_REVIEW, BACKLOG",
            "IN_REVIEW, ARCHIVED",
            "ARCHIVED, BACKLOG",
            "ARCHIVED, TO_DO",
            "ARCHIVED, IN_PROGRESS",
            "ARCHIVED, IN_REVIEW",
            "ARCHIVED, DONE",
            "ARCHIVED, ARCHIVED",
            "BACKLOG, BACKLOG",
            "TO_DO, TO_DO",
            "IN_PROGRESS, IN_PROGRESS",
            "IN_REVIEW, IN_REVIEW",
            "DONE, DONE"
    })
    @DisplayName("Disallowed transitions must throw WorkflowTransitionDeniedException")
    void shouldDenyDisallowedTransitions(IssueStatus from, IssueStatus to) {
        assertThat(IssueStatusTransitionRule.isTransitionAllowed(from, to))
                .as("Transition from %s to %s should be denied", from, to)
                .isFalse();

        assertThatThrownBy(() -> IssueStatusTransitionRule.validateTransition(from, to))
                .isInstanceOf(WorkflowTransitionDeniedException.class)
                .hasMessageContaining("not permitted by the workflow state machine");
    }

    @Test
    @DisplayName("Null transitions must be denied safely")
    void shouldDenyNullTransitions() {
        assertThat(IssueStatusTransitionRule.isTransitionAllowed(null, IssueStatus.DONE)).isFalse();
        assertThat(IssueStatusTransitionRule.isTransitionAllowed(IssueStatus.BACKLOG, null)).isFalse();
        assertThat(IssueStatusTransitionRule.isTransitionAllowed(null, null)).isFalse();

        assertThatThrownBy(() -> IssueStatusTransitionRule.validateTransition(null, IssueStatus.DONE))
                .isInstanceOf(WorkflowTransitionDeniedException.class);
    }
}
