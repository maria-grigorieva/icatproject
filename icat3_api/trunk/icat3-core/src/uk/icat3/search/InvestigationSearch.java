/*
 * InvestigationSearch.java
 *
 * Created on 20 February 2007, 11:00
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */

package uk.icat3.search;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import org.apache.log4j.Logger;
import uk.icat3.entity.Instrument;
import uk.icat3.entity.Investigation;
import uk.icat3.exceptions.InsufficientPrivilegesException;
import uk.icat3.manager.ManagerUtil;
import uk.icat3.security.GateKeeper;
import uk.icat3.util.AccessType;
import uk.icat3.util.InvestigationInclude;
import uk.icat3.util.LogicalOperator;
import static uk.icat3.util.Queries.*;

/**
 * This is the service to allows access to search through the icat schema.
 * Checks are made through SQL and JPQL for access rights to view investigations
 *
 * @author Glen Drinkwater
 */
public class InvestigationSearch extends ManagerUtil {
    
    // Global class logger
    static Logger log = Logger.getLogger(InvestigationSearch.class);
    
    //used for type of user search
    private enum SearchType { SURNAME, USERID };
    
    
    /**
     * Searches a single keyword for a users and returns all the Id of the investigations
     *
     * @param userId federalId of the user.
     * @param keyword keywords to search
     * @param startIndex start index of the results found
     * @param number_results number of results found from the start index
     * @param manager manager object that will facilitate interaction with underlying database
     * @return collection investigation ids
     */
    public static Collection<Long>  searchByKeywordRtnId(String userId, String keyword, int startIndex, int number_results, EntityManager manager)  {
        log.trace("searchByKeyword("+userId+", "+keyword+", "+startIndex+", "+number_results+", EntityManager)");
        
        Collection<BigDecimal> investigationsId = null;
        if(number_results < 0){
            //get all, maybe should limit this to 500?
            investigationsId = manager.createNamedQuery(INVESTIGATION_NATIVE_LIST_BY_KEYWORD_RTN_ID).setParameter(1,userId).setParameter(2,"%"+keyword+"%").setMaxResults(MAX_QUERY_RESULTSET).getResultList();
        } else {
            //list all Investigation ids that the users has access to
            investigationsId = manager.createNamedQuery(INVESTIGATION_NATIVE_LIST_BY_KEYWORD_RTN_ID).setParameter(1,userId).setParameter(2,"%"+keyword+"%").setMaxResults(number_results).setFirstResult(startIndex).getResultList();
        }
        //turn into longs
        Collection<Long> investigationsIds = new ArrayList<Long>();
        for(BigDecimal bd : investigationsId){
            investigationsIds.add(bd.longValue());
        }
        return investigationsIds;
    }
    
    /**
     * Searches a single keyword for a user and returns all the Id of the investigations
     *
     * @param userId federalId of the user.
     * @param keyword keyword to search
     * @param manager manager object that will facilitate interaction with underlying database
     * @return collection of investigation ids
     */
    public static Collection<Long> searchByKeywordRtnId(String userId, String keyword, EntityManager manager)  {
        //search and return all investigations
        return  searchByKeywordRtnId(userId, keyword, -1, -1, manager);
    }
    
    /**
     * Searches the investigations the user has access to view by keyword
     *
     * @param userId federalId of the user.
     * @param keyword keyword to search
     * @param startIndex start index of the results found
     * @param number_results number of results found from the start index
     * @param manager manager object that will facilitate interaction with underlying database
     * @return collection of {@link Investigation} investigation objects
     */
    private static Collection<Investigation>  searchByKeywordImpl(String userId, String keyword, int startIndex, int number_results, EntityManager manager)  {
        log.trace("searchByKeyword("+userId+", "+keyword+", "+startIndex+", "+number_results+", EntityManager)");
        
        Collection<Investigation> investigations = null;
        if(number_results < 0){
            //get all, maybe should limit this to 500?
            investigations = manager.createNamedQuery(INVESTIGATION_NATIVE_LIST_BY_KEYWORD).setParameter(1,userId).setParameter(2,"%"+keyword+"%").setMaxResults(MAX_QUERY_RESULTSET).getResultList();
        } else {
            //list all Investigation ids that the users has access to
            investigations = manager.createNamedQuery(INVESTIGATION_NATIVE_LIST_BY_KEYWORD).setParameter(1,userId).setParameter(2,"%"+keyword+"%").setMaxResults(number_results).setFirstResult(startIndex).getResultList();
        }
        return investigations;
    }
    
