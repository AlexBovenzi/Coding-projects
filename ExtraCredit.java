package gitlet;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import static gitlet.Repository.*;
import static gitlet.Utils.*;

public class ExtraCredit {

    @SuppressWarnings("unchecked")
    public static void untrackedStatus() {
        List<String> dir = plainFilenamesIn(CWD);
        Commit c = Commit.readHeadCommit();
        TreeMap<String, String> stage = readObject(STAGINGAREA, TreeMap.class);
        for (String s: dir) {
            if (!c.containsk(s) && !stage.containsKey(s)) {
                System.out.println(s);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public static void modsStatus() {
        List<String> dir = plainFilenamesIn(CWD);
        Commit c = Commit.readHeadCommit();
        ArrayList<String> remove = readObject(TOBEREMOVED, ArrayList.class);
        for (HashMap.Entry<String, String> file : c.getEntrySet()) {
            if (dir.contains(file.getKey())) {
                if (!readContentsAsString(join(CWD, file.getKey())).equals(file.getValue())) {
                    System.out.println(file.getKey() + " (modified)");
                }
            } else {
                if (!remove.contains(file.getKey())) {
                    System.out.println(file.getKey() + " (deleted)");
                }
            }
        }
    }
}
