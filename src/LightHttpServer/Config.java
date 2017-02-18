package LightHttpServer;

import java.util.HashMap;
import java.util.Map;

public class Config {
	public String httpVersion;
	public String host;
	
	public int port;
	public int coreNum;
	public int maxThreadNum;
	public int keepAliveTime;
	public String documentRootDirectoryPath;
	public String CGIAlias;
	public String CGIPath;
	public String indexFilePath;
	public String serverVersion;
	public Map<String, Map<String, String>> headersForPathMap;
	
	public void loadConfig(){
		httpVersion="HTTP/1.0";
		host="localhost";
		port=8000;
		maxThreadNum=10;
		keepAliveTime=30;
		documentRootDirectoryPath="E:/workspace/www/";
		CGIAlias="/cgi-bin/";
		CGIPath="E:/workspace/www/cgi-bin/";
		indexFilePath="index.html";
		coreNum=Runtime.getRuntime().availableProcessors();
		serverVersion="LightHttpServer/0.1";
		headersForPathMap=new HashMap<String, Map<String,String>>();
		Map<String, String> defaultHeaders=new HashMap<String, String>();
//		defaultHeaders.put("Cache-Control", "max-age = 20");
//		defaultHeaders.put("Cache-Control", "must-revalidate");
		defaultHeaders.put("Last-Modified", "0");
		defaultHeaders.put("ETag", "0");
		
		headersForPathMap.put("/static/",defaultHeaders);
		
		
	}
}
