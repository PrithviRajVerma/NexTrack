package com.nextrack.architecture;

import com.nextrack.NexTrackApplication;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModulithArchitectureTests {

    @Test
    void verifyModularMonolithStructure() {
        ApplicationModules modules = ApplicationModules.of(NexTrackApplication.class);
        modules.verify();
    }
}
