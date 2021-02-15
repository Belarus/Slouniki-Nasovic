package prepare.html;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class JsDomWrapper implements Map<String, Object> {
    static DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();

    private final Element el;

    public static Document parseDoc(byte[] xml) throws Exception {
        DocumentBuilder builder = factory.newDocumentBuilder();
        return builder.parse(new ByteArrayInputStream(xml));
    }

    public JsDomWrapper(Element el) {
        this.el = el;
    }

    @Override
    public int size() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean isEmpty() {
        return el.getFirstChild() == null && el.getAttributes().getLength() == 0;
    }

    @Override
    public String toString() {
        throw new RuntimeException("Trying to show JsDomWrapper as text");
    }

    @Override
    public boolean containsKey(Object key) {
        if (el.hasAttribute(key.toString())) {
            return true;
        }

        for (Node n = el.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                if (n.getNodeName().equals(key)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean containsValue(Object value) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Object get(Object key) {
        if ("textContent".equals(key)) {
            return el.getTextContent();
        }
        if (el.hasAttribute(key.toString())) {
            return el.getAttribute(key.toString());
        }

        List<JsDomWrapper> r = new ArrayList<>();
        for (Node n = el.getFirstChild(); n != null; n = n.getNextSibling()) {
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                if (n.getNodeName().equals(key)) {
                    r.add(new JsDomWrapper((Element) n));
                }
            }
        }
        return r.isEmpty() ? null : r;
    }

    @Override
    public Object put(String key, Object value) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Object remove(Object key) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void putAll(Map<? extends String, ? extends Object> m) {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public void clear() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Set<String> keySet() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Collection<Object> values() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public Set<Map.Entry<String, Object>> entrySet() {
        throw new RuntimeException("Not implemented");
    }
}
