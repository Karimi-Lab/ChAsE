package heidi.dimreduction;

import java.io.File;


import Jama.Matrix;

/** Java wrapper for glimmer embedding code. */
public class Glimmer {

	/** strips absolute path off given class' url */
	public static String getPathToJarfileDir(Object classToUse) {
		String url = classToUse.getClass().getResource(
				"/" + classToUse.getClass().getName().replaceAll("\\.", "/")
						+ ".class").toString();
		if (url.startsWith("file:")) url = url.substring(5);
		else if (url.startsWith("jar:file:")) url = url.substring(4+5);
		url = url.replaceFirst("/[^/]+\\.jar!.*$", "/");
		File dir = new File(url);
		url = dir.getAbsolutePath();
		return url;
	}

	// look for first occurrence of 'heidi' in path to Glimmer.class to form base path 
	static {
		String libpath = getPathToJarfileDir(new Glimmer());
		final String baseName = File.separator+"heidi"+File.separator;
		int basePos = libpath.indexOf(baseName);
		if (basePos != -1) 
		  libpath = libpath.substring(0, basePos+baseName.length())+"lib"+File.separator;
		else libpath = "."+File.separator;
		//System.setProperty("java.library.path", System.getProperty("java.library.path")+":"+libpath);
		//System.loadLibrary("glimmercpu");
		//TODO make the above system independent method work
	    try{
		      String osName= System.getProperty("os.name");
		      if (!osName.contains("Windows")) {
		  		  System.load(libpath+"libglimmercpu.jnilib");
		      } else {
		    	  System.load(libpath+"glimmercpu.dll");
		      }
		}catch (Exception e){
			System.out.println("Exception caught ="+e.getMessage());
		}
	}

	/**
	 * Java interface for glimmer producing an embedding of points pts into
	 * embedding coordinates eco, which are both assumed to be double[][] arrays
	 * of same length. If 
	 */
	public native int embed(double[][] pts, double[][] eco, boolean chalmers);
	int embed(double[][] pts, double[][] eco) {
		return embed(pts, eco, true);
	}

	public static native float[] embtest(double[][] pts, float[] tmp);
	
	/** test some features */
	public static void main(String[] args) {
		Glimmer g = new Glimmer();
		int npts = 20;
		int orig_dim = 5;
		int embed_dim = 2;
		Matrix pts = new Matrix(npts, orig_dim);
		Matrix eco = new Matrix(npts, embed_dim);
		java.util.Random rg = new java.util.Random( 19580428 );
		//double [][] pbuf = pts.getArray();
		for(int j=0; j<npts; j++)
			for(int i=0; i<orig_dim; i++) {
				pts.set(j,i, rg.nextDouble());
				//System.out.print(Double.toString(pbuf[j][i])+" ");
			}
		g.embed(pts.getArray(), eco.getArray(), true);
		pts.print(4, 2); //TODO ensure embedding coordinates are properly returned
		//TODO create a table from eco and add to project
	}

}
