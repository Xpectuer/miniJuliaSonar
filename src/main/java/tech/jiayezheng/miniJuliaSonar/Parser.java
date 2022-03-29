package tech.jiayezheng.miniJuliaSonar;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.FileUtils;
import tech.jiayezheng.miniJuliaSonar.ast.Node;

import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;

// parser -> dump.json -> parser.toAst
public class Parser {
    private static final String dumpJuliaResource = "tech/jiayezheng/miniJuliaSonar/julia/dump_julia.jl";
    private Process juliaProcess;
    private String jsonizer;
    private static final String JULIA_EXE = "julia";
    private String parserLog;
    private String exchangeFile;
    private String endMark;
    private String file;
    private String content;

    private static final int TIMEOUT = 30000;


    public Parser() {
        exchangeFile = $.getTempFile("json");
        endMark = $.getTempFile("end");
        jsonizer = $.getTempFile("dump_julia");
        parserLog = $.getTempFile("parser_log");

        startJuliaProcess();
    }

    private void startJuliaProcess() {
        if (juliaProcess != null) {
            juliaProcess.destroy();
        }

        try {
            URL url = Thread.currentThread().getContextClassLoader().getResource(dumpJuliaResource);
            FileUtils.copyURLToFile(url, new File(jsonizer));
        } catch (IOException ioe) {
            $.die("Failed to copy URL to File:" + dumpJuliaResource);
        }

        juliaProcess = startInterpreter(JULIA_EXE);
        if (juliaProcess != null) {
            $.msg("started: " + JULIA_EXE);
        } else {
            $.die("Please set Julia in PATH.");
        }


    }


    private int logCount = 0;

    private Process startInterpreter(String juliaExe) {
        Process p;
        try {
            // TODO: run interactive ?
            ProcessBuilder builder = new ProcessBuilder(juliaExe, "-i", jsonizer);
            builder.redirectErrorStream(true);
            builder.redirectOutput(new File(parserLog + "-" + (logCount++)));
            builder.environment().remove("PYTHONPATH");
            p = builder.start();
        } catch (Exception e) {
            $.msg("Failed to start Julia process: " + juliaExe);
            return null;
        }
        return p;
    }

    public void close() {
        if (juliaProcess != null) {
            juliaProcess.destroy();
        }
        // TODO: activate
        //        if(!Analyzer.self.hasOption("debug")) {
        //            new File(exchangeFile).delete();
        //            new File(endMark).delete();
        //            new File(jsonizer).delete();
        //            new File(parserLog).delete();
        //        }
    }

    public Node parseFile(String filename) {
        file = filename;
        content = $.readFile(filename);

        if (juliaProcess != null) {
            Node node = parseFileInner(filename, juliaProcess);
            if (node == null) {
                // TODO:
                //  Analyzer.self.failedToParse.add(filename);
                return null;
            } else {
                return node;
            }
        } else {
            // TODO:
            //  Analyzer.self.failedToParse.add(filename);
            return null;
        }
    }

    public Node parseFileInner(String filename, Process juliaProcess) {

        File endMarker = new File(endMark);
        cleanTemp();

        String s1 = $.escapeWindowsPath(filename);
        String s2 = $.escapeWindowsPath(exchangeFile);
        String s3 = $.escapeWindowsPath(endMark);
        String dumpCommand = "dump_json('" + s1 + "', '" + s2 + "', '" + s3 + "')";

        if (!sendCommand(dumpCommand, juliaProcess)) {
            cleanTemp();
            return null;
        }

        long waitStart = System.currentTimeMillis();
        while(!endMarker.exists()) {
            if(System.currentTimeMillis() - waitStart > TIMEOUT) {
                // time out
                cleanTemp();
                startJuliaProcess();
                return null;
            }

            try {
                Thread.sleep(1);
            } catch(InterruptedException ie) {
                cleanTemp();
                return null;
            }
        }

        String json = $.readFile(exchangeFile);

        if(json != null) {
            cleanTemp();
            JSONObject jsonObject = JSON.parseObject(json);

            return  convert(jsonObject);
        } else {
            cleanTemp();
            return null;
        }

    }

    // TODO: implement convert
//    private Node convert(JSONObject jsonObject) {
//
//    }

    private boolean sendCommand(String cmd, Process juliaProcess) {
        try {
            OutputStreamWriter writer = new OutputStreamWriter(juliaProcess.getOutputStream());
            writer.write(cmd);
            writer.write("\n");
            writer.flush();
            return true;
        } catch (IOException ioe) {
            $.msg("\n Failed to send command to interpreter: " + cmd);
            return false;
        }
    }

    private void cleanTemp() {
        new File(exchangeFile).delete();
        new File(endMark).delete();
    }


}
