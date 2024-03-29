package org.icatproject.core.manager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.jms.JMSException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.persistence.EntityExistsException;
import javax.persistence.EntityManager;
import javax.transaction.NotSupportedException;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.transaction.UserTransaction;

import org.apache.log4j.Logger;
import org.icatproject.core.Constants;
import org.icatproject.core.IcatException;
import org.icatproject.core.IcatException.IcatExceptionType;
import org.icatproject.core.entity.EntityBaseBean;
import org.icatproject.core.entity.Log;
import org.icatproject.core.entity.Session;
import org.icatproject.core.manager.Lucene.LuceneSearchResult;
import org.icatproject.core.manager.PropertyHandler.Operation;
import org.icatproject.core.oldparser.OldGetQuery;
import org.icatproject.core.oldparser.OldInput;
import org.icatproject.core.oldparser.OldLexerException;
import org.icatproject.core.oldparser.OldParserException;
import org.icatproject.core.oldparser.OldSearchQuery;
import org.icatproject.core.oldparser.OldTokenizer;
import org.icatproject.core.parser.GetQuery;
import org.icatproject.core.parser.IncludeClause;
import org.icatproject.core.parser.IncludeClause.Step;
import org.icatproject.core.parser.Input;
import org.icatproject.core.parser.LexerException;
import org.icatproject.core.parser.ParserException;
import org.icatproject.core.parser.SearchQuery;
import org.icatproject.core.parser.Token;
import org.icatproject.core.parser.Tokenizer;

@Stateless
@TransactionManagement(TransactionManagementType.BEAN)
public class BeanManager {

	@EJB
	GateKeeper gateKeeper;

	@EJB
	PropertyHandler propertyHandler;

	@EJB
	LuceneSingleton luceneSingleton;

	Lucene lucene;

	private static DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
	private static EntityInfoHandler eiHandler = EntityInfoHandler.getInstance();
	private boolean log;
	private static BufferedWriter logFile;

	private static final Logger logger = Logger.getLogger(BeanManager.class);

	private Set<String> logRequests;

	private Map<String, NotificationRequest> notificationRequests;

	private boolean luceneActive;

	private static long next;
	private static final Pattern timestampPattern = Pattern.compile(":ts(\\d{14})");

	@PostConstruct
	void init() {
		String luceneHost = propertyHandler.getLuceneHost();
		if (luceneHost != null) {
			String jndi = "java:global/icat.ear-" + Constants.API_VERSION + "/icat.exposed-"
					+ Constants.API_VERSION
					+ "/LuceneSingleton!org.icatproject.core.manager.Lucene";

			Context ctx = null;
			try {
				ctx = new InitialContext();
				ctx.addToEnvironment("org.omg.CORBA.ORBInitialHost", luceneHost);
				ctx.addToEnvironment("org.omg.CORBA.ORBInitialPort",
						Integer.toString(propertyHandler.getLucenePort()));
				lucene = (Lucene) ctx.lookup(jndi);
				logger.info("Found Lucene: " + lucene + " with jndi " + jndi + " at " + luceneHost
						+ ":" + propertyHandler.getLucenePort());
			} catch (NamingException e) {
				String msg = e.getClass() + " reports " + e.getMessage() + " from " + jndi;
				logger.fatal(msg);
				throw new IllegalStateException(msg);
			}
		} else {
			lucene = luceneSingleton;
		}

		logRequests = propertyHandler.getLogRequests();
		log = !logRequests.isEmpty();
		notificationRequests = propertyHandler.getNotificationRequests();
		luceneActive = lucene.getActive();
		if (luceneActive) {
			logger.info("Lucene is active");
		}
	}

	public CreateResponse create(String userId, EntityBaseBean bean, EntityManager manager,
			UserTransaction userTransaction) throws IcatException {

		logger.info(userId + " creating " + bean.getClass().getSimpleName());
		try {
			userTransaction.begin();
			try {
				long time = log ? System.currentTimeMillis() : 0;
				bean.preparePersist(userId, manager, gateKeeper);
				logger.debug(bean + " prepared for persist.");
				manager.persist(bean);
				logger.debug(bean + " persisted.");
				manager.flush();
				logger.debug(bean + " flushed.");
				// Check authz now everything persisted
				gateKeeper.performAuthorisation(userId, bean, AccessType.CREATE, manager);
				NotificationMessage notification = new NotificationMessage(Operation.C, bean,
						manager, notificationRequests);

				userTransaction.commit();
				long beanId = bean.getId();

				if (lucene != null) {
					bean.addToLucene(lucene);
				}
				if (log) {
					logWrite(time, userId, "create", bean.getClass().getSimpleName(), beanId,
							manager, userTransaction);
				}
				return new CreateResponse(beanId, notification);
			} catch (EntityExistsException e) {
				userTransaction.rollback();
				throw new IcatException(IcatException.IcatExceptionType.OBJECT_ALREADY_EXISTS,
						e.getMessage());
			} catch (IcatException e) {
				userTransaction.rollback();
				throw e;
			} catch (Throwable e) {
				userTransaction.rollback();
				logger.trace("Transaction rolled back for creation of " + bean + " because of "
						+ e.getClass() + " " + e.getMessage());
				updateCache();
				bean.preparePersist(userId, manager, gateKeeper);
				bean.isUnique(manager);
				bean.isValid(manager, true);
				throw new IcatException(IcatException.IcatExceptionType.INTERNAL,
						"Unexpected DB response " + e.getClass() + " " + e.getMessage());
			}
		} catch (IllegalStateException e) {
			throw new IcatException(IcatException.IcatExceptionType.INTERNAL,
					"IllegalStateException" + e.getMessage());
		} catch (SecurityException e) {
			throw new IcatException(IcatException.IcatExceptionType.INTERNAL, "SecurityException"
					+ e.getMessage());
		} catch (SystemException e) {
			throw new IcatException(IcatException.IcatExceptionType.INTERNAL, "SystemException"
					+ e.getMessage());
		} catch (NotSupportedException e) {
			throw new IcatException(IcatException.IcatExceptionType.INTERNAL,
					"NotSupportedException" + e.getMessage());
		}

	}

