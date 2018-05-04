package edu.cmu.cs.cs214.hw6;

import edu.cmu.cs.cs214.hw6.sequential.DiffComposition;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.lib.Repository;

import java.util.List;
import java.util.Map;

/***
 * This interface defines API for parallel gitRepo and sequential gitRepo
 */
public interface GitRepo {
    /***
     * Called when you'd like to get Repository class
     * @return a Repository
     */
    Repository getRepo();

    /***
     * Called when you'd like to access the map containing all DiffEntries and its composition(parent, child)
     * according to the gitRepo
     * @return a map containing all DiffEntries and its composition(parent, child)
     */
    Map<List<DiffEntry>, DiffComposition> getDiffsMap();
}
