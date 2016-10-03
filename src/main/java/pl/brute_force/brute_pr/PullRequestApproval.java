package pl.brute_force.brute_pr;

import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.pull.PullRequestParticipant;
import com.atlassian.bitbucket.pull.PullRequestParticipantStatus;
import pl.brute_force.brute_pr.config.Config;

import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.google.common.collect.Iterables.concat;
import static com.google.common.collect.Maps.uniqueIndex;
import static com.google.common.collect.Sets.difference;
import static com.google.common.collect.Sets.newHashSet;

public class PullRequestApproval {

    private final Config config;
    private final UserUtils utils;

    public PullRequestApproval(Config config, UserUtils utils) {
        this.config = config;
        this.utils = utils;
    }

    public boolean isPullRequestApproved(PullRequest pr) {
        Integer requiredReviews = config.getRequiredReviews();
        return (requiredReviews == null || seenReviewers(pr).size() >= requiredReviews) && !isBlockedByRequiredReviewer(pr);
    }

    public boolean isBlockedByRequiredReviewer(PullRequest pr) {
        return Boolean.TRUE.equals(config.getBlockByRequiredReviewer()) && blockedReviewers(pr).size() > 0;
    }

    public Set<String> blockedReviewers(PullRequest pr) {
        Map<String, PullRequestParticipant> map = transformReviewers(pr);
        Set<String> blockingReviewers = newHashSet();

        for (String req : concat(config.getRequiredReviewers(), utils.dereferenceGroups(config.getRequiredReviewerGroups()))) {
            if (reviewerIsBlocking(map.get(req))) {
                blockingReviewers.add(req);
            }
        }
        return blockingReviewers;
    }

    public Set<String> missingRevieiwers(PullRequest pr) {
        Map<String, PullRequestParticipant> map = transformReviewers(pr);
        Set<String> missingReviewers = newHashSet();

        for (String req : concat(config.getRequiredReviewers(), utils.dereferenceGroups(config.getRequiredReviewerGroups()))) {
            if (reviewerIsMissing(map.get(req)) && !(submitterIsRequiredReviewer(pr, req) && exactlyEnoughRequiredReviewers())) {
                missingReviewers.add(req);
            }
        }
        return missingReviewers;
    }

    public Set<String> missingRevieiwersNames(PullRequest pr) {
        Map<String, PullRequestParticipant> map = transformReviewers(pr);
        Set<String> missingReviewers = newHashSet();

        for (String req : concat(config.getRequiredReviewers(), utils.dereferenceGroups(config.getRequiredReviewerGroups()))) {
            if (reviewerIsMissing(map.get(req)) && !(submitterIsRequiredReviewer(pr, req) && exactlyEnoughRequiredReviewers())) {
                missingReviewers.add(utils.getUserDisplayNameByName(req));
            }
        }
        return missingReviewers;
    }

    public Set<String> seenReviewers(PullRequest pr) {
        Set<String> required = newHashSet(concat(config.getRequiredReviewers(), utils.dereferenceGroups(config.getRequiredReviewerGroups())));
        return difference(required, missingRevieiwers(pr));
    }

    Map<String, PullRequestParticipant> transformReviewers(PullRequest pr) {
        return uniqueIndex(pr.getReviewers(), input -> {
            return input.getUser().getSlug();
        });
    }

    boolean reviewerIsBlocking(PullRequestParticipant reviewer) {
        return reviewer != null && reviewer.getStatus() == PullRequestParticipantStatus.NEEDS_WORK;
    }

    boolean reviewerIsMissing(PullRequestParticipant reviewer) {
        return reviewer == null || !reviewer.isApproved();
    }

    boolean submitterIsRequiredReviewer(PullRequest pr, String username) {
        return pr.getAuthor().getUser().getSlug().equals(username);
    }

    boolean exactlyEnoughRequiredReviewers() {
        return Objects.equals(config.getRequiredReviewers().size(), config.getRequiredReviews());
    }
}
