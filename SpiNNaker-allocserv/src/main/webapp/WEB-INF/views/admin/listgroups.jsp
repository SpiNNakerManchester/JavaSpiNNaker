<%@ page trimDirectiveWhitespaces="true" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<html>
<%--
Copyright (c) 2022 The University of Manchester

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
	<jsp:param value="Group List" name="title"/>
</jsp:include>
<body>

<h1>Spalloc Groups</h1>
<p>
<button onclick="location.href='${ createGroupUri }'"
		type="button">Create a new group</button>
<p>
<table>
	<thead>
		<tr>
			<th>Group</th>
			<th>Actions</th>
		</tr>
	</thead>
	<tbody>
		<tr><th colspan="2" class="minordetail">Internal Groups</th></tr>
		<c:forEach items="${ localgroups }" var="item">
			<tr>
				<td>
					<c:out value="${ item.key }" escapeXml="true" />
				</td>
				<td>
					<button onclick="location.href='${ item.value }'"
							type="button">Manage</button>
				</td>
			</tr>
		</c:forEach>
		<tr><th colspan="2" class="minordetail">HBP Organisations</th></tr>
		<c:forEach items="${ orggroups }" var="item">
			<tr>
				<td>
					<c:out value="${ item.key }" escapeXml="true" />
				</td>
				<td>
					<button onclick="location.href='${ item.value }'"
							type="button">Manage</button>
				</td>
			</tr>
		</c:forEach>
		<tr><th colspan="2" class="minordetail">HBP Collabratories</th></tr>
		<c:forEach items="${ collabgroups }" var="item">
			<tr>
				<td>
					<c:out value="${ item.key }" escapeXml="true" />
				</td>
				<td>
					<button onclick="location.href='${ item.value }'"
							type="button">Manage</button>
				</td>
			</tr>
		</c:forEach>
	</tbody>
</table>

<jsp:include page="footer.jsp" />
</body>
</html>
