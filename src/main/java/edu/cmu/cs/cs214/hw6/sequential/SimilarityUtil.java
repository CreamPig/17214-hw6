package edu.cmu.cs.cs214.hw6.sequential;

import edu.cmu.cs.cs214.hw6.GitRepo;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Queue;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutionException;

/***
 * This class contains utilities for calculating similarity between revisions in both parallel and sequential method
 */
public class SimilarityUtil {

    private GitRepo gitRepoA;
    private GitRepo gitRepoB;

    private int numOfRevisions;
    private volatile boolean hasProduced = false;

    private static final int N_THREADS = 10;
    private static final int QUEUE_CAPACITY = 4 * N_THREADS;

    private Queue<Double> pq;
    public Queue<Double> getPq(){ return pq; }

    private Map<List<DiffEntry>, Map<String, Integer>> diffsFrequencyMapA;

    /***
     * a defensive copy getter for diffFrequencyMapA
     * @return a diffFrequencyMap representing pair of List<DiffEntry> and Map<String, Integer>
     */
    public Map<List<DiffEntry>, Map<String, Integer>> getDiffsFrequencyMapA() {
        return new ConcurrentHashMap<>(diffsFrequencyMapA) ; }
    private Map<List<DiffEntry>, Map<String, Integer>> diffsFrequencyMapB;

    /***
     * a defensive copy getter for diffFrequencyMapB
     * @return a diffFrequencyMap representing pair of List<DiffEntry> and Map<String, Integer>
     */
    public Map<List<DiffEntry>, Map<String, Integer>> getDiffsFrequencyMapB() {
        return new ConcurrentHashMap<>(diffsFrequencyMapB); }
    private Map<Double, List<DiffComposition[]>> similarityMap;


    /***
     * This is a constructor to instantiate the class. In instantiation, the constructor will automatically perform getDiffsFrequencyMap to get
     * frequencyMap for both repo and then perform getSimilarityMap to calculate similarity according to isParallel value
     * @param gitRepoA specify one of repo you'd like to calculate similarity
     * @param gitRepoB specify the other repo you'd like to calculate similarity
     * @param numOfRevisions specify the N of most similar revision that will be printed between two repos
     * @param isParallel specify whether using parallel or sequential method to calculate similarity
     */
    public SimilarityUtil(GitRepo gitRepoA, GitRepo gitRepoB, int numOfRevisions, boolean isParallel) {
        this.gitRepoA = gitRepoA;
        this.gitRepoB = gitRepoB;
        this.numOfRevisions = numOfRevisions;
        pq = new PriorityQueue<>((Double d1, Double d2) -> {
            if (d1 > d2) {
                return -1;
            } else {
                return 1;
            }
        });

        diffsFrequencyMapA = isParallel? new ConcurrentHashMap<>(): new HashMap<>();
        diffsFrequencyMapB = isParallel? new ConcurrentHashMap<>(): new HashMap<>();
        getDiffsFrequencyMapSequential(gitRepoA, diffsFrequencyMapA);
        getDiffsFrequencyMapSequential(gitRepoB, diffsFrequencyMapB);
        similarityMap = isParallel? getSimilarityMapParallel() : getSimilarityMapSequential();
    }


    private Map<Double, List<DiffComposition[]>> getSimilarityMapParallel() {
        Map<Double, List<DiffComposition[]>> map = new ConcurrentHashMap<>();
        ExecutorService pool = Executors.newFixedThreadPool(N_THREADS);
        BlockingQueue<Future<Map<Double, List<DiffComposition[]>>>> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);

        Thread producerT = new Thread(() -> compareDiffEntry(pool, queue));
        Thread consumerT = new Thread(() -> {
            try {
                while (!hasProduced || !queue.isEmpty()) {
                    Map<Double, List<DiffComposition[]>> newSimilarityMap = queue.take().get();
                    if (newSimilarityMap != null) {
                        for (Double d : newSimilarityMap.keySet()) {
                            if (!map.containsKey(d)) {
                                map.put(d, new ArrayList<>());
                                map.get(d).addAll(newSimilarityMap.get(d));
                                pq.offer(d);
                                continue;
                            }

                            for (DiffComposition[] list : newSimilarityMap.get(d)) {
                                if (!isDuplicacted(map.get(d), list)) {
                                    map.get(d).add(list);
                                }
                            }
                            if (!pq.contains(d)) {
                                pq.offer(d);
                            }
                        }
                    }
                }

            } catch (ExecutionException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
                System.exit(-1);
            }
        });

