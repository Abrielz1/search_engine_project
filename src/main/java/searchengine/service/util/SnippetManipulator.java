package searchengine.service.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import searchengine.model.Lemma;
import searchengine.service.indexing.LemmaFinder;

import java.util.List;
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

        String[] textArray = pageText.split("\\s+");
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

        return this.snippedFinalizer(resultText.toString().concat(" ..."));
    }

    private String checkWord(String word,
                             String lemma,
                             Map<String, Set<String>> mapOfLemmasAndForms) {
        String[] splitWord = word.split("\\s+");

        for (String proceedWord : splitWord) {

            if (mapOfLemmasAndForms.get(lemma) == null) {
                return word;
            }

            if (mapOfLemmasAndForms.get(lemma).stream()
                    .anyMatch(proceedWord
                            .replaceAll("Ё", "е")
                            .replaceAll("Ё", "е")
                            ::equalsIgnoreCase)) {

                word = word.replace(proceedWord,
                        START_TAG.concat(proceedWord).concat(END_TAG));
            }
        }

        return word;
    }

    private String snippedFinalizer(String resultText) {
        String snippet = "";
        Integer startIndex = this.startIndexOfTextFinder(resultText);

        String text = resultText.substring(startIndex,
                resultText.length() - 1);

        String rawText = text.replace("</?b>", "")
                .substring(0, SNIPPET_LENGTH);

        int tagsCounter = StringUtils.countOccurrencesOf(resultText,
                START_TAG);

        for (int i = tagsCounter; i >= 1; i--) {
            int end = text.indexOf(" ", SNIPPET_LENGTH + i * (START_TAG.length() + END_TAG.length()));
            snippet = text.substring(0, end);

            if (snippet.replaceAll("</?b>", "").length() < rawText.length()) {
                break;
            }
        }

        return snippet;
    }

    private Integer startIndexOfTextFinder(String resultText) {

        if (!resultText.contains(START_TAG)) {
            return 0;
        }

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
