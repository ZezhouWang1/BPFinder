package one;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static one.Constants.*;

public class DynamicTracer {

    private static final long RUNNING_MINUTES = 1;
    public static void dynamicExecute() {
        new File(OUTPUT_FILE).delete();
        //execCommand(ADB + " connect 127.0.0.1:62001", 5);
        execCommand(ADB +" install "+ APK_SOOT_OUTPUT, 5);
        execCommand(ADB + " logcat -c", 5);
        execCommand(ADB + " shell CLASSPATH=/sdcard/monkey.jar:/sdcard/framework.jar exec app_process /system/bin tv.panda.test.monkey.Monkey -p " + TARGET_PACKAGE + " --uiautomatormix --running-minutes "
                + RUNNING_MINUTES + " -v -v", RUNNING_MINUTES*60 + 15);
        execCommand(ADB + " logcat -d", 15);
        System.out.println("Complete and exit.");
    }

    private static void execCommand(String command, long allowedSeconds) {
        ExecutorService singleThreadExecutor = Executors.newSingleThreadExecutor();
        Callable<String> callable = () -> {
            try {
                // 要执行的命令
                // 使用ProcessBuilder创建进程
                ProcessBuilder processBuilder = new ProcessBuilder(command.split("\\s+"));
                Process process = processBuilder.start();

                // 获取命令行的输出流
                InputStream inputStream = process.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));

                // 持续监听输出
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains("All stack")) {
                        String target = line.substring(line.indexOf("All stack content: ")
                                + "All stack content: ".length());
                        String[] stackTrace = target.substring(1, target.length() - 1)
                                .split(", ");
                        saveStackTrace(stackTrace, OUTPUT_FILE);
                    }
                }
                // 等待命令执行完成
                int exitCode = process.waitFor();
                return "Command " + command + " executed with exit code: " + exitCode;

            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
                return "Exception occurred.";
            }
        };

        Future<String> future = singleThreadExecutor.submit(callable);
        try {
            String result = future.get();
            singleThreadExecutor.awaitTermination(allowedSeconds, TimeUnit.SECONDS);
            System.out.println(result);
        } catch (InterruptedException | ExecutionException e){
            e.printStackTrace();
        }finally {
            singleThreadExecutor.shutdownNow();
        }
    }

    private static void saveStackTrace(String[] stackTraceArray, String fileName) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(fileName, true))) {
            List<String> reversed = new ArrayList<>(Arrays.asList(stackTraceArray));
            Collections.reverse(reversed);
            writer.println(reversed.stream().
                    map(s -> s.substring(0, s.indexOf('(')))
                    .collect(Collectors.joining(DELIMITER)));
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }catch (ArrayIndexOutOfBoundsException ignored){

        }

    }
}
