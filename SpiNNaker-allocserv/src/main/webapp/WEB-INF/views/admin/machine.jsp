<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<html>
<%--
Copyright (c) 2021 The University of Manchester

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
--%>
<jsp:include page="../head.jsp">
	<jsp:param value="Spalloc Machine Management" name="title"/>
	<jsp:param name="spalloclib" value="true" />
</jsp:include>
<body>

<h1>Machine Management</h1>
<h2>Define Machines</h2>
<c:if test="${ not empty definedMachines }">
	<c:forEach items="${ definedMachines }" var="m">
		Machine called "<code><c:out value="${ m.name }" escapeXml="true" /></code>"
		(${ m.width }&times;${ m.height }&times;3) defined.<br>
	</c:forEach>
</c:if>
<form method="POST" enctype="multipart/form-data">
	<sec:csrfInput />
	<table>
		<tr>
			<td><label>Select a configuration file to upload:</label></td>
			<td><input type="file" name="file" /></td>
		</tr>
		<tr>
			<td colspan="2"><input type="submit" value="Submit" /></td>
		</tr>
	</table>
</form>

<h2>Existing Machines</h2>
<table>
	<thead>
		<tr>
			<th rowspan="2">Name</th>
			<th rowspan="2">Tags</th>
			<th colspan="4">Reports</th>
		</tr>
		<tr>
			<th class="subheading">Board</th>
			<th class="subheading">Reporter</th>
			<th class="subheading">Time</th>
			<th class="subheading">Issue</th>
		</tr>
	</thead>
	<tbody>
		<c:forEach items="${ machineTagging }" var="tagging">
			<spring:eval var="reportlist"
					expression="machineReports.get(tagging.name)" />
			<c:choose>
				<c:when test="${ empty reportlist }">
					<tr>
						<td>
							<a href="${ tagging.url }">
								<code><c:out value="${ tagging.name }" /></code>
							</a>
							<form method="POST">
								<sec:csrfInput/>
								<input type="hidden" name="machine" value="${ tagging.name }" />
								<c:choose>
									<c:when test="${ machineNames[tagging.name] }">
										(<span class="componentenabled">&#9745;</span>
										<input type="submit" name="outOfService" value="Disable" />)
									</c:when>
									<c:otherwise>
										(<span class="componentdisabled">&#8999;</span>
										<input type="submit" name="intoService" value="Enable" />)
									</c:otherwise>
								</c:choose>
							</form>
						</td>
						<td>
							<form method="POST">
								<sec:csrfInput/>
								<input type="hidden" name="machine" value="${ tagging.name }" />
								<input name="retag" value="${ tagging.tags }" />
								<div class="hiddensubmit">
									<input type="submit" class="hiddensubmit" />
								</div>
							</form>
						</td>
						<td colspan="4"><!-- Nothing --></td>
					</tr>
				</c:when>
				<c:otherwise>
					<c:forEach items="${ reportlist }"
							var="report" varStatus="loop">
						<tr>
							<c:if test="${ loop.first }">
								<spring:eval var="nrows"
										expression="reportlist.size()" />
								<td rowspan="${ nrows }">
									<a href="${ tagging.url }">
										<code><c:out value="${ tagging.name }" /></code>
									</a>
									<form method="POST">
										<sec:csrfInput/>
										<input type="hidden" name="machine" value="${ tagging.name }" />
										<c:choose>
											<c:when test="${ machineNames[tagging.name] }">
												(<span class="componentenabled">&#9745;</span>
												<input type="submit" name="outOfService" value="Disable" />)
											</c:when>
											<c:otherwise>
												(<span class="componentdisabled">&#8999;</span>
												<input type="submit" name="intoService" value="Enable" />)
											</c:otherwise>
										</c:choose>
									</form>
								</td>
								<td rowspan="${ nrows }">
									<form method="POST">
										<sec:csrfInput/>
										<input type="hidden" name="machine" value="${ tagging.name }" />
										<input name="retag" value="${ tagging.tags }" />
										<div class="hiddensubmit">
											<input type="submit" class="hiddensubmit" />
										</div>
									</form>
								</td>
							</c:if>
							<td><c:out value="${ report.boardId }" /></td>
							<td><c:out value="${ report.reporter }" /></td>
							<td>
								<span id="report-timestamp-${ report.id }"></span><c:out value="${ report.timestamp }" /></span>
								<script defer="defer">
									prettyTimestamp("report-timestamp-${ report.id }");
								</script>
							</td>
							<td>
								<pre><c:out value="${ report.issue }" /></pre>
							</td>
						</tr>
					</c:forEach>
				</c:otherwise>
			</c:choose>
		</c:forEach>
	</tbody>
</table>
<c:choose>
	<c:when test="${ defaultCount < 1 }">
		<h3>No machines are tagged as default</h3>
		Clients may fail.
	</c:when>
	<c:when test="${ defaultCount > 1 }">
		<h3>Multiple machines are tagged as default</h3>
		Clients may get unexpected results.
	</c:when>
</c:choose>

<jsp:include page="footer.jsp" />
</body>
</html>
