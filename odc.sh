#!/bin/sh
#
# Run the ODC

#profiler="-javaagent:/Users/jimg/src/jip-src-1.2/profile/profile.jar \
#-Dprofile.properties=/Users/jimg/src/olfs/resources/metacat/profile.properties"
export CLASSPATH=lib/commons-httpclient-3.0.1.jar:lib/geotrans2.jar:lib/gnu-regexp-1.1.4.jar:lib/jaf-1.1_activation.jar:lib/javamail-1.4_mail.jar:lib/javamail-1.4_mailapi.jar:lib/jdom-1.0.jar:lib/jogl.jar:lib/jython.jar:lib/netcdf-4.0.jar:lib/opendap-0.0.9.jar:tools/IPSClient.jar:./odc.jar

# then in the 'dist' directory, run as

java opendap.clients.odc.ApplicationController

# java $profiler -Xms512m -Xmx2048m -jar ../libexec/EMLWriter.jar $*