	public List<CreateResponse> createMany(String userId, List<EntityBaseBean> beans,
			EntityManager manager, UserTransaction userTransaction) throws IcatException {
		try {
			userTransaction.begin();
			List<CreateResponse> crs = new ArrayList<CreateResponse>();
			try {
				long time = log ? System.currentTimeMillis() : 0;
				for (EntityBaseBean bean : beans) {
					bean.preparePersist(userId, manager, gateKeeper);
					logger.trace(bean + " prepared for persist.");
					manager.persist(bean);
					logger.trace(bean + " persisted.");
					manager.flush();
					logger.trace(bean + " flushed.");
					// Check authz now everything persisted
					gateKeeper.performAuthorisation(userId, bean, AccessType.CREATE, manager);
					NotificationMessage notification = new NotificationMessage(Operation.C, bean,
							manager, notificationRequests);
					CreateResponse cr = new CreateResponse(bean.getId(), notification);
					crs.add(cr);
				}
				userTransaction.commit();

				if (log && !crs.isEmpty()) {
					logWrite(time, userId, "createMany", beans.get(0).getClass().getSimpleName(),
							crs.get(0).getPk(), manager, userTransaction);
				}

				if (lucene != null) {
					for (EntityBaseBean bean : beans) {
						bean.addToLucene(lucene);
					}
				}

				return crs;
			} catch (EntityExistsException e) {
				userTransaction.rollback();
				throw new IcatException(IcatException.IcatExceptionType.OBJECT_ALREADY_EXISTS,
						e.getMessage(), crs.size());
			} catch (IcatException e) {
				userTransaction.rollback();
				e.setOffset(crs.size());
				throw e;
			} catch (Throwable e) {
				userTransaction.rollback();
				logger.trace("Transaction rolled back for creation because of " + e.getClass()
						+ " " + e.getMessage());
				updateCache();
				int pos = crs.size();
				EntityBaseBean bean = beans.get(pos);
				try {
					bean.preparePersist(userId, manager, gateKeeper);
					bean.isUnique(manager);
					bean.isValid(manager, true);
				} catch (IcatException e1) {
					e1.setOffset(pos);
					throw e1;
				}
				/* Now look for duplicates within the list of objects provided */
				Class<? extends EntityBaseBean> entityClass = bean.getClass();
				Map<Field, Method> getters = eiHandler.getGetters(entityClass);

				for (List<Field> constraint : eiHandler.getConstraintFields(entityClass)) {
					for (int i = 0; i < pos; i++) {
						boolean diff = false;
						for (Field f : constraint) {
							Object value = getValue(getters, f, bean);
							Object value2 = getValue(getters, f, beans.get(i));
							if (!value.equals(value2)) {
								logger.debug("No problem with object " + i + " as " + f.getName()
										+ " has value " + value2 + " and not " + value);
								diff = true;
								break;
							}
						}
						if (!diff) {
							StringBuilder erm = new StringBuilder();
							for (Field f : constraint) {
								if (erm.length() == 0) {
									erm.append(entityClass.getSimpleName() + " exists with ");
								} else {
									erm.append(", ");
								}
								erm.append(f.getName() + " = '" + getValue(getters, f, bean) + "'");
							}
							throw new IcatException(
									IcatException.IcatExceptionType.OBJECT_ALREADY_EXISTS,
									erm.toString(), pos);
						}
					}
				}
				throw new IcatException(IcatException.IcatExceptionType.INTERNAL,
						"Unexpected DB response " + e.getClass() + " " + e.getMessage(), pos);

			}
		} catch (IllegalStateException e) {
			throw new IcatException(IcatException.IcatExceptionType.INTERNAL,
					"IllegalStateException" + e.getMessage(), -1);
		} catch (SecurityException e) {
			throw new IcatException(IcatException.IcatExceptionType.INTERNAL, "SecurityException"
					+ e.getMessage(), -1);
		} catch (SystemException e) {
			throw new IcatException(IcatException.IcatExceptionType.INTERNAL, "SystemException"
					+ e.getMessage(), -1);
		} catch (NotSupportedException e) {
			throw new IcatException(IcatException.IcatExceptionType.INTERNAL,
					"NotSupportedException" + e.getMessage(), -1);
		}
	}

