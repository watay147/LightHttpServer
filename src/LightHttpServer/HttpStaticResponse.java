package LightHttpServer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HttpStaticResponse extends HttpResponse{
	
	
	
	
	public String version;
	public int status;
	public String reasonPhrase;
	public Map<String, List<String>> headers;
	public HttpEntity entity;
	
	public HttpStaticResponse(){
		this.headers=new HashMap<String, List<String>>();
		
	}
	
	public void setStatusLine(String version,int status,String reasonPhrase) {
		this.version=version;
		this.status=status;
		this.reasonPhrase=reasonPhrase;
	}
}
