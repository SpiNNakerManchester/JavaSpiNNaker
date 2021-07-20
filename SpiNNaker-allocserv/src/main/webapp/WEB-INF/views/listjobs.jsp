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

<c:if test="${empty jobList}">
	<p>No jobs are alive!</p>
</c:if>
<c:if test="${not empty jobList}">
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
<c:forEach items="${jobList}" var="item">
<tr>
    <td class="textColumn"><code>${item.id }</code></td>
    <td class="textColumn">${item.state }</td>
    <td class="textColumn">${item.powered ? "on" : "off" }</td>
    <td class="numberColumn">${item.numBoards }</td>
    <td class="textColumn"><code>${item.machineName }</code></td>
    <td class="textColumn"><code>${item.creationTimestamp }</code></td>
    <td class="textColumn"><code>${item.keepaliveInterval }</code></td>
    <td class="textColumn">
    ${item.owner.getOrElse("") }
    <c:if test="${item.host.present}">
	    (${item.host.get()})
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
