/////////////////////////////////////////////////////////////////////////////
// This file is part of the OPeNDAP Data Connector project.
//
// Copyright (c) 2007 OPeNDAP, Inc.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
//
// You can contact OPeNDAP, Inc. at PO Box 112, Saunderstown, RI. 02874-0112.
/////////////////////////////////////////////////////////////////////////////

package opendap.clients.odc;

/**
 * <p>Title: Form Layout</p>
 * <p>Description: lays out swing components in a form-like manner</p>
 * <p>Copyright: Copyright (c) 2004-7</p>
 * <p>Company: OPeNDAP.org</p>
 * @author John Chamberlain
 * @version 2.64
 */

/**
 *  This is FormLayout. In general it is comparable to the GridLayout with the difference
 *  that the concept of a label-control pair is supported. Unlike the GridLayout components
 *  in it cannot be overlapped nor can multiple elements span multiple rows. In all other
 *  respects however you will probably find that FormLayout is simpler and more powerful
 *  to use than GridLayout.
 *
 *  To use this layout manager first add your elements, both labels and controls to the form
 *  container without using any constraints (FormLayout does not use the constraint system).
 *  If you have no further requirements and have added the elements in the order label,
 *  component, label, component, etc, then the layout manager will display the form correctly.
 *
 *  To control the order and placement of the elements use the setOrder method. For example,
 *  if you set the order of A to 1 and B to 2 then element A will appear on line 1 and element
 *  B will appear on line 2. The numbers are relative so if you skip a number then the greater
 *  numbered element will follow the lower-numbered element. If you use the same number for two
 *  or more different elements then the order will be determined by the order in which they
 *  were added to the container.
 *
 *  The component specified in the setOrder methods can be either the label component or the
 *  control component. It makes no difference which is used. This is true of the spacing and fill
 *  methods as well. It is also allowable to make either the label or control null.
 *
 *  To control vertical sizing of elements such as list boxes set their preferred size to be the
 *  size desired. You have the option of setting one element as the vertical fill using the
 *  setFill_Vertical method. In this case all other components will be vertically sized to their
 *  preferred size and the row containing the fill element will take all remaining vertical space.
 *
 *  Horizontal Fill: You can specify a horizontal fill element or elements for each row using
 *  the setFill_Horizontal method. Fill settings only apply to the control part of the element.
 *  Labels and all the remaining elements in the row will be sized to their preferred size.
 *  If you wish to weight fill elements you can do so using the the setHorizontalWeight method.
 *  Elements that do not have a weight specified default to a weighting of 1 (the lowest weighting).
 *  For example, if there are three elements in a row, A, B and C, and B is weighted to 2 and C is
 *  weighted to 3 then B will be set to double A's size and C will be triple A's size. Min/Max
 *  settings have precedence over weightings.
 *
 *  Weightings: weightings can be specified for elements that are set to fill. In this case
 *  preferred sizes are ignored and the elements are scaled to their weighting. If an element
 *  is not set to fill its weighting is ignored. Weightings must be positive integers. If a negative
 *  weighting is supplied, then the absolute value of the weighting will be used.
 *
 *  Global Fill: you can put the form into global fill mode to fill all available space using the
 *  the global fill property. When global fill is true all alignment and other fill settings are
 *  ignored. JLabel elements will be sized to their preferred size and all other elements will be
 *  sized to fill the available space. In this mode weightings are respected.
 *
 *  Alignment: the FormLayout ignores alignment settings on the elements themselves. You must use the
 *  FormLayout's setAlignment method. Alignment comes in two basic flavors:
 *     - element (none, left, center or right)
 *     - column (none, left, center, right, label, control)
 *  You can either set alignment by column or by element. Element alignment must be left, center or right.
 *  Only columns can be label or control aligned. Label alignment means every label in the column will
 *  be right-aligned. Control alignment means every control in the column will be left-aligned. The difference
 *  between the two is that if the column is label-aligned then if separations are different on different
 *  rows then the controls will be indented (not lined up), and vice versa.  Element alignment overrides
 *  column alignment so if an element has its own alignment it will ignore the boundaries of the column.
 *
 *  EXAMPLE: columns 1 and 3 are label-aligned and element D (in column 2) is center-aligned. Elements H
 *  and I are left-aligned and J is right-aligned:
 *
 *        AAA: AAAAAAA                         BBB: BBBBBBBB
 *       CCCC: CCCCCCCCCCCC    DDD:DDDDDD       EE: EEEEEEEEEEEEE
 *         FF: FFFFF                        GGGGGG: GGGGGG
 *    HHH: HHHHHHHHHHHHHHHHHHHHH II: IIIIIIIIII       JJJJ: JJJJJ
 *
 *  Columns: when ordering elements you can assign their columns. If a column is not assigned or is the same
 *  as another element in the same row then the element will be layed out in the next available column. For example,
 *  if element D in the example above were not assigned to column 2 it would still lay out in column 2 because
 *  that is the next available column, but if none of the elements were assigned to a column then element D's
 *  center alignment would be ignored and it would appear in column 2 with B and G. Element E would then be in
 *  column 3 by itself. If there is no element in a column that column is ignored.
 *
 *  Note that column alignments can only be done for the first thousand columns.
 *
 *  Filling and Columns:  If a column is aligned then all elements in that column will by default stay within
 *  the bounds of the column. If an element is given its own alignment it will ignore the column setting and
 *  be aligned according to its neighboring elements, no matter what column they are in. The behaviour of
 *  elements in columns with no alignment is the same.
 *
 *  Spacing: there are five kinds of spacing, indent, separation, trailing, above and below. Indent spacing is
 *  placed before the element, separation is placed between the label and control only if both are present,
 *  trailing is placed after the element, and vertical line spacing is added above and/or below the line.
 */

