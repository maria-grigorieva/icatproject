package org.icatproject.core.entity;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import javax.persistence.Column;
import javax.persistence.EntityManager;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;
import javax.persistence.PrePersist;
import javax.persistence.PreUpdate;
import javax.persistence.Query;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.xml.bind.annotation.XmlElement;

import org.apache.log4j.Logger;
import org.icatproject.core.IcatException;
import org.icatproject.core.IcatException.IcatExceptionType;
import org.icatproject.core.manager.AccessType;
import org.icatproject.core.manager.EntityInfoHandler;
import org.icatproject.core.manager.EntityInfoHandler.Relationship;
import org.icatproject.core.manager.GateKeeper;
import org.icatproject.core.manager.Lucene;
import org.icatproject.core.parser.IncludeClause.Step;

@SuppressWarnings("serial")
@MappedSuperclass
public abstract class EntityBaseBean implements Serializable {

	private static final EntityInfoHandler eiHandler = EntityInfoHandler.getInstance();

	private static final Logger logger = Logger.getLogger(EntityBaseBean.class);

	@Column(name = "CREATE_ID", nullable = false)
	@XmlElement
	protected String createId;

	@Column(name = "CREATE_TIME", nullable = false)
	@Temporal(value = TemporalType.TIMESTAMP)
	@XmlElement
	protected Date createTime;

	@Id
	@GeneratedValue
	protected Long id;

	@Column(name = "MOD_ID", nullable = false)
	protected String modId;

	@Column(name = "MOD_TIME", nullable = false)
	@Temporal(value = TemporalType.TIMESTAMP)
	@XmlElement
	protected Date modTime;

	/*
	 * If this method is overridden it should be called as well by super.addToClone()
	 */
	void addToClone(EntityBaseBean clone) {
		clone.createId = createId;
		clone.createTime = createTime;
		clone.id = id;
		clone.modId = modId;
		clone.modTime = modTime;
	}

	public void addToLucene(Lucene lucene) throws IcatException {
		lucene.addDocument(this);
		Class<? extends EntityBaseBean> klass = this.getClass();
		Set<Relationship> rs = eiHandler.getRelatedEntities(klass);
		Map<Field, Method> getters = eiHandler.getGetters(klass);
		for (Relationship r : rs) {
			if (r.isCollection()) {
				Method m = getters.get(r.getField());
				try {
					@SuppressWarnings("unchecked")
					List<EntityBaseBean> collection = (List<EntityBaseBean>) m.invoke(this);
					if (!collection.isEmpty()) {
						for (EntityBaseBean bean : collection) {
							bean.addToLucene(lucene);
						}
					}
				} catch (Exception e) {
					throw new IcatException(IcatExceptionType.INTERNAL, e.getMessage());
				}
			}

		}

	}

	@SuppressWarnings("unchecked")
	private List<EntityBaseBean> allowedMany(Step step, Map<Field, Method> getters,
			GateKeeper gateKeeper, String userId, EntityManager manager)
			throws IllegalAccessException, IllegalArgumentException, InvocationTargetException,
			IcatException {
		Field field = step.getRelationship().getField();
		List<EntityBaseBean> beans = (List<EntityBaseBean>) getters.get(field).invoke(this);
		if (step.isAllowed()) {
			return beans;
		} else {
			return gateKeeper.getReadable(userId, beans, manager);
		}
	}

	private EntityBaseBean allowedOne(Relationship r, Method method, GateKeeper gateKeeper,
			String userId, EntityManager manager) throws IllegalAccessException,
			IllegalArgumentException, InvocationTargetException, IcatException {
		EntityBaseBean bean = (EntityBaseBean) method.invoke(this);
		if (bean != null && !gateKeeper.allowed(r)) {
			try {
				gateKeeper.performAuthorisation(userId, bean, AccessType.READ, manager);
			} catch (IcatException e) {
				if (e.getType() == IcatExceptionType.INSUFFICIENT_PRIVILEGES) {
					return null;
				} else {
					throw e;
				}
			}
		}
		return bean;
	}

