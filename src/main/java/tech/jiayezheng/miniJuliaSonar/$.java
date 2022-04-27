package tech.jiayezheng.miniJuliaSonar;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class $ {


    private static boolean debug = false;

    public void setDebug(boolean debug) {
        $.debug = debug;
    }


    private $() {

    }


    public static String getTempFile(String filename) {
        String tmpDir = getTempDir();
        return makePathString(tmpDir, filename);
    }

    private static String makePathString(String tmpDir, String filename) {
        return unifyPath(makePath(tmpDir, filename).getPath());
    }

    public static File makePath(String... filenames) {
        File ret = new File(filenames[0]);

        for (int i = 1; i < filenames.length; i++) {
            ret = new File(ret, filenames[i]);
        }

        return ret;
    }

    private static String getTempDir() {
        String tmp = System.getProperty("java.io.tmpdir");
        String sep = System.getProperty("file.separator");
        if (tmp.endsWith(sep)) {
            return tmp;
        }
        return tmp + sep;
    }

    private static String unifyPath(String path) {
        return unifyPath(new File(path));
    }

    private static String unifyPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException io) {
            die("Failed to get canonial path !");
            return "";
        }
    }

    public static void die(String msg) {
        die(msg, null);
    }

    public static void die(String msg, Exception e) {
        System.err.println(msg);

        if (e != null) {
            System.err.println("Exception:" + e + "\n");
        }

        Thread.dumpStack();
        System.exit(2);
    }

    public static void msg(String msg) {
        // TODO: Resolve Analyzer Options
        System.out.println(msg);
    }

    public static void debugf(String format,Object... args) {
        if($.debug) {
            System.out.printf(format,args);
        }

    }


    public static String readFile(String path) {
        byte[] contents = getBytesFromFile(path);
        if (contents == null) {
            return null;
        } else {
            return new String(contents, StandardCharsets.UTF_8);
        }
    }

    private static byte[] getBytesFromFile(String path) {
        try {
            return FileUtils.readFileToByteArray(new File(path));
        } catch (IOException io) {
            return null;
        }
    }

    public static String escapeWindowsPath(String path) {
        return path.replace("\\", "\\\\");
    }
}