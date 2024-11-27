package raid.misc;

import raid.servers.Server;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

public abstract class Util {
    public final static int MAX_BUFFER = 1024;

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

    /**
     * Given a certain file, splits it in half and returns it as two new Files,
     * not damaging the full file.
     * @param file {@link} File to split
     * @return {@link Result} storing both halves of the given file
     */
    public static Result<File, File> splitFile(File file) {
        Result<String, String> result = getFileNameAndExtension(file.getName());
        String name1 = result.getResult1() + "_1." + result.getResult2();
        String name2 = result.getResult1() + "_2." + result.getResult2();

        return splitFile(file, name1, name2);
    }


    /**
     * Given a certain file, splits it in half and returns it as two new Files,
     * not damaging the full file.
     * @param file {@link File} to split
     * @param firstName Name of the first half
     * @param secondName Name of the second half
     * @return {@link Result} storing both halves of the given file, named
     * as specified by parameters
     */
    public static Result<File, File> splitFile(File file, String firstName, String secondName) {
        final String auxPath = "C:\\Users\\gmiga\\Documents\\RaidTesting\\Auxiliar\\";
        long fileLength = file.length();

        File file1 = new File(auxPath + firstName);
        File file2 = new File(auxPath + secondName);

        Result<File, File> result = null;

        BufferedInputStream bis = null;
        BufferedOutputStream bos1 = null;
        BufferedOutputStream bos2 = null;

        try {
            bis = new BufferedInputStream(new FileInputStream(file));
            bos1 = new BufferedOutputStream(new FileOutputStream(file1));
            bos2 = new BufferedOutputStream(new FileOutputStream(file2));

            byte[] buffer = new byte[1];

            for (long i = 0; i < fileLength / 2; i++) {
                bis.read(buffer);
                bos1.write(buffer);
            }
            for (long i = (fileLength / 2); i <= fileLength ; i++) {
                bis.read(buffer);
                bos2.write(buffer);
            }

            result = new Result<>(file1, file2);
        }
        catch (IOException e) {
            System.out.println(e.getMessage());
        }
        finally {
            Util.closeResource(bis);
            Util.closeResource(bos1);
            Util.closeResource(bos2);
        }

        return result;
    }


    /**
     * Returns a Result object containing the file name (without the extension)
     * and the file extension as two separate parts.
     *
     * @param file the File object
     * @return a Result<String, String> where result1 is the file name and result2 is the file extension
     */
    public static Result<String, String> getFileNameAndExtension(File file) {
        return getFileNameAndExtension(file.getName());
    }


    /**
     * Returns a Result object containing the file name (without the extension)
     * and the file extension as two separate parts.
     *
     * @param fileName Name of a File
     * @return a Result<String, String> where result1 is the file name and result2 is the file extension
     */
    public static Result<String, String> getFileNameAndExtension(String fileName) {
        int lastIndexOfDot = fileName.lastIndexOf('.');

        // If no extension is found or the dot is the last character
        if (lastIndexOfDot == -1 || lastIndexOfDot == fileName.length() - 1) {
            return new Result<>(fileName, ""); // No extension
        }

        String nameWithoutExtension = fileName.substring(0, lastIndexOfDot);
        String extension = fileName.substring(lastIndexOfDot + 1);

        return new Result<>(nameWithoutExtension, extension);
    }
}
