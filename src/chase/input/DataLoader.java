/**
 * Imported from spark.dataloader.DataLoader
 * Date: May 29, 2012
 */

package chase.input;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

import collab.BioReader;

/**
 * Contains the logic regarding which files are currently accepted, etc.
 * 
 * @author Cydney Nielsen
 *
 */
public class DataLoader {
	
	private static Logger log = Logger.getLogger(DataLoader.class);
	
	protected static String[] BWIG = {".bigwig", ".bw"}; 
	protected static String[] WIG = {".wig", ".wig.gz", ".wig.zip"};
	protected static String[] GFF = {".gff", ".bed"};
	
	public static void checkDataFileExt(String f) throws IllegalArgumentException {
		boolean accept = false;
		if (isBigWig(f)) {
			accept = true;
		} else if (isWig(f)) {
			accept = true;
		}
		if (!accept) {
			log.error("Unknown file extension for data file: '" + f + "'");
			throw new IllegalArgumentException("Unknown file extension for file:\n" + 
					"   '" + f + "'\n" + 
					"See the User Guide for currently supported formats.");
		}
	}
	
	protected static boolean checkExt(String f, String[] exts) {
		f = f.toLowerCase();
		for (String ext: exts) {
			if (f.endsWith(ext.toLowerCase())) {
				return true;
			}
		}
		return false;
	}
	
	public static void checkRegionsFileExt(String f) throws IllegalArgumentException {
		boolean accept = false;
		if (isGFF(f)) {
			accept = true;
		}
		if (!accept) {
			log.error("Unexpected file extension for regions file: '" + f + "'. Must be '.gff'.");
			throw new IllegalArgumentException("Unexpected file extension for regions file:\n" + 
					"   '" + f + "'\n" + 
					"Must be '.gff'.");
		}
	}
	
	public static boolean isBigWig(String f) {
		return checkExt(f, BWIG);
	}
	
	public static boolean isGFF(String f) {
		return checkExt(f, GFF);
	}
	
	public static boolean isWig(String f) {
		return checkExt(f, WIG);
	}

	public static void parseDataFile(String f) throws IOException, IllegalArgumentException {
		checkDataFileExt(f);
		// TODO: fill in parse
	}
	
	public static String parseSampleName(String f) throws IOException, IllegalArgumentException, InterruptedException {
		if (isBigWig(f)) {
			return removeExt(f, BWIG);
		} else if (isWig(f)) {
			BioReader br = new BioReader();
			// TODO: headerOnly should be only necessary parameter here
			int tempNumBins = 10;
			boolean includeZeros = true;
			boolean headerOnly = true;
			br.readWIG(f, tempNumBins, includeZeros, headerOnly);
			// TODO: perhaps this default sample name behavior should be inside BioReader
			String sName = br.getSampleName();
			if (sName == null) {
				return removeExt(f, WIG);
			} else {
				return br.getSampleName();
			}
		} else {
			log.error("Unknown file extension for file: '" + f + "'");
			throw new IllegalArgumentException("Unknown file extension for file:\n" + 
					"   '" + f + "'\n" + 
					"See the User Guide for currently supported formats.");
		}
	}
	
	protected static String removeExt(String f, String[] exts) {
		String fName = new File(f).getName();
		for (String ext: exts) {
			if (fName.endsWith(ext)) {
				int i = fName.lastIndexOf(ext);
				return fName.substring(0, i);
			}
		}
		return f;
	}
	
	public static String removeExt(String f) {
		if (isBigWig(f)) {
			return removeExt(f, BWIG);
		} else if (isWig(f)) {
			return removeExt(f, WIG);
		} else if (isGFF(f)) {
			return removeExt(f, GFF);
		} else {
			return f;
		}
	}
	
}
