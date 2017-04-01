# The Not So Random Trek Web Site

A companion website that derandomizes the [Random Trek podcast](https://www.theincomparable.com/randomtrek) episodes.

This website is static and built using [Jekyll](https://jekyllrb.com).

The episode data lives in the folder `_data/` and is extracted from `_data/ran.yml`.

The main page is built in `/index.html`.

### Running jekyll

You can start the site using normal Jekyll:

```
jekyll serve -w
```

The web site can be found at localhost:4000/notsorandomtrek/

### Scraper for Episode information

In the subfolder \_scraper is a Java project that retrieves the episode
information from the Random Trek, Memory Alpha and Overcast.fm websites.

As it is a Java project, it is built using Apache Maven. It uses JSoup
for parsing the HTML from the scraped web sites. The output is the YAML
necessary to add to the `ran.yml` file.

Building is (once you have a Java 8 Development Kit and Apache Maven
installed) as simple as running `mvn package` inside the \_scraper
folder:

```
notsorandomtrek$ cd _scraper
_scraper$ mvn package
```

Running the scraper:

```
_scraper$ java -jar target/randomtrek-scraper-0.1.jar 142`
```

The _142_ is the episode number you wish to retrieve.

You need to separately retrieve the episode image.

## TODO

This is still a work in progress (and will remain so I think until
Random Trek has recorded its final episode).

Here's a list of todo's:

- [x] Add ~Deep Space 9~, ~Voyager~, ~Enterprise~ and ~special~ episodes
- [x] Add direct links to Random Trek episodes to overcast.fm's website for direct playing
- [ ] Add links to imdb for each episode
- [x] Fix the UI such that the tabs work correctly
- [x] Style the UI to look much better
- [x] Style the UI to work on a small screen
- [ ] Make a [Bootstrap](https://getbootstrap.com) theme based on LCars
