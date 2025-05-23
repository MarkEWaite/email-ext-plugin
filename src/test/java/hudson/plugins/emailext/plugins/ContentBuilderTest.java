package hudson.plugins.emailext.plugins;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.ListMultimap;
import hudson.EnvVars;
import hudson.model.AbstractBuild;
import hudson.model.BuildListener;
import hudson.model.StreamBuildListener;
import hudson.model.TaskListener;
import hudson.plugins.emailext.ExtendedEmailPublisher;
import hudson.plugins.emailext.ExtendedEmailPublisherContext;
import hudson.plugins.emailext.ExtendedEmailPublisherDescriptor;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
import org.jenkinsci.plugins.tokenmacro.TokenMacro;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class ContentBuilderTest {

    private ExtendedEmailPublisher publisher;
    private BuildListener listener;
    private AbstractBuild<?, ?> build;

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule j) throws Exception {
        this.j = j;

        listener = new StreamBuildListener(System.out, StandardCharsets.UTF_8);

        publisher = new ExtendedEmailPublisher();
        publisher.defaultContent = "For only 10 easy payment of $69.99 , AWESOME-O 4000 can be yours!";
        publisher.defaultSubject = "How would you like your very own AWESOME-O 4000?";
        publisher.recipientList = "ashlux@gmail.com";

        Field f = ExtendedEmailPublisherDescriptor.class.getDeclaredField("defaultBody");
        f.setAccessible(true);
        f.set(publisher.getDescriptor(), "Give me $4000 and I'll mail you a check for $40,000!");
        f = ExtendedEmailPublisherDescriptor.class.getDeclaredField("defaultSubject");
        f.setAccessible(true);
        f.set(publisher.getDescriptor(), "Nigerian needs your help!");

        f = ExtendedEmailPublisherDescriptor.class.getDeclaredField("recipientList");
        f.setAccessible(true);
        f.set(publisher.getDescriptor(), "ashlux@gmail.com");

        build = mock(AbstractBuild.class);
        when(build.getEnvironment(listener)).thenReturn(new EnvVars());
    }

    @Test
    void testTransformText_shouldExpand_$PROJECT_DEFAULT_CONTENT() {
        assertEquals(
                publisher.defaultContent,
                ContentBuilder.transformText(
                        "$PROJECT_DEFAULT_CONTENT", publisher, build, j.createLocalLauncher(), listener));
        assertEquals(
                publisher.defaultContent,
                ContentBuilder.transformText(
                        "${PROJECT_DEFAULT_CONTENT}", publisher, build, j.createLocalLauncher(), listener));
    }

    @Test
    void testTransformText_shouldExpand_$PROJECT_DEFAULT_SUBJECT() {
        assertEquals(
                publisher.defaultSubject,
                ContentBuilder.transformText("$PROJECT_DEFAULT_SUBJECT", publisher, build, listener));
        assertEquals(
                publisher.defaultSubject,
                ContentBuilder.transformText("${PROJECT_DEFAULT_SUBJECT}", publisher, build, listener));
    }

    @Test
    void testTransformText_shouldExpand_$DEFAULT_CONTENT() {
        assertEquals(
                publisher.getDescriptor().getDefaultBody(),
                ContentBuilder.transformText("$DEFAULT_CONTENT", publisher, build, listener));
        assertEquals(
                publisher.getDescriptor().getDefaultBody(),
                ContentBuilder.transformText("${DEFAULT_CONTENT}", publisher, build, listener));
    }

    @Test
    void testTransformText_shouldExpand_$DEFAULT_SUBJECT() {
        assertEquals(
                publisher.getDescriptor().getDefaultSubject(),
                ContentBuilder.transformText("$DEFAULT_SUBJECT", publisher, build, listener));
        assertEquals(
                publisher.getDescriptor().getDefaultSubject(),
                ContentBuilder.transformText("${DEFAULT_SUBJECT}", publisher, build, listener));
    }

    @Test
    void testTransformText_shouldExpand_$DEFAULT_RECIPIENT_LIST() {
        assertEquals(
                publisher.getDescriptor().getDefaultRecipients(),
                ContentBuilder.transformText("$DEFAULT_RECIPIENTS", publisher, build, listener));
        assertEquals(
                publisher.getDescriptor().getDefaultRecipients(),
                ContentBuilder.transformText("${DEFAULT_RECIPIENTS}", publisher, build, listener));
    }

    @Test
    void testTransformText_shouldExpand_$DEFAULT_PRESEND_SCRIPT() {
        assertEquals(
                publisher.getDescriptor().getDefaultPresendScript(),
                StringUtils.trimToNull(
                        ContentBuilder.transformText("$DEFAULT_PRESEND_SCRIPT", publisher, build, listener)));
        assertEquals(
                publisher.getDescriptor().getDefaultPresendScript(),
                StringUtils.trimToNull(
                        ContentBuilder.transformText("${DEFAULT_PRESEND_SCRIPT}", publisher, build, listener)));
    }

    @Test
    void testTransformText_shouldExpand_$DEFAULT_POSTSEND_SCRIPT() {
        assertEquals(
                publisher.getDescriptor().getDefaultPostsendScript(),
                StringUtils.trimToNull(
                        ContentBuilder.transformText("$DEFAULT_POSTSEND_SCRIPT", publisher, build, listener)));
        assertEquals(
                publisher.getDescriptor().getDefaultPostsendScript(),
                StringUtils.trimToNull(
                        ContentBuilder.transformText("${DEFAULT_POSTSEND_SCRIPT}", publisher, build, listener)));
    }

    @Test
    void testTransformText_noNPEWithNullDefaultSubjectBody() throws NoSuchFieldException, IllegalAccessException {
        Field f = ExtendedEmailPublisherDescriptor.class.getDeclaredField("defaultBody");
        f.setAccessible(true);
        f.set(publisher.getDescriptor(), null);
        f = ExtendedEmailPublisherDescriptor.class.getDeclaredField("defaultSubject");
        f.setAccessible(true);
        f.set(publisher.getDescriptor(), null);
        assertEquals("", ContentBuilder.transformText("$DEFAULT_SUBJECT", publisher, build, listener));
        assertEquals("", ContentBuilder.transformText("$DEFAULT_CONTENT", publisher, build, listener));
    }

    @Test
    void testEscapedToken() throws IOException, InterruptedException {
        build = mock(AbstractBuild.class);
        EnvVars testVars = new EnvVars();
        testVars.put("FOO", "BAR");
        when(build.getEnvironment(listener)).thenReturn(testVars);

        assertEquals("\\BAR", ContentBuilder.transformText("\\${ENV, var=\"FOO\"}", publisher, build, listener));
    }

    @Test
    void testRuntimeMacro() {
        RuntimeContent content = new RuntimeContent("Hello, world");
        assertEquals(
                "Hello, world",
                ContentBuilder.transformText(
                        "${RUNTIME}",
                        new ExtendedEmailPublisherContext(
                                publisher, build, build.getWorkspace(), j.createLocalLauncher(), listener),
                        Collections.singletonList(content)));
    }

    private static class RuntimeContent extends TokenMacro {

        public static final String MACRO_NAME = "RUNTIME";
        private final String replacement;

        public RuntimeContent(String replacement) {
            this.replacement = replacement;
        }

        @Override
        public boolean acceptsMacroName(String name) {
            return name.equals(MACRO_NAME);
        }

        @Override
        public List<String> getAcceptedMacroNames() {
            return Collections.singletonList(MACRO_NAME);
        }

        @Override
        public String evaluate(
                AbstractBuild<?, ?> ab,
                TaskListener tl,
                String string,
                Map<String, String> map,
                ListMultimap<String, String> lm) {
            return replacement;
        }
    }
}
