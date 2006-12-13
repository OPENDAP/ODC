#include <windows.h>
#include <string>
#include <iostream>
#include <fstream>
#include <sstream>
#include "jni.h"
using namespace std;

void vShowError(string sErrorMessage);
void vShowLastError(string sErrorMessage);
void vShowMessage(string sMessage);
void vDestroyVM(JNIEnv *env, JavaVM *jvm);
void vAddOption(const string& sName);

JavaVMOption* vm_options;
int mctOptions = 0;
int mctOptionCapacity = 0;

jboolean GetApplicationHome(char *buf, jint sz);
bool zStartupConfiguration_Load( string sAppHome, string sFilePath );
string sConfig_getJREPath();
string sConfig_getRuntimePath();
string sConfig_getJVMPath();
string sConfig_getBootPath();
string sConfig_getOptionBootPath();
string sConfig_getClassPath();
string sConfig_getOptionClassPath();
string sConfig_getStartupClass();
string sConfig_getApplicationParameters();
int iConfig_getApplicationParameterCount();
string sConfig_getApplicationParameter( int iIndex ); 
int iConfig_getOptionCount();
string sConfig_getOption( int iOptionIndex );

typedef jint (CALLBACK *CreateJavaVM)(JavaVM **pvm, JNIEnv **penv, void *args);