    /**
     * Searches the investigations the user has access to view by keyword
     *
     * @param userId federalId of the user.
     * @param keyword keyword to search
     * @param manager manager object that will facilitate interaction with underlying database
     * @return collection of {@link Investigation} investigation objects
     */
    public static Collection<Investigation> searchByKeyword(String userId, String keyword, EntityManager manager)  {
        //search and return all investigations
        return  searchByKeywordImpl(userId, keyword, -1, -1, manager);
    }
    
    /**
     * Searches the investigations the user has access to view by keyword
     *
     * @param userId federalId of the user.
     * @param keyword keyword to search
     * @param startIndex start index of the results found
     * @param number_results number of results found from the start index
     * @param manager manager object that will facilitate interaction with underlying database
     * @return collection of {@link Investigation} investigation objects
     */
    public static Collection<Investigation> searchByKeyword(String userId, String keyword, int startIndex, int number_results, EntityManager manager) {
        return  searchByKeywordImpl(userId, keyword, startIndex, number_results, manager);
    }
    
    /**
     * Searches the investigations the user has access to view federalId or surname
     *
     * @param userId federalId of the user.
     * @param searchString federalId or surname
     * @param startIndex start index of the results found
     * @param number_results number of results found from the start index
     * @param manager manager object that will facilitate interaction with underlying database
     * @return collection of {@link Investigation} investigation objects
     */
    private  static Collection<Investigation> searchByUserSurnameImpl(String userId, String searchString, SearchType searchType, int startIndex, int number_results, InvestigationInclude include, EntityManager manager)  {
        log.trace("searchByUserImpl("+userId+", "+searchType+", "+searchString+", "+startIndex+", "+number_results+", "+include+", EntityManager)");
        Collection<Investigation> investigations = null;
        if(number_results < 0){
            
            //get all, maybe should limit this to 500?
            if(searchType == searchType.SURNAME){
                log.trace("Searching by SURNAME");
                investigations = manager.createNamedQuery(INVESTIGATION_LIST_BY_SURNAME).setParameter("userId",userId).setParameter("surname","%"+searchString+"%").setMaxResults(MAX_QUERY_RESULTSET).getResultList();
            } else {
                log.trace("Searching by USERID");
                investigations = manager.createNamedQuery(INVESTIGATION_LIST_BY_USERID).setParameter("userId",userId).setParameter("userIdSearched","%"+searchString+"%").setMaxResults(MAX_QUERY_RESULTSET).getResultList();
            }
        } else {
            if(searchType == searchType.SURNAME){
                //list all Investigation ids that the users has access to
                log.trace("Searching by SURNAME");
                investigations = manager.createNamedQuery(INVESTIGATION_LIST_BY_SURNAME).setParameter("userId",userId).setParameter("surname","%"+searchString+"%").setMaxResults(number_results).setFirstResult(startIndex).getResultList();
            } else {
                log.trace("Searching by USERID");
                investigations = manager.createNamedQuery(INVESTIGATION_LIST_BY_USERID).setParameter("userId",userId).setParameter("userIdSearched","%"+searchString+"%").setMaxResults(number_results).setFirstResult(startIndex).getResultList();
            }
        }
        
        //add all the investigation information to the list of investigations
        getInvestigationInformation(userId, investigations,include, manager);
        
        
        return investigations;
    }
    
    /**
     * Searches the investigations the user has access to view by investigator surname
     *
     * @param userId federalId of the user.
     * @param surname investigator surname
     * @param manager manager object that will facilitate interaction with underlying database
     * @return collection of {@link Investigation} investigation objects
     */
    public static Collection<Investigation> searchByUserSurname(String userId, String surname, EntityManager manager)  {
        //search and return all investigations
        return  searchByUserSurnameImpl(userId, surname, SearchType.SURNAME, -1, -1, InvestigationInclude.NONE, manager);
    }
    
    /**
     * Searches the investigations the user has access to view by investigator surname
     *
     * @param userId federalId of the user.
     * @param surname investigator surname
     * @param startIndex start index of the results found
     * @param number_results number of results found from the start index
     * @param manager manager object that will facilitate interaction with underlying database
     * @return collection of {@link Investigation} investigation objects
     */
    public static Collection<Investigation> searchByUserSurname(String userId, String surname, int startIndex, int number_results, EntityManager manager)  {
        return  searchByUserSurnameImpl(userId, surname, SearchType.SURNAME, startIndex, number_results, InvestigationInclude.NONE, manager);
    }
    
