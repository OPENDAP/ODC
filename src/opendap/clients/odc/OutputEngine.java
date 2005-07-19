package opendap.clients.odc;

/**
 * Title:        OutputEngine
 * Description:  Methods to generate output
 * Company:      OPeNDAP.org
 * @author       John Chamberlain
 * @version      2.42
 */

import java.io.*;
import java.util.*;
import java.net.*;
import opendap.dap.*;
import java.awt.event.*;

public class OutputEngine implements ByteCounter {

	Activity activityOutput; // currently this class can support just one output activity (todo)

	// todo consolidate the routines that use the ListModelOutput object with the load-to-plotter
	// mechanism that uses the methods below
	long mTotalBytes = 0;
	public void vReportByteCount_EverySecond( long nByteCount ){
		ApplicationController.getInstance().vShowStatus_NoCache("Received " + Utility.getByteCountString(nByteCount) + " (" + nByteCount + ")");
	}
	public void vReportByteCount_Total( long nByteCount ){
		mTotalBytes = nByteCount;
	}

	public boolean zOutputToImageViewer(DodsURL[] aURLs, javax.swing.JButton jbuttonActivator, ActionListener action, StringBuffer sbError){
		try {
			if( aURLs == null ){
				sbError.append("internal error, no URLs supplied");
				return false;
			}
			int ctSelectedURLs = aURLs.length;
			if( ctSelectedURLs < 1 ){
				ApplicationController.vShowWarning("No URLs selected for image viewer");
				return true;
			}
			int ctValidURLs = 0;
			for(int xURL = 0; xURL < ctSelectedURLs; xURL++ ){
				if( aURLs[xURL].getType() == DodsURL.TYPE_Image ) ctValidURLs++;
			}
			if( ctValidURLs < 1 ){
				ApplicationController.vShowWarning("None of the selected URLs point to image files.");
				return true;
			}
			DodsURL[] aImageURLs = new DodsURL[ctValidURLs];
			String[] asTargetOption = new String[ctValidURLs];
			int xImageURL = -1;
			for(int xURL = 0; xURL < ctSelectedURLs; xURL++ ){
				if( aURLs[xURL].getType() == DodsURL.TYPE_Image ){
					xImageURL++;
					aImageURLs[xImageURL] = aURLs[xURL];
					asTargetOption[xImageURL] = aURLs[xURL].getFileName();
				}
			}
			OutputProfile op = new OutputProfile(aImageURLs, OutputProfile.TARGET_ViewImage, OutputProfile.FORMAT_Data_Raw, asTargetOption, null);
			activityOutput = new Activity();
			ListModelOutput lmo = new ListModelOutput(op, activityOutput);
			activityOutput.vDoActivity(jbuttonActivator, action, lmo, "Generating output, " + op);
			return true;
		} catch(Exception ex) {
			sbError.append("Unexpected error in output frame: " + ex);
			try { activityOutput.vCancelActivity(); } catch(Exception exCancel){}
			activityOutput = null;
			return false;
		}
	}

	public boolean zOutputToTextView(DodsURL[] aURLs, javax.swing.JButton jbuttonActivator, ActionListener action, StringBuffer sbError){
		try {
			if( aURLs == null ){
				sbError.append("internal error, no URLs supplied");
				return false;
			}
			int ctSelectedURLs = aURLs.length;
			if( ctSelectedURLs < 1 ){
				ApplicationController.vShowWarning("No URLs selected for text view");
				return true;
			}
			int ctValidURLs = 0;
			for(int xURL = 0; xURL < ctSelectedURLs; xURL++ ){
				int iTYPE = aURLs[xURL].getType();
				if( iTYPE == DodsURL.TYPE_Data || iTYPE == DodsURL.TYPE_Text ) ctValidURLs++;
			}
			if( ctValidURLs < 1 ){
				ApplicationController.vShowWarning("None of the selected URLs point to OPeNDAP data (or text).");
				return true;
			}
			DodsURL[] aDataURLs = new DodsURL[ctValidURLs];
			String[] asTargetOption = new String[ctValidURLs];
			int xDataURL = -1;
			for(int xURL = 0; xURL < ctSelectedURLs; xURL++ ){
				int iTYPE = aURLs[xURL].getType();
				if( iTYPE == DodsURL.TYPE_Data || iTYPE == DodsURL.TYPE_Text ){
					xDataURL++;
					aDataURLs[xDataURL] = aURLs[xURL];
					asTargetOption[xDataURL] = null;
				}
			}
			OutputStream os = ApplicationController.getInstance().getAppFrame().getTextViewerOS();
			OutputProfile op = new OutputProfile(aDataURLs, OutputProfile.TARGET_ViewText, OutputProfile.FORMAT_Data_ASCII_text, asTargetOption, os);
			activityOutput = new Activity();
			ListModelOutput lmo = new ListModelOutput(op, activityOutput);
			activityOutput.vDoActivity(jbuttonActivator, action, lmo, "Generating output, " + op);
			return true;
		} catch(Exception ex) {
			sbError.append("Unexpected error in output frame: " + ex);
			try { activityOutput.vCancelActivity(); } catch(Exception exCancel){}
			activityOutput = null;
			return false;
		}
	}

	public boolean zOutputToTableView(DodsURL[] aURLs, javax.swing.JButton jbuttonActivator, ActionListener action, StringBuffer sbError){
		try {
			if( aURLs == null ){
				sbError.append("internal error, no URLs supplied");
				return false;
			}
			int ctSelectedURLs = aURLs.length;
			if( ctSelectedURLs < 1 ){
				ApplicationController.vShowWarning("No URLs selected for table view");
				return true;
			}
			int ctValidURLs = 0;
			for(int xURL = 0; xURL < ctSelectedURLs; xURL++ ){
				int iTYPE = aURLs[xURL].getType();
				if( iTYPE == DodsURL.TYPE_Data || iTYPE == DodsURL.TYPE_Text ) ctValidURLs++;
			}
			if( ctValidURLs < 1 ){
				ApplicationController.vShowWarning("None of the selected URLs point to OPeNDAP data (or text) for table");
				return true;
			}
			DodsURL[] aDataURLs = new DodsURL[ctValidURLs];
			String[] asTargetOption = new String[ctValidURLs];
			int xDataURL = -1;
			for(int xURL = 0; xURL < ctSelectedURLs; xURL++ ){
				int iTYPE = aURLs[xURL].getType();
				if( iTYPE == DodsURL.TYPE_Data || iTYPE == DodsURL.TYPE_Text ){
					xDataURL++;
					aDataURLs[xDataURL] = aURLs[xURL];
					asTargetOption[xDataURL] = null;
				}
			}
			OutputStream os = ApplicationController.getInstance().getAppFrame().getTableViewerOS(sbError);
			if( os == null ){
				sbError.insert(0, "Error obtaining output stream for table viewer: ");
				return false;
			}
			OutputProfile op = new OutputProfile(aDataURLs, OutputProfile.TARGET_ViewTable, OutputProfile.FORMAT_Data_ASCII_records, asTargetOption, os);
			activityOutput = new Activity();
			ListModelOutput lmo = new ListModelOutput(op, activityOutput);
			activityOutput.vDoActivity(jbuttonActivator, action, lmo, "Generating output, " + op);
			ApplicationController.getInstance().getAppFrame().vActivateViewTablePanel();
			return true;
		} catch(Exception ex) {
			sbError.append("Unexpected error in output frame: " + ex);
			try { activityOutput.vCancelActivity(); } catch(Exception exCancel){}
			activityOutput = null;
			return false;
		}
	}

