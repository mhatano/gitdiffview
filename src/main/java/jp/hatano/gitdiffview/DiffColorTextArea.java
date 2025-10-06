package jp.hatano.gitdiffview;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.datatransfer.*;
import java.io.IOException;

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
                String colorStr = fg != null ? String.format("#%02x%02x%02x", fg.getRed(), fg.getGreen(), fg.getBlue()) : null;
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