    /**
     * Searches the investigations the user has access to view by federalId
     *
     * @param userId federalId of the user.
     * @param searchUserId federalId of user
     * @param manager manager object that will facilitate interaction with underlying database
     * @return collection of {@link Investigation} investigation objects
     */
    public static Collection<Investigation> searchByUserID(String userId, String searchUserId, EntityManager manager) {
        //search and return all investigations
        return  searchByUserSurnameImpl(userId, searchUserId, SearchType.USERID, -1, -1, InvestigationInclude.NONE, manager);
    }
    
    /**
     * Searches the investigations the user has access to view by federalId
     *
     * @param userId federalId of the user.
     * @param searchUserId federalId of user
     * @param startIndex start index of the results found
     * @param number_results number of results found from the start index
     * @param manager manager object that will facilitate interaction with underlying database
     * @return collection of {@link Investigation} investigation objects
     */
    public static Collection<Investigation> searchByUserID(String userId, String searchUserId, int startIndex, int number_results, EntityManager manager)  {
        return  searchByUserSurnameImpl(userId, searchUserId, SearchType.USERID, startIndex, number_results,InvestigationInclude.NONE, manager);
    }
    
    /**
     *  Searches investigations from the ones they can view by the advanced criteria
     *
     * @param userId federalId of the user.
     * @param advanDTO {@Link AdvancedSearchDetails}
     * @param startIndex start index of the results found
     * @param number_results number of results found from the start index
     * @param manager manager object that will facilitate interaction with underlying database
     * @return collection of {@link Investigation} investigation objects
     */
    private static Collection<Investigation> searchByAdvancedImpl(String userId, AdvancedSearchDetails advanDTO, int startIndex, int number_results, EntityManager manager){
        if(advanDTO == null) throw new IllegalArgumentException("AdvancedSearchDTO cannot be null");
        log.trace("searchByAdvancedImpl("+userId+", "+advanDTO);
        
        Collection<Investigation> investigations = null;
        
        //dynamically create the query
        String SQL = ADVANCED_SEARCH_SQL_1;
        
        if(advanDTO.isInstruments()){
            //add insturments section: AND instrument IN(?,?,?,etc)
            SQL += "AND instrument IN(";
            //add in the instruments in the IN() cause of SQL
            int i = 1;
            for(String instrument : advanDTO.getInstruments()){
                if(i == advanDTO.getInstruments().size()) SQL += "?instrument"+(i++)+"";
                else  SQL += "?instrument"+(i++)+" , ";
            }
            SQL += ")";
        }
        
        //check for other parameters now
        if(advanDTO.isOtherParameters()){
            
            SQL += " AND id IN(";
            boolean firstSearch = false;
            
            if(advanDTO.isInvestigators()){
                //add investigator section:
                /* SELECT i.investigation_id
                    FROM investigator i, facility_user fu
                    WHERE i.facility_user_id = fu.facility_user_id
                    AND (Lower(fu.last_name) LIKE '%'||Lower(?)||'%'
                    OR Lower(fu.last_name) LIKE '%'||Lower(?)||'%')
                 */
                
                SQL += "SELECT i.investigation_id "+
                        "FROM investigator i, facility_user fu "+
                        "WHERE i.facility_user_id = fu.facility_user_id "+
                        "AND (";
                
                int i = 1;
                for(String investigators : advanDTO.getInvestigators()){
                    if(i == 1) SQL += "Lower(fu.last_name) LIKE '%'||Lower(?investigator"+(i++)+")||'%'";
                    else  SQL += "OR Lower(fu.last_name) LIKE '%'||Lower(?investigator"+(i++)+")||'%'";
                }
                SQL += ")";
                
                firstSearch = true;
            }
            
            if(advanDTO.isKeywords()){
                //add keywords section:
                /*
                    SELECT investigation_id
                    FROM keyword
                    WHERE Lower(name) LIKE '%'||Lower(?)||'%'
                    OR Lower(name) LIKE '%'||Lower(?)||'%'
                    OR Lower(name) LIKE '%'||Lower(?)||'%'
                 */
                if(firstSearch) SQL += " INTERSECT ";
                firstSearch = true;
                
                SQL += "SELECT investigation_id FROM keyword WHERE ";
                int i = 1;
                for(String keyword : advanDTO.getKeywords()){
                    if(i == 1) SQL += "Lower(name) LIKE '%'||Lower(?keyword"+(i++)+")||'%'";
                    else  SQL += "OR Lower(name) LIKE '%'||Lower(?keyword"+(i++)+")||'%'";
                }
            }
            
            if(advanDTO.getSampleName() != null){
                //add sample section
                 /*
                    SELECT investigation_id
                    FROM sample
                    WHERE Lower(name) LIKE '%'||Lower(?)||'%'
                  **/
                
                if(firstSearch) SQL += " INTERSECT ";
                firstSearch = true;
                
                SQL += "SELECT investigation_id FROM sample WHERE Lower(name) LIKE '%'||Lower(?sampleName)||'%' ";
            }
            
            //is run number
            if(advanDTO.isRunNumber()){
                log.trace("Searching data file name, run number and create time only");
                //add data file and run number section
                /*
                    SELECT ds.investigation_id
                    FROM dataset ds, DATAFILE df, datafile_parameter dfp
                    WHERE df.dataset_id = ds.id
                    AND (InStr(Lower(df.name),Lower(?)) > 0 OR ? IS NULL)
                    AND ((df.datafile_create_time >= ?date1 AND df.datafile_create_time < (?date2-1))
                    OR ?date1 IS NULL)
                    AND dfp.datafile_id = df.id
                    AND dfp.NAME = 'run_number'
                    AND dfp.numeric_value BETWEEN ?1 AND ?2
                 */
                if(firstSearch) SQL += " INTERSECT ";
                firstSearch = true;
                
                SQL += "SELECT ds.investigation_id "+
                        "FROM dataset ds, DATAFILE df, DATAFILE_PARAMETER dfp "+ //partitioned tabel instaed of datafile_parameter
                        "WHERE df.dataset_id = ds.id "+
                        "AND (Lower(df.name) LIKE '%'||Lower(?datafile_name)||'%' OR ?datafile_name IS NULL) "+
                        "AND (( (df.datafile_create_time >= ?startDate AND df.datafile_create_time < (?endDate))) "+
                        "OR df.datafile_create_time IS NULL) "+
                        "AND dfp.datafile_id = df.id "+
                        "AND dfp.NAME = 'run_number' "+
                        "AND dfp.numeric_value BETWEEN ?lower AND ?upper ";
                
            } else if(advanDTO.isDatFileParameters()){
                log.trace("Searching data file name and create time only");
                //add datafile only section
                /*
                    SELECT ds.investigation_id
                    FROM dataset ds, DATAFILE df
                    WHERE df.dataset_id = ds.id
                    AND (InStr(Lower(df.name),Lower(?)) > 0 OR ? IS NULL)
                    AND ((df.datafile_create_time >= ?date1 AND df.datafile_create_time < (?date2-1))
                    OR ?date1 IS NULL)
                 */
                if(firstSearch) SQL += " INTERSECT ";
                firstSearch = true;
                
                SQL += " SELECT ds.investigation_id "+
                        "FROM dataset ds, DATAFILE df "+
                        "WHERE df.dataset_id = ds.id "+
                        "AND (Lower(df.name) LIKE '%'||Lower(?datafile_name)||'%' OR ?datafile_name IS NULL) "+
                        "AND (( (df.datafile_create_time >= ?startDate AND df.datafile_create_time < (?endDate))) OR df.datafile_create_time IS NULL)";
            }
            
            SQL += ")";
        }
        
        //set all the paramaters now
        //set query with datafile as entity object
        Query query = manager.createNativeQuery(SQL,Investigation.class);
        
        //sets the paramters
        query = query.setParameter("userId",userId);
        query = query.setParameter("inv_title",advanDTO.getInvestigationName());
        query = query.setParameter("bcat_inv_str",advanDTO.getBackCatalogueInvestigatorString());
        query = query.setParameter("inv_number",advanDTO.getExperimentNumber());
        
        //set upper run number
        if(advanDTO.isRunNumber()){
            if(advanDTO.getRunEnd() != null) query = query.setParameter("upper",advanDTO.getRunEnd());
            else query = query.setParameter("upper",1000000000L);
            
            //set lower run number
            if(advanDTO.getRunStart() != null) query = query.setParameter("lower",advanDTO.getRunStart());
            else query = query.setParameter("lower",0L);
            
            //dates need to be added
            query.setParameter("startDate",advanDTO.getYearRangeStart());
            query.setParameter("endDate",advanDTO.getYearRangeEnd());
            query.setParameter("datafile_name",advanDTO.getDatafileName());
        }
        //set data file name
        if(advanDTO.isDatFileParameters()){
            //dates need to be added
            query.setParameter("startDate",advanDTO.getYearRangeStart());
            query.setParameter("endDate",advanDTO.getYearRangeEnd());
            query.setParameter("datafile_name",advanDTO.getDatafileName());
        }
        
        //set instruments
        if(advanDTO.isInstruments()){
            int j = 1;
            for(String instrument : advanDTO.getInstruments()){
                query = query.setParameter("instrument"+j++,instrument);
            }
        }
        
        //set instruments
        if(advanDTO.isKeywords()){
            int j = 1;
            for(String keyword : advanDTO.getKeywords()){
                query = query.setParameter("keyword"+j++,keyword);
            }
        }
        
        //set investigators
        if(advanDTO.isInvestigators()){
            int j = 1;
            for(String investigator : advanDTO.getInvestigators()){
                query = query.setParameter("investigator"+j++,investigator);
            }
        }
        
        //set sample name
        if(advanDTO.getSampleName() != null){
            query.setParameter("sampleName",advanDTO.getSampleName());
        }
        
        log.trace("DYNAMIC SQL: "+SQL);
        
        if(number_results < 0){
            //get all, maybe should limit this to 500?
            investigations = query.setMaxResults(MAX_QUERY_RESULTSET).getResultList();
        } else {
            investigations = query.setMaxResults(number_results).setFirstResult(startIndex).getResultList();
        }
        
        Collection<Investigation> allowed = new ArrayList<Investigation>();
        //TODO no security on this search so done at the end
        for (Investigation investigation : investigations) {
            try {
                GateKeeper.performAuthorisation(userId, investigation, AccessType.READ, manager);
                allowed.add(investigation);
            } catch (InsufficientPrivilegesException ex) {
                //ignore
            }
        }
        
        //add all the investigation information to the list of investigations
        getInvestigationInformation(userId, allowed,advanDTO.getInvestigationInclude(), manager);
        
        return allowed;
    }
    
