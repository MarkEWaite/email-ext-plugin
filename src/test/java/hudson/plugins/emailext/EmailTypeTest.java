package hudson.plugins.emailext;

import static org.junit.jupiter.api.Assertions.*;

import hudson.model.FreeStyleProject;
import hudson.plugins.emailext.plugins.recipients.DevelopersRecipientProvider;
import hudson.plugins.emailext.plugins.recipients.ListRecipientProvider;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jvnet.hudson.test.Issue;
import org.jvnet.hudson.test.JenkinsRule;
import org.jvnet.hudson.test.junit.jupiter.WithJenkins;

@WithJenkins
class EmailTypeTest {

    private JenkinsRule j;

    @BeforeEach
    void setUp(JenkinsRule j) {
        this.j = j;
    }

    @Test
    void testHasNoRecipients() {
        EmailType t = new EmailType();

        assertFalse(t.getHasRecipients());
    }

    @Test
    void testHasDeveloperRecipients() {
        EmailType t = new EmailType();

        t.addRecipientProvider(new DevelopersRecipientProvider());

        assertTrue(t.getHasRecipients());
    }

    @Test
    void testHasRecipientList() {
        EmailType t = new EmailType();

        t.addRecipientProvider(new ListRecipientProvider());

        assertTrue(t.getHasRecipients());
    }

    @Test
    void testHasDeveloperAndRecipientList() {
        EmailType t = new EmailType();

        t.addRecipientProvider(new ListRecipientProvider());
        t.addRecipientProvider(new DevelopersRecipientProvider());

        assertTrue(t.getHasRecipients());
    }

    @Test
    void testCompressBuildAttachment() {
        EmailType t = new EmailType();
        t.setCompressBuildLog(true);

        assertTrue(t.getCompressBuildLog());
    }

    @Test
    void testDefaultCompressBuildAttachment() {
        EmailType t = new EmailType();

        assertFalse(t.getCompressBuildLog());
    }

    @Test
    void testUpgradeToRecipientProvider() throws IOException {
        URL url = this.getClass().getResource("/recipient-provider-upgrade.xml");
        File jobConfig = new File(url.getFile());

        final ExtendedEmailPublisherDescriptor desc =
                j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        FreeStyleProject prj = j.createFreeStyleProject();
        prj.updateByXml((Source) new StreamSource(new FileReader(jobConfig)));

        ExtendedEmailPublisher pub = (ExtendedEmailPublisher) prj.getPublisher(desc);

        // make sure the publisher got picked up
        assertNotNull(pub);

        // make sure the trigger was marshalled
        assertFalse(pub.configuredTriggers.isEmpty());

        // should have developers, requestor and culprits
        assertEquals(
                3,
                pub.configuredTriggers.get(0).getEmail().getRecipientProviders().size());
    }

    @Test
    @Issue("JENKINS-24506")
    void testUpgradeTriggerWithNoRecipients() throws IOException {
        URL url = this.getClass().getResource("/recipient-provider-upgrade2.xml");
        File jobConfig = new File(url.getFile());

        final ExtendedEmailPublisherDescriptor desc =
                j.jenkins.getDescriptorByType(ExtendedEmailPublisherDescriptor.class);
        FreeStyleProject prj = j.createFreeStyleProject();
        prj.updateByXml((Source) new StreamSource(new FileReader(jobConfig)));

        ExtendedEmailPublisher pub = (ExtendedEmailPublisher) prj.getPublisher(desc);

        // make sure the publisher got picked up
        assertNotNull(pub);

        assertNotNull(pub.getConfiguredTriggers().get(0).getEmail().getRecipientProviders());
    }
}
