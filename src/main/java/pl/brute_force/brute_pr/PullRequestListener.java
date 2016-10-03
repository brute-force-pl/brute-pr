package pl.brute_force.brute_pr;

import com.atlassian.bitbucket.build.BuildStatusSetEvent;
import com.atlassian.bitbucket.commit.Commit;
import com.atlassian.bitbucket.event.pull.PullRequestParticipantStatusUpdatedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestRescopedEvent;
import com.atlassian.bitbucket.event.pull.PullRequestUpdatedEvent;
import com.atlassian.bitbucket.permission.Permission;
import com.atlassian.bitbucket.pull.*;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.user.SecurityService;
import com.atlassian.bitbucket.util.Page;
import com.atlassian.bitbucket.util.PageRequestImpl;
import com.atlassian.event.api.EventListener;
import com.atlassian.sal.api.transaction.TransactionTemplate;
import com.google.common.collect.Iterables;
import pl.brute_force.brute_pr.config.Config;
import pl.brute_force.brute_pr.config.ConfigDao;

public class PullRequestListener {
    public static final int MAX_COMMITS = 1048576;

    private final ConfigDao configDao;
    private final PullRequestService prService;
    private final SecurityService securityService;
    private final RegexUtils regexUtils;
    private final TransactionTemplate txTemplate;

    public PullRequestListener(ConfigDao configDao, PullRequestService prService, SecurityService securityService, RegexUtils regexUtils, TransactionTemplate txTemplate) {
        this.configDao = configDao;
        this.prService = prService;
        this.securityService = securityService;
        this.regexUtils = regexUtils;
        this.txTemplate = txTemplate;
    }

    @EventListener
    public void prRescopedListener(PullRequestRescopedEvent event) {
        if (sourceCommitChanged(event)) {
            unapprovePullRequest(event.getPullRequest());
        }
    }

    @EventListener
    public void prUpdatedListener(PullRequestUpdatedEvent event) {
        if (event.getPreviousToBranch() != null) {
            unapprovePullRequest(event.getPullRequest());
        }
    }

    @EventListener
    public void prApprovalListener(PullRequestParticipantStatusUpdatedEvent event) {
        automergePullRequest(event.getPullRequest());
    }

    @EventListener
    public void buildStatusListener(BuildStatusSetEvent event) {
        PullRequest pr = findPRByCommitId(event.getCommitId());
        if (pr != null) {
            automergePullRequest(pr);
        }
    }

    private boolean sourceCommitChanged(PullRequestRescopedEvent event) {
        return !event.getPreviousFromHash().equals(event.getPullRequest().getFromRef().getLatestCommit());
    }

    void automergePullRequest(final PullRequest pr) {
        Repository repo = pr.getToRef().getRepository();
        Config config = configDao.getConfigForRepo(repo.getProject().getKey(), repo.getSlug());
        String toBranch = regexUtils.formatBranchName(pr.getToRef().getId());
        String fromBranch = regexUtils.formatBranchName(pr.getFromRef().getId());

        if ((regexUtils.match(config.getAutomergePRs(), toBranch) || regexUtils.match(config.getAutomergePRsFrom(), fromBranch)) &&
                !regexUtils.match(config.getBlockedPRs(), toBranch) && prService.canMerge(repo.getId(), pr.getId()).canMerge()) {
            securityService.withPermission(Permission.ADMIN, "Automerging pull request").call(() -> prService.merge(new PullRequestMergeRequest.Builder(pr).build()));
        }
    }

    private void unapprovePullRequest(PullRequest pr) {
        Repository repo = pr.getToRef().getRepository();
        Config config = configDao.getConfigForProject(repo.getProject().getKey());
        if (Boolean.TRUE.equals(config.getAutoUnapprove())) {
            txTemplate.execute(() -> {
                for (PullRequestParticipant participant : Iterables.concat(pr.getReviewers(),
                        pr.getParticipants())) {
                    securityService.impersonating(participant.getUser(), "Unapproving pull-request on behalf of user")
                            .call(() -> prService.withdrawApproval(
                                    pr.getToRef().getRepository().getId(),
                                    pr.getId()
                            ));
                }
                return null;
            });
        }
    }

    PullRequest findPRByCommitId(String commitId) {
        int start = 0;
        Page<PullRequest> requests = null;
        while (requests == null || requests.getSize() > 0) {
            requests = prService.search(new PullRequestSearchRequest.Builder()
                    .state(PullRequestState.OPEN)
                    .build(), new PageRequestImpl(start, 10));
            for (PullRequest pr : requests.getValues()) {
                Page<Commit> commits = prService.getCommits(pr.getToRef().getRepository().getId(), pr.getId(), new PageRequestImpl(0, MAX_COMMITS));
                for (Commit c : commits.getValues()) {
                    if (c.getId().equals(commitId)) {
                        return pr;
                    }
                }
            }
            start += 10;
        }
        return null;
    }
}