int WINAPI WinMain(HINSTANCE hInstance, HINSTANCE hPrevInstance, char* sCommandLine, int iCmdShow) {
//int main(int __argc, char* __argv[]){ //for a console or unix application (useful for debugging)

	string STARTUP_FILE = "startup.ini";

	JNIEnv *env;
	JavaVM *jvm;
	jint jintVMStartupReturnValue;
	jclass jclassStartup;
	jmethodID midStartup;
	
	// Check Startup Parameters (note Microsoft Compilers set __argc and _argv automatically)
	bool zVerbose = false;
	for( int xCommandArg = 0; xCommandArg < __argc; xCommandArg++ ){
		if( strcmp( __argv[xCommandArg], "-verbose" ) == 0 ) zVerbose = true;
	}
	if( zVerbose ){
		vShowMessage("1 starting");
	}

	// Path Determination

	// --- application home
	char home[2000];
	if (!GetApplicationHome(home, sizeof(home))) {
		vShowError("Unable to determine application home.");
		return 0;
	}
	string sAppHome(home);
	if( zVerbose ){
		vShowMessage("2 application home:" + sAppHome);
	}

	string sStartupConfigurationFile = sAppHome + "\\" + STARTUP_FILE; // windows file separator
	if (!zStartupConfiguration_Load(sAppHome, sStartupConfigurationFile)) {
		vShowError("Unable to find startup configuration file: " + sStartupConfigurationFile);
		return 0;
	}
	if( zVerbose ){
		vShowMessage("3 startup configuration file:" + sStartupConfigurationFile);
	}

	string sJVMPath = sConfig_getJVMPath();
	string sOption_AppHome = "-Dapplication.home=" + sAppHome;
	string sOption_BootPath = sConfig_getOptionBootPath();
	string sOption_ClassPath = sConfig_getOptionClassPath();
	string sOption_VM_Error = "-XX:+ShowMessageBoxOnError";
	string sOption_CheckJNI = "-Xcheck:jni";
	string sOption_VerboseJNI = "-verbose:jni"; /* print JNI-related messages */

	if( zVerbose ){
		vShowMessage("4 jvm path:" + sJVMPath);
	}

	// add fixed options
	// vAddOption(sOption_BootPath); // this has to have all the jars specified
	vAddOption(sOption_ClassPath);
	vAddOption(sOption_AppHome);
	// vAddOption(sOption_VM_Error);
	// vAddOption(sOption_CheckJNI);
	// vAddOption(sOption_VerboseJNI);

	// add user-defined options
	for( int xOption = 1; xOption <= iConfig_getOptionCount(); xOption++ ){
		vAddOption(sConfig_getOption(xOption));
		if( zVerbose ){
			vShowMessage("5 user option:" + sConfig_getOption(xOption));
		}
	}

	// initialize args
	JavaVMInitArgs vm_args;
	vm_args.version = JNI_VERSION_1_2;
	vm_args.options = vm_options;
	vm_args.nOptions = mctOptions;
	vm_args.ignoreUnrecognized = JNI_TRUE;

   // load jvm library
   if( zVerbose ){
	   vShowMessage("6 loading jvm library");
   }
   HINSTANCE hJVM = LoadLibrary(sJVMPath.c_str());
   if( hJVM == NULL ){
      vShowLastError("Failed to load JVM from " + sJVMPath);
      return 0;
   }

   // try to start 1.2/3/4 VM
   // uses handle above to locate entry point
   if( zVerbose ){
	   vShowMessage("7 determining jvm entry point");
   }
   CreateJavaVM lpfnCreateJavaVM = (CreateJavaVM)GetProcAddress(hJVM, "JNI_CreateJavaVM"); 
   if( zVerbose ){
	   vShowMessage("8 starting jvm");
   }
   jintVMStartupReturnValue = (*lpfnCreateJavaVM)(&jvm, &env, &vm_args);

   // try to start 1.2/3/4 VM -- UNIX STYLE
   // jintVMStartupReturnValue = JNI_CreateJavaVM(&jvm, (void**)&env, &vm_args);

   // test for success
   if (jintVMStartupReturnValue < 0) {
      string sErrorMessage = "Unable to create VM.";
      vShowError(sErrorMessage);
      vDestroyVM(env, jvm);
      return 0;
   }

   // find startup class
   string sStartupClass = sConfig_getStartupClass(); 
   if( zVerbose ){
	   vShowMessage("9 startup class: " + sStartupClass);
   }
   if( sStartupClass == "" ){
      string sErrorMessage = 
        "No startup class defined in startup.ini";
      vShowError(sErrorMessage);
      vDestroyVM(env, jvm);
      return 0;
   }
   jclassStartup = 
      env->FindClass(sStartupClass.c_str());
   if( jclassStartup == NULL ) {
      string sErrorMessage = 
        "Unable to find startup class [" + 
        sStartupClass + "] in classpath " + sOption_ClassPath;
      vShowError(sErrorMessage);
      vDestroyVM(env, jvm);
      return 0;
   }

   // find startup method
   string sStartupMethod_Identifier = "main";
   string sStartupMethod_TypeDescriptor = 
     "([Ljava/lang/String;)V";
   if( zVerbose ){
	   vShowMessage("10 startup method: " + sStartupMethod_Identifier + " " + sStartupMethod_TypeDescriptor);
   }
   midStartup = 
     env->GetStaticMethodID(jclassStartup,
      sStartupMethod_Identifier.c_str(),
      sStartupMethod_TypeDescriptor.c_str());
   if (midStartup == NULL) {
      string sErrorMessage = 
      "Unable to find startup method [" 
      + sStartupClass + "." 
      + sStartupMethod_Identifier 
      + "] with type descriptor [" + 
     sStartupMethod_TypeDescriptor + "]";
      vShowError(sErrorMessage);
      vDestroyVM(env, jvm);
      return 0;
   }

   // create array of args to startup method
   jstring jstringCurrentArg;
   jclass jclassString;
   jobjectArray jobjectArray_args;
   jclassString = env->FindClass("java/lang/String");
   int ctJavaArgs = iConfig_getApplicationParameterCount();
   jobjectArray_args = (jobjectArray)env->NewObjectArray(ctJavaArgs, jclassString, NULL);
   int xJavaArg;
   for( xJavaArg = 1; xJavaArg <= ctJavaArgs; xJavaArg++ ){
	   jstringCurrentArg = env->NewStringUTF(sConfig_getApplicationParameter(xJavaArg).c_str());
	   if (jstringCurrentArg == NULL){
          continue;
	   }
	   env->SetObjectArrayElement(jobjectArray_args, xJavaArg, (jobject)jstringCurrentArg);
	   if( zVerbose ){
		   vShowMessage("11 application arg:" + sConfig_getApplicationParameter(xJavaArg));
	   }
   }

   // call the startup method - 
   // this starts the Java program
   if( zVerbose ){
	   vShowMessage("12 calling startup method");
   }
   env->CallStaticVoidMethod(jclassStartup, 
     midStartup, jobjectArray_args);

   // attempt to detach main thread before exiting
   if (jvm->DetachCurrentThread() != 0) {
      vShowError("Could not detach main thread.\n");
   }

   // this call will hang as long as there are 
   // non-daemon threads remaining
   jvm->DestroyJavaVM();

   return 0;

}

