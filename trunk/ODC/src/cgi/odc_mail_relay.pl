#!/usr/bin/perl
use CGI;

my $query    = new CGI;

my $param_recipient    = $query->param('send_to');      # example: "odc-help@opendap.org"
my $param_reply_to     = $query->param('reply_to');     # example: "user@school.edu"
my $param_subject      = $query->param('subject');      # example: "[odc client message]"
my $param_body         = $query->param('content');      # example: "I could not get program to start..."

my $sendmail = "/usr/sbin/sendmail -t";                 # sendmail command line

my $send_to  = "To: ".$param_recipient;
my $reply_to = "Reply-to: ".$param_reply_to;
my $subject  = "Subject: ".$param_subject;
my $content  = "Thanks for your submission.";

unless ($param_recipient) {
  print "Error: recipient missing";
  print $query->header;
  exit;
}

open(SENDMAIL, "|$sendmail") or die "Cannot open $sendmail: $!";
print SENDMAIL $reply_to;
print SENDMAIL $subject;
print SENDMAIL $send_to;
print SENDMAIL "Content-type: text/plain\n\n";
print SENDMAIL $content;
close(SENDMAIL);

print "Mail sent";
print $query->header;
    