import java.awt.*;
import java.util.ArrayList;

// imports for test panel
import javax.swing.*;

//layout manager interface:
//	public void invalidateLayout( Container target ){}
//	public void layoutContainer( Container container ){}
//	public void addLayoutComponent( String sName, Component component ){}
//	public void addLayoutComponent( Component component, Object constraints ){}
//	public void removeLayoutComponent( Component component ){}
//	public float getLayoutAlignmentX( Container container ){ return 0; }
//	public float getLayoutAlignmentY( Container container ){ return 0; }
//	public Dimension preferredLayoutSize( Container container ){ return null; }
//	public Dimension minimumLayoutSize( Container container ){ return null; }
//	public Dimension maximumLayoutSize( Container container ){ return null; }

public class FormLayout implements LayoutManager2 {

	public static final void main( String[] args ){
		JFrame frameTest = new JFrame();
		FormTestPanel panelTest = new FormTestPanel();
		frameTest.getContentPane().add( panelTest );
		frameTest.setVisible( true );
	}

	Container mContainer; // each form layout can only manage one container

	int mpxMinimumWidth = 0;
	int mpxPreferredWidth = 0;
	int mpxMaximumWidth = 0;
	int mpxMinimumHeight = 0;
	int mpxPreferredHeight = 0;
	int mpxMaximumHeight = 0;

