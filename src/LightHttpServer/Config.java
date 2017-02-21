package LightHttpServer;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;






import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;





import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

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
	public String html404Content="<html><body><h1>404 Not Found</h1></body></html>";
	
	public void loadConfig(){
		Document xmlDocument=loadConfigDocument();
		if(xmlDocument==null){
			httpVersion = "HTTP/1.0";
			host = "localhost";
			port = 8800;
			maxThreadNum = 10;
			keepAliveTime = 30;
			documentRootDirectoryPath = "www/";
			CGIAlias = "/cgi-bin/";
			CGIPath = "www/cgi-bin/";
			indexFilePath = "index.html";
			coreNum = Runtime.getRuntime().availableProcessors();
			serverVersion = "LightHttpServer/0.1";
			
		}
		else{
			try{
				
			httpVersion = "HTTP/1.0";
			coreNum = Runtime.getRuntime().availableProcessors();
			serverVersion = "LightHttpServer/0.1";
		
			NodeList nodeList;
			
			nodeList= xmlDocument.getElementsByTagName("port");
			port=nodeList.getLength()>0?Integer.valueOf(nodeList.item(0).getTextContent().trim()):80;
			if(port<0||port>65536)
				throw new Exception("\"port\" in \"config.xml\" should be set between 0 to 65536");
			
			nodeList= xmlDocument.getElementsByTagName("max_threads");
			maxThreadNum=nodeList.getLength()>0?Integer.valueOf(nodeList.item(0).getTextContent().trim()):80;
			
			nodeList= xmlDocument.getElementsByTagName("service_wait_time");
			keepAliveTime =nodeList.getLength()>0?Integer.valueOf(nodeList.item(0).getTextContent().trim()):80;
			
			nodeList=xmlDocument.getElementsByTagName("host");
			if(nodeList.getLength()<=0)
				throw new Exception("At least one \"host\" must be set");
			
			Node hostNode=nodeList.item(0);
			List<Node> selectedNodes;
			selectedNodes=findChildNodeByTag(hostNode,"host_name");
			host =selectedNodes.size()>0?selectedNodes.get(0).getTextContent().trim():"localhost";
			
			selectedNodes=findChildNodeByTag(hostNode,"index_file");
			indexFilePath =selectedNodes.size()>0?selectedNodes.get(0).getTextContent().trim():"index.html";
			
			selectedNodes=findChildNodeByTag(hostNode,"document_root_directory_path");
			if(selectedNodes.size()<=0)
				throw new Exception("\"document_root_directory_path\" should be set in \"host\"");
			documentRootDirectoryPath =selectedNodes.get(0).getTextContent().trim();
			
			selectedNodes=findChildNodeByTag(hostNode,"cgi");
			if(selectedNodes.size()>0){
				Node CGINode=selectedNodes.get(0);
				selectedNodes=findChildNodeByTag(CGINode,"url_alias");
				if(selectedNodes.size()==0)
					throw new Exception("\"url_alias\" should be set in \"cgi\"");
				CGIAlias=selectedNodes.get(0).getTextContent().trim();
				selectedNodes=findChildNodeByTag(CGINode,"document_path");
				if(selectedNodes.size()==0)
					throw new Exception("\"document_path\" should be set in \"cgi\"");
				CGIPath=selectedNodes.get(0).getTextContent().trim();
			}
			
			selectedNodes=findChildNodeByTag(hostNode,"headers");
			if(selectedNodes.size()>0){
				Node headersNode=selectedNodes.get(0);
				selectedNodes=findChildNodeByTag(headersNode,"set");
				setHeadersMap(selectedNodes);
				
			}
			
			
		
			}
			catch (Exception e){
				e.printStackTrace(System.out);
		 		System.exit(-1);
			}
		}
		
		
	}
	
	private void setHeadersMap(List<Node> setNodeList) throws Exception{
		headersForPathMap = new HashMap<String, Map<String, String>>();
		Node setNode;
		List<Node> nodeList;
		String path;
		
		for(int i=0;i<setNodeList.size();i++){
			setNode=setNodeList.get(i);
			nodeList=findChildNodeByTag(setNode,"url_alias");
			if(nodeList.size()==0)
				throw new Exception("\"url_alias\" should be set in \"set\"");
			path=nodeList.get(0).getTextContent().trim();
			nodeList=findChildNodeByTag(setNode,"header");
			if(nodeList.size()>0){
				Map<String, String> header=new HashMap<>();
				for(int j=0;j<nodeList.size();j++){
					Node headerNode=nodeList.get(j);
					String key=null,value=null;
					NodeList childList=headerNode.getChildNodes();
					for(int k=0;k<childList.getLength();k++){
						Node node=childList.item(k);
						if(node.getNodeName().equals("key"))
							key=node.getTextContent().trim();
						if(node.getNodeName().equals("value"))
							value=node.getTextContent().trim();
					}
					if(key!=null&&value!=null){
						header.put(key,value);
					}
				}
				headersForPathMap.put(path, header);
			}
		}
	}
	
	
	private List<Node> findChildNodeByTag(Node parentNode,String tagName) {
		NodeList nodeList=parentNode.getChildNodes();
		List<Node> resNodeList=new ArrayList<Node>();
		Node node;
		for(int i=0;i<nodeList.getLength();i++){
			node=nodeList.item(i);
			if(node.getNodeName().equals(tagName)){
				resNodeList.add(node);
			}
		}
		return resNodeList;
		
		
	}
	
	
	
	private Document loadConfigDocument() {
		 try {
			 	File file=new File("config.xml");
	            InputSource is = new InputSource(new FileInputStream(file));
	            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
	            DocumentBuilder builder=factory.newDocumentBuilder();
	            return builder.parse(is);
	        }
	        catch (IOException e){
	        	System.out.println("Config file \"config.xml\" not found, using default setting...");
	            return null;
	        }
		 	catch (ParserConfigurationException e) {
		 		System.out.println("Exception occured when parsing \"config.xml\":");
		 		e.printStackTrace(System.out);
		 		System.exit(-1);
		 		 return null;
			}
		 
		 	catch (SAXException e) {
		 		System.out.println("Exception occured when parsing \"config.xml\":");
		 		e.printStackTrace(System.out);
		 		System.exit(-1);
		 		 return null;
				// TODO: handle exception
			}
		
	}
}
