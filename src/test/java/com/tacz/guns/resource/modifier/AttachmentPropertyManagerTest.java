package com.tacz.guns.resource.modifier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * {@code functionEval} runs the {@code function} field of an attachment modifier. The source comes
 * from a gun pack, so it can be anything, and it runs on hot paths such as recoil.
 */
class AttachmentPropertyManagerTest {
    @Test
    void theScriptSeesTheValueAndTheDefault() {
        assertEquals(4.0, AttachmentPropertyManager.functionEval(2, 0, "y = x * 2"));
        assertEquals(9.0, AttachmentPropertyManager.functionEval(0, 9, "y = r"));
    }

    @Test
    void aScriptThatFailsReturnsItsInputRatherThanTheLastResult() {
        assertEquals(4.0, AttachmentPropertyManager.functionEval(2, 0, "y = x * 2"));
        assertEquals(5.0, AttachmentPropertyManager.functionEval(5, 0, "error('boom')"));
    }

    @Test
    void aScriptThatWritesNothingReturnsItsInput() {
        assertEquals(7.0, AttachmentPropertyManager.functionEval(7, 0, "local unused = 1"));
    }

    @Test
    void aScriptThatWritesSomethingOtherThanANumberReturnsItsInput() {
        assertEquals(7.0, AttachmentPropertyManager.functionEval(7, 0, "y = 'not a number'"));
    }

    @Test
    void aScriptThatDoesNotCompileReturnsItsInput() {
        assertEquals(3.0, AttachmentPropertyManager.functionEval(3, 0, "y = ("));
    }

    @Test
    void theSandboxedApisAreNotAvailableToAModifierScript() {
        // os and io are absent, so the call fails and the value passes through untouched
        assertEquals(3.0, AttachmentPropertyManager.functionEval(3, 0, "y = os.time()"));
        assertEquals(3.0, AttachmentPropertyManager.functionEval(3, 0, "y = io.open('build.gradle')"));
    }

    @Test
    void repeatedEvaluationOfTheSameScriptIsStable() {
        for (int i = 1; i <= 5; i++) {
            assertEquals(i * 3.0, AttachmentPropertyManager.functionEval(i, 0, "y = x * 3"));
        }
    }
}
