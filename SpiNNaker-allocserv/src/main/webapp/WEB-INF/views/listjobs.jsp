<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
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
<jsp:include page="head.jsp">
	<jsp:param name="title" value="SpiNNaker Job List"/>
	<jsp:param name="refresh" value="30"/>
</jsp:include>
<body>

<h1>SpiNNaker Jobs</h1>

<c:choose>
	<c:when test="${ not empty jobList }">
		<table border="1">
			<thead>
				<tr>
					<th>ID</th>
					<th>State</th>
					<th>Power</th>
					<th>Boards</th>
					<th>Machine</th>
					<th>Created at</th>
					<th>Keepalive</th>
					<th>Owner (Host)</th>
				</tr>
			</thead>
			<tbody>
				<c:forEach items="${ jobList }" var="job">
					<tr>
						<td class="textColumn">
							<c:choose>
								<c:when test="${ job.detailsUrl.present }">
									<spring:eval var="jobDetailsUrl" expression="job.detailsUrl.get()" />
									<a href="${ jobDetailsUrl }"><code>${ job.id }</code></a>
								</c:when>
								<c:otherwise>
									<code>${ job.id }</code>
								</c:otherwise>
							</c:choose>
						</td>
						<td class="textColumn">${ job.state }</td>
						<td class="textColumn">${ job.powered ? "on" : "off" }</td>
						<td class="numberColumn">
							<spring:eval expression="job.numBoards.orElse(0)" />
						</td>
						<td class="textColumn">
							<code><c:out value="${ job.machineName }" escapeXml="true" /></code>
						</td>
						<td class="textColumn">
							<script defer="defer">
								prettyTimestamp("start-${ job.id }");
							</script>
							<code id="start-${ job.id }">${ job.creationTimestamp }</code>
						</td>
						<td class="textColumn">
							<script defer="defer">
								prettyDuration("alive-${ job.id }");
							</script>
							<code id="alive-${ job.id }">${ job.keepaliveInterval }</code>
						</td>
						<td class="textColumn">
							<c:if test="${ job.owner.present }">
								<spring:eval expression="job.owner.get()" htmlEscape="true" />
							</c:if>
							<c:if test="${ job.host.present }">
								(<spring:eval expression="job.host.get()" htmlEscape="true" />)
							</c:if>
						</td>
					</tr>
				</c:forEach>
			</tbody>
		</table>
	</c:when>
	<c:otherwise>
		<p>No jobs are alive!</p>
	</c:otherwise>
</c:choose>

<jsp:include page="basicfooter.jsp" />
</body>
</html>
