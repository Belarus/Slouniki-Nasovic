package prepare.html;

public class HtmlOut extends JoinWithSeparators {
    public HtmlOut() {
        super("", "");
    }

    public IHtmlPart prepare() {
        return new JoinWithSeparators("", "");
    }

    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        out(out);
        return out.toString();
    }

    public void log(String msg) {
        System.err.println(msg);
    }

    public void dump() {
        System.err.println("============================");
        dump("");
    }
}
