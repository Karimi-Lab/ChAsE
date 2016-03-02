package util;

public class Utils {

	public static class Rect
	{
		public Rect() {left = top = width = height = 0;}
		public Rect(float l, float t, float w, float h) {left = l; top = t; width = w; height = h;}
		public Rect clone()
		{
			return new Rect(left, top, width, height);
		}
		public float left() {return left;}
		public float right() {return left + width;}
		public float top() {return top;}
		public float bottom() {return top + height;}
		public float width() {return width;}
		public float height() {return height;}
		public float hcenter() {return left + width/2;}
		public float vcenter() {return top + height/2;}
		public int ileft() {return (int)left;}
		public int iright() {return (int)(left + width);}
		public int itop() {return (int)top;}
		public int ibottom() {return (int)(top + height);}
		public int iwidth() {return (int)width;}
		public int iheight() {return (int)height;}
		public int ihcenter() {return (int)(left + width/2);}
		public int ivcenter() {return (int)(top + height/2);}
		public void setLeft(float l) {left = l;}
		public void setRight(float r) {width = r - left;}
		public void setTop(float t) {top = t;}
		public void setBottom(float b) {height = b - top;}
		public void setWidth(float w) {width = w;}
		public void setHeight(float h) {height = h;}
		public boolean isInside(double x, double y) {return x >= left && x <= right() && y >= top && y <= bottom();}
		
		private float left, top, width, height;
	}

	public static class Bound2D
	{
		public Bound2D() {xmin = ymin = Double.POSITIVE_INFINITY; xmax = ymax = Double.NEGATIVE_INFINITY;}
		public Bound2D(double _xmin, double _xmax, double _ymin, double _ymax) {xmin = _xmin; ymin = _ymin; xmax = _xmax; ymax = _ymax;}
		public Bound2D clone()
		{
			return new Bound2D(xmin, xmax, ymin, ymax);
		}
		public double dx() {return xmax - xmin;}
		public double dy() {return ymax - ymin;}
		public boolean isInside(double x, double y) {return x >= xmin && x <= xmax && y >= ymin && y <= ymax;}
		public double xmin, xmax;
		public double ymin, ymax;
	}

    public static int[] intSequence(int start, int end, int step)
    {
    	int size = (end - start) / step + 1;
    	if (size <= 0)
    		return null;
    	
    	int[] a= new int[size];
    	for (int i = 0; i < size; ++i)
    	{
    		a[i] = start + i*step;
    	}
    	return a;
    }

    public static double[] subArray (double[] original, int[] indices)
    {
    	double[] a = new double[indices.length];
    	for (int i = 0; i < indices.length; ++i)
    		a[i] = original[indices[i]];
    	return a;
    }
    
    /**
     * concatenates two arrays
     * @param a   {a[0], ..., a[n-1]}
     * @param b   {b[0], ..., b[m-1]}
     * @return c = {a[0], ..., a[n-1], b[0], ..., b[m-1]}
     */
    public static double[] concatArray(double[] a, double[] b)
    {
    	double[] result = new double[a.length + b.length];
    	System.arraycopy(a, 0, result, 0, a.length);
    	System.arraycopy(b, 0, result, a.length, b.length);
    	return result;
    }
    
    /**
     * concatenates two arrays
     * @param a   input array: {a[0], ..., a[n-1]}
     * @param b   input array: {b[0], ..., b[m-1]}
     * @return    new output array: c = {a[0], ..., a[n-1], b[0], ..., b[m-1]}
     */
    public static int[] concatArray(int[] a, int[] b)
    {
    	int[] result = new int[a.length + b.length];
    	System.arraycopy(a, 0, result, 0, a.length);
    	System.arraycopy(b, 0, result, a.length, b.length);
    	return result;
    }

    /**
     * returns element-wise addition of two arrays
     * @param a  input array
     * @param b  input array
     * @return   new output array: c[i] = a[i] + b[i]
     */
    public static double[] sumArray(double[] a, double[] b)
    {
    	if (a.length != b.length)
    		return null;
    	
    	double[] result = new double[a.length];
    	for (int i = 0; i < a.length; ++i)
    		result[i] = a[i] + b[i];
    	return result;
    }

    /**
     * returns element-wise multiplication of an array with a scalar
     * @param a    input array
     * @param m    multiplicant
     * @return     new output array: c[i] = m * a[i]
     */
    public static double[] multArray(double[] a, double m)
    {
    	double[] result = new double[a.length];
    	for (int i = 0; i < a.length; ++i)
    		result[i] = a[i] * m;
    	return result;
    }
    
    /**
     * clamps the array elements between a min an max value
     * @param a     input array
     * @param min   min value to clamp against
     * @param max   max value to clamp against
     * @return      reference to input array
     */
    public static double[] clampArray(double[] a, double min, double max)
    {
    	for (int i = 0; i < a.length; ++i)
    		a[i] = Math.min(Math.max(a[i], min), max);
    	return a;
    }
    
    /**
     * Reverses the array elements
     * @param a     input array
     * @return      reference to input array
     */
    public static double[] reverseArray(double[] a)
    {
    	for (int i = 0; i < a.length / 2; ++i)
    	{
    		double tmp = a[i];
    		a[i] = a[a.length - i - 1];
    		a[a.length - i - 1] = tmp;
    	}
    	return a;
    }

    /*
    @SuppressWarnings("unchecked")
    public static <T> T[] concat(T[] a, T[] b) {
        final int alen = a.length;
        final int blen = b.length;
        final T[] result = (T[]) java.lang.reflect.Array.
                newInstance(a.getClass().getComponentType(), alen + blen);
        System.arraycopy(a, 0, result, 0, alen);
        System.arraycopy(b, 0, result, alen, blen);
        return result;
    }
    */

}