	public NotificationMessage delete(String userId, EntityBaseBean bean, EntityManager manager,
			UserTransaction userTransaction) throws IcatException {
		try {
			userTransaction.begin();
			try {
				long time = log ? System.currentTimeMillis() : 0;
				EntityBaseBean beanManaged = find(bean, manager);
				gateKeeper.performAuthorisation(userId, beanManaged, AccessType.DELETE, manager);
				NotificationMessage notification = new NotificationMessage(Operation.D, bean,
						manager, notificationRequests);
				manager.remove(beanManaged);
				manager.flush();
				logger.trace("Deleted bean " + bean + " flushed.");
				userTransaction.commit();
				if (lucene != null) {
					bean.removeFromLucene(lucene);
				}
				if (log) {
					logWrite(time, userId, "delete", bean.getClass().getSimpleName(), bean.getId(),
							manager, userTransaction);
				}
				return notification;
			} catch (IcatException e) {
				userTransaction.rollback();
				throw e;
			} catch (Throwable e) {
				userTransaction.rollback();
				updateCache();
				throw new IcatException(IcatException.IcatExceptionType.INTERNAL,
						"Unexpected DB response " + e.getClass() + " " + e.getMessage());
			}
		} catch (IllegalStateException e) {
			throw new IcatException(IcatException.IcatExceptionType.INTERNAL,
					"IllegalStateException" + e.getMessage());
		} catch (SecurityException e) {
			throw new IcatException(IcatException.IcatExceptionType.INTERNAL, "SecurityException"
					+ e.getMessage());
		} catch (SystemException e) {
			throw new IcatException(IcatException.IcatExceptionType.INTERNAL, "SystemException"
					+ e.getMessage());
		} catch (NotSupportedException e) {
			throw new IcatException(IcatException.IcatExceptionType.INTERNAL,
					"NotSupportedException" + e.getMessage());
		}
	}

	public List<NotificationMessage> deleteMany(String userId, List<EntityBaseBean> beans,
			EntityManager manager, UserTransaction userTransaction) throws IcatException {
		try {
			userTransaction.begin();
			List<NotificationMessage> nms = new ArrayList<NotificationMessage>();
			Long beanId = null;
			try {
				long time = log ? System.currentTimeMillis() : 0;
				for (EntityBaseBean bean : beans) {

					EntityBaseBean beanManaged = find(bean, manager);
					if (beanId == null) {
						beanId = bean.getId();
					}
					gateKeeper
							.performAuthorisation(userId, beanManaged, AccessType.DELETE, manager);
					NotificationMessage notification = new NotificationMessage(Operation.D, bean,
							manager, notificationRequests);
					manager.remove(beanManaged);
					manager.flush();
					logger.trace("Deleted bean " + bean + " flushed.");
					nms.add(notification);
				}
				userTransaction.commit();

				if (lucene != null) {
					for (EntityBaseBean bean : beans) {
						bean.removeFromLucene(lucene);
					}
				}

				if (log && beanId != null) {
					logWrite(time, userId, "deleteMany", beans.get(0).getClass().getSimpleName(),
							beanId, manager, userTransaction);
				}

				return nms;
			} catch (IcatException e) {
				userTransaction.rollback();
				e.setOffset(nms.size());
				throw e;
			} catch (Throwable e) {
				userTransaction.rollback();
				logger.trace("Transaction rolled back for deletion because of " + e.getClass()
						+ " " + e.getMessage());
				updateCache();
				throw new IcatException(IcatException.IcatExceptionType.INTERNAL,
						"Unexpected DB response " + e.getClass() + " " + e.getMessage(), nms.size());
			}
		} catch (IllegalStateException e) {
			throw new IcatException(IcatException.IcatExceptionType.INTERNAL,
					"IllegalStateException " + e.getMessage(), -1);
		} catch (SecurityException e) {
			throw new IcatException(IcatException.IcatExceptionType.INTERNAL, "SecurityException "
					+ e.getMessage(), -1);
		} catch (SystemException e) {
			throw new IcatException(IcatException.IcatExceptionType.INTERNAL, "SystemException "
					+ e.getMessage(), -1);
		} catch (NotSupportedException e) {
			throw new IcatException(IcatException.IcatExceptionType.INTERNAL,
					"NotSupportedException " + e.getMessage(), -1);
		}
	}

	private EntityBaseBean find(EntityBaseBean bean, EntityManager manager) throws IcatException {
		Object primaryKey = bean.getId();
		Class<? extends EntityBaseBean> entityClass = bean.getClass();
		if (primaryKey == null) {
			throw new IcatException(IcatException.IcatExceptionType.NO_SUCH_OBJECT_FOUND,
					entityClass.getSimpleName() + " has null primary key.");
		}
		EntityBaseBean object = null;
		try {
			object = manager.find(entityClass, primaryKey);
		} catch (Throwable e) {
			throw new IcatException(IcatException.IcatExceptionType.INTERNAL,
					"Unexpected DB response " + e);
		}

		if (object == null) {
			throw new IcatException(IcatException.IcatExceptionType.NO_SUCH_OBJECT_FOUND,
					entityClass.getSimpleName() + "[id:" + primaryKey + "] not found.");
		}
		return object;
	}

