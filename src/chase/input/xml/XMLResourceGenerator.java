package chase.input.xml;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class XMLResourceGenerator {
	
	protected static String ATLAS_ROOT = "http://www.genboree.org/EdaccData/Release-5/";
	protected static String ENCODE_ROOT = "http://hgdownload.cse.ucsc.edu/goldenPath/hg19/encodeDCC/";
	protected static String REGIONS_ROOT = "http://www.bcgsc.ca/downloads/cydney-test/EdaccRegions/";
	
	// NOTE: ATLAS_PROCESSED_ROOT must match ATLAS_ROOT in structure
	protected static String ATLAS_PROCESSED_ROOT = "http://www.bcgsc.ca/downloads/cydney-test/EdaccDataProcessed/Release-5/";

	public void generateXMLResource(String oFileName) throws IOException {
		try {
			FileOutputStream oFileStream = new FileOutputStream(oFileName);
			String output;
			// write the header
			oFileStream.write(("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + 
					"<Files>\n" + 
					"<URLRoot>\n" + 
					"<AtlasURL>" + ATLAS_ROOT + "</AtlasURL>\n" +
					"<EncodeURL>" + ENCODE_ROOT + "</EncodeURL>\n" +
					"<RegionsURL>" + REGIONS_ROOT + "</RegionsURL>\n" + 
					"<AtlasProcessedURL>" + ATLAS_PROCESSED_ROOT + "</AtlasProcessedURL>\n" +
					"</URLRoot>\n").getBytes());

			// get epigenome atlas urls
			UrlCrawler g = new UrlCrawler();
			g.processFileHierarchy(ATLAS_ROOT);
			List<String> atlasUrls = g.getUrls();
			// write to output file
			for (String url: atlasUrls) {
				// remove the common url root
				url = url.split(ATLAS_ROOT)[1];
				if (url.endsWith(".wig.gz")) {
					output = "<Atlas type=\"wig\">\n  <URL>" + url + "</URL>\n</Atlas>";
				} else {
					throw new IOException("Unknown file extension: " + url);
				}
				oFileStream.write(output.getBytes());
			}
			
			// get ENCODE urls
			g = new UrlCrawler();
			g.processFileHierarchy(ENCODE_ROOT);
			List<String> encodeUrls = g.getUrls();
			// write to output file
			for (String url: encodeUrls) {
				// remove the common url root
				url = url.split(ENCODE_ROOT)[1];
				if (url.endsWith(".bigWig")) {
					output = "<Encode type=\"bigWig\">\n  <URL>" + url + "</URL>\n</Encode>";
				} else if (url.endsWith(".wig") || url.endsWith(".wig.gz")) {
					output = "<Encode type=\"wig\">\n  <URL>" + url + "</URL>\n</Encode>";
				} else {
					throw new IOException("Unknown file extension: " + url);
				}
				oFileStream.write(output.getBytes());
			}

			// manually add region file urls
			// bcgsc.ca downloads index page is too difficult to parse mostly because of 'Parent Directory' link
			String[] regionFiles = {"hg18/TSS/tss_hg18_+-3000.gff",
					"hg18/TSS/tss_hg18_+-3000_noNeighbors.gff",
					"hg18/genes/refseqTranscripts_hg18.gff",
					"hg18/CpGs/CpG_Islands_hg18.gff",
					"hg19/TSS/tss_hg19_+-3000.gff", 
					"hg19/TSS/tss_hg19_+-3000_noNeighbors.gff",
					"hg19/genes/refseqTranscripts_hg19.gff",
					"hg19/CpGs/CpG_Islands_hg19.gff"};
			oFileStream.write("\n".getBytes());
			// write to output file
			for (String rFile: regionFiles) {
				output = "<Regions type=\"gff\">\n  <URL>" + rFile + "</URL>\n</Regions>";
				oFileStream.write(output.getBytes());
			}
			
			// write the footer
			oFileStream.write(("</Files>\n").getBytes());

			oFileStream.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void main(final String[] args) {
		if (args.length == 1) {
			String oFileName = args[0];
			XMLResourceGenerator g = new XMLResourceGenerator();
			try {
				g.generateXMLResource(oFileName);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
}