    /**
     * Searches investigations from the ones they can view by the advanced criteria
     *
     * @param userId federalId of the user.
     * @param advanDTO {@Link AdvancedSearchDetails}
     * @param startIndex start index of the results found
     * @param number_results number of results found from the start index
     * @param manager manager object that will facilitate interaction with underlying database
     * @return collection of {@link Investigation} investigation objects
     */
    public static Collection<Investigation> searchByAdvanced(String userId, AdvancedSearchDetails advanDTO, int startIndex, int number_results, EntityManager manager){
        return searchByAdvancedImpl(userId, advanDTO, startIndex, number_results, manager);
    }
    
    /**
     * Searches investigations from the ones they can view by the advanced criteria
     *
     * @param userId federalId of the user.
     * @param advanDTO {@Link AdvancedSearchDetails}
     * @param manager manager object that will facilitate interaction with underlying database
     * @return collection of {@link Investigation} investigation objects
     */
    public static Collection<Investigation> searchByAdvanced(String userId, AdvancedSearchDetails advanDTO, EntityManager manager){
        return searchByAdvancedImpl(userId, advanDTO, -1, -1, manager);
    }
    
    /**
     *  Gets all the investigations associated with that user, ie. that they are investigator of.
     *
     * @param userId federalId of the user.
     * @param include information that is needed to be returned with the investigation
     * @param startIndex start index of the results found
     * @param number_results number of results found from the start index
     * @param manager manager object that will facilitate interaction with underlying database
     * @return collection of {@link Investigation} investigation objects
     */
    public static Collection<Investigation> getUsersInvestigations(String userId, InvestigationInclude include, int startIndex, int number_results, EntityManager manager){
        log.trace("getUserInvestigations("+userId+", "+startIndex+", "+number_results+", EnitiyManager)");
        
        Collection<Investigation> investigations = null;
        if(number_results < 0){
            //get all, maybe should limit this to 500?
            investigations = manager.createNamedQuery(INVESTIGATIONS_FOR_USER).setParameter("userId",userId).setMaxResults(MAX_QUERY_RESULTSET).getResultList();
        } else {
            investigations = manager.createNamedQuery(INVESTIGATIONS_FOR_USER).setParameter("userId",userId).setMaxResults(number_results).setFirstResult(startIndex).getResultList();
        }
        
        //add include information
        getInvestigationInformation(userId, investigations, include, manager);
        
        return investigations;
    }
    