	public EntityBaseBean get(String userId, String query, long primaryKey, EntityManager manager,
			UserTransaction userTransaction) throws IcatException {
		// Note that this uses no transactions (except for logging) as it is read only.

		long time = log ? System.currentTimeMillis() : 0;
		logger.debug(userId + " issues get for " + query);
		String[] words = query.trim().split("\\s+");
		if (words.length > 1 && words[1].toUpperCase().equals("INCLUDE")) {
			try {
				query = new OldGetQuery(new OldInput(OldTokenizer.getTokens(query))).getNewQuery();
				logger.debug("new style query: " + query);
			} catch (OldLexerException e) {
				throw new IcatException(IcatException.IcatExceptionType.BAD_PARAMETER,
						e.getMessage());
			} catch (OldParserException e) {
				throw new IcatException(IcatException.IcatExceptionType.BAD_PARAMETER,
						e.getMessage());
			}
		}
		GetQuery getQuery;
		try {
			getQuery = new GetQuery(new Input(Tokenizer.getTokens(query)), gateKeeper);
		} catch (LexerException e) {
			throw new IcatException(IcatException.IcatExceptionType.BAD_PARAMETER, e.getMessage());
		} catch (ParserException e) {
			throw new IcatException(IcatException.IcatExceptionType.BAD_PARAMETER, e.getMessage());
		}

		Class<? extends EntityBaseBean> entityClass = getQuery.getBean();
		EntityBaseBean beanManaged = manager.find(entityClass, primaryKey);
		if (beanManaged == null) {
			throw new IcatException(IcatException.IcatExceptionType.NO_SUCH_OBJECT_FOUND,
					entityClass.getSimpleName() + "[id:" + primaryKey + "] not found.");
		}

		gateKeeper.performAuthorisation(userId, beanManaged, AccessType.READ, manager);
		logger.debug("got " + entityClass.getSimpleName() + "[id:" + primaryKey + "]");

		IncludeClause include = getQuery.getInclude();
		boolean one = false;
		List<Step> steps = null;
		if (include != null) {
			one = include.isOne();
			steps = include.getSteps();
		}

		EntityBaseBean result = beanManaged.pruned(one, 0, steps, gateKeeper, userId, manager);
		if (log) {
			logRead(time, userId, "get", result.getClass().getSimpleName(), result.getId(), query,
					manager, userTransaction);
		}
		return result;
	}

	public EntityInfo getEntityInfo(String beanName) throws IcatException {
		return eiHandler.getEntityInfo(beanName);
	}

	public double getRemainingMinutes(String sessionId, EntityManager manager) throws IcatException {
		logger.debug("getRemainingMinutes for sessionId " + sessionId);
		Session session = getSession(sessionId, manager);
		return session.getRemainingMinutes();
	}

	private Session getSession(String sessionId, EntityManager manager) throws IcatException {
		Session session = null;
		if (sessionId == null || sessionId.equals("")) {
			throw new IcatException(IcatException.IcatExceptionType.SESSION,
					"Session Id cannot be null or empty.");
		}
		session = (Session) manager.find(Session.class, sessionId);
		if (session == null) {
			throw new IcatException(IcatException.IcatExceptionType.SESSION,
					"Unable to find user by sessionid: " + sessionId);
		}
		return session;
	}

	public String getUserName(String sessionId, EntityManager manager) throws IcatException {
		try {
			Session session = getSession(sessionId, manager);
			String userName = session.getUserName();
			logger.debug("user: " + userName + " is associated with: " + sessionId);
			return userName;
		} catch (IcatException e) {
			logger.debug("sessionId " + sessionId + " is not associated with valid session "
					+ e.getMessage());
			throw e;
		}
	}

	private Object getValue(Map<Field, Method> getters, Field f, EntityBaseBean bean)
			throws IcatException {
		Object value;
		try {
			value = getters.get(f).invoke(bean);
		} catch (IllegalArgumentException e) {
			throw new IcatException(IcatException.IcatExceptionType.INTERNAL,
					"IllegalArgumentException " + e.getMessage());
		} catch (IllegalAccessException e) {
			throw new IcatException(IcatException.IcatExceptionType.INTERNAL,
					"IllegalAccessException " + e.getMessage());
		} catch (InvocationTargetException e) {
			throw new IcatException(IcatException.IcatExceptionType.INTERNAL,
					"InvocationTargetException " + e.getMessage());
		}
		if (value instanceof EntityBaseBean) {
			value = "id:" + ((EntityBaseBean) value).getId();
		}
		return value;
	}

	public String login(String userName, int lifetimeMinutes, EntityManager manager,
			UserTransaction userTransaction) throws IcatException {
		Session session = new Session(userName, lifetimeMinutes);
		try {
			userTransaction.begin();
			try {
				long time = log ? System.currentTimeMillis() : 0;
				manager.persist(session);
				manager.flush();
				userTransaction.commit();
				String result = session.getId();
				logger.debug("Session " + result + " persisted.");
				if (log) {
					logSession(time, userName, "login", manager, userTransaction);
				}
				return result;
			} catch (Throwable e) {
				userTransaction.rollback();
				logger.trace("Transaction rolled back for login because of " + e.getClass() + " "
						+ e.getMessage());
				throw new IcatException(IcatException.IcatExceptionType.INTERNAL,
						"Unexpected DB response " + e.getClass() + " " + e.getMessage());
			}
		} catch (IllegalStateException e) {
			throw new IcatException(IcatException.IcatExceptionType.INTERNAL,
					"IllegalStateException " + e.getMessage());
		} catch (SecurityException e) {
			throw new IcatException(IcatException.IcatExceptionType.INTERNAL, "SecurityException "
					+ e.getMessage());
		} catch (SystemException e) {
			throw new IcatException(IcatException.IcatExceptionType.INTERNAL, "SystemException "
					+ e.getMessage());
		} catch (NotSupportedException e) {
			throw new IcatException(IcatException.IcatExceptionType.INTERNAL,
					"NotSupportedException " + e.getMessage());
		}
	}