	public boolean zOutputToFile(final DodsURL[] aURLs, final String sPath, int eFormat, final javax.swing.JButton jbuttonActivator, final ActionListener action, StringBuffer sbError){
		try {
			if( aURLs == null ){
				sbError.append("internal error, no URLs supplied");
				return false;
			}
			int ctSelectedURLs = aURLs.length;
			if( ctSelectedURLs < 1 ){
				ApplicationController.vShowWarning("No URLs selected for output to file");
				return true;
			}
			int ctValidURLs = 0;
			for(int xURL = 0; xURL < ctSelectedURLs; xURL++ ){
				String sLabel = aURLs[xURL].getBaseURL();
				int iTYPE = aURLs[xURL].getType();
				switch( iTYPE ){
					case DodsURL.TYPE_Catalog :
						ApplicationController.vShowWarning("Unable to save " + sLabel + ", catalog URLs not savable.");
						break;
					case DodsURL.TYPE_Directory :
						ApplicationController.vShowWarning("Unable to save " + sLabel + ", directory URLs not savable.");
						break;
					case DodsURL.TYPE_HTML :
						ApplicationController.vShowWarning("Unable to save " + sLabel + ", HTML URLs not savable.");
						break;
					case DodsURL.TYPE_Data :
						if( ((eFormat & OutputProfile.FORMAT_Data_ASCII_text) > 0 ||
						     (eFormat & OutputProfile.FORMAT_Data_ASCII_records) > 0) ){
							ctValidURLs++;
						} else {
							ApplicationController.vShowWarning("Unable to save " + sLabel + ", data URLs can only be saved as ASCII");
						}
    					break;
					case DodsURL.TYPE_Binary :
					case DodsURL.TYPE_Image :
					case DodsURL.TYPE_Text :
					default :
						if( (eFormat & OutputProfile.FORMAT_Data_Raw) > 0 ){
							ctValidURLs++;
						} else {
							ApplicationController.vShowWarning("Unable to save " + sLabel + ", non-OPeNDAP URLs can only be saved as binary");
						}
    					break;
				}
			}
			if( ctValidURLs < 1 ){
				ApplicationController.vShowError("None of the selected URLs point to a source savable to file (see warnings)");
				return true;
			}

			// Resolve the file location(s)
			// if the path is directory then separate files will be created
			// if the path is a file then files will be concatenated
			File filePath;
			try {
				filePath = new File(sPath);
			} catch(Exception ex) {
				sbError.append("unable to resolve path (" + sPath + "): " + ex);
				return false;
			}

			DodsURL[] aDataURLs = new DodsURL[ctValidURLs];
			String[] asTargetOption = new String[ctValidURLs];
			int xDataURL = -1;
			for(int xURL = 0; xURL < ctSelectedURLs; xURL++ ){
				int iTYPE = aURLs[xURL].getType();
				switch( iTYPE ){
					case DodsURL.TYPE_Catalog :
					case DodsURL.TYPE_Directory :
					case DodsURL.TYPE_HTML :
						continue;
					case DodsURL.TYPE_Data :
						if( ((eFormat & OutputProfile.FORMAT_Data_ASCII_text) > 0 ||
						     (eFormat & OutputProfile.FORMAT_Data_ASCII_records) > 0) ){
							xDataURL++;
							aDataURLs[xDataURL] = aURLs[xURL];
							if( filePath.isDirectory() ){
								String sFileName = aURLs[xURL].getFileName();
								if( sFileName == null || sFileName.length() == 0 ) sFileName = "output_" + System.currentTimeMillis();
								String sDirectory = filePath.getCanonicalPath();
								asTargetOption[xDataURL] = Utility.sConnectPaths(sDirectory, sFileName);
							} else {
								asTargetOption[xDataURL] = filePath.getCanonicalPath();
							}
						}
						break;
					case DodsURL.TYPE_Binary :
					case DodsURL.TYPE_Image :
					case DodsURL.TYPE_Text :
					default :
						if( (eFormat & OutputProfile.FORMAT_Data_Raw) > 0 ){
							ctValidURLs++;
						}
						xDataURL++;
						aDataURLs[xDataURL] = aURLs[xURL];
						if( filePath.isDirectory() ){
							String sFileName = aURLs[xURL].getFileName();
							if( sFileName == null || sFileName.length() == 0 ) sFileName = "output_" + System.currentTimeMillis();
							String sDirectory = filePath.getCanonicalPath();
							asTargetOption[xDataURL] = Utility.sConnectPaths(sDirectory, sFileName);
						} else {
							asTargetOption[xDataURL] = filePath.getCanonicalPath();
						}
				}
			}
			OutputProfile op = new OutputProfile(aDataURLs, OutputProfile.TARGET_File, eFormat, asTargetOption, null);
			activityOutput = new Activity();
			ListModelOutput lmo = new ListModelOutput(op, activityOutput);
			activityOutput.vDoActivity(jbuttonActivator, action, lmo, "Generating output, " + op);
			ApplicationController.getInstance().getAppFrame().vActivateViewTablePanel();
			return true;
		} catch(Exception ex) {
			sbError.append("Unexpected error in output frame: " + ex);
			try { activityOutput.vCancelActivity(); } catch(Exception exCancel){}
			activityOutput = null;
			return false;
		}

	}

	public boolean zOutputToPlotter(final DodsURL[] aURLs, final javax.swing.JButton jbuttonActivator, final ActionListener action, StringBuffer sbError){
		try {

			final ArrayList listValidURLs = new ArrayList();

			// get selection established
			if (aURLs == null) {
				sbError.append("internal error, no URLs supplied");
				return false;
			}
			final int ctSelectedURLs = aURLs.length;
			if (ctSelectedURLs < 1) {
				ApplicationController.vShowWarning("No URLs selected for plotter");
				return true;
			}
			int ctValidURLs = 0;
			for (int xURL = 0; xURL < ctSelectedURLs; xURL++) {
				int iTYPE = aURLs[xURL].getType();
				if (iTYPE == DodsURL.TYPE_Data){
					ctValidURLs++;
					listValidURLs.add( aURLs[xURL] );
				}
			}
			if (ctValidURLs < 1) {
				ApplicationController.vShowWarning(
					"None of the selected URLs point to OPeNDAP data.");
				return true;
			}
			ApplicationController.getInstance().getAppFrame().vActivatePlotter();
			Thread.yield();

			/* there are two reasons for using a sequential continuation for loading the
               URLs. one is to not send multiple simultaneous requests to the server, the
               the other is so that if an URL send fails the return will cause no
               more continuations to be executed so the load will stop

               note that this will not cause a stack problem because each launching
               activity exits after it launches the next one

			*/
			final Continuation_DoCancel conNextURL = new Continuation_DoCancel(){
				public void Do(){
					if( listValidURLs.size() == 0 ) return;
					DodsURL url = (DodsURL)listValidURLs.get(0);
					listValidURLs.remove(0);
					loadURLToPlotter(url, jbuttonActivator, action, this);
				}
				public void Cancel(){
				}
			};
			DodsURL url = (DodsURL)listValidURLs.get(0);
			listValidURLs.remove(0);
			loadURLToPlotter(url, jbuttonActivator, action, conNextURL);

			return true;
		} catch(Throwable t) {
			sbError.append("Unexpected error in output frame: ");
			Utility.vUnexpectedError(t, sbError);
			return false;
		}
	}

	public boolean zOutputProfile(javax.swing.JButton jbuttonActivator, ActionListener action, OutputProfile op, StringBuffer sbError){
		try {
			if( op == null ){
				sbError.append("internal error, no output profile supplied");
				return false;
			}
			activityOutput = new Activity();
			ListModelOutput lmo = new ListModelOutput(op, activityOutput);
			activityOutput.vDoActivity(jbuttonActivator, action, lmo, "Generating output profile, " + op);
			return true;
		} catch(Exception ex) {
			sbError.append("Unexpected error in profile output frame: " + ex);
			try { activityOutput.vCancelActivity(); } catch(Exception exCancel){}
			activityOutput = null;
			return false;
		}
	}

