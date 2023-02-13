/*
 * Copyright (c) 2021-2023 The University of Manchester
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.manchester.spinnaker.storage.threading;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.RowIdLifetime;
import java.sql.SQLException;

/**
 * A single-threaded database metadata wrapper.
 *
 * @author Donal Fellows
 */
final class OTMeta extends OTWrapper implements DatabaseMetaData {
	private final DatabaseMetaData m;

	OTMeta(OneThread ot, DatabaseMetaData m) {
		super(ot, m);
		this.m = m;
	}

	// Methods that are effectively static properties for the DB, and so can be
	// called from any thread.

	@Override
	public boolean allProceduresAreCallable() throws SQLException {
		return m.allProceduresAreCallable();
	}

	@Override
	public boolean allTablesAreSelectable() throws SQLException {
		return m.allTablesAreSelectable();
	}

	@Override
	public String getURL() throws SQLException {
		return m.getURL();
	}

	@Override
	public String getUserName() throws SQLException {
		return m.getUserName();
	}

	@Override
	public boolean isReadOnly() throws SQLException {
		return m.isReadOnly();
	}

	@Override
	public boolean nullsAreSortedHigh() throws SQLException {
		return m.nullsAreSortedHigh();
	}

	@Override
	public boolean nullsAreSortedLow() throws SQLException {
		return m.nullsAreSortedLow();
	}

	@Override
	public boolean nullsAreSortedAtStart() throws SQLException {
		return m.nullsAreSortedAtStart();
	}

	@Override
	public boolean nullsAreSortedAtEnd() throws SQLException {
		return m.nullsAreSortedAtEnd();
	}

	@Override
	public String getDatabaseProductName() throws SQLException {
		return m.getDatabaseProductName();
	}

	@Override
	public String getDatabaseProductVersion() throws SQLException {
		return m.getDatabaseProductVersion();
	}

	@Override
	public String getDriverName() throws SQLException {
		return m.getDriverName();
	}

	@Override
	public String getDriverVersion() throws SQLException {
		return m.getDriverVersion();
	}

	@Override
	public int getDriverMajorVersion() {
		return m.getDriverMajorVersion();
	}

	@Override
	public int getDriverMinorVersion() {
		return m.getDriverMinorVersion();
	}

	@Override
	public boolean usesLocalFiles() throws SQLException {
		return m.usesLocalFiles();
	}

	@Override
	public boolean usesLocalFilePerTable() throws SQLException {
		return m.usesLocalFilePerTable();
	}

	@Override
	public boolean supportsMixedCaseIdentifiers() throws SQLException {
		return m.supportsMixedCaseIdentifiers();
	}

	@Override
	public boolean storesUpperCaseIdentifiers() throws SQLException {
		return m.storesUpperCaseIdentifiers();
	}

	@Override
	public boolean storesLowerCaseIdentifiers() throws SQLException {
		return m.storesLowerCaseIdentifiers();
	}

	@Override
	public boolean storesMixedCaseIdentifiers() throws SQLException {
		return m.storesMixedCaseIdentifiers();
	}

	@Override
	public boolean supportsMixedCaseQuotedIdentifiers() throws SQLException {
		return m.supportsMixedCaseQuotedIdentifiers();
	}

	@Override
	public boolean storesUpperCaseQuotedIdentifiers() throws SQLException {
		return m.storesUpperCaseQuotedIdentifiers();
	}

	@Override
	public boolean storesLowerCaseQuotedIdentifiers() throws SQLException {
		return m.storesLowerCaseQuotedIdentifiers();
	}

	@Override
	public boolean storesMixedCaseQuotedIdentifiers() throws SQLException {
		return m.storesMixedCaseQuotedIdentifiers();
	}

	@Override
	public String getIdentifierQuoteString() throws SQLException {
		return m.getIdentifierQuoteString();
	}

	@Override
	public String getSQLKeywords() throws SQLException {
		return m.getSQLKeywords();
	}

	@Override
	public String getNumericFunctions() throws SQLException {
		return m.getNumericFunctions();
	}

	@Override
	public String getStringFunctions() throws SQLException {
		return m.getStringFunctions();
	}

	@Override
	public String getSystemFunctions() throws SQLException {
		return m.getSystemFunctions();
	}

	@Override
	public String getTimeDateFunctions() throws SQLException {
		return m.getTimeDateFunctions();
	}

