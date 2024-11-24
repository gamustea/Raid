package raid.misc;

import raid.servers.Server;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public abstract class Util {
    public static final int GET_FILE = 1;
    public static final int SAVE_FILE = 2;
    public static final int DELETE_FILE = 3;
    public static final int CLOSE_CONNECTION = 4;

    public final static int NOT_READY = 10;
    public final static int FILE_NOT_FOUND = 11;
    public final static int CRITICAL_ERROR = 12;
    public final static int FILE_DELETED = 13;
    public final static int FILE_STORED = 14;
    public final static int FILE_RETRIEVED = 15;

    public final static int SUCCESS = 20;


    public static void closeResource(Thread thread) {
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
    }

    public static void closeResource(Closeable closableResource) {
        if (closableResource != null) {
            try {
                closableResource.close();
            } catch (IOException e) {
                System.out.println("| ERROR WHILE CLOSING RESOURCE " + closableResource + "|");
            }
        }
    }

    public static String getProperty(String clave, String propertiesFile) {
        String valor = null;
        try {
            Properties props = new Properties();
            InputStream prIS = Server.class.getResourceAsStream(propertiesFile);
            props.load(prIS);
            valor = props.getProperty(clave);
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
        return valor;
    }

    public static String translateMessage(int message) {
        return switch (message) {
            case NOT_READY -> "| PROCESS IS NOT READY |";
            case FILE_NOT_FOUND -> "| FILE NOT FOUND |";
            case CRITICAL_ERROR -> " | CRITICAL ERROR | ";
            case FILE_DELETED -> "| FILE SUCCESSFULLY DELETED |";
            case FILE_STORED -> "| FILE SUCCESSFULLY STORED |";
            case FILE_RETRIEVED -> "| FILE SUCCESSFULLY RETRIEVED |";
            case SUCCESS -> "| PROCESS HAS HAD SUCCESS |";
            default -> "";
        };
    }

    public static boolean existsFileWithName(String fileName) {
        File file = new File(fileName);
        return file.exists();
    }

    /**
     * Checks that the given path exists in the current host. If it does not,
     * method creates it in the given.
     * @param path {@link Path} to check
     */
    public static void checkPathExistence(Path path) {
        try {
            if (!Files.exists(path)) {
                Files.createDirectory(path);
            }
        } catch (IOException e) {
            System.out.println("Error while creating " + path);
        }
    }

    public static void depositContent(File inFile, File outFile) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(inFile));
        BufferedWriter bw = new BufferedWriter(new FileWriter(outFile));

        String linea;
        while ((linea = br.readLine()) != null) {
            bw.write(linea);
        }
        bw.flush();
    }
}
