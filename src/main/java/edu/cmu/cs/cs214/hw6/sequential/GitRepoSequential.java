package edu.cmu.cs.cs214.hw6.sequential;

import edu.cmu.cs.cs214.hw6.GitRepo;
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

/***
 * This class will mainly be in charge of accessing local repo using jGit.
 * This class implements GitRepo and contains a sequential method to get all DiffEntry
 * and two commit's SHA1 corresponding the DiffEntry
 */
public class GitRepoSequential implements GitRepo {
    private Git git;
    private Repository repo;
    public Repository getRepo() { return repo; }


    private Map<List<DiffEntry>, DiffComposition> diffsMap;
    public Map<List<DiffEntry>, DiffComposition> getDiffsMap() { return diffsMap; }

    /***
     * The constructor to instantiate the class
     * @param repoPath specify local repository's path
     */
    public GitRepoSequential(String repoPath) {
        try {
            this.git = Git.open(new File(repoPath));
            this.repo = git.getRepository();
            diffsMap = generateDiffsMap();
            repo.close();
            git.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Map<List<DiffEntry>, DiffComposition> generateDiffsMap(){
        Map<List<DiffEntry>, DiffComposition> map = new HashMap<>();
        try {

            List<Ref> branches = git.branchList().call();
            RevWalk revWalk = new RevWalk(repo);
            for (Ref branch : branches) {
                Iterable<RevCommit> commits = git.log().add(repo.resolve(branch.getName())).call();
                for (RevCommit rev : commits) {
                    List<RevCommit> parents = new ArrayList<>();
                    for (RevCommit parentIncompleteHeader : rev.getParents()) {
                        RevCommit deepCopy = revWalk.parseCommit(parentIncompleteHeader.getId());
                        parents.add(deepCopy);
                    }
                    if (parents.size() == 0) {
                        continue;
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
                }
            }


        } catch (IOException e) {
            e.printStackTrace();
        } catch (GitAPIException e) {
            e.printStackTrace();
        }

        return map;
    }
}
