/*
 * Copyright (c) 2021 The University of Manchester
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
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
	public final boolean allProceduresAreCallable() throws SQLException {
		return m.allProceduresAreCallable();
	}

	@Override
	public final boolean allTablesAreSelectable() throws SQLException {
		return m.allTablesAreSelectable();
	}

	@Override
	public final String getURL() throws SQLException {
		return m.getURL();
	}

	@Override
	public final String getUserName() throws SQLException {
		return m.getUserName();
	}

	@Override
	public final boolean isReadOnly() throws SQLException {
		return m.isReadOnly();
	}

	@Override
	public final boolean nullsAreSortedHigh() throws SQLException {
		return m.nullsAreSortedHigh();
	}

	@Override
	public final boolean nullsAreSortedLow() throws SQLException {
		return m.nullsAreSortedLow();
	}

	@Override
	public final boolean nullsAreSortedAtStart() throws SQLException {
		return m.nullsAreSortedAtStart();
	}

	@Override
	public final boolean nullsAreSortedAtEnd() throws SQLException {
		return m.nullsAreSortedAtEnd();
	}

	@Override
	public final String getDatabaseProductName() throws SQLException {
		return m.getDatabaseProductName();
	}

	@Override
	public final String getDatabaseProductVersion() throws SQLException {
		return m.getDatabaseProductVersion();
	}

	@Override
	public final String getDriverName() throws SQLException {
		return m.getDriverName();
	}

	@Override
	public final String getDriverVersion() throws SQLException {
		return m.getDriverVersion();
	}

	@Override
	public final int getDriverMajorVersion() {
		return m.getDriverMajorVersion();
	}

	@Override
	public final int getDriverMinorVersion() {
		return m.getDriverMinorVersion();
	}

	@Override
	public final boolean usesLocalFiles() throws SQLException {
		return m.usesLocalFiles();
	}

	@Override
	public final boolean usesLocalFilePerTable() throws SQLException {
		return m.usesLocalFilePerTable();
	}

	@Override
	public final boolean supportsMixedCaseIdentifiers() throws SQLException {
		return m.supportsMixedCaseIdentifiers();
	}

	@Override
	public final boolean storesUpperCaseIdentifiers() throws SQLException {
		return m.storesUpperCaseIdentifiers();
	}

	@Override
	public final boolean storesLowerCaseIdentifiers() throws SQLException {
		return m.storesLowerCaseIdentifiers();
	}

	@Override
	public final boolean storesMixedCaseIdentifiers() throws SQLException {
		return m.storesMixedCaseIdentifiers();
	}

	@Override
	public final boolean supportsMixedCaseQuotedIdentifiers()
			throws SQLException {
		return m.supportsMixedCaseQuotedIdentifiers();
	}

	@Override
	public final boolean storesUpperCaseQuotedIdentifiers()
			throws SQLException {
		return m.storesUpperCaseQuotedIdentifiers();
	}

	@Override
	public final boolean storesLowerCaseQuotedIdentifiers()
			throws SQLException {
		return m.storesLowerCaseQuotedIdentifiers();
	}

	@Override
	public final boolean storesMixedCaseQuotedIdentifiers()
			throws SQLException {
		return m.storesMixedCaseQuotedIdentifiers();
	}

	@Override
	public final String getIdentifierQuoteString() throws SQLException {
		return m.getIdentifierQuoteString();
	}

	@Override
	public final String getSQLKeywords() throws SQLException {
		return m.getSQLKeywords();
	}

	@Override
	public final String getNumericFunctions() throws SQLException {
		return m.getNumericFunctions();
	}

	@Override
	public final String getStringFunctions() throws SQLException {
		return m.getStringFunctions();
	}

	@Override
	public final String getSystemFunctions() throws SQLException {
		return m.getSystemFunctions();
	}

	@Override
	public final String getTimeDateFunctions() throws SQLException {
		return m.getTimeDateFunctions();
	}

	@Override
	public final String getSearchStringEscape() throws SQLException {
		return m.getSearchStringEscape();
	}

	@Override
	public final String getExtraNameCharacters() throws SQLException {
		return m.getExtraNameCharacters();
	}

	@Override
	public final boolean supportsAlterTableWithAddColumn() throws SQLException {
		return m.supportsAlterTableWithAddColumn();
	}

	@Override
	public final boolean supportsAlterTableWithDropColumn()
			throws SQLException {
		return m.supportsAlterTableWithDropColumn();
	}

	@Override
	public final boolean supportsColumnAliasing() throws SQLException {
		return m.supportsColumnAliasing();
	}

	@Override
	public final boolean nullPlusNonNullIsNull() throws SQLException {
		return m.nullPlusNonNullIsNull();
	}

	@Override
	public final boolean supportsConvert() throws SQLException {
		return m.supportsConvert();
	}

	@Override
	public final boolean supportsConvert(int fromType, int toType)
			throws SQLException {
		return m.supportsConvert(fromType, toType);
	}

	@Override
	public final boolean supportsTableCorrelationNames() throws SQLException {
		return m.supportsTableCorrelationNames();
	}

	@Override
	public final boolean supportsDifferentTableCorrelationNames()
			throws SQLException {
		return m.supportsDifferentTableCorrelationNames();
	}

	@Override
	public final boolean supportsExpressionsInOrderBy() throws SQLException {
		return m.supportsExpressionsInOrderBy();
	}

	@Override
	public final boolean supportsOrderByUnrelated() throws SQLException {
		return m.supportsOrderByUnrelated();
	}

	@Override
	public final boolean supportsGroupBy() throws SQLException {
		return m.supportsGroupBy();
	}

	@Override
	public final boolean supportsGroupByUnrelated() throws SQLException {
		return m.supportsGroupByUnrelated();
	}

	@Override
	public final boolean supportsGroupByBeyondSelect() throws SQLException {
		return m.supportsGroupByBeyondSelect();
	}

	@Override
	public final boolean supportsLikeEscapeClause() throws SQLException {
		return m.supportsLikeEscapeClause();
	}

	@Override
	public final boolean supportsMultipleResultSets() throws SQLException {
		return m.supportsMultipleResultSets();
	}

	@Override
	public final boolean supportsMultipleTransactions() throws SQLException {
		return m.supportsMultipleTransactions();
	}

	@Override
	public final boolean supportsNonNullableColumns() throws SQLException {
		return m.supportsNonNullableColumns();
	}

	@Override
	public final boolean supportsMinimumSQLGrammar() throws SQLException {
		return m.supportsMinimumSQLGrammar();
	}

	@Override
	public final boolean supportsCoreSQLGrammar() throws SQLException {
		return m.supportsCoreSQLGrammar();
	}

	@Override
	public final boolean supportsExtendedSQLGrammar() throws SQLException {
		return m.supportsExtendedSQLGrammar();
	}

	@Override
	public final boolean supportsANSI92EntryLevelSQL() throws SQLException {
		return m.supportsANSI92EntryLevelSQL();
	}

	@Override
	public final boolean supportsANSI92IntermediateSQL() throws SQLException {
		return m.supportsANSI92IntermediateSQL();
	}

	@Override
	public final boolean supportsANSI92FullSQL() throws SQLException {
		return m.supportsANSI92FullSQL();
	}

	@Override
	public final boolean supportsIntegrityEnhancementFacility()
			throws SQLException {
		return m.supportsIntegrityEnhancementFacility();
	}

	@Override
	public final boolean supportsOuterJoins() throws SQLException {
		return m.supportsOuterJoins();
	}

	@Override
	public final boolean supportsFullOuterJoins() throws SQLException {
		return m.supportsFullOuterJoins();
	}

	@Override
	public final boolean supportsLimitedOuterJoins() throws SQLException {
		return m.supportsLimitedOuterJoins();
	}

	@Override
	public final String getSchemaTerm() throws SQLException {
		return m.getSchemaTerm();
	}

	@Override
	public final String getProcedureTerm() throws SQLException {
		return m.getProcedureTerm();
	}

	@Override
	public final String getCatalogTerm() throws SQLException {
		return m.getCatalogTerm();
	}

	@Override
	public final boolean isCatalogAtStart() throws SQLException {
		return m.isCatalogAtStart();
	}

	@Override
	public final String getCatalogSeparator() throws SQLException {
		return m.getCatalogSeparator();
	}

	@Override
	public final boolean supportsSchemasInDataManipulation()
			throws SQLException {
		return m.supportsSchemasInDataManipulation();
	}

	@Override
	public final boolean supportsSchemasInProcedureCalls() throws SQLException {
		return m.supportsSchemasInProcedureCalls();
	}

	@Override
	public final boolean supportsSchemasInTableDefinitions()
			throws SQLException {
		return m.supportsSchemasInTableDefinitions();
	}

	@Override
	public final boolean supportsSchemasInIndexDefinitions()
			throws SQLException {
		return m.supportsSchemasInIndexDefinitions();
	}

	@Override
	public final boolean supportsSchemasInPrivilegeDefinitions()
			throws SQLException {
		return m.supportsSchemasInPrivilegeDefinitions();
	}

	@Override
	public final boolean supportsCatalogsInDataManipulation()
			throws SQLException {
		return m.supportsCatalogsInDataManipulation();
	}

	@Override
	public final boolean supportsCatalogsInProcedureCalls()
			throws SQLException {
		return m.supportsCatalogsInProcedureCalls();
	}

	@Override
	public final boolean supportsCatalogsInTableDefinitions()
			throws SQLException {
		return m.supportsCatalogsInTableDefinitions();
	}

	@Override
	public final boolean supportsCatalogsInIndexDefinitions()
			throws SQLException {
		return m.supportsCatalogsInIndexDefinitions();
	}

	@Override
	public final boolean supportsCatalogsInPrivilegeDefinitions()
			throws SQLException {
		return m.supportsCatalogsInPrivilegeDefinitions();
	}

	@Override
	public final boolean supportsPositionedDelete() throws SQLException {
		return m.supportsPositionedDelete();
	}

	@Override
	public final boolean supportsPositionedUpdate() throws SQLException {
		return m.supportsPositionedUpdate();
	}

	@Override
	public final boolean supportsSelectForUpdate() throws SQLException {
		return m.supportsSelectForUpdate();
	}

	@Override
	public final boolean supportsStoredProcedures() throws SQLException {
		return m.supportsStoredProcedures();
	}

	@Override
	public final boolean supportsSubqueriesInComparisons() throws SQLException {
		return m.supportsSubqueriesInComparisons();
	}

	@Override
	public final boolean supportsSubqueriesInExists() throws SQLException {
		return m.supportsSubqueriesInExists();
	}

	@Override
	public final boolean supportsSubqueriesInIns() throws SQLException {
		return m.supportsSubqueriesInIns();
	}

	@Override
	public final boolean supportsSubqueriesInQuantifieds() throws SQLException {
		return m.supportsSubqueriesInQuantifieds();
	}

	@Override
	public final boolean supportsCorrelatedSubqueries() throws SQLException {
		return m.supportsCorrelatedSubqueries();
	}

	@Override
	public final boolean supportsUnion() throws SQLException {
		return m.supportsUnion();
	}

	@Override
	public final boolean supportsUnionAll() throws SQLException {
		return m.supportsUnionAll();
	}

	@Override
	public final boolean supportsOpenCursorsAcrossCommit() throws SQLException {
		return m.supportsOpenCursorsAcrossCommit();
	}

	@Override
	public final boolean supportsOpenCursorsAcrossRollback()
			throws SQLException {
		return m.supportsOpenCursorsAcrossRollback();
	}

	@Override
	public final boolean supportsOpenStatementsAcrossCommit()
			throws SQLException {
		return m.supportsOpenStatementsAcrossCommit();
	}

	@Override
	public final boolean supportsOpenStatementsAcrossRollback()
			throws SQLException {
		return m.supportsOpenStatementsAcrossRollback();
	}

	@Override
	public final int getMaxBinaryLiteralLength() throws SQLException {
		return m.getMaxBinaryLiteralLength();
	}

	@Override
	public final int getMaxCharLiteralLength() throws SQLException {
		return m.getMaxCharLiteralLength();
	}

	@Override
	public final int getMaxColumnNameLength() throws SQLException {
		return m.getMaxColumnNameLength();
	}

	@Override
	public final int getMaxColumnsInGroupBy() throws SQLException {
		return m.getMaxColumnsInGroupBy();
	}

	@Override
	public final int getMaxColumnsInIndex() throws SQLException {
		return m.getMaxColumnsInIndex();
	}

	@Override
	public final int getMaxColumnsInOrderBy() throws SQLException {
		return m.getMaxColumnsInOrderBy();
	}

	@Override
	public final int getMaxColumnsInSelect() throws SQLException {
		return m.getMaxColumnsInSelect();
	}

	@Override
	public final int getMaxColumnsInTable() throws SQLException {
		return m.getMaxColumnsInTable();
	}

	@Override
	public final int getMaxConnections() throws SQLException {
		return m.getMaxConnections();
	}

	@Override
	public final int getMaxCursorNameLength() throws SQLException {
		return m.getMaxCursorNameLength();
	}

	@Override
	public final int getMaxIndexLength() throws SQLException {
		return m.getMaxIndexLength();
	}

	@Override
	public final int getMaxSchemaNameLength() throws SQLException {
		return m.getMaxSchemaNameLength();
	}

	@Override
	public final int getMaxProcedureNameLength() throws SQLException {
		return m.getMaxProcedureNameLength();
	}

	@Override
	public final int getMaxCatalogNameLength() throws SQLException {
		return m.getMaxCatalogNameLength();
	}

	@Override
	public final int getMaxRowSize() throws SQLException {
		return m.getMaxRowSize();
	}

	@Override
	public final boolean doesMaxRowSizeIncludeBlobs() throws SQLException {
		return m.doesMaxRowSizeIncludeBlobs();
	}

	@Override
	public final int getMaxStatementLength() throws SQLException {
		return m.getMaxStatementLength();
	}

	@Override
	public final int getMaxStatements() throws SQLException {
		return m.getMaxStatements();
	}

	@Override
	public final int getMaxTableNameLength() throws SQLException {
		return m.getMaxTableNameLength();
	}

	@Override
	public final int getMaxTablesInSelect() throws SQLException {
		return m.getMaxTablesInSelect();
	}

	@Override
	public final int getMaxUserNameLength() throws SQLException {
		return m.getMaxUserNameLength();
	}

	@Override
	public final int getDefaultTransactionIsolation() throws SQLException {
		return m.getDefaultTransactionIsolation();
	}

	@Override
	public final boolean supportsTransactions() throws SQLException {
		return m.supportsTransactions();
	}

	@Override
	public final boolean supportsTransactionIsolationLevel(int level)
			throws SQLException {
		return m.supportsTransactionIsolationLevel(level);
	}

	@Override
	public final boolean supportsDataDefinitionAndDataManipulationTransactions()
			throws SQLException {
		return m.supportsDataDefinitionAndDataManipulationTransactions();
	}

	@Override
	public final boolean supportsDataManipulationTransactionsOnly()
			throws SQLException {
		return m.supportsDataManipulationTransactionsOnly();
	}

	@Override
	public final boolean dataDefinitionCausesTransactionCommit()
			throws SQLException {
		return m.dataDefinitionCausesTransactionCommit();
	}

	@Override
	public final boolean dataDefinitionIgnoredInTransactions()
			throws SQLException {
		return m.dataDefinitionIgnoredInTransactions();
	}

	@Override
	public final boolean supportsResultSetType(int type) throws SQLException {
		return m.supportsResultSetType(type);
	}

	@Override
	public final boolean supportsResultSetConcurrency(int type, int concurrency)
			throws SQLException {
		return m.supportsResultSetConcurrency(type, concurrency);
	}

	@Override
	public final boolean ownUpdatesAreVisible(int type) throws SQLException {
		return m.ownUpdatesAreVisible(type);
	}

	@Override
	public final boolean ownDeletesAreVisible(int type) throws SQLException {
		return m.ownDeletesAreVisible(type);
	}

	@Override
	public final boolean ownInsertsAreVisible(int type) throws SQLException {
		return m.ownInsertsAreVisible(type);
	}

	@Override
	public final boolean othersUpdatesAreVisible(int type) throws SQLException {
		return m.othersUpdatesAreVisible(type);
	}

	@Override
	public final boolean othersDeletesAreVisible(int type) throws SQLException {
		return m.othersDeletesAreVisible(type);
	}

	@Override
	public final boolean othersInsertsAreVisible(int type) throws SQLException {
		return m.othersInsertsAreVisible(type);
	}

	@Override
	public final boolean updatesAreDetected(int type) throws SQLException {
		return m.updatesAreDetected(type);
	}

	@Override
	public final boolean deletesAreDetected(int type) throws SQLException {
		return m.deletesAreDetected(type);
	}

	@Override
	public final boolean insertsAreDetected(int type) throws SQLException {
		return m.insertsAreDetected(type);
	}

	@Override
	public final boolean supportsBatchUpdates() throws SQLException {
		return m.supportsBatchUpdates();
	}

	@Override
	public final boolean supportsSavepoints() throws SQLException {
		return m.supportsSavepoints();
	}

	@Override
	public final boolean supportsNamedParameters() throws SQLException {
		return m.supportsNamedParameters();
	}

	@Override
	public final boolean supportsMultipleOpenResults() throws SQLException {
		return m.supportsMultipleOpenResults();
	}

	@Override
	public final boolean supportsGetGeneratedKeys() throws SQLException {
		return m.supportsGetGeneratedKeys();
	}

	@Override
	public final boolean supportsResultSetHoldability(int holdability)
			throws SQLException {
		return m.supportsResultSetHoldability(holdability);
	}

	@Override
	public final int getResultSetHoldability() throws SQLException {
		return m.getResultSetHoldability();
	}

	@Override
	public final int getDatabaseMajorVersion() throws SQLException {
		return m.getDatabaseMajorVersion();
	}

	@Override
	public final int getDatabaseMinorVersion() throws SQLException {
		return m.getDatabaseMinorVersion();
	}

	@Override
	public final int getJDBCMajorVersion() throws SQLException {
		return m.getJDBCMajorVersion();
	}

	@Override
	public final int getJDBCMinorVersion() throws SQLException {
		return m.getJDBCMinorVersion();
	}

	@Override
	public final int getSQLStateType() throws SQLException {
		return m.getSQLStateType();
	}

	@Override
	public final boolean locatorsUpdateCopy() throws SQLException {
		return m.locatorsUpdateCopy();
	}

	@Override
	public final boolean supportsStatementPooling() throws SQLException {
		return m.supportsStatementPooling();
	}

	@Override
	public final RowIdLifetime getRowIdLifetime() throws SQLException {
		return m.getRowIdLifetime();
	}

	@Override
	public final boolean supportsStoredFunctionsUsingCallSyntax()
			throws SQLException {
		return m.supportsStoredFunctionsUsingCallSyntax();
	}

	@Override
	public final boolean autoCommitFailureClosesAllResultSets()
			throws SQLException {
		return m.autoCommitFailureClosesAllResultSets();
	}

	@Override
	public final boolean generatedKeyAlwaysReturned() throws SQLException {
		return m.generatedKeyAlwaysReturned();
	}

	// Methods that are working with result sets need to be thread-locked

	@Override
	public final Connection getConnection() throws SQLException {
		return wrap(m.getConnection());
	}

	@Override
	public final ResultSet getProcedures(String catalog, String schemaPattern,
			String procedureNamePattern) throws SQLException {
		validateThread();
		return wrap(
				m.getProcedures(catalog, schemaPattern, procedureNamePattern));
	}

	@Override
	public final ResultSet getProcedureColumns(String catalog,
			String schemaPattern, String procedureNamePattern,
			String columnNamePattern) throws SQLException {
		validateThread();
		return wrap(m.getProcedureColumns(catalog, schemaPattern,
				procedureNamePattern, columnNamePattern));
	}

	@Override
	public final ResultSet getTables(String catalog, String schemaPattern,
			String tableNamePattern, String[] types) throws SQLException {
		validateThread();
		return wrap(
				m.getTables(catalog, schemaPattern, tableNamePattern, types));
	}

	@Override
	public final ResultSet getSchemas() throws SQLException {
		validateThread();
		return wrap(m.getSchemas());
	}

	@Override
	public final ResultSet getCatalogs() throws SQLException {
		validateThread();
		return wrap(m.getCatalogs());
	}

	@Override
	public final ResultSet getTableTypes() throws SQLException {
		validateThread();
		return wrap(m.getTableTypes());
	}

	@Override
	public final ResultSet getColumns(String catalog, String schemaPattern,
			String tableNamePattern, String columnNamePattern)
			throws SQLException {
		validateThread();
		return wrap(m.getColumns(catalog, schemaPattern, tableNamePattern,
				columnNamePattern));
	}

	@Override
	public final ResultSet getColumnPrivileges(String catalog, String schema,
			String table, String columnNamePattern) throws SQLException {
		validateThread();
		return wrap(m.getColumnPrivileges(catalog, schema, table,
				columnNamePattern));
	}

	@Override
	public final ResultSet getTablePrivileges(String catalog,
			String schemaPattern, String tableNamePattern) throws SQLException {
		validateThread();
		return wrap(
				m.getTablePrivileges(catalog, schemaPattern, tableNamePattern));
	}

	@Override
	public final ResultSet getBestRowIdentifier(String catalog, String schema,
			String table, int scope, boolean nullable) throws SQLException {
		validateThread();
		return wrap(m.getBestRowIdentifier(catalog, schema, table, scope,
				nullable));
	}

	@Override
	public final ResultSet getVersionColumns(String catalog, String schema,
			String table) throws SQLException {
		validateThread();
		return wrap(m.getVersionColumns(catalog, schema, table));
	}

	@Override
	public final ResultSet getPrimaryKeys(String catalog, String schema,
			String table) throws SQLException {
		validateThread();
		return wrap(m.getPrimaryKeys(catalog, schema, table));
	}

	@Override
	public final ResultSet getImportedKeys(String catalog, String schema,
			String table) throws SQLException {
		validateThread();
		return wrap(m.getImportedKeys(catalog, schema, table));
	}

	@Override
	public final ResultSet getExportedKeys(String catalog, String schema,
			String table) throws SQLException {
		validateThread();
		return wrap(m.getExportedKeys(catalog, schema, table));
	}

	@Override
	public final ResultSet getCrossReference(String parentCatalog,
			String parentSchema, String parentTable, String foreignCatalog,
			String foreignSchema, String foreignTable) throws SQLException {
		validateThread();
		return wrap(m.getCrossReference(parentCatalog, parentSchema,
				parentTable, foreignCatalog, foreignSchema, foreignTable));
	}

	@Override
	public final ResultSet getTypeInfo() throws SQLException {
		validateThread();
		return wrap(m.getTypeInfo());
	}

	@Override
	public final ResultSet getIndexInfo(String catalog, String schema,
			String table, boolean unique, boolean approximate)
			throws SQLException {
		validateThread();
		return wrap(
				m.getIndexInfo(catalog, schema, table, unique, approximate));
	}

	@Override
	public final ResultSet getUDTs(String catalog, String schemaPattern,
			String typeNamePattern, int[] types) throws SQLException {
		validateThread();
		return wrap(m.getUDTs(catalog, schemaPattern, typeNamePattern, types));
	}

	@Override
	public final ResultSet getSuperTypes(String catalog, String schemaPattern,
			String typeNamePattern) throws SQLException {
		validateThread();
		return wrap(m.getSuperTypes(catalog, schemaPattern, typeNamePattern));
	}

	@Override
	public final ResultSet getSuperTables(String catalog, String schemaPattern,
			String tableNamePattern) throws SQLException {
		validateThread();
		return wrap(m.getSuperTables(catalog, schemaPattern, tableNamePattern));
	}

	@Override
	public final ResultSet getAttributes(String catalog, String schemaPattern,
			String typeNamePattern, String attributeNamePattern)
			throws SQLException {
		validateThread();
		return wrap(m.getAttributes(catalog, schemaPattern, typeNamePattern,
				attributeNamePattern));
	}

	@Override
	public final ResultSet getSchemas(String catalog, String schemaPattern)
			throws SQLException {
		validateThread();
		return wrap(m.getSchemas(catalog, schemaPattern));
	}

	@Override
	public final ResultSet getClientInfoProperties() throws SQLException {
		validateThread();
		return wrap(m.getClientInfoProperties());
	}

	@Override
	public final ResultSet getFunctions(String catalog, String schemaPattern,
			String functionNamePattern) throws SQLException {
		validateThread();
		return wrap(
				m.getFunctions(catalog, schemaPattern, functionNamePattern));
	}

	@Override
	public final ResultSet getFunctionColumns(String catalog,
			String schemaPattern, String functionNamePattern,
			String columnNamePattern) throws SQLException {
		validateThread();
		return wrap(m.getFunctionColumns(catalog, schemaPattern,
				functionNamePattern, columnNamePattern));
	}

	@Override
	public final ResultSet getPseudoColumns(String catalog,
			String schemaPattern, String tableNamePattern,
			String columnNamePattern) throws SQLException {
		validateThread();
		return wrap(m.getPseudoColumns(catalog, schemaPattern, tableNamePattern,
				columnNamePattern));
	}
}
