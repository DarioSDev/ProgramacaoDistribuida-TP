package pt.isec.pd.utils;

import pt.isec.pd.common.StudentAnswerInfo;
import pt.isec.pd.common.TeacherResultsData;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;

public class FileManager {

    public static void exportToCSV(File file, TeacherResultsData data) throws IOException {
        String sep = ";";

        try (PrintWriter pw = new PrintWriter(file, StandardCharsets.UTF_8)) {

            pw.println("\"dia\"" + sep + "\"hora inicial\"" + sep + "\"hora final\"" + sep + "\"enunciado da pergunta\"" + sep + "\"opção certa\"");

            pw.printf("\"%s\"%s\"%s\"%s\"%s\"%s\"%s\"%s\"%s\"%n",
                    data.getDate(), sep,
                    data.getStartTime(), sep,
                    data.getEndTime(), sep,
                    escape(data.getQuestionText()), sep,
                    data.getCorrectOptionLetter()
            );

            pw.println();

            pw.println("\"opção\"" + sep + "\"texto da opção\"");
            List<String> options = data.getOptions();
            for (int i = 0; i < options.size(); i++) {
                char letter = (char) ('a' + i);
                pw.printf("\"%s\"%s\"%s\"%n", letter, sep, escape(options.get(i)));
            }

            pw.println();

            pw.println("\"número de estudante\"" + sep + "\"nome\"" + sep + "\"e-mail\"" + sep + "\"resposta\"");

            for (StudentAnswerInfo info : data.getAnswers()) {
                pw.printf("\"%s\"%s\"%s\"%s\"%s\"%s\"%s\"%n",
                        safe(info.getStudentNumber()), sep,
                        safe(info.getStudentName()), sep,
                        safe(info.getStudentEmail()), sep,
                        safe(info.getAnswerLetter())
                );
            }

            pw.flush();
        }
    }

    private static String escape(String text) {
        if (text == null) return "";
        return text.replace("\"", "\"\"");
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}