package com.lemondo.commons.db;

import java.io.OutputStream;
import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public abstract class TableModel<T, L> implements Model<T, L> {

	protected final TableMetaData metaData;
	private final Map<String, Integer> columnDef;
	protected final Helper helper;

	public TableModel(TableMetaData meta, Helper helper) {
		this.metaData = meta;
		this.columnDef = (this.metaData == null) ? null : this.metaData.getColumnDef();
		this.helper = helper;
	}

	protected abstract Map<String, Object> bodyAsMap(T body);

	protected abstract T readRow(ResultSet rs, ResultSetMetaData rsmd, int numColumns) throws SQLException;

	protected abstract L readAll(ResultSet rs, ResultSetMetaData rsmd, int numColumns) throws SQLException;

	protected abstract void writeRows(OutputStream out, ResultSet rs, ResultSetMetaData rsmd, int numColumns) throws SQLException;

	protected CallableStatement prepareInsertStmnt(String key, Map<String, Object> body) {
		Set<String> columns = body.keySet();
		try {
			CallableStatement stmnt = helper.prepareCall(metaData.genInsertSql(columns));

			int i = 1;
			stmnt.setString(i++, key);
			for (String col : columns) {
				if (columnDef.containsKey(col)) {
					stmnt.setObject(i++, body.get(col), columnDef.get(col));
				} else {
					throw new RuntimeException("BOOM: invalid field!");
				}
			}

			return stmnt;
		} catch (SQLException e) {
			throw new RuntimeException("BOOM!", e);
		}
	}

	protected CallableStatement prepareUpdateStmnt(String key, Map<String, Object> body) {
		Set<String> columns = body.keySet();
		try {
			CallableStatement stmnt = helper.prepareCall(metaData.genUpdateSql(columns));

			int i = 1;
			for (String col : columns) {
				if (columnDef.containsKey(col)) {
					stmnt.setObject(i++, body.get(col), columnDef.get(col));
				} else {
					throw new RuntimeException("BOOM: invalid field!");
				}
			}
			stmnt.setString(i, key);

			return stmnt;
		} catch (SQLException e) {
			throw new RuntimeException("BOOM!", e);
		}
	}

	protected CallableStatement prepareDeleteStmnt(String key) {
		try {
			CallableStatement stmnt = helper.prepareCall(metaData.genDeleteSql());

			stmnt.setString(1, key);

			return stmnt;
		} catch (SQLException e) {
			throw new RuntimeException("BOOM!", e);
		}
	}

	private Set<FilterCondition> extractFilterFields(Map<String, Object> options) {
		Object filterRaw = options.get("filter");
		if (filterRaw == null) {
			return null;
		} else if (!(filterRaw instanceof Map)) {
			throw new RuntimeException("BOOM!");
		} else {
			Set<FilterCondition> filter = new HashSet<FilterCondition>();

			@SuppressWarnings("unchecked")
			Map<String, Object> filterMap = (Map<String, Object>) filterRaw;

			Set<String> filterFields = filterMap.keySet();
			for (String field : filterFields) {
				if (columnDef.containsKey(field)) {
					filter.add(new FilterCondition(field, "=", filterMap.get(field), columnDef.get(field)));
				}
			}

			return filter;
		}
	}

	private List<String> extractSortFields(Map<String, Object> options) {
		Object sortFieldsRaw = options.get("order");
		if (sortFieldsRaw == null) {
			return null;
		} else if (!(sortFieldsRaw instanceof List)) {
			throw new RuntimeException("BOOM!");
		} else {
			@SuppressWarnings("unchecked")
			List<String> sortFields = (List<String>) sortFieldsRaw;
			return sortFields;
		}
	}

	protected CallableStatement prepareSelectStmnt(String key, Map<String, Object> options) {
		Set<FilterCondition> filter = null;
		List<String> sortFields = null;
		if (options != null) {
			filter = extractFilterFields(options);
			sortFields = extractSortFields(options);
		}

		boolean allRows = key == null;

		try {
			CallableStatement stmnt = helper.prepareCall(metaData.genSelectSql(allRows, filter, sortFields));

			int i = 1;
			if (!allRows) {
				stmnt.setString(i++, key);
			}

			if (filter != null) {
				for (FilterCondition condition : filter) {
					stmnt.setObject(i++, condition.value, condition.type);
				}
			}
			// TODO Add paging here

			return stmnt;
		} catch (SQLException e) {
			throw new RuntimeException("BOOM!", e);
		}
	}

	@Override
	public int create(String key, T body) {
		try {
			return prepareInsertStmnt(key, bodyAsMap(body)).executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException("BOOM!", e);
		}
	}

	@Override
	public int update(String key, T body) {
		try {
			return prepareUpdateStmnt(key, bodyAsMap(body)).executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException("BOOM!", e);
		}
	}

	@Override
	public int delete(String key) {
		try {
			return prepareDeleteStmnt(key).executeUpdate();
		} catch (SQLException e) {
			throw new RuntimeException("BOOM!", e);
		}
	}

	@Override
	public T read(String key) {
		try {
			ResultSet rs = prepareSelectStmnt(key, null).executeQuery();

			if (rs.next()) {
				ResultSetMetaData rsmd = rs.getMetaData();
				return readRow(rs, rsmd, rsmd.getColumnCount());
			} else {
				throw new RuntimeException("BOOM: No data found");
			}
		} catch (SQLException e) {
			throw new RuntimeException("BOOM!", e);
		}
	}

	@Override
	public L list(Map<String, Object> options) {
		try {
			ResultSet rs = prepareSelectStmnt(null, options).executeQuery();

			ResultSetMetaData rsmd = rs.getMetaData();
			int numColumns = rsmd.getColumnCount();

			return readAll(rs, rsmd, numColumns);
		} catch (SQLException e) {
			throw new RuntimeException("BOOM!", e);
		}
	}

	@Override
	public void list(OutputStream out, Map<String, Object> options) {
		try {
			ResultSet rs = prepareSelectStmnt(null, options).executeQuery();

			ResultSetMetaData rsmd = rs.getMetaData();
			int numColumns = rsmd.getColumnCount();

			writeRows(out, rs, rsmd, numColumns);
		} catch (SQLException e) {
			throw new RuntimeException("BOOM!", e);
		}
	}

}