    /**
     *  Gets all the investigations associated with that user, ie. that they are investigator of.
     *
     * @param userId federalId of the user.
     * @param manager manager object that will facilitate interaction with underlying database
     * @return collection of {@link Investigation} investigation objects
     */
    public static Collection<Investigation> getUsersInvestigations(String userId, EntityManager manager){
        return getUsersInvestigations(userId, InvestigationInclude.NONE, -1, -1, manager);
    }
    
    /**
     *  Gets all the investigations associated with that user, ie. that they are investigator of.
     *
     * @param userId federalId of the user
     * @param startIndex start index of the results found
     * @param number_results number of results found from the start index
     * @param manager manager object that will facilitate interaction with underlying database
     * @return collection of {@link Investigation} investigation objects
     */
    public static Collection<Investigation> getUsersInvestigations(String userId, int startIndex, int number_results, EntityManager manager){
        return getUsersInvestigations(userId, InvestigationInclude.NONE, startIndex, number_results, manager);
    }
    
    /**
     *  Gets all the investigations associated with that user, ie. that they are investigator of.
     *
     * @param userId federalId of the user.
     * @param include information that is needed to be returned with the investigation
     * @param manager manager object that will facilitate interaction with underlying database
     * @return collection of {@link Investigation} investigation objects
     */
    public static Collection<Investigation> getUsersInvestigations(String userId, InvestigationInclude include, EntityManager manager){
        return getUsersInvestigations(userId, include, -1, -1, manager);
    }
    
