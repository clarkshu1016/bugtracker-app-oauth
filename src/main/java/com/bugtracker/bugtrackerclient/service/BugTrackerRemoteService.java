package com.bugtracker.bugtrackerclient.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service
@Primary
public class BugTrackerRemoteService implements IBugTrackerService {

    // Root URL for API
    @Value("${bugtracker.api.url}")
    private String API_URL;

    @Autowired
    private OAuth2AuthorizedClientService azdCliService;

    private final RestClient apiClient = RestClient.create();

    @Override
    public Bug createBug(Bug bug) {
        String token = getAccessToken();
        return apiClient
                .post()
                .uri(API_URL)
                .header("Authorization", "bearer " + token)
                .body(bug)
                .retrieve()
                .body(Bug.class);
    }

    @Override
    public Bug updateBug(Bug bug) {
        String token = getAccessToken();
        return apiClient
                .put()
                .uri(API_URL)
                .header("Authorization", "bearer " + token)
                .body(bug)
                .retrieve()
                .body(Bug.class);
    }

    @Override
    public List<Bug> findAllBugs() {

        String token = getAccessToken();
        return apiClient
                .get()
                .uri(API_URL)
                .header("Authorization", "bearer " + token)
                .retrieve()
                .body(new ParameterizedTypeReference<List<Bug>>() {});
    }


    @Override
    public Bug getBug(Long id) {
        String token = getAccessToken();
        return apiClient
                .get()
                .uri(API_URL + "/{id}",id)
                .header("Authorization", "bearer " + token)
                .retrieve()
                .body(Bug.class);
    }

    @Override
    public boolean deleteBug(Long bugId) {
        String token = getAccessToken();
        return apiClient
                .delete()
                .uri(API_URL + "/{id}",bugId)
                .header("Authorization", "bearer " + token)
                .retrieve()
                .body(Boolean.class);
    }

    @Override
    public BugTrackerConfiguration getConfiguration() {
        String token = getAccessToken();
        return apiClient
                .get()
                .uri(API_URL + "/configuration")
                .header("Authorization", "bearer " + token)
                .retrieve()
                .body(BugTrackerConfiguration.class);
    }

    @Override
    public void addProject(String newProject) {

        String token = getAccessToken();
        apiClient
                .post()
                .uri(API_URL + "/administration/project/{project}", newProject)
                .header("Authorization", "bearer " + token)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public void removeProject(String project) {

        String token = getAccessToken();
        apiClient
                .delete()
                .uri(API_URL + "/administration/project/{project}", project)
                .header("Authorization", "bearer " + token)
                .retrieve()
                .toBodilessEntity();
    }

    @Override
    public BugStatistics getBugStatistics(String token) {
        return apiClient
                .get()
                .uri(API_URL + "/statistics")
                .header("Authorization", "bearer " + token)
                .retrieve()
                .body(BugStatistics.class);
    }

    private String getAccessToken() {
        var authn = (OAuth2AuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
        String authzdId = authn.getAuthorizedClientRegistrationId();
        String name = authn.getName();

        OAuth2AuthorizedClient authzdCli = azdCliService.loadAuthorizedClient(authzdId, name);
        OAuth2AccessToken token = authzdCli.getAccessToken();

        String tokenValue = token.getTokenValue();
        System.out.println("** TOKEN = " + tokenValue);

        return tokenValue;
    }

}
