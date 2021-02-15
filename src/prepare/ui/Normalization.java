package prepare.ui;

import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

public class Normalization {
    static final Locale BE = new Locale("be");
    static final String LETTERS = "-'’ёйцукенгґшўзхфывапролджэячсмітьбюищъѣqwertyuiopasdfghjklzxcvbnmćłęΣѲѳχΒίβύσμαέυάөνεδζλονιėžωτπρӨü";
    static final String WORD = "´\u0302\u0306\u0308";
    static final String NOWORD = ",.:;!? {}()0123456789\n–—№«»[]\"½“”…=";
    static final String AS_WORD = LETTERS + WORD + LETTERS.toUpperCase(BE) + "ґ";

    public static String normalize(String str) {
        StringBuilder o = new StringBuilder();
        for (char c : str.trim().toLowerCase(BE).toCharArray()) {
            if (LETTERS.indexOf(c) >= 0) {
                o.append(c);
            } else if (WORD.indexOf(c) >= 0) {
            } else if (NOWORD.indexOf(c) >= 0) {
            } else if (c == 'ґ') {
                o.append('г');
            } else {
                System.out.println("Unknown char in norm: " + c + " - " + Integer.toHexString((int) c) + ": " + str);
            }
        }
        return o.toString();
    }

    private static void add(Set<String> result, StringBuilder buffer) {
        String s = buffer.toString();
        buffer.setLength(0);
        result.add(normalize(s));
        if (s.contains("-")) {
            for (String w : s.split("\\-")) {
                result.add(normalize(w));
            }
        }
    }

    public static Set<String> split(String str, String place) {
        Set<String> r = new TreeSet<>();
        StringBuilder w = new StringBuilder();
        boolean inWord = false;
        for (int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (AS_WORD.indexOf(c) >= 0) {
                inWord = true;
                w.append(c);
            } else if (NOWORD.indexOf(c) >= 0) {
                if (inWord) {
                    add(r,w);
                    inWord = false;
                }
            } else if (c == '<') {
                if (inWord) {
                    add(r,w);
                    inWord = false;
                }
                while (i < str.length() && str.charAt(i) != '>')
                    i++;
            } else {
                System.out.println("Unknown char in "+place+": " + c + " - " + Integer.toHexString((int) c) + ": " + str);
            }
        }
        if (inWord) {
            add(r,w);
        }
        return r;
    }
}
