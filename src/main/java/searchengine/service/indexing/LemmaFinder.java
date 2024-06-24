package searchengine.service.indexing;

import searchengine.model.Lemma;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface LemmaFinder {

    Map<String, Integer> collectLemmas(String text);

    Set<String> getLemmaSet(String text);

    Map<String, Set<String>> collectLemmasAndWords(String text);

    List<Lemma> getSortedLemmasSetFromDbAversSorted(Set<String> lemmasSet);
}
