

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.Collator;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.SimpleScriptContext;
import javax.xml.bind.JAXBContext;

import org.w3c.dom.Document;

import com.google.gson.Gson;

import prepare.html.HtmlOut;
import prepare.html.JsDomWrapper;
import prepare.ui.Normalization;
import xsd.Root;

/**
 * Чытае файлы xml і стварае выніковыя файлы.
 */
public class PrepareUI {
	private static final ScriptEngineManager FACTORY = new ScriptEngineManager();
	private static final ScriptEngine engine = FACTORY.getEngineByName("JavaScript");
	private static String script;

	static String template, templateArt, intros = "";

	static final Pattern RE_BOLD_HEADER = Pattern.compile("<b>(.+?)</b>");
	static final Pattern RE_LINK = Pattern.compile("\\{(.+?)(,.+?)?\\}");
	static final List<ArticleInfo> articles = Collections.synchronizedList(new ArrayList<>());
	static final Collator BE = Collator.getInstance(new Locale("be"));
	static final Map<String, String> replaces = new TreeMap<>();
	static final Set<String> errors = new TreeSet<>();

	public static void main(String[] args) throws Exception {
		JAXBContext JAXB = JAXBContext.newInstance(Root.class);
		template = new String(Files.readAllBytes(Paths.get("templates/index-template.html")), StandardCharsets.UTF_8);
		templateArt = new String(Files.readAllBytes(Paths.get("templates/art-template.html")), StandardCharsets.UTF_8);
		script = new String(Files.readAllBytes(Paths.get("data/output.js")), "UTF-8");

		for (Path f : Files.list(Paths.get("web-skeleton/")).collect(Collectors.toList())) {
			Files.copy(f, Paths.get("web/").resolve(f.getFileName()), StandardCopyOption.REPLACE_EXISTING);
		}
		Files.list(Paths.get("templates/")).filter(p -> p.getFileName().toString().matches("art[0-9]\\.html")).sorted()
				.forEach(p -> art(p));

		Files.list(Paths.get("data/")).filter(p -> p.toString().endsWith(".xml")).parallel().forEach(p -> {
			try {
				byte[] xml = Files.readAllBytes(p);
				Root root = (Root) JAXB.createUnmarshaller().unmarshal(new ByteArrayInputStream(xml));

				ArticleInfo a = new ArticleInfo();
				a.id = Integer.parseInt(p.getFileName().toString().replaceAll("^([0-9]+)\\.xml$", "$1"));
				a.newHeader = root.getSuc();
				a.newSynonyms = root.getSynonyms();
				a.html = html(xml);

				Matcher m = RE_BOLD_HEADER.matcher(a.html);
				List<String> headerWords = new ArrayList<>();
				while (m.find()) {
					headerWords.add(m.group(1).trim());
				}
				if (headerWords.isEmpty()) {
					throw new Exception(a.html);
				}
				a.header = String.join(", ", headerWords);
				articles.add(a);
			} catch (Exception ex) {
				throw new RuntimeException(ex);
			}
		});

		Collections.sort(articles, new Comparator<ArticleInfo>() {
			@Override
			public int compare(ArticleInfo a1, ArticleInfo a2) {
				int c = besort.compare(a1.header, a2.header);
				if (c == 0) {
					c = Integer.compare(a1.id, a2.id);
				}
				return c;
			}
		});
		for (int i = 0; i < articles.size(); i++) {
			articles.get(i).idx = i;
		}

		StringBuilder articlesHtml = new StringBuilder();
		List<Set<String>> wordsNew = new ArrayList<>();
		List<Set<String>> wordsHeader = new ArrayList<>();
		List<Set<String>> wordsText = new ArrayList<>();

		articles.parallelStream().forEach(a -> {
			String newHtml = setLinks(a.html);
			a.normalizedWordsNew = Normalization.split(a.newHeader + ", " + a.newSynonyms, " new header #" + a.id);
			a.normalizedWordsHeader = Normalization.split(a.header, " old header #" + a.id);
			a.normalizedWordsText = Normalization.split(newHtml.replaceAll("<.+?>", " "), " html #" + a.id);

			a.html = newHtml.replaceAll("^<p>", "<p><b>[" + a.newHeader.toUpperCase() + "]</b> ").replaceAll("</p>$",
					" <!--{ключавыя словы: " + a.newSynonyms + "}--></p>");
		});

		for (ArticleInfo a : articles) {
			articlesHtml.append("<div id='aidx" + a.idx + "' class='hidden'>\n  ").append(a.html)
					.append("</div>\n");
			wordsNew.add(a.normalizedWordsNew);
			wordsHeader.add(a.normalizedWordsHeader);
			wordsText.add(a.normalizedWordsText);
		}

		fillHeaders(a -> a.newHeader, "HEADER_NEW");
		fillHeaders(a -> a.header, "HEADER_ORIG");

		Gson gson = new Gson();
		replaces.put("{{{INTROS}}}", intros);
		replaces.put("{{{ARTICLES}}}", articlesHtml.toString());
		replaces.put("{{{WORDS_NEW}}}", gson.toJson(wordsNew));
		replaces.put("{{{WORDS_HEADER}}}", gson.toJson(wordsHeader));
		replaces.put("{{{WORDS_TEXT}}}", gson.toJson(wordsText));

		String html = template;
		for (Map.Entry<String, String> en : replaces.entrySet()) {
			html = html.replace(en.getKey(), en.getValue());
		}
		html = Normalizer.normalize(html.replaceAll("(.)´", "<u>$1</u>"), Normalizer.Form.NFC);
		Files.write(Paths.get("web/index.html"), html.getBytes(StandardCharsets.UTF_8));

		errors.forEach(System.out::println);
	}