	@Override
	public String getSearchStringEscape() throws SQLException {
		return m.getSearchStringEscape();
	}

	@Override
	public String getExtraNameCharacters() throws SQLException {
		return m.getExtraNameCharacters();
	}

	@Override
	public boolean supportsAlterTableWithAddColumn() throws SQLException {
		return m.supportsAlterTableWithAddColumn();
	}

	@Override
	public boolean supportsAlterTableWithDropColumn() throws SQLException {
		return m.supportsAlterTableWithDropColumn();
	}

	@Override
	public boolean supportsColumnAliasing() throws SQLException {
		return m.supportsColumnAliasing();
	}

	@Override
	public boolean nullPlusNonNullIsNull() throws SQLException {
		return m.nullPlusNonNullIsNull();
	}

	@Override
	public boolean supportsConvert() throws SQLException {
		return m.supportsConvert();
	}

	@Override
	public boolean supportsConvert(int fromType, int toType)
			throws SQLException {
		return m.supportsConvert(fromType, toType);
	}

	@Override
	public boolean supportsTableCorrelationNames() throws SQLException {
		return m.supportsTableCorrelationNames();
	}

	@Override
	public boolean supportsDifferentTableCorrelationNames()
			throws SQLException {
		return m.supportsDifferentTableCorrelationNames();
	}

	@Override
	public boolean supportsExpressionsInOrderBy() throws SQLException {
		return m.supportsExpressionsInOrderBy();
	}

	@Override
	public boolean supportsOrderByUnrelated() throws SQLException {
		return m.supportsOrderByUnrelated();
	}

	@Override
	public boolean supportsGroupBy() throws SQLException {
		return m.supportsGroupBy();
	}

	@Override
	public boolean supportsGroupByUnrelated() throws SQLException {
		return m.supportsGroupByUnrelated();
	}

	@Override
	public boolean supportsGroupByBeyondSelect() throws SQLException {
		return m.supportsGroupByBeyondSelect();
	}

	@Override
	public boolean supportsLikeEscapeClause() throws SQLException {
		return m.supportsLikeEscapeClause();
	}

	@Override
	public boolean supportsMultipleResultSets() throws SQLException {
		return m.supportsMultipleResultSets();
	}

	@Override
	public boolean supportsMultipleTransactions() throws SQLException {
		return m.supportsMultipleTransactions();
	}

	@Override
	public boolean supportsNonNullableColumns() throws SQLException {
		return m.supportsNonNullableColumns();
	}

	@Override
	public boolean supportsMinimumSQLGrammar() throws SQLException {
		return m.supportsMinimumSQLGrammar();
	}

	@Override
	public boolean supportsCoreSQLGrammar() throws SQLException {
		return m.supportsCoreSQLGrammar();
	}

	@Override
	public boolean supportsExtendedSQLGrammar() throws SQLException {
		return m.supportsExtendedSQLGrammar();
	}

	@Override
	public boolean supportsANSI92EntryLevelSQL() throws SQLException {
		return m.supportsANSI92EntryLevelSQL();
	}

	@Override
	public boolean supportsANSI92IntermediateSQL() throws SQLException {
		return m.supportsANSI92IntermediateSQL();
	}

	@Override
	public boolean supportsANSI92FullSQL() throws SQLException {
		return m.supportsANSI92FullSQL();
	}

	@Override
	public boolean supportsIntegrityEnhancementFacility() throws SQLException {
		return m.supportsIntegrityEnhancementFacility();
	}

	@Override
	public boolean supportsOuterJoins() throws SQLException {
		return m.supportsOuterJoins();
	}

	@Override
	public boolean supportsFullOuterJoins() throws SQLException {
		return m.supportsFullOuterJoins();
	}

	@Override
	public boolean supportsLimitedOuterJoins() throws SQLException {
		return m.supportsLimitedOuterJoins();
	}

	@Override
	public String getSchemaTerm() throws SQLException {
		return m.getSchemaTerm();
	}

	@Override
	public String getProcedureTerm() throws SQLException {
		return m.getProcedureTerm();
	}

	@Override
	public String getCatalogTerm() throws SQLException {
		return m.getCatalogTerm();
	}

	@Override
	public boolean isCatalogAtStart() throws SQLException {
		return m.isCatalogAtStart();
	}