    public FormLayout( Container container ){ mContainer = container; }
	public void layoutContainer( Container ignored ){
		Dimension dimContainerSize = mContainer.getSize();
		Insets insetsContainer = mContainer.getInsets();
		double widthCanvas  = dimContainerSize.getWidth() - insetsContainer.left - insetsContainer.right;
		double heightCanvas = dimContainerSize.getHeight() - insetsContainer.top - insetsContainer.bottom;
		int ctComponent = mContainer.getComponentCount();

		// eliminate any defined elments that are no longer in the container
		for( int xElement = listDefinedElements.size(); xElement > 0; xElement-- ){
			FormElement element = (FormElement)listDefinedElements.get(xElement - 1);
			boolean zLabelExists = false;
			boolean zControlExists = false;
			if( element.componentLabel != null ){
				for( int xComponent = 1; xComponent <= ctComponent; xComponent++ ){
					Component component = mContainer.getComponent( xComponent - 1 );
					if( component == element.componentLabel ){ zLabelExists = true; break; }
				}
			}
			if( element.componentControl != null ){
				for( int xComponent = 1; xComponent <= ctComponent; xComponent++ ){
					Component component = mContainer.getComponent( xComponent - 1 );
					if( component == element.componentControl ){ zControlExists = true; break; }
				}
			}
			if( zLabelExists || zControlExists ){
				if( element.componentLabel != null && !zLabelExists ) element.componentLabel = null;
				if( element.componentControl != null && !zControlExists ) element.componentControl = null;
			} else { // defined control no longer exists, delete it
				listDefinedElements.remove(element);
			}
		}

System.out.println("form has " + ctComponent + " components");

		// define any components in the container that are not defined
		Component componentLabel = null;
		Component componentControl = null;
		Component componentCurrent = null;
		for( int xComponent = 1; xComponent <= ctComponent; xComponent++ ){
			componentLabel = null;
			componentControl = null;
			componentCurrent = mContainer.getComponent( xComponent - 1 );
			FormElement elementCurrent = getFormElement( componentCurrent );
			if( elementCurrent == null ){
				if( componentCurrent instanceof javax.swing.JLabel ||
					componentCurrent instanceof java.awt.Label ){
					componentLabel = componentCurrent;
System.out.println("putting label component " + xComponent + " in element");
					if( xComponent < ctComponent ){
						Component componentNext = mContainer.getComponent( xComponent );
						FormElement elementNext = getFormElement( componentNext );
						if( elementNext == null ){
							if( ! (componentNext  instanceof javax.swing.JLabel) &&
								! (componentNext instanceof java.awt.Label ) ){
								componentControl = componentNext;
System.out.println("putting control component " + xComponent + " in element with label");
								xComponent++;
							}
						}
					}
				} else {
					componentControl = componentCurrent;
System.out.println("putting control component " + xComponent + " in element");
				}
				setOrder(componentLabel, componentControl, false);
System.out.println("set order");
			} else {
				// element is defined
			}
		}

		int ctElement = listDefinedElements.size();

System.out.println("form has " + ctElement + " elements");

// TODO the mappings must be improved to take into account alignments

		// establish the row mapping
		int[] aiRowMapping = new int[ctElement + 1]; // maps elements to rows (one-based)
		int iRowMax = 0;
		for( int xElement = 1; xElement <= ctElement; xElement++ ){ // find the max row
			FormElement element = (FormElement)listDefinedElements.get( xElement - 1 );
			if( element.miRow > iRowMax ) iRowMax = element.miRow;
		}
		for( int xElement = 1; xElement <= ctElement; xElement++ ){ // assign original mappings
			FormElement element = (FormElement)listDefinedElements.get( xElement - 1 );
			if( element.miRow > 0 ){
				aiRowMapping[xElement] = element.miRow;
			} else {
				aiRowMapping[xElement] = ++iRowMax;
			}
		}
System.out.println("row mapping established");

		// compress mapping into sequential list of rows - this eliminates skipped row numbers
		// in other words if the elements have rows like 2 3 4 4 4 6 7 9 9
		// the mapping will become 1 2 3 3 3 4 5 6 6
		int[] aiRowMappingSorted = new int[ctElement + 1];
		System.arraycopy(aiRowMapping, 0, aiRowMappingSorted, 0, ctElement + 1);
		java.util.Arrays.sort(aiRowMappingSorted);
		int iSequentialMapping = 0;
		for( int xSortedMapping = 1; xSortedMapping <= ctElement; xSortedMapping++ ){
			if( aiRowMappingSorted[xSortedMapping] != aiRowMappingSorted[xSortedMapping - 1] ) iSequentialMapping++;
			for( int xMapping = 1; xMapping < aiRowMapping.length; xMapping++ ){
				if( aiRowMapping[xMapping] == aiRowMappingSorted[xSortedMapping] ) aiRowMapping[xMapping] = iSequentialMapping;
			}
		}
		int ctRows = iSequentialMapping; // the last mapping will now be the total number of rows
System.out.println("rows sequenced");

		// determine how many elements are in each row and the max number of elements in any row (= the number of columns)
		int[] aiRowElementCount = new int[ ctRows + 1 ];
		int ctColumns = 0;
		for( int xElement1 = 1; xElement1 <= ctElement; xElement1++ ){
			aiRowElementCount[aiRowMapping[xElement1]]++;
			if( aiRowElementCount[aiRowMapping[xElement1]] > ctColumns )
			    ctColumns = aiRowElementCount[aiRowMapping[xElement1]];
		}
System.out.println("row element count determined: " + ctColumns);

		// repeat the row process for columns with the difference that elements in
		// the same row and the same column will be adjusted to the next available column
		int[] aiColumnMapping = new int[ctElement + 1]; // maps elements to Columns (one-based)
		int iColumnMax = 0;
		for( int xElement = 1; xElement <= ctElement; xElement++ ){ // find the max Column
			FormElement element = (FormElement)listDefinedElements.get( xElement - 1 );
			if( element.miColumn > iColumnMax ) iColumnMax = element.miColumn;
		}
		int[] aiElementInCurrentRow = new int[iColumnMax + 1];
		for( int xRow = 1; xRow <= ctRows; xRow++ ){

			// clear the row buffer
			for( int xColumn = 1; xColumn <= iColumnMax; xColumn++ ){
				aiElementInCurrentRow[xColumn] = 0;
			}

			// isolate elements in this row with an assigned column and adjust any duplicates
			for( int xElement = 1; xElement <= ctElement; xElement++ ){
				if( aiRowMapping[xElement] == xRow ){
					FormElement element = (FormElement)listDefinedElements.get( xElement - 1 );
					if( element.miColumn > 0 ){
						if( aiElementInCurrentRow[element.miColumn] == 0 ){
							aiElementInCurrentRow[element.miColumn] = xElement;
						} else { // duplicate column assigment, determine whether there is space to the right
							int xGapLocation = element.miColumn + 1;
							for( ; xGapLocation <= iColumnMax; xGapLocation++ ){
								if( aiElementInCurrentRow[xGapLocation] == 0 ) break; // found gap
							}
							if( xGapLocation <= iColumnMax ){ // shift to right
								while( aiElementInCurrentRow[xGapLocation - 1] != element.miColumn ){
									aiElementInCurrentRow[xGapLocation] = aiElementInCurrentRow[xGapLocation - 1];
									xGapLocation--;
								}
							} else { // must shift to left
								for( xGapLocation = element.miColumn - 1; xGapLocation > 0; xGapLocation-- ){
									if( aiElementInCurrentRow[xGapLocation] == 0 ) break; // found gap
								}
								while( aiElementInCurrentRow[xGapLocation + 1] != element.miColumn ){
									aiElementInCurrentRow[xGapLocation] = aiElementInCurrentRow[xGapLocation + 1];
									xGapLocation++;
								}
							}
							aiElementInCurrentRow[xGapLocation] = xElement;
						}
					}
				}
			}
System.out.println("row " + xRow + " column mapping established");

			// add any unassigned elements
			for( int xElement = 1; xElement <= ctElement; xElement++ ){
				if( aiRowMapping[xElement] == xRow ){
					FormElement element = (FormElement)listDefinedElements.get( xElement - 1 );
					if( element.miColumn == 0 ){
						for( int xColumn = 1; xColumn <= iColumnMax; xColumn++ ){
							if( aiElementInCurrentRow[xColumn] == 0 ) aiElementInCurrentRow[xColumn] = xElement;
							break;
						}
					}
				}
			}
System.out.println("row " + xRow + " added any unassigned elements");

			// if any column assignments are greater than ctColumns shift assignments left to fit within ctColumns
			while( true ){
				int xRightmostAssignment = iColumnMax;
				for( ; xRightmostAssignment > 0; xRightmostAssignment-- ){
					if( aiElementInCurrentRow[xRightmostAssignment] != 0 ) break; // found the rightmost assignment
				}
				if( xRightmostAssignment <= ctColumns ) break; // we're ok, else shift left
				int xGapLocation = xRightmostAssignment - 1;
				for( ; xGapLocation > 0; xGapLocation-- ){
					if( aiElementInCurrentRow[xGapLocation] == 0 ) break; // found gap
				}
				for( ; xGapLocation < xRightmostAssignment; xGapLocation++ ){
					aiElementInCurrentRow[xGapLocation] = aiElementInCurrentRow[xGapLocation + 1];
				}
				aiElementInCurrentRow[xRightmostAssignment] = 0;
			}
System.out.println("row " + xRow + " left shifted columns");

			// create the column mappings for this row
			for( int xColumn = 1; xColumn < ctColumns; xColumn++ ){
				aiColumnMapping[aiElementInCurrentRow[xColumn]] = xColumn;
			}
System.out.println("row " + xRow + " column mappings complete");

		}

		// make the master array mapping
		// maps [column][row] to element index
		FormElement[][] aMapping = new FormElement[ ctColumns + 1 ][ ctRows + 1 ];
		for( int xElement = 1; xElement <= ctElement; xElement++ ){
			FormElement element = (FormElement)listDefinedElements.get( xElement - 1 );
			aMapping[aiColumnMapping[xElement]][aiRowMapping[xElement]] = element;
		}
System.out.println("master array mapping complete");

		// determine the column minimum width taking into account the column alignments
		int[] apxColumnMinimumWidth = new int[ ctColumns + 1 ];
		if( mzGlobalFill ){
System.out.println("global fill, alignments are ignored");
			// alignments are ignored
		} else {
System.out.println("determining column minimum widths out of " + ctColumns + " colummns");
			for( int xColumn = 1; xColumn <= ctColumns; xColumn++ ){
System.out.println("column " + xColumn);
				if( maiColumnAlignment[xColumn] == ALIGNMENT_None ){
					apxColumnMinimumWidth[xColumn] = 0;
				} else {
					for( int xRow = 1; xRow <= ctRows; xRow++ ){
System.out.println("row " + xRow);
						FormElement element = aMapping[xColumn][xRow];
						if( element == null ) continue;
						int pxMinimumWidth_Element =    element.miSpacing_indent
														+ ( element.componentLabel != null && element.componentControl != null ? element.miSpacing_separation : 0 )
														+ element.miSpacing_trailing
														+ ( element.componentLabel != null ? (int)element.componentLabel.getMinimumSize().getWidth() : 0 )
														+ ( element.componentControl != null ? (int)element.componentControl.getMinimumSize().getWidth() : 0 );
						if( pxMinimumWidth_Element > apxColumnMinimumWidth[xColumn] ) apxColumnMinimumWidth[xColumn] = pxMinimumWidth_Element;
System.out.println("pxMinimumWidth_Element: " + pxMinimumWidth_Element);
					}
				}
			}
		}
System.out.println("column minimum widths determined");

		// determine the component guide sizes ( widths )
		int pxMinimumWidth_form = 0;
		int pxPreferredWidth_form = 0;
		int pxMaximumWidth_form = 0;
		int[] apxWidth_preferred_row = new int[ ctRows + 1 ];
		for( int xRow = 1; xRow <= ctRows; xRow++ ){
			int pxMinimumWidth_CurrentRow = 0;
			int pxPreferredWidth_CurrentRow = 0;
			int pxMaximumWidth_CurrentRow = 0;
			int ctElementsInThisRow = aiRowElementCount[xRow];
System.out.println("row " + xRow + " has " + ctElementsInThisRow + " elements");
			for( int xColumn = 1; xColumn <= ctColumns; xColumn++ ){
				FormElement element = aMapping[xColumn][xRow];
				if( element == null ) continue;
				ctElementsInThisRow++;

				// minimum
				int pxMinimumWidth_Element =    element.miSpacing_indent
												+ ( element.componentLabel != null && element.componentControl != null ? element.miSpacing_separation : 0 )
												+ element.miSpacing_trailing
												+ ( element.componentLabel != null ? (int)element.componentLabel.getMinimumSize().getWidth() : 0 )
												+ ( element.componentControl != null ? (int)element.componentControl.getMinimumSize().getWidth() : 0 );
				if( apxColumnMinimumWidth[xColumn] > pxMinimumWidth_Element ) pxMinimumWidth_Element = apxColumnMinimumWidth[xColumn];
				element.iBounds_minimum_width = pxMinimumWidth_Element;
				pxMinimumWidth_CurrentRow += pxMinimumWidth_Element;

				// preferred
				int pxPreferredWidth_Element =  element.miSpacing_indent
												+ ( element.componentLabel != null && element.componentControl != null ? element.miSpacing_separation : 0 )
												+ element.miSpacing_trailing
												+ ( element.componentLabel != null ? (int)element.componentLabel.getPreferredSize().getWidth() : 0 )
												+ ( element.componentControl != null ? (int)element.componentControl.getPreferredSize().getWidth() : 0 );
				if( pxPreferredWidth_Element < element.iBounds_minimum_width ) pxPreferredWidth_Element = element.iBounds_minimum_width;
				element.iBounds_preferred_width = pxPreferredWidth_Element;
				pxPreferredWidth_CurrentRow += pxPreferredWidth_Element;
System.out.println("row preferred width:" + pxPreferredWidth_CurrentRow);

				// maximum
				int pxMaximumWidth_Element =  element.miSpacing_indent
												+ ( element.componentLabel != null && element.componentControl != null ? element.miSpacing_separation : 0 )
												+ element.miSpacing_trailing
												+ ( element.componentLabel != null ? (int)element.componentLabel.getMaximumSize().getWidth() : 0 )
												+ ( element.componentControl != null ? (int)element.componentControl.getMaximumSize().getWidth() : 0 );
				if( pxMaximumWidth_Element < element.iBounds_minimum_width ) pxMaximumWidth_Element = element.iBounds_minimum_width;
				element.iBounds_maximum_width = pxMaximumWidth_Element;
				pxMaximumWidth_CurrentRow += pxMaximumWidth_Element;

			}
			apxWidth_preferred_row[xRow] = pxPreferredWidth_CurrentRow;
			if( pxMinimumWidth_CurrentRow > pxMinimumWidth_form ) pxMinimumWidth_form = pxMinimumWidth_CurrentRow;
			if( pxPreferredWidth_CurrentRow > pxPreferredWidth_form ) pxPreferredWidth_form = pxPreferredWidth_CurrentRow;
			if( pxMaximumWidth_CurrentRow > pxMaximumWidth_form ) pxMaximumWidth_form = pxMaximumWidth_CurrentRow;

System.out.println("row " + xRow + " element ct: " + ctElementsInThisRow + " preferred width: " + pxPreferredWidth_CurrentRow + " minimum: " + pxMinimumWidth_CurrentRow + " max: " + pxMaximumWidth_CurrentRow );
		}
System.out.println("guide sizes determined");

		// adjust the guide sizes to account for alignments
		// TODO

		mpxMinimumWidth = pxMinimumWidth_form;
		mpxPreferredWidth = pxPreferredWidth_form;
		mpxMaximumWidth = pxMaximumWidth_form;

		// determine the form size ( widths )
		int pxFormWidth;
		if( pxPreferredWidth_form <= widthCanvas ){
			pxFormWidth = pxPreferredWidth_form;
		} else if( pxMinimumWidth_form <= widthCanvas ){
			pxFormWidth = (int)widthCanvas;
		} else {
			pxFormWidth = pxMinimumWidth_form;
		}

		// determine component realized size ( width )
		for( int xRow = 1; xRow <= ctRows; xRow++ ){


			int pxPreferredWidth_CurrentRow = apxWidth_preferred_row[xRow];
System.out.println("form width: " + pxFormWidth + " preferred width of row " + xRow + ": " + pxPreferredWidth_CurrentRow);
			int pxFillWidth_CurrentRow = pxFormWidth - pxPreferredWidth_CurrentRow;
			if( pxFillWidth_CurrentRow < 0 ) pxFillWidth_CurrentRow = 0;

			FormElement elementLastInRow = null;
			int psTotalWidthUsed = 0;
			if( pxFillWidth_CurrentRow == 0 ){ // there is no extra space in row, all elements will be their min/preferred size

				// in the first pass determine all the items at minimum size
				int pxPreferredWidth_non_minimums = 0;
				int pxTotalWidth_minimums = 0;
				float fScaleDown = pxPreferredWidth_CurrentRow == 0 ? 1 : 1 - pxFormWidth / pxPreferredWidth_CurrentRow;
				for( int xColumn = 1; xColumn <= ctColumns; xColumn++ ){
					FormElement element = aMapping[xColumn][xRow];
					if( element == null ) continue;
					if( element.iBounds_minimum_width > element.iBounds_preferred_width * fScaleDown ){
						element.iBounds_control_width = element.iBounds_minimum_width;
						pxTotalWidth_minimums += element.iBounds_control_width;
						psTotalWidthUsed += element.iBounds_control_width;
						elementLastInRow = element;
					} else {
						pxPreferredWidth_non_minimums += element.iBounds_preferred_width;
					}
				}

				// second pass: scale down all items that are above minimum size
				float fPreferredScaleDown =  pxPreferredWidth_non_minimums == 0 ? 1 : 1 - (pxFormWidth - pxTotalWidth_minimums)/pxPreferredWidth_non_minimums;
				int pxTotalWidth_scaled = 0;
				for( int xColumn = 1; xColumn <= ctColumns; xColumn++ ){
					FormElement element = aMapping[xColumn][xRow];
					if( element == null ) continue;
					if( element.iBounds_minimum_width > element.iBounds_preferred_width * fScaleDown ){
						// these items were sized to minimum in the first pass
					} else {
						element.iBounds_control_width = Math.round( element.iBounds_preferred_width * fPreferredScaleDown );
						pxTotalWidth_scaled += element.iBounds_control_width;
						psTotalWidthUsed += element.iBounds_control_width;
						elementLastInRow = element;
					}
				}

			} else { // non-fill items will be preferred size and fill items will stretch

				// non-fill items are their preferred sizes
				for( int xColumn = 1; xColumn <= ctColumns; xColumn++ ){
					FormElement element = aMapping[xColumn][xRow];
					if( element == null ) continue;
					if( ! element.mzFill ){
						element.iBounds_control_width = element.iBounds_preferred_width;
						psTotalWidthUsed += element.iBounds_control_width;
					}
				}

				// calculate the total weighting and number of stretchable items
				int iWeighting_total = 0;
				int ctFillElements = 0;
				for( int xColumn = 1; xColumn <= ctColumns; xColumn++ ){
					FormElement element = aMapping[xColumn][xRow];
					if( element == null || !element.mzFill ) continue;
					ctFillElements++;
					iWeighting_total += (element.miWeighting == 0) ? 1 : element.miWeighting;
				}

				// first pass: determine all the fill items at minimum size
				int pxTotalWidth_minimums = 0;
				for( int xColumn = 1; xColumn <= ctColumns; xColumn++ ){
					FormElement element = aMapping[xColumn][xRow];
					if( !element.mzFill || element == null ) continue;
					if( element.iBounds_minimum_width > pxFillWidth_CurrentRow * element.miWeighting / iWeighting_total ){
						element.iBounds_control_width = element.iBounds_minimum_width;
						pxTotalWidth_minimums += element.iBounds_minimum_width;
						psTotalWidthUsed += element.iBounds_control_width;
						elementLastInRow = element;
					}
				}

				// second pass: determine all the fill items at maximum size
				int pxTotalWidth_maximums = 0;
				for( int xColumn = 1; xColumn <= ctColumns; xColumn++ ){
					FormElement element = aMapping[xColumn][xRow];
					if( !element.mzFill || element == null ) continue;
					if( element.iBounds_minimum_width > pxFillWidth_CurrentRow * element.miWeighting / iWeighting_total ){
						// these items were sized in the first pass
					} else if( element.iBounds_maximum_width < (pxFillWidth_CurrentRow - pxTotalWidth_minimums) * element.miWeighting / iWeighting_total ){
						element.iBounds_control_width = element.iBounds_maximum_width;
						pxTotalWidth_maximums += element.iBounds_control_width ;
						psTotalWidthUsed += element.iBounds_control_width;
						elementLastInRow = element;
					}
				}

				// third pass: scale remaining items
				int pxTotalWidth_filled = 0;
				if( iWeighting_total == 0 ) iWeighting_total = 1;
				for( int xColumn = 1; xColumn <= ctColumns; xColumn++ ){
					FormElement element = aMapping[xColumn][xRow];
					if( element == null ) continue;
					if( element.iBounds_minimum_width > pxFillWidth_CurrentRow * element.miWeighting / iWeighting_total ){
						// these items were sized to minimum in the first pass
					} else if( element.iBounds_maximum_width < (pxFillWidth_CurrentRow - pxTotalWidth_minimums) * element.miWeighting / iWeighting_total ){
						// these items were sized to maximum in the second pass
					} else {
						int pxRemainingSpace = pxFillWidth_CurrentRow - pxTotalWidth_minimums - pxTotalWidth_maximums;
						element.iBounds_control_width = Math.round( pxRemainingSpace * element.miWeighting / iWeighting_total );
						pxTotalWidth_filled += element.iBounds_control_width;
						psTotalWidthUsed += element.iBounds_control_width;
						elementLastInRow = element;
					}
				}
			}

			// adjust the last item for rounding so that that total width matches form width
			if( psTotalWidthUsed != pxFormWidth && elementLastInRow != null ){
				elementLastInRow.iBounds_control_width += pxFormWidth - psTotalWidthUsed;
			}

			// TODO the spacing for null elements must be calculated and used
			// because there may be gaps

		}

		// determine the element locations
		int px_x = 0;
		int px_y = 0;
		px_y = insetsContainer.top + MARGIN_top;
		for( int xRow = 1; xRow <= ctRows; xRow++ ){
			px_x = insetsContainer.left + MARGIN_left;
			for( int xColumn = 1; xColumn <= ctColumns; xColumn++ ){
				// if( xRow > aiColumnElementCount[xColumn] || xColumn > aiRowElementCount[xRow] ) continue;
			}
		}

		// set the component bounds
		for( int xElement = 1; xElement <= ctElement; xElement++ ){
			FormElement element = (FormElement)listDefinedElements.get( xElement - 1 );
			element.componentLabel.setBounds( element.iBounds_label_x, element.iBounds_label_y, element.iBounds_label_width, element.iBounds_label_height );
			element.componentControl.setBounds( element.iBounds_control_x, element.iBounds_control_y, element.iBounds_control_width, element.iBounds_control_height );
		}
	}
	public void invalidateLayout( Container ignored ){}
	public float getLayoutAlignmentX( Container ignored ){ return 0; }
	public float getLayoutAlignmentY( Container ignored ){ return 0; }
	public Dimension preferredLayoutSize( Container parent ){
		int pxInsetsWidth = parent.getInsets().left + parent.getInsets().right;
		int pxInsetsHeigh = parent.getInsets().top + parent.getInsets().bottom;
		return new Dimension( mpxPreferredWidth + pxInsetsWidth, mpxPreferredHeight + pxInsetsHeigh );
	}
	public Dimension minimumLayoutSize( Container parent ){
		int pxInsetsWidth = parent.getInsets().left + parent.getInsets().right;
		int pxInsetsHeigh = parent.getInsets().top + parent.getInsets().bottom;
		return new Dimension( mpxMinimumWidth + pxInsetsWidth, mpxMinimumHeight + pxInsetsHeigh );
	}
	public Dimension maximumLayoutSize( Container parent ){
		int pxInsetsWidth = parent.getInsets().left + parent.getInsets().right;
		int pxInsetsHeigh = parent.getInsets().top + parent.getInsets().bottom;
		return new Dimension( mpxMaximumWidth + pxInsetsWidth, mpxMaximumHeight + pxInsetsHeigh );
	}

