package jp.hatano.gitdiffview;

import java.util.prefs.Preferences;
import javax.swing.DefaultComboBoxModel;

public class EncodingHistoryManager {
    private static final String PREF_KEY_ENCODING_HISTORY = "encodingHistory";
    private final Preferences prefs;

    public EncodingHistoryManager(Class<?> clazz) {
        this.prefs = Preferences.userNodeForPackage(clazz);
    }

    public void saveEncodingHistory(DefaultComboBoxModel<String> model) {
        StringBuilder sb = new StringBuilder();
        int max = Math.min(10, model.getSize());
        for (int i = 0; i < max; i++) {
            sb.append(model.getElementAt(i)).append("\n");
        }
        prefs.put(PREF_KEY_ENCODING_HISTORY, sb.toString());
    }

    public String[] loadEncodingHistory() {
        String encodingHistoryStr = prefs.get(PREF_KEY_ENCODING_HISTORY, "");
        if (!encodingHistoryStr.isEmpty()) {
            return encodingHistoryStr.split("\n");
        }
        return new String[0];
    }

    public void saveLastEncoding(String encoding) {
        String[] history = loadEncodingHistory();
        StringBuilder sb = new StringBuilder();
        sb.append(encoding).append("\n");
        int count = 1;
        for (String enc : history) {
            if (!enc.equals(encoding) && !enc.isEmpty()) {
                sb.append(enc).append("\n");
                count++;
                if (count >= 10) break;
            }
        }
        prefs.put(PREF_KEY_ENCODING_HISTORY, sb.toString());
    }

    public String getLastEncoding() {
        String[] history = loadEncodingHistory();
        if (history.length > 0 && !history[0].isEmpty()) {
            return history[0];
        }
        return "UTF-8";
    }
}
