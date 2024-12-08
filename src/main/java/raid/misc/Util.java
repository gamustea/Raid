package raid.misc;

import raid.clients.Client;
import raid.servers.EastServer;
import raid.servers.Server;
import raid.servers.WestServer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Class containing different universal methods used to easy the work of the {@link Server} intances
 * and the {@link Client} object's job. Contains different codes that represent performance, as well as
 * paths for properties files, among other kinds of utilities. The util codes are the following:
 * <ul>
 *     <li>{@code GET_FILE}: ask the server to retrieve a file to you</li>
 *     <li>{@code SAVE_FILE}: ask the server to save a file</li>
 *     <li>{@code DELETE_FILE}: ask the server to delete a file</li>
 *     <li>{@code LIST_FILES}: ask the server to list the files it contains</li>
 *     <li>{@code CLOSE_CONNECTION}: ask the server to shut down the connection</li>
 *     <li>{@code STAND_BY}: ask the server to stand by, while the client is performing operations</li>
 *     <li>{@code NOT_READY}: make other processes know the current one is not ready</li>
 *     <li>{@code FILE_NOT_FOUND}: make the client know the file he/she was searching does not exist</li>
 *     <li>{@code CRITICAL_ERROR}: make other processes know a critical error happened</li>
 *     <li>{@code FILE_DELETED}: make the client know the request of deleting was successful</li>
 *     <li>{@code FILE_STORED}: make the client know the request of saving was successful</li>
 *     <li>{@code FILE_RETRIEVED}: make the client know the request of getting was successful</li>
 *     <li>{@code STANDING_BY}: make the client know the server is standing by and waiting for ongoing operations</li>
 *     <li>{@code SUCCESS}: make any processes know that an operation has had success</li>
 * </ul>
 *
 */
public abstract class Util {

    public final static int MAX_BUFFER = 1024;

    public final static String HOSTS = "/hosts.properties";
    public final static String PORTS = "/ports.properties";
    public final static String PATHS = "/absoluteRoutes.properties";

    public final static String SERVER_FILE_PATH = getProperty("SERVER_PATH", PATHS);
    public final static String CLIENT_FILE_PATH = getProperty("CLIENT_PATH", PATHS);

    // ========= UTIL CODES FOR CLIENT-SERVER COMMUNICATION =========
    public static final int GET_FILE = 1;
    public static final int SAVE_FILE = 2;
    public static final int DELETE_FILE = 3;
    public static final int LIST_FILES = 4;
    public static final int CLOSE_CONNECTION = 5;
    public static final int STAND_BY = 6;
    public final static int NOT_READY = 10;
    public final static int FILE_NOT_FOUND = 11;
    public final static int CRITICAL_ERROR = 12;
    public final static int FILE_DELETED = 13;
    public final static int FILE_STORED = 14;
    public final static int FILE_RETRIEVED = 15;
    public final static int FILES_LISTED = 16;
    public final static int STANDING_BY = 17;
    public final static int SUCCESS = 20;


    /**
     * Interrupts a thread if the thread is still running.
     * @param thread {@link Thread} to interrupt.
     */
    public static void closeResource(Thread thread) {
        if (thread != null && thread.isAlive()) {
            thread.interrupt();
        }
    }


    /**
     * Checks whether the given object is potentially closable and, in
     * that case, tries to close it. Otherwise, won't do anything.
     * @param closableResource {@link Closeable} to close
     */
    public static void closeResource(Closeable closableResource) {
        if (closableResource != null) {
            try {
                closableResource.close();
            } catch (IOException e) {
                System.out.println("| ERROR WHILE CLOSING RESOURCE " + closableResource + "|");
            }
        }
    }


    /**
     * Searches an attribute in the given properties file.
     * @param key Attribute to search
     * @param propertiesFile Name of the properties file
     * @return Value of the attribute
     */
    public static String getProperty(String key, String propertiesFile) {
        String valor = null;
        try {
            Properties props = new Properties();
            InputStream prIS = Server.class.getResourceAsStream(propertiesFile);
            props.load(prIS);
            valor = props.getProperty(key);
        } catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
        return valor;
    }