void vDestroyVM(JNIEnv *env, JavaVM *jvm)
{
   if (env->ExceptionOccurred()) {
      env->ExceptionDescribe();
   }
   jvm->DestroyJavaVM();
}

void vShowMessage(string sMessage) {
   MessageBox(NULL, sMessage.c_str(), "ODC Launch Message", MB_OK);
}

void vShowError(string sError) {
   MessageBox(NULL, sError.c_str(), "ODC Launch Error", MB_OK);
}


/* Shows an error message in an OK box with the
   system GetLastError appended in brackets */
void vShowLastError(string sLocalError) {
   LPVOID lpSystemMsgBuf;
   FormatMessage(
      FORMAT_MESSAGE_ALLOCATE_BUFFER |
      FORMAT_MESSAGE_FROM_SYSTEM |
      FORMAT_MESSAGE_IGNORE_INSERTS,
    NULL,
      GetLastError(),
    MAKELANGID(LANG_NEUTRAL, SUBLANG_DEFAULT),
      (LPTSTR) &lpSystemMsgBuf,    0,    NULL );
      string sSystemError =
        string((LPTSTR)lpSystemMsgBuf);
    vShowError(sLocalError +
        " [" + sSystemError + "]");
}

void vAddOption(const string& sValue) {	
	mctOptions++;
	if (mctOptions >= mctOptionCapacity) {
		if (mctOptionCapacity == 0) {
			mctOptionCapacity = 3;
			vm_options = (JavaVMOption*)malloc(mctOptionCapacity *  sizeof(JavaVMOption));
		} else {
			JavaVMOption *tmp;
			mctOptionCapacity *= 2;
			tmp = (JavaVMOption*)malloc(mctOptionCapacity * sizeof(JavaVMOption));
			memcpy(tmp, vm_options, (mctOptions-1) * sizeof(JavaVMOption));
			free(vm_options);
			vm_options = tmp;
		}
	}
	vm_options[mctOptions-1].optionString = (char*)sValue.c_str();
}

/* If buffer is "c:\app\bin\java",
 * then put "c:\app" into buf. */
jboolean GetApplicationHome(char *buf, jint sz) {
   char *cp;
   GetModuleFileName(0, buf, sz);
   *strrchr(buf, '\\') = '\0';
   if ((cp = strrchr(buf, '\\')) == 0) {
      // This happens if the application is in a
    // drive root, and there is no bin directory.
      buf[0] = '\0';
      return JNI_FALSE;
   }
   return JNI_TRUE;
}

struct structStartupConfiguration { // defaults as shown
   string sAppHome;
   string sJREPath;             // sAppHome + "\\jre";
   string sRuntimePath;         // sJREPath + \\bin\\classic\\"; // must contain jvm.dll
   string sJVMPath;             // sRuntimePath + "jvm.dll";
   string sBootPath;            // sJREPath + "\\lib";
   string sOption_BootPath;     // "-Dsun.boot.class.path="  + sBootPath;
   string sClassPath;           // sAppHome + "\\classes";
   string sOption_ClassPath;    // "-Djava.class.path= + sClassPath;
   string sStartupClass;
   int ctApplicationParameters;
   string asApplicationParameters[100];
   int ctOptions;
   string asOptions[100];
};

struct structStartupConfiguration mStartupConfiguration;

/* the startup configuration file must be in the application home directory and the path to it 
   must be _MAX_PATH (260) characters long or shorter */
