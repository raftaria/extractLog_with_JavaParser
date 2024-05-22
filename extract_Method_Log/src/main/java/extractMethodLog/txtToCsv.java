package extractMethodLog;

/* 위의 코드를 참고하여 작업하였음 */
/* https://www.baeldung.com/java-csv */

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class txtToCsv {
    public static void main(String[] args) {
        String path = "testproject/raw/";
        String target_txt = path + "target.txt";
        String input_txt = path + "input.txt";
        String logsta_txt = path + "logsta.txt";
        String logposi_txt = path + "logposi.txt";
        String labeled_txt = path + "labeled.txt";
        String stage1_csv = path + "stage-1.csv";
        String stage2_csv = path + "stage-2.csv";

        createStage1CSV(input_txt, labeled_txt, stage1_csv);
        createStage2CSV(input_txt, target_txt, logsta_txt, logposi_txt, stage2_csv);
    }

    private static void createStage1CSV(String inputFile, String labelFile, String outputFile) {
        try {
            BufferedReader inputReader = new BufferedReader(new FileReader(inputFile));
            BufferedReader labelReader = new BufferedReader(new FileReader(labelFile));
            FileWriter writer = new FileWriter(outputFile);

            // CSV header
            writer.write(",Input,Label\n");

            String inputLine;
            String labelLine;
            int index = 0;

            while ((inputLine = inputReader.readLine()) != null && (labelLine = labelReader.readLine()) != null) {
                writer.write(index + "," + escapeSpecialCharacters(inputLine) + "," + escapeSpecialCharacters(labelLine) + "\n");
//                writer.write(index + "," + inputLine + "," + labelLine + "\n");
                index++;
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void createStage2CSV(String inputFile, String targetFile, String logstaFile, String logposiFile, String outputFile) {
        try {
            BufferedReader inputReader = new BufferedReader(new FileReader(inputFile));
            BufferedReader targetReader = new BufferedReader(new FileReader(targetFile));
            BufferedReader logstaReader = new BufferedReader(new FileReader(logstaFile));
            BufferedReader logposiReader = new BufferedReader(new FileReader(logposiFile));
            FileWriter writer = new FileWriter(outputFile);

            // CSV header
            writer.write(",Input,Target,LogStatement,Position,Level,Message\n");

            String inputLine;
            String targetLine;
            String logstaLine;
            String logposiLine;
            int index = 0;

            while ((inputLine = inputReader.readLine()) != null &&
                    (targetLine = targetReader.readLine()) != null &&
                    (logstaLine = logstaReader.readLine()) != null &&
                    (logposiLine = logposiReader.readLine()) != null) {

                String input = escapeSpecialCharacters(inputLine);
                String target = escapeSpecialCharacters(targetLine);
                String position = escapeSpecialCharacters(logposiLine);

                String[] logstaLineSplit = logstaLine.split("\\$\\$");
                String log_sta = escapeSpecialCharacters(logstaLineSplit[0] + ";");
                String log_lv = escapeSpecialCharacters(logstaLineSplit[1]);
                String log_msg = escapeSpecialCharacters(logstaLineSplit[2]);

                writer.write(index + "," +
                        input + "," +
                        target + "," +
                        log_sta + "," +
                        position + "," +
                        log_lv + "," +
                        log_msg.replaceFirst(" ", "") + "\n");
                index++;
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* 특수문자를 다루기 위한 메서드 */
    public static String escapeSpecialCharacters(String data) {
        if (data == null) {
            throw new IllegalArgumentException("Input data cannot be null");
        }
        String escapedData = data.replaceAll("\\R", " ");
        if (data.contains(",") || data.contains("\"") || data.contains("'")) {
            data = data.replace("\"", "\"\"");
            escapedData = "\"" + data + "\"";
        }
        return escapedData;
    }
}