<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<%@ taglib prefix="form" uri="http://www.springframework.org/tags/form"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags" %>
<html>
<%--
Copyright (c) 2021 The University of Manchester

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

	https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
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
			<form:label path="machineName">Machine:&nbsp;</form:label><form:select
					path="machineName" disabled="true">
				<form:options items="${ names }" />
			</form:select>
			<form:input type="hidden" path="machineName" />
			<c:if test="${ not empty board.bmpSerial }">
				<p>
					<span class="minordetail">
						Serial: <spring:eval expression="board.bmpSerial" />
						<c:if test="${ not empty board.physicalSerial }">
							(<spring:eval expression="board.physicalSerial" />)
						</c:if>
					</span>
				</p>
			</c:if>
			<br>
			<h3>Triad coordinates:</h3>
			<form:label path="x">X:&nbsp;</form:label><form:input
					path="x" type="number" disabled="true" min="0" max="255" />
			<form:label path="y">Y:&nbsp;</form:label><form:input
					path="y" type="number" disabled="true" min="0" max="255" />
			<form:label path="z">Z:&nbsp;</form:label><form:input
					path="z" type="number" disabled="true" min="0" max="255" />
			<br>
			<h3>Physical coordinates:</h3>
			<form:label path="cabinet">Cabinet:&nbsp;</form:label><form:input
					path="cabinet" type="number" disabled="true" min="0" max="255" />
			<form:label path="frame">Frame:&nbsp;</form:label><form:input
					path="frame" type="number" disabled="true" min="0" max="255" />
			<form:label path="board">Board:&nbsp;</form:label><form:input
					path="board" type="number" disabled="true" min="0" max="255" />
			<br>
			<h3>Network coordinates:</h3>
			<form:label path="ipAddress">IP&nbsp;Address:&nbsp;</form:label><form:input
					path="ipAddress" type="text" disabled="true" size="15" />
			<c:if test="${ not board.addressPresent }">
				<p>
				<strong>No IP address for board!</strong>
				Might be no actual hardware present.
			</c:if>
			<p>
			<h2>Current State (read only)</h2>
			Temperature: <span id="temperatureDisplay">Reading...</span>
			<!-- ${tempDataUri}?board_id=${board.id} -->
			<script defer="defer">
				loadTemperature("${ tempDataUri }", ${board.id}, "temperatureDisplay");
			</script>
			<p>
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
			<c:choose>
				<c:when test="${ board.enabled }">
					<form:input path="enabled" type="hidden" value="false" />
					Enabled: <span class="componentenabled">&#9745;</span>
					<input type="submit" value="Disable" />
				</c:when>
				<c:otherwise>
					<form:input path="enabled" type="hidden" value="true" />
					Enabled: <span class="componentdisabled">&#8999;</span>
					<input type="submit" value="Enable" />
				</c:otherwise>
			</c:choose>
			<br>
			Note that disabling a board only means that it will not be handed
			out in future allocations.
		</form:form>
		<h2>Blacklisted Hardware</h2><br/>
		<textarea id="blacklistDisplay"></textarea><br/>
		<button id="saveBl" onclick="saveBlacklist('${ blacklistControlUri }', ${ board.id }, ${ board.bmpId }, 'blacklistDisplay', 'saveBl', 'loadBl')" class="warningbutton" enabled="false">Write New Blacklist</button>
		<button id="loadBl" onclick="loadBlacklist('${ blacklistControlUri }', ${ board.id }, ${ board.bmpId }, 'blacklistDisplay', 'saveBl', 'loadBl')">Read Blacklist</button>
		<br/>
	</c:when>
	<c:otherwise>
		<form:form method="POST" modelAttribute="board">
			<h2>Specify a machine...</h2>
			<form:label path="machineName">Machine:&nbsp;</form:label><form:select
					path="machineName">
				<form:options items="${ names }"/>
			</form:select>
			<h2>...and the coordinates within a machine</h2>
			<br>
			<h3>Triad coordinates:</h3>
			<form:label path="x">X:&nbsp;</form:label><form:input
					path="x" type="number" min="0" max="255" />
			<form:label path="y">Y:&nbsp;</form:label><form:input
					path="y" type="number" min="0" max="255" />
			<form:label path="z">Z:&nbsp;</form:label><form:input
					path="z" type="number" min="0" max="255" />
			<br>
			<h3>Physical coordinates:</h3>
			<form:label path="cabinet">Cabinet:&nbsp;</form:label><form:input
					path="cabinet" type="number" min="0" max="255" />
			<form:label path="frame">Frame:&nbsp;</form:label><form:input
					path="frame" type="number" min="0" max="255" />
			<form:label path="board">Board:&nbsp;</form:label><form:input
					path="board" type="number" min="0" max="255" />
			<br>
			<h3>Network coordinates:</h3>
			<form:label path="ipAddress">IP&nbsp;Address:&nbsp;</form:label><form:input
					path="ipAddress" type="text" size="15" />
			<p>
			<input type="submit" value="Look Up Board" />
		</form:form>
	</c:otherwise>
</c:choose>
<button onclick="window.location.href='${ baseuri }'">Clear form</button>

<jsp:include page="footer.jsp" />
</body>
</html>
