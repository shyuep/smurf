package net.shyue.smurf.Utils;

/**
 * Interface class for a similarity compartor. Used to develop functional
 * interfaces for the CollectionGrouper group() function which sorts a given
 * list into various bins based on similarity.
 *
 * @param <K> Similarity parameter.
 * @param <T> Type of list.
 * @author shyue
 * @version 0.1
 * 
 */
public interface SimilarityComparator <K,T> {

    /**
     * Returns true if two objects are similar.
     * @param o1
     * @param o2
     * @return
     */
    boolean areSimilar(T o1, T o2);
    
    /**
     * Returns a unique identifer.  Ideally, this should return the same identifier
     * for all similar objects.  For some criteria, e.g. simple species binning,
     * this is easily satisfied.
     * 
     * Where that is not possible or extremely difficult to implement e.g. binning
     * by some floating criteria within a certain tolerance, the identifer should
     * be based on the FIRST object in each binned list.
     * @param o1
     * @return
     */
    K getIdentifier(T o1);

}
