package jp.hatano.gitdiffview;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class RepoHistoryManager {
    private static final Logger LOGGER = Logger.getLogger(RepoHistoryManager.class.getName());
    private static final Path HISTORY_FILE_PATH = Paths.get(System.getProperty("user.home"), ".gitdiffview_repo_history");
    private static final int MAX_HISTORY = 10;
    
    // Load repository history from file
    public static List<String> loadHistory() {
        if (!Files.exists(HISTORY_FILE_PATH)) {
            return new ArrayList<>();
        }
        try {
            return Files.lines(HISTORY_FILE_PATH, StandardCharsets.UTF_8)
                    .map(String::trim)
                    .filter(line -> !line.isEmpty() && isGitRepo(line))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to load repository history", e);
            return new ArrayList<>();
        }
    }
    
    // Save repository history to file
    public static void saveHistory(List<String> history, String selected) {
        LinkedHashSet<String> set = new LinkedHashSet<>();
        if (selected != null && !selected.isEmpty() && isGitRepo(selected)) {
            set.add(selected);
        }
        for (String s : history) {
            if (set.size() >= MAX_HISTORY) break;
            if (isGitRepo(s)) {
                set.add(s);
            }
        }
        try {
            Files.write(HISTORY_FILE_PATH, set, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to save repository history", e);
        }
    }
    
    // Check if the given path is a git repository
    public static boolean isGitRepo(String path) {
        if (path == null || path.isEmpty()) return false;
        return Files.isDirectory(Paths.get(path, ".git"));
    }
}
