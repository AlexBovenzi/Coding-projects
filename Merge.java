package gitlet;

//import org.apache.commons.math3.geometry.spherical.oned.ArcsSet;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeMap;

import static gitlet.Repository.*;
import static gitlet.Utils.*;

/**@author Alex Bovenzi
 * M E R G E
 * */
public class Merge {
    /**
     * This class handles all of the merge related issues.
     */

    @SuppressWarnings("unchecked")
    public static void errorCheck(String branch) {
        TreeMap<String, String> stage = readObject(STAGINGAREA, TreeMap.class);
        ArrayList<String> remove = readObject(TOBEREMOVED, ArrayList.class);
        if (!stage.isEmpty() || !remove.isEmpty()) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }
        if (!Checkout.theCWDisClear()) {
            System.out.println("There is an untracked file in the way;"
                    + " delete it, or add and commit it first.");
            System.exit(0);
        }
        TreeMap<String, String> branches = readObject(BRANCHES, TreeMap.class);
        if (!branches.containsKey(branch)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        String curBranch = Checkout.currentBranch();
        if (curBranch.equals(branch)) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }
    }

    private static Commit splitPoint(String branch) {
        String curID = readContentsAsString(HEAD);
        String branchID = getBranchID(branch);
        LinkedList<String> curList = Commit.getCommitChain(curID);
        LinkedList<String> branchList = Commit.getCommitChain(branchID);
        if (curList.contains(branchID)) {
            System.out.println("Given branch is an ancestor of the current branch.");
            System.exit(0);
        }
        if (branchList.contains(curID)) {
            System.out.println("Current branch fast-forwarded.");
            Checkout.checkoutBranch(branch);
            System.exit(0);
        }
        for (String other : branchList) {
            for (String cur : curList) {
                if (cur.equals(other)) {
                    return Commit.readSpecificCommit(other);
                }
            }
        }
        return null;
    }

    public static void merge(String branch) {
        errorCheck(branch);
        Commit split = splitPoint(branch);
        Commit other = Commit.readSpecificCommit(getBranchID(branch));
        Commit head = Commit.readHeadCommit();

        LinkedList<String> allFiles = makeAList(split, other, head);

        //algorithm
        for (String file : allFiles) {
            //not in split

            if (!split.containsk(file)) {
                if (!other.containsk(file)) {
                    cwdAndStage(file, head.get(file));
                } else if (!head.containsk(file)) {
                    cwdAndStage(file, other.get(file));
                } else if (!head.get(file).equals(other.get(file))) {
                    conflict(file, head.get(file), other.get(file));
                }
            } else if (other.containsk(file) && head.containsk(file)) {

                if (split.get(file).equals(head.get(file))
                        && !split.get(file).equals(other.get(file))) { // case 1
                    cwdAndStage(file, other.get(file));
                    continue;
                } else if (!split.get(file).equals(head.get(file))
                        && split.get(file).equals(other.get(file))) { //case 2
                    continue;
                } else if (head.get(file).equals(other.get(file))) { //case 3b
                    continue;
                }

                conflict(file, head.get(file), other.get(file));

                continue;

            } else if (!other.containsk(file) && head.containsk(file)) { //case 6

                if (head.get(file).equals(split.get(file))) {
                    cwdAndRemove(file);
                } else {
                    conflict(file, head.get(file), other.get(file));
                    continue;
                }
            }
        }
        String newMessage = "Merged " + branch + " into " + head.getbranch() + ".";
        Repository.commit(newMessage, getBranchID(branch));
    }


    private static void cwdAndStage(String name, String blob) {
        writeContents(join(CWD, name), blob);
        add(name);
    }

    private static void cwdAndRemove(String name) {
        restrictedDelete(join(CWD, name));
        remove(name);
    }

    private static void conflict(String filename, String head, String branch) {
        if (branch == null) {
            branch = "";
        }
        if (head == null) {
            head = "";
        }
        String newContents = "<<<<<<< HEAD\n" + head + "=======\n" + branch + ">>>>>>>\n";
        writeContents(join(CWD, filename), newContents);
        add(filename);
        System.out.println("Encountered a merge conflict.");
    }

    private static LinkedList<String> makeAList(Commit a, Commit b, Commit c) {
        LinkedList<String> allFiles = new LinkedList<>();

        for (HashMap.Entry<String, String> branch : a.getEntrySet()) {
            String file = branch.getKey();
            if (!allFiles.contains(file)) {
                allFiles.addLast(file);
            }
        }
        for (HashMap.Entry<String, String> branch : b.getEntrySet()) {
            String file = branch.getKey();
            if (!allFiles.contains(file)) {
                allFiles.addLast(file);
            }
        }
        for (HashMap.Entry<String, String> branch : c.getEntrySet()) {
            String file = branch.getKey();
            if (!allFiles.contains(file)) {
                allFiles.addLast(file);
            }
        }
        return allFiles;
    }

    private static boolean caseOne(Commit split, Commit head, Commit other, String file) {
        return split.containsk(file) && head.containsk(file) && other.containsv(file)
                && head.get(file).equals(split.get(file))
                && !other.get(file).equals(split.get(file));
    }

    private static boolean caseTwo(Commit split, Commit head, Commit other, String file) {
        return split.containsk(file) && head.containsk(file) && other.containsv(file)
                && !head.get(file).equals(split.get(file))
                && other.get(file).equals(split.get(file));
    }

    private static boolean caseThreeA(Commit split, Commit head, Commit other, String file) {
        return split.containsk(file) && head.containsk(file) && other.containsv(file)
                && !head.get(file).equals(split.get(file))
                && !other.get(file).equals(split.get(file))
                && head.get(file).equals(other.get(file));
    }

    private static boolean caseThreeB(Commit split, Commit head, Commit other, String file) {
        return split.containsk(file) && head.containsk(file) && other.containsv(file)
                && !head.get(file).equals(split.get(file))
                && !other.get(file).equals(split.get(file))
                && !other.get(file).equals(head.get(file));
    }

    private static boolean caseThreeC(Commit split, Commit head, Commit other, String file) {
        return !split.containsk(file) && head.containsk(file) && other.containsv(file)
                && head.get(file).equals(other.get(file));
    }

    private static boolean caseFour(Commit split, Commit head, Commit other, String file) {
        return !split.containsk(file) && head.containsk(file) && !other.containsv(file);
    }

    private static boolean caseFive(Commit split, Commit head, Commit other, String file) {
        return !split.containsk(file) && !head.containsk(file) && other.containsv(file);
    }

    private static boolean caseSix(Commit split, Commit head, Commit other, String file) {
        return split.containsk(file) && head.containsk(file) && !other.containsv(file)
                && head.get(file).equals(split.get(file));
    }

    private static boolean caseSeven(Commit split, Commit head, Commit other, String file) {
        return split.containsk(file) && !head.containsk(file) && other.containsv(file)
                && other.get(file).equals(split.get(file));
    }
}