	static String html(byte[] xml) throws Exception {
		HtmlOut out = new HtmlOut();
		SimpleScriptContext context = new SimpleScriptContext();
		context.setAttribute("out", out, ScriptContext.ENGINE_SCOPE);
		context.setAttribute("header", null, ScriptContext.ENGINE_SCOPE);
		Document doc = JsDomWrapper.parseDoc(xml);
		context.setAttribute("articleDoc", doc, ScriptContext.ENGINE_SCOPE);
		context.setAttribute("article", new JsDomWrapper(doc.getDocumentElement()), ScriptContext.ENGINE_SCOPE);
		engine.eval(script, context);
		out.normalize();
		return out.toString();
	}

	static void art(Path p) {
		try {
			List<String> lines = Files.readAllLines(p);
			String title = lines.get(0);
			intros += "<p><a href='" + p.getFileName() + "'>" + lines.get(0) + "</a></p>\n";
            if (p.getFileName().toString().equals("art4.html")) {
                intros += "<hr/>\n";
            }
			lines.set(0, "<h3>" + lines.get(0) + "</h3>");
			String html = "";
			for (String s : lines) {
				if (s.startsWith("<")) {
					html += s + "\n";
				} else {
					html += "<p>" + s + "</p>\n";
				}
			}
			String o = templateArt.replace("{{{TITLE}}}", title).replace("{{{TEXT}}}", html);
			o = Normalizer.normalize(o.replaceAll("(.)´", "<u>$1</u>"), Normalizer.Form.NFC);
			Files.write(Paths.get("web/" + p.getFileName().toString()), o.getBytes(StandardCharsets.UTF_8));
		} catch (Exception ex) {
			throw new RuntimeException(ex);
		}
	}

	static String splitUI(List<String> list) {
		StringBuilder o = new StringBuilder();
		for (String s : list) {
			o.append("<div class='col-md-4'>").append(s).append("</div>");
		}
		return o.toString();
	}

	static String setLinks(String html) {
		Matcher m = RE_LINK.matcher(html);
		while (m.find()) {
			String linkedWord = m.group(1);
			String afterWord = m.group(2);
			if (afterWord == null)
				afterWord = "";
			String linkedIndexesArray = getArticleIdx(linkedWord);
			html = html.replace("{" + linkedWord + afterWord + "}", "<a href='javascript:' onclick='showPopover(this,"
					+ linkedIndexesArray + ");'>" + linkedWord + afterWord + "</a>");
		}
		return html;
	}

	static String getArticleIdx(String word) {
		List<ArticleInfo> r = articles.stream().filter(a -> a.header.equalsIgnoreCase(word))
				.collect(Collectors.toList());
		String wordNorm = Normalization.normalize(word);
		return articles.stream().filter(a -> Normalization.normalize(a.header).equals(wordNorm))
				.map(a -> Integer.toString(a.idx)).collect(Collectors.joining(",", "[", "]"));
	}