	class ListModelOutput implements Continuation_DoCancel, ByteCounter {
		private OutputProfile mOutputProfile;
		private long mTotalBytes = 0;
		private Activity mActivity;
		ListModelOutput(Activity activity){
			mActivity = activity;
		}
		ListModelOutput(OutputProfile op, Activity activity){
			mOutputProfile = op;
			mActivity = activity;
		}
		OutputProfile getOutputProfile(){ return mOutputProfile; }
		public void vReportByteCount_EverySecond( long nByteCount ){
			ApplicationController.getInstance().vShowStatus_NoCache("Received " + Utility.getByteCountString(nByteCount) + " (" + nByteCount + ")");
		}
		public void vReportByteCount_Total( long nByteCount ){
			mTotalBytes = nByteCount;
		}
		public void Cancel(){
			// after the activity cancels this is called to do cleanup -- none here
		}
		public void Do(){
			OutputStream os = null;
			try {
				if( mOutputProfile == null ) return;
				StringBuffer sbError = new StringBuffer(250);
				int ctURLs = mOutputProfile.getURLCount();
				if( ctURLs < 1 ){
					ApplicationController.vShowError("internal error, no URLs specified");
					return;
				}
				for( int xURL = 1; xURL <= ctURLs; xURL++ ){
					DodsURL url = mOutputProfile.getURL(xURL);
					if( url == null ) continue;
					int iTarget = mOutputProfile.getTarget(xURL);
					int iFormat = mOutputProfile.getFormat(xURL);
					int iURLType = url.getType();
					String sTargetOption = mOutputProfile.getTargetOption(xURL);
					String sFormatDescription = OutputProfile.sFormatDescription(iFormat);
					String sTargetDescription = OutputProfile.sTargetDescription(iTarget);
					ApplicationController.vShowStatus("Outputting URL " + xURL + " of " + ctURLs + "  " + url.getFullURL() + " [" + url.getTypeString() + "] as " + sFormatDescription + " to " + sTargetDescription);
					os = mOutputProfile.getTargetOS();
                    if( os == null ){
						if( iTarget == OutputProfile.TARGET_File ){
							if( sTargetOption == null ){
								ApplicationController.vShowError("unable to output to file, internal error, no target file/directory specified");
								return; // terminate because the likelihood is all files will fail the same way
							}
							String sSeparator = Utility.getFileSeparator();
							File fileTarget;
							try {
								fileTarget = new File(sTargetOption);
							} catch(Exception ex) {
								ApplicationController.vShowError("unable to output to file, file/directory resolution failed for (" + sTargetOption + "): " + ex);
								continue;
							}

							// validate that file can be created
							if( !fileTarget.exists() ){
								try {
									fileTarget.createNewFile();
								} catch(Exception ex) {
									ApplicationController.vShowError("unable to output to file, file/directory creation failed for (" + sTargetOption + "): " + ex);
									continue;
								}
							}

							// obtain output stream
							try {
							    FileOutputStream fos = new FileOutputStream(fileTarget);
							} catch(Exception ex) {
								ApplicationController.vShowError("unable to output to file, stream creation failed for (" + sTargetOption + "): " + ex);
								continue;
							}

						} else if( iTarget == OutputProfile.TARGET_ViewImage ){
							os = ApplicationController.getInstance().getAppFrame().getImageViewerOS( sTargetOption, sbError );
							if( os == null ){
								ApplicationController.vShowError("unable to open cache file for target " + sTargetOption + ": " + sbError);
								sbError.setLength(0);
								continue;
							}
						} else if( iTarget == OutputProfile.TARGET_StandardOut ) {
							os = System.out;
						} else if( iTarget == OutputProfile.TARGET_ViewText ) {
							os = ApplicationController.getInstance().getTextViewerOS();
							if( os == null ){
								ApplicationController.vShowError("Internal error, text viewer output stream unavailable");
								return; // abort
							}
						} else if( iTarget == OutputProfile.TARGET_ViewImage ) {
							os = ApplicationController.getInstance().getImageViewerOS(sTargetOption, sbError);
							if( os == null ){
								ApplicationController.vShowError("Internal error, image viewer output stream unavailable");
								return; // abort
							}
						} else if( iTarget == OutputProfile.TARGET_ViewTable ) {
							// os = ApplicationController.getInstance().getTableViewerOS(sTargetOption, sbError);
							if( os == null ){
								ApplicationController.vShowError("Internal error, table view output stream unavailable: " + sbError);
								return; // abort
							}
						} else if( iTarget == OutputProfile.TARGET_Plotter ) {
							ApplicationController.vShowError("Internal error, plotter has no output stream unavailable: " + sbError);
							return; // abort
						} else {
							ApplicationController.vShowError("internal error, unknown target (" + iTarget + ")");
							continue;
						}
                    }
					if( iTarget == OutputProfile.TARGET_ViewText ){
						ApplicationController.getInstance().getAppFrame().vActivateViewTextPanel();
					}
					vOutput(url, os, iFormat);
					String sTerminator = mOutputProfile.getTerminator();
					if( sTerminator != null ) os.write(sTerminator.getBytes());
					try{
						if( os != null ){
							os.flush();
							if( os != null && os != System.out && os != System.err ){
								os.close();
							}
						}
					} catch(Exception exx) {
// there is an NPE occurring in FilterOutputStream of unknown cause
//						Utility.vUnexpectedError(exx, "error closing stream");
					}
					if( iTarget == OutputProfile.TARGET_ViewImage ){
						ApplicationController.getInstance().getAppFrame().vActivateViewImagePanel();
						ApplicationController.getInstance().getAppFrame().getImageViewer().vRefreshImageList(sTargetOption);
					}
				}
			} catch(Exception ex) {
				StringBuffer sb = new StringBuffer("Unexpected error during output: ");
				Utility.vUnexpectedError(ex, sb);
				ApplicationController.vShowError(sb.toString());
				if( os != null )
				    try { os.close(); } catch(Exception exx) {}
			}
		}

		private boolean zOpenOutputStream(OutputStream[] eggOutputStream, int iTarget, String sTargetOption, StringBuffer sbError){

			if( eggOutputStream == null ){ sbError.append("internal error, no egg"); return false; }

			// first, determine if there are multiple outputs or only one
			// currently uses only one output
			int ctOutputs = 1;

			if( ctOutputs == 0 ){
				sbError.append("no output target(s) specified");
				return false;
			} else if( ctOutputs == 1 ) { // return just that one output stream
				if( (iTarget == OutputProfile.TARGET_File) ) {
					try {
						File fileOut = new File(sTargetOption);
						FileOutputStream fos = new FileOutputStream(fileOut);
						eggOutputStream[0] = fos;
						return true;
					} catch(Exception ex) {
						sbError.append("failed to open file [" + sTargetOption + "]: " + ex);
						return false;
					}
				} else if( iTarget == OutputProfile.TARGET_StandardOut ) {
					eggOutputStream[0] = System.out;
					return true;
				} else if( iTarget == OutputProfile.TARGET_ViewText ) {
					eggOutputStream[0] = ApplicationController.getInstance().getTextViewerOS();
					return true;
				} else if( iTarget == OutputProfile.TARGET_ViewImage ) {
					eggOutputStream[0] = ApplicationController.getInstance().getImageViewerOS(sTargetOption, sbError);
					return (eggOutputStream[0] != null);
				} else if( iTarget == OutputProfile.TARGET_ViewTable ) {
					sbError.append("table view has no input stream (not implemented)");
					return false;
//					eggOutputStream[0] = ApplicationController.getInstance().getTableViewerOS(sTargetOption, sbError);
//					return (eggOutputStream[0] != null);
				} else if( iTarget == OutputProfile.TARGET_Plotter ) {
					sbError.append("plotter has no input stream");
					return false;
				} else return false; // should not happen
			} else { // consolidate the multiple output streams into a filter stream
				StreamForwarder sf = new StreamForwarder(null);
				int ctSuccessfulTargets = 0;
				if( (iTarget & OutputProfile.TARGET_File) > 0) {
					try {
						FileOutputStream fos = new FileOutputStream(sTargetOption);
						if( sf.add(fos, sbError) ){
							ApplicationController.getInstance().vShowStatus("targeting file " + sTargetOption);
							ctSuccessfulTargets++;
						} else {
							sbError.insert(0, "failed to target file " + sTargetOption);
							ApplicationController.getInstance().vShowError(sbError.toString());
							sbError.setLength(0);
						}
					} catch(Exception ex) {
						sbError.insert(0, "failed to open file [" + sTargetOption + "]: " + ex);
						ApplicationController.getInstance().vShowError(sbError.toString());
						sbError.setLength(0);
					}
				}
				if( (iTarget & OutputProfile.TARGET_StandardOut) > 0) {
					if( sf.add(System.out, sbError) ){
						ApplicationController.getInstance().vShowStatus("targeting System.out");
						ctSuccessfulTargets++;
					} else {
						sbError.insert(0, "failed to target System.out");
						ApplicationController.getInstance().vShowError(sbError.toString());
						sbError.setLength(0);
					}
				}
				if( (iTarget & OutputProfile.TARGET_ViewText) > 0) {
					if( sf.add(ApplicationController.getInstance().getTextViewerOS(), sbError) ){
						ApplicationController.getInstance().vShowStatus("targeting text viewer");
						ctSuccessfulTargets++;
					} else {
						sbError.insert(0, "failed to target text viewer: ");
						ApplicationController.getInstance().vShowError(sbError.toString());
						sbError.setLength(0);
					}
				}
				if( (iTarget & OutputProfile.TARGET_ViewImage) > 0) {
					if( sTargetOption == null ){
						sbError.insert(0, "failed to target image viewer because file name did not exist");
						ApplicationController.getInstance().vShowError(sbError.toString());
						sbError.setLength(0);
					} else {
						OutputStream os = ApplicationController.getInstance().getImageViewerOS(sTargetOption, sbError);
						if( sf.add(os, sbError) ){
							ApplicationController.getInstance().vShowStatus("targeting image viewer");
							ctSuccessfulTargets++;
						} else {
							sbError.insert(0, "failed to target image viewer: ");
							ApplicationController.getInstance().vShowError(sbError.toString());
							sbError.setLength(0);
						}
					}
				}
				if( ctSuccessfulTargets ==  0 ){
					sbError.append("no output targets");
					return false;
				} else {
					eggOutputStream[0] = sf;
					return true;
				}
			}
		}

