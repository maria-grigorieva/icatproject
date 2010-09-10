/*
 * This code is developed in Institut Laue-Langevin (France).
 * Its goal is the implementation of parameter search into ICAT Web Service
 * 
 * Created on 6 juil. 2010
 */

package uk.icat3.parametersearch.datafile;

import java.util.ArrayList;
import uk.icat3.exceptions.ParameterSearchException;
import java.util.List;
import junit.framework.JUnit4TestAdapter;
import org.junit.Test;
import uk.icat3.entity.Datafile;
import static org.junit.Assert.*;
import uk.icat3.parametersearch.BaseParameterSearchTest;
import uk.icat3.search.parameter.ParameterComparisonCondition;
import uk.icat3.search.parameter.ParameterLogicalCondition;
import uk.icat3.search.parameter.ParameterType;
import uk.icat3.exceptions.NoParameterTypeException;
import uk.icat3.exceptions.NoParametersException;
import uk.icat3.search.parameter.util.ParameterValued;
import uk.icat3.search.DatafileSearch;
import uk.icat3.util.LogicalOperator;

/**
 *
 * @author cruzcruz
 */
public class DatafileTest extends BaseParameterSearchTest {

    /**
     * List comparators Test
     * 
     * @throws NoParameterTypeException
     * @throws ParameterSearchException
     */
    @Test
    public void listComparatorTest () throws NoParameterTypeException, ParameterSearchException {

        List<ParameterComparisonCondition> lc = new ArrayList<ParameterComparisonCondition>();
        lc.add(pcDatafile.get(0));
        lc.add(pcSample.get(0));

        List<Datafile> ld = (List<Datafile>) DatafileSearch
                .searchByParameterListComparators("SUPER_USER", lc, -1, -1, em);

       assertTrue("Results of investigations should not be ZERO", (ld.size() == 1));
    }

    

    /**
     * Parameter list test
     * 
     * @throws NoParameterTypeException
     * @throws NoParametersException
     * @throws ParameterSearchException
     */
    @Test
    public void listParameterTest () throws NoParameterTypeException, NoParametersException, ParameterSearchException {
        List<ParameterValued> lp = new ArrayList<ParameterValued>();

        ParameterValued pv1 = new ParameterValued(ParameterType.DATASET, parameter.get("dataset1"));
        ParameterValued pv2 = new ParameterValued(ParameterType.SAMPLE, parameter.get("sample1"));
        ParameterValued pv3 = new ParameterValued(ParameterType.DATAFILE, parameter.get("datafile1"));
        ParameterValued pv4 = new ParameterValued(ParameterType.DATAFILE, parameter.get("datafile2"));

        lp.add(pv1);
        lp.add(pv2);
        lp.add(pv3);
        lp.add(pv4);

        List<Datafile> li = (List<Datafile>) DatafileSearch
            .searchByParameterListParameter("SUPER_USER", lp, 1, -1, em);

        assertTrue("Results of investigations should not be ZERO", (li.size() == 1));
    }

    /**
     * Operable test
     * 
     * @throws NoParameterTypeException
     * @throws ParameterSearchException
     */
    @Test
    public void operableTest () throws NoParameterTypeException, ParameterSearchException {
        ParameterLogicalCondition op1 = new ParameterLogicalCondition(LogicalOperator.OR);
        ParameterLogicalCondition op2 = new ParameterLogicalCondition(LogicalOperator.AND);

        op2.add(pcDatafile.get(0));
        op2.add(pcDatafile.get(1));
        op2.add(pcSample.get(0));
        op2.add(pcDataset.get(0));
        op1.add(op2);
        op1.add(pcDatafile.get(2));

        List<Datafile> li = (List<Datafile>) DatafileSearch
                .searchByParameterOperable("SUPER_USER", op1, 1, -1, em);
        
       assertTrue("Results of investigations should be 2 not " + li.size(), (li.size() == 2));
    }
    

    public static junit.framework.Test suite(){
        return new JUnit4TestAdapter(DatafileTest.class);
    }
}