	/** custom formatting (all one-based arrays) */
	public static final int ALIGNMENT_None = 0;
	public static final int ALIGNMENT_Left = 1;
	public static final int ALIGNMENT_Center = 2;
	public static final int ALIGNMENT_Right = 3;
	public static final int ALIGNMENT_Label = 4;
	public static final int ALIGNMENT_Control = 5;
	public static final int USE_DEFAULT = -1;
	public int DEFAULT_SPACING_indent = 4;
	public int DEFAULT_SPACING_separation = 3;
	public int DEFAULT_SPACING_trailing = 8;
	public int DEFAULT_SPACING_above = 0;
	public int DEFAULT_SPACING_below = 6;
	public int MARGIN_left = 0;
	public int MARGIN_right = 0;
	public int MARGIN_top = 0;
	public int MARGIN_bottom = 0;
	ArrayList listDefinedElements = new ArrayList();
	int[] maiColumnAlignment = new int[1000]; // each index is a column
	boolean mzGlobalFill = false;
	FormElement mVerticalFill = null;

	private FormElement getFormElement( Component label, Component control ){
		if( label == null && control == null ) return null;
		for( int xElement = 1; xElement <= listDefinedElements.size(); xElement++ ){
			FormElement element = (FormElement)listDefinedElements.get(xElement - 1);
			if( element.componentLabel == label || element.componentControl == control ) return element;
		}
		return null;
	}

