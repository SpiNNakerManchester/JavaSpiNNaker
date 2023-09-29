<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
<%@ taglib prefix="c" uri="jakarta.tags.core"%>
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
	<td>
		<spring:eval htmlEscape="true"
				expression="job.owner.orElse('[SHROUDED]')" />
	</td>
</tr>
<tr>
	<th class="lineTitle">State:</th>
	<%-- TODO: Refresh state with updated data periodically; full refresh on major change to fix board map --%>
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
	<td>
		<spring:eval expression="job.ownerHost.orElse('[SHROUDED]')"
				htmlEscape="true" />
	</td>
</tr>
<tr>
	<th class="lineTitle">Raw request:</th>
	<td><details><summary><em>Click to show</em></summary>
	<c:if test="${ not empty job.request }">
		<pre id="rawRequest"><c:out escapeXml="true"
				value="${ job.request }" /></pre>
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
				<spring:eval
						expression="job.width.get()"
				/>&times;<spring:eval
						expression="job.height.get()" />
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
				<spring:eval expression="job.boards.size()" />
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
	<td><a href="${ job.machineUrl }"><c:out
			value="${ job.machine }" escapeXml="true" /></a></td>
</tr>
</table>
<c:if test="${ job.state != 'DESTROYED' }">
<p>
<form method="POST" action="${ deleteUri }">
	<sec:csrfInput />
	Reason: <input type="text" name="reason" />
	<input type="submit" class="warningbutton" value="Destroy this job" />
</form>
</c:if>

<jsp:include page="basicfooter.jsp" />
</body>
</html>