	public void logout(String sessionId, EntityManager manager, UserTransaction userTransaction)
			throws IcatException {
		logger.debug("logout for sessionId " + sessionId);
		try {
			userTransaction.begin();
			try {
				long time = log ? System.currentTimeMillis() : 0;
				Session session = getSession(sessionId, manager);
				manager.remove(session);
				manager.flush();
				userTransaction.commit();
				logger.debug("Session " + session.getId() + " removed.");
				if (log) {
					logSession(time, session.getUserName(), "logout", manager, userTransaction);
				}
			} catch (IcatException e) {
				userTransaction.rollback();
				logger.trace("Transaction rolled back for logout because of " + e.getClass() + " "
						+ e.getMessage());
				if (e.getType() == IcatExceptionType.SESSION) {
					throw e;
				} else {
					throw new IcatException(IcatException.IcatExceptionType.INTERNAL, e.getClass()
							+ " " + e.getMessage());
				}
			} catch (Exception e) {
				throw new IcatException(IcatException.IcatExceptionType.INTERNAL, e.getClass()
						+ " " + e.getMessage());
			}
		} catch (IllegalStateException e) {
			throw new IcatException(IcatException.IcatExceptionType.INTERNAL,
					"IllegalStateException" + e.getMessage());
		} catch (SecurityException e) {
			throw new IcatException(IcatException.IcatExceptionType.INTERNAL, "SecurityException"
					+ e.getMessage());
		} catch (SystemException e) {
			throw new IcatException(IcatException.IcatExceptionType.INTERNAL, "SystemException"
					+ e.getMessage());
		} catch (NotSupportedException e) {
			throw new IcatException(IcatException.IcatExceptionType.INTERNAL,
					"NotSupportedException" + e.getMessage());
		} catch (RuntimeException e) {
			throw new IcatException(IcatException.IcatExceptionType.INTERNAL, e.getClass() + " "
					+ e.getMessage());
		}
	}

	private void logRead(long time, String userName, String operation, String entityName,
			Long entityId, String query, EntityManager manager, UserTransaction userTransaction)
			throws IcatException {
		long now = System.currentTimeMillis();
		if (logRequests.contains("file:R")) {
			writeLogFile(now, userName + "\t" + operation + "\t" + time + "\t" + (now - time)
					+ "\t" + entityName + "\t" + entityId + "\t" + query);
		}
		if (logRequests.contains("table:R")) {
			writeTable(now, userName, operation, now - time, entityName, entityId, query, manager,
					userTransaction);
		}
	}

	private void logSession(long time, String userName, String operation, EntityManager manager,
			UserTransaction userTransaction) throws IcatException {
		long now = System.currentTimeMillis();
		if (logRequests.contains("file:S")) {
			writeLogFile(now, userName + "\t" + operation + "\t" + time + "\t" + (now - time));
		}
		if (logRequests.contains("table:S")) {
			writeTable(now, userName, operation, now - time, null, null, null, manager,
					userTransaction);
		}
	}

	private void logWrite(long time, String userName, String operation, String entityName,
			long entityId, EntityManager manager, UserTransaction userTransaction)
			throws IcatException {
		long now = System.currentTimeMillis();
		if (logRequests.contains("file:W")) {
			writeLogFile(now, userName + "\t" + operation + "\t" + time + "\t" + (now - time)
					+ "\t" + entityName + "\t" + entityId);
		}
		if (logRequests.contains("table:W")) {
			writeTable(now, userName, operation, now - time, entityName, entityId, null, manager,
					userTransaction);
		}
	}

	// This code might be in EntityBaseBean however this would mean that it
	// would be processed by JPA which gets confused by it.
	public void merge(EntityBaseBean thisBean, Object fromBean, EntityManager manager)
			throws IcatException {
		Class<? extends EntityBaseBean> klass = thisBean.getClass();
		Map<Field, Method> setters = eiHandler.getSettersForUpdate(klass);
		Map<Field, Method> getters = eiHandler.getGetters(klass);

		for (Entry<Field, Method> fieldAndMethod : setters.entrySet()) {
			Field field = fieldAndMethod.getKey();
			try {
				Method m = getters.get(field);
				Object value = m.invoke(fromBean, new Object[0]);
				if (EntityBaseBean.class.isAssignableFrom(field.getType())) {
					logger.debug("Needs special processing as " + value + " is a bean");
					if (value != null) {
						Object pk = ((EntityBaseBean) value).getId();
						value = (EntityBaseBean) manager.find(field.getType(), pk);
						fieldAndMethod.getValue().invoke(thisBean, new Object[] { value });
					} else {
						fieldAndMethod.getValue().invoke(thisBean, (EntityBaseBean) null);
					}
				} else {
					fieldAndMethod.getValue().invoke(thisBean, new Object[] { value });
				}
				logger.trace("Updated " + klass.getSimpleName() + "." + field.getName() + " to "
						+ value);
			} catch (Exception e) {
				e.printStackTrace();
				throw new IcatException(IcatException.IcatExceptionType.INTERNAL, "" + e);
			}
		}

	}

	public void refresh(String sessionId, int lifetimeMinutes, EntityManager manager,
			UserTransaction userTransaction) throws IcatException {
		logger.debug("logout for sessionId " + sessionId);
		try {
			userTransaction.begin();
			try {
				long time = log ? System.currentTimeMillis() : 0;
				Session session = getSession(sessionId, manager);
				session.refresh(lifetimeMinutes);
				manager.flush();
				userTransaction.commit();
				logger.debug("Session " + session.getId() + " refreshed.");
				if (log) {
					logSession(time, session.getUserName(), "refresh", manager, userTransaction);
				}
			} catch (IcatException e) {
				userTransaction.rollback();
				logger.trace("Transaction rolled back for logout because of " + e.getClass() + " "
						+ e.getMessage());
				if (e.getType() == IcatExceptionType.SESSION) {
					throw e;
				} else {
					throw new IcatException(IcatException.IcatExceptionType.INTERNAL, e.getClass()
							+ " " + e.getMessage());
				}
			} catch (Exception e) {
				throw new IcatException(IcatException.IcatExceptionType.INTERNAL, e.getClass()
						+ " " + e.getMessage());
			}
		} catch (IllegalStateException e) {
			throw new IcatException(IcatException.IcatExceptionType.INTERNAL,
					"IllegalStateException" + e.getMessage());
		} catch (SecurityException e) {
			throw new IcatException(IcatException.IcatExceptionType.INTERNAL, "SecurityException"
					+ e.getMessage());
		} catch (SystemException e) {
			throw new IcatException(IcatException.IcatExceptionType.INTERNAL, "SystemException"
					+ e.getMessage());
		} catch (NotSupportedException e) {
			throw new IcatException(IcatException.IcatExceptionType.INTERNAL,
					"NotSupportedException" + e.getMessage());
		}
	}