		private void vOutput(DodsURL url, OutputStream os, int iFormat){
			final byte[] abNewline = { '\n' };
            StringBuffer sbError = new StringBuffer(250);
            if( url == null ){
				ApplicationController.getInstance().vShowWarning("no url given");
                return;
            }
			try {
				String sName = url.getTitle();
				if( iFormat == OutputProfile.FORMAT_URLs ){
					vOutput_URL( url, os );
					return;
				}
				if( iFormat == OutputProfile.FORMAT_Info ) {
					vOutput_Info( url, os );
					return;
				}
				if( iFormat == OutputProfile.FORMAT_Data_Raw ) {
					vOutput_Raw( url, os );
					return;
				}
				if( iFormat == OutputProfile.FORMAT_Data_ASCII_text ) {
					vOutput_ASCII( url, os );
					return;
				}
				if( iFormat == OutputProfile.FORMAT_Data_ASCII_records ) {
					vOutput_FormattedASCII( url, os, mActivity );
					return;
				}
				ApplicationController.vShowError("Unsupported format: " + iFormat);
			} catch(Exception ex) {
				ApplicationController.vShowError("Unexpected error delivering output: " + ex);
			}
		}

		private void vOutput_URL(DodsURL url, OutputStream os){
			StringBuffer sbError = new StringBuffer(80);
			try {
				final byte[] abNewline = { '\n' };
				int iType = url.getType();
				if( iType == DodsURL.TYPE_Data || iType == DodsURL.TYPE_Directory ){
					os.write(url.getFullURL().getBytes());
					os.write(abNewline);
				} else if( iType == DodsURL.TYPE_Catalog ){
					Catalog catalog = url.getCatalog();
					DodsURL[] urlsCatalog = null;
					if( catalog != null ) urlsCatalog = catalog.getURLs();
					if( urlsCatalog == null ){
						String sFullURL = url.getFullURL();
						os.write(sFullURL.getBytes());
						os.write(abNewline);
					} else {
						for( int xCatalogURL = 0; xCatalogURL < urlsCatalog.length; xCatalogURL++ ){
							if( urlsCatalog[xCatalogURL] == null ){
								ApplicationController.vShowWarning("internal inconsistency; catalog url " + xCatalogURL + " was null");
							} else {
								String sFullURL = urlsCatalog[xCatalogURL].getFullURL();
								os.write(sFullURL.getBytes());
								os.write(abNewline);
							}
						}
					}
				} else {
				}
			} catch(Exception ex) {
				Utility.vUnexpectedError(ex, sbError);
				ApplicationController.vShowError(sbError.toString());
			}
		}

		private void vOutput_Info(DodsURL url, OutputStream os){
			StringBuffer sbError = new StringBuffer(80);
			try {
				int iType = url.getType();
				String sDDS = null;
				String sDAS = null;
				if( iType == DodsURL.TYPE_Data ){
					String sBaseURL = url.getBaseURL();
//					sDDS = Utility.sFetchHttpString(sBaseURL + ".dds", sbError);
//					sDAS = Utility.sFetchHttpString(sBaseURL + ".das", sbError);
				    sDDS = IO.getStaticContent(sBaseURL + ".dds", null, mActivity, sbError);
				    sDAS = IO.getStaticContent(sBaseURL + ".das", null, mActivity, sbError);
					if( sDDS == null ) sDDS = "[DDS not available: " + sbError + "]";
					if( sDAS == null ) sDAS = "[DAS not available: " + sbError + "]\n";
					sDDS += "\n";
					os.write(sDDS.getBytes());
					sDAS += "\n";
					os.write(sDAS.getBytes());
				} else if( iType == DodsURL.TYPE_Directory ){
					String sDirectoryInfo;
					Model_DirectoryTree dt = url.getDirectoryTree();
					if( dt == null ){
						sDirectoryInfo = "[directory tree unavailable]"; // todo
					} else {
						sDirectoryInfo = dt.getPrintout();
					}
					sDirectoryInfo += "\n";
					os.write(sDirectoryInfo.getBytes());
				} else if( iType == DodsURL.TYPE_Catalog ){
					ApplicationController.vShowWarning("catalog info not supported");
				}
			} catch(Exception ex) {
				Utility.vUnexpectedError(ex, sbError);
				ApplicationController.vShowError(sbError.toString());
			}
		}

		private DataDDS getDataDDS(DodsURL url, Activity activity, StringBuffer sbError){
			try {
				if( url.getType() != DodsURL.TYPE_Data ){
					sbError.append("Currently data output is supported for data urls only; not catalogs or directories");
					return null;
				}
				String sFullURL = url.getFullURL();
				String sBaseURL = url.getBaseURL();
				String sConstraintExpression = url.getConstraintExpression_Encoded();
				ApplicationController.vShowStatus("Connecting to " + sBaseURL + "...");
				OpendapConnection conn = new OpendapConnection();
				conn.setUserAgent(ApplicationController.getInstance().getVersionString());
				DataDDS datadds = conn.getDataDDS(sBaseURL, sConstraintExpression, this, activity, sbError);
				if( datadds == null ){
					if( sbError.length() > 0 ) // otherwise user cancelled
						ApplicationController.vShowError("Error getting data for " + sFullURL + " at " + mTotalBytes +": " + sbError);
					return null;
				} else {
					ApplicationController.vShowStatus("Received " + Utility.getByteCountString(mTotalBytes) + " (" + mTotalBytes + ") for " + sFullURL);
				}
				return datadds;
			} catch(Exception ex) {
				Utility.vUnexpectedError(ex, sbError);
				return null;
			}
		}

		private boolean array_zValidateDimension( int iStart, int iStride, int iStop, StringBuffer sbError ){
			if( iStart < 0 ){
				sbError.append("start index (" + iStart + ") less then zero");
				return false;
			}
			if( iStop < iStart ){
				sbError.append("stop index (" + iStop + ") less then start index (" + iStart + ")");
				return false;
			}
			if( iStride < 1 ){
				sbError.append("stride (" + iStride + ") less then one");
				return false;
			}
			return true;
		}

		int array_getIndexCount( int iStart, int iStride, int iStop ){
			int ctIndex = 1; // the start index is always included
			for( iStart += iStride; iStart < iStop; iStart += iStride ){
				ctIndex++;
			}
			ctIndex++; // the end index is always included
			return ctIndex;
		}

