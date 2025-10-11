package jp.hatano.gitdiffview;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.charset.Charset;
import java.util.*;
import java.util.prefs.Preferences;

public class GitDiffViewApp extends JFrame {
    private final class DefaultListCellRendererExtension extends DefaultListCellRenderer {
        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            String str = value == null ? "" : value.toString();
            if (str.length() > 30) str = str.substring(0, 28) + "...";
            return super.getListCellRendererComponent(list, str, index, isSelected, cellHasFocus);
        }
    }

    private EncodingHistoryManager encodingHistoryManager;
    private JComboBox<String> repoBox;
    private JButton repoSelectButton;
    private JComboBox<String> branchBox;
    private JButton loadButton;
    private JComboBox<String> commitBox1;
    private JComboBox<String> commitBox2;
    private JButton showFilesButton;
    private JList<String> fileList;
    private DefaultListModel<String> fileListModel;
    private DiffColorTextArea diffArea;
    private JComboBox<String> encodingBox; // Encoding selection
    private java.util.List<String> commitIds = new ArrayList<>();
    private String repoPath = "";
    private String branch = "";
    private String currentEncoding = "UTF-8"; // Default encoding

    // Preferences keys for window position/size and encoding history
    private static final String PREF_KEY_WIN_X = "windowX";
    private static final String PREF_KEY_WIN_Y = "windowY";
    private static final String PREF_KEY_WIN_W = "windowW";
    private static final String PREF_KEY_WIN_H = "windowH";
    private static final String PREF_KEY_LAST_REPOSITORY = "lastRepository";
    private static final String PREF_KEY_ENCODING_HISTORY = "encodingHistory";
    static final String PREF_DIFF_ADD_COLOR = "diffAddColor";
    static final String PREF_DIFF_DEL_COLOR = "diffDelColor";
    static final String PREF_DIFF_HEAD_COLOR = "diffHeadColor";
    Preferences prefs = Preferences.userNodeForPackage(GitDiffViewApp.class);

    // Button to open color scheme dialog
    private JButton colorSchemeButton;
    // Current color scheme
    Color addColor = DiffColorTextArea.darkGreen;
    Color delColor = Color.RED;
    Color headColor = Color.BLUE;

    public GitDiffViewApp() {
        encodingHistoryManager = new EncodingHistoryManager(GitDiffViewApp.class);
        setTitle("Git Diff Viewer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Restore window position and size
        int x = prefs.getInt(PREF_KEY_WIN_X, 100);
        int y = prefs.getInt(PREF_KEY_WIN_Y, 100);
        int w = prefs.getInt(PREF_KEY_WIN_W, 1000);
        int h = prefs.getInt(PREF_KEY_WIN_H, 700);
        setBounds(x, y, w, h);

        setLayout(new BorderLayout());

        // 1st row: Repository & Branch
        JPanel repoPanel = new JPanel();
        repoPanel.setLayout(new BoxLayout(repoPanel, BoxLayout.X_AXIS));
        java.util.List<String> repoHistory = RepoHistoryManager.loadHistory();
        repoBox = new JComboBox<>(repoHistory.toArray(new String[0]));
        repoBox.setEditable(true);
        String lastRepo = prefs.get(PREF_KEY_LAST_REPOSITORY,repoHistory.get(0));
        repoBox.setSelectedItem(lastRepo);
        RepoHistoryManager.saveHistory(repoHistory,lastRepo);
        repoSelectButton = new JButton("...");
        branchBox = new JComboBox<>();
        branchBox.setPreferredSize(new Dimension(220, 26));
        loadButton = new JButton("Load Commits");

        repoPanel.add(Box.createHorizontalStrut(8));
        repoPanel.add(new JLabel("Repo Path:"));
        repoPanel.add(Box.createHorizontalStrut(4));
        repoPanel.add(repoBox);
        repoPanel.add(repoSelectButton);
        repoPanel.add(Box.createHorizontalStrut(8));
        repoPanel.add(new JLabel("Branch:"));
        repoPanel.add(Box.createHorizontalStrut(4));
        repoPanel.add(branchBox);
        repoPanel.add(Box.createHorizontalStrut(8));
        repoPanel.add(loadButton);
        repoPanel.add(Box.createHorizontalGlue());

        // Repository select dialog
        repoSelectButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int ret = chooser.showOpenDialog(this);
            java.util.List<String> hist = RepoHistoryManager.loadHistory();
            if ( ret == JFileChooser.APPROVE_OPTION ) {
                File dir = chooser.getSelectedFile();
                repoBox.setSelectedItem(dir.getAbsolutePath());
                loadBranches();
                String path = dir.getAbsolutePath();
                if ( RepoHistoryManager.isGitRepo(path) ) {
                    hist.add(path);
                }
                RepoHistoryManager.saveHistory(hist,path);
            }
        });
        repoBox.addActionListener(e -> {
            String prev = repoPath;
            String current = repoBox.getEditor().getItem().toString().trim();
            if ( prev != null && !prev.isEmpty() && !prev.equals(current) && RepoHistoryManager.isGitRepo(prev) ) {
                java.util.List<String> hist = RepoHistoryManager.loadHistory();
                for ( int i = 0 ; i < repoBox.getItemCount() ; i ++ ) {
                    String s = repoBox.getItemAt(i);
                    if ( RepoHistoryManager.isGitRepo(s) ) {
                        hist.add(s);
                    }
                }
                RepoHistoryManager.saveHistory(hist,prev);
                if ( ((DefaultComboBoxModel<String>)repoBox.getModel()).getIndexOf(prev) < 0 ) {
                    repoBox.addItem(prev);
                }
            }
            loadBranches();
        });
        repoBox.getEditor().addActionListener(e -> loadBranches());
        branchBox.addActionListener(e -> {
            Object sel = branchBox.getSelectedItem();
            branch = sel == null ? "" : sel.toString();
        });

        // 2nd row: Commit selection + Encoding
        JPanel commitPanel = new JPanel();
        commitPanel.setLayout(new BoxLayout(commitPanel, BoxLayout.X_AXIS));
        commitBox1 = new JComboBox<>();
        commitBox2 = new JComboBox<>();
        Dimension comboSize = new Dimension(220, 26);
        commitBox1.setMaximumSize(comboSize);
        commitBox2.setMaximumSize(comboSize);
        commitBox1.setPreferredSize(comboSize);
        commitBox2.setPreferredSize(comboSize);
        commitBox1.setRenderer(new DefaultListCellRendererExtension());
        commitBox1.addActionListener(e -> fileListModel.clear());
        commitBox2.setRenderer(new DefaultListCellRendererExtension());
        commitBox2.addActionListener(e -> fileListModel.clear());
        showFilesButton = new JButton("Show Diff Files");

        // Encoding selection dropdown (editable, with history)
        java.util.List<String> encodings = new ArrayList<>(Arrays.asList("UTF-8", "Shift_JIS", "EUC-JP", "ISO-8859-1", "US-ASCII"));
        // Load encoding history from preferences
        String encodingHistoryStr = prefs.get(PREF_KEY_ENCODING_HISTORY, "");
        if (!encodingHistoryStr.isEmpty()) {
            String[] hist = encodingHistoryStr.split("\n");
            for (int i = hist.length - 1; i >= 0; i--) {
                String enc = hist[i];
                if (!enc.isEmpty() && !encodings.contains(enc)) {
                    encodings.add(enc);
                }
            }
        }
        // Move last used encoding to the top
        String lastEncoding = encodings.get(0);
        if (!encodingHistoryStr.isEmpty()) {
            String[] hist = encodingHistoryStr.split("\n");
            if (hist.length > 0 && !hist[0].isEmpty()) {
                lastEncoding = hist[0];
            }
        }
        encodings.remove(lastEncoding);
        encodings.add(0, lastEncoding);
        currentEncoding = lastEncoding;

        encodingBox = new JComboBox<>(encodings.toArray(new String[0]));
        encodingBox.setEditable(true);
        encodingBox.setSelectedItem(currentEncoding);
        encodingBox.setMaximumSize(new Dimension(120, 26));

        encodingBox.addActionListener(e -> {
            String selected = (String) encodingBox.getEditor().getItem();
            if (selected == null || selected.isEmpty()) return;
            if (!selected.equals(currentEncoding)) {
                if (Charset.isSupported(selected)) {
                    // Move selected encoding to top and save to history
                    DefaultComboBoxModel<String> model = (DefaultComboBoxModel<String>) encodingBox.getModel();
                    model.removeElement(selected);
                    model.insertElementAt(selected, 0);
                    encodingBox.setSelectedIndex(0);
                    currentEncoding = selected;
                    encodingHistoryManager.saveEncodingHistory((DefaultComboBoxModel<String>) encodingBox.getModel());
                    showDiffForSelectedFile();
                } else {
                    // Show error dialog for unknown encoding
                    JOptionPane.showMessageDialog(this, "Unknown encoding: " + selected, "Encoding Error", JOptionPane.ERROR_MESSAGE);
                    // Revert to previous encoding in the dropdown
                    encodingBox.setSelectedItem(currentEncoding);
                }
            }
        });

        commitPanel.add(Box.createHorizontalStrut(8));
        commitPanel.add(new JLabel("Commit 1:"));
        commitPanel.add(Box.createHorizontalStrut(4));
        commitPanel.add(commitBox1);
        commitPanel.add(Box.createHorizontalStrut(8));
        commitPanel.add(new JLabel("Commit 2:"));
        commitPanel.add(Box.createHorizontalStrut(4));
        commitPanel.add(commitBox2);
        commitPanel.add(Box.createHorizontalStrut(8));
        commitPanel.add(showFilesButton);
        commitPanel.add(Box.createHorizontalStrut(16));
        commitPanel.add(new JLabel("Encoding:"));
        commitPanel.add(Box.createHorizontalStrut(4));
        commitPanel.add(encodingBox);
        commitPanel.add(Box.createHorizontalGlue());

        // Move Config Diff Colors button to the second row (commitPanel)
        colorSchemeButton = new JButton("Config Diff Colors");
        colorSchemeButton.addActionListener(e -> diffArea.showColorSchemeDialog(this));
        commitPanel.add(Box.createHorizontalStrut(8));
        commitPanel.add(colorSchemeButton);

        // Panel for both rows
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.add(repoPanel);
        topPanel.add(commitPanel);
        add(topPanel, BorderLayout.NORTH);

        fileListModel = new DefaultListModel<>();
        fileList = new JList<>(fileListModel);
        JScrollPane fileScroll = new JScrollPane(fileList);
        fileScroll.setPreferredSize(new Dimension(250, 0));
        add(fileScroll, BorderLayout.WEST);

        String strAddColor = prefs.get(PREF_DIFF_ADD_COLOR, "0,128,0");
        String strDelColor = prefs.get(PREF_DIFF_DEL_COLOR, "255,0,0");
        String strHeadColor = prefs.get(PREF_DIFF_HEAD_COLOR, "0,0,255");
        addColor = DiffColorTextArea.parseColor(strAddColor);
        delColor = DiffColorTextArea.parseColor(strDelColor);
        headColor = DiffColorTextArea.parseColor(strHeadColor);

        diffArea = new DiffColorTextArea(addColor, delColor, headColor);

        JScrollPane diffScroll = new JScrollPane(diffArea);
        add(diffScroll, BorderLayout.CENTER);

        loadButton.addActionListener(e -> loadCommits());
        showFilesButton.addActionListener(e -> loadDiffFiles());
        fileList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showDiffForSelectedFile();
            }
        });
        // Clear diff area when commit is selected
        ActionListener clearDiffListener = e -> diffArea.setText("");
        commitBox1.addActionListener(clearDiffListener);
        commitBox2.addActionListener(clearDiffListener);

        // Save window position, size, and encoding history on close
        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                prefs.putInt(PREF_KEY_WIN_X, getX());
                prefs.putInt(PREF_KEY_WIN_Y, getY());
                prefs.putInt(PREF_KEY_WIN_W, getWidth());
                prefs.putInt(PREF_KEY_WIN_H, getHeight());
                prefs.put(PREF_KEY_LAST_REPOSITORY, (String)(repoBox.getSelectedItem()));
                encodingHistoryManager.saveEncodingHistory((DefaultComboBoxModel<String>) encodingBox.getModel());
                diffArea.saveDiffColors(GitDiffViewApp.this);
            }
        });
    }

    // Save encoding history to preferences (top 10, last used at top)
    // Encoding history logic moved to EncodingHistoryManager
    
    private void setWaitCursor(boolean wait) {
        Cursor cursor = wait ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor();
        setCursor(cursor);
        // Also apply to main child components (add more if needed)
        repoBox.setCursor(cursor);
        branchBox.setCursor(cursor);
        commitBox1.setCursor(cursor);
        commitBox2.setCursor(cursor);
        fileList.setCursor(cursor);
        diffArea.setCursor(cursor);
    }

    private void loadCommits() {
        setWaitCursor(true);
        try {
            repoPath = repoBox.getEditor().getItem().toString().trim();
            Object sel = branchBox.getSelectedItem();
            branch = sel == null ? "" : sel.toString();
            commitBox1.removeAllItems();
            commitBox2.removeAllItems();
            commitBox1.setEnabled(false); // Temporarily disable
            commitBox2.setEnabled(false); // Temporarily disable
            commitBox1.revalidate();
            commitBox1.repaint();
            commitBox2.revalidate();
            commitBox2.repaint();
            commitIds.clear();
            int count = 0;
            try {
                ProcessBuilder pb = new ProcessBuilder(
                    "git", "-C", repoPath, "log", branch, "--pretty=format:%H %s"
                );
                Process proc = pb.start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] parts = line.split(" ", 2);
                    if (parts.length > 0) {
                        commitIds.add(parts[0]);
                        String label = parts[0] + (parts.length > 1 ? (" " + parts[1]) : "");
                        commitBox1.addItem(label);
                        commitBox2.addItem(label);
                        count++;
                    }
                }
                if (count > 0) {
                    commitBox1.setSelectedIndex(0);
                    if (commitBox2.getItemCount() > 1) commitBox2.setSelectedIndex(1);
                    commitBox1.setEnabled(true); // Enable if items exist
                    commitBox2.setEnabled(true);
                } else {
                    commitBox1.setEnabled(false); // Just in case
                    commitBox2.setEnabled(false);
                }
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Failed to load commits: " + ex.getMessage());
            }
        } finally {
            setWaitCursor(false);
        }
    }

    // Get local branch list of repository and set to branchBox
    private void loadBranches() {
        setWaitCursor(true);
        try {
            String path = repoBox.getEditor().getItem().toString().trim();
            branchBox.removeAllItems();
            branchBox.setEnabled(false);
            // Clear and disable commit list when updating branch list
            commitBox1.removeAllItems();
            commitBox2.removeAllItems();
            commitBox1.setEnabled(false);
            commitBox2.setEnabled(false);
            commitIds.clear();
            if (path.isEmpty()) return;
            java.util.List<String> branches = new ArrayList<>();
            try {
                ProcessBuilder pb = new ProcessBuilder("git", "-C", path, "branch", "--list");
                Process proc = pb.start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.replace("*", "").trim();
                    if (!line.isEmpty()) branches.add(line);
                }
            } catch (IOException ex) {
                // ignore
            }
            // Default is master if exists, otherwise first
            int defIdx = 0;
            for (int i = 0; i < branches.size(); i++) {
                branchBox.addItem(branches.get(i));
                if (branches.get(i).equals("master")) defIdx = i;
            }
            if (branchBox.getItemCount() > 0) {
                branchBox.setSelectedIndex(defIdx);
                branchBox.setEnabled(true);
            } else {
                branchBox.setEnabled(false);
            }
        } finally {
            setWaitCursor(false);
        }
    }

    private void loadDiffFiles() {
        int idx1 = commitBox1.getSelectedIndex();
        int idx2 = commitBox2.getSelectedIndex();
        int maxIdx = commitIds.size() - 1;
        if (idx1 < 0 || idx2 < 0 || idx1 > maxIdx || idx2 > maxIdx || idx1 == idx2) return;
        // Clear file list every time to prevent duplicates
        fileListModel.clear();
        String c1 = commitIds.get(idx1);
        String c2 = commitIds.get(idx2);
        try {
            // diff in order: commit2, commit1
            ProcessBuilder pb = new ProcessBuilder(
            "git", "-C", repoPath, "diff", "--name-only", c2, c1
            );
            Process proc = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                fileListModel.addElement(line);
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Failed to load diff files: " + ex.getMessage());
        }
    }

    private void showDiffForSelectedFile() {
        String file = fileList.getSelectedValue();
        if (file == null) return;
        int idx1 = commitBox1.getSelectedIndex();
        int idx2 = commitBox2.getSelectedIndex();
        int maxIdx = commitIds.size() - 1;
        if (idx1 < 0 || idx2 < 0 || idx1 > maxIdx || idx2 > maxIdx || idx1 == idx2) return;
        String c1 = commitIds.get(idx1);
        String c2 = commitIds.get(idx2);
        try {
            // Use selected encoding for diff output
            ProcessBuilder pb = new ProcessBuilder(
                "git", "-C", repoPath, "diff", c2, c1, "--", file
            );
            Process proc = pb.start();
            InputStream is = proc.getInputStream();
            InputStreamReader isr = new InputStreamReader(is, Charset.forName(currentEncoding));
            BufferedReader reader = new BufferedReader(isr);
            StringBuilder diff = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                diff.append(line).append("\n");
            }
            diffArea.setDiffText(diff.toString());
            // Scroll to top after setting diff text
            SwingUtilities.invokeLater(() -> diffArea.setCaretPosition(0));
        } catch (IOException ex) {
            diffArea.setText("Failed to load diff: " + ex.getMessage());
            SwingUtilities.invokeLater(() -> diffArea.setCaretPosition(0));
        }
    }

    public static void main(String[] options) {
        SwingUtilities.invokeLater(() -> {
            GitDiffViewApp app = new GitDiffViewApp();
            // Set last selected repository
            java.util.List<String> history = RepoHistoryManager.loadHistory();
            if (!history.isEmpty()) {
                app.repoBox.setSelectedItem(history.get(0));
                app.loadBranches();
            }
            app.setVisible(true);
        });
    }
}
