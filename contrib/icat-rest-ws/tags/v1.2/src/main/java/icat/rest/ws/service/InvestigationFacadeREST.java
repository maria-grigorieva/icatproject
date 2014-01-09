/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package icat.rest.ws.service;

import icat.rest.ws.converter.*;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;

import org.icatproject.core.entity.Investigation;
import org.icatproject.core.manager.SearchResponse;
import org.icatproject.core.manager.BeanManager;
import org.icatproject.core.IcatException;

/**
 *
 * @author 3qr
 */
@Stateless
@Path("experiment/")
public class InvestigationFacadeREST extends AbstractFacade<Investigation> {

  @PersistenceContext(unitName = "icat4PU")
  private EntityManager em;
  private static final Logger log = Logger.getLogger(InvestigationFacadeREST.class);

  public InvestigationFacadeREST() {
    super(Investigation.class);
  }

  @GET
  @Path("{facil}")
  @Produces({"application/xml", "application/json"})
  public InvestigationInstrumentConverter getInstruments(@PathParam("facil") String facility, @Context HttpServletRequest requestContext) {
    ArrayList<String> list = new ArrayList<String>();
    try {
      String yourIP = requestContext.getRemoteAddr().toString();
      log.info("Beginning getInstruments(): " + yourIP);
      String query = "Instrument.name ORDER BY name <-> Facility [ name = ':facility']";
      query = query.replace(":facility", facility);
      SearchResponse results = BeanManager.search(RestfulConstant.RESTFUL_USER, query, em);
      Iterator iter = results.getList().iterator();
      while (iter.hasNext()) {
        list.add((String) iter.next());
      }
      log.info("Ending getInstruments(): " + list.size());
    } catch (IcatException ex) {
      log.error("In getInstruments: got IcatException " + ex.getMessage());
    } catch (Exception ex) {
      log.error("In getInstruments: got Exception " + ex.getMessage());
    } finally {
      return new InvestigationInstrumentConverter(list);
    }
  }

  @GET
  @Path("{facil}/{inst}")
  @Produces({"application/xml", "application/json"})
  public InvestigationConverter getInvestigations(@PathParam("facil") String facility, @PathParam("inst") String instrument, @Context HttpServletRequest requestContext) {
    try {
      String yourIP = requestContext.getRemoteAddr().toString();
      log.info("Beginning getProposals: " + yourIP);
      String query = "DISTINCT Investigation.name ORDER BY name <-> Instrument [ name = ':instrument']";
      query = query.replace(":instrument", instrument);
      SearchResponse results = BeanManager.search(RestfulConstant.RESTFUL_USER, query, em);
      ArrayList<String> list = new ArrayList<String>();
      Iterator iter = results.getList().iterator();
      while (iter.hasNext()) {
        list.add((String) iter.next());
      }
      log.info("Ending getProposals, found " + list.size() + " proposals for instrment " + instrument);
      return new InvestigationConverter(list);
    } catch (IcatException ex) {
      log.error("In getProposals: got IcatException " + ex.getMessage());
      return new InvestigationConverter();
    }
  }

  @GET
  @Path("{facil}/{inst}/all")
  @Produces({"application/xml", "application/json"})
  public InvestigationAllConverter getInvestigationsAll(@PathParam("facil") String facility, @PathParam("inst") String instrument, @Context HttpServletRequest requestContext) {
    try {
      String yourIP = requestContext.getRemoteAddr().toString();
      log.info("Beginning getProposalsAll: " + yourIP);
      String query = "Investigation INCLUDE Dataset, DatasetType <-> Instrument [name = ':instrument']";
      query = query.replace(":instrument", instrument);
      SearchResponse results = BeanManager.search(RestfulConstant.RESTFUL_USER, query, em);
      log.info("Ending getProposalsAll, found " + results.getList().size() + " proposals for instrument " + instrument);
      ArrayList<Investigation> invList = new ArrayList<Investigation>();
      if (results.getList().isEmpty()) {
        return new InvestigationAllConverter();
      } else {
        Iterator iter = results.getList().iterator();
        while (iter.hasNext()) {
          Investigation inv = (Investigation) iter.next();
          invList.add(inv);
        }
        return new InvestigationAllConverter(invList);
      }
    } catch (IcatException ex) {
      log.error("In getProposalsAll: got IcatException " + ex.getMessage());
      return new InvestigationAllConverter();
    }
  }

