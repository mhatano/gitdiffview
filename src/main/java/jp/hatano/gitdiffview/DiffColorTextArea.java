package jp.hatano.gitdiffview;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.ItemListener;

public class DiffColorTextArea extends JTextPane {
    public static final Color darkGreen = new Color(0,128,0);
    private Color addColor = darkGreen;
    private Color delColor = Color.RED;
    private Color headColor = Color.BLUE;
    
    private static final String[] FONT_SELECTIONS = { "Consolas", "Menlo", "Dejavu Sans Mono" };

    public DiffColorTextArea() {
        setEditable(false);
        Font currFont = getFont();
        Font newFont = null;
        for (String fontName : FONT_SELECTIONS) {
            newFont = new Font(fontName, currFont.getStyle(), 14);
            if ( !newFont.getFamily().equals("Dialog") ) break;
        }
        setFont(newFont);
    }

    public DiffColorTextArea(final boolean color) {
        this();
        if (color) {
            setColors(Color.RED, Color.BLUE, Color.BLACK);
        } else {
            setColors(darkGreen, Color.RED, Color.BLUE);
        }
    }
    
    public DiffColorTextArea(Color addColor, Color delColor, Color headColor) {
        this();
        this.addColor = addColor;
        this.delColor = delColor;
        this.headColor = headColor;
    }
    
    public void setColors(Color add, Color del, Color head) {
        this.addColor = add;
        this.delColor = del;
        this.headColor = head;
        // Reset text to redraw with new colors
        setDiffText(getText());
    }
    
