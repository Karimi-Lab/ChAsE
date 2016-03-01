// added by hyounesy. inspired by java.awt.Scrollbar
package controlP5;

import processing.core.PApplet;

public class Scrollbar extends Controller {

	// the actual maximum value of the scroll bar is the maximum minus the visible amount.
	// The left side of the thumb (bubble) indicates the value of the scroll bar. 
	// e.g. if maximum is 300 and the visible amount is 60, the actual maximum value is 240. 

	public static final int HORIZONTAL = 0;
	public static final int VERTICAL   = 1;
	
	// _myValue, _myMin, _myMax already defined in parent class Controller
	int     _orientation;	// indicates the orientation of the scroll bar.
    float   _visibleAmount;	// the size of the scroll bar's thumb (bubble), representing the visible portion.
	boolean _isResizable = false; // whether the scrollbar is a resizable scrollbar.
	boolean _isDragging = false;  // whether the scrollbar is being dragged (either moved or resized)
	
	float	_sizerLength; // length of the sizer region for resizable scrollbar (in pixels)
	float   _minThumbLength; // minimum length of the thumb (in pixels)
	float   _minVisibleAmount; // minimum visible amount.

	private float _mouseOffset; // the mouse offset from the top of the thumb. (in scroller units, and not in pixels)
	
