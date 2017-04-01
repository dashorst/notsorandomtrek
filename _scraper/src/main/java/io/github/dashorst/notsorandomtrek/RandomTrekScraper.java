package io.github.dashorst.notsorandomtrek;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.HashMap;
import java.util.Map;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

public class RandomTrekScraper {
	public static void main(String[] args) throws Exception {
		if (args.length > 0) {
			scrapeShow(Integer.valueOf(args[0]));
		} else {
            System.err.println("Usage: java -jar randomtrekscraper <episode number>");
            System.exit(1);
		}
	}

	private static void scrapeShow(int nr) throws Exception {
		Map<Integer, String> overcastEpisodes = getOvercastEpisodes();

		String alpha = getEpisodeMemoryAlphaLink(nr);
		String show = getShow(nr);
		int season = 0;
		int episodeNumber = 0;
		if (!(alpha == null || alpha.isEmpty())) {
			season = getEpisodeSeason(nr, alpha);
			episodeNumber = getEpisodeNumber(nr, alpha);
		}

		System.out.println("- id: " + String.format("%s-s%02de%02d", show, season, episodeNumber));
		System.out.println("  show: " + show);
		System.out.println("  season: " + season);
		System.out.println("  episode: " + episodeNumber);
		System.out.println("  code: " + String.format("S%02dE%02d", season, episodeNumber));
		System.out.println("  randomtrek_id: " + nr);
		System.out.println("  title: \"" + getEpisodeTitle(nr, alpha) + "\"");
		System.out.println("  description: >\n    \"" + getEpisodeDescription(nr, alpha) + "\"");
		System.out.println("  memoryalpha: " + alpha);
		System.out.println("  itunes: https://itunes.apple.com/us/podcast/random-trek/id883813961");
		System.out.println("  pocketcasts: http://pca.st/randomtrek");
		System.out.println("  overcast: https://overcast.fm/itunes883813961/random-trek");
		System.out.println("  overcast_episode: " + overcastEpisodes.get(nr));
		System.out.println("  randomtrek:");
		System.out.println("    randomtrek_id: " + nr);
		// System.out.println(" title: " + getEpisodeRandomTrekTitle(nr));
		System.out.println("    date: " + getEpisodeRandomTrekDate(nr));
		System.out.println("    duration: " + getEpisodeRandomTrekDuration(nr));
		System.out.println("    size: " + getEpisodeRandomTrekSize(nr));
		System.out.println("    description: >\n      \"" + getEpisodeRandomTrekDescription(nr) + "\"");
		System.out.println("    hosts:");
		getHosts(nr);
	}

	private static Map<Integer, String> getOvercastEpisodes() throws IOException, URISyntaxException {
		Map<Integer, String> overcastEpisodes = new HashMap<>();
		Document document = Jsoup.parse(new URI("https://overcast.fm/itunes883813961/random-trek").toURL(), 2000);

		Elements episodeCells = document.getElementsByClass("extendedepisodecell");

		episodeCells.forEach(e -> {
			Elements titles = e.getElementsByClass("title");
			Element title = titles.first();
			int nr = Integer.valueOf(title.text().split(":")[0]);
			String url = "https://overcast.fm" + e.attr("href");
			overcastEpisodes.put(nr, url);
		});
		return overcastEpisodes;
	}

	private static String getShow(int nr) throws Exception {
		String show = "specials";
		if (getEpisodeRandomTrekTitle(nr).contains("(VOY)"))
			show = "voy";
		if (getEpisodeRandomTrekTitle(nr).contains("(TNG)"))
			show = "tng";
		if (getEpisodeRandomTrekTitle(nr).contains("(DS9)"))
			show = "ds9";
		if (getEpisodeRandomTrekTitle(nr).contains("(TOS)"))
			show = "tos";
		if (getEpisodeRandomTrekTitle(nr).contains("(ENT)"))
			show = "ent";
		return show;
	}

	public static String getEpisodeRandomTrekTitle(int id) throws Exception {
		Document document = getRandomtrekEpisodeDocument(id);
		return document.getElementsByAttributeValue("property", "og:title").first().attr("content").trim();
	}