	private FormElement getFormElement( Component component ){
		if( component == null ) return null;
		for( int xElement = 1; xElement <= listDefinedElements.size(); xElement++ ){
			FormElement element = (FormElement)listDefinedElements.get(xElement - 1);
			if( element.componentLabel == component || element.componentControl == component ) return element;
		}
		return null;
	}

	public void setMargin( int left, int right, int top, int bottom ){
		MARGIN_left = left;
		MARGIN_right = right;
		MARGIN_top = top;
		MARGIN_bottom = bottom;
	}

	public void setSpacing_Default( int indent, int separation, int trailing, int above, int below ){
		DEFAULT_SPACING_indent = ( indent == USE_DEFAULT ) ? 4 : indent;
		DEFAULT_SPACING_separation = ( separation == USE_DEFAULT ) ? 3 : separation;
		DEFAULT_SPACING_trailing = ( trailing == USE_DEFAULT ) ? 8 : trailing;
		DEFAULT_SPACING_above = ( above == USE_DEFAULT ) ? 0 : above;
		DEFAULT_SPACING_below = ( below == USE_DEFAULT ) ? 6 : below;
	}
	public void setSpacing( Component item, int indent, int separation, int trailing, int above, int below ){
		FormElement element = getFormElement( item );
		if( element == null ) return;
		element.miSpacing_indent = ( indent == USE_DEFAULT ) ? 4 : indent;
		element.miSpacing_separation = ( separation == USE_DEFAULT ) ? 3 : separation;
		element.miSpacing_trailing = ( trailing == USE_DEFAULT ) ? 8 : trailing;
		element.miSpacing_line_above = ( above == USE_DEFAULT ) ? 0 : above;
		element.miSpacing_line_below = ( below == USE_DEFAULT ) ? 6 : below;
	}
	public void setOrder( Component label, Component control ){
		setOrder( label, control, true );
	}
	private void setOrder( Component label, Component control, boolean zReorder ){
		FormElement element = getFormElement( label, control );
		if( element == null ){
			if( label == null && control == null ) return;
			element = new FormElement();
			element.componentLabel = label;
			element.componentControl = control;
			listDefinedElements.add( element ); // add element at end
System.out.println("adding new element");
		} else {
System.out.println("element exists");
			if( zReorder ){
System.out.println("reordering element");
				listDefinedElements.remove( element );
				listDefinedElements.add( element ); // add element at end
			}
		}
	}

