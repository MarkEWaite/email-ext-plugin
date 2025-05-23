package hudson.plugins.emailext.plugins.content;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import hudson.model.AbstractBuild;
import hudson.model.TaskListener;
import hudson.tasks.test.AbstractTestResultAction;
import hudson.util.StreamTaskListener;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for TestCountsContentTest.
 *
 * @author Seiji Sogabe
 */
class TestCountsContentTest {

    private TestCountsContent target;
    private AbstractBuild<?, ?> build;
    private TaskListener listener;

    @BeforeEach
    void setUp() {
        target = new TestCountsContent();
        build = mock(AbstractBuild.class);
        listener = StreamTaskListener.fromStdout();
    }

    @Test
    void testGetContent_NoTestResults() throws Exception {
        target.var = "total";
        assertEquals("", target.evaluate(build, listener, TestCountsContent.MACRO_NAME));
    }

    /**
     * Verifies that token expansion works for pipeline builds (JENKINS-38519).
     */
    @Test
    void testGetContent_withWorkspaceAndNoTestResults() throws Exception {
        target.var = "total";
        assertEquals("", target.evaluate(build, build.getWorkspace(), listener, TestCountsContent.MACRO_NAME));
    }

    @Test
    void testGetContent() throws Exception {
        AbstractTestResultAction<?> results = mock(AbstractTestResultAction.class);
        when(results.getTotalCount()).thenReturn(5);
        when(results.getTotalCount() - results.getFailCount() - results.getSkipCount())
                .thenReturn(2);
        when(results.getFailCount()).thenReturn(2);
        when(results.getSkipCount()).thenReturn(1);
        when(build.getAction(AbstractTestResultAction.class)).thenReturn(results);

        assertEquals("5", target.evaluate(build, listener, TestCountsContent.MACRO_NAME));
        target.var = "total";
        assertEquals("5", target.evaluate(build, listener, TestCountsContent.MACRO_NAME));
        target.var = "pass";
        assertEquals("2", target.evaluate(build, listener, TestCountsContent.MACRO_NAME));
        target.var = "fail";
        assertEquals("2", target.evaluate(build, listener, TestCountsContent.MACRO_NAME));
        target.var = "skip";
        assertEquals("1", target.evaluate(build, listener, TestCountsContent.MACRO_NAME));
        target.var = "SKIP";
        assertEquals("1", target.evaluate(build, listener, TestCountsContent.MACRO_NAME));
        target.var = "wrongvar";
        assertEquals("", target.evaluate(build, listener, TestCountsContent.MACRO_NAME));
    }
}
