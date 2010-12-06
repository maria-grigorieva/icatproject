/*
 * This code is developed in Institut Laue-Langevin (France).
 * Its goal is the implementation of parameter search into ICAT Web Service
 * 
 * Created on 22 nov. 2010
 */

package uk.icat3.restriction;

import uk.icat3.restriction.attribute.RestrictionAttributes;

/**
 * This class represents restriction conditions with which we
 * construct the restrictions structures.
 *
 * @author cruzcruz
 */
public class RestrictionCondition {
    /** Indicates if restriction condition is negated (NOT) */
    private boolean negate = false;
    /** Contain order by field, if exists */
    private RestrictionAttributes orderByAttr = null;
    /** Indicates the order direction */
    private boolean orderByAsc = true;
    /** Maximun number of results to return */
    private Integer maxResults;

    /**
     * Negates the restriction condition 'cond'
     *
     * @param cond Restriction condition to negate
     * @return
     */
    public static RestrictionCondition Not (RestrictionCondition cond) {
        cond.negate = true;
        return cond;
    }
    /**
     * Creates an order by descend object
     *
     * @param attr Attriube to order by
     * @return
     */
    public static RestrictionCondition orderByDesc (RestrictionAttributes attr) {
        RestrictionCondition a = new RestrictionCondition();
        a.setOrderByDesc(attr);
        return a;
    }
    /**
     * Creates an order by ascend object
     *
     * @param attr Attriube to order by
     * @return
     */
    public static RestrictionCondition orderByAsc (RestrictionAttributes attr) {
        RestrictionCondition a = new RestrictionCondition();
        a.setOrderByAsc(attr);
        return a;
    }

    /**
     * Set descending order and attribute
     *
     * @param attr Attribute to order by
     */
    public RestrictionCondition setOrderByDesc (RestrictionAttributes attr) {
        this.orderByAttr = attr;
        this.orderByAsc = false;
        return this;
    }

    /**
     * Check if max results was set
     *
     * @return true it max results was set. Otherwise false.
     */
    public boolean hasMaxResults () {
        if (this.maxResults == null || this.maxResults < 0)
            return false;
        return true;
    }

    ////////////////////////////////////////////////////////////////////
    //                   GETTERS and SETTERS                          //
    ////////////////////////////////////////////////////////////////////

    /**
     * Set ascending order and attribute
     *
     * @param attr Attribute to order by
     */
    public void setOrderByAsc (RestrictionAttributes attr) {
        this.orderByAttr = attr;
        this.orderByAsc = true;
    }
    
    /**
     * Check if this condition is negated
     *
     * @return True if this condition is negated. Otherwise false
     */
    public boolean isNegate() {
        return negate;
    }
    /**
     * Get attribute to order by.
     *
     * @return Attribute to order by.
     */
    public RestrictionAttributes getOrderByAttr() {
        return orderByAttr;
    }

    /**
     * Check if the restriction has order by
     * 
     * @return true if has order by. Otherwise false
     */
    public boolean hasOrder () {
        if (this.orderByAttr == null)
            return false;
        return true;
    }
    /**
     * Check if order is Ascending
     *
     * @return True if this condition is negated. Otherwise false
     */
    public boolean isOrderByAsc() {
        return orderByAsc;
    }
    /**
     * Negate this condition
     *
     * @return True if this condition is negated. Otherwise false
     */
    public void setNegate(boolean negate) {
        this.negate = negate;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    public void setOrderByAsc(boolean orderByAsc) {
        this.orderByAsc = orderByAsc;
    }

    public void setOrderByAttr(RestrictionAttributes orderByAttr) {
        this.orderByAttr = orderByAttr;
    }

    /**
     * Number maximun of results to return.
     *
     * @return maximun results to return
     */
    public int getMaxResults() {
        if (maxResults == null)
            return -1;
        return maxResults;
    }
}
