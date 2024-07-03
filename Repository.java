package gitlet;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeMap;

import static gitlet.Utils.*;


/** Represents a gitlet repository.
 *  does at a high level.
 *
 *  @author Alex Bovenzi
 */
public class Repository {
    /**
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    public static final File CWD = new File(System.getProperty("user.dir"));
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    public static final File REPOSITORY = join(GITLET_DIR, "repository");
    public static final File STAGINGAREA = join(GITLET_DIR, "stagingArea");
    public static final File HEAD = join(GITLET_DIR, "HEAD");
    public static final File BRANCHES = join(GITLET_DIR, "branches");
    public static final File TOBEREMOVED = join(GITLET_DIR, "ToBeRemoved");
    public static final File CURRENTBRANCH = join(GITLET_DIR, "curBranch");
    public static final String MASTER = "master";

    public static void init() {
        if (!GITLET_DIR.exists()) {
            GITLET_DIR.mkdir();

            new Commit();
        } else {
            System.out.println("A Gitlet version-control system already "
                    + "exists in the current directory.");
        }
    }
    @SuppressWarnings("unchecked")
    public static void add(String file) {
        File fileToAdd = join(CWD, file);
        if (fileToAdd.exists()) {
            //serialize the file to add
            String blob = readContentsAsString(fileToAdd);

            TreeMap<String, String> stage;
            ArrayList<String> remove = readObject(TOBEREMOVED, ArrayList.class);

            //remove it from the removal staging area and write it
            remove.remove(file);
            writeObject(TOBEREMOVED, remove);

            stage = readObject(STAGINGAREA, TreeMap.class);

            //get the head commit from the hash table
            Commit head = Commit.readHeadCommit();

            //check to see if any changes have been made to file that's to be added
            if (stage.get(file) == null || !stage.get(file).equals(blob)
                             || head.containsv(blob)) {
                stage.put(file, blob);
            }
            if (head.get(file) != null && head.get(file).equals(blob)) {
                //if it doesnt exist add it to staging area
                stage.remove(file, blob);
            }
            writeObject(STAGINGAREA, stage);
        } else {
            System.out.println("File does not exist.");
        }
    }

    public static void commit(String s, String op) {
        if (s.isBlank()) {
            System.out.println("Please enter a commit message.");
        } else {
            new Commit(s, op);
        }
    }

    public static void checkout(String[] args) {
        if (args[1].equals("--")) {
            Checkout.checkoutFile(args[2]);
        } else if (args.length == 4 && args[2].equals("--")) {
            Checkout.checkoutSpecificFile(args[1], args[3]);
        } else if (args.length == 2) {
            Checkout.checkoutBranch(args[1]);
        } else {
            System.out.println("Incorrect operands");
        }

    }
    public static void log() {
        Commit.log(Commit.readHeadCommit());
    }
    @SuppressWarnings("unchecked")
    public static void globalLog() {
        HashMap<String, Commit> repositorymap = readObject(REPOSITORY, HashMap.class);
        for (HashMap.Entry<String, Commit> commit : repositorymap.entrySet()) {
            commit.getValue().printCommit();
        }
    }
    @SuppressWarnings("unchecked")
    public static void find(String message) {
        HashMap<String, Commit> repositorymap = readObject(REPOSITORY, HashMap.class);
        boolean match = false;
        for (HashMap.Entry<String, Commit> commit : repositorymap.entrySet()) {
            if (commit.getValue().messageCompare(message)) {
                System.out.println(commit.getKey());
                match = true;
            }
        }
        if (!match) {
            System.out.println("Found no commit with that message.");
        }

    }
    @SuppressWarnings("unchecked")
    public static void branch(String B) {
        TreeMap<String, String> branches;
        branches = readObject(BRANCHES, TreeMap.class);
        if (branches.containsKey(B)) {
            System.out.println("A branch with that name already exists.");
        } else {
            branches.put(B, Commit.readHeadID());
            writeObject(BRANCHES, branches);
        }
    }
    @SuppressWarnings("unchecked")
    public static void status() {
        //Gitlet dir needs to be created before checking status
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            return;
        }
        TreeMap<String, String> branches, stage;
        branches = readObject(BRANCHES, TreeMap.class);
        stage = readObject(STAGINGAREA, TreeMap.class);
        System.out.println("=== Branches ===");
        for (HashMap.Entry<String, String> branch : branches.entrySet()) {
            if (branch.getKey().equals(Checkout.currentBranch())) {
                System.out.print("*");
            }
            System.out.println(branch.getKey());
        }
        System.out.println("\n=== Staged Files ===");

        for (HashMap.Entry<String, String> entry : stage.entrySet()) {
            System.out.println(entry.getKey());
        }

        ArrayList<String> remove = readObject(TOBEREMOVED, ArrayList.class);
        System.out.println("\n=== Removed Files ===");
        for (String filename: remove) {
            System.out.println(filename);
        }
        System.out.println("\n=== Modifications Not Staged For Commit ===");
        ExtraCredit.modsStatus();
        System.out.println("\n=== Untracked Files ===");
        ExtraCredit.untrackedStatus();
        System.out.println();
    }
    @SuppressWarnings("unchecked")
    public static void remove(String fileToBeRemoved) {
        TreeMap<String, String> stage;
        ArrayList<String> remove;
        stage = readObject(STAGINGAREA, TreeMap.class);
        remove = readObject(TOBEREMOVED, ArrayList.class);
        Commit headCommit = Commit.readHeadCommit();
        if (stage.containsKey(fileToBeRemoved)) {
            stage.remove(fileToBeRemoved);
            writeObject(STAGINGAREA, stage);
        } else if (headCommit.containsk(fileToBeRemoved)) {
            remove.add(fileToBeRemoved);
            writeObject(TOBEREMOVED, remove);
            File deleteThisFile = join(CWD, fileToBeRemoved);
            if (deleteThisFile.exists()) {
                restrictedDelete(deleteThisFile);
            }
        } else {
            System.out.println("No reason to remove the file.");
        }
    }
    @SuppressWarnings("unchecked")
    public static void removeBranch(String branchName) {
        TreeMap<String, String> branches = readObject(BRANCHES, TreeMap.class);
        String currentBranch = Checkout.currentBranch();
        if (branches.containsKey(branchName)) {
            if (branchName.equals(currentBranch)) {
                System.out.println("Cannot remove the current branch.");
            } else {
                branches.remove(branchName);
                writeObject(BRANCHES, branches);
            }
        } else {
            System.out.println("A branch with that name does not exist.");
        }
    }
    @SuppressWarnings("unchecked")
    public static void reset(String id) {
        Commit resetToThis = Commit.readSpecificCommit(id);
        if (Checkout.theresAnUntrackedFileInTheWay(resetToThis)) {
            System.out.println("There is an untracked file in the way;"
                    + " delete it, or add and commit it first.");
        } else {
            Checkout.clearCWD();
            for (HashMap.Entry<String, String> entry : resetToThis.getEntrySet()) {
                Checkout.checkoutSpecificFile(id, entry.getKey());
            }
            Checkout.clearStagingArea();
            writeContents(HEAD, id);
            Checkout.setCurrentBranch(resetToThis.getbranch());
            TreeMap<String, String> branches = readObject(BRANCHES, TreeMap.class);
            branches.put(resetToThis.getbranch(), id);
            writeObject(BRANCHES, branches);
        }
    }

    @SuppressWarnings("unchecked")
    public static String getBranchID(String branchName) {
        TreeMap<String, String> branches = readObject(BRANCHES, TreeMap.class);
        if (!branches.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        return branches.get(branchName);
    }
}
