package fr.mmarie.core.gitlab;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import fr.mmarie.api.gitlab.User;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import retrofit.Response;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static fr.mmarie.Assertions.assertThat;
import static io.dropwizard.testing.FixtureHelpers.fixture;
import static org.assertj.core.api.Assertions.assertThat;

public class GitLabServiceTestIT {

    private GitLabService gitLabService;

    public static final int PORT = 1339;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(PORT);

    private String privateToken = "N1bJ4n8-rbFAEf8Syrh2";
    private GitLabConfiguration gitLabConfiguration = new GitLabConfiguration(privateToken,
            String.format("http://localhost:%d", PORT));

    @Before
    public void setUp() throws Exception {
        gitLabService = new GitLabService(gitLabConfiguration);
    }

    @Test
    public void testGetUser() throws Exception {
        Long userId = 1L;
        String mockedUser = fixture("fixtures/gitlab/user.json");

        wireMockRule.stubFor(get(urlEqualTo("/api/v3/users/" + userId + "?private_token=" + privateToken))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withBody(mockedUser)));

        Response<User> response = gitLabService.getUser(userId);

        assertThat(response.code())
                .isEqualTo(200);

        assertThat(response.body())
                .hasId(1L)
                .hasUsername("john_smith")
                .hasName("John Smith");

        wireMockRule.verify(getRequestedFor(urlEqualTo("/api/v3/users/" + userId + "?private_token=" + privateToken)));
    }

    @Test
    public void extractIssuesFromMessageWithoutIssue() throws Exception {
        String message = "test: no issue";

        final List<String> issues = gitLabService.extractIssuesFromMessage(message);

        assertThat(issues)
                .hasSize(0);
    }

    @Test
    public void extractIssuesFromMessageWithOneIssue() throws Exception {
        String message = "test(#TEST-1): single issue";

        final List<String> issues = gitLabService.extractIssuesFromMessage(message);

        assertThat(issues)
                .hasSize(1)
                .containsExactly("TEST-1");
    }

    @Test
    public void extractIssuesFromMessageWithMoreThanOneIssue() throws Exception {
        String message = "test(#TEST-1): issue related to #TEST-15289";

        final List<String> issues = gitLabService.extractIssuesFromMessage(message);

        assertThat(issues)
                .hasSize(2)
                .containsExactly("TEST-1", "TEST-15289");
    }
}