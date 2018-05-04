package edu.cmu.cs.cs214.hw6.sequential;

import edu.cmu.cs.cs214.hw6.GitRepo;
import edu.cmu.cs.cs214.hw6.parallel.GitRepoParallel;
import org.eclipse.jgit.diff.DiffEntry;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class GitRepoSequentialTest {
    GitRepo gitRepoBPal = new GitRepoSequential("src/main/resources/.gitRepo50");

    @Test
    public void testGetRepo() {
        assertNotNull(gitRepoBPal.getRepo());
    }

    @Test
    public void testGetDiffsMap() {
        assertNotNull(gitRepoBPal.getDiffsMap());
    }

    @Test
    public void testDiffsMapSizeNotZero() {
        assertTrue(gitRepoBPal.getDiffsMap().size() != 0);
    }

    @Test
    public void testOnDiffhasParent() {
        for (List<DiffEntry> diffs : gitRepoBPal.getDiffsMap().keySet()) {
            assertNotNull(gitRepoBPal.getDiffsMap().get(diffs).getParentSHA1());
            break;
        }
    }

    @Test
    public void testOnDiffhasChild() {
        for (List<DiffEntry> diffs : gitRepoBPal.getDiffsMap().keySet()) {
            assertNotNull(gitRepoBPal.getDiffsMap().get(diffs).getChildSHA1());
            break;
        }
    }
}