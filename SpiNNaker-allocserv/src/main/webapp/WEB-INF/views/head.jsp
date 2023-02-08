<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
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
<head>
	<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
	<meta name="viewport" content="width=device-width, initial-scale=1">
	<title>${ param.title }</title>
	<link rel="stylesheet" href="<c:url value="/spalloc/system/resources/spalloc.css"/>">
	<c:if test="${ param.spalloclib ne null }">
		<script src="<c:url value="/spalloc/system/resources/spinnaker.js"/>">
		</script>
	</c:if>
	<c:if test="${ param.refresh ne null }">
		<meta http-equiv="refresh" content="${ param.refresh }">
	</c:if>
</head>
