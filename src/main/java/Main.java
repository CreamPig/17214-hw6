import edu.cmu.cs.cs214.hw6.GitRepo;
import edu.cmu.cs.cs214.hw6.parallel.GitRepoParallel;
import edu.cmu.cs.cs214.hw6.sequential.GitRepoSequential;
import edu.cmu.cs.cs214.hw6.sequential.SimilarityUtil;

/***
 * This class can be run by gradle. It simply runs parallel and sequential program and print the result for comparison.
 */
public class Main {

    /***
     * This main method will print similarity results and time cost from both sequential and parallel program.
     * @param args this program doesn't support command line arguments. All arguments should be written in program.
     */
    public static void main(String[] args) {
        final int numOfRevision = 5;
        final double timeDenominator = 1_000_000_000.0;
        long seqStartTime = System.nanoTime();
        System.out.println("sequential program starting...");
        GitRepo gitRepoA = new GitRepoSequential("src/main/resources/.gitRepo50");
        GitRepo gitRepoB = new GitRepoSequential("src/main/resources/.gitRepo350");
        new SimilarityUtil(gitRepoA, gitRepoB, numOfRevision, false).printMostN();
        System.out.println((System.nanoTime() - seqStartTime) / timeDenominator);

        System.out.println("sequential program ended");
        System.out.println("==================================");

        long palStartTime = System.nanoTime();
        System.out.println("parallel program starting...");
        GitRepo gitRepoAPal = new GitRepoParallel("src/main/resources/.gitRepo50");
        GitRepo gitRepoBPal = new GitRepoParallel("src/main/resources/.gitRepo350");
        new SimilarityUtil(gitRepoAPal, gitRepoBPal, numOfRevision, true).printMostN();
        System.out.println((System.nanoTime() - palStartTime) / timeDenominator);

        System.out.println("parallel program ended");
        System.out.println("==================================");

        System.exit(-1);
    }
}