  @GET
  @Path("{facil}/{inst}/{prop}")
  @Produces({"application/xml", "application/json"})
  public InvestigationDatasetConverter getRuns(@PathParam("facil") String facility, @PathParam("inst") String instrument, @PathParam("prop") String proposal, @Context HttpServletRequest requestContext) {
    try {
      String yourIP = requestContext.getRemoteAddr().toString();
      log.info("Beginning getRuns: " + yourIP);
      String query = "DISTINCT Dataset.name ORDER BY name <-> Investigation [name = ':proposal'] <-> Instrument [ name = ':instrument']";
      query = query.replace(":instrument", instrument).replace(":proposal", proposal);
      SearchResponse results = BeanManager.search(RestfulConstant.RESTFUL_USER, query, em);
      Iterator iter = results.getList().iterator();
      log.info("results size: " + results.getList().size());
      TreeSet set = new TreeSet();
      while (iter.hasNext()) {
        String runNumber = (String) iter.next();
        set.add(Integer.parseInt(runNumber));
      }
      String runRange = getRunRange(set);
      return new InvestigationDatasetConverter(runRange);
    } catch (IcatException ex) {
      log.error("In getRuns: got IcatException " + ex.getMessage());
      return new InvestigationDatasetConverter();
    }
  }

  @GET
  @Path("{facil}/{inst}/{prop}/meta")
  @Produces({"application/xml", "application/json"})
  public InvestigationCollectionMetaConverter getRunsMeta(@PathParam("facil") String facility, @PathParam("inst") String instrument, @PathParam("prop") String proposal, @Context HttpServletRequest requestContext) {
    try {
      String yourIP = requestContext.getRemoteAddr().toString();
      log.info("Beginning getMeta: " + yourIP);
      String query = "Investigation INCLUDE Dataset [ name = ':proposal' ] <-> Instrument [ name = ':instrument']";
      query = query.replace(":instrument", instrument).replace(":proposal", proposal);
      SearchResponse results = BeanManager.search(RestfulConstant.RESTFUL_USER, query, em);
      log.info("Ending getMeta, found " + results.getList().size() + " proposals for instrument " + instrument);
      if (results.getList().isEmpty()) {
        return new InvestigationCollectionMetaConverter();
      } else {
        ArrayList<Investigation> list = new ArrayList<Investigation>();
        Iterator iter = results.getList().iterator();
        while (iter.hasNext()) {
          Investigation inv = (Investigation) iter.next();
          list.add(inv);
        }
        return new InvestigationCollectionMetaConverter(list);
      }
    } catch (IcatException ex) {
      log.error("In getMeta: got IcatException " + ex.getMessage());
      return new InvestigationCollectionMetaConverter();
    }
  }

  @GET
  @Path("{facil}/{inst}/{prop}/all")
  @Produces({"application/xml", "application/json"})
  public InvestigationCollectionAllConverter getRunsAll(@PathParam("facil") String facility, @PathParam("inst") String instrument, @PathParam("prop") String proposal, @Context HttpServletRequest requestContext) {
    try {
      String yourIP = requestContext.getRemoteAddr().toString();
      log.info("Beginning getProposalAll: " + yourIP);
      String query = "Investigation INCLUDE Dataset, DatasetParameter, DatasetType, ParameterType [name = ':proposal'] <-> Instrument [name = ':instrument']";
      query = query.replace(":instrument", instrument).replace(":proposal", proposal);
      SearchResponse results = BeanManager.search(RestfulConstant.RESTFUL_USER, query, em);
      log.info("Ending getAll, found " + results.getList().size() + " proposals for instrument " + instrument);
      if (results.getList().isEmpty()) {
        return new InvestigationCollectionAllConverter();
      } else {
        ArrayList<Investigation> list = new ArrayList<Investigation>();
        Iterator iter = results.getList().iterator();
        while (iter.hasNext()) {
          Investigation inv = (Investigation) iter.next();
          list.add(inv);
        }
        return new InvestigationCollectionAllConverter(list);
      }
    } catch (IcatException ex) {
      log.error("In getProposalAll: got IcatException " + ex.getMessage());
      return new InvestigationCollectionAllConverter();
    }
  }

  @Override
  protected EntityManager getEntityManager() {
    return em;
  }

  private String getRunRange(TreeSet set) {
    log.info("Beginning getRunRange()");
    String runRange = "";
    int oldRunNumber = 0;

    Iterator it = set.iterator();
    boolean firstRun = true;
    boolean inIncrement = false;
    while (it.hasNext()) {
      int runNumber = (Integer) it.next();
      if (firstRun) {
        runRange = Integer.toString(runNumber);
        firstRun = false;
      } else {
        if (runNumber == oldRunNumber + 1) {
          if (!inIncrement) {
            runRange = runRange.concat("-");
            inIncrement = true;
          }
        } else {
          if (inIncrement) {
            runRange = runRange.concat(Integer.toString(oldRunNumber));
            inIncrement = false;
          }
          runRange = runRange.concat(", ").concat(Integer.toString(runNumber));
        }
      }
      oldRunNumber = runNumber;
    }

    //Be careful here, we might have exited in the middle of an incremental range. If so, tack on the last number.
    if (inIncrement) {
      runRange = runRange.concat(Integer.toString(oldRunNumber));
    }
    log.info("Ending getRunRange() run range is: " + runRange);
    return runRange;
  }
}