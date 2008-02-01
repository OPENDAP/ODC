package opendap.clients.odc;

/**
 * Title:        StreamForwarder
 * Description:  Forwards an output stream to any number of target output streams
 * Copyright:    Copyright (c) 2002
 * Company:      University of Rhode Island, Graduate School of Oceanography
 * @author       John Chamberlain
 * @version      2.0
 */

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

import java.io.*;

class StreamForwarder extends FilterOutputStream {

	private int DEFAULT_CAPACITY = 10;
	private OutputStream[] maOutputStreams;
	private int mctOutputStreams;

	public StreamForwarder(OutputStream newout) {
		super(newout);
		maOutputStreams = new OutputStream[DEFAULT_CAPACITY + 1];
		mctOutputStreams = DEFAULT_CAPACITY;
	}

	public boolean add(OutputStream outputstreamNew, StringBuffer sbError) {
		try {
			for (int xOutputStream = 1; xOutputStream <= mctOutputStreams; xOutputStream++) {
				if (maOutputStreams[xOutputStream] == outputstreamNew) { // already in there
					return true;
				}
			}
			for (int xOutputStream = 1; xOutputStream <= mctOutputStreams; xOutputStream++) {
				if (maOutputStreams[xOutputStream] == null) {
					maOutputStreams[xOutputStream] = outputstreamNew;
					return true;
				}
			}

			// in this case there are no free slots--we need to expand the array
			OutputStream[] maNewOutputStreamArray = new OutputStream[mctOutputStreams*2 + 1];
			for (int xOutputStream = 1; xOutputStream <= mctOutputStreams; xOutputStream++) {
				maNewOutputStreamArray[xOutputStream] = maOutputStreams[xOutputStream];
			}
			maOutputStreams = maNewOutputStreamArray;
			maOutputStreams[mctOutputStreams+1] = outputstreamNew;
			mctOutputStreams = mctOutputStreams*2;
			return true;
		} catch(Exception ex) {
			ApplicationController.vUnexpectedError(ex, sbError);
			return false;
		}
	}

	public synchronized void remove(OutputStream outputstreamToRemove) {
		for (int xOutputStream = 1; xOutputStream <= mctOutputStreams; xOutputStream++) {
			if (maOutputStreams[xOutputStream] == outputstreamToRemove) maOutputStreams[xOutputStream] = null;
		}
	}

	public synchronized void write(int b) throws IOException {
		for (int xOutputStream = 1; xOutputStream <= mctOutputStreams; xOutputStream++) {
			if (maOutputStreams[xOutputStream] != null)	{
				maOutputStreams[xOutputStream].write(b);
			}
		}
	}

	public synchronized void write(byte[] data) throws IOException {
		for (int xOutputStream = 1; xOutputStream <= mctOutputStreams; xOutputStream++) {
			if (maOutputStreams[xOutputStream] != null) {
				maOutputStreams[xOutputStream].write(data);
			}
		}
	}

	public synchronized void write(byte[] data, int offset, int length) throws IOException {
		String s = new String(data, offset, length); // todo use writers
		for (int xOutputStream = 1; xOutputStream <= mctOutputStreams; xOutputStream++) {
			if (maOutputStreams[xOutputStream] != null) {
				maOutputStreams[xOutputStream].write(data, offset, length);
			}
		}
	}

	public synchronized void flush() throws IOException {
		for (int xOutputStream = 1; xOutputStream <= mctOutputStreams; xOutputStream++) {
			if (maOutputStreams[xOutputStream] != null) maOutputStreams[xOutputStream].flush();
		}
	}

	public synchronized void close() throws IOException {
		for (int xOutputStream = 1; xOutputStream <= mctOutputStreams; xOutputStream++) {
			if (maOutputStreams[xOutputStream] != null) maOutputStreams[xOutputStream].close();
		}
	}
}