    public void setDiffText(String diff) {
        setText("");
        StyledDocument doc = getStyledDocument();
        StyleContext sc = StyleContext.getDefaultStyleContext();
        Style defaultStyle = sc.getStyle(StyleContext.DEFAULT_STYLE);
        Style addStyle = doc.addStyle("add", null);
        StyleConstants.setForeground(addStyle, addColor);
        StyleConstants.setBold(addStyle, true);
        Style delStyle = doc.addStyle("del", null);
        StyleConstants.setForeground(delStyle, delColor);
        StyleConstants.setBold(delStyle, true);
        Style headStyle = doc.addStyle("head", null);
        StyleConstants.setForeground(headStyle, headColor);
        StyleConstants.setBold(headStyle, true);
        try {
            for (String line : diff.split("\n")) {
                if (line.startsWith("+")) {
                    doc.insertString(doc.getLength(), line + "\n", addStyle);
                } else if (line.startsWith("-")) {
                    doc.insertString(doc.getLength(), line + "\n", delStyle);
                } else if ( startsWithAnyOf(line,"@@","diff","index","---","+++")) {
                    doc.insertString(doc.getLength(), line + "\n", headStyle);
                } else {
                    doc.insertString(doc.getLength(), line + "\n", defaultStyle);
                }
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
    
    private boolean startsWithAnyOf(String line, String... prefixes) {
        for (String prefix : prefixes) {
            if (line.startsWith(prefix)) return true;
        }
        return false;
    }

    @Override
    public void copy() {
        // get selected text range
        int start = getSelectionStart();
        int end = getSelectionEnd();
        if (start == end) return; // nothing selected
        try {
            StyledDocument doc = getStyledDocument();
            StringBuilder plain = new StringBuilder();
            StringBuilder html = new StringBuilder();
            html.append("<html><body><pre style='font-family:monospace;font-size:14px;'>");
            for (int i = start; i < end; ) {
                Element elem = doc.getCharacterElement(i);
                AttributeSet attr = elem.getAttributes();
                String text = doc.getText(i, elem.getEndOffset() - i);
                // Remove trailing \n for html (keep for plain)
                String htmlText = text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
                Color fg = StyleConstants.getForeground(attr);
                boolean bold = StyleConstants.isBold(attr);
                String colorStr = fg != null ? getColorString(fg) : null;
                html.append("<span style='");
                if (colorStr != null) html.append("color:" + colorStr + ";");
                if (bold) html.append("font-weight:bold;");
                html.append("'>").append(htmlText).append("</span>");
                plain.append(text);
                i = elem.getEndOffset();
            }
            html.append("</pre></body></html>");
            // Set both plain and html to clipboard
            Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
            clipboard.setContents(new HtmlSelection(plain.toString(), html.toString()), null);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
    
    void saveDiffColors(GitDiffViewApp gitDiffViewApp) {
        String strAddColor = getColorString(gitDiffViewApp.addColor);
        String strDelColor = getColorString(gitDiffViewApp.delColor);
        String strHeadColor = getColorString(gitDiffViewApp.headColor);
        gitDiffViewApp.prefs.put(GitDiffViewApp.PREF_DIFF_ADD_COLOR, strAddColor);
        gitDiffViewApp.prefs.put(GitDiffViewApp.PREF_DIFF_DEL_COLOR, strDelColor);
        gitDiffViewApp.prefs.put(GitDiffViewApp.PREF_DIFF_HEAD_COLOR, strHeadColor);
    }
    
    private String getColorString(Color color) {
        return String.format("%d,%d,%d", color.getRed(), color.getGreen(), color.getBlue());
    }

    void showColorSchemeDialog(GitDiffViewApp gitDiffViewApp) {
        JDialog dialog = new JDialog(gitDiffViewApp, "Config Diff Colors", true);
        dialog.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5,5,5,5);
        gbc.gridx = 0; gbc.gridy = 0;
        dialog.add(new JLabel("Added Lines:"), gbc);
        gbc.gridy++;
        dialog.add(new JLabel("Removed Lines:"), gbc);
        gbc.gridy++;
        dialog.add(new JLabel("Header Lines:"), gbc);
        
        String[] colorNames = {"GREEN", "RED", "BLUE", "BLACK", "CYAN", "MAGENTA", "ORANGE", "PINK", "YELLOW", "GRAY", "BRIGHTGREEN"};
        Color[] colorValues = {darkGreen, Color.RED, Color.BLUE, Color.BLACK, Color.CYAN, Color.MAGENTA, Color.ORANGE, Color.PINK, Color.YELLOW, Color.GRAY, Color.GREEN};
        JComboBox<String> addBox = new JComboBox<>(colorNames);
        JComboBox<String> delBox = new JComboBox<>(colorNames);
        JComboBox<String> headBox = new JComboBox<>(colorNames);
        gbc.gridx = 1; gbc.gridy = 0;
        dialog.add(addBox, gbc);
        gbc.gridy++;
        dialog.add(delBox, gbc);
        gbc.gridy++;
        dialog.add(headBox, gbc);
        
        JButton okBtn = new JButton("OK");
        JButton cancelBtn = new JButton("Cancel");
        gbc.gridx = 0; gbc.gridy++;
        dialog.add(okBtn, gbc);
        gbc.gridx = 1;
        dialog.add(cancelBtn, gbc);
        
        // Reflect the current colors as the default selection
        int addIdx = 0, delIdx = 1, headIdx = 2;
        for (int i = 0; i < colorValues.length; i++) {
            if (gitDiffViewApp.addColor.equals(colorValues[i])) addIdx = i;
            if (gitDiffViewApp.delColor.equals(colorValues[i])) delIdx = i;
            if (gitDiffViewApp.headColor.equals(colorValues[i])) headIdx = i;
        }
        addBox.setSelectedIndex(addIdx);
        delBox.setSelectedIndex(delIdx);
        headBox.setSelectedIndex(headIdx);
        
        JButton convBtn = new JButton("Select Conventional Colors");
        gbc.gridx = 0; gbc.gridy++;
        gbc.gridwidth = 2;
        dialog.add(convBtn, gbc);
        gbc.gridwidth = 1;
        
        convBtn.addActionListener(e -> {
            addBox.setSelectedItem("GREEN");
            delBox.setSelectedItem("RED");
            headBox.setSelectedItem("BLUE");
        });
        
        okBtn.setEnabled(!addBox.getSelectedItem().equals(delBox.getSelectedItem()));
        ItemListener checkListener = e -> okBtn.setEnabled(!addBox.getSelectedItem().equals(delBox.getSelectedItem()));
        addBox.addItemListener(checkListener);
        delBox.addItemListener(checkListener);
        
        okBtn.addActionListener(e -> {
            gitDiffViewApp.addColor = colorValues[addBox.getSelectedIndex()];
            gitDiffViewApp.delColor = colorValues[delBox.getSelectedIndex()];
            gitDiffViewApp.headColor = colorValues[headBox.getSelectedIndex()];
            setColors(gitDiffViewApp.addColor, gitDiffViewApp.delColor, gitDiffViewApp.headColor);
            dialog.dispose();
        });
        cancelBtn.addActionListener(e -> dialog.dispose());
        dialog.pack();
        dialog.setLocationRelativeTo(gitDiffViewApp);
        dialog.setVisible(true);
    }
    
    public static Color parseColor(String string) {
        try {
            String[] parts = string.split(",");
            if ( parts.length != 3 ) return Color.BLACK;

            int color[] = new int[parts.length];
            for ( int i = 0; i < 3 ; i++ ) {
                String part = parts[i].trim();
                if ( part.startsWith("#") ) {
                    color[i] = Integer.parseInt(part.substring(1),16);
                } else if ( part.startsWith("0") ) {
                    color[i] = Integer.parseInt(part,8);
                } else {
                    color[i] = Integer.parseInt(part);    
                }
            }
            return newColor(color);
        } catch (Exception e) {
            return Color.BLACK;
        }
    }

    private static Color newColor(int[] color) {
        return new Color(
            Math.min(Math.max(color[0], 0), 255),
            Math.min(Math.max(color[1], 0), 255),
            Math.min(Math.max(color[2], 0), 255)            
        );
    }
    
    static class HtmlSelection implements Transferable {
        private final String plain;
        private final String html;
        private static final DataFlavor[] flavors = {
            DataFlavor.stringFlavor,
            new DataFlavor("text/html;class=java.lang.String", "HTML Format")
        };
        HtmlSelection(String plain, String html) {
            this.plain = plain;
            this.html = html;
        }
        @Override
        public DataFlavor[] getTransferDataFlavors() { return flavors; }
        @Override
        public boolean isDataFlavorSupported(DataFlavor flavor) {
            for (DataFlavor f : flavors) if (f.equals(flavor)) return true;
            return false;
        }
        @Override
        public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException {
            if (flavor.equals(flavors[0])) return plain;
            if (flavor.equals(flavors[1])) return html;
            throw new UnsupportedFlavorException(flavor);
        }
    }
}
