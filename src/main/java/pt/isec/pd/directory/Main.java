package pt.isec.pd.directory;

public class Main {
    public static void main(String[] args) {
        System.out.printf("Hello directory!");
        if (args.length != 1) {
            System.err.println("Uso: java DirectoryMain <porto_escuta_UDP>");
            System.exit(1);
        }

        int udpPort = Integer.parseInt(args[0]);
        DirectoryService directoryService = new DirectoryService(udpPort);
        directoryService.start();
    }
}