	/** one-based row number */
	public void setOrder( Component label, Component control, int row ){
		FormElement element = getFormElement( label, control );
		if( element == null ){
			if( label == null && control == null ) return;
			element = new FormElement();
			element.componentLabel = label;
			element.componentControl = control;
		} else {
			listDefinedElements.remove( element );
		}
		listDefinedElements.add( element ); // add element at end
		if( row > 0 ) element.miRow = row;
	}

	/** one-based column number */
	public void setOrder( Component label, Component control, int row, int column ){
		FormElement element = getFormElement( label, control );
		if( element == null ){
			if( label == null && control == null ) return;
			element = new FormElement();
			element.componentLabel = label;
			element.componentControl = control;
		} else {
			listDefinedElements.remove( element );
		}
		listDefinedElements.add( element ); // add element at end
		if( row > 0 ) element.miRow = row;
		if( column > 0 ) element.miColumn = column;
	}
	public void setFill_Vertical( Component item ){
		if( item == null ){
			mVerticalFill = null;
			return;
		}
		FormElement element = getFormElement( item );
		if( element != null ){
			mVerticalFill = element;
		}
	}
	public void setFill_Horizontal( Component component, boolean zValue ){
		FormElement element = getFormElement( component );
		if( element == null ) return;
		element.mzFill = zValue ;
	}
	public void setWeighting( Component component, int weight ){
		FormElement element = getFormElement( component );
		if( element == null ) return;
		if( weight < 0 ) weight = weight * -1;
		element.miWeighting = weight ;
	}
	public void setAlignment( Component component, int ALIGNMENT ){
		FormElement element = getFormElement( component );
		if( element == null || ALIGNMENT < 0 || ALIGNMENT > 5 ) return;
		element.miAlignment = ALIGNMENT ;
	}
	public void setAlignment( int ALIGNMENT ){ setAlignment( 1, ALIGNMENT); }
	public void setAlignment( int column, int ALIGNMENT ){
		if( column < 1 || column > 999 ) return; // cannot have more than 999 columns
		if( ALIGNMENT < 0 || ALIGNMENT > 5 ) return;
		maiColumnAlignment[column] = ALIGNMENT;
	}

