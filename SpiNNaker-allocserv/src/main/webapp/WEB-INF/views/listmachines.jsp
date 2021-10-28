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

<c:if test="${ empty machineList }">
	<p>No machines are defined!</p>
</c:if>
<c:if test="${ not empty machineList }">
<table border="1">
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
<c:forEach items="${ machineList }" var="machine">
<tr>
	<td class="textColumn">
		<c:if test="${ machine.detailsUrl.present }">
			<a href="${ machine.detailsUrl.get() }">
				<code><c:out value="${ machine.name }" escapeXml="true" /></code>
			</a>
		</c:if>
		<c:if test="${ !machine.detailsUrl.present }">
			<code><c:out value="${ machine.name }" escapeXml="true" /></code>
		</c:if>
	</td>
	<td class="numberColumn">${ machine.numBoards }</td>
	<td class="numberColumn">${ machine.numInUse }</td>
	<td class="numberColumn">${ machine.numJobs }</td>
	<td class="textColumn">
	<c:forEach items="${ machine.tags }" var="tag" varStatus="loop">
		<code><c:out value="${ tag }" escapeXml="true"/></code><c:if test="${ !loop.last }">,</c:if>
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
