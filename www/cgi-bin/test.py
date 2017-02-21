import os,cgi
import Cookie
form = cgi.FieldStorage()
print "Set-Cookie: xx=5825"
print "Set-Cookie: ss=55"
print "Set-Cookie: xx=5888"
print "Content-type: text/html\n";  
print "<html><body><h1>cgi got!</h1>"
print "<p> QUERY_STRING:"+os.environ.get( "QUERY_STRING")+"</p>"

print "<p> Post:hh="+form.getvalue("hh")+"</p><br>"
if 'HTTP_COOKIE' in os.environ:
    cookie_string=os.environ.get('HTTP_COOKIE')
    c=Cookie.SimpleCookie()
    c.load(cookie_string)

    try:
        data=c['xx'].value
        print "cookie data: "+data+"<br>"
        print "cookie data: "+c['ss'].value+"<br>"
    except KeyError:
        print "cookie not set<br>"
else:
    print "no cookie"
print "</body><html>"