    /**
     *  Gets all the investigation ids associated with that user, ie. thart they are investigator of.
     *
     * @param userId federalId of the user.
     * @param manager manager object that will facilitate interaction with underlying database
     * @return collection of {@link Investigation} investigation ids
     */
    public static Collection<Long> getUsersInvestigationsRtnId(String userId, EntityManager manager){
        log.trace("getUsersInvestigationsRtnId("+userId+", EnitiyManager)");
        
        return  manager.createNamedQuery(INVESTIGATIONS_FOR_USER_RTN_ID).setParameter("userId",userId).getResultList();
    }
    
    /**
     * Search by a collection of keywords for investigations that user has access to view
     *
     * @param userId federalId of the user.
     * @param keywords Collection of keywords to search on
     * @param operator {@link LogicalOperator}, either AND or OR, default AND
     * @param include {@link InvestigationInclude}
     * @param fuzzy search with wildcards, e.g like copper searches for %copper% i.e anything with copper in keyword, default false
     * @param use_security search all investigations regardless of who owns it, default true
     * @param startIndex start index of the results found, default 0
     * @param number_results number of results found from the start index, default {@link Queries}.MAX_QUERY_RESULTSET
     * @param manager manager object that will facilitate interaction with underlying database
     * @return collection of {@link Investigation} investigation objects
     */
    public static Collection<Investigation> searchByKeywords(String userId, Collection<String> keywords, LogicalOperator operator,  InvestigationInclude include, boolean fuzzy, boolean use_security, int startIndex, int number_results, EntityManager manager)  {
        log.trace("searchByKeywords("+userId+", "+keywords+", "+operator +", "+include+", "+fuzzy+", "+use_security+", "+startIndex+", "+number_results+", EntityManager)");
        
        Collection<Investigation> investigations = null;
        String SQL = null;
        
        //dynamically create the SQL
        if(use_security)  SQL = INVESTIGATION_NATIVE_LIST_BY_KEYWORDS_SQL;
        else  SQL = INVESTIGATION_NATIVE_LIST_BY_KEYWORDS_SQL_NOSECURITY;
        
        int i  = 2;
        //check if fuzzy
        if(fuzzy){
            //fuzzy so LIKE
            for(String keyword : keywords){
                if(i == 2) SQL = SQL + "NAME LIKE ?"+(i++);
                else  SQL = SQL +" OR NAME LIKE ?"+(i++);
                
            }
        } else {
            //none fuzzy, =
            for(String keyword : keywords){
                if(i == 2) SQL = SQL + "NAME = ?"+(i++);
                else  SQL = SQL +" OR NAME = ?"+(i++);
            }
        }
        
        //TODO: this section only works for one fuzzy keyword (AND)
        //need to do this if used a EJB cos of the hashcode difference if LogicalOperator is serialized
        //so == and equals do not work, only on string of object
        if(operator.toString().equals(LogicalOperator.AND.toString())) {
            SQL = SQL +" GROUP BY ID, PREV_INV_NUMBER, BCAT_INV_STR, VISIT_ID, GRANT_ID, INV_ABSTRACT," +
                    " RELEASE_DATE, TITLE, MOD_TIME, INV_NUMBER, MOD_ID, INV_TYPE, INSTRUMENT, ";
            
            //TODO put in check that works with one fuzy keyword
            if(fuzzy) SQL = SQL +"FACILITY_CYCLE HAVING Count(*) >= ?number_keywords";
            else SQL = SQL + "FACILITY_CYCLE HAVING Count(*) = ?number_keywords";
        }
        
        SQL = SQL +" ORDER BY TITLE ASC";
        
        log.info("DYNAMIC SQL GENERATED: "+SQL);
        
        //set query with investigation as entity object
        Query query = manager.createNativeQuery(SQL, Investigation.class);
        
        //use security??
        if(use_security) query = query.setParameter("userId",userId);
        else query = query.setParameter("userId","%");
        
        //set keywords
        int j = 2;
        for(String keyword : keywords){
            if(fuzzy) query = query.setParameter(j++,"%"+keyword+"%");
            else query.setParameter(j++,keyword);
        }
        
        //add in the number of keywords
        query.setParameter("number_keywords",keywords.size());
        
        //run query
        if(number_results < 0){
            //get all, maybe should limit this to 500?
            investigations = query.setMaxResults(MAX_QUERY_RESULTSET).getResultList();
        } else {
            investigations = query.setMaxResults(number_results).setFirstResult(startIndex).getResultList();
        }
        
        log.trace("number of investigations returned is: "+investigations.size());
        //add all the investigation information to the list of investigations
        getInvestigationInformation(userId, investigations,include, manager);
        
        return investigations;
    }
    
