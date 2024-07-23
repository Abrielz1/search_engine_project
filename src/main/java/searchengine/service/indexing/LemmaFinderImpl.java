package searchengine.service.indexing;

import lombok.extern.slf4j.Slf4j;
import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import searchengine.model.Lemma;
import searchengine.repository.LemmaRepository;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
public class LemmaFinderImpl implements LemmaFinder {

    private final LemmaRepository lemmaRepository;

    private final LuceneMorphology luceneMorphology = new RussianLuceneMorphology();

    private final String[] particlesNames = new String[]{"МЕЖД", "ПРЕДЛ", "СОЮЗ"};

    public LemmaFinderImpl(LemmaRepository lemmaRepository) throws IOException {
        this.lemmaRepository = lemmaRepository;
    }

    @Override
    public Map<String, Integer> collectLemmas(String text) {

        Map<String, Integer> lemmas = new HashMap<>();

        for (String word : stringManipulator(text)) {
            if (this.isWrongWord(word)) {
                continue;
            }

            List<String> normalWordForms = luceneMorphology
                    .getNormalForms(word);

            if (normalWordForms.isEmpty()) {
                continue;
            }

            if (lemmas.containsKey(normalWordForms.get(0))) {
                lemmas.put(normalWordForms.get(0),
                        lemmas.get(normalWordForms.get(0)) + 1);
            } else {
                lemmas.put(normalWordForms.get(0), 1);
            }
        }

        return lemmas;
    }

    @Override
    public Set<String> getLemmaSet(String text) {

        Set<String> lemmasSet = new HashSet<>();

        for (String word: this.stringManipulator(text)) {

            if (isWrongWord(word)) {
                continue;
            }

            List<String> normalWordForms = luceneMorphology.getNormalForms(word);

            if(normalWordForms.isEmpty()){
                continue;
            }

            lemmasSet.add(normalWordForms.get(0));
        }

        return lemmasSet;
    }

    @Override
    public Map<String, Set<String>> collectLemmasAndWords(String text) {

        Map<String, Set<String>> mapLemmasAndWords = new HashMap<>();

        for (String word : this.stringManipulator(text)) {

            if (isWrongWord(word)) {
                continue;
            }

            List<String> normalWordForms = luceneMorphology.getNormalForms(word);

            if(normalWordForms.isEmpty()){
                continue;
            }

            if (mapLemmasAndWords.containsKey(normalWordForms.get(0))) {
                Set<String> temp = new HashSet<>(mapLemmasAndWords.get(normalWordForms.get(0)));
                temp.add(normalWordForms.get(0));
                mapLemmasAndWords.put(normalWordForms.get(0), temp);
            } else {
                mapLemmasAndWords.put(normalWordForms.get(0), new HashSet<>() {{
                    add(normalWordForms.get(0));
                }});
            }
        }

        return mapLemmasAndWords;
    }

    @Override
    public List<Lemma> getSortedLemmasSetFromDbAversSorted(Set<String> lemmasSet) {

        List<Lemma> lemmaList = lemmaRepository.findByLemma(lemmasSet);

        if (lemmaList.size() < lemmasSet.size()) {
            return null;
        }

        return lemmaList;
    }

    @Override
    public String getNormalWordForm(String word) {

        if (this.isWrongWord(word)) {
            return null;
        }

        List<String> normalWordForms = luceneMorphology
                .getNormalForms(word);

        if (normalWordForms.isEmpty()) {
            return null;
        }

        return luceneMorphology
                .getNormalForms(word).get(0);
    }

    private String[] stringManipulator(String text) {

        return text.toLowerCase(Locale.ROOT)
                .replaceAll("ё", "е")
                .replaceAll("Ё", "Е")
                .replaceAll("([^а-я\\s])", " ")
                .trim()
                .split("\\s+");
    }

    private boolean isWrongWord(String word) {

        if (!StringUtils.hasText(word)) {
            return true;
        }

        List<String> baseWordFormsList = luceneMorphology.getMorphInfo(word);
        return this.isWordParticle(baseWordFormsList);
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