package searchengine.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class LemmaFinderImpl implements LemmaFinder {

    private final LuceneMorphology luceneMorphology = new RussianLuceneMorphology();

    private final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ"};

    public LemmaFinderImpl() throws IOException {
    }

    @Override
    public Map<String, Integer> collectLemmas(String text) {

        String[] strings = stringManipulator(text);
        Map<String, Integer> lemmas = new HashMap<>();

        for (String word : strings) {
            if (isWrongWord(word)) {
                continue;
            }

            List<String> normalWordForms = luceneMorphology
                    .getNormalForms(word);

            if (normalWordForms.isEmpty()) {
                continue;
            }

            String normalWord = normalWordForms.get(0);

            if (lemmas.containsKey(normalWord)) {
                lemmas.put(normalWord, lemmas.get(word) + 1);
            } else {
                lemmas.put(word, 1);
            }
        }

        return lemmas;
    }

    @Override
    public Set<String> getLemmaSet(String text) {
        return null;
    }

    @Override
    public Map<String, Set<String>> collectLemmasAndWords(String text) {
        return null;
    }

    private String[] stringManipulator(String text) {
        return text.toLowerCase(Locale.ROOT)
                .replaceAll("Ё", "Е")
                .replaceAll("ё", "е")
                .replaceAll("([^а-я]\\s])", " ")
                .trim()
                .split("\\s+");
    }

    private boolean isWrongWord(String word) {
        if (!StringUtils.hasText(word)) {
            return true;
        }

        List<String> baseWordFormsList = luceneMorphology.getMorphInfo(word);
        return isWordParticle(baseWordFormsList);
    }

    private boolean isParticle(String word) {
        String base = word.toUpperCase().substring(word.indexOf("|"));

        for (String i : particlesNames) {
            if (base.contains(i)) {
                return true;
            }
        }
        return false;
    }

    private boolean isWordParticle(List<String> baseWordFormsList) {
        return baseWordFormsList.stream().anyMatch(this::isParticle);
    }
}