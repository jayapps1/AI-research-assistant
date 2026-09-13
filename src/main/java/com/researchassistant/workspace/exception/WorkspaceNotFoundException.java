package com.researchassistant.workspace.exception;

import com.researchassistant.common.exception.ResourceNotFoundException;

public class WorkspaceNotFoundException extends ResourceNotFoundException {

    public WorkspaceNotFoundException() {
        super("Workspace not found.");
    }
}