	/**
	 * Gets the createId of this entity.
	 * 
	 * @return the createId
	 */
	public String getCreateId() {
		return createId;
	}

	/**
	 * Gets the createTime of this entity.
	 * 
	 * @return the modTime
	 */
	public Date getCreateTime() {
		return this.createTime;
	}

	public Long getId() {
		return id;
	}

	/**
	 * Gets the modId of this entity.
	 * 
	 * @return the modId
	 */
	public String getModId() {
		return this.modId;
	}

	/**
	 * Gets the modTime of this entity.
	 * 
	 * @return the modTime
	 */
	public Date getModTime() {
		return this.modTime;
	}

	final public void isUnique(EntityManager manager) throws IcatException {

		Class<? extends EntityBaseBean> entityClass = this.getClass();

		Map<Field, Method> getters = eiHandler.getGetters(entityClass);
		for (List<Field> constraint : eiHandler.getConstraintFields(entityClass)) {
			StringBuilder queryString = new StringBuilder();
			for (Field f : constraint) {
				if (queryString.length() == 0) {
					queryString.append("SELECT COUNT(o) FROM " + entityClass.getSimpleName()
							+ " o WHERE (");
				} else {
					queryString.append(") AND (");
				}
				String name = f.getName();
				queryString.append("o." + name + " = :" + name + " OR o." + name + " IS NULL");
			}
			Query query = manager.createQuery(queryString.toString() + ")");
			for (Field f : constraint) {
				Object value;
				try {
					value = getters.get(f).invoke(this);
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
				query = query.setParameter(f.getName(), value);
			}
			logger.debug("Checking uniqueness with " + query);
			long count = (Long) query.getSingleResult();
			if (count != 0) {
				StringBuilder erm = new StringBuilder();
				for (Field f : constraint) {
					Object value;
					try {
						value = getters.get(f).invoke(this);
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
					if (erm.length() == 0) {
						erm.append(entityClass.getSimpleName() + " exists with ");
					} else {
						erm.append(", ");
					}
					erm.append(f.getName() + " = '" + value + "'");
				}
				throw new IcatException(IcatException.IcatExceptionType.OBJECT_ALREADY_EXISTS,
						erm.toString());
			}
		}
	}

	private void isValid() throws IcatException {

		Class<? extends EntityBaseBean> klass = this.getClass();
		logger.trace("Checking validity of " + klass.getSimpleName());
		List<Field> notNullFields = eiHandler.getNotNullableFields(klass);
		Map<Field, Method> getters = eiHandler.getGetters(klass);

		for (Field field : notNullFields) {

			Object value;
			try {
				Method method = getters.get(field);
				logger.trace("Getter: " + method);
				value = method.invoke(this, (Object[]) new Class[] {});
			} catch (Exception e) {
				throw new IcatException(IcatException.IcatExceptionType.INTERNAL, "" + e);
			}

			if (value == null) {
				throw new IcatException(IcatException.IcatExceptionType.VALIDATION, this.getClass()
						.getSimpleName() + ": " + field.getName() + " cannot be null.");
			}
		}

		Map<Field, Integer> stringFields = eiHandler.getStringFields(klass);
		for (Entry<Field, Integer> entry : stringFields.entrySet()) {
			Field field = entry.getKey();
			Integer length = entry.getValue();
			Method method = getters.get(field);
			Object value;
			try {
				value = method.invoke(this, (Object[]) new Class[] {});
			} catch (Exception e) {
				throw new IcatException(IcatException.IcatExceptionType.INTERNAL, "" + e);
			}
			if (value != null) {
				if (((String) value).length() > length) {
					throw new IcatException(IcatException.IcatExceptionType.VALIDATION, getClass()
							.getSimpleName()
							+ ": "
							+ field.getName()
							+ " cannot have length > "
							+ length);
				}
			}
		}

	}

	/*
	 * If this method is overridden it should normally be called as well by super.isValid()
	 */
	public void isValid(EntityManager manager) throws IcatException {
		isValid(manager, true);
	}

	public void isValid(EntityManager manager, boolean deepValidation) throws IcatException {
		isValid();
	}

	/*
	 * If this method is overridden it should normally be called as well by super.postMergeFixup()
	 */
	public void postMergeFixup(EntityManager manager, GateKeeper gateKeeper) throws IcatException {
		// Do nothing by default
	}

	/*
	 * If this method is overridden it should be called as well by super.preparePersist(). Note that
	 * it recurses down though all to-many relationships.
	 */
	public void preparePersist(String modId, EntityManager manager, GateKeeper gateKeeper)
			throws IcatException {
		this.id = null;
		this.modId = modId;
		Class<? extends EntityBaseBean> klass = this.getClass();
		Set<Relationship> rs = eiHandler.getRelatedEntities(klass);
		Map<Field, Method> getters = eiHandler.getGetters(klass);
		for (Relationship r : rs) {
			if (r.isCollection()) {
				Method m = getters.get(r.getField());
				try {
					@SuppressWarnings("unchecked")
					List<EntityBaseBean> collection = (List<EntityBaseBean>) m.invoke(this);
					if (!collection.isEmpty()) {
						Method rev = r.getInverseSetter();
						for (EntityBaseBean bean : collection) {
							bean.preparePersist(modId, manager, gateKeeper);
							rev.invoke(bean, this);
						}
					}
				} catch (Exception e) {
					throw new IcatException(IcatExceptionType.INTERNAL, e.getClass() + " "
							+ e.getMessage());
				}
			}
		}
	}

	/**
	 * Automatically updates deleted, modTime, createTime and modId when entity is created
	 */
	@PrePersist
	public void prePersist() {
		if (modId == null) {
			throw new RuntimeException("modId not set on " + this);
		}
		createId = modId;
		createTime = modTime = new Date();
	}

	/**
	 * Automatically updates modTime when entity is persisted or merged
	 */
	@PreUpdate
	public void preUpdate() {
		modTime = new Date();
	}

	/**
	 * Clone a pruned copy of tree
	 * 
	 * @param one
	 *            true if INCLUDE 1 specified
	 * @param hereVarNum
	 *            current idVarNum. If steps is null this value is irrelevant.
	 * @param steps
	 *            list of steps to consider. This may be null. Steps starting with hereVarNum will
	 *            be followed. Steps hold the starting idVarNum, the field to navigate and the
	 *            destination idVarNum.
	 * @param manager
	 * @param userId
	 * @return
	 * @throws IcatException
	 */
	public EntityBaseBean pruned(boolean one, int hereVarNum, List<Step> steps,
			GateKeeper gateKeeper, String userId, EntityManager manager) throws IcatException {
		Class<? extends EntityBaseBean> klass = this.getClass();
		if (logger.isDebugEnabled()) {
			if (one) {
				logger.debug("Pruning " + klass.getSimpleName() + " INCLUDE 1");
			} else if (steps != null) {
				logger.debug("Pruning " + klass.getSimpleName() + " INCLUDE from " + hereVarNum);
			} else {
				logger.debug("Pruning " + klass.getSimpleName());
			}
		}
		try {
			Constructor<? extends EntityBaseBean> con = eiHandler.getConstructor(klass);
			EntityBaseBean clone = con.newInstance();
			clone.id = this.id;
			clone.createTime = this.createTime;
			clone.createId = this.createId;
			clone.modTime = this.modTime;
			clone.modId = this.modId;
			Set<Field> atts = eiHandler.getAttributes(klass);
			Map<Field, Method> getters = eiHandler.getGetters(klass);
			Map<Field, Method> setters = eiHandler.getSettersForUpdate(klass);
			for (Field att : atts) {
				Object value = getters.get(att).invoke(this);
				if (value != null) {
					setters.get(att).invoke(clone, new Object[] { value });
				}
			}
			if (one) {
				for (Relationship r : eiHandler.getOnes(klass)) {
					Field att = r.getField();
					EntityBaseBean value = allowedOne(r, getters.get(att), gateKeeper, userId,
							manager);
					if (value != null) {
						value = value.pruned(false, 0, null, gateKeeper, userId, manager);
						setters.get(att).invoke(clone, new Object[] { value });
					}
				}
			} else if (steps != null) {
				for (Step step : steps) {
					if (step.getHereVarNum() == hereVarNum) {
						Relationship r = step.getRelationship();
						Field field = r.getField();
						if (r.isCollection()) {
							List<EntityBaseBean> values = allowedMany(step, getters, gateKeeper,
									userId, manager);
							@SuppressWarnings("unchecked")
							List<EntityBaseBean> cloneList = (List<EntityBaseBean>) getters.get(
									field).invoke(clone);
							for (EntityBaseBean value : values) {
								value = value.pruned(false, step.getThereVarNum(), steps,
										gateKeeper, userId, manager);
								cloneList.add(value);
							}
						} else {
							EntityBaseBean value = allowedOne(r, getters.get(field), gateKeeper,
									userId, manager);
							if (value != null) {
								value = value.pruned(false, step.getThereVarNum(), steps,
										gateKeeper, userId, manager);
								setters.get(field).invoke(clone, new Object[] { value });
							}
						}
					}
				}
			}
			return clone;
		} catch (Exception e) {
			reportUnexpected(e);
			throw new IcatException(IcatException.IcatExceptionType.INTERNAL, "" + e);
		}

	}

	public void removeFromLucene(Lucene lucene) throws IcatException {
		lucene.deleteDocument(this);
		Class<? extends EntityBaseBean> klass = this.getClass();
		Set<Relationship> rs = eiHandler.getRelatedEntities(klass);
		Map<Field, Method> getters = eiHandler.getGetters(klass);
		for (Relationship r : rs) {
			if (r.isCollection()) {
				Method m = getters.get(r.getField());
				try {
					@SuppressWarnings("unchecked")
					List<EntityBaseBean> collection = (List<EntityBaseBean>) m.invoke(this);
					if (!collection.isEmpty()) {
						for (EntityBaseBean bean : collection) {
							bean.removeFromLucene(lucene);
						}
					}
				} catch (Exception e) {
					throw new IcatException(IcatExceptionType.INTERNAL, e.getMessage());
				}
			}

		}

	}

	private void reportUnexpected(Throwable e) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		e.printStackTrace(new PrintStream(baos));
		logger.error("Internal exception: " + baos);
	}

	public void setId(Long id) {
		this.id = id;
	}

	/**
	 * Sets the modId of this entity to the specified value.
	 * 
	 * @param modId
	 *            the new modId
	 */
	public void setModId(String modId) {
		this.modId = modId;
	}

	public void updateInLucene(Lucene lucene) throws IcatException {
		lucene.updateDocument(this);
		Class<? extends EntityBaseBean> klass = this.getClass();
		Set<Relationship> rs = eiHandler.getRelatedEntities(klass);
		Map<Field, Method> getters = eiHandler.getGetters(klass);
		for (Relationship r : rs) {
			if (r.isCollection()) {
				Method m = getters.get(r.getField());
				try {
					@SuppressWarnings("unchecked")
					List<EntityBaseBean> collection = (List<EntityBaseBean>) m.invoke(this);
					if (!collection.isEmpty()) {
						for (EntityBaseBean bean : collection) {
							bean.updateInLucene(lucene);
						}
					}
				} catch (Exception e) {
					throw new IcatException(IcatExceptionType.INTERNAL, e.getMessage());
				}
			}

		}

	}

}
