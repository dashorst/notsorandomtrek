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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.select.Elements;

public class RandomTrekScraper {
	private static ExecutorService executor = Executors.newSingleThreadExecutor();

	private static Map<String, String> memoryAlphaMappings = new HashMap<>();

	public enum Show {
		specials, tos, voy, tng, ds9, ent;

		public static Show of(String title) {
			title = title.toLowerCase();
			if (title.contains("(voy)"))
				return voy;
			if (title.contains("(tng)") || title.contains("darmok"))
				return tng;
			if (title.contains("(ds9)") || title.contains("equilibrium"))
				return ds9;
			if (title.contains("(tos)") || title.contains("the squire of gothos")
					|| title.contains("the devil in the dark"))
				return tos;
			if (title.contains("(ent)") || title.contains("the andorian incident"))
				return ent;
			return specials;
		}
	}

	static {
		memoryAlphaMappings.put("https://memory-alpha.wikia.com/wiki/The_Measure_of_a_Man_(episode)",
				"https://memory-alpha.wikia.com/wiki/The_Measure_Of_A_Man_(episode)");

		memoryAlphaMappings.put("https://memory-alpha.wikia.com/wiki/Judgement_(episode)",
				"https://memory-alpha.wikia.com/wiki/Judgement");

		memoryAlphaMappings.put("https://memory-alpha.wikia.com/wiki/Workforce_Part_I_&_II_(episode)",
				"https://memory-alpha.wikia.com/wiki/Workforce_(episode)");

		memoryAlphaMappings.put("https://memory-alpha.wikia.com/wiki/The_Search_Part_1_and_2_(episode)",
				"https://memory-alpha.wikia.com/wiki/The_Search,_Part_I_(episode)");

		memoryAlphaMappings.put("https://memory-alpha.wikia.com/wiki/Vis_A_Vis_(episode)",
				"https://memory-alpha.wikia.com/wiki/Vis_à_Vis_(episode)");

		memoryAlphaMappings.put("https://memory-alpha.wikia.com/wiki/Time's_Arrow_Part_1_and_2_(episode)",
				"https://memory-alpha.wikia.com/wiki/Time%27s_Arrow_(episode)");

		memoryAlphaMappings.put("https://memory-alpha.wikia.com/wiki/Homefront\"_and_\"Paradise_Lost_(episode)",
				"https://memory-alpha.wikia.com/wiki/Homefront");
	}

	private static int specialsCount = 0;

	private static Future<Map<Integer, String>> overcastEpisodes;

	public static void main(String[] args) throws Exception {
		try {
			overcastEpisodes = getOvercastEpisodes();

			if (args.length == 1) {
				String par1 = args[0];
				if (par1.matches("\\d+"))
					scrapeShow(Integer.valueOf(args[0]));
			} else {
				int max = getMaxEpisodeNumber();
				for (int nr = 1; nr <= max; nr++) {
					scrapeShow(nr);
				}
			}
		} finally {
			executor.shutdown();
		}
	}

	private static int getMaxEpisodeNumber() throws MalformedURLException, IOException, URISyntaxException {
		Document randomtrekHome = Jsoup.parse(new URI("https://www.theincomparable.com/randomtrek/").toURL(), 2000);

		return randomtrekHome.getElementsByAttributeValue("class", "episode-number").stream().map(e -> e.text())
				.map(Integer::valueOf).max((e1, e2) -> e1 - e2).orElse(142);
	}

	private static void scrapeShow(int nr) throws Exception {
		Show show = getShow(nr);

		Optional<String> alpha = Optional.empty();

		int season = 0;
		int episodeNumber = 0;

		if (show != Show.specials) {
			alpha = Optional.of(getEpisodeMemoryAlphaLink(nr));
			season = getEpisodeSeason(nr, alpha.get());
			episodeNumber = getEpisodeNumber(nr, alpha.get());
		} else {
			episodeNumber = ++specialsCount;
		}

		System.out.println("- id: " + String.format("%s-s%02de%02d", show, season, episodeNumber));
		System.out.println("  show: " + show);
		System.out.println("  season: " + season);
		System.out.println("  episode: " + episodeNumber);
		System.out.println("  code: " + String.format("S%02dE%02d", season, episodeNumber));
		System.out.println("  randomtrek_id: " + nr);

		if (alpha.isPresent()) {
			System.out.println("  title: \"" + getEpisodeTitle(nr, alpha.get()) + "\"");
			System.out.println("  description: >\n    \"" + getEpisodeDescription(nr, alpha.get()) + "\"");
			System.out.println("  memoryalpha: " + alpha.get());
		} else {
			System.out.println("  title: \"" + getEpisodeRandomTrekTitle(nr) + "\"");
			System.out.println("  description: >\n    \"" + getEpisodeRandomTrekDescription(nr) + "\"");
		}
		System.out.println("  itunes: https://itunes.apple.com/us/podcast/random-trek/id883813961");
		System.out.println("  pocketcasts: http://pca.st/randomtrek");
		System.out.println("  overcast: https://overcast.fm/itunes883813961/random-trek");
		System.out.println("  overcast_episode: " + overcastEpisodes.get(2, TimeUnit.SECONDS).get(nr));

		getVideos(nr);

		System.out.println("  randomtrek:");
		System.out.println("    randomtrek_id: " + nr);
		System.out.println("    title: \"" + getEpisodeRandomTrekTitle(nr) + "\"");
		System.out.println("    date: " + getEpisodeRandomTrekDate(nr));
		System.out.println("    duration: " + getEpisodeRandomTrekDuration(nr));
		System.out.println("    size: " + getEpisodeRandomTrekSize(nr));
		System.out.println("    description: >\n      \"" + getEpisodeRandomTrekDescription(nr) + "\"");
		System.out.println("    hosts:");
		getHosts(nr);
	}

