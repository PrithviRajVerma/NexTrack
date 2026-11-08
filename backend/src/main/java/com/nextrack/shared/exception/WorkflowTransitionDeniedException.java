package com.nextrack.shared.exception;

public class WorkflowTransitionDeniedException extends RuntimeException {

    public WorkflowTransitionDeniedException(String message) {
        super(message);
    }
}