    /**
     * Given a Util message, translates it into a {@link String}, so
     * that it can be interpreted by the user.
     * @param message A valid Util code
     * @return {@link String} translation for the given code
     */
    public static String translateMessage(int message) {
        return switch (message) {
            case NOT_READY -> "| PROCESS IS NOT READY |";
            case FILE_NOT_FOUND -> "| FILE NOT FOUND |";
            case CRITICAL_ERROR -> " | CRITICAL ERROR | ";
            case FILE_DELETED -> "| FILE SUCCESSFULLY DELETED |";
            case FILE_STORED -> "| FILE SUCCESSFULLY STORED |";
            case FILE_RETRIEVED -> "| FILE SUCCESSFULLY RETRIEVED |";
            case FILES_LISTED -> "| LIST OF FILES SUCCESSFULLY RETRIEVED |";
            case SUCCESS -> "| PROCESS HAS HAD SUCCESS |";
            case STANDING_BY -> "| ORDER RECEIVED - SERVER STANDING BY |";
            default -> "";
        };
    }


    /**
     * Checks whether there is a file stored with the given name.
     * @param fileName Name of the file
     * @return True if the file exits and false otherwise
     */
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


    /**
     * Takes the content of a {@link File} and deposits it into another, by adding NOT OVERWRITING.
     * The content of the read file won't be deleted, but the content of the written
     * file would be the sum between both file contents.
     * @param inFile File to add content.
     * @param outFile File to read content.
     * @throws IOException If an I/O error happens.
     */
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
    public static Result<File, File> splitFile(File file, String currentPath) {
        Result<String, String> result = getFileNameAndExtension(file.getName());
        String name1 = result.result1() + "_1." + result.result2();
        String name2 = result.result1() + "_2." + result.result2();

        return splitFile(file, currentPath, name1, name2);
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
    public static Result<File, File> splitFile(File file, String currentPath, String firstName, String secondName) {
        currentPath += "\\";
        checkPathExistence(Path.of(currentPath));
        long fileLength = file.length();

        File file1 = new File(currentPath + firstName);
        File file2 = new File(currentPath + secondName);

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
     * Takes the content of two {@link File} instances and deposits it into another {@link File}.
     * None of the original file contents would remain damaged at the end of this call - only the destination
     * file would be the sum of the contents of both files.
     * @param file1 First file to deposit.
     * @param file2 Second file to deposit.
     * @param destination Destination file, sum of {@code file1} and {@code file2}.
     * @throws IOException If an I/O error happens.
     */
    public static void mergeFiles(File file1, File file2, File destination) throws IOException {
        FileInputStream fileReader = null;
        FileOutputStream fileWriter = null;
        int bytesRead;
        byte[] buffer = new byte[MAX_BUFFER];

        try {
            fileWriter = new FileOutputStream(destination);
            fileReader = new FileInputStream(file1);
            while ((bytesRead = fileReader.read(buffer)) != -1) {
                fileWriter.write(buffer, 0, bytesRead);
            }
            fileWriter.flush();
            closeResource(fileReader);

            fileReader = new FileInputStream(file2);
            while ((bytesRead = fileReader.read(buffer)) != -1) {
                fileWriter.write(buffer, 0, bytesRead);
            }
            fileWriter.flush();
            closeResource(fileReader);
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        finally {
            closeResource(fileReader);
            closeResource(fileWriter);
        }
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


    /**
     * Used by {@link EastServer} and {@link WestServer}
     * to get the name of the original {@link File} name given by the user.
     * Takes the name of a file with the character string {@code "_1"} or
     * {@code "_2"} and transforms it so that, at the time of returning,
     * those characters no longer exist.
     * @param fileName Crafted file name (may contain {@code "_1"} or
     * {@code "_2"}, or not).
     * @return Name of the original file.
     */
    public static String getCorrectFileName(String fileName) {
        if (!fileName.contains("_1") && !fileName.contains("_2")) {
            return fileName;
        }
        else {
            String extension = getFileNameAndExtension(fileName).result2();
            String trueName;

            String[] parts1 = fileName.split("_1");
            String[] parts2 = fileName.split("_2");

            if (parts1.length == 2) {
                trueName = parts1[0] + "." + extension;
            }
            else {
                trueName = parts2[0] + "." + extension;
            }

            return trueName;
        }
    }


    /**
     * Closes an {@link ExecutorService} if it's not already
     * shut down or is null
     *
     * @param executor el ExecutorService a cerrar
     * @param timeout  el tiempo máximo para esperar el cierre
     * @param unit     la unidad de tiempo del timeout
     */
    public static void shutdownExecutorService(ExecutorService executor, long timeout, TimeUnit unit) {
        if (executor != null && !executor.isShutdown()) {
            try {
                // Intentar una parada ordenada
                executor.shutdown();
                if (!executor.awaitTermination(timeout, unit)) {
                    // Forzar la parada si no se completa dentro del timeout
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                // Forzar la parada si ocurre una interrupción
                executor.shutdownNow();
                // Restaurar el estado de interrupción del hilo
                Thread.currentThread().interrupt();
            }
        }
    }
}
