<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
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
	<jsp:param value="SpiNNaker Allocation Service" name="title"/>
</jsp:include>
<body>

<h1>SpiNNaker Allocation Service</h1>
<p class="version">
Version: ${ version }
<br>
Build: ${ build }
</p>
<sec:authorize access="hasRole('ADMIN')">
	<p>
		<a href="<c:url value="admin/"/>">Service Administration</a>
	</p>
</sec:authorize>
<sec:authorize access="hasRole('READER')">
	<h2>SpiNNaker Machines</h2>
	<p>
		<a href="<c:url value="list_machines/"/>">List</a>
	</p>
	<h2>SpiNNaker Jobs</h2>
	<p>
		<a href="<c:url value="list_jobs/"/>">List</a>
	</p>
</sec:authorize>

<jsp:include page="basicfooter.jsp" />
</body>
</html>
