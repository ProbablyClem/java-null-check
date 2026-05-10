package com.example.agent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Basic test to verify the agent class can be loaded.
 * The actual agent functionality is tested in the demo-app integration tests.
 */
class NullCheckAgentTest {

    @Test
    void agentClassShouldExist() {
        assertNotNull(NullCheckAgent.class);
    }

    @Test
    void premainMethodShouldExist() throws NoSuchMethodException {
        assertNotNull(NullCheckAgent.class.getMethod("premain", String.class, java.lang.instrument.Instrumentation.class));
    }

    @Test
    void agentmainMethodShouldExist() throws NoSuchMethodException {
        assertNotNull(NullCheckAgent.class.getMethod("agentmain", String.class, java.lang.instrument.Instrumentation.class));
    }
}