	@Override
	public String getCatalogSeparator() throws SQLException {
		return m.getCatalogSeparator();
	}

	@Override
	public boolean supportsSchemasInDataManipulation() throws SQLException {
		return m.supportsSchemasInDataManipulation();
	}

	@Override
	public boolean supportsSchemasInProcedureCalls() throws SQLException {
		return m.supportsSchemasInProcedureCalls();
	}

	@Override
	public boolean supportsSchemasInTableDefinitions() throws SQLException {
		return m.supportsSchemasInTableDefinitions();
	}

	@Override
	public boolean supportsSchemasInIndexDefinitions() throws SQLException {
		return m.supportsSchemasInIndexDefinitions();
	}

	@Override
	public boolean supportsSchemasInPrivilegeDefinitions() throws SQLException {
		return m.supportsSchemasInPrivilegeDefinitions();
	}

	@Override
	public boolean supportsCatalogsInDataManipulation() throws SQLException {
		return m.supportsCatalogsInDataManipulation();
	}

	@Override
	public boolean supportsCatalogsInProcedureCalls() throws SQLException {
		return m.supportsCatalogsInProcedureCalls();
	}

	@Override
	public boolean supportsCatalogsInTableDefinitions() throws SQLException {
		return m.supportsCatalogsInTableDefinitions();
	}

	@Override
	public boolean supportsCatalogsInIndexDefinitions() throws SQLException {
		return m.supportsCatalogsInIndexDefinitions();
	}

	@Override
	public boolean supportsCatalogsInPrivilegeDefinitions()
			throws SQLException {
		return m.supportsCatalogsInPrivilegeDefinitions();
	}

	@Override
	public boolean supportsPositionedDelete() throws SQLException {
		return m.supportsPositionedDelete();
	}

	@Override
	public boolean supportsPositionedUpdate() throws SQLException {
		return m.supportsPositionedUpdate();
	}

	@Override
	public boolean supportsSelectForUpdate() throws SQLException {
		return m.supportsSelectForUpdate();
	}

	@Override
	public boolean supportsStoredProcedures() throws SQLException {
		return m.supportsStoredProcedures();
	}

	@Override
	public boolean supportsSubqueriesInComparisons() throws SQLException {
		return m.supportsSubqueriesInComparisons();
	}

	@Override
	public boolean supportsSubqueriesInExists() throws SQLException {
		return m.supportsSubqueriesInExists();
	}

	@Override
	public boolean supportsSubqueriesInIns() throws SQLException {
		return m.supportsSubqueriesInIns();
	}

	@Override
	public boolean supportsSubqueriesInQuantifieds() throws SQLException {
		return m.supportsSubqueriesInQuantifieds();
	}

	@Override
	public boolean supportsCorrelatedSubqueries() throws SQLException {
		return m.supportsCorrelatedSubqueries();
	}

	@Override
	public boolean supportsUnion() throws SQLException {
		return m.supportsUnion();
	}

	@Override
	public boolean supportsUnionAll() throws SQLException {
		return m.supportsUnionAll();
	}

	@Override
	public boolean supportsOpenCursorsAcrossCommit() throws SQLException {
		return m.supportsOpenCursorsAcrossCommit();
	}

	@Override
	public boolean supportsOpenCursorsAcrossRollback() throws SQLException {
		return m.supportsOpenCursorsAcrossRollback();
	}

	@Override
	public boolean supportsOpenStatementsAcrossCommit() throws SQLException {
		return m.supportsOpenStatementsAcrossCommit();
	}

	@Override
	public boolean supportsOpenStatementsAcrossRollback() throws SQLException {
		return m.supportsOpenStatementsAcrossRollback();
	}

	@Override
	public int getMaxBinaryLiteralLength() throws SQLException {
		return m.getMaxBinaryLiteralLength();
	}

	@Override
	public int getMaxCharLiteralLength() throws SQLException {
		return m.getMaxCharLiteralLength();
	}

	@Override
	public int getMaxColumnNameLength() throws SQLException {
		return m.getMaxColumnNameLength();
	}

	@Override
	public int getMaxColumnsInGroupBy() throws SQLException {
		return m.getMaxColumnsInGroupBy();
	}

	@Override
	public int getMaxColumnsInIndex() throws SQLException {
		return m.getMaxColumnsInIndex();
	}