	public List<?> search(String userId, String query, EntityManager manager,
			UserTransaction userTransaction) throws IcatException {
		// Note that this currently uses no transactions. This is simpler and
		// should give better performance

		long time = log ? System.currentTimeMillis() : 0;
		logger.info(userId + " searching for " + query);

		if (!query.toUpperCase().trim().startsWith("SELECT")) {

			/* Parse the query */

			try {
				OldSearchQuery oldSearchQuery = new OldSearchQuery(new OldInput(
						OldTokenizer.getTokens(query)));
				query = oldSearchQuery.getNewQuery();
			} catch (OldLexerException e) {
				throw new IcatException(IcatException.IcatExceptionType.BAD_PARAMETER,
						e.getMessage());
			} catch (OldParserException e) {
				throw new IcatException(IcatException.IcatExceptionType.BAD_PARAMETER,
						e.getMessage());
			}
			logger.debug("new style query: " + query);

		}
		/* New style query - parse it */
		List<Token> tokens = null;
		try {
			tokens = Tokenizer.getTokens(query);
		} catch (LexerException e) {
			throw new IcatException(IcatException.IcatExceptionType.BAD_PARAMETER, e.getMessage());
		}
		Input input = new Input(tokens);
		SearchQuery q;
		try {
			q = new SearchQuery(input, gateKeeper);
		} catch (ParserException e) {
			throw new IcatException(IcatException.IcatExceptionType.BAD_PARAMETER, e.getMessage());
		}

		/* Get the JPQL which includes authz restrictions */
		String jpql = q.getJPQL(userId, manager);
		logger.info("JPQL: " + jpql);

		/* Null query indicates that nothing accepted by authz */
		if (jpql == null) {
			return q.noAuthzResult();
		}

		/* Create query - which may go wrong */
		javax.persistence.Query jpqlQuery;
		try {
			jpqlQuery = manager.createQuery(jpql);
		} catch (IllegalArgumentException e) {
			/* Parse the original query but without trailing LIMIT and INCLUDE clauses */
			try {
				input.reset();
				StringBuilder sb = new StringBuilder();
				Token token = null;
				token = input.consume();

				while (token != null && token.getType() != Token.Type.LIMIT
						&& token.getType() != Token.Type.INCLUDE) {
					if (sb.length() != 0) {
						sb.append(" ");
					}
					sb.append(token.getValue());
					token = input.consume();
				}
				gateKeeper.checkJPQL(sb.toString());

			} catch (ParserException e1) {
				throw new IcatException(IcatException.IcatExceptionType.INTERNAL,
						"Parsing error should not occur as already parsed " + e.getMessage());
			}
			throw new IcatException(IcatExceptionType.INTERNAL, "Derived JPQL reports "
					+ e.getMessage());
		}

		/* add parameter values for any timestamps */
		Matcher m = timestampPattern.matcher(jpql);
		while (m.find()) {
			Date d = null;
			try {
				synchronized (df) { // Access to data formats must be synchronized
					d = df.parse(m.group(1));
				}
			} catch (ParseException e) {
				// This cannot happen - honest
			}
			jpqlQuery.setParameter("ts" + m.group(1), d);
		}
		try {
			jpqlQuery.setParameter("user", userId);
		} catch (IllegalArgumentException e) {
			// Ignore
		}

		Integer offset = q.getOffset();
		if (offset != null) {
			jpqlQuery.setFirstResult(offset);
		}
		Integer number = q.getNumber();
		if (number != null) {
			jpqlQuery.setMaxResults(number);
		}

		List<?> result = jpqlQuery.getResultList();

		logger.debug("Obtained " + result.size() + " results.");

		if (result.size() > 0 && result.get(0) instanceof EntityBaseBean) {
			IncludeClause include = q.getIncludeClause();
			boolean one = false;
			List<Step> steps = null;
			if (include != null) {
				one = include.isOne();
				steps = include.getSteps();
			}
			List<Object> clones = new ArrayList<Object>();
			for (Object beanManaged : result) {
				clones.add(((EntityBaseBean) beanManaged).pruned(one, 0, steps, gateKeeper, userId,
						manager));
			}
			if (log) {
				EntityBaseBean bean = (EntityBaseBean) clones.get(0);
				logRead(time, userId, "search", bean.getClass().getSimpleName(), bean.getId(),
						query, manager, userTransaction);
			}
			return clones;
		} else {
			if (log) {
				logRead(time, userId, "search", null, null, query, manager, userTransaction);
			}
			return result;
		}

	}

