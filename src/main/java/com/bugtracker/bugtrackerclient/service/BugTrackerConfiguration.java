package com.bugtracker.bugtrackerclient.service;

import java.util.List;
import java.util.Objects;

public record BugTrackerConfiguration(List<String> projects) {

        public BugTrackerConfiguration(List<String> projects) {
                Objects.requireNonNull(projects);
                this.projects = List.copyOf(projects);
        }
}
