Very rough notes.

Need to create a cacerts file that has the certificate for dxcluster.co.uk

See http://stackoverflow.com/questions/6659360/how-to-solve-javax-net-ssl-sslhandshakeexception-error

Save certificate from a browser to the dxcluster.co.uk file

keytool -import -file dxcluster.co.uk -alias dxcluster -keystore <path to cacerts file>

This path will be the ~/dxclusterwatch directory.