		private void vOutput_FormattedASCII(DodsURL url, OutputStream os, Activity activity){
			String sFullURL = url.getFullURL();
			StringBuffer sbError = new StringBuffer(80);
			DataDDS datadds = getDataDDS(url, activity, sbError);
			if( datadds == null ){
				if( sbError.length() > 0 )
					ApplicationController.getInstance().vShowError("Failed to get data DDS: " + sbError);
				return;
			}
			try {
				Enumeration enum = datadds.getVariables();
				int xVariable = 0;
				while( enum.hasMoreElements() ){
					xVariable++;
					BaseType basetype = (BaseType)enum.nextElement();
					if( !zOutput_FormattedVariable( null, basetype, os, sbError ) ){
						ApplicationController.vShowError("Error outputting variable " + xVariable + ": " + sbError);
						continue;
					}
				}
				ApplicationController.vShowStatus("Output complete for " + sFullURL);
			} catch(Exception ex) {
				Utility.vUnexpectedError(ex, sbError);
				ApplicationController.vShowError(sbError.toString());
			}
		}

		// pass null for the parent name if the child is at the root level
		private boolean zOutput_FormattedVariable( String sParentName, BaseType basetype, OutputStream os, StringBuffer sbError ){
			try {
				StringBuffer sbOut = new StringBuffer(250);
				if( basetype == null ){
					sbError.append("null base type");
					return false;
				}
				String sVariableName = (sParentName==null) ? basetype.getName() : sParentName + "." + basetype.getName();
				if(basetype instanceof DArray) {
					if( !zOutput_FormattedArray( sVariableName, (DArray)basetype, null, os, sbError )){
						sbError.insert(0, "Failed to output array " + sVariableName + ": ");
						return false;
					}
				}
				else if(basetype instanceof DStructure ) {
					DStructure dstructure = (DStructure)basetype;
					Enumeration enum = dstructure.getVariables();
					int xVariable = 0;
					while( enum.hasMoreElements() ){
						xVariable++;
						BaseType btElement = (BaseType)enum.nextElement();
						if( !zOutput_FormattedVariable( sVariableName, btElement, os, sbError ) ){
							ApplicationController.vShowError("Error outputting structure " + sVariableName + "." + xVariable + ": " + sbError);
							continue;
						}
					}
				}
				else if(basetype instanceof DSequence) {
					DSequence dsequence = (DSequence)basetype;
					int ctVariable = dsequence.elementCount(false);
					int ctPrimitiveVariables = 0;
					int ctRows = dsequence.getRowCount();
					// first output any non-primitive values and collect the primitives into a DArray
					int[] aiVariableIndex = new int[ctVariable+1];
					for(int xVariable = 0; xVariable < ctVariable; xVariable++){
						BaseType btElement = dsequence.getVar(xVariable);
						String sElementName = btElement.getName();
						if( btElement instanceof DStructure || btElement instanceof DGrid || btElement instanceof DArray || btElement instanceof DSequence ){
							for( int xRow = 1; xRow <= ctRows; xRow++ ){ // todo xxx dimensionalize rows
								BaseType btRowElement = dsequence.getVariable(xRow-1, sElementName);
								if( !zOutput_FormattedVariable( sVariableName + "_row_" + xRow, btElement, os, sbError ) ){
									sbError.append("Error outputting sequence row " + xRow + " " + sVariableName + "." + sElementName + ": " + sbError);
									return false;
								}
							}
						} else {
							ctPrimitiveVariables++;
							aiVariableIndex[ctPrimitiveVariables] = xVariable;
						}
					}
					if( ctPrimitiveVariables > 0 ){
						aiVariableIndex[0] = ctPrimitiveVariables;
						if( !zOutput_FormattedSequence( sVariableName, dsequence, aiVariableIndex, os, sbError ) ){
							sbError.append("Error outputting sequence " + sVariableName + ": " + sbError);
							return false;
						}
					}

				}
				else if(basetype instanceof DGrid) {
					DGrid dgrid = (DGrid)basetype;
					BaseType btArray = dgrid.getVar(0);
					if( btArray == null ){
						sbError.insert(0, "grid has no array component");
						return false;
					}
					DArray darray = (DArray)btArray;
					int ctDimensions = darray.numDimensions();
					int ctElements = dgrid.elementCount(false);
					int ctMaps = ctElements-1;
					if( ctDimensions != ctMaps ){
						sbError.insert(0, "grid with " + ctDimensions + "-dimensional array has only " + ctMaps + " maps");
						return false;
					}
					ArrayList listMap = new ArrayList();
					for(int xMap = 1; xMap <= ctMaps; xMap++ ){
						BaseType btMap = dgrid.getVar(xMap);
						if( btMap == null ){
							sbError.insert(0, "map component " + xMap + " was unexpectedly null");
							return false;
						}
						listMap.add(btMap);
					}
					if( !zOutput_FormattedArray( sVariableName, darray, listMap, os, sbError )){
						sbError.insert(0, "Failed to output array " + sVariableName + ": ");
						return false;
					}
				}
				else if( basetype instanceof DByte ||
						 basetype instanceof DBoolean ||
						 basetype instanceof DInt16 ||
						 basetype instanceof DUInt16 ||
						 basetype instanceof DInt32 ||
						 basetype instanceof DUInt32 ||
						 basetype instanceof DFloat32 ||
						 basetype instanceof DFloat64 ||
						 basetype instanceof DString
						) {
					if( !zOutput_FormattedVariable( sVariableName, basetype, os, sbError ) ){
						sbError.append("Error outputting scalar " + sVariableName + ": " + sbError);
						return false;
					}
				}
				else {
					sbError.append("Unknown base type: " + basetype.getTypeName());
					return false;
				}
				return true;
			} catch(Exception ex) {
				Utility.vUnexpectedError(ex, sbError);
				return false;
			}
		}

		private boolean zOutput_FormattedSequence( String sVariableName, DSequence dsequence, int[] aiVariableIndex, OutputStream os, StringBuffer sbError){
			try {
				StringBuffer sbOut = new StringBuffer(250);
				if( dsequence == null ){
					sbError.append("null input");
					return false;
				}
				int iRowCount = dsequence.getRowCount();
				int ctVariable = aiVariableIndex[0];
				int iFieldCount = ctVariable + 1; // one additional column for the row number

				// output header {"table name" RowCount: iRowCount ColumnCount: iFieldCount DimColumns: "dim 1" "dim 2" ... "dim n-1" ValColumns: "val" 1 3 5 7 ... n ColumnTypes: String Int32 ...}
				sbOut.setLength(0);
				sbOut.append("{\"" + sVariableName + "\" RowCount: " + iRowCount + " ColumnCount: " + iFieldCount + " DimColumns:"); // todo double escape quotation marks
				StringBuffer sbColumnTypes = new StringBuffer(80);
				sbOut.append(" \"Row\"");
				sbColumnTypes.append(" Int32");
				sbOut.append(" ValColumns: Variables");
				for( int xVariable = 1; xVariable <= ctVariable; xVariable++ ){
					BaseType btVariable = dsequence.getVar(xVariable-1);
					sbOut.append(" \"" + btVariable.getName() + "\"");
					sbColumnTypes.append(" " + btVariable.getTypeName());
				}
				sbOut.append(" ColumnTypes:" + sbColumnTypes);
				sbOut.append("}");
				os.write(sbOut.toString().getBytes());

				// print rows
				PrintWriter pw = new PrintWriter(os);
				for(int xRow = 1; xRow <= iRowCount; xRow++ ){
					pw.print('{');
					pw.print(xRow);
					pw.print(' ');
					Vector vRow = dsequence.getRow(xRow-1);
					int xVariable = 1;
					for( ; xVariable < ctVariable; xVariable++ ){
						BaseType btVariable = (BaseType)vRow.get(aiVariableIndex[xVariable]);
						btVariable.printVal(pw, "", false);
						pw.print(' ');
					}
					BaseType btVariable = (BaseType)vRow.get(aiVariableIndex[ctVariable]); // the last value has no following space
					btVariable.printVal(pw, "", false);
					pw.print('}');
				}
				pw.println("{/\"" + sVariableName + "\"}");
				pw.flush();
				return true;
			} catch(java.net.SocketException exSocket) {
				sbError.append("Socket error [" + exSocket + "]. Client may have closed connection.");
				return false;
			} catch(Exception ex) {
				Utility.vUnexpectedError(ex, sbError);
				return false;
			}
		}