	public Scrollbar(
			ControlP5 theControlP5,
			ControllerGroup theParent,
			String theName,
			float theX, float theY, int theWidth, int theHeight)
	{
		super(theControlP5, theParent, theName, theX, theY, theWidth, theHeight);
		
		_orientation = theWidth > theHeight ? HORIZONTAL : VERTICAL;
	    _myValue = 0;
	    _visibleAmount = 1;
	    _myMin = 0;
	    _myMax = 100;
	    _isResizable = false;
		_minThumbLength = 10;
		_minVisibleAmount = 1 ;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see controlP5.Controller#updateInternalEvents(processing.core.PApplet)
	 */
	public void updateInternalEvents(PApplet theApplet) {
		if (_myControlWindow.isMouseOver)
		{
			float thumbLength  = _visibleAmount * (_orientation == HORIZONTAL ? width : height) / (_myMax - _myMin);
			//_sizerLength = Math.min(_orientation == HORIZONTAL ? height : width, thumbLength / 4);

			float m = _orientation == HORIZONTAL ?
					_myMin + (_myMax - _myMin) * (_myControlWindow.mouseX - position.x()) / width :
					_myMin + (_myMax - _myMin) * (_myControlWindow.mouseY - position.y()) / height;

			if (_isDragging)
			{
				if (_myControlWindow.mousePressed)
				{
					theApplet.cursor(PApplet.ARROW);
					float newValue = m - _mouseOffset;
					
					if (_isResizable && _mouseOffset < _visibleAmount*_sizerLength/thumbLength)
					{// starting sizer
						_visibleAmount = Math.max(_minVisibleAmount, _visibleAmount + _myValue - Math.max(newValue, _myMin));
						setValue(newValue);
					}
					else if (_isResizable && _mouseOffset > _visibleAmount * (1 - _sizerLength/thumbLength))
					{// ending sizer
						_visibleAmount = Math.min(Math.max(_minVisibleAmount, _visibleAmount + newValue - _myValue), _myMax - _myValue);
						_mouseOffset += newValue - _myValue;
						setValue(_myValue); // just to fire the event
					}
					else
					{
						setValue(Math.max(newValue, _myMin));
					}
				}
				else
				{
					_isDragging = false;
					setValue(_myValue); // just to fire the event
				}
			}
			else if (_myControlWindow.mouseX >= position.x() && _myControlWindow.mouseX <= position.x() + width &&
				     _myControlWindow.mouseY >= position.y() && _myControlWindow.mouseY <= position.y() + height &&
				     isMousePressed)
			{
				if (m >= _myValue && m <= _myValue + _visibleAmount)
				{
					_mouseOffset = m - _myValue;
					_isDragging = true;
				}
			}
		}
	}

	@Override
	protected void mousePressed() {
	}

	@Override
	protected void mouseReleased() {
	}

	@Override
	protected void mouseReleasedOutside() {
	}
	
    /**
     * Sets the values of four properties for this scroll bar: value, visibleAmount, minimum, and maximum.
     */
	public void setValues(int orientation, float value, float visibleSize, float minimum, float maximum)
	{
		_orientation = orientation;
		_myMin = minimum;
		_myMax = maximum;
		setVisibleAmount(visibleSize);
		setValue(value);
	}

	public float getVisibleAmount() {
		return _visibleAmount;
	}

	public void setVisibleAmount(float newAmount) {
		_visibleAmount = Math.min(Math.max(newAmount, 0), _myMax - _myMin);
	}

	public boolean isResizable() {
		return _isResizable;
	}

	public void setResizable(boolean resizable)
	{
		_isResizable = resizable;
	}

	@Override
	public void setValue(float theValue) {
		_myValue = Math.min(Math.max(theValue, _myMin), _myMax - _visibleAmount);
		broadcast(FLOAT);
	}
	
	public boolean isDragging() {
		return _isDragging;
	}
	
	public float getMinThumbLength() {
		return _minThumbLength;
	}

	public void setMinThumbLength(float minLength) {
		_minThumbLength = minLength;
	}
	
	public float getMinVisibleAmount() {
		return _minVisibleAmount;
	}

	public void setMinVisibleAmount(float minAmount) {
		_minVisibleAmount = minAmount;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * controlP5.ControllerInterface#addToXMLElement(controlP5.ControlP5XMLElement
	 * )
	 */
	public void addToXMLElement(ControlP5XMLElement theElement) {
		ControlP5.logger().info("saving Scrollbar is not implemented yet.");
	}
	
	
	public void updateDisplayMode(int theMode) {
		_myDisplayMode = theMode;
		switch (theMode) {
		case (DEFAULT):
			_myDisplay = new ScrollbarDisplay();
			break;
		case (IMAGE):
		case (SPRITE):
		case (CUSTOM):
		default:
			break;
		}
	}

	class ScrollbarDisplay implements ControllerDisplay
	{
		public void display(PApplet theApplet, Controller theController)
		{

			theApplet.pushStyle();
			_myValue = Math.min(Math.max(_myValue, _myMin), _myMax - _visibleAmount);

			theApplet.strokeWeight(1.f);
			boolean bFilled = theController.color().getAlpha() != 0;
			if (bFilled) {
				theApplet.noStroke();
			} else {
				theApplet.noFill();
			}
			
			theApplet.smooth();

			if (bFilled) {
				theApplet.fill(theController.color().colorBackground);
			} else {
				theApplet.stroke(theController.color().colorBackground | 0xFF000000);
			}
			
			theApplet.rect(0, 0, width, height);
			
			float thumbLength  = _visibleAmount * (_orientation == HORIZONTAL ? width : height) / (_myMax - _myMin); 
			_sizerLength = Math.min(_orientation == HORIZONTAL ? height : width, thumbLength / 3);
			
			float size   = _visibleAmount / (_myMax - _myMin);
			float offset = (_myValue - _myMin) / (_myMax - _myMin);
			float gap = 2;

			if (bFilled) {
				theApplet.fill(theController.color().colorForeground);
			} else {
				theApplet.stroke(theController.color().colorForeground  | 0xFF000000);
			}
			
			if (_orientation == HORIZONTAL)
			{
				// draw the thumb
				theApplet.rect(offset * width + gap, gap, size * width - gap*2, height - gap*2);
				
				if (_isResizable && _sizerLength > gap*2)
				{// draw the sizers
					gap += 2;

					if (bFilled) {
						theApplet.fill(theController.color().colorActive);
					} else {
						theApplet.stroke(theController.color().colorActive | 0xFF000000);
					}
					// left arrow
					theApplet.beginShape();
					theApplet.vertex(offset * width + gap, height / 2);
					theApplet.vertex(offset * width + _sizerLength, height - gap);
					theApplet.vertex(offset * width + _sizerLength, gap);
					theApplet.endShape(PApplet.CLOSE);
	
					//right arrow
					theApplet.beginShape();
					theApplet.vertex((offset + size) * width - gap, height / 2);
					theApplet.vertex((offset + size) * width - _sizerLength, height - gap);
					theApplet.vertex((offset + size) * width - _sizerLength, gap);
					theApplet.endShape(PApplet.CLOSE);
				}
			}
			else if (_orientation == VERTICAL)
			{
				// draw the thumb
				theApplet.rect(gap, offset*height + gap, width - gap*2, size * height - gap*2);

				if (_isResizable && _sizerLength > gap*2)
				{// draw the sizers
					gap += 2;

					if (bFilled) {
						theApplet.fill(theController.color().colorActive);
					} else {
						theApplet.stroke(theController.color().colorActive | 0xFF000000);
					}
					
					// up arrow
					theApplet.beginShape();
					theApplet.vertex(width / 2, offset * height + gap);
					theApplet.vertex(width - gap, offset * height + _sizerLength);
					theApplet.vertex(        gap, offset * height + _sizerLength);
					theApplet.endShape(PApplet.CLOSE);
	
					//down arrow
					theApplet.beginShape();
					theApplet.vertex(width / 2, (offset + size) * height - gap);
					theApplet.vertex(width - gap, (offset + size) * height - _sizerLength);
					theApplet.vertex(        gap, (offset + size) * height - _sizerLength);
					theApplet.endShape(PApplet.CLOSE);
				}
			}
			
			theApplet.popStyle();

			//_myCaptionLabel.draw(theApplet, 0, height + 4);
			//_myValueLabel.draw(theApplet, _myCaptionLabel.width() + 4, height + 4);
		}

	}
}
