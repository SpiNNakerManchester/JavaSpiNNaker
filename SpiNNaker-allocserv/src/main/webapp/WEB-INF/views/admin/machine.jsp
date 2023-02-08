<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form" %>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<html>
<%--
Copyright (c) 2021 The University of Manchester

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
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
								<span id="report-timestamp-${ report.id }"><c:out value="${ report.timestamp }" /></span>
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
