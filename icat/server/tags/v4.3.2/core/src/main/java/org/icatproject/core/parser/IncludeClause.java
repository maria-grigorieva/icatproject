package org.icatproject.core.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.icatproject.core.IcatException;
import org.icatproject.core.entity.EntityBaseBean;
import org.icatproject.core.manager.EntityInfoHandler;
import org.icatproject.core.manager.EntityInfoHandler.Relationship;
import org.icatproject.core.manager.GateKeeper;

public class IncludeClause {

	public class Step {

		private Relationship relationship;
		private int hereVarNum;
		private int thereVarNum;
		private boolean allowed;

		public Step(int hereVarNum, String fieldName, int thereVarNum, GateKeeper gateKeeper)
				throws IcatException, ParserException {
			for (Relationship r : eiHandler.getRelatedEntities(types.get(hereVarNum))) {
				if (r.getField().getName().equals(fieldName)) {
					types.put(thereVarNum, r.getDestinationBean());
					relationship = r;
					break;
				}
			}
			if (relationship == null) {
				throw new ParserException("Problem with INCLUDE clause: " + fieldName
						+ " is not a field of " + types.get(hereVarNum).getSimpleName());
			}
			this.hereVarNum = hereVarNum;
			this.thereVarNum = thereVarNum;
			this.allowed = gateKeeper.allowed(relationship);
		}

		public Relationship getRelationship() {
			return relationship;
		}

		public int getHereVarNum() {
			return hereVarNum;
		}

		public int getThereVarNum() {
			return thereVarNum;
		}

		public boolean isAllowed() {
			return allowed;
		}

	}

	private static final EntityInfoHandler eiHandler = EntityInfoHandler.getInstance();

	static Logger logger = Logger.getLogger(IncludeClause.class);

	private boolean one;

	private List<Step> steps = new ArrayList<Step>();
	Map<Integer, Class<? extends EntityBaseBean>> types = new HashMap<>();

	public IncludeClause(Class<? extends EntityBaseBean> bean, Input input, String idVar,
			GateKeeper gateKeeper) throws ParserException, IcatException {
		Map<String, Integer> idVarMap = new HashMap<>();
		idVarMap.put(idVar, 0);
		types.put(0, bean);
		int fabricatedStepCount = 0;
		input.consume(Token.Type.INCLUDE);
		Token t = input.peek(0);
		if (t.getValue().equals("1")) {
			input.consume(Token.Type.INTEGER);
			one = true;
		} else {
			fabricatedStepCount = processStep(input, idVarMap, fabricatedStepCount, gateKeeper);
		}

		t = input.peek(0);
		while (t != null && t.getType() == Token.Type.COMMA) {
			t = input.consume(Token.Type.COMMA);
			fabricatedStepCount = processStep(input, idVarMap, fabricatedStepCount, gateKeeper);
			t = input.peek(0);
		}
		logger.debug(this);
	}

	public List<Step> getSteps() {
		return steps;
	}

	public boolean isOne() {
		return one;
	}

	private int processStep(Input input, Map<String, Integer> idVarMap, int fabricatedStepCount,
			GateKeeper gateKeeper) throws ParserException, IcatException {
		Token t = input.consume(Token.Type.NAME);
		String path = t.getValue();
		String var = null;
		t = input.peek(0);
		if (t != null && t.getType() == Token.Type.AS) {
			t = input.consume(Token.Type.AS);
			var = input.consume(Token.Type.NAME).getValue();
		} else {
			if (t != null && t.getType() == Token.Type.NAME) {
				var = input.consume(Token.Type.NAME).getValue();
			}
		}

		/* Check the path */
		String[] eles = path.split("\\.");
		if (eles.length < 2) {
			throw new ParserException("Path " + path + " must contain a '.'");
		}
		String idv = eles[0].toUpperCase();
		Integer hereVarNum = idVarMap.get(idv);
		Integer thereVarNum;
		if (hereVarNum == null) {
			throw new ParserException("variable " + idv
					+ " mentioned in INCLUDE clause is not defined");
		}
		for (int i = 1; i < eles.length - 1; i++) {
			thereVarNum = idVarMap.size() + fabricatedStepCount;
			steps.add(new Step(hereVarNum, eles[i], thereVarNum, gateKeeper));
			fabricatedStepCount++;
			hereVarNum = thereVarNum;
		}

		/* Check the var */
		if (var != null) {
			if (var.indexOf('.') >= 0) {
				throw new ParserException("Variable " + var + " must not contain a '.'");
			}
			idv = var.toUpperCase();
			thereVarNum = idVarMap.get(idv);
			if (thereVarNum != null) {
				throw new ParserException("variable " + idv
						+ " mentioned in INCLUDE clause is already defined");
			}
			thereVarNum = idVarMap.size() + fabricatedStepCount;
			idVarMap.put(idv, thereVarNum);
			steps.add(new Step(hereVarNum, eles[eles.length - 1], thereVarNum, gateKeeper));
		} else {
			thereVarNum = idVarMap.size() + fabricatedStepCount;
			steps.add(new Step(hereVarNum, eles[eles.length - 1], thereVarNum, gateKeeper));
			fabricatedStepCount++;
		}
		return fabricatedStepCount;
	}

	@Override
	public String toString() {
		if (one) {
			return ("INCLUDE 1");
		} else {
			StringBuilder sb = new StringBuilder();
			for (Step step : steps) {
				if (sb.length() == 0) {
					sb.append("INCLUDE ");
				} else {
					sb.append(", ");
				}
				sb.append(step.hereVarNum + "." + step.relationship.getField().getName() + " -> "
						+ step.thereVarNum);
			}
			return sb.toString();
		}

	}

}