	/** not used */
	public void addLayoutComponent( String sName, Component component ){}
	public void addLayoutComponent( Component component, Object constraints ){}
	public void removeLayoutComponent( Component component ){}

	public String toString(){
		return this.toString();
	}

}

class FormElement {
//	int[] maiSpacing_indent = new int[100];
//	int[] maiSpacing_separation = new int[100];
//	int[] maiSpacing_trailing = new int[100];
//	int[] maiSpacing_line = new int[100];
//	int[] maiWeighting = new int[100];
//	boolean[] mazFill = new boolean[100];
//	int[] maiAlignment = new int[100]; // each index is an element
	Component componentLabel = null;
	Component componentControl = null;
	int miRow;
	int miColumn;
	int miSpacing_indent;
	int miSpacing_separation;
	int miSpacing_trailing;
	int miSpacing_line_above;
	int miSpacing_line_below;
	int miWeighting;
	int miAlignment;
	boolean mzFill = false;
	boolean mzSpacer = false; // spacer elements are used where there would be a blank space in the grid

	int iBounds_minimum_width;
	int iBounds_preferred_width;
	int iBounds_maximum_width;
	int iBounds_width_total;
	int iBounds_height_total;
	int iBounds_label_x;
	int iBounds_label_y;
	int iBounds_label_width;
	int iBounds_label_height;
	int iBounds_control_x;
	int iBounds_control_y;
	int iBounds_control_width;
	int iBounds_control_height;
}