        producerT.start();
        consumerT.start();

        try {
            producerT.join();
            hasProduced = true;
            consumerT.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return map;
    }

    private void compareDiffEntry(ExecutorService pool, BlockingQueue<Future<Map<Double, List<DiffComposition[]>>>> queue) {
        for (List<DiffEntry> diffEntryA : gitRepoA.getDiffsMap().keySet()) {
            Map<String, Integer> frequencyMapA = diffsFrequencyMapA.get(diffEntryA);
            int totalSquareA = calculateTotalSquare(frequencyMapA);

            // avoid denominator being 0 where there might be a merge but nothing on git
            if (totalSquareA == 0) {
                continue;
            }

            try {
                queue.put(pool.submit(() -> generateDiffMap(frequencyMapA, totalSquareA, diffEntryA)));
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }
    }

    private Map<Double, List<DiffComposition[]>> generateDiffMap(Map<String, Integer> frequencyMapA, int totalSquareA, List<DiffEntry> diffEntryA) {
        Map<Double, List<DiffComposition[]>> newMap = new ConcurrentHashMap<>();
        for (List<DiffEntry> diffEntryB : gitRepoB.getDiffsMap().keySet()) {
            Map<String, Integer> frequencyMapB = diffsFrequencyMapB.get(diffEntryB);
            if (frequencyMapB == null) {
                continue;
            }
            int totalSquareB = calculateTotalSquare(frequencyMapB);


            if (totalSquareB == 0) {
                continue;
            }
            int innerDot = 0;
            for (String s : frequencyMapA.keySet()) {
                int valueA = frequencyMapA.getOrDefault(s, 0);
                int valueB = frequencyMapB.getOrDefault(s, 0);
                innerDot += valueA * valueB;
            }

            double result = innerDot / (Math.sqrt(totalSquareA) * Math.sqrt(totalSquareB));

            // in case of overflow
            if (Double.isNaN(result)) {
                continue;
            }

            // dealing with double type might be problematic, namely there might be a situation where
            // cosine similarity is over 1. Using the following condition to amend if it's over 1.
            if (result > 1) {
                result = 1;
            }

            DiffComposition[] diffCompositions = new DiffComposition[2];
            diffCompositions[0] = gitRepoA.getDiffsMap().get(diffEntryA);
            diffCompositions[1] = gitRepoB.getDiffsMap().get(diffEntryB);
            if (!newMap.containsKey(result)) {
                newMap.put(result, new ArrayList<>());
            }
            if (!isDuplicacted(newMap.get(result), diffCompositions)) {
                newMap.get(result).add(diffCompositions);
            }
        }
        return newMap;
    }

    private boolean isDuplicacted(List<DiffComposition[]> list, DiffComposition[] diffComposition) {
        for (DiffComposition[] diffs : list) {
            if (diffComposition[0].getChildSHA1().equals(diffs[0].getChildSHA1())
                    && diffComposition[0].getParentSHA1().equals(diffs[0].getParentSHA1())
                    && diffComposition[1].getChildSHA1().equals(diffs[1].getChildSHA1())
                    && diffComposition[1].getParentSHA1().equals(diffs[1].getParentSHA1())) {
                return true;
            }
            if (diffComposition[1].getChildSHA1().equals(diffs[0].getChildSHA1())
                    && diffComposition[1].getParentSHA1().equals(diffs[0].getParentSHA1())
                    && diffComposition[0].getChildSHA1().equals(diffs[1].getChildSHA1())
                    && diffComposition[0].getParentSHA1().equals(diffs[1].getParentSHA1())) {
                return true;
            }
        }
        return false;
    }
    private int calculateTotalSquare(Map<String, Integer> map) {
        int totalSquare = 0;
        for (String s : map.keySet()) {
            totalSquare += Math.pow(map.get(s), 2);
        }
        return totalSquare;
    }

    private void getDiffsFrequencyMapSequential(GitRepo gitRepo, Map<List<DiffEntry>, Map<String, Integer>> map) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        DiffFormatter df = new DiffFormatter(out);
        df.setRepository(gitRepo.getRepo());

        for (List<DiffEntry> diffs : gitRepo.getDiffsMap().keySet()) {
            if (!map.containsKey(diffs)) {
                Map<String, Integer> frequencyMap = new HashMap<>();
                map.put(diffs, frequencyMap);
            }

            Map<String, Integer> updatedMap = map.get(diffs);
            try {
                for (DiffEntry diff : diffs) {
                    df.format(diff);
                    diff.getOldId();
                    String diffText = out.toString("UTF-8");
                    for (String line : diffText.split("\n")) {
                        for (String word : line.split(" ")) {
                            updatedMap.put(word, updatedMap.getOrDefault(word, 0) + 1);
                        }
                    }
                    out.reset();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        df.close();
    }

    private Map<Double, List<DiffComposition[]>> getSimilarityMapSequential() {
        Map<Double, List<DiffComposition[]>> map = new HashMap<>();
        for (List<DiffEntry> diffEntryA : gitRepoA.getDiffsMap().keySet()) {
            Map<String, Integer> frequencyMapA = diffsFrequencyMapA.get(diffEntryA);
            int totalSquareA = calculateTotalSquare(frequencyMapA);

            // avoid denominator being 0 where there might be a merge but nothing on git
            if (totalSquareA == 0) {
                continue;
            }

            for (List<DiffEntry> diffEntryB : gitRepoB.getDiffsMap().keySet()) {
                Map<String, Integer> frequencyMapB = diffsFrequencyMapB.get(diffEntryB);
                int totalSquareB = calculateTotalSquare(frequencyMapB);

                if (totalSquareB == 0) {
                    continue;
                }
                int innerDot = 0;
                for (String s : frequencyMapA.keySet()) {
                    int valueA = frequencyMapA.getOrDefault(s, 0);
                    int valueB = frequencyMapB.getOrDefault(s, 0);
                    innerDot += valueA * valueB;
                }

                double result = innerDot / (Math.sqrt(totalSquareA) * Math.sqrt(totalSquareB));

                // in case of overflow
                if (Double.isNaN(result)) {
                    continue;
                }

                // dealing with double type might be problematic, namely there might be a situation where
                // cosine similarity is over 1. Using the following condition to amend if it's over 1.
                if (result > 1) {
                    result = 1;
                }

                DiffComposition[] diffCompositions = new DiffComposition[2];
                diffCompositions[0] = gitRepoA.getDiffsMap().get(diffEntryA);
                diffCompositions[1] = gitRepoB.getDiffsMap().get(diffEntryB);
                if (!map.containsKey(result)) {
                    map.put(result, new ArrayList<>());
                }
                if (!isDuplicacted(map.get(result), diffCompositions)) {
                    map.get(result).add(diffCompositions);
                }
                if (!pq.contains(result)) {
                    pq.offer(result);
                }
            }
        }
        return map;
    }


    /***
     * Will print N of most similar revisions.
     * @return return true if return count is less or equal to numOfRevisions; otherwise, false
     */
    public boolean printMostN() {
        int count = 0;
        while (!pq.isEmpty() && count < numOfRevisions) {
            double d = pq.poll();
            List<DiffComposition[]> diffCompositions = similarityMap.get(d);
            for (DiffComposition[] diffs : diffCompositions) {
                System.out.println(diffs[0].getChildSHA1() + ", " + diffs[0].getParentSHA1() + ";");
                System.out.println(diffs[1].getChildSHA1() + ", " + diffs[1].getParentSHA1() + ":");
                System.out.println("Similarity: " + d);
                System.out.println("================================");
                count++;
                if (count == numOfRevisions) {
                    break;
                }
            }
        }
        return numOfRevisions >= count;
    }
}
