package jp.hatano.gitdiffview;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;

public class DiffColorTextArea extends JTextPane {
    public DiffColorTextArea() {
        setEditable(false);
        setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
    }
    
    public void setDiffText(String diff) {
        setText("");
        StyledDocument doc = getStyledDocument();
        StyleContext sc = StyleContext.getDefaultStyleContext();
        Style defaultStyle = sc.getStyle(StyleContext.DEFAULT_STYLE);
        Style addStyle = doc.addStyle("add", null);
        StyleConstants.setForeground(addStyle, new Color(0, 128, 0));
        StyleConstants.setBold(addStyle, true);
        Style delStyle = doc.addStyle("del", null);
        StyleConstants.setForeground(delStyle, Color.RED);
        StyleConstants.setBold(delStyle, true);
        Style headStyle = doc.addStyle("head", null);
        StyleConstants.setForeground(headStyle, Color.BLUE);
        StyleConstants.setBold(headStyle, true);
        try {
            for (String line : diff.split("\n")) {
                if (line.startsWith("+")) {
                    doc.insertString(doc.getLength(), line + "\n", addStyle);
                } else if (line.startsWith("-")) {
                    doc.insertString(doc.getLength(), line + "\n", delStyle);
                } else if (line.startsWith("@@") || line.startsWith("diff") || line.startsWith("index") || line.startsWith("---") || line.startsWith("+++")) {
                    doc.insertString(doc.getLength(), line + "\n", headStyle);
                } else {
                    doc.insertString(doc.getLength(), line + "\n", defaultStyle);
                }
            }
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
}
