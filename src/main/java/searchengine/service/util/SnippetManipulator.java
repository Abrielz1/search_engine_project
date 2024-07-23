package searchengine.service.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import searchengine.model.Lemma;
import searchengine.service.indexing.LemmaFinder;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class SnippetManipulator {

    private final LemmaFinder lemmaFinder;

    private final String START_TAG = "<b>";

    private final String END_TAG = "</b>";

    private final int SNIPPET_LENGTH = 240;

    public String createSnippet(String pageText,
                                List<Lemma> sortedLemmas) {
        String threeDots = " ...";
        String[] textArray = pageText.split(" ");
        StringBuilder resultText = new StringBuilder();
        Map<String, Set<String>> mapOfLemmasAndForms = lemmaFinder.collectLemmasAndWords(pageText);

        for (String word : textArray) {
            for (Lemma lemma : sortedLemmas) {
                word = this.checkWord(word,
                        lemma.getLemma(),
                        mapOfLemmasAndForms);
            }

            resultText.append(word).append(" ");
        }

        if (!resultText.toString().contains("<b>") && !resultText.toString().contains("<b>")) {
            return null;
        }

        return this.snippedFinalizer(resultText.toString());
    }

    private String checkWord(String word,
                             String lemma,
                             Map<String, Set<String>> lemmasAndWords) {

        String[] formattedWord = word
                .replaceAll("([^А-Яа-яЁё\\-])", " ")
                .trim().split("[- ]");

        for (String part : formattedWord) {

            if (lemmasAndWords.get(lemma) == null) {
                return word;
            }

            part = part.replaceAll("Ё", "Е")
                    .replaceAll("ё", "е")
                    .trim()
                    .toLowerCase(Locale.ROOT);

            if (lemmaFinder.getNormalWordForm(part) != null) {
                part = lemmaFinder.getNormalWordForm(part);
            } else {
                continue;
            }

            if (part == null) {
                continue;
            }

            if ((lemmasAndWords.get(lemma).stream().anyMatch(part
                    ::equalsIgnoreCase))) {

//                word = word.replace(part, START_TAG
//                        .concat(part).concat(END_TAG));

                String part1 = word.replace(part, START_TAG);
                String part2 = word.replace(part, END_TAG);
                word = part1 + part + part2;
            }
        }

        return word;
    }

    private String snippedFinalizer(String resultText) {
        if (resultText == null) {
            return "";
        }

        int startIndex = this.startIndexOfTextFinder(resultText);

        String text = resultText.substring(startIndex,
                resultText.length() - 1);

        String rawText;

        rawText = text.replace("</?b>", "");
        int len = rawText.length() - 1;
        if (startIndex < SNIPPET_LENGTH) {
            rawText = text.substring(startIndex, SNIPPET_LENGTH);
        } else {
            rawText = rawText.substring(0, Math.min(len, SNIPPET_LENGTH));
        }

        String snippet = "";

        int tagsCounter = StringUtils.countOccurrencesOf(resultText,
                                                         START_TAG);

        for (int i = tagsCounter; i >= 1; i--) {
            int end = text.indexOf(" ", SNIPPET_LENGTH + i * (START_TAG.length() + END_TAG.length()));

            if (text.length() <= 1 || end <= 1) {
                break;
            }

            snippet = text.substring(0, end);

            if (snippet.replaceAll("</?b>", "").length() < rawText.length()) {
                break;
            }
        }

        return snippet;
    }

    private Integer startIndexOfTextFinder(String resultText) {

//        if (!resultText.contains(START_TAG)) {
//            return 0;
//        }

        String[] split = resultText.substring(resultText.indexOf(START_TAG),
                        resultText.indexOf(END_TAG))
                .split("(?<=[.!?·])\\s*(?=(<b>|\"|«|· )?[А-ЯЁ])");

        String maxSentence = "";
        int maxValue = 0;

        for (String word : split) {
            int count = StringUtils.countOccurrencesOf(word, START_TAG);

            if (count > maxValue) {
                maxValue = count;
                maxSentence = word;
            }
        }

        int startIndex = resultText.indexOf(START_TAG,
                resultText.indexOf(maxSentence));

        int leftSpace = SNIPPET_LENGTH / 6;

        return resultText.indexOf(" ", startIndex > leftSpace ?
                startIndex - leftSpace : startIndex);

    }
}
