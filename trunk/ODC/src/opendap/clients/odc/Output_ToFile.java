package opendap.clients.odc;

/**
 * Title:        Output_ToFile
 * Description:  Methods to generate output
 * Copyright:    Copyright (c) 2002
 * Company:      University of Rhode Island, Graduate School of Oceanography
 * @author       John Chamberlain
 * @version      3.00
 */

import opendap.dap.*;
import java.io.*;
import java.nio.*;
import java.nio.channels.*;

public class Output_ToFile {
	Output_ToFile(){}
}

class Output_NetCDF1 {

	public final static int NC_BYTE      = 1;         // data is array of 8 bit signed integer
	public final static int NC_CHAR      = 2;         // data is array of characters, i.e., text
	public final static int NC_SHORT     = 3;         // data is array of 16 bit signed integer
	public final static int NC_LONG      = 4;         // data is array of 32 bit signed integer
	public final static int NC_FLOAT     = 5;         // data is array of IEEE single precision float
	public final static int NC_DOUBLE    = 6;         // data is array of IEEE double precision float
	public final static int NC_DIMENSION = 10;
	public final static int NC_VARIABLE  = 11;
	public final static int NC_ATTRIBUTE = 12;

	public final static int NC_ZERO      = 0;

	public final static boolean zWriteNetCDF_1( DataDDS ddds, String sPath, StringBuffer sbError ){

		// acquire channel
		FileChannel fc;
		try {
			File file = new File(sPath);
			FileOutputStream fos = new FileOutputStream(file);
			fc = fos.getChannel();
		} catch(Exception ex) {
			sbError.append("Failed to acquire file channel for " + sPath + ": " + ex);
			return false;
		}

		// determine number of records
		int ctRecords = 0;

		// construct header
		try {
			ByteBuffer bb_Header = ByteBuffer.allocateDirect(4 + 4 + 0);
			bb_Header.put((byte)'C');
			bb_Header.put((byte)'D');
			bb_Header.put((byte)'F');
			bb_Header.put((byte) 1 );
			bb_Header.putInt(ctRecords);
			if( ctRecords == 0 ){
				bb_Header.putInt(NC_ZERO);
				bb_Header.putInt(NC_ZERO);
			} else {
				for( int xRecord = 1; xRecord <= ctRecords; xRecord++ ){
					int ctDimension = 0;
					if( ctDimension > 0 ){ // no dimension record
						bb_Header.putInt(NC_ZERO);
						bb_Header.putInt(NC_ZERO);
					} else {
						bb_Header.putInt(NC_DIMENSION);
						bb_Header.putInt(ctDimension);
						for( int xDimension = 1; xDimension <= ctDimension; ctDimension++ ){
							String sName = "";
							byte[] abName = new byte[sName.length()];
							// utility function to convert string to ascii bytes (not double chars)
							bb_Header.put(abName);
							bb_Header.putInt(ctDimension);
						}
					}
				}
			}
		} catch(Exception ex) {
		}

		return true;
	}
/**
netcdf_file := header  data
header  := magic  numrecs  dim_array  gatt_array  var_array

magic   := 'C'  'D'  'F'  VERSION_BYTE

VERSION_BYTE := '\001'    // the file format version number

numrecs    := NON_NEG

dim_array  :=  ABSENT | NC_DIMENSION  nelems  [dim ...]

gatt_array :=  att_array  // global attributes

att_array  :=  ABSENT | NC_ATTRIBUTE  nelems  [attr ...]

var_array  :=  ABSENT | NC_VARIABLE   nelems  [var ...]

ABSENT  := ZERO  ZERO     // Means array not present (equivalent to
                          // nelems == 0).

nelems  := NON_NEG        // number of elements in following sequence

dim     := name  dim_size

name    := string

dim_size := NON_NEG       // If zero, this is the record dimension.
                          // There can be at most one record dimension.

attr    := name  nc_type  nelems  [values]

nc_type := NC_BYTE | NC_CHAR | NC_SHORT | NC_LONG | NC_FLOAT | NC_DOUBLE

var     := name  nelems  [dimid ...]  vatt_array  nc_type  vsize  begin
                          // nelems is the rank (dimensionality) of the
                          // variable; 0 for scalar, 1 for vector, 2 for
                          // matrix, ...

vatt_array :=  att_array  // variable-specific attributes

dimid   := NON_NEG        // Dimension ID (index into dim_array) for
                          // variable shape.  We say this is a "record
                          // variable" if and only if the first
                          // dimension is the record dimension.

vsize    := NON_NEG       // Variable size.  If not a record variable,
                          // the amount of space, in bytes, allocated to
                          // that variable's data.  This number is the
                          // product of the dimension sizes times the
                          // size of the type, padded to a four byte
                          // boundary.  If a record variable, it is the
                          // amount of space per record.  The netCDF
                          // "record size" is calculated as the sum of
                          // the vsize's of the record variables.

begin   := NON_NEG        // Variable start location.  The offset in
                          // bytes (seek index) in the file of the
                          // beginning of data for this variable.

data    := non_recs  recs

non_recs := [values ...]  // Data for first non-record var, second
                          // non-record var, ...

recs    := [rec ...]      // First record, second record, ...

rec     := [values ...]   // Data for first record variable for record
                          // n, second record variable for record n, ...
                          // See the note below for a special case.

values  := [bytes] | [chars] | [shorts] | [ints] | [floats] | [doubles]

string  := nelems  [chars]

bytes   := [BYTE ...]  padding

chars   := [CHAR ...]  padding

shorts  := [SHORT ...]  padding

ints    := [INT ...]

floats  := [FLOAT ...]

doubles := [DOUBLE ...]

padding := <0, 1, 2, or 3 bytes to next 4-byte boundary>
                          // In header, padding is with 0 bytes.  In
                          // data, padding is with variable's fill-value.

NON_NEG := <INT with non-negative value>

ZERO    := <INT with zero value>

BYTE    := <8-bit byte>

CHAR    := <8-bit ACSII/ISO encoded character>

SHORT   := <16-bit signed integer, Bigendian, two's complement>

INT     := <32-bit signed integer, Bigendian, two's complement>

FLOAT   := <32-bit IEEE single-precision float, Bigendian>

DOUBLE  := <64-bit IEEE double-precision float, Bigendian>
*/
}

