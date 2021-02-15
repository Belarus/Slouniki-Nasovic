package prepare.html;

import java.util.ArrayList;
import java.util.List;

public class JoinWithSeparators implements IHtmlPart {
    private final String sep, lastSep;
    protected List<IHtmlPart> children = new ArrayList<>();

    public JoinWithSeparators(String sep, String lastSep) {
        this.sep = sep;
        this.lastSep = lastSep;
    }

    public JoinWithSeparators text(String text) {
        if (text != null) {
            children.add(new Part("text", text, escapeText(text), true));
        }
        return this;
    }

    public JoinWithSeparators textPseudoHTML(String text) {
        if (text != null) {
            children.add(new Part("text", text, escapeTextPseudoHTML(text), true));
        }
        return this;
    }

    public JoinWithSeparators requiredTag(String tag) {
        children.add(new Part("tag", tag, tag, true));
        return this;
    }

    public JoinWithSeparators tag(String tag) {
        children.add(new Part("tag", tag, tag, false));
        return this;
    }

    public JoinWithSeparators add(IHtmlPart part) {
        if (part == null) {
            throw new RuntimeException("Part can't be null");
        }
        children.add(part);
        return this;
    }

    public JoinWithSeparators join() {
        JoinWithSeparators r = new JoinWithSeparators("", "");
        children.add(r);
        return r;
    }

    public JoinWithSeparators join(String tagSep) {
        JoinWithSeparators r = new JoinWithSeparators(tagSep, tagSep);
        children.add(r);
        return r;
    }

    public JoinWithSeparators join(String tagSep, String tagLastSep) {
        JoinWithSeparators r = new JoinWithSeparators(tagSep, tagLastSep);
        children.add(r);
        return r;
    }

    public void clear() {
        children.clear();
    }

    /**
     * It removes child levels if there is no required texts.
     */
    public String normalize() {
        for (int i = 0; i < children.size(); i++) {
            if (children.get(i) instanceof JoinWithSeparators) {
                ((JoinWithSeparators) children.get(i)).normalize();
                if (children.get(i).isEmpty()) {
                    children.remove(i);
                    i--;
                }
            }
        }
        return toString();
    }

    @Override
    public boolean isEmpty() {
        for (IHtmlPart p : children) {
            if (!p.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean notEmpty() {
        return !isEmpty();
    }

    @Override
    public void out(StringBuilder out) {
        for (int i = 0; i < children.size(); i++) {
            if (i == 0) {
            } else if (i == children.size() - 1) {
                if (lastSep.length() > 0) {
                    out.append(lastSep);
                }
            } else {
                if (sep.length() > 0) {
                    out.append(sep);
                }
            }
            children.get(i).out(out);
        }
    }

    public String out() {
        StringBuilder o = new StringBuilder();
        out(o);
        return o.toString();
    }

    public boolean endsWith(String suffix) {
        return out().replaceAll("<[^>]+>", "").endsWith(suffix);
    }

    public void dump(String prefix) {
        System.err.println(prefix + "== join");
        for (IHtmlPart ch : children) {
            ch.dump(prefix + "  ");
        }
    }

    static String escapeText(String text) {
        String t = text;
        t = t.replace('+', '\u0301');
        t = t.replace("&", "&amp;");
        t = t.replace("<", "&lt;");
        t = t.replace(">", "&gt;");
        t = t.replace(" -- ", " &mdash; ");
        return t;
    }

    static String escapeTextPseudoHTML(String text) {
        String t = text;
        t = t.replace('+', '\u0301');
        t = t.replace("&", "&amp;");
        t = t.replace("<", "&lt;");
        t = t.replace(">", "&gt;");
        t = t.replace(" -- ", " &mdash; ");
        t = t.replace("〈", "<");
        t = t.replace("〉", ">");
        return t;
    }

    static class Part implements IHtmlPart {
        private final String type, orig, out;
        private final boolean required;

        public Part(String type, String orig, String out, boolean required) {
            this.type = type;
            if (orig == null) {
                throw new NullPointerException();
            }
            this.orig = orig;
            this.out = out;
            this.required = required;
        }

        @Override
        public void out(StringBuilder o) {
            o.append(out);
        }

        @Override
        public boolean isEmpty() {
            return !required;
        }

        @Override
        public boolean notEmpty() {
            return !isEmpty();
        }

        @Override
        public void dump(String prefix) {
            System.err.println(prefix + "== " + type + ": " + orig);
        }
    }
}