bool zStartupConfiguration_Load( string sAppHome, string sFilePath ){

	mStartupConfiguration.sAppHome = sAppHome;
	mStartupConfiguration.sJREPath = "";             // sAppHome + "\\jre";
	mStartupConfiguration.sRuntimePath = "";         // sJREPath + \\bin\\classic\\"; // must contain jvm.dll
	mStartupConfiguration.sJVMPath = "";             // sRuntimePath + "jvm.dll";
	mStartupConfiguration.sBootPath = "";            // sJREPath + "\\lib";
	mStartupConfiguration.sOption_BootPath = "";     // "-Dsun.boot.class.path="  + sBootPath;
	mStartupConfiguration.sClassPath = "";           // sAppHome + "\\classes";
	mStartupConfiguration.sOption_ClassPath = "";    // "-Djava.class.path= + sClassPath;
	mStartupConfiguration.sStartupClass = "";
	mStartupConfiguration.ctOptions = 0;
	mStartupConfiguration.ctApplicationParameters = 0;

	// load file as string
	ifstream in(sFilePath.c_str());
	// todo make sure file was there
	string sFileText, sFileLine;
	while(getline(in, sFileLine))
		sFileText += sFileLine + "\n";

	// convert string to char array
	size_t iFileSize; // size_t is an ANSI C type for strings and memory blocks
	char* pszFileText;
	iFileSize = sFileText.length();
	pszFileText = (char*)sFileText.c_str();

	// parse the string and populate the config structure
	unsigned long posLineCursor = 0;
	unsigned long posBOL = 0; // beginning of line
	unsigned long posEOL = 0; // end of line
	unsigned long iLineNumber = 1;
	while(true){ // loop through lines
		if( posLineCursor == iFileSize ) break; // all done
		if( pszFileText[posLineCursor] == '\n' || 
			pszFileText[posLineCursor] == '\r' ){ // found end of a line
			posEOL = posLineCursor-1;
			while(true){ // strip any remaining whitespace
				posLineCursor++;
				if( posLineCursor == iFileSize ) break;
				if( pszFileText[posLineCursor] == '\n' || 
					pszFileText[posLineCursor] == '\r' ||
					pszFileText[posLineCursor] == '\t' ||
					pszFileText[posLineCursor] == ' '){ 
					// continue
				} else {
					break;
				}
			}
			if( pszFileText[posBOL] == '#' ){ // line is a comment, skip it
				posBOL = posLineCursor;
				iLineNumber++;
				continue;
			}
			int iState_InName = 1;
			int iState_InValue = 2;
			int iState = iState_InName;
			char bufName[1000];
			char bufValue[5000];
			string sName;
			string sValue;
			int posValueBuffer = 0;
			for( unsigned long posLine = posBOL; posLine <= posEOL; posLine++ ){ // scan the line
				switch( iState ){
					case 1: // in name
						if( pszFileText[posLine] == '=' ){  // end of name
							sName = string(bufName);
							while( true ){ // advance past any leading whitespace
								if( posLine+1 > posEOL ){ // value is blank -- interpret as null
									sValue = "";
									break;
								}
								if( pszFileText[posLine+1] == '\t' || pszFileText[posLine+1] == ' ' ){
									posLine++; 
								} else { // found the start of the value
									posValueBuffer = 0;
									iState = iState_InValue;
									break;
								}
							}
						} else if( pszFileText[posLine] == '\t' || pszFileText[posLine] == ' ' ) {
							// ignore whitespace
						} else { // add character to buffer
							bufName[posLine-posBOL] = pszFileText[posLine];
							bufName[posLine-posBOL+1] = '\0'; // make sure always null terminated
						}
						break;
					case 2: // in value (internal whitespace is not ignored)
						if( pszFileText[posLine] == '$' ){
							if( pszFileText[posLine+1] == '$' ){ // literal $
								bufValue[posValueBuffer] = '$';
								bufValue[posValueBuffer+1] = '\0'; // make sure always null terminated
								posLine++; // advance past escape char
							} else { // insert application home
								size_t lenAppHome = sAppHome.size();
								for( unsigned int posAppHome = 0; posAppHome < lenAppHome; posAppHome++ ){
									bufValue[posValueBuffer] = sAppHome.at(posAppHome);
									posValueBuffer++;
								}
								bufValue[posValueBuffer] = '\0';
							}
						} else {
							bufValue[posValueBuffer] = pszFileText[posLine];
							posValueBuffer++;
							bufValue[posValueBuffer] = '\0'; // make sure always null terminated

						}
						if( posLine == posEOL ){ // end of value
							sValue = string(bufValue);
						}
						break;
				}
			}
			if( sName == "JREPath" ){
				mStartupConfiguration.sJREPath = sValue;
			} else if( sName == "RuntimePath" ){
				mStartupConfiguration.sRuntimePath = sValue;
			} else if( sName == "JVMpath" ){
				mStartupConfiguration.sJVMPath = sValue;
			} else if( sName == "BootPath" ){
				mStartupConfiguration.sBootPath = sValue;
			} else if( sName == "Option_BootPath" ){
				mStartupConfiguration.sOption_BootPath = sValue;
			} else if( sName == "ClassPath" ){
				mStartupConfiguration.sClassPath = sValue;
			} else if( sName == "Option_ClassPath" ){
				mStartupConfiguration.sOption_ClassPath = sValue;
			} else if( sName == "StartupClass" ){
				mStartupConfiguration.sStartupClass = sValue;
			} else if( sName == "ApplicationParameter" ){
				mStartupConfiguration.ctApplicationParameters++;
				mStartupConfiguration.asApplicationParameters[mStartupConfiguration.ctApplicationParameters] = sValue;
			} else if( sName == "Option" ){
				mStartupConfiguration.ctOptions++;
				mStartupConfiguration.asOptions[mStartupConfiguration.ctOptions] = sValue;
			} else {
			      vShowError("Unknown configuration parameter: " + sName);
			}
			posBOL = posLineCursor; // done with this line ready to do another
			iLineNumber++;
		} else {
			posLineCursor++; // keep looking for end of line
		}
	}
	return true;
}