		// the listMap is an ArrayList containing the map base types which are one-dimensional arrays of values
		private boolean zOutput_FormattedArray( String sVariableName, DArray darray, ArrayList listMap, OutputStream os, StringBuffer sbError ){
			try {
				StringBuffer sbOut = new StringBuffer(250);
				if( darray == null ){
					sbError.append("null input");
					return false;
				}

				Enumeration eDimensions = darray.getDimensions();
				ArrayList listDimensions = new ArrayList();
				int ctDimensions = 0;
				while(eDimensions.hasMoreElements()) {
					ctDimensions++;
					listDimensions.add(eDimensions.nextElement());
				}
				if( ctDimensions != darray.numDimensions() ){
					sbError.append("Inconsistent data structure; states " + darray.numDimensions() + " dimensions and actual count is " + ctDimensions);
					return false;
				}
				if( ctDimensions == 0 ) return true; // ignore zero-dimensional arrays
				DArrayDimension dimensionLast = (DArrayDimension)listDimensions.get(ctDimensions - 1);
				int iStart = dimensionLast.getStart();
				int iStride = dimensionLast.getStride();
				int iStop = dimensionLast.getStop();
				if( !array_zValidateDimension( iStart, iStride, iStop, sbError ) ){
					sbError.append("Last dimension of array " + sVariableName + " is invalid: " + sbError);
					return false;
				}
				int ctLastDimensionIndexes = array_getIndexCount( iStart, iStride, iStop );
				int iFieldCount;
			    if( ctDimensions == 1 ){
					iFieldCount = 2; // one for the index/value label and one for the data value
				} else {
				    iFieldCount = ctDimensions - 1 + ctLastDimensionIndexes;
				}

				// build dimensional mappings if they are not provided
				if( listMap == null ){
					listMap = new ArrayList();
					for( int xDimension = 1; xDimension <= ctDimensions; xDimension++ ){
						DArrayDimension dim = (DArrayDimension)listDimensions.get(xDimension-1);
						int iDimSize = dim.getSize();
						iStart = dim.getStart();
						iStride = dim.getStride();
						iStop = dim.getStop();
						String sDimName = dim.getName();
						if( sDimName == null ) sDimName = "Dim_" + xDimension;
						DArray darrayDimMap = new DArray(sDimName);
						darrayDimMap.addVariable(new DInt32(sDimName));
						darrayDimMap.appendDim(iDimSize);
						darrayDimMap.setLength(iDimSize);
						Int32PrimitiveVector pv = (Int32PrimitiveVector)darrayDimMap.getPrimitiveVector();
						int xVector=0;
						int iValue = iStart;
						while(true){
							pv.setValue(xVector, iValue);
							int iPreviousValue = iValue;
							xVector++;
							iValue += iStride;
							if( iValue >= iStop ){
								if( iPreviousValue < iStop ){
									pv.setValue(xVector, iStop);
								}
								break;
							}
						}
						listMap.add(darrayDimMap);
					}
				} else { // validate map
					int ctMapDimensions = listMap.size();
					if( ctMapDimensions != ctDimensions ){
						sbError.append("Inconsistent mapping information; has " + ctMapDimensions + " dimensions where actual count is " + ctDimensions);
						return false;
					}
					for(int xMap = 0; xMap < listMap.size(); xMap++ ){
						Object oMap = listMap.get(xMap);
						if( oMap instanceof DArray ){
							DArray darrayMapInfo = (DArray)oMap;
							DArrayDimension dimMap = darrayMapInfo.getFirstDimension();
							DArrayDimension dimActual = (DArrayDimension)listDimensions.get(xMap);
							if( dimActual.getSize() == dimMap.getSize() ){
								// map matches actual array for this dimension
							} else {
								sbError.append("Inconsistent mapping information; has " + dimMap.getSize() + " elements in the " + xMap + " dimension where actual size is " + dimActual.getSize());
								return false;
							}
						} else {
							sbError.append("Map " + xMap + " is not a DArray");
							return false;
						}
					}
				}

				// determine row count
				int xDimension = 1;
				int iRowCount = 1;
				if( ctDimensions == 1 ){
					iRowCount = ((DArrayDimension)listDimensions.get(xDimension-1)).getSize();
				} else {
					for(; xDimension <= ctDimensions - 1; xDimension++ ){
						int lenDimension = ((DArrayDimension)listDimensions.get(xDimension-1)).getSize();
						iRowCount *= lenDimension;
					}
				}

				// determine data type (Int16, Int32, Float32, Float64)
				String sTypeName;
				PrimitiveVector pvector = darray.getPrimitiveVector();
				if( pvector instanceof Int16PrimitiveVector ){
					sTypeName = "Int16";
				} else if( pvector instanceof Int32PrimitiveVector ){
					sTypeName = "Int32";
				} else if( pvector instanceof Float32PrimitiveVector ){
					sTypeName = "Float32";
				} else if( pvector instanceof Float64PrimitiveVector ){
					sTypeName = "Float64";
				} else {
					sTypeName = "?";
				}

				// output header {"table name" RowCount: iRowCount ColumnCount: iFieldCount DimColumns: "dim 1" "dim 2" ... "dim n-1" ValColumns: "val" 1 3 5 7 ... n ColumnTypes: String Int32 ...}
				sbOut.setLength(0);
				sbOut.append("{\"" + sVariableName + "\" RowCount: " + iRowCount + " ColumnCount: " + iFieldCount + " DimColumns:"); // todo double escape quotation marks
				xDimension = 1;
				StringBuffer sbColumnTypes = new StringBuffer(80);
				if( ctDimensions == 1 ){
					DArrayDimension dim = ((DArrayDimension)listDimensions.get(0));
					String sDimensionName = dim.getName();
					if( sDimensionName == null ){
						sbOut.append(" \"Index\"");
					} else {
						sbOut.append(" \"" + sDimensionName + "\"");
					}
					sbColumnTypes.append(" Int32");
				} else {
					for(; xDimension <= ctDimensions - 1; xDimension++ ){
						DArrayDimension dimCurrent = ((DArrayDimension)listDimensions.get(xDimension-1));
						String sDimensionName = dimCurrent.getName();
						if( sDimensionName == null ){
							sbOut.append(" \"Dim_" + xDimension + "\"");
						} else {
							sbOut.append(" \"" + sDimensionName + "\"");
						}
						sbColumnTypes.append(" Int32");
					}
				}
				sbOut.append(" ValColumns: ");
				String sLastDimName = dimensionLast.getName();
				if( ctDimensions == 1 ){
					sbOut.append("\"Value\" \"" + sVariableName + "\"");
					sbColumnTypes.append(" " + sTypeName);
				} else {
					if( sLastDimName == null ){
						sLastDimName = "Dim_" + ctDimensions;
					}
					sbOut.append("\"" + sLastDimName + "\"");
					int xDimIndex = iStart;
					while(true){
						sbOut.append(" " + xDimIndex);
						sbColumnTypes.append(" " + sTypeName);
						xDimIndex += iStride;
						if( xDimIndex >= iStop ){
							sbOut.append(" " + iStop);
							sbColumnTypes.append(" " + sTypeName);
							break;
						}
					}
				}
				sbOut.append(" ColumnTypes:" + sbColumnTypes);
				sbOut.append("}");
				os.write(sbOut.toString().getBytes());

				// output values {dim_index 1 ... val1.1 val1.2 val1.3 ... val1.n}{dim_index1... val2.1 val2.2 val2.3 ... val2.n} etc.
				int[] aiMapIndexes = new int[ctDimensions]; // stores where you are in the array as you recurse
				int[] aiDimLengths = new int[ctDimensions];
				for( xDimension = 1; xDimension <= ctDimensions; xDimension++ ){
					DArrayDimension d = (DArrayDimension)listDimensions.get(xDimension-1);
					aiDimLengths[xDimension-1] = d.getSize();
				}
				PrintWriter pw = new PrintWriter(os);
				if( ctDimensions == 1 ){
					array_OutputList(listMap, pvector, pw, aiDimLengths[0]);
				} else {
					array_Output(listMap, aiMapIndexes, ctDimensions - 1, pvector, pw, 0, ctDimensions, aiDimLengths, 0);
				}
				pw.println("{/\"" + sVariableName + "\"}");
				pw.flush();
				return true;
			} catch(java.net.SocketException exSocket) {
				sbError.append("Socket error [" + exSocket + "]. Client may have closed connection.");
				return false;
			} catch(Exception ex) {
				Utility.vUnexpectedError(ex, sbError);
				return false;
			}
		}

