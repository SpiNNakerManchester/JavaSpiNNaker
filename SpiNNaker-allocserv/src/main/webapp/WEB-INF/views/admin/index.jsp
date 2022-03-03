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
<jsp:include page="../head.jsp">
	<jsp:param value="Spalloc Service Administration" name="title"/>
</jsp:include>
<body>

<h1>Spalloc Service Administration</h1>
<p>
<a href="${ machineUri }">Manage Machines</a>
<span class="minordetail">(defining, tagging, service status, reported issues)</span>
<p>
<a href="${ boardsUri }">Manage Individual Boards</a>
<span class="minordetail">(lookup, current use, enablement status)</span>
<p>
<a href="${ usersUri }">Manage Users</a>
<span class="minordetail">(creation, suspension, deletion)</span>
<p>
<a href="${ groupsUri }">Manage Groups</a>
<span class="minordetail">(creation, deletion, quotas, membership)</span>

<jsp:include page="../basicfooter.jsp" />
</body>
</html>
