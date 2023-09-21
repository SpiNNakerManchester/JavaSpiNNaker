<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
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
	<jsp:param name="title" value="SpiNNaker Job List"/>
	<jsp:param name="refresh" value="30"/>
	<jsp:param name="spalloclib" value="true" />
</jsp:include>
<body>

<h1>SpiNNaker Jobs</h1>

<c:choose>
	<c:when test="${ not empty jobList }">
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
				<c:forEach items="${ jobList }" var="job">
					<tr>
						<td class="textColumn">
							<c:choose>
								<c:when test="${ job.detailsUrl.present }">
									<spring:eval var="jobDetailsUrl"
											expression="job.detailsUrl.get()" />
									<a href="${ jobDetailsUrl }"><code>${ job.id }</code></a>
								</c:when>
								<c:otherwise>
									<code>${ job.id }</code>
								</c:otherwise>
							</c:choose>
						</td>
						<td class="textColumn">${ job.state }</td>
						<td class="textColumn">${ job.powered ? "on" : "off" }</td>
						<td class="numberColumn">
							<spring:eval expression="job.numBoards.orElse(0)" />
						</td>
						<td class="textColumn">
							<code><c:out value="${ job.machineName }" escapeXml="true" /></code>
						</td>
						<td class="textColumn">
							<script defer="defer">
								prettyTimestamp("start-${ job.id }");
							</script>
							<code id="start-${ job.id }">${ job.creationTimestamp }</code>
						</td>
						<td class="textColumn">
							<script defer="defer">
								prettyDuration("alive-${ job.id }");
							</script>
							<code id="alive-${ job.id }">${ job.keepaliveInterval }</code>
						</td>
						<td class="textColumn">
							<c:if test="${ job.owner.present }">
								<spring:eval htmlEscape="true"
										expression="job.owner.get()" />
							</c:if>
							<c:if test="${ job.host.present }">
								(<spring:eval htmlEscape="true"
										expression="job.host.get()" />)
							</c:if>
						</td>
					</tr>
				</c:forEach>
			</tbody>
		</table>
	</c:when>
	<c:otherwise>
		<p>No jobs are alive!</p>
	</c:otherwise>
</c:choose>

<jsp:include page="basicfooter.jsp" />
</body>
</html>