	public List<?> searchText(String userId, String query, int maxCount, String entityName,
			EntityManager manager, UserTransaction userTransaction) throws IcatException {
		long time = log ? System.currentTimeMillis() : 0;
		List<EntityBaseBean> results = new ArrayList<EntityBaseBean>();
		if (lucene != null) {
			LuceneSearchResult last = null;
			List<String> allResults = Collections.emptyList();
			/*
			 * As results may be rejected and maxCount may be 1 ensure that we don't make a huge
			 * number of a calls to Lucene
			 */
			int blockSize = Math.max(1000, maxCount);
			do {
				if (last == null) {
					last = lucene.search(query, blockSize, entityName);
				} else {
					last = lucene.searchAfter(query, blockSize, entityName, last);
				}
				allResults = last.getResults();
				logger.debug("Got " + allResults.size() + " results from Lucene for '" + query
						+ "' blockSize = " + blockSize);
				for (String result : allResults) {
					int i = result.indexOf(':');
					String eName = result.substring(0, i);
					long entityId = Long.parseLong(result.substring(i + 1));
					try {
						@SuppressWarnings("unchecked")
						Class<EntityBaseBean> klass = (Class<EntityBaseBean>) Class
								.forName(Constants.ENTITY_PREFIX + eName);
						EntityBaseBean beanManaged = manager.find(klass, entityId);
						if (beanManaged != null) {
							try {
								gateKeeper.performAuthorisation(userId, beanManaged,
										AccessType.READ, manager);
								results.add(beanManaged.pruned(false, -1, null, gateKeeper, userId,
										manager));
								if (results.size() == maxCount) {
									break;
								}
							} catch (IcatException e) {
								// Nothing to do
							}
						}
					} catch (ClassNotFoundException e) {
						throw new IcatException(IcatExceptionType.INTERNAL, e.getMessage());
					}
				}
			} while (results.size() != maxCount && allResults.size() == blockSize);
		}
		if (log) {
			if (results.size() > 0) {
				EntityBaseBean result = results.get(0);
				logRead(time, userId, "searchText", result.getClass().getSimpleName(),
						result.getId(), query, manager, userTransaction);
			} else {
				logRead(time, userId, "searchText", null, null, query, manager, userTransaction);
			}
		}
		logger.debug("Returning " + results.size() + " results");
		return results;
	}

	public boolean isAccessAllowed(String userId, EntityBaseBean bean, EntityManager manager,
			UserTransaction userTransaction, AccessType accessType) throws IcatException {
		if (accessType == AccessType.CREATE) {
			return createAllowed(userId, bean, manager, userTransaction);
		} else {
			try {
				gateKeeper.performAuthorisation(userId, bean, accessType, manager);
				return true;
			} catch (IcatException e) {
				if (e.getType() != IcatExceptionType.INSUFFICIENT_PRIVILEGES) {
					throw e;
				}
				return false;
			}
		}
	}

	private boolean createAllowed(String userId, EntityBaseBean bean, EntityManager manager,
			UserTransaction userTransaction) throws IcatException {
		try {
			userTransaction.begin();
			try {
				try {
					bean.preparePersist(userId, manager, gateKeeper);
					logger.debug(bean + " prepared for persist (createAllowed).");
					manager.persist(bean);
					logger.debug(bean + " persisted (createAllowed).");
					manager.flush();
					logger.debug(bean + " flushed (createAllowed).");
				} catch (EntityExistsException e) {
					throw new IcatException(IcatException.IcatExceptionType.OBJECT_ALREADY_EXISTS,
							e.getMessage());
				} catch (Throwable e) {
					userTransaction.rollback();
					logger.debug("Transaction rolled back for creation of " + bean + " because of "
							+ e.getClass() + " " + e.getMessage());
					bean.preparePersist(userId, manager, gateKeeper);
					bean.isUnique(manager);
					bean.isValid(manager, true);
					throw new IcatException(IcatException.IcatExceptionType.INTERNAL,
							"Unexpected DB response " + e.getClass() + " " + e.getMessage());
				}
				try {
					gateKeeper.performAuthorisation(userId, bean, AccessType.CREATE, manager);
					return true;
				} catch (IcatException e) {
					if (e.getType() != IcatExceptionType.INSUFFICIENT_PRIVILEGES) {
						throw e;
					}
					return false;
				} catch (Throwable e) {
					throw new IcatException(IcatException.IcatExceptionType.INTERNAL, e.getClass()
							+ " " + e.getMessage());
				}
			} finally {
				if (userTransaction.getStatus() != Status.STATUS_NO_TRANSACTION) {
					userTransaction.rollback();
					logger.debug("Transaction rolled back (createAllowed)");
				}
			}
		} catch (IcatException e) {
			logger.debug(e.getClass() + " " + e.getMessage());
			throw e;
		} catch (Exception e) {
			logger.error("Transaction problem? " + e.getClass() + " " + e.getMessage());
			throw new IcatException(IcatException.IcatExceptionType.INTERNAL, e.getClass() + " "
					+ e.getMessage());
		}

	}

