function [URLs_requested, URLs] = ODCget(arg1)

%-------------------------------------------------------------------
%     Copyright 2004 (C) URI/MIT
%     $Revision$
%
% DESCRIPTION:
%
%   ODCget is a Matlab function that can obtain URLs (Uniform Resource Locations)
%   that have been selected using the OPeNDAP Data Connector, or ODC.  
%    
%   The URLs obtained from the ODC can be used to load data into the workspace using
%   the OPeNDAP Matlab clients loaddods.  The URLs are returned to the workspace in
%   a cell array of strings.  To use them in loaddods you must use char(urls(#)),
%   where # is the index number of the URL you wish to load.
%
%   Before running ODCget in Matlab, it is necessary to be running the ODC application 
%   and to have selected the URL(s) in that application first.
%
% INPUT:
%  echo: a scalar variable indicating whether to echo URLs during retrieval from the ODC.
%
% OUTPUT:
%  [urls_requested, urls]
%  
%  urls_requested: a scalar variable indicating the number of URLs returned.
%  urls: A cell array of strings, each string representing one URL retrieved
%        from the ODC using its 'getselectedurls' command.
%
% EXAMPLES:
%  [count urls] = ODCget(1)
%       -> Returns a scalar variable 'count' that contains the number of URLs 
%          retrieved from the ODC.
%       -> Returns a cell array of strings, 'urls' that contain the URLs
%          retrieved from the ODC
%
% CALLER: general purpose
% CALLEE: ODCget.m
%
% AUTHORS: Xin, URI
%	   Dan Holloway, URI
%---------------------------------------------------------------------

  if nargout > 0
      URLs_requested = 0;
      URLs = [];
  end

% Check for Matlab version

  ver = version;
  verMajor = str2num(ver(1));

  if verMajor <= 5
      disp(' This program requires Matlab version 6 or higher');
      return;
  else  % Check if Java is enabled
      version -java;
      if strcmp(ans,'Java is not enabled') ~=0
          disp('This program required Java enabled');
          return;
      end
  end

% test('clear') will clear all the variables in workspace and start to get variables with request number R1
  echoon = 0;
  if nargin == 1
      echoon = arg1;
  end

  try
      % create instance for java class of opendap.clients.odc.IPSClient.ODCclient
      ODCObject = opendap.clients.odc.IPSClient.ODCclient;  
  catch
      disp('Undefined Java class of opendap.matlab.ODCclient');
      disp('or need to check classpath.txt file for Java class path');
  end

  ODCObject.matlab_vLoadURLs;

  HadError = ODCObject.matlab_hasError;
  if HadError
      javaError = ODCObject.matlab_getError;
      disp(char(javaError))
      idx = 1;
  else
      URL_count = ODCObject.matlab_getURLCount;

      idx = 1;

      for i = 1:URL_count
          javaURL = ODCObject.matlab_getURL(i);
          matlabURL = char(javaURL);
          if echoon
  	     disp(sprintf(' Reading: %s', matlabURL));
          end
	
	  pos = strmatch('command:',matlabURL);
          if isempty(pos) 
              if isempty(URLs)
	         URLs = cellstr(matlabURL);
	      else
	         URLs(idx) = cellstr(matlabURL);
              end
	      idx = idx + 1;
          end
      end
  end
  URLs_requested = idx - 1;

  return

% $Id$

% $Log: ODCget.m,v $
% Revision 1.4  2004/09/29 21:04:26  dan
% Major revisions to ODCget.m.  This now operates as a function
% returning the number of URLs and URLs are return arguments to the
% caller.  The URLs are returned as a cell array of strings.  The
% input argument is a scalar variable to control echoing of the
% URLs as they're retrieved from the ODC IPC server.
%
