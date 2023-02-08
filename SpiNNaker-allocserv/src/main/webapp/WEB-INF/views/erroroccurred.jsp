<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<html>
<%--
Copyright (c) 2021 The University of Manchester

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
<jsp:include page="head.jsp">
	<jsp:param value="An error occurred" name="title"/>
</jsp:include>
<body>

<h1>Error</h1>
<pre style="white-space: pre-wrap">
<c:out value="${ error }" escapeXml="true"/>
</pre>

<h3>Please enter the correct details</h3>
<a href="${ baseuri }">Retry</a>

<jsp:include page="basicfooter.jsp" />
</body>
</html>
