package jp.hatano.gitdiffview;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;

public class DiffColorTextArea extends JTextPane {
    private final Color addColor;
    private final Color delColor;
    private final Color headColor;

    public DiffColorTextArea(final boolean color) {
        if ( color ) {
            addColor = Color.RED;
            delColor = Color.BLUE;
            headColor = Color.BLACK;
        } else {
            addColor = Color.GREEN;
            delColor = Color.RED;
            headColor = Color.BLUE;
        }
        setEditable(false);
        setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
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