class Output_HDF4 {
	public boolean zWriteHDF4( DataDDS ddds, OutputStream os, StringBuffer sbError ){
		byte[] abFileSignature = new byte[4];
		abFileSignature[0] = 0x0E; // ctrl-N
		abFileSignature[1] = 0x03; // ctrl-C
		abFileSignature[2] = 0x13; // ctrl-S
		abFileSignature[3] = 0x01; // ctrl-A
		try {
			os.write(abFileSignature);
		} catch(Throwable th) {
			sbError.append("Error writing: " + th);
			return false;
		}
		return true;
	}

	class HDF4_DataDescriptor {
		public static final int DFTAG_NULL    = 0x0001;
		public static final int DFTAG_VERSION = 0x001E;
		public static final int DFTAG_NT      = 0x006A; // number type
		public static final int DFTAG_MT      = 0x006B; // machine type
		public static final int DFTAG_FID     = 0x0064; // file identifier
		public static final int DFTAG_FD      = 0x0065; // file description
		public static final int DFTAG_DIL     = 0x0068; // data identifier label
		public static final int DFTAG_DIA     = 0x0069; // data identifier annotation
		public static final int DFTAG_SDD     = 0x02BD; // scientific data dimension
		public static final int DFTAG_SD      = 0x02BE; // scientific data
		public static final int DFTAG_SDL     = 0x02C0; // scientific data label
		public static final int DFTAG_SDU     = 0x02C1; // scientific data units
		public static final int DFTAG_FV      = 0x02DC; // fill value (missing value)
		short shortTag;
		short shortReference;
		int miOffset;
		int miLength;
		byte[] getOutput(){
			byte[] abOutput = new byte[12];
			abOutput[0] = (byte)((shortTag & 0xFF00) >> 4 );
			abOutput[1] = (byte)(shortTag & 0x00FF);
			abOutput[2] = (byte)((shortReference & 0xFF00) >> 4 );
			abOutput[3] = (byte)(shortReference & 0x00FF);
			return abOutput;
		}
	}
}

