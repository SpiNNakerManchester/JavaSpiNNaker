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
	<jsp:param value="Spalloc Job" name="title"/>
</jsp:include>
<jsp:include page="jobdetails_obj.jsp">
	<jsp:param name="boards" value="${job.boards}"/>
</jsp:include>
<script src="spalloc.js">
</script>
<body>

<h1>Spalloc Job</h1>

<table class="lineTitle">
<tr>
	<th>Job ID:</th>
	<td>${job.id }</td>
</tr>
<tr>
	<th>Owner:</th>
	<td>${job.owner.orElse('[SHROUDED]') }</td>
</tr>
<tr>
	<th>State:</th>
	<td>${job.state }</td>
</tr>
<tr>
	<th>Start time:</th>
	<td>${job.startTime }</td>
</tr>
<tr>
	<th>Keepalive:</th>
	<td>${job.keepAlive }</td>
</tr>
<tr>
	<th>Owner host:</th>
	<td>${job.ownerHost }</td>
</tr>
<tr>
	<th>Request:</th>
	<td>${job.request }</td>
</tr>
<tr>
	<th>Allocation:</th>
	<td>
		<c:if test="${ not empty job.boards }">
			<canvas id="board_layout" width="300" height="200"></canvas>
			<script defer="defer">
// TODO what goes here?
			</script>
			<em>TODO: board layout</em>
		</c:if>
		<c:if test="${ empty job.boards }">
			No allocation
		</c:if>
	</td>
</tr>
<tr>
	<th>Hostname:</th>
	<td>${(job.boards.get(0)?.address) ?: 'not yet allocated' }</td>
</tr>
<tr>
	<th>Width:</th>
	<td>${job.width }</td>
</tr>
<tr>
	<th>Height:</th>
	<td>${job.height }</td>
</tr>
<tr>
	<th>Num boards:</th>
	<td>${job.boards.size() > 0 ? job.boards.size : 'not yet allocated' }</td>
</tr>
<tr>
	<th>Board power:</th>
	<td>${job.powered ? 'on' : 'off' }</td>
</tr>
<tr>
	<th>Running on:</th>
	<td><a href="${job.machineUrl }">${job.machine }</a></td>
</tr>
</table>

<jsp:include page="basicfooter.jsp" />
</body>
</html>