		private void array_OutputList(ArrayList listMap, PrimitiveVector pvector, PrintWriter pw, int iDimLength) {
			DArray darrayMap = (DArray)listMap.get(0);
			for(int xDim = 0; xDim < iDimLength; xDim++) {
				pw.print("{");
				PrimitiveVector pvMap = darrayMap.getPrimitiveVector();
				pvMap.printSingleVal(pw, xDim);
				pw.print(" ");
				pvector.printSingleVal(pw, xDim);
				pw.print("}");
			}
		}

		private int array_Output(ArrayList listMap, int[] aiMapIndexes, int ctDimColumns, PrimitiveVector pvector, PrintWriter pw, int index, int dims, int aiDimLengths[], int offset) {
			if (dims == 1) {
				pw.print("{");
				for( int xDim = 1; xDim <= ctDimColumns; xDim++ ){
					DArray darrayMap = (DArray)listMap.get(xDim-1);
					PrimitiveVector pvMap = darrayMap.getPrimitiveVector();
					pvMap.printSingleVal(pw, aiMapIndexes[xDim]);
					pw.print(" ");
				}
				for(int i=0; i<aiDimLengths[offset]-1; i++) {
					pvector.printSingleVal(pw, index++);
					pw.print(" ");
				}
				pvector.printSingleVal(pw, index++);
				pw.print("}");
				if( ctDimColumns > 0 ){
					int xCurrentColumn = ctDimColumns;
					while(true){
						aiMapIndexes[xCurrentColumn]++;
						if( aiMapIndexes[xCurrentColumn] < aiDimLengths[xCurrentColumn - 1] ) break;
						aiMapIndexes[xCurrentColumn] = 0;
						if( xCurrentColumn == 1 ) break; // should not happen
						xCurrentColumn--;
						continue;
					}
				}
				return index;
			} else {
				for(int i=0; i<aiDimLengths[offset]-1; i++) {
					index = array_Output(listMap, aiMapIndexes, ctDimColumns, pvector, pw, index, dims-1, aiDimLengths, offset+1);
				}
				index = array_Output(listMap, aiMapIndexes, ctDimColumns, pvector, pw, index, dims-1, aiDimLengths, offset+1);
				return index;
			}
		}

		public void vOutput_ASCII(DodsURL url, OutputStream os){
			String sFullURL = url.getFullURL();
			StringBuffer sbError = new StringBuffer(80);
			DataDDS datadds = getDataDDS(url, mActivity, sbError);
			if( datadds == null ){
				if( sbError.length() > 0 )
					ApplicationController.vShowError("Failed to get data DDS for ASCII output: " + sbError);
				return;
			}
			try {
				String sDataHeader = "[ODC BEGIN: " + sFullURL + "]\n";
				String sDataFooter = "[ODC END: " + sFullURL + "]\n";
				os.write(sDataHeader.getBytes());
				datadds.printVal(os);
				os.write(sDataFooter.getBytes());
				ApplicationController.vShowStatus("Download complete from " + sFullURL);
			} catch(java.net.SocketException exSocket) {
				ApplicationController.vShowError("Socket error [" + exSocket + "]. Client may have closed connection.");
				return;
			} catch(Exception ex) {
				Utility.vUnexpectedError(ex, sbError);
				ApplicationController.vShowError(sbError.toString());
			}
		}

		/* 1.4.1 version
		public void vOutput_Raw(DodsURL url, OutputStream os){
			StringBuffer sbError = new StringBuffer(80);
			java.net.HttpURLConnection connection = null;
			ReadableByteChannel rbcSource = null;
			WritableByteChannel wbcDestination = null;
			try {
				String sFullURL = url.getFullURL();
				if( Utility.isDodsURL(sFullURL) ){
					sFullURL = Utility.sDetemineDodsRawPath(sFullURL, sbError);
					if( sFullURL == null ){
						ApplicationController.vShowError("Unable to determine raw path from DODS URL " +  url.getFullURL() + ": " + sbError);
						return;
					}
				}
				URL httpurl = new URL(sFullURL);
				ApplicationController.vShowStatus("Fetching " + sFullURL + "...");
				connection = (HttpURLConnection)httpurl.openConnection();
				int iContentLength = connection.getContentLength();
				int iResponseCode = connection.getResponseCode();
				if( iResponseCode == HttpURLConnection.HTTP_OK ){
					ApplicationController.vShowStatus("Connection established to " + sFullURL);
				} else {
					ApplicationController.vShowError("Fetch failed with HTTP response code " + iResponseCode);
					return;
				}
				InputStream isSource = connection.getInputStream();
				rbcSource = Channels.newChannel(isSource);
				wbcDestination = Channels.newChannel(os);
				if( Utility.zChannelCopy(rbcSource, wbcDestination, sbError) ){
					ApplicationController.vShowStatus("Download complete from " + sFullURL);
				} else {
					ApplicationController.vShowError("Download from " + sFullURL + " failed: " + sbError.toString());
				}
			} catch(Exception ex) {
				Utility.vUnexpectedError(ex, sbError);
				ApplicationController.vShowError(sbError.toString());
			} finally {
				try {
					if( connection != null ) connection.disconnect();
				} catch(Exception ex) {
					ApplicationController.vShowWarning("Error disconnecting: " + ex);
				}
				try {
					if( wbcDestination != null ) wbcDestination.close();
				} catch(Exception ex) {
					ApplicationController.vShowWarning("Error closing output channel: " + ex);
				}
			}
		}
		--- end 1.4.1 version --- */

		public void vOutput_Raw(DodsURL url, OutputStream os){
			StringBuffer sbError = new StringBuffer(80);
			java.net.HttpURLConnection connection = null;
			try {
				String sFullURL = url.getFullURL();
				if( Utility.isDodsURL(sFullURL) ){
					sFullURL = Utility.sDetemineDodsRawPath(sFullURL, sbError);
					if( sFullURL == null ){
						ApplicationController.vShowError("Unable to determine raw path from DODS URL " +  url.getFullURL() + ": " + sbError);
						return;
					}
				}
				URL httpurl = new URL(sFullURL);
				ApplicationController.vShowStatus("Fetching " + sFullURL + "...");
				connection = (HttpURLConnection)httpurl.openConnection();
				int iContentLength = connection.getContentLength();
				int iResponseCode = connection.getResponseCode();
				if( iResponseCode == HttpURLConnection.HTTP_OK ){
					ApplicationController.vShowStatus("Connection established to " + sFullURL);
				} else {
					ApplicationController.vShowError("Fetch failed with HTTP response code " + iResponseCode);
					return;
				}
				InputStream isSource = connection.getInputStream();
				while(isSource.available() > 0){
					os.write(isSource.read());
				}
// 1.4.1 only
//				if( Utility.zChannelCopy(rbcSource, wbcDestination, sbError) ){
//					ApplicationController.vShowStatus("Download complete from " + sFullURL);
//				} else {
//					ApplicationController.vShowError("Download from " + sFullURL + " failed: " + sbError.toString());
//				}
			} catch(Exception ex) {
				Utility.vUnexpectedError(ex, sbError);
				ApplicationController.vShowError(sbError.toString());
			} finally {
				try {
					if( connection != null ) connection.disconnect();
				} catch(Exception ex) {
					ApplicationController.vShowWarning("Error disconnecting: " + ex);
				}
// 1.4.1 only
//				try {
//					if( wbcDestination != null ) wbcDestination.close();
//				} catch(Exception ex) {
//					ApplicationController.vShowWarning("Error closing output channel: " + ex);
//				}
			}
		}

	}