/**
NetCDF 1.0 Specification

netcdf_file := header  data

header  := magic  numrecs  dim_array  gatt_array  var_array

magic   := 'C'  'D'  'F'  VERSION_BYTE

VERSION_BYTE := '\001'    // the file format version number

numrecs    := NON_NEG

dim_array  :=  ABSENT | NC_DIMENSION  nelems  [dim ...]

gatt_array :=  att_array  // global attributes

att_array  :=  ABSENT | NC_ATTRIBUTE  nelems  [attr ...]

var_array  :=  ABSENT | NC_VARIABLE   nelems  [var ...]

ABSENT  := ZERO  ZERO     // Means array not present (equivalent to
                          // nelems == 0).

nelems  := NON_NEG        // number of elements in following sequence

dim     := name  dim_size

name    := string

dim_size := NON_NEG       // If zero, this is the record dimension.
                          // There can be at most one record dimension.

attr    := name  nc_type  nelems  [values]

nc_type := NC_BYTE | NC_CHAR | NC_SHORT | NC_LONG | NC_FLOAT | NC_DOUBLE

var     := name  nelems  [dimid ...]  vatt_array  nc_type  vsize  begin
                          // nelems is the rank (dimensionality) of the
                          // variable; 0 for scalar, 1 for vector, 2 for
                          // matrix, ...

vatt_array :=  att_array  // variable-specific attributes

dimid   := NON_NEG        // Dimension ID (index into dim_array) for
                          // variable shape.  We say this is a "record
                          // variable" if and only if the first
                          // dimension is the record dimension.

vsize    := NON_NEG       // Variable size.  If not a record variable,
                          // the amount of space, in bytes, allocated to
                          // that variable's data.  This number is the
                          // product of the dimension sizes times the
                          // size of the type, padded to a four byte
                          // boundary.  If a record variable, it is the
                          // amount of space per record.  The netCDF
                          // "record size" is calculated as the sum of
                          // the vsize's of the record variables.

begin   := NON_NEG        // Variable start location.  The offset in
                          // bytes (seek index) in the file of the
                          // beginning of data for this variable.

data    := non_recs  recs

non_recs := [values ...]  // Data for first non-record var, second
                          // non-record var, ...

recs    := [rec ...]      // First record, second record, ...

rec     := [values ...]   // Data for first record variable for record
                          // n, second record variable for record n, ...
                          // See the note below for a special case.

values  := [bytes] | [chars] | [shorts] | [ints] | [floats] | [doubles]

string  := nelems  [chars]

bytes   := [BYTE ...]  padding

chars   := [CHAR ...]  padding

shorts  := [SHORT ...]  padding

ints    := [INT ...]

floats  := [FLOAT ...]

doubles := [DOUBLE ...]

padding := <0, 1, 2, or 3 bytes to next 4-byte boundary>
                          // In header, padding is with 0 bytes.  In
                          // data, padding is with variable's fill-value.

NON_NEG := <INT with non-negative value>

ZERO    := <INT with zero value>

BYTE    := <8-bit byte>

CHAR    := <8-bit ACSII/ISO encoded character>

SHORT   := <16-bit signed integer, Bigendian, two's complement>

INT     := <32-bit signed integer, Bigendian, two's complement>

FLOAT   := <32-bit IEEE single-precision float, Bigendian>

DOUBLE  := <64-bit IEEE double-precision float, Bigendian>

// tags are 32-bit INTs
NC_BYTE      := 1         // data is array of 8 bit signed integer
NC_CHAR      := 2         // data is array of characters, i.e., text
NC_SHORT     := 3         // data is array of 16 bit signed integer
NC_LONG      := 4         // data is array of 32 bit signed integer
NC_FLOAT     := 5         // data is array of IEEE single precision float
NC_DOUBLE    := 6         // data is array of IEEE double precision float
NC_DIMENSION := 10
NC_VARIABLE  := 11
NC_ATTRIBUTE := 12

Computing File Offsets

To calculate the offset (position within the file) of a specified data value, let external_sizeof be the external size in bytes of one data value of the appropriate type for the specified variable, nc_type:

NC_BYTE         1
NC_CHAR         1
NC_SHORT        2
NC_LONG         4
NC_FLOAT        4
NC_DOUBLE       8

On open() (or endef()), scan through the array of variables, denoted var_array above, and sum the vsize fields of "record" variables to compute recsize.

Form the the products of the dimension sizes for the variable from right to left, skipping the leftmost (record) dimension for record variables, and storing the results in a product array for each variable. For example:

Non-record variable:

        dimension sizes:        [  5  3  2 7]
        product:                [210 42 14 7]

Record variable:

        dimension sizes:        [0  2  9 4]
        product:                [0 72 36 4]

At this point, the left-most product, when rounded up to the next multiple of 4, is the variable size, vsize, in the grammar above. For example, in the non-record variable above, the value of the vsize field is 212 (210 rounded up to a multiple of 4). For the record variable, the value of vsize is just 72, since this is already a multiple of 4.

Let coord be an array of the coordinates of the desired data value, and offset be the desired result. Then offset is just the file offset of the first data value of the desired variable (its begin field) added to the inner product of the coord and product vectors times the size, in bytes, of each datum for the variable. Finally, if the variable is a record variable, the product of the record number, `coord[0]', and the record size, recsize is added to yield the final offset value.

In pseudo-C code, here's the calculation of offset:

for (innerProduct = i = 0; i < var.rank; i++)
        innerProduct += product[i] * coord[i]
offset = var.begin;
offset += external_sizeof * innerProduct
if(IS_RECVAR(var))
        offset += coord[0] * recsize;

So, to get the data value (in external representation):

lseek(fd, offset, SEEK_SET);
read(fd, buf, external_sizeof);

A special case: Where there is exactly one record variable, we drop the restriction that each record be four-byte aligned, so in this case there is no record padding.
Examples

By using the grammar above, we can derive the smallest valid netCDF file, having no dimensions, no variables, no attributes, and hence, no data. A CDL representation of the empty netCDF file is

netcdf empty { }

This empty netCDF file has 32 bytes, as you may verify by using `ncgen -b empty.cdl' to generate it from the CDL representation. It begins with the four-byte "magic number" that identifies it as a netCDF version 1 file: 'C', 'D', 'F', '\001'. Following are seven 32-bit integer zeros representing the number of records, an empty array of dimensions, an empty array of global attributes, and an empty array of variables.

Below is an (edited) dump of the file produced on a big-endian machine using the Unix command

od -xcs empty.nc

Each 16-byte portion of the file is displayed with 4 lines. The first line displays the bytes in hexadecimal. The second line displays the bytes as characters. The third line displays each group of two bytes interpreted as a signed 16-bit integer. The fourth line (added by human) presents the interpretation of the bytes in terms of netCDF components and values.

   4344    4601    0000    0000    0000    0000    0000    0000
  C   D   F 001  \0  \0  \0  \0  \0  \0  \0  \0  \0  \0  \0  \0
  17220   17921   00000   00000   00000   00000   00000   00000
[magic number ] [  0 records  ] [  0 dimensions   (ABSENT)    ]

   0000    0000    0000    0000    0000    0000    0000    0000
 \0  \0  \0  \0  \0  \0  \0  \0  \0  \0  \0  \0  \0  \0  \0  \0
  00000   00000   00000   00000   00000   00000   00000   00000
[  0 global atts  (ABSENT)    ] [  0 variables    (ABSENT)    ]

As a slightly less trivial example, consider the CDL

netcdf tiny {
dimensions:
        dim = 5;
variables:
        short vx(dim);
data:
        vx = 3, 1, 4, 1, 5 ;
}

which corresponds to a 92-byte netCDF file. The following is an edited dump of this file:

   4344    4601    0000    0000    0000    000a    0000    0001
  C   D   F 001  \0  \0  \0  \0  \0  \0  \0  \n  \0  \0  \0 001
  17220   17921   00000   00000   00000   00010   00000   00001
[magic number ] [  0 records  ] [NC_DIMENSION ] [ 1 dimension ]

   0000    0003    6469    6d00    0000    0005    0000    0000
 \0  \0  \0 003   d   i   m  \0  \0  \0  \0 005  \0  \0  \0  \0
  00000   00003   25705   27904   00000   00005   00000   00000
[  3 char name = "dim"        ] [ size = 5    ] [ 0 global atts

   0000    0000    0000    000b    0000    0001    0000    0002
 \0  \0  \0  \0  \0  \0  \0 013  \0  \0  \0 001  \0  \0  \0 002
  00000   00000   00000   00011   00000   00001   00000   00002
 (ABSENT)     ] [NC_VARIABLE  ] [ 1 variable  ] [ 2 char name =

   7678    0000    0000    0001    0000    0000    0000    0000
  v   x  \0  \0  \0  \0  \0 001  \0  \0  \0  \0  \0  \0  \0  \0
  30328   00000   00000   00001   00000   00000   00000   00000
 "vx"         ] [1 dimension  ] [ with ID 0   ] [ 0 attributes

   0000    0000    0000    0003    0000    000c    0000    0050
 \0  \0  \0  \0  \0  \0  \0 003  \0  \0  \0  \f  \0  \0  \0   P
  00000   00000   00000   00003   00000   00012   00000   00080
 (ABSENT)     ] [type NC_SHORT] [size 12 bytes] [offset:    80]

   0003    0001    0004    0001    0005    8001
 \0 003  \0 001  \0 004  \0 001  \0 005 200 001
  00003   00001   00004   00001   00005  -32767
[    3] [    1] [    4] [    1] [    5] [fill ]

*/
