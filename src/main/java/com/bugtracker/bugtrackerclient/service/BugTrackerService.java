package com.bugtracker.bugtrackerclient.service;

import jakarta.annotation.PostConstruct;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class BugTrackerService implements IBugTrackerService {

    private static final List<String> INITIAL_APPS
            = List.of("App1", "App2", "App3", "App4", "App5");

    private final AtomicReference<BugTrackerConfiguration> trackerConfig
            = new AtomicReference<>(new BugTrackerConfiguration(INITIAL_APPS));

    private final AtomicLong idGenerator = new AtomicLong();

    private final  List<Bug> bugs = Collections.synchronizedList(new ArrayList<>());

    @PostConstruct
    public void initialize() {
        long id = idGenerator.getAndIncrement();
        Bug b1 = new Bug(id,"johndoe", "Test does not work", "test does not work", "App1", Bug.BugSeverity.MAJOR, Bug.BugState.OPEN);
        bugs.add(b1);

        long id2 = idGenerator.getAndIncrement();
        Bug b2 = new Bug(id2,"johndoe", "Integration does not work 1", "Integration does not work", "App2", Bug.BugSeverity.MAJOR, Bug.BugState.CLOSED);
        bugs.add(b2);

        long id3 = idGenerator.getAndIncrement();
        Bug b3 = new Bug(id3,"johndoe", "Nullpointer exception when 0 passed", "Nullpointer exception when 0 passed", "App3", Bug.BugSeverity.CRITICAL, Bug.BugState.CLOSED);
        bugs.add(b3);

        long id4 = idGenerator.getAndIncrement();
        Bug b4 = new Bug(id4,"johndoe", "coredump in certain situations", "coredump in certain situations", "App4", Bug.BugSeverity.MINOR, Bug.BugState.CLOSED);
        bugs.add(b4);

    }

    @Override
    public Bug createBug(Bug bug) {

        // extract the user of the Application
        OAuth2AuthenticationToken token
            = (OAuth2AuthenticationToken)SecurityContextHolder.getContext().getAuthentication();
        OidcUser principal = (OidcUser)token.getPrincipal();

        // create a new Bug
        long id = idGenerator.getAndIncrement();
        Bug clonedBug = new Bug(
                            id,
                            principal.getPreferredUsername(),
                            bug.headline(),
                            bug.description(),
                            bug.project(),
                            bug.severity(),
                            bug.state());
        bugs.add(clonedBug);
        return clonedBug;
    }

    @Override
    public Bug updateBug(Bug bug) {

        int index = IntStream.range(0, this.bugs.size())
                .filter(i -> this.bugs.get(i).id().equals(bug.id()))
                .findFirst()
                .orElseThrow();

        this.bugs.set(index, bug);
        return bug;
    }

    @Override
    public List<Bug> findAllBugs() {
        return List.copyOf(this.bugs);
    }

    @Override
    public boolean deleteBug(Long bugId) {
        return this.bugs.removeIf(bug -> bug.id().equals(bugId));
    }

    @Override
    public Bug getBug(Long id) {
        return this.bugs.stream()
                .filter(b -> b.id().equals(id))
                .findFirst()
                .orElseThrow();
    }

    @Override
    public BugTrackerConfiguration getConfiguration() {
        return trackerConfig.get();
    }

    @Override
    public void addProject(String newProject) {
        BugTrackerConfiguration configuration = trackerConfig.get();
        List<String> projects = configuration.projects();

        List<String> newApps = new ArrayList<>();
        newApps.add(newProject);
        newApps.addAll(projects);
        trackerConfig.set(new BugTrackerConfiguration(newApps));
    }

    @Override
    public void removeProject(String project) {

        BugTrackerConfiguration configuration = trackerConfig.get();
        List<String> applications = configuration.projects();
        List<String> newAppList
                = applications.stream().filter(a -> !a.equals(project)).toList();
        trackerConfig.set(new BugTrackerConfiguration(newAppList));
    }

    @Override
    public BugStatistics getBugStatistics(String token) {
        List<Bug> allBugs = findAllBugs();

        Map<Bug.BugState, Long> countsMap
                = allBugs.stream().collect(Collectors.groupingBy(
                Bug::state, Collectors.counting()));

        return new BugStatistics(
                countsMap.get(Bug.BugState.OPEN),
                countsMap.get(Bug.BugState.CLOSED));
    }

}
