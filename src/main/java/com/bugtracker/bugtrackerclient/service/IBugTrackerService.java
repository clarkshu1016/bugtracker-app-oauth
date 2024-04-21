package com.bugtracker.bugtrackerclient.service;

import java.util.List;

public interface IBugTrackerService {

    Bug createBug(Bug bug);
    Bug updateBug(Bug bug);
    List<Bug> findAllBugs();
    Bug getBug(Long id);
    boolean deleteBug(Long bugId);
    BugTrackerConfiguration getConfiguration();
    void addProject(String newProject);
    void removeProject(String project);
    BugStatistics getBugStatistics(String token);
}
