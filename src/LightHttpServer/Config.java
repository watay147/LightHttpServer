package LightHttpServer;

public class Config {
	public String host;
	public int port;
	public int coreNum;
	public int maxThreadNum;
	public int keepAliveTime;
	public String documentRootDirectoryPath;
	public String indexFilePath;
	
	
	public void loadConfig(){
		host="localhost";
		port=8888;
		maxThreadNum=10;
		keepAliveTime=30;
		documentRootDirectoryPath="E:/workspace/www/";
		indexFilePath="index.html";
		coreNum=Runtime.getRuntime().availableProcessors();
	}
}