	@Override
	public int getMaxColumnsInOrderBy() throws SQLException {
		return m.getMaxColumnsInOrderBy();
	}

	@Override
	public int getMaxColumnsInSelect() throws SQLException {
		return m.getMaxColumnsInSelect();
	}

	@Override
	public int getMaxColumnsInTable() throws SQLException {
		return m.getMaxColumnsInTable();
	}

	@Override
	public int getMaxConnections() throws SQLException {
		return m.getMaxConnections();
	}

	@Override
	public int getMaxCursorNameLength() throws SQLException {
		return m.getMaxCursorNameLength();
	}

	@Override
	public int getMaxIndexLength() throws SQLException {
		return m.getMaxIndexLength();
	}

	@Override
	public int getMaxSchemaNameLength() throws SQLException {
		return m.getMaxSchemaNameLength();
	}

	@Override
	public int getMaxProcedureNameLength() throws SQLException {
		return m.getMaxProcedureNameLength();
	}

	@Override
	public int getMaxCatalogNameLength() throws SQLException {
		return m.getMaxCatalogNameLength();
	}

	@Override
	public int getMaxRowSize() throws SQLException {
		return m.getMaxRowSize();
	}

	@Override
	public boolean doesMaxRowSizeIncludeBlobs() throws SQLException {
		return m.doesMaxRowSizeIncludeBlobs();
	}

	@Override
	public int getMaxStatementLength() throws SQLException {
		return m.getMaxStatementLength();
	}

	@Override
	public int getMaxStatements() throws SQLException {
		return m.getMaxStatements();
	}

	@Override
	public int getMaxTableNameLength() throws SQLException {
		return m.getMaxTableNameLength();
	}

	@Override
	public int getMaxTablesInSelect() throws SQLException {
		return m.getMaxTablesInSelect();
	}

	@Override
	public int getMaxUserNameLength() throws SQLException {
		return m.getMaxUserNameLength();
	}

	@Override
	public int getDefaultTransactionIsolation() throws SQLException {
		return m.getDefaultTransactionIsolation();
	}

	@Override
	public boolean supportsTransactions() throws SQLException {
		return m.supportsTransactions();
	}

	@Override
	public boolean supportsTransactionIsolationLevel(int level)
			throws SQLException {
		return m.supportsTransactionIsolationLevel(level);
	}

	@Override
	public boolean supportsDataDefinitionAndDataManipulationTransactions()
			throws SQLException {
		return m.supportsDataDefinitionAndDataManipulationTransactions();
	}

	@Override
	public boolean supportsDataManipulationTransactionsOnly()
			throws SQLException {
		return m.supportsDataManipulationTransactionsOnly();
	}

	@Override
	public boolean dataDefinitionCausesTransactionCommit() throws SQLException {
		return m.dataDefinitionCausesTransactionCommit();
	}

	@Override
	public boolean dataDefinitionIgnoredInTransactions() throws SQLException {
		return m.dataDefinitionIgnoredInTransactions();
	}

	@Override
	public boolean supportsResultSetType(int type) throws SQLException {
		return m.supportsResultSetType(type);
	}

	@Override
	public boolean supportsResultSetConcurrency(int type, int concurrency)
			throws SQLException {
		return m.supportsResultSetConcurrency(type, concurrency);
	}

	@Override
	public boolean ownUpdatesAreVisible(int type) throws SQLException {
		return m.ownUpdatesAreVisible(type);
	}

	@Override
	public boolean ownDeletesAreVisible(int type) throws SQLException {
		return m.ownDeletesAreVisible(type);
	}

	@Override
	public boolean ownInsertsAreVisible(int type) throws SQLException {
		return m.ownInsertsAreVisible(type);
	}

	@Override
	public boolean othersUpdatesAreVisible(int type) throws SQLException {
		return m.othersUpdatesAreVisible(type);
	}

	@Override
	public boolean othersDeletesAreVisible(int type) throws SQLException {
		return m.othersDeletesAreVisible(type);
	}

	@Override
	public boolean othersInsertsAreVisible(int type) throws SQLException {
		return m.othersInsertsAreVisible(type);
	}

	@Override
	public boolean updatesAreDetected(int type) throws SQLException {
		return m.updatesAreDetected(type);
	}

	@Override
	public boolean deletesAreDetected(int type) throws SQLException {
		return m.deletesAreDetected(type);
	}

