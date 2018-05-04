package edu.cmu.cs.cs214.hw6.parallel;

import edu.cmu.cs.cs214.hw6.GitRepo;
import edu.cmu.cs.cs214.hw6.sequential.DiffComposition;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
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
 * This class will mainly be in charge of accessing local repo using jGit.
 * This class implements GitRepo and contains a parallel method to get all DiffEntry
 * and two commit's SHA1 corresponding the DiffEntry
 */
public class GitRepoParallel implements GitRepo {
    private Git git;
    private Repository repo;
    public Repository getRepo() { return repo; }


    private volatile boolean hasProduced = false;

    private static final int N_THREADS = 10;
    private static final int QUEUE_CAPACITY = 4 * N_THREADS;

    private Map<List<DiffEntry>, DiffComposition> diffsMap;
    public Map<List<DiffEntry>, DiffComposition> getDiffsMap() { return diffsMap; }

    /***
     * The constructor to instantiate the class
     * @param repoPath specify local repository's path
     */
    public GitRepoParallel(String repoPath) {
        try {
            this.git = Git.open(new File(repoPath));
            this.repo = git.getRepository();
            diffsMap = generateDiffsMapParallel();
            repo.close();
            git.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Map<List<DiffEntry>, DiffComposition> generateDiffsMapParallel() {
        Map<List<DiffEntry>, DiffComposition> map = new ConcurrentHashMap<>();
        ExecutorService pool = Executors.newFixedThreadPool(N_THREADS);
        BlockingQueue<Future<Map<List<DiffEntry>, DiffComposition>>> queue = new ArrayBlockingQueue<>(QUEUE_CAPACITY);


        Thread producerThread = new Thread(() -> generateDiffsMap(pool, queue));
        Thread consumerThread = new Thread(() -> {
            try {
                while (!hasProduced || !queue.isEmpty()) {
                    Map<List<DiffEntry>, DiffComposition> newMap = queue.take().get();
                    if (newMap != null) {
                        for (List<DiffEntry> diffEntries : newMap.keySet()) {
                            map.put(diffEntries, newMap.get(diffEntries));
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

        producerThread.start();
        consumerThread.start();

        try {
            producerThread.join();
            hasProduced = true;
            consumerThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        return map;
    }
    private void generateDiffsMap(ExecutorService pool, BlockingQueue<Future<Map<List<DiffEntry>, DiffComposition>>> queue) {
        try {
            List<Ref> branches = git.branchList().call();
            for (Ref branch : branches) {
                Iterable<RevCommit> commits = git.log().add(repo.resolve(branch.getName())).call();
                for (RevCommit rev : commits) {
                    queue.put(pool.submit(() -> generateDiffs(rev)));
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
            System.exit(-1);
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
    }

    private Map<List<DiffEntry>, DiffComposition> generateDiffs(RevCommit rev){
        Map<List<DiffEntry>, DiffComposition> map = new HashMap<>();
        try {
            List<RevCommit> parents = new ArrayList<>();
            RevWalk revWalk = new RevWalk(repo);
            for (RevCommit parentIncompleteHeader : rev.getParents()) {
                RevCommit deepCopy = revWalk.parseCommit(parentIncompleteHeader.getId());
                parents.add(deepCopy);
            }
            if (parents.size() == 0) {
                return null;
            }
            ObjectId childId = rev.getTree().getId();
            for (RevCommit parent : parents) {
                ObjectId parentId = parent.getTree().getId();
                ObjectReader reader = repo.newObjectReader();

                CanonicalTreeParser oldTreeIter = new CanonicalTreeParser();
                oldTreeIter.reset(reader, parentId);

                CanonicalTreeParser newTreeIter = new CanonicalTreeParser();
                newTreeIter.reset(reader, childId);

                List<DiffEntry> diffs = git.diff().setOldTree(oldTreeIter).setNewTree(newTreeIter).call();
                map.put(diffs, new DiffComposition(rev.getName(), parent.getName()));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }
        return map;
    }

}
