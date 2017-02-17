package LightHttpServer;

import java.util.List;
import java.util.Map;

public class HttpRequest {
	
	public interface Method {
        int UNKNOWN = -1;
        int GET = 0;
        int POST = 1;
        int PUT = 2;
        int DELETE = 3;
        int HEAD = 4;
        int OPTIONS = 5;
        int TRACE = 6;
        int PATCH = 7;
    }
	
	
	public int method;
	public String requestUrl;
	public String host;
	public String version;
	//may have duplicated headers, all values belong to the same key of a header stored as a List
	public Map<String, List<String>> headers;
	public HttpEntity entity;
	
	
	
}
