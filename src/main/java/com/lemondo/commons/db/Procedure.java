package com.lemondo.commons.db;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;
import java.util.Map;

import com.lemondo.commons.db.meta.ProcMetaData;
import com.lemondo.commons.db.meta.ProcParam;

public class Procedure {

	private final ProcMetaData meta;
	private final List<ProcParam> params;
	private final Integer returnType;

	private final Helper helper;

	public Procedure(ProcMetaData meta, Helper helper) {
		this.meta = meta;
		if (this.meta != null) {
			this.params = this.meta.getParamDef();
			this.returnType = this.meta.getReturnType();
		} else {
			this.params = null;
			this.returnType = null;
		}

		this.helper = helper;
	}

	private CallableStatement prepareCall(Map<String, Object> args) throws SQLException {
		CallableStatement stmnt = helper.prepareCall(meta.genProcedureCall());

		int startInd = 1;

		if (returnType != null) {
			stmnt.registerOutParameter(startInd++, returnType);
		}

		if (params != null) {
			if (args == null) {
				for (int i = 0; i < params.size(); i++) {
					stmnt.setNull(startInd + i, params.get(i).getType());
				}
			} else {
				for (int i = 0; i < params.size(); i++) {
					Object val = args.get(params.get(i).getName());
					if (val != null) {
						stmnt.setObject(startInd + i, val, params.get(i).getType());
					} else {
						stmnt.setNull(startInd + i, params.get(i).getType());
					}
				}
			}
		}

		return stmnt;
	}

	public Object executeFunction(Map<String, Object> args) throws SQLException {
		CallableStatement stmnt = prepareCall(args);
		stmnt.execute();

		if (returnType == null) {
			return null;
		} else if (returnType == Types.INTEGER) {
			return stmnt.getInt(1);
		} else if (returnType == Types.BIGINT) {
			return stmnt.getLong(1);
		} else if (returnType == Types.VARCHAR || returnType == Types.CHAR) {
			return stmnt.getString(1);
		} else {
			return stmnt.getObject(1);
		}
	}
	
	public void executeProcedure(Map<String, Object> args) throws SQLException {
		prepareCall(args).execute();
	}

	public ResultSet executeQuery(Map<String, Object> args) throws SQLException {
		return prepareCall(args).executeQuery();
	}

}
