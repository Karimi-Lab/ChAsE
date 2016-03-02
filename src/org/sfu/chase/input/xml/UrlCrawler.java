package org.sfu.chase.input.xml;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

public class UrlCrawler {
	
	protected List<String> m_Urls;
	
	public UrlCrawler() {
		m_Urls = new ArrayList<String>();
	}
	
	public void clear() {
		m_Urls.clear();
	}
	
	public List<String> getUrls() {
		return m_Urls;
	}
	
	public void processFileHierarchy(String urlString) {
		try {	
			System.out.println(urlString);
			URL url = new URL(urlString);
			InputStream in = url.openStream();
			InputStreamReader reader = new InputStreamReader(in);
			// ParserCallback class to handle the href tags
			LinkHandler callback = new LinkHandler();
			// key step to enable recursion
			callback.setCurrentPath(urlString);
			// parse the HTML document
			new ParserDelegator().parse(reader, callback, false);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public class LinkHandler extends HTMLEditorKit.ParserCallback {
		protected String m_CurrentPath;
		protected String m_CurrentFile;
		// target files all have text before an <HR> tag that should be skipped
		protected boolean pastHeader = false;
		// used to keep track of whether or not parsing a 'parent directory' link
		protected boolean skipLink = false;
		
		public void setCurrentPath(String p) {
			m_CurrentPath = p;
		}
		
		public void handleStartTag(HTML.Tag t, MutableAttributeSet a, int pos) {
			if (t == HTML.Tag.A) {
				skipLink = false;
				m_CurrentFile = (String)a.getAttribute(HTML.Attribute.HREF);
			}
		}
		
		public void handleText(char[] data, int pos) {
			if (pastHeader) {
				// some custom checks to skip over parent directory links, etc.
				String s_Data = new String(data);
				if (s_Data.equals("../") || s_Data.equals("Parent Directory")) {
					skipLink = true;
				}
			}
		}
		
		public void handleEndTag(HTML.Tag t, int pos) {
			// only process the tag once both tag info and text have been parsed
			if (t == HTML.Tag.A) {
				if (!pastHeader) { return; } // ignore any links in the header text
				if (skipLink) { return; }
				if (m_CurrentFile == null) { return; }
				
				if (m_CurrentFile.endsWith("/")) {
					String path = m_CurrentPath + m_CurrentFile;
					// recursively process the directories
					processFileHierarchy(path);
				}
				if (m_CurrentFile.endsWith("wig.gz")) {
					// System.out.println(m_CurrentPath + m_CurrentFile);
					m_Urls.add(m_CurrentPath + m_CurrentFile);
				} else if (m_CurrentFile.endsWith(".wig")) {
					m_Urls.add(m_CurrentPath + m_CurrentFile);
					// System.out.println(m_CurrentPath + m_CurrentFile);
				} else if (m_CurrentFile.endsWith(".bigWig")) {
					m_Urls.add(m_CurrentPath + m_CurrentFile);
					// System.out.println(m_CurrentPath + m_CurrentFile);
				}
			}
		}
		
		public void handleSimpleTag(HTML.Tag t, MutableAttributeSet a, int pos) {
			if (t == HTML.Tag.HR) {
				pastHeader = true;
			}
		}
	}
	
	public static void main(final String[] args) {
		if (args.length == 1) {
			String urlRoot = args[0];
			UrlCrawler g = new UrlCrawler();
			g.processFileHierarchy(urlRoot);
			System.out.println(g.getUrls().size() + " target urls");
		}
	}
	
}
