package pl.brute_force.brute_pr;

import com.atlassian.bitbucket.project.Project;
import com.atlassian.bitbucket.pull.PullRequest;
import com.atlassian.bitbucket.pull.PullRequestParticipant;
import com.atlassian.bitbucket.pull.PullRequestRef;
import com.atlassian.bitbucket.repository.Repository;
import com.atlassian.bitbucket.scm.pull.MergeRequest;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import pl.brute_force.brute_pr.config.Config;
import pl.brute_force.brute_pr.config.ConfigDao;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class MergeBlockerTest {
  @Mock
  private ConfigDao configDao;
  @Mock
  private UserUtils userUtils;
  @Mock
  private RegexUtils regexUtils;
  @InjectMocks
  private MergeBlocker sut;

  @Mock
  MergeRequest merge;
  @Mock
  PullRequest pr;
  @Mock
  Repository repository;
  @Mock
  Project project;
  @Mock
  PullRequestRef ref;


  @Before
  @SuppressWarnings("unchecked")
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    when(merge.getPullRequest()).thenReturn(pr);
    when(pr.getToRef()).thenReturn(ref);
    when(ref.getRepository()).thenReturn(repository);
    when(ref.getId()).thenReturn(RegexUtils.REFS_PREFIX + "master");
    when(repository.getProject()).thenReturn(project);
    when(repository.getSlug()).thenReturn("repo_1");
    when(project.getKey()).thenReturn("PRJ");
    when(userUtils.dereferenceGroups(anyList())).thenReturn(Lists.<String>newArrayList());
    when(regexUtils.match(anyList(), anyString())).thenCallRealMethod();
    when(regexUtils.formatBranchName(anyString())).thenCallRealMethod();
    when(userUtils.getUserDisplayNameByName(Mockito.eq("user1"))).thenReturn("First user");
    when(userUtils.getUserDisplayNameByName(Mockito.eq("user2"))).thenReturn("Second user");
  }

  @Test
  public void testBlocking_blocked() throws Exception {
    when(configDao.getConfigForRepo(project.getKey(), repository.getSlug())).thenReturn(Config.builder()
        .blockedPRs(newArrayList("master"))
        .build());
    sut.check(merge);
    verify(merge, times(1)).veto(anyString(), anyString());
  }

  @Test
  public void testBlocking_notBlocked() throws Exception {
    when(configDao.getConfigForRepo(project.getKey(), repository.getSlug())).thenReturn(Config.builder()
        .blockedPRs(newArrayList("bugfix"))
        .build());
    sut.check(merge);
    verify(merge, never()).veto(anyString(), anyString());
  }

  @Test
  public void testBlocking_missingRequiredReviewer() throws Exception {
    Set<PullRequestParticipant> p = Sets.newHashSet(
        TestUtils.mockParticipant("user1", false)
    );
    PullRequestParticipant author = TestUtils.mockParticipant("author", false);
    when(pr.getReviewers()).thenReturn(p);
    when(pr.getAuthor()).thenReturn(author);
    when(configDao.getConfigForRepo(project.getKey(), repository.getSlug())).thenReturn(Config.builder()
        .blockedPRs(newArrayList("bugfix"))
        .requiredReviewers(newArrayList("user1"))
        .requiredReviews(1)
        .build());
    sut.check(merge);
    verify(merge, times(1)).veto(anyString(), anyString());
  }

  @Test
  public void testBlocking_reviewerIsAuthor_notEnoughApprovals() throws Exception {
    Set<PullRequestParticipant> p = Sets.newHashSet(
        TestUtils.mockParticipant("user2", false)
    );
    PullRequestParticipant author = TestUtils.mockParticipant("user1", false);
    when(pr.getReviewers()).thenReturn(p);
    when(pr.getAuthor()).thenReturn(author);
    when(configDao.getConfigForRepo(project.getKey(), repository.getSlug())).thenReturn(Config.builder()
        .blockedPRs(newArrayList("bugfix"))
        .requiredReviewers(newArrayList("user1", "user2"))
        .requiredReviews(1)
        .build());
    sut.check(merge);
    verify(merge, times(1)).veto(anyString(), anyString());
  }

  @Test
  public void testBlocking_reviewerIsAuthor_matchingNumberOfApprovals() throws Exception {
    Set<PullRequestParticipant> p = Sets.newHashSet(
        TestUtils.mockParticipant("user2", true)
    );
    PullRequestParticipant author = TestUtils.mockParticipant("user1", false);
    when(pr.getReviewers()).thenReturn(p);
    when(pr.getAuthor()).thenReturn(author);
    when(configDao.getConfigForRepo(project.getKey(), repository.getSlug())).thenReturn(Config.builder()
        .blockedPRs(newArrayList("bugfix"))
        .requiredReviewers(newArrayList("user1", "user2"))
        .requiredReviews(2)
        .build());
    sut.check(merge);
    verify(merge, never()).veto(anyString(), anyString());
  }

  @Test
  public void testBlocking_reviewerIsAuthor_approved() throws Exception {
    Set<PullRequestParticipant> p = Sets.newHashSet(
        TestUtils.mockParticipant("user2", true)
    );
    PullRequestParticipant author = TestUtils.mockParticipant("user1", false);
    when(pr.getReviewers()).thenReturn(p);
    when(pr.getAuthor()).thenReturn(author);
    when(configDao.getConfigForRepo(project.getKey(), repository.getSlug())).thenReturn(Config.builder()
        .blockedPRs(newArrayList("bugfix"))
        .requiredReviewers(newArrayList("user2"))
        .requiredReviews(1)
        .build());
    sut.check(merge);
    verify(merge, never()).veto(anyString(), anyString());
  }
}
