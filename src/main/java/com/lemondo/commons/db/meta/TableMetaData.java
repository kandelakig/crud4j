package com.lemondo.commons.db.meta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.lemondo.commons.db.PrimarykeyType;
import com.lemondo.commons.db.exception.InvalidFieldException;

public class TableMetaData {

	private String tableName;
	private Map<String, Integer> columnDef;
	private PrimarykeyType pkType;
	private boolean deactivatedFlag;

	public TableMetaData(String tableName, Map<String, Integer> columnDef, PrimarykeyType pkType, boolean deactivatedFlag) {
		super();
		this.tableName = tableName;
		this.columnDef = new HashMap<String, Integer>(columnDef);
		this.pkType = pkType;
		this.deactivatedFlag = deactivatedFlag;
	}

	public Map<String, Integer> getColumnDef() {
		return new HashMap<String, Integer>(this.columnDef);
	}

	public PrimarykeyType getPkType() {
		return this.pkType;
	}

	public String genInsertSql(Set<String> columns, boolean autoGeneratedKey) throws InvalidFieldException {
		StringBuilder insertClause = new StringBuilder("INSERT INTO ").append(tableName);
		StringBuilder valuesClause = new StringBuilder(" VALUES");

		String prefix;
		if (!autoGeneratedKey) {
			insertClause.append(" (`id`");
			valuesClause.append(" (?");
			prefix = ",";
		} else {
			prefix = " (";
		}

		for (String col : columns) {
			if (columnDef.containsKey(col)) {
				insertClause.append(prefix).append("`").append(col).append("`");
				valuesClause.append(prefix).append("?");
				prefix = ",";
			} else {
				throw new InvalidFieldException("Table `" + tableName + "` does not contain field `" + col + "`");
			}
		}

		return insertClause.append(")").append(valuesClause).append(")").toString();
	}

	public String genUpdateSql(Set<String> columns) throws InvalidFieldException {
		StringBuilder updateSql = new StringBuilder("UPDATE ").append(tableName).append(" SET");

		String prefix = " `";
		for (String col : columns) {
			if (columnDef.containsKey(col)) {
				updateSql.append(prefix).append(col).append("`=?");
				prefix = ",`";
			} else {
				throw new InvalidFieldException("Table `" + tableName + "` does not contain field `" + col + "`");
			}
		}

		return updateSql.append(" WHERE `id`=?").append(deactivatedFlag ? " AND `deactivated`=0" : "").toString();
	}

	public String genDeleteSql() {
		StringBuilder deleteSql = new StringBuilder();

		if (deactivatedFlag) {
			deleteSql.append("UPDATE ").append(tableName).append(" SET `deactivated`=1 WHERE `deactivated`=0 AND `id`=?");
		} else {
			deleteSql.append("DELETE FROM ").append(tableName).append(" WHERE `id`=?");
		}

		return deleteSql.toString();
	}

	private String genFilterString(Set<FilterCondition> filter) {
		StringBuilder result = new StringBuilder();

		String prefix = "";
		for (FilterCondition condition : filter) {
			result.append(prefix).append("`").append(condition.getColumnName()).append("`").append(condition.getOperator()).append("?");
			prefix = " AND ";
		}

		return result.toString();
	}

	private String genOrderByString(List<String> sortFields) {
		if (sortFields == null) {
			return null;
		} else {
			StringBuilder result = new StringBuilder();

			String prefix = " ORDER BY ";
			for (int i = 0; i < sortFields.size(); i++) {
				result.append(prefix).append(sortFields.get(i));
				prefix = ",";
			}

			return result.toString();
		}
	}

	public String genSelectSql(boolean allRows, Set<FilterCondition> filter, List<String> sortFields) {
		StringBuilder selectSql = new StringBuilder("SELECT `id`");

		Set<String> columns = columnDef.keySet();
		for (String column : columns) {
			selectSql.append(",`").append(column).append("`");
		}

		selectSql.append(" FROM ").append(tableName);

		String filterPrefix = " WHERE ";
		if (deactivatedFlag) {
			selectSql.append(filterPrefix).append("`deactivated`=0");
			filterPrefix = " AND ";
		}
		if (!allRows) {
			selectSql.append(filterPrefix).append("`id`=?");
			filterPrefix = " AND ";
		}
		if (filter != null) {
			selectSql.append(filterPrefix).append(genFilterString(filter));
		}
		if (allRows && sortFields != null) {
			selectSql.append(genOrderByString(sortFields));
		}
		// TODO Add paging here

		return selectSql.toString();
	}

}