	static void fillHeaders(Function<ArticleInfo, String> headerMapper, String prefix) {
		Stream<Character> chars = articles.stream().map(headerMapper).map(h -> Character.toUpperCase(h.charAt(0)))
				.distinct();
		String header = articles.stream().map(headerMapper).map(charFirst).distinct()
				.map(ch -> "<a href='javascript:firstLetter(" + prefix + ",\"" + ch + "\");'>" + ch + "</a> ")
				.sorted(besort).collect(Collectors.joining());
		String headerShow = articles.stream().map(headerMapper).map(charFirst).distinct()
				.map(ch -> "<a href='javascript:showLetter(" + prefix + ",\"" + ch + "\");'>" + ch + "</a> ")
				.sorted(besort).collect(Collectors.joining());
		String headerReverse = articles.stream().map(headerMapper).map(charLast).distinct()
				.map(ch -> "<a href='javascript:lastLetter(" + prefix + "_REVERSE,\"" + ch + "\");'>" + ch + "</a> ")
				.sorted(besort).collect(Collectors.joining());

		List<String> sortedWords = articles.stream().map(a -> headerMapper.apply(a) + "#" + a.idx)
				.sorted(besort).collect(Collectors.toList());
		List<String> sortedWordsReverse = articles.stream().map(a -> headerMapper.apply(a) + "#" + a.idx)
				.sorted(besortreverse).collect(Collectors.toList());

		Map<Character, List<ArticleInfo>> first = new HashMap<>();
		chars.forEach(c -> first.put(c, new ArrayList<>()));
		for (ArticleInfo a : articles) {
			char c = Character.toUpperCase(headerMapper.apply(a).charAt(0));
			first.get(c).add(a);
		}
		StringBuilder words = new StringBuilder();
		for (char c : first.keySet()) {
			List<String> letterWords = first.get(c).stream()
					.map(a -> "<a href='javascript:showPopup(" + a.idx + ")'>" + a.newHeader + "</a>").sorted(BE)
					.collect(Collectors.toList());
			words.append("<div id='" + prefix + "-" + c + "' class='row'>").append(splitUI(letterWords))
					.append("</div>\n");
		}
		replaces.put("{{{" + prefix + "_HEADERS}}}", header);
		replaces.put("{{{" + prefix + "_HEADERS_SHOW}}}", headerShow);
		replaces.put("{{{" + prefix + "_HEADERS_REVERSE}}}", headerReverse);
		replaces.put("{{{" + prefix + "_WORDS}}}", words.toString());
		Gson gson = new Gson();
		replaces.put("{{{" + prefix + "}}}", gson.toJson(sortedWords));
		replaces.put("{{{" + prefix + "_REVERSE}}}", gson.toJson(sortedWordsReverse));
	}

	static Function<String, Character> charFirst = s -> {
		for (int i = 0; i < s.length(); i++) {
			char c = Character.toUpperCase(s.charAt(i));
			if (c == 'Ґ') {
				return 'Г';
			} else if (Character.isLetter(c)) {
				return c;
			}
		}
		return 0;
	};
	static Function<String, Character> charLast = s -> {
		for (int i = s.length() - 1; i >= 0; i--) {
			char c = Character.toUpperCase(s.charAt(i));
			if (c == 'Ґ') {
				return 'Г';
			} else if (Character.isLetter(c)) {
				return c;
			}
		}
		return 0;
	};

    static final String ORDER = "\0абвгдеёжзиійклмнопрстуўфхцчшщъыьѣэюяѳѵ";

    static Comparator<String> besort = new Comparator<String>() {
        @Override
        public int compare(String s1, String s2) {
            s1 = s1.toLowerCase();
            s2 = s2.toLowerCase();
            int i1 = 0, i2 = 0;
            while (true) {
                int p1, p2;
                do {
                    if (i1 >= s1.length()) {
                        p1 = 0;
                        break;
                    }
                    char c = s1.charAt(i1);
                    if (c == 'ґ') {
                        c = 'г';
                    }
                    p1 = ORDER.indexOf(c);
                    i1++;
                } while (p1 < 0);
                do {
                    if (i2 >= s2.length()) {
                        p2 = 0;
                        break;
                    }
                    char c = s2.charAt(i2);
                    if (c == 'ґ') {
                        c = 'г';
                    }
                    p2 = ORDER.indexOf(c);
                    i2++;
                } while (p2 < 0);
                if (p1 < p2) {
                    return -1;
                } else if (p1 > p2) {
                    return 1;
                } else if (p1 == 0 && p2 == 0) {
                    return 0;
                }
            }
        }
    };
    static Comparator<String> besortreverse = new Comparator<String>() {
        @Override
        public int compare(String o1, String o2) {
            StringBuilder s1 = new StringBuilder(o1.toLowerCase());
            StringBuilder s2 = new StringBuilder(o2.toLowerCase());
            s1.setLength(o1.indexOf('#'));
            s2.setLength(o2.indexOf('#'));
            s1.reverse();
            s2.reverse();
            return besort.compare(s1.toString(), s2.toString());
        }
    };

	public static class ArticleInfo {
		int id;
		int idx;
		String html;
		String header;
		String newHeader;
		String newSynonyms;
		// асобныя словы(малымі літарамі без націскаў): асучасненыя і сінонімы
		Set<String> normalizedWordsNew = new TreeSet<>();
		// асобныя словы(малымі літарамі без націскаў і без гачыкаў): загаловак з
		// Насовіча
		Set<String> normalizedWordsHeader = new TreeSet<>();
		// асобныя словы(малымі літарамі без націскаў і без гачыкаў): тэкст з Насовіча
		Set<String> normalizedWordsText = new TreeSet<>();
	}
}
