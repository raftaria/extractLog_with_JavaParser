package extractMethodLog;

import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import com.github.javaparser.ParseResult;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.Node;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.comments.Comment;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.expr.NameExpr;
import com.github.javaparser.ast.expr.MethodCallExpr;
import com.github.javaparser.ast.stmt.ExpressionStmt;
import com.github.javaparser.utils.SourceRoot;

public class extractMethodLog {
    public static void main(String[] args) {
        String sourcePath = "testproject";
        String target_txt = sourcePath + "/raw/target.txt";
        String input_txt = sourcePath + "/raw/input.txt";
        String logsta_txt = sourcePath + "/raw/logsta.txt";
        String logposi_txt = sourcePath + "/raw/logposi.txt";
        String allMethod_txt = sourcePath + "/raw/allMethod.txt";

        ArrayList<String> target_list = new ArrayList<>();
        ArrayList<String> logsta_list = new ArrayList<>();

        try {
            PrintWriter target_writer = new PrintWriter(target_txt);
            PrintWriter input_writer = new PrintWriter(input_txt);
            PrintWriter logsta_writer = new PrintWriter(logsta_txt);
            PrintWriter logposi_writer = new PrintWriter(logposi_txt);
            PrintWriter allMethod_writer = new PrintWriter(allMethod_txt);

            File rootDir = new File(sourcePath);
            List<File> projectDirs = new ArrayList<>();
            findProjectDirs(rootDir, projectDirs);

            for(File projectDir : projectDirs) {
                extractMethods(projectDir.getPath(), target_list);
            }


            for (int i = 0; i < target_list.size(); i++) {
                System.out.println("Method_" + i + " " + target_list.get(i));
                allMethod_writer.println(target_list.get(i));
            }

            extractLogs(target_list, logsta_list);
            for (int i = 0; i < logsta_list.size(); i++) {
                System.out.println("Log_" + i + " " + logsta_list.get(i));
            }

            // Write methods with log statements to "target.txt"
            for (String method : target_list) {
                MethodDeclaration method_Decl = StaticJavaParser.parseMethodDeclaration(method);
                List<String> logs = method_Decl.findAll(MethodCallExpr.class).stream()
                        .filter(mc -> mc.getScope().isPresent() &&
                                mc.getScope().get().toString().matches(".*log.*|.*LOG.*") &&
                                mc.getNameAsString().matches("info|trace|debug|warn|error|fatal"))
                        .map(MethodCallExpr::toString)
                        .collect(Collectors.toList());

                // Save to target.txt
                if (!logs.isEmpty()) {
                    for(String log : logs) {
//                        /* ********** */
//                        System.out.println(method);
                        target_writer.println(addSpaces(method));
                    }
                }
            }

            // Write log statements to "logsta.txt"
            for (String log : logsta_list) {
//                /* ********** */
//                System.out.println(log);
                logsta_writer.println(addSpaces(log));
            }

            // Save methods with removed log statements to "input.txt" and modified methods to "logposi.txt"
            for (String method : target_list) {
                MethodDeclaration methodDecl = StaticJavaParser.parseMethodDeclaration(method);
                List<ExpressionStmt> logStatements = methodDecl.findAll(ExpressionStmt.class).stream()
                        .filter(stmt -> {
                            if (stmt.getExpression() instanceof MethodCallExpr) {
                                MethodCallExpr mc = (MethodCallExpr) stmt.getExpression();
                                return mc.getScope().isPresent() &&
                                        mc.getScope().get().toString().matches(".*log.*|.*LOG.*") &&
                                        mc.getNameAsString().matches("info|trace|debug|warn|error|fatal");
                            }
                            return false;
                        })
                        .collect(Collectors.toList());

                if (!logStatements.isEmpty()) {
                    for (ExpressionStmt logStmt : logStatements) {
                        // Modify method to replace log statements with <LOG>
                        MethodDeclaration modified_method = StaticJavaParser.parseMethodDeclaration(method);
                        modified_method.findAll(ExpressionStmt.class).stream()
                                .filter(stmt -> stmt.equals(logStmt))
                                .forEach(stmt -> stmt.replace(new ExpressionStmt(new NameExpr("<LOG>"))));

                        // Save to logposi.txt
                        String nonLogMethod = modified_method.toString();
                        logposi_writer.println(addSpaces(nonLogMethod));

                        // Write method with removed log statements to input.txt
                        MethodDeclaration modified_input_method = StaticJavaParser.parseMethodDeclaration(method);
                        modified_input_method.findAll(ExpressionStmt.class).stream()
                                .filter(stmt -> stmt.equals(logStmt))
                                .forEach(Node::remove);
//                        /* ********** */
//                        System.out.println(modified_input_method);
                        // Save to input.txt
                        input_writer.println(addSpaces(modified_input_method.toString()));
                    }
                }
            }
            /* https://www.baeldung.com/java-io-streams-closing */
            target_writer.close();
            input_writer.close();
            logsta_writer.close();
            logposi_writer.close();
            allMethod_writer.close();
        } catch (Exception e) {
            System.out.println("Error occurred: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void findProjectDirs(File rootDir, List<File> projectDirs) {
        for (File file : rootDir.listFiles()) {
            if(file.isDirectory()) { projectDirs.add(file); }
        }
    }

    // 메서드 추출하는 메서드
    public static void extractMethods(String sourcePath, ArrayList<String> methodList) throws Exception {
        SourceRoot sourceRoot = new SourceRoot(Paths.get(sourcePath));
        List<ParseResult<CompilationUnit>> parseResults = sourceRoot.tryToParse();

        for (ParseResult<CompilationUnit> parseResult : parseResults) {
            if (parseResult.isSuccessful() && parseResult.getResult().isPresent()) {
                CompilationUnit cu = parseResult.getResult().get();

                // Remove All Comments
                List<Comment> comments = cu.getAllContainedComments();
                comments.forEach(Node::remove);

                // Extract Method
                for (MethodDeclaration method : cu.findAll(MethodDeclaration.class)) {
                    // exclusion abstract method
                    if (method.isAbstract()) {
                        continue;
                    }

                    // A value what Starting with Java Access Modifier.
                    boolean isPublicProtectedPrivate = method.getModifiers().stream()
                            .anyMatch(m -> m.getKeyword().asString().equals("public")
                                    || m.getKeyword().asString().equals("protected")
                                    || m.getKeyword().asString().equals("private"));
                    // A value what Starting with Java Annotation.
                    boolean hasOverride = method.getAnnotations().stream()
                            .anyMatch(a -> a.getName().asString().equals("Override")
                                    || a.getName().asString().equals("Overriding"));

                    if (isPublicProtectedPrivate || hasOverride) {
                        // Annotation 변수 저장
                        String annotations = method.getAnnotations().stream()
                                .map(a -> "@" + a.getName().asString() + " ")
                                .collect(Collectors.joining());
                        // Method 정보 저장
                        String method_Name = method.getDeclarationAsString(true, true, true);
                        String method_Body = method.getBody().map(Node::toString).orElse("");
                        String full_Method = annotations + method_Name + method_Body;

                        // Add full method in List
                        methodList.add(full_Method);
                    }
                }
            }
        }
    }

    // 로그 추출 메서드
    public static void extractLogs(ArrayList<String> method_list, ArrayList<String> log_list) {
        for (String method : method_list) {
            // 메소드 본문을 파싱하여 로그 문 추출
            MethodDeclaration methodDecl = StaticJavaParser.parseMethodDeclaration(method);
            List<MethodCallExpr> logCalls = methodDecl.findAll(MethodCallExpr.class).stream()
                    .filter(mc -> mc.getScope().isPresent() && mc.getScope().get().toString().matches(".*log.*|.*LOG.*") &&
                            mc.getNameAsString().matches("info|trace|debug|warn|error|fatal"))
                    .collect(Collectors.toList());

            for (MethodCallExpr logCall : logCalls) {
                String logLevel = logCall.getNameAsString();
                String logMessage = logCall.getArguments().toString();
                if (logMessage.startsWith("[") && logMessage.endsWith("]")) {
                    logMessage = logMessage.substring(1, logMessage.length() - 1);
                }
                log_list.add(logCall+ "$$" + logLevel + "$$" + "(" + logMessage + ")");
            }
        }
    }

    // Tokenization
    private static String addSpaces(String code) {
        // 1. 문자열 리터럴을 임시로 보호하기 위해.
        List<String> protected_strings = new ArrayList<>();
        Matcher stringMatcher = Pattern.compile("\"([^\"]*)\"").matcher(code);
        int index = 0;
        while (stringMatcher.find()) {
            protected_strings.add(stringMatcher.group());
            code = code.replace(stringMatcher.group(), "___STRING" + index++ + "___");
        }

        // 2. String Tokenization
        String tokenized_code = code
                .replaceAll("(<LOG>;)", "PLACEHOLDER_LOG")
                .replaceAll("([{}();.,\\[\\]])", " $1 ")
                .replaceAll("(@)", "$1 ")
                .replaceAll("(<=|>=|==|!=|->|<-)", " $1 ")  // 연산자를 먼저 보호
                .replaceAll("(?<![=!<>])!(?![=!<>])", " ! ")
                .replaceAll("<\\s*>", " < > ")  // 제네릭 <> 처리
                .replaceAll("(?<![<>])<(?![<>])", " < ")  // 제네릭 < 분할
                .replaceAll("(?<![<>])>(?![<>])", " > ")  // 제네릭 > 분할
                .replaceAll("\\s+", " ")
                .replaceAll("PLACEHOLDER_LOG", "<LOG>")
                .replaceAll(" < =", " <=").replaceAll(" > =", " >=")
                .replaceAll(" - >", " ->").replaceAll(" < -", " <-");

        // 3. 보호한 문자열 리터럴을 복원.
        for (int i = 0; i < protected_strings.size(); i++) {
            tokenized_code = tokenized_code.replace("___STRING" + i + "___", protected_strings.get(i));
        }
        return tokenized_code.trim();
    }
}