string sConfig_getJREPath(){ 
	if( mStartupConfiguration.sJREPath == "" ) return mStartupConfiguration.sAppHome + "\\jre";
	return mStartupConfiguration.sJREPath; 
}
string sConfig_getRuntimePath(){ 
	if( mStartupConfiguration.sRuntimePath == "" ) return sConfig_getJREPath() + "\\bin\\classic\\";
	return mStartupConfiguration.sRuntimePath; 
}
string sConfig_getJVMPath(){ 
	if( mStartupConfiguration.sJVMPath == "" ) return sConfig_getRuntimePath() + "jvm.dll";
	return mStartupConfiguration.sJVMPath; 
}
string sConfig_getBootPath(){ 
	if( mStartupConfiguration.sBootPath == "" ) return sConfig_getJREPath() + "\\lib";
	return mStartupConfiguration.sBootPath; 
}
string sConfig_getOptionBootPath(){ 
	if( mStartupConfiguration.sOption_BootPath == "" ) return "-Dsun.boot.class.path=" + sConfig_getBootPath();
	return mStartupConfiguration.sOption_BootPath; 
}
string sConfig_getClassPath(){ 
	if( mStartupConfiguration.sClassPath == "" ) return mStartupConfiguration.sAppHome + "\\classes";
	return mStartupConfiguration.sClassPath; 
}
string sConfig_getOptionClassPath(){ 
	if( mStartupConfiguration.sOption_ClassPath == "" ) return "-Djava.class.path=" + sConfig_getClassPath();
	return mStartupConfiguration.sOption_ClassPath; 
}
string sConfig_getStartupClass(){ 
	if( mStartupConfiguration.sStartupClass == "" ) return "";
	return mStartupConfiguration.sStartupClass; 
}
int iConfig_getApplicationParameterCount(){
	return mStartupConfiguration.ctApplicationParameters;
}
string sConfig_getApplicationParameter( int iIndex ){ 
	if( mStartupConfiguration.asApplicationParameters == NULL )	return "";
	return mStartupConfiguration.asApplicationParameters[iIndex]; 
}
int iConfig_getOptionCount(){
	return mStartupConfiguration.ctOptions;
}
string sConfig_getOption( int iOptionIndex ){
	return mStartupConfiguration.asOptions[iOptionIndex];
}