	private static Future<Map<Integer, String>> getOvercastEpisodes() throws IOException, URISyntaxException {
		return executor.submit(() -> {
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
		});
	}

	private static Show getShow(int nr) throws Exception {
		String title = getEpisodeRandomTrekTitle(nr);
		return Show.of(title);
	}

	enum VideoServiceMatch {
		URI, text;
	}

	static class VideoService {
		private VideoServiceMatch matchMode;
		private String matchCode;
		private String name;
		private String url;

		public VideoService(VideoServiceMatch matchMode, String matchCode, String name) {
			this.matchMode = matchMode;
			this.matchCode = matchCode;
			this.name = name;
		}
	}

	public static void getVideos(int nr) throws Exception {
		System.out.println("  videos:");

		List<VideoService> services = new ArrayList<>();
		services.add(new VideoService(VideoServiceMatch.URI, "hulu.com", "Hulu"));
		services.add(new VideoService(VideoServiceMatch.URI, "netflix.com", "Netflix"));
		services.add(new VideoService(VideoServiceMatch.URI, "cbs.com", "CBS"));
		services.add(new VideoService(VideoServiceMatch.text, "amazon", "Amazon"));

		services.forEach(service -> {
			Optional<String> serviceUrl;
			try {
				serviceUrl = getVideoServiceUrl(nr, service);

				serviceUrl.ifPresent(url -> {
					System.out.println("  - service: " + service.name);
					System.out.println("    url: " + url);
				});
			} catch (Exception e) {
			}
		});
	}

	public static Optional<String> getVideoServiceUrl(int id, VideoService service) throws Exception {
		Document document = getRandomtrekEpisodeDocument(id);
		Element linksDiv = document.getElementById("links");

		Optional<Element> affiliateLink;
		if (service.matchMode == VideoServiceMatch.URI) {
			Elements links = linksDiv.getElementsByAttributeValueContaining("href", service.matchCode);
			affiliateLink = links.stream().findFirst();
		} else {
			Elements links = linksDiv.getElementsByTag("a");
			affiliateLink = links.stream().filter(e -> e.text().toLowerCase().trim().contains(service.matchCode))
					.findFirst();
		}
		return affiliateLink.map(e -> e.attr("href").trim());
	}

	public static String getEpisodeRandomTrekTitle(int id) throws Exception {
		Document document = getRandomtrekEpisodeDocument(id);
		return document.getElementsByAttributeValue("property", "og:title").first().attr("content").trim().replace("\"",
				"\\\"");
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
		String text = durationNode.text();
		return getDurationInMinutes(text);
	}

	public static int getDurationInMinutes(String text) {
		Pattern hourMinutesPattern = Pattern.compile("(\\d+) hours?, (\\d+) minutes?");
		Matcher hourMinutesMatcher = hourMinutesPattern.matcher(text);
		if (hourMinutesMatcher.find()) {
			return Integer.valueOf(hourMinutesMatcher.group(1)) * 60 + Integer.valueOf(hourMinutesMatcher.group(2));
		}
		Pattern minutesPattern = Pattern.compile("(\\d+) minutes?");
		Matcher minutesMatcher = minutesPattern.matcher(text);
		if (minutesMatcher.find()) {
			return Integer.valueOf(minutesMatcher.group(1));
		}
		return Integer.valueOf(text.replace("minutes", "").trim());
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

		description1 = description1.replace("\n", " ").trim();
		description2 = description2.replace("\n", " ").trim();

		return description1 + (!description2.isEmpty() ? "\n      " : "") + description2;
	}

	public static String getEpisodeMemoryAlphaLink(int id) throws Exception {
		String url = "";

		String title = getEpisodeRandomTrekTitle(id);
		Pattern pattern = Pattern.compile("\"(.*)\".*");
		Matcher matcher = pattern.matcher(title);
		if (matcher.find()) {
			title = matcher.group(1);
			title = title.replaceAll(" ", "_");
			url = "https://memory-alpha.wikia.com/wiki/" + title + "_(episode)";
		} else {
			Pattern pattern2 = Pattern.compile("([^\\(]+) \\(.*");
			Matcher matcher2 = pattern2.matcher(title);
			if (matcher2.find()) {
				title = matcher2.group(1);
				title = title.replaceAll(" ", "_");
				url = "https://memory-alpha.wikia.com/wiki/" + title + "_(episode)";
			}
		}
		url = memoryAlphaMappings.getOrDefault(url, url).replace("\\", "");
		return url;
	}

	public static void getHosts(int id) throws Exception {
		Document document = getRandomtrekEpisodeDocument(id);
		String prefix = "https://www.theincomparable.com/person/";
		document.getElementsByAttributeValueStarting("href", prefix).stream().forEach(e -> {
			if (e.attr("href").trim().endsWith("php"))
				return;
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
		return node.attr("content").replace(" (episode)", "").replace("\"", "\\\"");
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
		String numbers = episodeIdentifier.substring(2);
		if (numbers.contains("/"))
			numbers = numbers.substring(0, numbers.indexOf("/"));
		return Integer.valueOf(numbers);
	}

	public static String getEpisodeDescription(int id, String url) throws Exception {
		Document document = getMemoryAlphaEpisodeDocument(id, url);

		return document.getElementById("WikiaArticle").getElementsByTag("p").first().text().trim();
//		Element node = document.getElementsByAttributeValue("property", "og:description").last();
//		return node.attr("content").trim();
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
