package jp.hatano.gitdiffview;

import java.io.*;
import java.util.*;

public class RepoHistoryManager {
    private static final String HISTORY_FILE = System.getProperty("user.home") + "/.gitdiffview_repo_history";
    private static final int MAX_HISTORY = 10;
    
    // Load repository history from file
    public static List<String> loadHistory() {
        List<String> list = new ArrayList<>();
        File file = new File(HISTORY_FILE);
        if (!file.exists()) return list;
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (!line.trim().isEmpty() && isGitRepo(line.trim())) {
                    list.add(line.trim());
                }
            }
        } catch (IOException e) {
            // ignore
        }
        return list;
    }
    
    // Save repository history to file
    public static void saveHistory(List<String> history, String selected) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        if (selected != null && !selected.isEmpty() && isGitRepo(selected)) set.add(selected);
        for (String s : history) {
            if (set.size() >= MAX_HISTORY) break;
            if (isGitRepo(s)) set.add(s);
        }
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(HISTORY_FILE))) {
            bw.write(selected);
            bw.newLine();
            for (String s : set) {
                if ( !s.equals(selected) ) {
                    bw.write(s);
                    bw.newLine();
                }
            }
        } catch (IOException e) {
            // ignore
        }
    }
    
    // Check if the given path is a git repository
    public static boolean isGitRepo(String path) {
        if (path == null || path.isEmpty()) return false;
        File gitDir = new File(path, ".git");
        return gitDir.exists() && gitDir.isDirectory();
    }
}