	public static String getEpisodeRandomTrekDate(int id) throws Exception {
		Document document = getRandomtrekEpisodeDocument(id);

		Node postdate = document.getElementsByAttributeValue("class", "postdate").first().childNode(0);
		String date = postdate.toString().replace("•", "").trim();
		date = LocalDate.parse(date, DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG))
				.format(DateTimeFormatter.ISO_DATE);
		return date;
	}

	public static int getEpisodeRandomTrekDuration(int id) throws Exception {
		Document document = getRandomtrekEpisodeDocument(id);

		Element durationNode = (Element) document.getElementsByAttributeValue("class", "postdate").first().childNode(1);
		return Integer.valueOf(durationNode.text().replace("minutes", "").trim());
	}

	public static int getEpisodeRandomTrekSize(int id) throws Exception {
		Document document = getRandomtrekEpisodeDocument(id);

		Node sizeNode = document.getElementsByAttributeValue("class", "download").first().childNode(2);
		String node = sizeNode.toString();
		return Integer.valueOf(node.substring(0, node.indexOf("M")).trim().substring(1));
	}

	public static String getEpisodeRandomTrekDescription(int id) throws Exception {
		Document document = getRandomtrekEpisodeDocument(id);

		Element entry = document.getElementsByAttributeValue("id", "entry").first();

		Elements paragraphs = entry.getElementsByTag("p");
		String description1 = paragraphs.size() > 1 ? paragraphs.get(1).outerHtml() : "";
		String description2 = paragraphs.size() > 2 ? paragraphs.get(2).outerHtml() : "";
		if (description2.contains("class=\"postdate\""))
			description2 = "";

		description1 = description1.trim().replace("\n", " ");
		description2 = description2.trim().replace("\n", " ");

		return description1 + (!description2.isEmpty() ? "\n    " : "") + description2;
	}

	public static String getEpisodeMemoryAlphaLink(int id) throws Exception {
		Document document = getRandomtrekEpisodeDocument(id);
		return document.getElementsByAttributeValueStarting("href", "http://memory-alpha.wikia.com/wiki").attr("href");
	}

	public static void getHosts(int id) throws Exception {
		Document document = getRandomtrekEpisodeDocument(id);
		String prefix = "https://www.theincomparable.com/person/";
		document.getElementsByAttributeValueStarting("href", prefix).forEach(e -> {
			System.out.println("    - code: " + e.attr("href").trim().substring(prefix.length()).replace("/", ""));
			System.out.println("      name: " + e.text());
		});
	}

	private static Document getRandomtrekEpisodeDocument(int id) throws IOException, MalformedURLException {
		Path path = Paths.get("target", "randomtrek");
		Path directories = Files.createDirectories(path);

		Path rtHtmlFile = directories.resolve("rt-" + id + ".html");
		Document document = null;
		if (!Files.exists(rtHtmlFile)) {
			document = Jsoup.parse(new URL("https://www.theincomparable.com/randomtrek/" + id), 2000);

			try (ByteArrayInputStream bais = new ByteArrayInputStream(
					document.toString().getBytes(Charset.forName("UTF-8")));) {
				Files.copy(bais, rtHtmlFile);
			}
		} else {
			document = Jsoup.parse(rtHtmlFile.toFile(), "UTF-8");
		}
		return document;
	}

	public static int getSeason(int id, String url) throws Exception {
		Document document = getMemoryAlphaEpisodeDocument(id, url);
		System.out.println(document);
		return 1;
	}

	public static String getEpisodeTitle(int id, String url) throws Exception {
		Document document = getMemoryAlphaEpisodeDocument(id, url);
		Element node = document.getElementsByAttributeValue("property", "og:title").last();
		return node.attr("content").replace(" (episode)", "");
	}

	public static int getEpisodeSeason(int id, String url) throws Exception {
		Document document = getMemoryAlphaEpisodeDocument(id, url);

		Element seasonIdentifier = document.getElementsContainingText(", Episode ").last();
		String episodeIdentifier = seasonIdentifier.getElementsByTag("a").last().text();
		return Integer.valueOf(episodeIdentifier.substring(0, 1));
	}

	public static int getEpisodeNumber(int id, String url) throws Exception {
		Document document = getMemoryAlphaEpisodeDocument(id, url);

		Element seasonIdentifier = document.getElementsContainingText(", Episode ").last();
		String episodeIdentifier = seasonIdentifier.getElementsByTag("a").last().text();
		return Integer.valueOf(episodeIdentifier.substring(2));
	}

	public static String getEpisodeDescription(int id, String url) throws Exception {
		Document document = getMemoryAlphaEpisodeDocument(id, url);

		Element node = document.getElementsByAttributeValue("property", "og:description").last();
		return node.attr("content").trim();
	}

	private static Document getMemoryAlphaEpisodeDocument(int id, String url)
			throws IOException, MalformedURLException {
		Path path = Paths.get("target", "randomtrek");
		Path directories = Files.createDirectories(path);

		Path rtHtmlFile = directories.resolve("ma-" + id + ".html");
		Document document = null;
		if (!Files.exists(rtHtmlFile)) {
			document = Jsoup.parse(new URL(url), 2000);

			try (ByteArrayInputStream bais = new ByteArrayInputStream(
					document.toString().getBytes(Charset.forName("UTF-8")));) {
				Files.copy(bais, rtHtmlFile);
			}
		} else {
			document = Jsoup.parse(rtHtmlFile.toFile(), "UTF-8");
		}
		return document;
	}
}
