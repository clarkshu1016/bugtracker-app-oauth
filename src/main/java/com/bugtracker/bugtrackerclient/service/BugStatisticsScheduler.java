package com.bugtracker.bugtrackerclient.service;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.oauth2.client.*;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Component;

@Component
public class BugStatisticsScheduler {

    @Autowired
    private IBugTrackerService trackerServ;

    @Autowired
    ClientRegistrationRepository regRepo;

    @Autowired
    OAuth2AuthorizedClientService authService;


    /* midnight run */
    @Scheduled(cron = "@daily")
    public void dumpStatistics() {
        System.out.println("==> firing dumpStatistics()");

        // Build an OAuth2 request for the Okta provider
        OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest.withClientRegistrationId("bugstat")
                .principal("bugtracker-stats")
                .build();

        // Perform the actual authorization request using the authorized client service and authorized client
        // manager. This is where the JWT is retrieved from Keycloak
        var cliMgr = authorizedClientServiceAndManager(regRepo, authService);
        OAuth2AuthorizedClient authorizedClient = cliMgr.authorize(authorizeRequest);

        // Get the token from the authorized client object
        OAuth2AccessToken token = authorizedClient.getAccessToken();
        System.out.println("Token = " + token.getTokenValue());

        BugStatistics statistics = trackerServ.getBugStatistics(token.getTokenValue());
        System.out.printf("Open : %d, Closed : %d\n",
                statistics.numOpen(), statistics.numClosed());
    }

    public AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientServiceAndManager (
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientService authorizedClientService) {

        OAuth2AuthorizedClientProvider authorizedClientProvider =
                OAuth2AuthorizedClientProviderBuilder.builder()
                        .clientCredentials()
                        .build();

        AuthorizedClientServiceOAuth2AuthorizedClientManager authorizedClientManager =
                new AuthorizedClientServiceOAuth2AuthorizedClientManager(
                        clientRegistrationRepository, authorizedClientService);
        authorizedClientManager.setAuthorizedClientProvider(authorizedClientProvider);

        return authorizedClientManager;
    }

}
