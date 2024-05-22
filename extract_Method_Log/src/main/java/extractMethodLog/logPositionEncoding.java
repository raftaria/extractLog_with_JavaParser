package extractMethodLog;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class logPositionEncoding {
    public static void main(String[] args) throws IOException {
        String sourcePath = "testproject/raw/";
        String inputFile = sourcePath + "logposi.txt";
        String outputFile = sourcePath + "labeled.txt";

        try {
            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
            FileWriter writer = new FileWriter(outputFile);
            String line;
            while ((line = reader.readLine()) != null) {
                // 문자열을 공백을 기준으로 토큰화.
                String[] tokens = line.split("\\s+");
                StringBuilder modifiedLine = new StringBuilder();

                boolean logFound = false;
                for (int i = 0; i < tokens.length; i++) {
                    if ("<LOG>".equals(tokens[i])) {
                        logFound = true;
                        if (i > 0) {
                            // <LOG> 바로 앞에 있는 토큰을 1로 치환
                            modifiedLine.setCharAt(modifiedLine.length() - 2, '1');
                        }
                        // <LOG> 토큰은 건너뜀
                        continue;
                    }

                    if (logFound) {
                        modifiedLine.append("0 ");
                    } else {
                        modifiedLine.append("0 ");
                    }
                }
                // 라인의 마지막 공백 제거 후 작성
                writer.write(modifiedLine.toString().trim() + "\n");
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}