	@Override
	public boolean insertsAreDetected(int type) throws SQLException {
		return m.insertsAreDetected(type);
	}

	@Override
	public boolean supportsBatchUpdates() throws SQLException {
		return m.supportsBatchUpdates();
	}

	@Override
	public boolean supportsSavepoints() throws SQLException {
		return m.supportsSavepoints();
	}

	@Override
	public boolean supportsNamedParameters() throws SQLException {
		return m.supportsNamedParameters();
	}

	@Override
	public boolean supportsMultipleOpenResults() throws SQLException {
		return m.supportsMultipleOpenResults();
	}

	@Override
	public boolean supportsGetGeneratedKeys() throws SQLException {
		return m.supportsGetGeneratedKeys();
	}

	@Override
	public boolean supportsResultSetHoldability(int holdability)
			throws SQLException {
		return m.supportsResultSetHoldability(holdability);
	}

	@Override
	public int getResultSetHoldability() throws SQLException {
		return m.getResultSetHoldability();
	}

	@Override
	public int getDatabaseMajorVersion() throws SQLException {
		return m.getDatabaseMajorVersion();
	}

	@Override
	public int getDatabaseMinorVersion() throws SQLException {
		return m.getDatabaseMinorVersion();
	}

	@Override
	public int getJDBCMajorVersion() throws SQLException {
		return m.getJDBCMajorVersion();
	}

	@Override
	public int getJDBCMinorVersion() throws SQLException {
		return m.getJDBCMinorVersion();
	}

	@Override
	public int getSQLStateType() throws SQLException {
		return m.getSQLStateType();
	}

	@Override
	public boolean locatorsUpdateCopy() throws SQLException {
		return m.locatorsUpdateCopy();
	}

	@Override
	public boolean supportsStatementPooling() throws SQLException {
		return m.supportsStatementPooling();
	}

	@Override
	public RowIdLifetime getRowIdLifetime() throws SQLException {
		return m.getRowIdLifetime();
	}

	@Override
	public boolean supportsStoredFunctionsUsingCallSyntax()
			throws SQLException {
		return m.supportsStoredFunctionsUsingCallSyntax();
	}

	@Override
	public boolean autoCommitFailureClosesAllResultSets() throws SQLException {
		return m.autoCommitFailureClosesAllResultSets();
	}

	@Override
	public boolean generatedKeyAlwaysReturned() throws SQLException {
		return m.generatedKeyAlwaysReturned();
	}

	// Methods that are working with result sets need to be thread-locked

	@Override
	public Connection getConnection() throws SQLException {
		return wrap(m.getConnection());
	}

	@Override
	public ResultSet getProcedures(String catalog, String schemaPattern,
			String procedureNamePattern) throws SQLException {
		validateThread();
		return wrap(
				m.getProcedures(catalog, schemaPattern, procedureNamePattern));
	}

	@Override
	public ResultSet getProcedureColumns(String catalog, String schemaPattern,
			String procedureNamePattern, String columnNamePattern)
			throws SQLException {
		validateThread();
		return wrap(m.getProcedureColumns(catalog, schemaPattern,
				procedureNamePattern, columnNamePattern));
	}

	@Override
	public ResultSet getTables(String catalog, String schemaPattern,
			String tableNamePattern, String[] types) throws SQLException {
		validateThread();
		return wrap(
				m.getTables(catalog, schemaPattern, tableNamePattern, types));
	}

	@Override
	public ResultSet getSchemas() throws SQLException {
		validateThread();
		return wrap(m.getSchemas());
	}

	@Override
	public ResultSet getCatalogs() throws SQLException {
		validateThread();
		return wrap(m.getCatalogs());
	}

	@Override
	public ResultSet getTableTypes() throws SQLException {
		validateThread();
		return wrap(m.getTableTypes());
	}

	@Override
	public ResultSet getColumns(String catalog, String schemaPattern,
			String tableNamePattern, String columnNamePattern)
			throws SQLException {
		validateThread();
		return wrap(m.getColumns(catalog, schemaPattern, tableNamePattern,
				columnNamePattern));
	}

	@Override
	public ResultSet getColumnPrivileges(String catalog, String schema,
			String table, String columnNamePattern) throws SQLException {
		validateThread();
		return wrap(m.getColumnPrivileges(catalog, schema, table,
				columnNamePattern));
	}

