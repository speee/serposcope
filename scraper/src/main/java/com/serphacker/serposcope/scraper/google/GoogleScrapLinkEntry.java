package com.serphacker.serposcope.scraper.google;

public class GoogleScrapLinkEntry {

    public final static byte SERIAL_VERSION = 1;

	private String url;

	private String title;
	
	private String nonAmpUrl;
	
	private Integer featuredRank;

	public GoogleScrapLinkEntry() {		
	}

	public GoogleScrapLinkEntry(String url) {
		this.url = url;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getNonAmpUrl() {
		return nonAmpUrl;
	}

	public void setNonAmpUrl(String nonAmpUrl) {
		this.nonAmpUrl = nonAmpUrl;
	}

	public Integer getFeaturedRank() {
		return featuredRank;
	}

	public void setFeaturedRank(Integer featuredRank) {
		this.featuredRank = featuredRank;
	}
	
}
