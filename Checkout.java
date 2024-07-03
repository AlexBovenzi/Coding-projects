package gitlet;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import static gitlet.Repository.*;
import static gitlet.Utils.*;

/**This class handles the checkout command. there are three different checkouts that need
 * to be handled
 * @author Alex Bovenzi
 */
public class Checkout {

    public static void checkoutFile(String f) {
        // get the head commit
        Commit head = Commit.readHeadCommit();
        // see if Head contains the
        if (head.containsk(f)) {
            //save the blob of the file we want
            String blob = head.get(f);
            //pathToReplace is the file were replacing
            File pathToReplace = join(Repository.CWD, f);
            writeContents(pathToReplace, blob);
        } else {
            //failure case, print message if file is not in latest commit
            System.out.println("File does not exist in that commit.");
        }
    }

    public static void checkoutSpecificFile(String id, String f) {
        Commit specificCommit = Commit.readSpecificCommit(substringtoWholeString(id));
        if (specificCommit.containsk(f)) {
            String blob = specificCommit.get(f);
            File pathToReplace = join(Repository.CWD, f);
            writeContents(pathToReplace, blob);
        } else {
            //failure case, print message if file is not in latest commit
            System.out.println("File does not exist in that commit.");
        }
    }
    @SuppressWarnings("unchecked")
    public static void checkoutBranch(String branch) {
        if (!theCWDisClear()) {
            System.out.println("There is an untracked file in the way; delete it,"
                    + " or add and commit it first.");
            return;
        }
        TreeMap<String, String> branches = readObject(BRANCHES, TreeMap.class);
        if (branches.containsKey(branch)) {
            if (branch.equals(currentBranch())) {
                System.out.println("No need to checkout the current branch.");
            } else {
                setCurrentBranch(branch);
                clearCWD();
                clearStagingArea();

                //get the sha1 of the specific branch and put it in the head
                writeContents(HEAD, branches.get(branch));
                //use the branch name to get ID
                String branchID = branches.get(branch);
                Commit newBranch = Commit.readSpecificCommit(branchID);
                for (HashMap.Entry<String, String> entry : newBranch.getEntrySet()) {
                    String blob = entry.getValue();
                    File pathToReplace = join(CWD, entry.getKey());
                    writeContents(pathToReplace, blob);
                }
            }
        } else {
            System.out.println("No such branch exists.");
        }
    }
    @SuppressWarnings("unchecked")
    private static boolean theTreeIsClean() {
        Commit c = Commit.readHeadCommit();
        TreeMap<String, String> staging = readObject(STAGINGAREA, TreeMap.class);
        List<String> files = plainFilenamesIn(CWD);
        if (!staging.isEmpty()) {
            return false;
        }
        for (String f : files) {
            if (!c.containsv(f)) {
                return false;
            }
        }
        ArrayList<String> remove = readObject(TOBEREMOVED, ArrayList.class);
        if (!remove.isEmpty()) {
            return false;
        }
        return true;
    }
    @SuppressWarnings("unchecked")
    public static void clearStagingArea() {
        TreeMap<String, String> stagingarea = readObject(STAGINGAREA, TreeMap.class);
        stagingarea.clear();
        writeObject(STAGINGAREA, stagingarea);
    }
    public static void clearCWD() {
        List<String> dir = plainFilenamesIn(CWD);
        for (String file : dir) {
            restrictedDelete(join(CWD, file));
        }
    }
    @SuppressWarnings("unchecked")
    public static boolean theCWDisClear() {
        List<String> dir = plainFilenamesIn(CWD);
        Commit c = Commit.readHeadCommit();
        TreeMap<String, String> stage = readObject(STAGINGAREA, TreeMap.class);
        for (String file: dir) {
            if (!c.containsk(file) && !stage.containsKey(file)) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    public static boolean theresAnUntrackedFileInTheWay(Commit other) {
        List<String> dir = plainFilenamesIn(CWD);
        Commit curr = Commit.readHeadCommit();
        TreeMap<String, String> stagingArea = readObject(STAGINGAREA, TreeMap.class);
        for (String file: dir) {
            if (other.containsk(file)
                    && !curr.containsk(file)
                    && !stagingArea.containsKey(file)) {
                return true;
            }
        }
        return false;
    }

    public static String currentBranch() {
        return readContentsAsString(CURRENTBRANCH);
    }
    public static void setCurrentBranch(String branchName) {
        writeContents(CURRENTBRANCH, branchName);
    }

    @SuppressWarnings("unchecked")
    public static String substringtoWholeString(String id) {
        HashMap<String, Commit> repositorymap = readObject(REPOSITORY, HashMap.class);
        for (HashMap.Entry<String, Commit> entry: repositorymap.entrySet()) {
            if (entry.getKey().contains(id)) {
                return entry.getKey();
            }
        }
        System.out.println("No commit with that id exists.");
        System.exit(0);
        return "";
    }

    private static void checkoutCommit(String id) {
        Commit specificCommit = Commit.readSpecificCommit(substringtoWholeString(id));
        for (HashMap.Entry<String, String> entry: specificCommit.getEntrySet()) {
            String blob = entry.getValue();
            File pathToReplace = join(Repository.CWD, entry.getKey());
            writeContents(pathToReplace, blob);
        }
    }
}
