import edu.cmu.cs.cs214.hw6.GitRepo;
import edu.cmu.cs.cs214.hw6.sequential.GitRepoSequential;
import edu.cmu.cs.cs214.hw6.sequential.SimilarityUtil;

/***
 * This is a main class for performing computing similarity in sequential manner.
 */
public class SequentialMain {
    /***
     * To load repos for computing similarity and indicate how much time does this sequential program use.
     * @param args this program doesn't support command line arguments. All arguments should be written in program.
     */
    public static void main(String[] args) {
        final int numOfRevision = 5;
        final double timeDenominator = 1_000_000_000.0;
        long seqStartTime = System.nanoTime();
        GitRepo gitRepoA = new GitRepoSequential("src/main/resources/.gitRepo50");
        GitRepo gitRepoB = new GitRepoSequential("src/main/resources/.gitRepo350");
        new SimilarityUtil(gitRepoA, gitRepoB, numOfRevision, false).printMostN();
        System.out.println((System.nanoTime() - seqStartTime) / timeDenominator);

        System.exit(-1);
    }
}
