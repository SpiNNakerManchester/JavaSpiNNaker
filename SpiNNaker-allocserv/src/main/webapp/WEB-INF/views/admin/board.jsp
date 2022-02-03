<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<html>
<%--
Copyright (c) 2021-2022 The University of Manchester

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
	<jsp:param value="Board Management" name="title"/>
	<jsp:param name="spalloclib" value="true" />
</jsp:include>
<h1>Spalloc Board Management</h1>

<spring:eval expression="machineNames.keySet()" var="names" />
<c:choose>
	<c:when test="${ board.idPresent }">
		<form:form method="POST" modelAttribute="board">
			<form:input path="id" type="hidden" />
			<h2>Board Location</h2>
			<form:label path="machineName">Machine:</form:label>

			<form:select path="machineName" disabled="true">
				<form:options items="${ names }" />
			</form:select>
			<br>
			Triad coordinates:
			<form:label path="x">X:</form:label>
			<form:input path="x" type="number" disabled="true" />
			<form:label path="y">Y:</form:label>
			<form:input path="y" type="number" disabled="true" />
			<form:label path="z">Z:</form:label>
			<form:input path="z" type="number" disabled="true" />
			<br>
			Physical coordinates:
			<form:label path="cabinet">Cabinet:</form:label>
			<form:input path="cabinet" type="number" disabled="true" />
			<form:label path="frame">Frame:</form:label>
			<form:input path="frame" type="number" disabled="true" />
			<form:label path="board">Board:</form:label>
			<form:input path="board" type="number" disabled="true" />
			<br>
			Network coordinates:
			<form:label path="ipAddress">IP Address:</form:label>
			<form:input path="ipAddress" type="text" disabled="true" />
			<c:if test="${ not board.addressPresent }">
			<p>
			<strong>No IP address for board!</strong>
			Might be no actual hardware present.
			</c:if>
			<p>
			<h2>Current State (read only)</h2>
			Current job:
			<c:choose>
				<c:when test="${ board.jobAllocated }">
					<a href='<c:url
							value="/spalloc/system/job_info/${ board.jobId }" />'
					>${ board.jobId }</a>
				</c:when>
				<c:otherwise>
					<em>None</em>
				</c:otherwise>
			</c:choose>
			<p>
			<c:choose>
				<c:when test="${ board.powered }">
					Powered <em>ON</em> at
					<span id="boardTimestamp">${ board.lastPowerOn }</span>
				</c:when>
				<c:otherwise>
					Powered <em>OFF</em> at
					<span id="boardTimestamp">${ board.lastPowerOff }</span>
				</c:otherwise>
			</c:choose>
			<script defer="defer">
				prettyTimestamp("boardTimestamp");
			</script>
			<p>
			<c:if test="${ not empty board.reports }">
				<h2>Issue Reports for Board</h2>
				<table>
					<thead>
						<tr>
							<th>Reporter</th>
							<th>Time</th>
							<th>Issue</th>
						</tr>
					</thead>
					<tbody>
						<c:forEach items="${ board.reports }" var="r">
							<tr>
								<td>
									<c:out value="${ r.reporter }" escapeXml="true" />
								</td>
								<td>
									<span id="timestamp-${ r.id }">
										<c:out value="${ r.timestamp }" escapeXml="true" />
									</span>
									<script defer="defer">
										prettyTimestamp("timestamp-${ r.id }");
									</script>
								</td>
								<td>
									<c:out value="${ r.issue }" escapeXml="true" />
								</td>
							</tr>
						</c:forEach>
					</tbody>
				</table>
				<p>
			</c:if>
			<h2>Control State</h2>
			<form:label path="enabled">Enable:</form:label>
			<form:checkbox path="enabled" />
			<br>
			Note that disabling a board only means that it will not be handed
			out in future allocations.
			<p>
			<input type="submit" value="Change State" />
		</form:form>
	</c:when>
	<c:otherwise>
		<form:form method="POST" modelAttribute="board">
			<h2>Specify a machine...</h2>
			<form:label path="machineName">Machine:</form:label>
			<form:select path="machineName">
				<form:options items="${ names }"/>
			</form:select>
			<h2>...and the coordinates within a machine</h2>
			<br>
			Triad coordinates:
			<form:label path="x">X:</form:label>
			<form:input path="x" type="number"/>
			<form:label path="y">Y:</form:label>
			<form:input path="y" type="number"/>
			<form:label path="z">Z:</form:label>
			<form:input path="z" type="number"/>
			<br>
			Physical coordinates:
			<form:label path="cabinet">Cabinet:</form:label>
			<form:input path="cabinet" type="number"/>
			<form:label path="frame">Frame:</form:label>
			<form:input path="frame" type="number"/>
			<form:label path="board">Board:</form:label>
			<form:input path="board" type="number"/>
			<br>
			Network coordinates:
			<form:label path="ipAddress">IP Address:</form:label>
			<form:input path="ipAddress" type="text"/>
			<p>
			<input type="submit" value="Look Up Board" />
		</form:form>
	</c:otherwise>
</c:choose>
<button onclick="window.location.href='${ baseuri }'">Clear form</button>

<jsp:include page="footer.jsp" />
</body>
</html>
