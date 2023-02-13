<html>
<%--
Copyright (c) 2021-2023 The University of Manchester

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
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
