package edu.cmu.cs.cs214.hw6.sequential;

import edu.cmu.cs.cs214.hw6.GitRepo;
import edu.cmu.cs.cs214.hw6.parallel.GitRepoParallel;
import org.junit.Test;

import java.util.Queue;

import static org.junit.Assert.*;

public class SimilarityUtilTest {
    GitRepo gitRepoAPal = new GitRepoParallel("src/main/resources/.gitRepo50");
    GitRepo gitRepoBPal = new GitRepoParallel("src/main/resources/.gitRepo350");

    GitRepo gitRepoA = new GitRepoSequential("src/main/resources/.gitRepo50");
    GitRepo gitRepoB = new GitRepoSequential("src/main/resources/.gitRepo350");

    SimilarityUtil parallelUtil = new SimilarityUtil(gitRepoAPal, gitRepoBPal, 5, true);
    SimilarityUtil sequentialUtil = new SimilarityUtil(gitRepoA, gitRepoB, 5, false);

    @Test
    public void testInitParallelMapA() {
        assertNotNull(parallelUtil.getDiffsFrequencyMapA());
    }

    @Test
    public void testInitParallelMapB() {
        assertNotNull(parallelUtil.getDiffsFrequencyMapB());
    }

    @Test
    public void testInitSequentialMapA() {
        assertNotNull(sequentialUtil.getDiffsFrequencyMapA());
    }

    @Test
    public void testInitSequentialMapB() {
        assertNotNull(sequentialUtil.getDiffsFrequencyMapB());
    }

    @Test
    public void testPriorityQueueParallel() {
        Queue<Double> queue = parallelUtil.getPq();
        for (int i = 0; i < 5; i++) {
            if (!queue.isEmpty()) {
                double bigger = queue.poll();
                if (!queue.isEmpty()) {
                    double smaller = queue.poll();
                    assertTrue(bigger >= smaller);
                }
            }
        }
    }

    @Test
    public void testPriorityQueueSequential() {
        Queue<Double> queue = sequentialUtil.getPq();
        for (int i = 0; i < 5; i++) {
            if (!queue.isEmpty()) {
                double bigger = queue.poll();
                if (!queue.isEmpty()) {
                    double smaller = queue.poll();
                    assertTrue(bigger >= smaller);
                }
            }
        }
    }

    @Test
    public void testPrintNParallel() {
        assertTrue(parallelUtil.printMostN());
    }

    @Test
    public void testPrintNSequential() {
        assertTrue(sequentialUtil.printMostN());
    }

    @Test
    public void testOnQueueSize() {
        assertTrue(parallelUtil.getPq().size() == sequentialUtil.getPq().size());
    }
}