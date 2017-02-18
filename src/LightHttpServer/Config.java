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
	public String indexFilePath;
	public String serverVersion;
	public Map<String, Map<String, String>> headersForPathMap;
	
	public void loadConfig(){
		httpVersion="HTTP/1.0";
		host="localhost";
		port=8888;
		maxThreadNum=10;
		keepAliveTime=30;
		documentRootDirectoryPath="E:/workspace/www/";
		indexFilePath="index.html";
		coreNum=Runtime.getRuntime().availableProcessors();
		serverVersion="LightHttpServer/0.1";
		headersForPathMap=new HashMap<String, Map<String,String>>();
		Map<String, String> defaultHeaders=new HashMap<String, String>();
		defaultHeaders.put("Cache-Control", "max-age = 20");
		headersForPathMap.put("/",defaultHeaders);
		
	}
}
