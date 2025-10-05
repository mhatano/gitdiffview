package jp.hatano.gitdiffview;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;

public class GitDiffViewApp extends JFrame {
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
    private java.util.List<String> commitIds = new ArrayList<>();
    private String repoPath = "";
    private String branch = "";
    
    public GitDiffViewApp() {
        setTitle("Git Diff Viewer");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());
        
        // 1段目: リポジトリ・ブランチ
        JPanel repoPanel = new JPanel();
        repoPanel.setLayout(new BoxLayout(repoPanel, BoxLayout.X_AXIS));
        java.util.List<String> repoHistory = RepoHistoryManager.loadHistory();
        repoBox = new JComboBox<>(repoHistory.toArray(new String[0]));
        repoBox.setEditable(true);
        if (!repoHistory.isEmpty()) repoBox.setSelectedIndex(0);
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
        
        // リポジトリ選択ダイアログ
        repoSelectButton.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
            int ret = chooser.showOpenDialog(this);
            if (ret == JFileChooser.APPROVE_OPTION) {
                File dir = chooser.getSelectedFile();
                repoBox.setSelectedItem(dir.getAbsolutePath());
                loadBranches();
            }
        });
        // ComboBoxでパス入力時もブランチ更新
        repoBox.addActionListener(e -> {
            String prev = repoPath;
            String current = repoBox.getEditor().getItem().toString().trim();
            if (prev != null && !prev.isEmpty() && !prev.equals(current) && RepoHistoryManager.isGitRepo(prev)) {
                java.util.List<String> hist = new java.util.ArrayList<>();
                for (int i = 0; i < repoBox.getItemCount(); i++) {
                    String s = repoBox.getItemAt(i);
                    if (RepoHistoryManager.isGitRepo(s)) hist.add(s);
                }
                RepoHistoryManager.saveHistory(hist, prev);
                // --- 追加: ドロップダウンも即時更新 ---
                if (((DefaultComboBoxModel<String>)repoBox.getModel()).getIndexOf(prev) < 0) {
                    repoBox.addItem(prev);
                }
            }
            loadBranches();
        });
        repoBox.getEditor().addActionListener(e -> loadBranches());
        // ブランチ選択時に値をセット
        branchBox.addActionListener(e -> {
            Object sel = branchBox.getSelectedItem();
            branch = sel == null ? "" : sel.toString();
        });
        
        // 2段目: コミット選択
        JPanel commitPanel = new JPanel();
        commitPanel.setLayout(new BoxLayout(commitPanel, BoxLayout.X_AXIS));
        commitBox1 = new JComboBox<>();
        commitBox2 = new JComboBox<>();
        Dimension comboSize = new Dimension(220, 26);
        commitBox1.setMaximumSize(comboSize);
        commitBox2.setMaximumSize(comboSize);
        commitBox1.setPreferredSize(comboSize);
        commitBox2.setPreferredSize(comboSize);
        commitBox1.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                String str = value == null ? "" : value.toString();
                if (str.length() > 30) str = str.substring(0, 28) + "...";
                return super.getListCellRendererComponent(list, str, index, isSelected, cellHasFocus);
            }
        });
        commitBox1.addActionListener(e -> fileListModel.clear());
        commitBox2.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                String str = value == null ? "" : value.toString();
                if (str.length() > 30) str = str.substring(0, 28) + "...";
                return super.getListCellRendererComponent(list, str, index, isSelected, cellHasFocus);
            }
        });
        commitBox2.addActionListener(e -> fileListModel.clear());
        showFilesButton = new JButton("Show Diff Files");
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
        commitPanel.add(Box.createHorizontalGlue());
        
        // 2段をまとめるパネル
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
        
        diffArea = new DiffColorTextArea();
        JScrollPane diffScroll = new JScrollPane(diffArea);
        add(diffScroll, BorderLayout.CENTER);
        
        loadButton.addActionListener(e -> loadCommits());
        showFilesButton.addActionListener(e -> loadDiffFiles());
        fileList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                showDiffForSelectedFile();
            }
        });
        // コミット選択時にdiffエリアをクリア
        ActionListener clearDiffListener = e -> diffArea.setText("");
        commitBox1.addActionListener(clearDiffListener);
        commitBox2.addActionListener(clearDiffListener);
    }
    
    private void loadCommits() {
        repoPath = repoBox.getEditor().getItem().toString().trim();
        Object sel = branchBox.getSelectedItem();
        branch = sel == null ? "" : sel.toString();
        commitBox1.removeAllItems();
        commitBox2.removeAllItems();
        commitBox1.setEnabled(false); // 追加: 一旦disable
        commitBox2.setEnabled(false); // 追加: 一旦disable
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
                commitBox1.setEnabled(true); // 要素があればenable
                commitBox2.setEnabled(true);
            } else {
                commitBox1.setEnabled(false); // 念のため
                commitBox2.setEnabled(false);
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Failed to load commits: " + ex.getMessage());
        }
    }
    
    // リポジトリのローカルブランチ一覧を取得し、branchBoxにセット
    private void loadBranches() {
        String path = repoBox.getEditor().getItem().toString().trim();
        branchBox.removeAllItems();
        branchBox.setEnabled(false);
        // --- 追加: ブランチリストを更新する際にコミットリストもクリアして無効化 ---
        commitBox1.removeAllItems();
        commitBox2.removeAllItems();
        commitBox1.setEnabled(false);
        commitBox2.setEnabled(false);
        commitIds.clear();
        // --- ここまで追加 ---
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
        // デフォルトはmaster優先、なければ最初
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
    }
    
    private void loadDiffFiles() {
        int idx1 = commitBox1.getSelectedIndex();
        int idx2 = commitBox2.getSelectedIndex();
        int maxIdx = commitIds.size() - 1;
        if (idx1 < 0 || idx2 < 0 || idx1 > maxIdx || idx2 > maxIdx || idx1 == idx2) return;
        // --- 追加: ファイルリストの重複防止のため毎回クリア ---
        fileListModel.clear();
        // --- ここまで追加 ---
        String c1 = commitIds.get(idx1);
        String c2 = commitIds.get(idx2);
        try {
            // commit2, commit1の順でdiff
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
            // commit2, commit1の順でdiff
            ProcessBuilder pb = new ProcessBuilder(
            "git", "-C", repoPath, "diff", c2, c1, "--", file
            );
            Process proc = pb.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()));
            StringBuilder diff = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                diff.append(line).append("\n");
            }
            diffArea.setDiffText(diff.toString());
        } catch (IOException ex) {
            diffArea.setText("Failed to load diff: " + ex.getMessage());
        }
    }
    
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GitDiffViewApp app = new GitDiffViewApp();
            // 前回選択リポジトリをセット
            java.util.List<String> history = RepoHistoryManager.loadHistory();
            if (!history.isEmpty()) {
                app.repoBox.setSelectedItem(history.get(0));
                app.loadBranches();
            }
            app.addWindowListener(new java.awt.event.WindowAdapter() {
                public void windowClosing(java.awt.event.WindowEvent e) {
                    // 履歴保存
                    java.util.List<String> hist = new java.util.ArrayList<>();
                    for (int i = 0; i < app.repoBox.getItemCount(); i++) {
                        String s = app.repoBox.getItemAt(i);
                        if (RepoHistoryManager.isGitRepo(s)) hist.add(s);
                    }
                    String selected = app.repoBox.getEditor().getItem().toString();
                    RepoHistoryManager.saveHistory(hist, selected);
                }
            });
            app.setVisible(true);
        });
    }
}
