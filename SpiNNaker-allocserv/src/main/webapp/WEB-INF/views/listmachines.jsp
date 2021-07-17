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
	<jsp:param value="Machine List" name="title"/>
</jsp:include>
<body>

<h1>Spalloc Machines</h1>

<c:if test="${empty machineList}">
	<p>No machines are defined!</p>
</c:if>
<c:if test="${not empty machineList}">
<table border>
<thead>
<tr>
  	<th>Name</th>
  	<th>Num boards</th>
  	<th>In-use</th>
	<th>Jobs</th>
  	<th>Tags</th>
</tr>
</thead>
<tbody>
<c:forEach items="${machineList}" var="item">
<tr>
    <td class="textColumn"><code>${item.name }</code></td>
    <td class="numberColumn">${item.numBoards }</td>
    <td class="numberColumn">${item.numInUse }</td>
    <td class="numberColumn">${item.numJobs }</td>
    <td class="textColumn">
    <c:forEach items="${item.tags }" var="tag">
		<code>${tag }</code>,
    </c:forEach>
    </td>
</tr>
</c:forEach>
</tbody>
</table>
</c:if>

<jsp:include page="basicfooter.jsp" />
</body>
</html>