class FormTestPanel extends JPanel {
	JPanel panelControls = new JPanel();
	JPanel panelDisplay = new JPanel();
	FormTestPanel(){

		panelControls.add( new JLabel("control panel") );

		JLabel label1 = new JLabel("label 1");
System.out.println("label 1 preferred size: " + label1.getPreferredSize().getWidth() );

		panelDisplay.setLayout( new FormLayout(panelDisplay) );
		panelDisplay.add( label1 );
		panelDisplay.add( new JTextField("text field 1") );
		panelDisplay.add( new JLabel("label 2") );
		panelDisplay.add( new JTextField("text field 2") );
		panelDisplay.add( new JLabel("label 3") );
		panelDisplay.add( new JTextField("text field 3") );
		panelDisplay.add( new JLabel("label 4") );
		panelDisplay.add( new JTextField("text field 4") );
		panelDisplay.add( new JLabel("label 5") );
		panelDisplay.add( new JTextField("text field 5") );
		panelDisplay.add( new JLabel("label 6") );
		panelDisplay.add( new JTextField("text field 6") );

		this.setLayout( new java.awt.BorderLayout() );
		this.add( panelControls, java.awt.BorderLayout.NORTH );
		this.add( panelDisplay, java.awt.BorderLayout.CENTER );

	}


}