	@Override
	public ResultSet getTablePrivileges(String catalog, String schemaPattern,
			String tableNamePattern) throws SQLException {
		validateThread();
		return wrap(
				m.getTablePrivileges(catalog, schemaPattern, tableNamePattern));
	}

	@Override
	public ResultSet getBestRowIdentifier(String catalog, String schema,
			String table, int scope, boolean nullable) throws SQLException {
		validateThread();
		return wrap(m.getBestRowIdentifier(catalog, schema, table, scope,
				nullable));
	}

	@Override
	public ResultSet getVersionColumns(String catalog, String schema,
			String table) throws SQLException {
		validateThread();
		return wrap(m.getVersionColumns(catalog, schema, table));
	}

	@Override
	public ResultSet getPrimaryKeys(String catalog, String schema, String table)
			throws SQLException {
		validateThread();
		return wrap(m.getPrimaryKeys(catalog, schema, table));
	}

	@Override
	public ResultSet getImportedKeys(String catalog, String schema,
			String table) throws SQLException {
		validateThread();
		return wrap(m.getImportedKeys(catalog, schema, table));
	}

	@Override
	public ResultSet getExportedKeys(String catalog, String schema,
			String table) throws SQLException {
		validateThread();
		return wrap(m.getExportedKeys(catalog, schema, table));
	}

	@Override
	public ResultSet getCrossReference(String parentCatalog,
			String parentSchema, String parentTable, String foreignCatalog,
			String foreignSchema, String foreignTable) throws SQLException {
		validateThread();
		return wrap(m.getCrossReference(parentCatalog, parentSchema,
				parentTable, foreignCatalog, foreignSchema, foreignTable));
	}

	@Override
	public ResultSet getTypeInfo() throws SQLException {
		validateThread();
		return wrap(m.getTypeInfo());
	}

	@Override
	public ResultSet getIndexInfo(String catalog, String schema, String table,
			boolean unique, boolean approximate) throws SQLException {
		validateThread();
		return wrap(
				m.getIndexInfo(catalog, schema, table, unique, approximate));
	}

	@Override
	public ResultSet getUDTs(String catalog, String schemaPattern,
			String typeNamePattern, int[] types) throws SQLException {
		validateThread();
		return wrap(m.getUDTs(catalog, schemaPattern, typeNamePattern, types));
	}

	@Override
	public ResultSet getSuperTypes(String catalog, String schemaPattern,
			String typeNamePattern) throws SQLException {
		validateThread();
		return wrap(m.getSuperTypes(catalog, schemaPattern, typeNamePattern));
	}

	@Override
	public ResultSet getSuperTables(String catalog, String schemaPattern,
			String tableNamePattern) throws SQLException {
		validateThread();
		return wrap(m.getSuperTables(catalog, schemaPattern, tableNamePattern));
	}

	@Override
	public ResultSet getAttributes(String catalog, String schemaPattern,
			String typeNamePattern, String attributeNamePattern)
			throws SQLException {
		validateThread();
		return wrap(m.getAttributes(catalog, schemaPattern, typeNamePattern,
				attributeNamePattern));
	}

	@Override
	public ResultSet getSchemas(String catalog, String schemaPattern)
			throws SQLException {
		validateThread();
		return wrap(m.getSchemas(catalog, schemaPattern));
	}

	@Override
	public ResultSet getClientInfoProperties() throws SQLException {
		validateThread();
		return wrap(m.getClientInfoProperties());
	}

	@Override
	public ResultSet getFunctions(String catalog, String schemaPattern,
			String functionNamePattern) throws SQLException {
		validateThread();
		return wrap(
				m.getFunctions(catalog, schemaPattern, functionNamePattern));
	}

	@Override
	public ResultSet getFunctionColumns(String catalog, String schemaPattern,
			String functionNamePattern, String columnNamePattern)
			throws SQLException {
		validateThread();
		return wrap(m.getFunctionColumns(catalog, schemaPattern,
				functionNamePattern, columnNamePattern));
	}

	@Override
	public ResultSet getPseudoColumns(String catalog, String schemaPattern,
			String tableNamePattern, String columnNamePattern)
			throws SQLException {
		validateThread();
		return wrap(m.getPseudoColumns(catalog, schemaPattern, tableNamePattern,
				columnNamePattern));
	}
}
