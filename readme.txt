Very rough notes.

Configuration storage
This is in ~/dxclusterwatch, and is a properties file, dxclusterwatch.properties.

mkdir ~/dxclusterwatch

Also in this directory is a cacerts file that has the certificate for dxcluster.co.uk,
so copy the JRE's cacerts, and add the new certificate to it, copying the new cacerts
into the ~/dxclusterwatch/cacerts file. Use the default passphrase of changeit, as in:

See http://stackoverflow.com/questions/6659360/how-to-solve-javax-net-ssl-sslhandshakeexception-error

1. Save certificate from a browser to the dxcluster.co.uk file

2. cp /Library/Java/JavaVirtualMachines/jdk1.7.0_75.jdk/Contents/Home/jre/lib/security/cacerts ~/dxclusterwatch/cacerts
3. keytool -import -file dxcluster.co.uk -alias dxcluster -keystore ~/dxclusterwatch/cacerts

This path will be the ~/dxclusterwatch directory.

The configuration is in the ~/dxclusterwatch/dxclusterwatch.properties file, and it has the following properties:
callsigns= (a comma-separated list of callsigns to watch for (i.e. the dx callsign))
siteRepoPath= (a path to a cloned repo of a 'pages' site, e.g. bitbucket pages.
pollMinutes= (how many minutes between dxcluster.co.uk polls, suggest 2 mins)
tweetSeconds= (how many seconds between posting tweets, suggest 30 secs)
hgExecutablePath= (path to the hg executable on your system)
consumerKey= (twitter API Oauth consumer key)
consumerSecret= (twitter API Oauth consumer secret)
accessToken= (twitter API access token)
accessSecret= (twitter API access secret)
maxListingEntries= (maximum size of the report listing; most recent listings first, but only up to this many)
enableFeedReading= (whether to enable feed reading, yes/true, no/false)
enablePageUpdating= (whether to update/publish the page)
enableTweeting= (whether to enable tweeting)
serverURI= (server URI, typically https://www.dxcluster.co.uk/index.php?/api/all)

Things to change for next run:
2) add crowbars: disable flags in the config for
   a) reading from the API (done)
   b) updating the webpage (done)
   c) posting to twitter
3) fix the init.d script to actually stop the process (not sure why this didn't work)
4) package as .deb
5) add a deploy-to-host script; no need to go the full Ansible here... 
6) make twitter content better; not just an ongoing unprocessed stream of data from the cluster.
   possibly:
   tweet every N minutes if any new IMD callsigns heard with their frequencies, and if any have changed
   frequencies (possibly show all frequencies a callsign has been heard on, in the last 15 mins?)
   1305: GB8MD on 14074,10118,7101; CW2IMD on 7109,3582
   i.e. maintain a set of active stations, their frequencies. stations and frequencies can come and go
   in those sets; tweet the changes. keep tweets short, possibly without comments and who heard them.
   
7) similar content for the web page, but can show more info.
   1305: GB8MD on 14074 by M0CUV "Receive-only Marconi station between 1914-1921"
                  10118 by G7AGI "Solid copy, OM"
                  7011 by G0POT "Fine QRS QSO, OM!"
         CW2IMD on 7109 by IY0ABC "Fortissimo!"
                   3582 by W1ALU "Great event station"

8) also a map of all stations and the frequencies they've been heard on (with last heard time for that frequency)
9) go over the log file for the day, downgrade useless diagnostics.
10) reconstruct the data feed from log.
11) create a non-bitbucket publisher: upload to SFTP server.
                   
                   
                   
   