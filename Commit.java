package gitlet;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;


import static gitlet.Repository.*;
import static gitlet.Utils.*;

/** Represents a gitlet commit object.
 *  does at a high level.
 *
 *  @author Alex Bovenzi
 */
public class Commit implements Serializable {
    /**
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     *
     */

    /** The message of this Commit. */
    private String message;
    private String parent;
    private Date timestamp;
    private String branch;
    private String otherParent;
    private TreeMap<String, String> committedFiles = new TreeMap<>();



    /** This is where the commit part of Init happens.
     */
    public Commit() {

        message = "initial commit";
        parent = null;
        otherParent = null;
        timestamp = new Date(0);
        branch = MASTER;

        HashMap<String, Commit> repo = new HashMap<>();
        TreeMap<String, String> stage = new TreeMap<>();

        TreeMap<String, String> branches = new TreeMap<>();
        ArrayList<String> removal = new ArrayList<>();

        String hash = sha1(serialize(this));
        repo.put(hash, this);
        branches.put(MASTER, hash);
        writeObject(REPOSITORY, repo);
        writeObject(STAGINGAREA, stage);
        writeContents(HEAD, hash);
        writeObject(BRANCHES, branches);
        writeObject(TOBEREMOVED, removal);
        writeContents(CURRENTBRANCH, MASTER);

    }

    /** This method handles the Commit call
     * @param m = message for commit
     */
    @SuppressWarnings("unchecked")
    public Commit(String m, String op) {
        message = m;
        timestamp = new Date();
        parent = readContentsAsString(HEAD);
        branch = Checkout.currentBranch();
        otherParent = op;
        TreeMap<String, String> stagingMap;
        ArrayList<String> remove;
        remove = readObject(TOBEREMOVED, ArrayList.class);

        //Check that the staging area is not empty
        stagingMap = readObject(STAGINGAREA, TreeMap.class);
        if (stagingMap.isEmpty() && remove.isEmpty()) {
            System.out.println("No changes added to the commit.");
            return;
        }

        //get head commit
        Commit head = Commit.readHeadCommit();

        //add all items from the staging area
        for (HashMap.Entry<String, String> stage : stagingMap.entrySet()) {
            head.committedFiles.put(stage.getKey(), stage.getValue());
        }

        //Remove the items staged for removal
        for (String toRemove: remove) {
            head.committedFiles.remove(toRemove);
        }
        remove.clear();
        writeObject(TOBEREMOVED, remove);

        // make the new commit contain all necessary files to track
        this.committedFiles = head.committedFiles;

        //make new head UID head hash
        String newHead = sha1(serialize(this));

        HashMap<String, Commit> repo = readObject(REPOSITORY, HashMap.class);
        //put the new hash into the the repository
        repo.put(newHead, this);

        //write the resulting contents
        writeObject(REPOSITORY, repo);

        // clear the staging area and resave it
        stagingMap.clear();
        writeObject(STAGINGAREA, stagingMap);

        //maybe....?
        TreeMap<String, String> branches;
        branches = readObject(BRANCHES, TreeMap.class);
        //branches.put(Checkout.IDtobranch(readHeadID()), NewHead);
        branches.put(branch, newHead);
        writeObject(BRANCHES, branches);

        //update head pointer and save
        writeContents(HEAD, newHead);

    }

    @SuppressWarnings("unchecked")
    public static Commit readHeadCommit() {
        String headhash = readContentsAsString(HEAD);
        HashMap<String, Commit> repositorymap = readObject(REPOSITORY, HashMap.class);
        return repositorymap.get(headhash);
    }
    public static String readHeadID() {
        return readContentsAsString(HEAD);
    }
    @SuppressWarnings("unchecked")
    public static Commit readSpecificCommit(String id) {
        HashMap<String, Commit> repositorymap = readObject(REPOSITORY, HashMap.class);
        for (HashMap.Entry<String, Commit> repo : repositorymap.entrySet()) {
            if (repo.getKey().equals(id) || repo.getKey().substring(0, 6).equals(id)) {
                return repositorymap.get(repo.getKey());
            }
        }
        System.out.println("No commit with that id exists.");
        System.exit(0);
        return null;
    }
    public static void log(Commit C) {
        C.printCommit();
        if (C.parent != null) {
            log(readSpecificCommit(C.parent));
        }
    }
    public void printCommit() {
        SimpleDateFormat s = new SimpleDateFormat("EEE MMM d HH:mm:ss yyyy Z");
        System.out.println("===");
        System.out.println("commit " + sha1(serialize(this)));
        System.out.println("Date: " + s.format(this.timestamp));
        System.out.println(this.message);
        System.out.println();
    }
    public boolean messageCompare(String s) {
        return message.equals(s);
    }

    public static LinkedList<String> getCommitChain(String c) {
        LinkedList<String> ids = new LinkedList<>();
        ids.addLast(c);
        Commit cur = readSpecificCommit(c);
        while (cur.parent != null) {
            ids.addLast(cur.parent);
            if (cur.otherParent != null) {
                ids.addLast(cur.otherParent);
            }
            cur = readSpecificCommit(cur.parent);
        }
        return ids;
    }
    public String getParent() {
        return this.parent;
    }
    public String getMessage() {
        return this.message;
    }
    public String getbranch() {
        return this.branch;
    }
    public String get(String k) {
        return committedFiles.get(k);
    }
    public boolean containsk(String k) {
        return committedFiles.containsKey(k);
    }
    public boolean containsv(String v) {
        return committedFiles.containsValue(v);
    }
    public TreeMap<String, String> getTreeMap() {
        return committedFiles;
    }
    public Set<Map.Entry<String, String>> getEntrySet() {
        return committedFiles.entrySet();
    }
}
