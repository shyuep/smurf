package net.shyue.smurf.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Class containing static methods to sort a provided list into various bins
 * according to some similarity criteria.  The comparison is provided by the
 * areSimilar interface which allows custom similarity criteria to be developed.
 *
 * @author shyue
 */
public final class CollectionBinner {

    private CollectionBinner() {
    }

    /**
     * Method to "bin" a list according to some similarity criteria.  The comparison
     * is provided by the areSimilar interface which allows custom similarity criteria to be developed.
     *
     * Example :
     * One possible use is to group a list of atoms by the element, in which
     * case the areSimilar criteria compares the species and return true is the species
     * are the same.  In this case, this would seem to duplicate the equals construct.
     * But the areSimilar allows for more complex binning, like sorting by both
     * distance within a certain tolerance and species.
     *
     * The returned Map contains a unique identifier key with the values being the
     * list of atoms satisfying the similarity criteria.
     * 
     * @param <K>
     * @param <T>
     * @param list
     * @param comparator
     * @return
     */
    public static <K, T> Map<K, List<T>> group(List<T> list, SimilarityComparator<K, T> comparator) {
        Map<K, List<T>> groupedMap = new HashMap<K, List<T>>();
        for (T item : list) {
            K existingGrp = null;
            for (K key : groupedMap.keySet()) {
                if (comparator.areSimilar(groupedMap.get(key).get(0), item)) {
                    existingGrp = key;
                    break;
                }
            }
            if (existingGrp != null) {
                groupedMap.get(existingGrp).add(item);
            } else {
                List<T> newList = new ArrayList<T>();
                newList.add(item);
                groupedMap.put(comparator.getIdentifier(item), newList);
            }

        }
        return groupedMap;
    }

    /**
     * Generate a string representation of a Map of a binned list.
     * @param <K>
     * @param <T>
     * @param map
     * @return
     */
    public static <K, T> String binnedMapToString(Map<K, List<T>> map) {
        StringBuilder build = new StringBuilder();
        for (K key : map.keySet()) {
            build.append(key+"\n");
            List<T> currentSet = map.get(key);
            for (T at : currentSet) {
                build.append("\t" + at);
            }
        }
        return build.toString();
    }
}
