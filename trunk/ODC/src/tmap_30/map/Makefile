JAVAC = javac

JAVAFLAGS = -classpath ${CLASSPATH}

MAPCLASSES = GridBagConstraints2.class MapConstants.class \
	MapRegion.class MapTool.class MapCanvas.class  \
	MapConstants.class MapGrid.class MaxZoomException.class \
	MinZoomException.class PointRegion.class ToolHandle.class \
	PTTool.class PTcXTool.class PTcXYTool.class PTcYTool.class \
	XTool.class XYTool.class XcYTool.class YTool.class YcXTool.class

all: $(MAPCLASSES)

clean:
	rm -f *.class
	rm -f *~

distclean: clean

.SUFFIXES: .java .class

.java.class:
	$(JAVAC) $(JAVAFLAGS) $<

