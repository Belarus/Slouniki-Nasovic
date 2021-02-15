package prepare.html;

public interface IHtmlPart {
    void out(StringBuilder out);

    boolean isEmpty();

    boolean notEmpty();

    void dump(String prefix);
}
