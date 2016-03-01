// Based on the density plot sample from: http://www.openprocessing.org/visuals/?visualID=9210#
// Download link: http://www.openprocessing.org/visuals/applets/visual0dd820a5d02d8624c7903be37a9ff3c9/OpenProcessingSketch9210.zip

package still.gui;

import java.util.ArrayList;
import java.util.Collections;

import processing.core.PApplet;

class GradientPoint implements Comparable {

	public float pos;
	public int clr;

	public GradientPoint(float pos, int clr) {
		this.pos = pos;
		this.clr = clr;
	}

	public GradientPoint() {
	}

	public int compareTo(Object o) {
		GradientPoint other = (GradientPoint) o;
		return Float.compare(this.pos, other.pos);
	}
}

class PGradient {

	ArrayList gradient;
	int size;
	ArrayList colorList;
	boolean needCalc = true;
	static PApplet ap;
	
	static PGradient GRAY = null;
	static PGradient RED = null;
	static PGradient GREEN = null;
	static PGradient BLUE = null;
	static PGradient ORANGE = null;
	static PGradient MAGENTA = null;
	static PGradient CYAN = null;
	static PGradient FIRE = null;
	static PGradient JET = null;
	
	static PGradient Gradients[] = null;

	static public void init(PApplet a)
	{
		ap = a;

		GRAY = new PGradient(1024, new GradientPoint[] {
				new GradientPoint(0.00000f, ap.color(255, 255, 255)),
				new GradientPoint(1.00000f, ap.color(0, 0, 0))});

		RED = new PGradient(1024, new GradientPoint[] {
				new GradientPoint(0.00000f, ap.color(255, 255, 255)),
				new GradientPoint(1.00000f, ap.color(128, 0, 0))});

		GREEN = new PGradient(1024, new GradientPoint[] {
				new GradientPoint(0.00000f, ap.color(255, 255, 255)),
				new GradientPoint(1.00000f, ap.color(0, 128, 0))});

		BLUE = new PGradient(1024, new GradientPoint[] {
				new GradientPoint(0.00000f, ap.color(255, 255, 255)),
				new GradientPoint(1.00000f, ap.color(0, 0, 128))});

		ORANGE = new PGradient(1024, new GradientPoint[] {
				new GradientPoint(0.00000f, ap.color(255, 255, 255)),
				new GradientPoint(1.00000f, ap.color(128, 128, 0))});
		
		MAGENTA = new PGradient(1024, new GradientPoint[] {
				new GradientPoint(0.00000f, ap.color(255, 255, 255)),
				new GradientPoint(1.00000f, ap.color(128, 0, 128))});
		
		CYAN   = new PGradient(1024, new GradientPoint[] {
				new GradientPoint(0.00000f, ap.color(255, 255, 255)),
				new GradientPoint(1.00000f, ap.color(0, 128, 128))});

		FIRE = new PGradient(2 << 10, new GradientPoint[] {
				new GradientPoint(0.00000f, ap.color(255, 255, 255)),
				new GradientPoint(0.33333f, ap.color(255, 255, 0)),
				new GradientPoint(0.66666f, ap.color(255, 0, 0)),
				new GradientPoint(1.00000f, ap.color(0, 0, 0))});

		JET = new PGradient(2 << 10, new GradientPoint[] {
				new GradientPoint(0.00000f, ap.color(0, 0, 255)),
				new GradientPoint(0.33333f, ap.color(0, 255, 255)),
				new GradientPoint(0.66666F, ap.color(255, 255, 0)),
				new GradientPoint(1.00000f, ap.color(255, 0, 0))});
		
		Gradients = new PGradient[]{GRAY, RED, GREEN, BLUE, ORANGE, MAGENTA, CYAN, FIRE, JET};
	}
	
	public PGradient(int size) {
		gradient = new ArrayList();
		this.size = size;
		colorList = new ArrayList();
		for (int i = 0; i < size; i++) {
			colorList.add(ap.color(0));
		}
	}

	public PGradient(int size, GradientPoint[] pts) {
		gradient = new ArrayList();
		this.size = size;
		colorList = new ArrayList();
		for (int i = 0; i < size; i++) {
			colorList.add(ap.color(0));
		}

		for (int i = 0; i < pts.length; i++) {
			GradientPoint pt = pts[i];
			gradient.add(pt);
		}
	}

	public void addColor(float pos, int clr) {
		GradientPoint pt = new GradientPoint();
		pt.pos = pos;
		pt.clr = clr;
		gradient.add(pt);
		needCalc = true;
	}

	public void addColor(GradientPoint pt) {
		gradient.add(pt);
		needCalc = true;
	}

	public void calcGradient() {

		Collections.sort(gradient);
		for (int i = 1; i < gradient.size(); i++) {
			GradientPoint pt1 = (GradientPoint) gradient.get(i - 1);
			GradientPoint pt2 = (GradientPoint) gradient.get(i);

			int startPos = (int) (pt1.pos * size);
			int endPos = (int) (pt2.pos * size);
			int width = endPos - startPos;
			for (int j = 0; j < width; j++) {
				colorList.set(startPos + j, ap.lerpColor(pt1.clr, pt2.clr,
						(1f / width) * (float) j));
			}
		}
		needCalc = false;
	}

	public int getColor(int idx) {
		if (needCalc) {
			calcGradient();
		}
		return (Integer) colorList.get(Math.max(0, Math.min(idx, colorList.size() - 1)));
	}
}