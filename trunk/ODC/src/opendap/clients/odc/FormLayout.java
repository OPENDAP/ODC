package opendap.clients.odc;

/**
 * <p>Title: Form Layout</p>
 * <p>Description: lays out swing components in a form-like manner</p>
 * <p>Copyright: Copyright (c) 2004-8</p>
 * <p>Company: OPeNDAP.org</p>
 * @author John Chamberlain
 * @version 3.00
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
 *  To control the order and placement of the elements use the add(...) method. For example,
 *  if you add A to row 1 and B to row 2 then element A will appear on line 1 and element
 *  B will appear on line 2. If you use the same number for two or more different elements
 *  then the order will be determined by the order in which they were added to the container.
 *
 *  If you add an item with no row or column specified then it will go in the first
 *  unoccupied row. For example if you add A(1,3) and B(2,2) and C(4) and D() and E() then
 *  D will go in row 3, column 1 and E will go in row 5, column 1.
 *
 *  If two items have the same row and column, then the second item will be added as an additional
 *  column of the row. For example, if a row has 5 elements in it and elements 2 and 3 both
 *  specify column 2 then the elements will be arranged as follows: 12X453 where X is empty.
 *
 *  EXAMPLE: Element A is added as 3, element B is added, element C is added as 2, element D and
 *  element E are added, element F is added as 1, element G is added. The row order will be:
 *
 *  F G C D E A B
 *
 *  If two elements are assigned the same row but no column is specified they will be in the same
 *  row and the column order will depend on the order in which the elements were added.
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
 *  the global fill property. When global fill is true all alignment and other horizontal fill settings
 *  are ignored. Label elements will be sized to their preferred size and all other elements will be
 *  sized to fill the available space. In this mode weightings are respected. Global fill will only
 *  be applied when the canvas size exceeds the total preferred size of the elements. Global fill only
 *  applies to the width dimension.
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
 *  If elements in the same row have different spacing the next row will be pushed down accordingly. In other words
 *  rows will not overlap.
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
		int widthCanvas  = dimContainerSize.width - insetsContainer.left - insetsContainer.right;
		int heightCanvas = dimContainerSize.height - insetsContainer.top - insetsContainer.bottom;

		// eliminate any defined elments that are no longer in the container
		// define any components in the container that are not defined
		layout_1_IdentifyElements();

		int ctElement = listDefinedElements.size();

System.out.println("form has " + ctElement + " elements");

// TODO the mappings must be improved to take into account alignments

		// identify the set rows
		int ctSetRows = 0;
		int[] aiSetRows = new int[ctElement + 1];
		for( int xElement = 1; xElement <= ctElement; xElement++ ){
			FormElement element = (FormElement)listDefinedElements.get( xElement - 1 );
			if( element.miRow > 0 ){
				ctSetRows++;
				aiSetRows[ctSetRows] = element.miRow;
			}
		}

		// identify the set columns
//		int[][] aiSetColumns = new int[ctElement + 1][ctElement + 1];
//		for( int xElement = 1; xElement <= ctElement; xElement++ ){
//			FormElement element = (FormElement)listDefinedElements.get( xElement - 1 );
//			if( element.miRow > 0 ){
//				if( element.miColumn > 0 ){
//					for( int xColumn = 1; xColumn <= ctElement; xColumn++ ){
//						if( aiSetColumns[x
//			}
//		}

		// establish the row mapping
		java.util.Arrays.sort(aiSetRows);
		int[] aiRowMapping = new int[ctElement + 1]; // maps elements to rows (one-based)
		int iRowCurrent = 0;
		for( int xSetRow = 1; xSetRow <= ctSetRows; xSetRow++ ){
			for( int xElement = 1; xElement <= ctElement; xElement++ ){ // add any initial unset rows
				FormElement element = (FormElement)listDefinedElements.get( xElement - 1 );
				if( element.miRow > 0 ) break;
				iRowCurrent++;
				aiRowMapping[xElement] = iRowCurrent;
			}
		}

		int iRowMax = 0;
		int ctSpecifiedRows = 0;

		// count specified rows
		for( int xElement = 1; xElement <= ctElement; xElement++ ){ // find the max row
			FormElement element = (FormElement)listDefinedElements.get( xElement - 1 );
			if( element.miRow > 0 ){
				ctSpecifiedRows++;
				if( element.miRow > iRowMax ) iRowMax = element.miRow;
				aiMapping_Element2Row[xElement] = element.miRow;
			}
		}

		// map unspecified rows
		int ctUnspecifiedRows = 0;
		for( int xElement = 1; xElement <= ctElement; xElement++ ){
			FormElement element = (FormElement)listDefinedElements.get( xElement - 1 );
			if( element.miRow > 0 ){
				// dealt with above
			} else {
				ctUnspecifiedRows++;
				aiMapping_Element2Row[xElement] = iRowMax + ctUnspecifiedRows;
			}
		}

System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
System.out.println("row mapping established:");
System.out.println(dumpArray(aiMapping_Element2Row, 0, 0));
System.out.println("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");

		// compress mapping into sequential list of rows - this eliminates skipped row numbers
		// in other words if the elements have rows like 2 3 4 4 4 6 7 9 9
		// the mapping will become 1 2 3 3 3 4 5 6 6
		int[] aiMappingSorted_Element2Row = new int[ctElement + 1];
		System.arraycopy(aiMapping_Element2Row, 0, aiMappingSorted_Element2Row, 0, ctElement + 1);
		java.util.Arrays.sort(aiMappingSorted_Element2Row);
		int iSequentialMapping = 0;
		for( int xSortedMapping = 1; xSortedMapping <= ctElement; xSortedMapping++ ){
			if( aiMappingSorted_Element2Row[xSortedMapping] != aiMappingSorted_Element2Row[xSortedMapping - 1] ) iSequentialMapping++;
			for( int xMapping = 1; xMapping < aiMapping_Element2Row.length; xMapping++ ){
				if( aiMapping_Element2Row[xMapping] == aiMappingSorted_Element2Row[xSortedMapping] ) aiMapping_Element2Row[xMapping] = iSequentialMapping;
			}
		}
		int ctRows = iSequentialMapping; // the last mapping will now be the total number of rows
System.out.println("rows sequenced");

		// determine how many elements are in each row and the max number of elements in any row (= the number of columns)
		int[] aiRowElementCount = new int[ ctRows + 1 ];
		int ctMaxElementsPerRow = 0;
		for( int xElement1 = 1; xElement1 <= ctElement; xElement1++ ){
System.out.println("incrementing " + aiMapping_Element2Row[xElement1]);
			aiRowElementCount[aiMapping_Element2Row[xElement1]]++;
			if( aiRowElementCount[aiMapping_Element2Row[xElement1]] > ctMaxElementsPerRow )
			    ctMaxElementsPerRow = aiRowElementCount[aiMapping_Element2Row[xElement1]];
		}
System.out.println("max elements per row: " + ctMaxElementsPerRow);

		// repeat the row process for columns with the difference that elements in
		// the same row and the same column will be adjusted to the last column
		int[] aiColumnMapping = new int[ctElement + 1]; // maps elements to Columns (one-based)
		int iColumnMax = 1;
		int ctColumns = 0;
		for( int xElement = 1; xElement <= ctElement; xElement++ ){ // find the max Column
			FormElement element = (FormElement)listDefinedElements.get( xElement - 1 );
			if( element.miColumn > ctColumns ) ctColumns = element.miColumn;
		}
		int[] aiElementInCurrentRow = new int[ctElement + 1];
		for( int xRow = 1; xRow <= ctRows; xRow++ ){

			// clear the row buffer
			int ctElementsInRow = 0;
			for( int xElement = 1; xElement <= ctElement; xElement++ ){
				if( aiMapping_Element2Row[xElement] == xRow ){
					aiElementInCurrentRow[++ctElementsInRow] = xElement;
				}
			}
			if( ctElementsInRow == 0 ) continue;

			// determine the max specified column
			int ctSpecficiedColumns = 0;
			int xMaxColumnInRow = 0;
			for( int xElementInRow = 1; xElementInRow <= ctElementsInRow; xElementInRow++ ){
				FormElement element = (FormElement)listDefinedElements.get( aiElementInCurrentRow[xElementInRow] - 1 );
				if( element.miColumn > 0 ){
					ctSpecficiedColumns++;
					if( element.miColumn > xMaxColumnInRow ) xMaxColumnInRow = element.miColumn;
				}
			}

			// map duplicate items to end of row
			int ctExtraColumnsInRow = 0;
			int[] aiColumnsToElement = new int[xMaxColumnInRow + 1];
			for( int xElementInRow = 1; xElementInRow <= ctElementsInRow; xElementInRow++ ){
				int xElement = aiElementInCurrentRow[xElementInRow];
				FormElement element = (FormElement)listDefinedElements.get( xElement - 1 );
				if( element.miColumn > 0 ){
					if( aiColumnsToElement[element.miColumn] > 0 ){ // then this column already has an item in it
						ctExtraColumnsInRow++;
						aiColumnMapping[xElement] = ctExtraColumnsInRow;
					} else {
						aiColumnMapping[xElement] = element.miColumn;
						aiColumnsToElement[element.miColumn] = xElement;
					}
				}
			}

			// debugging
			for( int xColumn = 1; xColumn <= ctColumns; xColumn++ ){
System.out.println("element in column " + xColumn + " is " + aiElementInCurrentRow[xColumn]);
			}

			// if any column assignments are greater than ctColumns shift assignments left to fit within ctColumns
			while( true ){
				int xRightmostAssignment = iColumnMax;
				for( ; xRightmostAssignment > 0; xRightmostAssignment-- ){
					if( aiElementInCurrentRow[xRightmostAssignment] != 0 ) break; // found the rightmost assignment
				}
			}

			// map unspecified items to end of row
			for( int xElementInRow = 1; xElementInRow <= ctElementsInRow; xElementInRow++ ){
				int xElement = aiElementInCurrentRow[xElementInRow];
				FormElement element = (FormElement)listDefinedElements.get( xElement - 1 );
				if( element.miColumn > 0 ){
					// mapped in clause above
				} else {
					ctExtraColumnsInRow++;
					aiColumnMapping[xElement] = ctExtraColumnsInRow;
				}
			}

			if( ctSpecficiedColumns + ctExtraColumnsInRow > ctColumns ) ctColumns = ctSpecficiedColumns + ctExtraColumnsInRow;
System.out.println("==========================================");
System.out.println("row " + xRow + " column mappings complete:");
System.out.println(dumpArray(aiColumnMapping, 0, 0));
System.out.println("==========================================");

			// create the column mappings for this row
			for( int xColumn = 1; xColumn <= ctColumns; xColumn++ ){
				aiColumnMapping[aiElementInCurrentRow[xColumn]] = xColumn;
System.out.println("after shift element in column " + xColumn + " is " + aiElementInCurrentRow[xColumn]);
			}
System.out.println("row " + xRow + " column mappings complete");

		}

		// make the master array mapping
		// maps [column][row] to element index
		FormElement[][] aMapping = new FormElement[ ctColumns + 1 ][ ctRows + 1 ];
		for( int xElement = 1; xElement <= ctElement; xElement++ ){
			FormElement element = (FormElement)listDefinedElements.get( xElement - 1 );
			aMapping[aiColumnMapping[xElement]][aiMapping_Element2Row[xElement]] = element;
System.out.println("element " + xElement + " " + element + " mapped to " + aiColumnMapping[xElement] + " " + aiRowMapping[xElement]);
		}
System.out.println("master array mapping complete");
vDumpMapping( aMapping, ctColumns, ctRows );

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
		int[] apxWidth_padding_row = new int[ ctRows + 1 ];
		int[] apxWidth_preferred_row = new int[ ctRows + 1 ];
		int[] apxWidth_minimum_row = new int[ ctRows + 1 ];
		for( int xRow = 1; xRow <= ctRows; xRow++ ){
			int pxMinimumWidth_CurrentRow = 0;
			int pxPreferredWidth_CurrentRow = 0;
			int pxMaximumWidth_CurrentRow = 0;
			int pxPadding_CurrentRow = 0;
			int ctElementsInThisRow = 0;
System.out.println("\n**********\nrow " + xRow + " has " + ctElementsInThisRow + " elements");
			for( int xColumn = 1; xColumn <= ctColumns; xColumn++ ){
				FormElement element = aMapping[xColumn][xRow];
System.out.println("element: " + element);
				if( element == null ){
					element.iBounds_minimum_width = 0;
					element.iBounds_preferred_width = 0;
					element.iBounds_maximum_width = 0;
					continue;
				}
				ctElementsInThisRow++;
				int pxPadding_CurrentElement = element.miSpacing_indent
												+ ( element.componentLabel != null && element.componentControl != null ? element.miSpacing_separation : 0 )
												+ element.miSpacing_trailing;
				pxPadding_CurrentRow += pxPadding_CurrentElement;

				// minimum
				int pxMinimumWidth_Element =    pxPadding_CurrentElement
												+ ( element.componentLabel != null ? (int)element.componentLabel.getMinimumSize().getWidth() : 0 )
												+ ( element.componentControl != null ? (int)element.componentControl.getMinimumSize().getWidth() : 0 );
				if( apxColumnMinimumWidth[xColumn] > pxMinimumWidth_Element ) pxMinimumWidth_Element = apxColumnMinimumWidth[xColumn];
				element.iBounds_minimum_width = pxMinimumWidth_Element;
				pxMinimumWidth_CurrentRow += pxMinimumWidth_Element;

				// preferred
				int pxPreferredWidth_Element =  pxPadding_CurrentElement
												+ ( element.componentLabel != null ? (int)element.componentLabel.getPreferredSize().getWidth() : 0 )
												+ ( element.componentControl != null ? (int)element.componentControl.getPreferredSize().getWidth() : 0 );
				if( pxPreferredWidth_Element < element.iBounds_minimum_width ) pxPreferredWidth_Element = element.iBounds_minimum_width;
				element.iBounds_preferred_width = pxPreferredWidth_Element;
				pxPreferredWidth_CurrentRow += pxPreferredWidth_Element;
System.out.println("label: " + xColumn + " " + xRow + " preferred size: " + element.componentLabel.getPreferredSize() + "row preferred width:" + pxPreferredWidth_CurrentRow);

				// maximum
				int pxMaximumWidth_Element =	pxPadding_CurrentElement
												+ ( element.componentLabel != null ? (int)element.componentLabel.getMaximumSize().getWidth() : 0 )
												+ ( element.componentControl != null ? (int)element.componentControl.getMaximumSize().getWidth() : 0 );
				if( pxMaximumWidth_Element < element.iBounds_minimum_width ) pxMaximumWidth_Element = element.iBounds_minimum_width;
				element.iBounds_maximum_width = pxMaximumWidth_Element;
				pxMaximumWidth_CurrentRow += pxMaximumWidth_Element;

			}
			pxPreferredWidth_CurrentRow += MARGIN_left + MARGIN_right;
			pxMinimumWidth_CurrentRow += MARGIN_left + MARGIN_right;
			pxPadding_CurrentRow += MARGIN_left + MARGIN_right;
			apxWidth_preferred_row[xRow] = pxPreferredWidth_CurrentRow;
			apxWidth_minimum_row[xRow] = pxMinimumWidth_CurrentRow;
			apxWidth_padding_row[xRow] = pxPadding_CurrentRow;
			if( pxMinimumWidth_CurrentRow > pxMinimumWidth_form ) pxMinimumWidth_form = pxMinimumWidth_CurrentRow;
			if( pxPreferredWidth_CurrentRow > pxPreferredWidth_form ) pxPreferredWidth_form = pxPreferredWidth_CurrentRow;
			if( pxMaximumWidth_CurrentRow > pxMaximumWidth_form ) pxMaximumWidth_form = pxMaximumWidth_CurrentRow;

System.out.println("row " + xRow + " element ct: " + ctElementsInThisRow + " preferred width: " + pxPreferredWidth_CurrentRow + " minimum: " + pxMinimumWidth_CurrentRow + " max: " + pxMaximumWidth_CurrentRow );
		}
System.out.println("guide sizes determined");

		// adjust the guide sizes to account for alignments
		// TODO
System.out.println("setting preferred width: " + pxPreferredWidth_form );
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
//			int pxMinimumWidth_CurrentRow = apxWidth_minimum_row[xRow];
System.out.println("form width: " + pxFormWidth + " preferred width of row " + xRow + ": " + pxPreferredWidth_CurrentRow);
			int pxFillWidth_CurrentRow = pxFormWidth - pxPreferredWidth_CurrentRow;
			if( pxFillWidth_CurrentRow < 0 ) pxFillWidth_CurrentRow = 0;

			FormElement elementLastInRow = null;
			int psTotalWidthUsed = apxWidth_padding_row[xRow];
			if( pxFillWidth_CurrentRow == 0 ){ // there is no extra space in row, all elements will be scaled down from their preferred size
				int iScale_numerator = pxPreferredWidth_CurrentRow - apxWidth_padding_row[xRow];
				int iScale_divisor = pxFormWidth - apxWidth_padding_row[xRow];
				for( int xColumn = 1; xColumn <= ctColumns; xColumn++ ){
					FormElement element = aMapping[xColumn][xRow];
					if( element == null ) continue;
					if( element.componentLabel != null ) element.iBounds_label_width = element.componentLabel.getMinimumSize().width * iScale_numerator / iScale_divisor;
System.out.println("label width no extra space: " + element.iBounds_label_width + " minimum: " + element.componentLabel.getMinimumSize().width + " scale: " + iScale_numerator + " " + iScale_divisor );
					if( element.componentControl != null ) element.iBounds_control_width = element.componentControl.getMinimumSize().width * iScale_numerator / iScale_divisor;
					psTotalWidthUsed += element.iBounds_label_width + element.iBounds_control_width;
					elementLastInRow = element;
				}

			} else { // non-fill items will be preferred size and fill items will stretch

				// labels are their preferred sizes
				for( int xColumn = 1; xColumn <= ctColumns; xColumn++ ){
					FormElement element = aMapping[xColumn][xRow];
					if( element == null ) continue;
					if( element.componentLabel != null ) element.iBounds_label_width = element.componentLabel.getPreferredSize().width;
System.out.println("label width preferred: " + element.iBounds_label_width);
					psTotalWidthUsed += element.iBounds_label_width;
				}

				// non-fill control items are their preferred sizes
				for( int xColumn = 1; xColumn <= ctColumns; xColumn++ ){
					FormElement element = aMapping[xColumn][xRow];
					if( element == null ) continue;
					if( ! element.mzFill && element.componentControl != null ){
						element.iBounds_control_width = element.componentControl.getPreferredSize().width;
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

System.out.println( "ctRows: " + ctRows + " ctColumns: " + ctColumns );

		// determine the component guide sizes ( heights )
		int[] apxHeight_minimum_row = new int[ ctRows + 1 ];
		int[] apxHeight_preferred_row = new int[ ctRows + 1 ];
		int[] apxHeight_maximum_row = new int[ ctRows + 1 ];
		for( int xRow = 1; xRow <= ctRows; xRow++ ){
			int pxMinimumHeight_CurrentRow = 0;
			int pxPreferredHeight_CurrentRow = 0;
			int pxMaximumHeight_CurrentRow= 0;
			for( int xColumn = 1; xColumn <= ctColumns; xColumn++ ){
				FormElement element = aMapping[xColumn][xRow];
				if( element == null ) continue;

				// minimum
				int pxMinimumHeight_Element_label =    element.miSpacing_line_above + element.miSpacing_line_below
												+ ( element.componentLabel != null ? (int)element.componentLabel.getMinimumSize().getHeight() : 0 );
				int pxMinimumHeight_Element_control =    element.miSpacing_line_above + element.miSpacing_line_below
												+ ( element.componentControl != null ? (int)element.componentControl.getMinimumSize().getHeight() : 0 );
				if( pxMinimumHeight_Element_label > pxMinimumHeight_CurrentRow ) pxMinimumHeight_CurrentRow = pxMinimumHeight_Element_label;
				if( pxMinimumHeight_Element_control > pxMinimumHeight_CurrentRow ) pxMinimumHeight_CurrentRow = pxMinimumHeight_Element_control;

				// preferred
				int pxPreferredHeight_Element_label =   element.miSpacing_line_above + element.miSpacing_line_below
												+ ( element.componentLabel != null ? (int)element.componentLabel.getPreferredSize().getHeight() : 0 );
				int pxPreferredHeight_Element_control =   element.miSpacing_line_above + element.miSpacing_line_below
												+ ( element.componentControl != null ? (int)element.componentControl.getPreferredSize().getHeight() : 0 );
				if( pxPreferredHeight_Element_label > pxPreferredHeight_CurrentRow ) pxPreferredHeight_CurrentRow = pxPreferredHeight_Element_label;
				if( pxPreferredHeight_Element_control > pxPreferredHeight_CurrentRow ) pxPreferredHeight_CurrentRow = pxPreferredHeight_Element_control;
System.out.println("preferred height of current row: " + pxPreferredHeight_CurrentRow );

				// maximum
				int pxMaximumHeight_Element_label =   element.miSpacing_line_above + element.miSpacing_line_below
												+ ( element.componentLabel != null ? (int)element.componentLabel.getMaximumSize().getHeight() : 0 );
				int pxMaximumHeight_Element_control =   element.miSpacing_line_above + element.miSpacing_line_below
												+ ( element.componentControl != null ? (int)element.componentControl.getMaximumSize().getHeight() : 0 );
				if( pxMaximumHeight_Element_label > pxMaximumHeight_CurrentRow ) pxMaximumHeight_CurrentRow = pxMaximumHeight_Element_label;
				if( pxMaximumHeight_Element_control > pxMaximumHeight_CurrentRow ) pxMaximumHeight_CurrentRow = pxMaximumHeight_Element_control;

			}
			apxHeight_minimum_row[xRow] = pxMinimumHeight_CurrentRow;
			apxHeight_preferred_row[xRow] = pxPreferredHeight_CurrentRow;
			apxHeight_maximum_row[xRow] = pxMaximumHeight_CurrentRow;
		}

		int pxMinimumHeight_form = 0;
		int pxPreferredHeight_form = 0;
		int pxMaximumHeight_form = 0;
		for( int xRow = 1; xRow <= ctRows; xRow++ ){
			pxMinimumHeight_form += apxHeight_minimum_row[xRow];
			pxPreferredHeight_form += apxHeight_preferred_row[xRow];
			pxMaximumHeight_form += apxHeight_maximum_row[xRow];
		}
		pxMinimumHeight_form += MARGIN_top + MARGIN_bottom;
		pxPreferredHeight_form += MARGIN_top + MARGIN_bottom;
		pxMaximumHeight_form += MARGIN_top + MARGIN_bottom;
		mpxPreferredHeight = pxPreferredHeight_form;
System.out.println("************************ setting preferred height to: " + mpxPreferredHeight);

		// determine the height of each row
		int[] apxHeight_row = new int[ ctRows + 1 ];
		if( pxPreferredHeight_form <= heightCanvas ){ // stay at the preferred height
			System.arraycopy( apxHeight_preferred_row, 0, apxHeight_row, 0, apxHeight_row.length );
		} else if( pxMinimumHeight_form <= heightCanvas ){ // scale up from the minimum to the canvas
			for( int xRow = 1; xRow <= ctRows; xRow++ ){
				int iScale_numerator = heightCanvas;
				int iScale_divisor = pxMinimumHeight_form;
				apxHeight_row[xRow] = apxHeight_minimum_row[xRow] * iScale_numerator / iScale_divisor;
			}
		} else { // everything is at minimum height
			System.arraycopy( apxHeight_minimum_row, 0, apxHeight_row, 0, apxHeight_row.length );
		}

		// determine the element heights
		for( int xElement = 1; xElement <= ctElement; xElement++ ){
			FormElement element = (FormElement)listDefinedElements.get( xElement - 1 );
			int xRow = aiMapping_Element2Row[xElement];
			int pxRowHeight = apxHeight_row[xRow];
			if( element.componentControl != null ){
				int pxControlPreferredHeight = element.componentControl.getPreferredSize().height + element.miSpacing_line_above + element.miSpacing_line_below;
				if( pxControlPreferredHeight <= pxRowHeight ){
					element.iBounds_control_height = element.componentControl.getPreferredSize().height;
				} else {
					element.iBounds_control_height = pxRowHeight - element.miSpacing_line_above - element.miSpacing_line_below;
				}
			}
			if( element.componentLabel != null ){
				int pxLabelPreferredHeight = element.componentLabel.getPreferredSize().height + element.miSpacing_line_above + element.miSpacing_line_below;
				if( pxLabelPreferredHeight <= pxRowHeight ){
					element.iBounds_label_height = element.componentLabel.getPreferredSize().height;
				} else {
					element.iBounds_label_height = pxRowHeight - element.miSpacing_line_above - element.miSpacing_line_below;
				}
			}
		}

		// determine element widths
//		for( int xRow = 1; xRow <= ctRows; xRow++ ){
//			for( int xColumn = 1; xColumn <= ctColumns; xColumn++ ){
//				FormElement element = aMapping[xColumn][xRow];
//				if( element == null ) continue;
//
//			}
//		}

		// determine element locations
		int px_x = 0;
		int px_y = 0;
		px_y = insetsContainer.top + MARGIN_top;
		for( int xRow = 1; xRow <= ctRows; xRow++ ){
			px_x = insetsContainer.left + MARGIN_left;
			for( int xColumn = 1; xColumn <= ctColumns; xColumn++ ){
				FormElement element = aMapping[xColumn][xRow];
				if( element == null ) continue;
				if( element.componentLabel != null ){
					element.iBounds_label_y = px_y + element.miSpacing_line_above;
					element.iBounds_label_x = px_x + element.miSpacing_indent;
System.out.println("label x y: " + element.iBounds_label_x + " " + element.iBounds_label_y + " width: " + element.iBounds_label_width );
					px_x += element.miSpacing_indent + element.iBounds_label_width + element.miSpacing_separation;
				}
				if( element.componentControl != null ){
					element.iBounds_control_y = px_y + element.miSpacing_line_above;
					element.iBounds_control_x = px_x;
System.out.println("control x y: " + element.iBounds_control_x + " " + element.iBounds_control_y );
					px_x += element.iBounds_control_width + element.miSpacing_trailing;
				}
			}
			px_y += apxHeight_row[xRow];
		}

		// set the component bounds
		for( int xElement = 1; xElement <= ctElement; xElement++ ){
			FormElement element = (FormElement)listDefinedElements.get( xElement - 1 );
System.out.println("element: " + xElement + " label x: " + element.iBounds_label_x + " label y: " + element.iBounds_label_y + " label width: " + element.iBounds_label_width + " label height: " + element.iBounds_label_height );
			element.componentLabel.setBounds( element.iBounds_label_x, element.iBounds_label_y, element.iBounds_label_width, element.iBounds_label_height );
System.out.println("control: " + xElement + " control x: " + element.iBounds_control_x + " control y: " + element.iBounds_control_y + " control width: " + element.iBounds_control_width + " control height: " + element.iBounds_control_height );
			element.componentControl.setBounds( element.iBounds_control_x, element.iBounds_control_y, element.iBounds_control_width, element.iBounds_control_height );
			if( element.componentLabel != null ) element.componentLabel.setBounds( element.iBounds_label_x, element.iBounds_label_y, element.iBounds_label_width, element.iBounds_label_height );
			if( element.componentControl != null ) element.componentControl.setBounds( element.iBounds_control_x, element.iBounds_control_y, element.iBounds_control_width, element.iBounds_control_height );
		}
	}

	// synchronizes the listDefinedElements data structure with the components in the container
	private void layout_1_IdentifyElements(){
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
	}

	public void invalidateLayout( Container ignored ){}
	public float getLayoutAlignmentX( Container ignored ){ return 0; }
	public float getLayoutAlignmentY( Container ignored ){ return 0; }
	public Dimension preferredLayoutSize( Container parent ){
		Insets insets;
		Dimension dimPreferred;
		if( parent == null ){
			dimPreferred = new Dimension( 0, 0 );
		} else {
			if( mpxPreferredWidth == 0 ) layoutContainer( parent ); // if the pref size is requested before the layout has been done for the first time, do it
			insets = parent.getInsets();
			int pxInsetsWidth = insets.left + insets.right;
			int pxInsetsHeigh = insets.top + insets.bottom;
			dimPreferred = new Dimension( mpxPreferredWidth + pxInsetsWidth, mpxPreferredHeight + pxInsetsHeigh );
		}
System.out.println("returning preferred size: " + dimPreferred );
		return dimPreferred;
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
		DEFAULT_SPACING_trailing = ( trailing == USE_DEFAULT ) ? 2 : trailing;
		DEFAULT_SPACING_above = ( above == USE_DEFAULT ) ? 0 : above;
		DEFAULT_SPACING_below = ( below == USE_DEFAULT ) ? 6 : below;
	}
	public void setSpacing( Component item, int indent, int separation, int trailing, int above, int below ){
		FormElement element = getFormElement( item );
		if( element == null ) return;
		element.miSpacing_indent = ( indent == USE_DEFAULT ) ? 4 : indent;
		element.miSpacing_separation = ( separation == USE_DEFAULT ) ? 3 : separation;
		element.miSpacing_trailing = ( trailing == USE_DEFAULT ) ? 2 : trailing;
		element.miSpacing_line_above = ( above == USE_DEFAULT ) ? 0 : above;
		element.miSpacing_line_below = ( below == USE_DEFAULT ) ? 6 : below;
	}

	/** element will be added at the last row of column 1 */
	public void add( Component label, Component control ){
		add( label, control, true );
	}

	/** element will be added at the last row of column 1 */
	private void add( Component label, Component control, boolean zReorder ){
		FormElement element = getFormElement( label, control );
		if( element == null ){
			if( label == null && control == null ) return;
			element = new FormElement( this );
			element.componentLabel = label;
			element.componentControl = control;
			listDefinedElements.add( element ); // add element at end
		} else {
			if( zReorder ){
				listDefinedElements.remove( element );
				listDefinedElements.add( element ); // add element at end
			}
		}
	}

	/** one-based row number
	 *  element will be added to the last available column of the chosen row */
	public void add( Component label, Component control, int row ){
		FormElement element = getFormElement( label, control );
		if( element == null ){
			if( label == null && control == null ) return;
			element = new FormElement( this );
			element.componentLabel = label;
			element.componentControl = control;
		} else {
			listDefinedElements.remove( element );
		}
		listDefinedElements.add( element ); // add element at end
		if( row > 0 ) element.miRow = row;
	}

	/** one-based row/column number
	 *  if an element already occupies this row/column
	 *  then it will be added as an additional column in this row */
	public void add( Component label, Component control, int row, int column ){
		FormElement element = getFormElement( label, control );
		if( element == null ){
			if( label == null && control == null ) return;
			element = new FormElement( this );
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

	void vDumpMapping( FormElement[][] aMapping, int ctColumns, int ctRows ){
		for( int xRow = 1; xRow <= ctRows; xRow++ ){
			for( int xColumn = 1; xColumn <= ctColumns; xColumn++ ){
				System.out.print("[" + xColumn + "][" + xRow + "]");
				FormElement fe = aMapping[xColumn][xRow];
				if( fe == null ){
					System.out.println(" is null");
				} else {
					System.out.print(" " + aMapping[xColumn][xRow].componentControl);
					System.out.println(" " + aMapping[xColumn][xRow].componentLabel);
				}
			}
		}
	}

	public static String dumpArray( int[] ai, int from, int to ){
		if( ai == null ) return "[null]";
		if( to == 0 ) to = ai.length - 1;
		StringBuffer sb = new StringBuffer(80);
		for( int x = from; x <= to; x++ ){
			sb.append("[" + x + "] = " + ai[x] + "\n");
		}
		return sb.toString();
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

	public FormElement( FormLayout layout ){
		miSpacing_indent = layout.DEFAULT_SPACING_indent;
		miSpacing_separation = layout.DEFAULT_SPACING_separation;
		miSpacing_trailing = layout.DEFAULT_SPACING_trailing;
		miSpacing_line_above = layout.DEFAULT_SPACING_above;
		miSpacing_line_below = layout.DEFAULT_SPACING_below;
	}
}

class FormTestPanel extends JPanel {
	JPanel panelControls = new JPanel();
	JPanel panelDisplay = new JPanel();
	public static void main( String[] args )
	{
		FormTestPanel test_panel = new FormTestPanel();
		JFrame frame = new JFrame();

		java.awt.event.WindowListener listenerCloser = new java.awt.event.WindowAdapter(){
			public void windowClosing( java.awt.event.WindowEvent e ){
				System.exit(0);
			}
		};
		frame.addWindowListener( listenerCloser );

		frame.add(test_panel);
		frame.pack();
		frame.setVisible( true );
	}
	FormTestPanel(){

//	for( int a = 1; a < 50; a++ ){
//		for( int b = a + 1; b < 51; b++ ){
//			for( int c = 1; c < 700; c++ ){
//				int a = 1;
//				int b = 13;
//				int c = 9;
//FindAnswer:
//				int ctSolutions = 0;
//				for( int x = -200; x < 200; x++ ){
//					for( int y = -200; y < 200; y++ ){
//						if( a*x + b*y == c ){
//							if( x + y < 80 ) break FindAnswer;
//							if( x > 0 && y > 0 ){
//								System.out.println("answer, a = " + a + " b = " + b + " c = " + c + " x = " + x + " y = " + y + " x+y = " + (x+y) );
//								ctSolutions++;
//							}
//							break FindAnswer;
//						}
//					}
//				}
//				System.out.println( "c: " + c + " number of solutions: " +  ctSolutions );
//			}
//		}
//	}

//		long nStart = System.currentTimeMillis();
//		double dAnswer = 0, dQuotient;
//		dQuotient = 4.123d;
//		double d1 = 5.6;
//		double d2 = 6.5;
//		for( int x = 1; x < 100000000; x++ ){
//			dAnswer = Math.sqrt(dQuotient);
//			dAnswer = d1 * d2;
//		}
//		System.out.println("total time: " + (System.currentTimeMillis() - nStart) );
//		if( true ) System.exit(0);

		this.setBorder( BorderFactory.createLineBorder( Color.YELLOW));
		panelControls.add( new JLabel("control panel") );

		panelDisplay.setLayout( new FormLayout(panelDisplay) );

		panelDisplay = this;
		JLabel label1 = new JLabel("label 1");
System.out.println("label 1 preferred size: " + label1.getPreferredSize().getWidth() );

		label1.setBorder( BorderFactory.createLineBorder(Color.BLUE ));
		FormLayout layout = new FormLayout( panelDisplay );
		panelDisplay.setLayout( layout );
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

//		layout.setSpacing_Default( 0, 5, 0, 0, 10 );
//		layout.setMargin( 30, 20, 25, 25 );

//		this.setLayout( new java.awt.BorderLayout() );
//		this.add( panelControls, java.awt.BorderLayout.NORTH );
//		this.add( panelDisplay, java.awt.BorderLayout.CENTER );

	}

	void vDumpComponentSizing( Component c ){
		System.out.println( "minimum: " + c.getMinimumSize() );
		System.out.println( "maximum: " + c.getMaximumSize() );
		System.out.println( "preferred: " + c.getPreferredSize() );
	}

}