    /**
     * Search by a collection of keywords for investigations that user has access to view
     *
     * @param userId federalId of the user.
     * @param keywords Collection of keywords to search on
     * @param includes {@link InvestigationInclude}
     * @param fuzzy search with wildcards, e.g like copper searches for %copper% i.e anything with copper in keyword, default false
     * @param manager manager object that will facilitate interaction with underlying database
     * @return collection of {@link Investigation} investigation objects
     */
    public static Collection<Investigation> searchByKeywords(String userId, Collection<String> keywords, InvestigationInclude includes, boolean fuzzy, EntityManager manager)  {
        //secuirty on, AND
        return searchByKeywords(userId, keywords, LogicalOperator.AND, includes, fuzzy ,true , -1, -1, manager);
    }
    
    /**
     * Search by a collection of keywords for investigations that user has access to view
     *
     * @param userId federalId of the user.
     * @param keywords Collection of keywords to search on
     * @param fuzzy search with wildcards, e.g like copper searches for %copper% i.e anything with copper in keyword, default false
     * @param manager manager object that will facilitate interaction with underlying database
     * @return collection of {@link Investigation} investigation objects
     */
    public static Collection<Investigation> searchByKeywords(String userId, Collection<String> keywords, boolean fuzzy, EntityManager manager)  {
        //secuirty on, AND, no includes
        return searchByKeywords(userId, keywords, LogicalOperator.AND, InvestigationInclude.NONE, fuzzy ,true , -1, -1, manager);
    }
    
    /**
     * Search by a collection of keywords for investigations that user has access to view
     *
     * @param userId federalId of the user.
     * @param keywords Collection of keywords to search on
     * @param manager manager object that will facilitate interaction with underlying database
     * @return collection of {@link Investigation} investigation objects
     */
    public static Collection<Investigation> searchByKeywords(String userId, Collection<String> keywords, EntityManager manager)  {
        //exact match, secuirty true, AND
        return searchByKeywords(userId, keywords, LogicalOperator.AND, InvestigationInclude.NONE, false ,true ,-1 , -1,manager);
    }
    
