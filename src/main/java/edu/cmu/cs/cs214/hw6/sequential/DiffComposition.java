package edu.cmu.cs.cs214.hw6.sequential;

/***
 * This class will record both parent's and child's SHA1 hash value according to the revision diff
 */
public class DiffComposition {
    private String childSHA1;
    public String getChildSHA1() { return childSHA1; }

    private String parentSHA1;
    public String getParentSHA1() { return parentSHA1; }


    /***
     * The constructor to instantiate class
     * @param childSHA1 child's SHA1 hash value
     * @param parentSHA1 parent's SHA hash value
     */
    public DiffComposition(String childSHA1, String parentSHA1) {
        this.childSHA1 = childSHA1;
        this.parentSHA1 = parentSHA1;
    }
}
