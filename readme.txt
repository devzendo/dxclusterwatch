Very rough notes.

Need to create a cacerts file that has the certificate for dxcluster.co.uk

See http://stackoverflow.com/questions/6659360/how-to-solve-javax-net-ssl-sslhandshakeexception-error

Save certificate from a browser to the dxcluster.co.uk file

keytool -import -file dxcluster.co.uk -alias dxcluster -keystore <path to cacerts file>

This path will be the ~/dxclusterwatch directory.

The configuration is in the ~/dxclusterwatch/dxclusterwatch.properties file, and it has the following properties:
callsigns= (a comma-separated list of callsigns to watch for (i.e. the dx callsign))
siteRepoPath= (a path to a cloned repo of a 'pages' site, e.g. bitbucket pages.
pollMinutes= (how many minutes between dxcluster.co.uk polls, suggest 2 mins)
 