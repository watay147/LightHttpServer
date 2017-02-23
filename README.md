# LightHttpServer
A light HTTP server in JAVA

## Features
- Multi-threading service and connections queue
- Complete request line and headers parsing
- Basic access control and MIME types mapping
- Access to static files supported, including corresponding HTTP cache mechanism
- Dynamically generated responses from CGI Scripts supported
- Cookie supported
- Configurable in XML

## Demo Usage
- Make sure java and python(required only for the demo CGI script) had been installed in your system and configured properly
- Execute command ```java -jar LightHttpServer.jar``` under "demo" folder
- Use a brower to access ```127.0.0.1:8800``` or any other proper url
- Feel free to modify the config.xml to configure the server

## Config Description
- max_threads: the max number of working threads
- thread_idle_time: the time for waiting a idle thread to be killed
- waiting_queue_size: the size of the connections in the waiting queue
- port: the listening port of the server
- host_name: the host name of the server
- host: for setting the configs of a host(multiple virtual hosts are waiting for implement)
    - index_file: the file name of the default index file
    - document_root_directory_path: the path of the root document directory
    - cgi
        - url_alias: the url alias of CGI scripts
        - document_path: the physical documents path for mapping the url alais of CGI scripts
    - headers: for setting the configs of the headers respect to specific url prefixes
        - set: each url prefix is configured by a set
            -  url_alias: the url prefix for setting the headers
            - header: a header is a unit for setting the headers
                - key: header's key
                - value: header's value

