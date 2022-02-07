<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
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
