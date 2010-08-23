/*
 * This code is developed in Institut Laue-Langevin (France).
 * Its goal is the implementation of parameter search into ICAT Web Service
 * 
 * Created on 6 juil. 2010
 */

package uk.icat3.parametersearch.datafile;

import java.util.ArrayList;
import uk.icat3.parametersearch.exception.ParameterSearchException;
import java.util.List;
import junit.framework.JUnit4TestAdapter;
import org.junit.Test;
import uk.icat3.entity.Datafile;
import static org.junit.Assert.*;
import uk.icat3.parametersearch.BaseParameterSearchTest;
import uk.icat3.parametersearch.ParameterComparator;
import uk.icat3.parametersearch.ParameterOperator;
import uk.icat3.parametersearch.ParameterType;
import uk.icat3.parametersearch.exception.NoParameterTypeException;
import uk.icat3.parametersearch.exception.NoParametersException;
import uk.icat3.parametersearch.util.ParameterValued;
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

        List<ParameterComparator> lc = new ArrayList<ParameterComparator>();
        lc.add(pcDatafile.get(0));

        List<Datafile> ld = (List<Datafile>) DatafileSearch
                .searchByParameterListComparators("SUPER_USER", lc, -1, -1, em);

       showFiles(ld);
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

        ParameterValued pv3 = new ParameterValued(ParameterType.DATAFILE, parameter.get("datafile1"));
        ParameterValued pv4 = new ParameterValued(ParameterType.DATAFILE, parameter.get("datafile2"));

        lp.add(pv3);
        lp.add(pv4);

        List<Datafile> li = (List<Datafile>) DatafileSearch
                .searchByParameterListParameter("SUPER_USER", lp, 1, -1, em);

       showFiles(li);
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
        ParameterOperator op1 = new ParameterOperator(LogicalOperator.OR);
        ParameterOperator op2 = new ParameterOperator(LogicalOperator.AND);

        op2.add(pcDatafile.get(0));
        op2.add(pcDatafile.get(1));
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
