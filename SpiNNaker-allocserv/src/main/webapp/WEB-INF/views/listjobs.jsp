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
	<jsp:param value="Job List" name="title"/>
</jsp:include>
<body>

<h1>Spalloc Jobs</h1>

<c:if test="${ empty jobList }">
	<p>No jobs are alive!</p>
</c:if>
<c:if test="${ not empty jobList }">
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
    <td class="textColumn"><code>${ job.id }</code></td>
    <td class="textColumn">${ job.state }</td>
    <td class="textColumn">${ job.powered ? "on" : "off" }</td>
    <td class="numberColumn">${ job.numBoards }</td>
    <td class="textColumn"><code>${ job.machineName }</code></td>
    <td class="textColumn"><code>${ job.creationTimestamp }</code></td>
    <td class="textColumn"><code>${ job.keepaliveInterval }</code></td>
    <td class="textColumn">
    ${ job.owner.getOrElse("") }
    <c:if test="${ job.host.present }">
	    (${ job.host.get() })
    </c:if>
    </td>
</tr>
</c:forEach>
</tbody>
</table>
</c:if>

<jsp:include page="basicfooter.jsp" />
</body>
</html>
