import edu.cmu.cs.cs214.hw6.GitRepo;
import edu.cmu.cs.cs214.hw6.parallel.GitRepoParallel;
import edu.cmu.cs.cs214.hw6.sequential.SimilarityUtil;

/***
 * This is a main class for performing computing similarity in parallel manner.
 */
public class ParallelMain {
    /***
     * To load repos for computing similarity and indicate how much time does this parallel program use.
     * @param args this program doesn't support command line arguments. All arguments should be written in program.
     */
    public static void main(String[] args) {
        final int numOfRevision = 5;
        final double timeDenominator = 1_000_000_000.0;
        long palStartTime = System.nanoTime();
        GitRepo gitRepoAPal = new GitRepoParallel("src/main/resources/.gitRepo50");
        GitRepo gitRepoBPal = new GitRepoParallel("src/main/resources/.gitRepo350");
        new SimilarityUtil(gitRepoAPal, gitRepoBPal, numOfRevision, true).printMostN();
        System.out.println((System.nanoTime() - palStartTime) / timeDenominator);

        System.exit(-1);
    }
}