	private boolean zAddToPlotter_Invoked( DodsURL url, StringBuffer sbError ){
		return ApplicationController.getInstance().getAppFrame().zAddDataToPlotter_Invoked( url, sbError );
	}

	private void loadURLToPlotter( final DodsURL url, final javax.swing.JButton jbuttonActivator, java.awt.event.ActionListener action, final Continuation_DoCancel con){
		try {
			if( url == null ){
				ApplicationController.vShowError("Unable to load, no URL provided.");
				return;
			}
			final Activity activity = new Activity();
			final OpendapConnection connection = new OpendapConnection();
			activity.vDoActivity(
			    jbuttonActivator,
			    action,
				new Continuation_DoCancel(){
				    public void Do(){
						try {
							int iURLType = url.getType();
							if( iURLType != DodsURL.TYPE_Data ){
								ApplicationController.vShowError("Cannot load; not a data URL");
								return;
							}
							String sBaseURL = url.getBaseURL();
							String sCE      = url.getConstraintExpression_Encoded();
							String sSubsettedURL = url.getFullURL();
							if( sSubsettedURL == null ){
								ApplicationController.vShowError("Cannot load; no associated URL string");
								return;
							}
							StringBuffer sbError = new StringBuffer(120);
							connection.setUserAgent(ApplicationController.getInstance().getVersionString());

                            // get data
							DataDDS dataDDS = connection.getDataDDS(sBaseURL, sCE, OutputEngine.this, activity, sbError);
							if( dataDDS == null ){
								if( sbError.length() > 0 )
									ApplicationController.vShowError("Error getting data for " + sSubsettedURL + " at " + mTotalBytes +": " + sbError);
								return;
							} else {
								ApplicationController.vShowStatus("Received " + Utility.getByteCountString(mTotalBytes) + " (" + mTotalBytes + ") for " + sSubsettedURL);
							}
							url.setData(dataDDS);

                            // get DAS (for axes information), ignore failure
                            DAS das = connection.getDAS(sBaseURL, sCE, activity, sbError);
                            if( das == null ){
                                // do nothing
                            } else {
                                url.setDAS(das);
                            }

							javax.swing.SwingUtilities.invokeLater(
								new Runnable(){
									public void run(){
										StringBuffer sbAddError = new StringBuffer(80);
										if( OutputEngine.this.zAddToPlotter_Invoked(url, sbAddError) ){
											if( con != null ) con.Do();
										} else {
											ApplicationController.vShowError("Failed to add data URL to plotter: " + sbAddError);
										}
									}
								}
							);
							return;
					   } catch(Exception ex) {
						   ApplicationController.vShowError("Unexpected error loading: " + ex);
						   if( con != null ) con.Cancel();
					   }
				   }
				   public void Cancel(){
					   if( con != null ) con.Cancel();
				   }
				}, "Loading data for " + url.getTitle() + " (" + url.getFullURL() + ")"
			);
		} catch(Exception ex) {
			StringBuffer sbError = new StringBuffer("Unexpected error loading: ");
			Utility.vUnexpectedError( ex, sbError );
			ApplicationController.vShowError(sbError.toString());
			if( con != null ) con.Cancel();
		}
	}

}

class OutputProfile {

	public final static int TARGET_StandardOut = 1;
	public final static int TARGET_File = 2;
	public final static int TARGET_ViewText = 4;
	public final static int TARGET_ViewImage = 8;
	public final static int TARGET_ViewTable = 16;
	public final static int TARGET_Plotter = 32;
	public final static int FORMAT_Data_Raw = 1;
	public final static int FORMAT_Data_DODS = 2;
	public final static int FORMAT_Data_ASCII_text = 3;
	public final static int FORMAT_Data_ASCII_records = 4;
	public final static int FORMAT_Info = 5;
	public final static int FORMAT_URLs = 6;
	public final static int FORMAT_IDL = 7;
	private OutputStream mos;
	private DodsURL[] maurls; // 0-based
	private int[] maiTarget;
	private int[] maiFormat;
	private String[] masTargetOption; // file name
	private String msTerminator; // string to be appended to output
	OutputProfile( DodsURL[] urls, OutputStream os, int iFormat, String sTerminator){
		maurls = urls;
		maiTarget = new int[urls.length];
		maiFormat = new int[urls.length];
		masTargetOption = new String[urls.length];
		for( int xURL = 0; xURL < urls.length; xURL++ ){
			maiTarget[xURL] = 0;
			masTargetOption[xURL] = null;
			maiFormat[xURL] = iFormat;
		}
		mos = os;
		msTerminator = sTerminator;
	}
	OutputProfile( DodsURL url, int iTarget, String sTargetOption, int iFormat, OutputStream os ){
		maurls = new DodsURL[1];
		maiTarget = new int[1];
		maiFormat = new int[1];
		masTargetOption = new String[1];
		maurls[0] = url;
		maiTarget[0] = iTarget;
		masTargetOption[0] = sTargetOption;
		maiFormat[0] = iFormat;
		mos = os;
	}
	OutputProfile( DodsURL url, int iTarget, String sTargetOption, int iFormat ){
		maurls = new DodsURL[1];
		maiTarget = new int[1];
		maiFormat = new int[1];
		masTargetOption = new String[1];
		maurls[0] = url;
		maiTarget[0] = iTarget;
		masTargetOption[0] = sTargetOption;
		maiFormat[0] = iFormat;
	}
	OutputProfile( DodsURL[] urls, int iTarget, int iFormat, String[] asTargetOption, OutputStream os ){
		maurls = urls;
		maiTarget = new int[urls.length];
		maiFormat = new int[urls.length];
		for( int xURL = 0; xURL < urls.length; xURL++ ){
			maiTarget[xURL] = iTarget;
			maiFormat[xURL] = iFormat;
		}
		masTargetOption = asTargetOption;
		mos = os;
	}
	OutputProfile( DodsURL[] urls, int[] aiTarget, int[] aiFormat, String[] asTargetOption, OutputStream os ){
		maurls = urls;
		maiTarget = aiTarget;
		maiFormat = aiFormat;
		masTargetOption = asTargetOption;
		mos = os;
	}
	OutputProfile( DodsURL[] urls, int iTarget, String sTargetOption, int iFormat ){
		maurls = urls;
		maiTarget = new int[urls.length];
		maiFormat = new int[urls.length];
		masTargetOption = new String[urls.length];
		for( int xURL = 0; xURL < urls.length; xURL++ ){
			maiTarget[xURL] = iTarget;
			masTargetOption[xURL] = sTargetOption;
			maiFormat[xURL] = iFormat;
		}
	}
	String getTerminator(){ return msTerminator; }
	int getTarget(int x){ if( maiTarget == null ) return 0; else return maiTarget[x-1]; }
	String getTargetOption(int x){ if( masTargetOption == null ) return null; else return masTargetOption[x-1]; }
	int getFormat(int x){ if( maiFormat == null ) return 0; else return maiFormat[x-1]; }
	int getURLCount(){ if( maurls == null ) return 0; else return maurls.length; }
    DodsURL getURL(int x){ if( maurls == null ) return null; else return maurls[x-1]; }
	OutputStream getTargetOS(){ return mos; }
	public static String sTargetDescription( final int TARGET ){
		switch( TARGET ){
			case TARGET_StandardOut: return "Standard Out";
			case TARGET_File: return "File";
			case TARGET_ViewText: return "View Text";
			case TARGET_ViewImage: return "View Image";
			default: return "[unknown]";
		}
	}
	public static String sFormatDescription( final int FORMAT ){
		switch( FORMAT ){
			case FORMAT_Data_Raw: return "Raw";
			case FORMAT_Data_DODS: return "DODS";
			case FORMAT_Data_ASCII_text: return "ASCII Text";
			case FORMAT_Data_ASCII_records: return "ASCII Records";
			case FORMAT_Info: return "Info";
			case FORMAT_URLs: return "URL";
			case FORMAT_IDL: return "IDL";
			default: return "[unknown]";
		}
	}
	public String toString(){
		return "" + maurls.length + " URLs " + maurls[0] + " target: " + sTargetDescription(maiTarget[0]) + " format: " + sFormatDescription(maiFormat[0]) + " option: " + masTargetOption[0];
	}

}




