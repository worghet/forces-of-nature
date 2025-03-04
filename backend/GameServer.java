
// import com.google.gson.Gson;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class GameServer {

    // == VARIABLES =================================

    // Server info
    private int serverPort;
    private String localHostAddress;
    private HttpServer httpServer;

    // api constants (for readability)
    static private final int GAMEPLAY_PAGE = 0;
    static private final int CHARACTER_SELECT_PAGE = 1;
    static private final int MAIN_MENU_PAGE = 2;

    // JSON serialization/deserialization object
    // private Gson gson = new Gson();

    // == METHODS =================================

    // method to use from outside of ts class
    public static void createGameServer(int requestedServerPort) {

        // 1. CHECK PORT AVAILIBILITY

        boolean portAvailible = false;

        // (essentially trying to run a server on that port.. seeing what happens)
        try (ServerSocket socket = new ServerSocket(requestedServerPort)) {
            // System.out.println("port " + requestedServerPort + " is... AVAILIBLE!");
            portAvailible = true;
        } catch (IOException e) {
            // print port is busy
            System.out.println("port " + requestedServerPort + " is... OCCUPIED!");
        }

        // continue with gameserver creation
        if (portAvailible) {
            GameServer gameServer = new GameServer(getLocalIPAddress(), requestedServerPort);
            // gameServer.startTimer() (todo)
        }

    }

    // primary constructor
    public GameServer(String hostAddress, int port) {

        // set instance variables
        localHostAddress = hostAddress;
        serverPort = port;

        // use try to catch errors
        try {
            httpServer = HttpServer.create(new InetSocketAddress(serverPort), 0);

            // ADD STATIC RESOURCE API ---------------------
            httpServer.createContext("/styles.css", new StaticFileHandler("frontend/styles.css", "text/css"));
            httpServer.createContext("/functionality.js",
                    new StaticFileHandler("frontend/functionality.js", "frontend/javascript"));

            // ADD PAGE APIS -------------
            httpServer.createContext("/forces-of-nature/gameplay", new WebpageHandler(GAMEPLAY_PAGE));
            httpServer.createContext("/forces-of-nature", new WebpageHandler(MAIN_MENU_PAGE));
            httpServer.createContext("/forces-of-nature/waitroom", new WebpageHandler(CHARACTER_SELECT_PAGE));
            

            // idk what this is
            httpServer.setExecutor(null);
            System.out.print("Starting server..");
            httpServer.start();
            System.out.println("STARTED!");
            System.out.println("http://" + hostAddress + ":" + serverPort + "/forces-of-nature/gameplay" +
                    "\n(IF FROM CHROMEBOOK, USE IP ADDRESS)\n----------------------------------------");

        } catch (Exception exception) {
            System.out.println("Server setup went wrong... " + exception.toString());
        }
    }

    // private startTimer {
    // // create runnable
    // }

    public static String getLocalIPAddress() {
        try {
            // Get all network interfaces
            Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
            while (networkInterfaces.hasMoreElements()) {
                NetworkInterface networkInterface = networkInterfaces.nextElement();

                // Get all IP addresses for each network interface
                Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
                while (inetAddresses.hasMoreElements()) {
                    InetAddress inetAddress = inetAddresses.nextElement();

                    // Ignore loopback addresses like 127.0.0.1
                    if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                        return inetAddress.getHostAddress(); // Return the first valid IP address found
                    }
                }
            }
        } catch (SocketException e) {
            e.printStackTrace();
        }
        return null;
    }

    // --------------------------------

    static class WebpageHandler implements HttpHandler {

        private int requestedPage;

        public WebpageHandler(int requestedPage) {
            this.requestedPage = requestedPage;
        }

        @Override
            public void handle(HttpExchange httpExchange) throws IOException {
                   // technically shouldnt get post anytime, but did if just in case
                if ("GET".equals(httpExchange.getRequestMethod())) {
    
                    // if message list not empty; load it (
                    // serve html
    
                    // get byt file path
                    System.out.println("giving page..");
    
                    String path = "";

                    switch (requestedPage) {
                        case GAMEPLAY_PAGE:
                            path = "frontend/game-screen.html";
                            break;
                
                        case MAIN_MENU_PAGE:
                            path = "frontend/main-screen.html";
                            break;
                        case CHARACTER_SELECT_PAGE:
                            path = "frontend/character-select-screen.html"; 
                            break;   
                    default:
                        break;
                }

                System.out.println(path + ".. GIVEN!");
                byte[] htmlBytes = Files.readAllBytes(Paths.get(path));
                httpExchange.getResponseHeaders().set("Content-Type", "text/html");

                // send it
                httpExchange.sendResponseHeaders(200, htmlBytes.length);
                OutputStream os = httpExchange.getResponseBody();
                os.write(htmlBytes);
                os.close();
            }

        }

    }

    static class StaticFileHandler implements HttpHandler {
        private String filePath;
        private String contentType;

        public StaticFileHandler(String filePath, String contentType) {
            this.filePath = filePath;
            this.contentType = contentType;
        }

        @Override
        public void handle(HttpExchange httpExchange) throws IOException {
            Path path = Paths.get(filePath);
            if (Files.exists(path)) {
                byte[] fileBytes = Files.readAllBytes(path);
                httpExchange.getResponseHeaders().set("Content-Type", contentType);
                httpExchange.sendResponseHeaders(200, fileBytes.length);
                OutputStream os = httpExchange.getResponseBody();
                os.write(fileBytes);
                os.close();
            } else {
                httpExchange.sendResponseHeaders(404, 0);
                httpExchange.close();
            }
        }
    }

}
