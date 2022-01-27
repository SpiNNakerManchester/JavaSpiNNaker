<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
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
	<jsp:param value="SpiNNaker Job" name="title"/>
	<jsp:param name="spalloclib" value="true" />
</jsp:include>
<script>
<jsp:include page="data/jobdetails_obj.jsp">
	<jsp:param name="job" value="${ job }"/>
</jsp:include>
</script>
<body>

<h1>SpiNNaker Job</h1>

<table>
<tr>
	<th class="lineTitle">Job ID:</th>
	<td>${ job.id }</td>
</tr>
<tr>
	<th class="lineTitle">Owner:</th>
	<td><c:out value="${ job.owner.orElse('[SHROUDED]') }" escapeXml="true" /></td>
</tr>
<tr>
	<th class="lineTitle">State:</th>
	<td>${ job.state }</td>
</tr>
<tr>
	<th class="lineTitle">
		Start time:
		<%-- Can't go in the element below; content will be replaced --%>
		<script defer="defer">
			prettyTimestamp("startTime");
		</script>
	</th>
	<td id="startTime">${ job.startTime }</td>
</tr>
<tr>
	<th class="lineTitle">
		Keep-alive:
		<%-- Can't go in the element below; content will be replaced --%>
		<script defer="defer">
			prettyDuration("keepAlive");
		</script>
	</th>
	<td id="keepAlive">${ job.keepAlive }</td>
</tr>
<tr>
	<th class="lineTitle">Owner host:</th>
	<td><c:out value="${ job.ownerHost.orElse('[SHROUDED]') }" escapeXml="true" /></td>
</tr>
<tr>
	<th class="lineTitle">Raw request:</th>
	<td><details><summary><em>Click to show</em></summary>
	<c:if test="${ not empty job.request }">
		<pre id="rawRequest"><c:out value="${ job.request }" escapeXml="true" /></pre>
		<script defer="defer">
			prettyJson("rawRequest");
		</script>
	</c:if>
	</details></td>
</tr>
<tr>
	<th class="lineTitle">Allocation:</th>
	<td>
		<c:choose>
			<c:when test="${ not empty job.boards }">
				<canvas id="board_layout" width="300" height="200"></canvas>
				<canvas id="tooltip" width="100" height="50"></canvas>
				<script defer="defer">
					drawJob("board_layout", "tooltip", job);
				</script>
			</c:when>
			<c:otherwise>Not currently allocated</c:otherwise>
		</c:choose>
	</td>
</tr>
<tr>
	<th class="lineTitle">Root board IP address:</th>
	<td>
		<c:choose>
			<c:when test="${ not empty job.boards }">
				${ job.boards[0].address }
			</c:when>
			<c:otherwise>Not currently allocated</c:otherwise>
		</c:choose>
	</td>
</tr>
<tr>
	<th class="lineTitle">Dimensions (chips):</th>
	<td>
		<c:choose>
			<c:when test="${ job.width.present }">
				${ job.width.get() }&times;${ job.height.get() }
			</c:when>
			<c:otherwise>Not currently allocated</c:otherwise>
		</c:choose>
	</td>
</tr>
<tr>
	<th class="lineTitle">Number of boards:</th>
	<td>
		<c:choose>
			<c:when test="${ not empty job.boards }">
				${ job.boards.size() }
			</c:when>
			<c:otherwise>Not currently allocated</c:otherwise>
		</c:choose>
	</td>
</tr>
<tr>
	<th class="lineTitle">Board power:</th>
	<td>
		<c:choose>
			<c:when test="${ not empty job.boards }">
				${ job.powered ? 'on' : 'off' }
			</c:when>
			<c:otherwise>Not currently allocated</c:otherwise>
		</c:choose>
	</td>
</tr>
<tr>
	<th class="lineTitle">SpiNNaker machine:</th>
	<td><a href="${ job.machineUrl }"><c:out value="${ job.machine }"
		escapeXml="true" /></a></td>
</tr>
</table>

<jsp:include page="basicfooter.jsp" />
</body>
</html>
