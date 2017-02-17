package LightHttpServer;

public class Launcher {
	
	public static void main(String[] args) {
		Server server=Server.getServer();
		server.init();
		server.start();
	}
}