    /**
     * Search by a collection of keywords for investigations that user has access to view
     *
     * @param userId federalId of the user.
     * @param keywords Collection of keywords to search on
     * @param includes {@link InvestigationInclude}
     * @param manager manager object that will facilitate interaction with underlying database
     * @return collection of {@link Investigation} investigation objects
     */
    public static Collection<Investigation> searchByKeywords(String userId, Collection<String> keywords, InvestigationInclude includes, EntityManager manager)  {
        //exact match, secuirty true, AND
        return searchByKeywords(userId, keywords, LogicalOperator.AND, includes, false ,true ,-1 , -1,manager);
    }
    
    /**
     * Search by a collection of keywords for investigations that user has access to view
     *
     * @param userId federalId of the user.
     * @param keywords Collection of keywords to search on
     * @param operator {@link LogicalOperator}, either AND or OR
     * @param manager manager object that will facilitate interaction with underlying database
     * @return collection of {@link Investigation} investigation objects
     */
    public static Collection<Investigation> searchByKeywords(String userId, Collection<String> keywords, LogicalOperator operator, EntityManager manager)  {
        //exact match, secuirty true, AND
        return searchByKeywords(userId, keywords, operator, InvestigationInclude.NONE, false ,true ,-1 , -1,manager);
    }
    
    /**
     * Search by a collection of keywords for investigations that user has access to view
     *
     * @param userId federalId of the user.
     * @param keywords Collection of keywords to search on
     * @param includes {@link InvestigationInclude}
     * @param operator {@link LogicalOperator}, either AND or OR
     * @param manager manager object that will facilitate interaction with underlying database
     * @return collection of {@link Investigation} investigation objects
     */
    public static Collection<Investigation> searchByKeywords(String userId, Collection<String> keywords, InvestigationInclude includes, LogicalOperator operator, EntityManager manager)  {
        //exact match, secuirty true, AND
        return searchByKeywords(userId, keywords, operator, includes,  false ,true ,-1 , -1,manager);
    }
    
    /**
     * Search by a collection of keywords for investigations that user has access to view
     *
     * @param userId federalId of the user.
     * @param keywords Collection of keywords to search on
     * @param operator {@link LogicalOperator}, either AND or OR
     * @param fuzzy search with wildcards, e.g like copper searches for %copper% i.e anything with copper in keyword, default false
     * @param manager manager object that will facilitate interaction with underlying database
     * @return collection of {@link Investigation} investigation objects
     */
    public static Collection<Investigation> searchByKeywords(String userId, Collection<String> keywords, LogicalOperator operator, boolean fuzzy, EntityManager manager)  {
        //exact match, secuirty true,
        return searchByKeywords(userId, keywords, operator, InvestigationInclude.NONE, fuzzy ,true ,-1 , -1,manager);
    }
    
    /**
     * Search by a collection of keywords for investigations that user has access to view
     *
     * @param userId federalId of the user.
     * @param keywords Collection of keywords to search on
     * @param operator {@link LogicalOperator}, either AND or OR
     * @param includes {@link InvestigationInclude}
     * @param fuzzy search with wildcards, e.g like copper searches for %copper% i.e anything with copper in keyword, default false
     * @param manager manager object that will facilitate interaction with underlying database
     * @return collection of {@link Investigation} investigation objects
     */
    public static Collection<Investigation> searchByKeywords(String userId, Collection<String> keywords, LogicalOperator operator, InvestigationInclude includes, boolean fuzzy, EntityManager manager)  {
        //exact match, secuirty true,
        return searchByKeywords(userId, keywords, operator, includes, fuzzy ,true ,-1 , -1,manager);
    }
    
    /**
     * Lists all the instruments in the database
     *
     * @param userId federalId of the user.
     * @param manager manager object that will facilitate interaction with underlying database
     * @return List of {@link Instrument}s
     */
    public static Collection<Instrument> listAllInstruments(EntityManager manager)  {
        log.trace("listAllInstruments(EntityManager)");
        return  manager.createNamedQuery(ALL_INSTRUMENTS).setMaxResults(MAX_QUERY_RESULTSET).getResultList();
    }
    
    /**
     * Lists all the user roles in the database
     *
     * @param userId federalId of the user.
     * @param manager manager object that will facilitate interaction with underlying database
     * @return List of {@link IcatRole}s
     */
    public static Collection<String> listAllRoles(EntityManager manager){
        log.trace("listAllRoles(EntityManager)");
        return  manager.createNamedQuery(ALL_ROLES).setMaxResults(MAX_QUERY_RESULTSET).getResultList();
    }
}