	public NotificationMessage update(String userId, EntityBaseBean bean, EntityManager manager,
			UserTransaction userTransaction) throws IcatException {
		try {
			userTransaction.begin();
			try {
				long time = log ? System.currentTimeMillis() : 0;
				EntityBaseBean beanManaged = find(bean, manager);
				gateKeeper.performAuthorisation(userId, beanManaged, AccessType.UPDATE, manager);
				beanManaged.setModId(userId);
				merge(beanManaged, bean, manager);
				beanManaged.postMergeFixup(manager, gateKeeper);
				manager.flush();
				logger.trace("Updated bean " + bean + " flushed.");
				NotificationMessage notification = new NotificationMessage(Operation.U, bean,
						manager, notificationRequests);
				userTransaction.commit();
				if (log) {
					logWrite(time, userId, "update", bean.getClass().getSimpleName(), bean.getId(),
							manager, userTransaction);
				}
				if (lucene != null) {
					bean.updateInLucene(lucene);
				}
				return notification;
			} catch (IcatException e) {
				userTransaction.rollback();
				throw e;
			} catch (Throwable e) {
				userTransaction.rollback();
				updateCache();
				EntityBaseBean beanManaged = find(bean, manager);
				beanManaged.setModId(userId);
				merge(beanManaged, bean, manager);
				beanManaged.postMergeFixup(manager, gateKeeper);
				beanManaged.isValid(manager, false);
				e.printStackTrace(System.err);
				throw new IcatException(IcatException.IcatExceptionType.INTERNAL,
						"Unexpected DB response " + e.getClass() + " " + e.getMessage());
			}
		} catch (IllegalStateException e) {
			throw new IcatException(IcatException.IcatExceptionType.INTERNAL,
					"IllegalStateException" + e.getMessage());
		} catch (SecurityException e) {
			throw new IcatException(IcatException.IcatExceptionType.INTERNAL, "SecurityException"
					+ e.getMessage());
		} catch (SystemException e) {
			throw new IcatException(IcatException.IcatExceptionType.INTERNAL, "SystemException"
					+ e.getMessage());
		} catch (NotSupportedException e) {
			throw new IcatException(IcatException.IcatExceptionType.INTERNAL,
					"NotSupportedException" + e.getMessage());
		}
	}

	private void updateCache() throws IcatException {
		try {
			gateKeeper.updateCache();
		} catch (JMSException e1) {
			String msg = e1.getClass() + " " + e1.getMessage();
			logger.error(msg);
			throw new IcatException(IcatException.IcatExceptionType.INTERNAL, msg);
		}
	}

	private synchronized static void writeLogFile(long now, String entry) {
		if (now > next) {
			if (next != 0) {
				try {
					logFile.close();
				} catch (IOException e1) {
					logger.warn("Unable to close file " + logFile);
				}
			}
			GregorianCalendar nextCal = new GregorianCalendar();
			String fileName = "icat.call.log." + nextCal.get(Calendar.YEAR) + "-"
					+ (nextCal.get(Calendar.MONTH) + 1) + "-" + nextCal.get(Calendar.DAY_OF_MONTH);

			nextCal.add(Calendar.DATE, 1);
			nextCal.set(Calendar.HOUR_OF_DAY, 0);
			nextCal.set(Calendar.MINUTE, 0);
			nextCal.set(Calendar.SECOND, 0);
			nextCal.set(Calendar.MILLISECOND, 0);
			next = nextCal.getTimeInMillis();

			try {
				logFile = new BufferedWriter(new FileWriter(new File(new File("..", "logs"),
						fileName), true));
			} catch (IOException e) {
				logger.warn("Unable to open file " + fileName + " for appending");
				next = 0;
			}
		}
		try {
			if (next != 0) {
				logFile.write(entry, 0, entry.length());
				logFile.newLine();
				logFile.flush();
			}
		} catch (IOException e) {
			logger.warn("Unable to write to " + logFile);
		}

	}

	private void writeTable(long timeStamp, String userId, String operation, long duration,
			String entityName, Long entityId, String query, EntityManager manager,
			UserTransaction userTransaction) throws IcatException {

		try {
			userTransaction.begin();
			Log logEntry = null;
			try {
				logEntry = new Log(operation, duration, entityName, entityId, query);
				logEntry.preparePersist(userId, manager, gateKeeper);
				manager.persist(logEntry);
				manager.flush();
				userTransaction.commit();
			} catch (Throwable e) {
				userTransaction.rollback();
				logger.error("Transaction rolled back for creation of " + logEntry + " because of "
						+ e.getClass() + " " + e.getMessage());

				throw new IcatException(IcatException.IcatExceptionType.INTERNAL,
						"Unexpected DB response " + e.getClass() + " " + e.getMessage());
			}
		} catch (IllegalStateException e) {
			throw new IcatException(IcatException.IcatExceptionType.INTERNAL,
					"IllegalStateException " + e.getMessage());
		} catch (SecurityException e) {
			throw new IcatException(IcatException.IcatExceptionType.INTERNAL, "SecurityException "
					+ e.getMessage());
		} catch (SystemException e) {
			throw new IcatException(IcatException.IcatExceptionType.INTERNAL, "SystemException "
					+ e.getMessage());
		} catch (NotSupportedException e) {
			throw new IcatException(IcatException.IcatExceptionType.INTERNAL,
					"NotSupportedException " + e.getMessage());
		}

	}

	public void lucenePopulate(String entityName, EntityManager manager) throws IcatException {
		if (luceneActive) {
			Class<?> klass = null;
			try {
				klass = Class.forName(Constants.ENTITY_PREFIX + entityName);
			} catch (ClassNotFoundException e) {
				throw new IcatException(IcatExceptionType.BAD_PARAMETER, e.getMessage());
			}
			lucene.populate(klass);
		}
	}

	public void luceneClear() throws IcatException {
		if (luceneActive) {
			lucene.clear();
		}
	}

	public void luceneCommit() throws IcatException {
		if (luceneActive) {
			lucene.commit();
		}
	}

	public List<String> luceneSearch(String query, int maxCount, String entityName,
			EntityManager manager) throws IcatException {
		if (luceneActive) {
			return lucene.search(query, maxCount, entityName).getResults();
		} else {
			return Collections.emptyList();
		}

	}

	public List<String> getProperties() {
		return propertyHandler.props();
	}

	public List<String> luceneGetPopulating() {
		if (luceneActive) {
			return lucene.getPopulating();
		} else {
			return Collections.emptyList();
		}
	}
}
