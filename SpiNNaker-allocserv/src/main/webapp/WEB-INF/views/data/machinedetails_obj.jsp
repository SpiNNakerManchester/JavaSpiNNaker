<%@ taglib prefix="c" uri="jakarta.tags.core"%>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="sec" uri="http://www.springframework.org/security/tags" %>
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
<c:if test="${ empty param.json }"><%-- Pass json=true to emit pure JSON --%>
machine = (
</c:if>
{
	"name": "${ machine.name }",
	"width": ${ machine.width },
	"height": ${ machine.height },
	"num_in_use": ${ machine.numInUse },
	"tags": [
	<c:forEach items="${ machine.tags }" var="tag" varStatus="loop">
		"<c:out value="${ tag }" escapeXml="true" />"
		<c:if test="${ !loop.last }">,</c:if>
	</c:forEach>],
	"jobs": [
	<c:forEach items="${ machine.jobs }" var="job" varStatus="loop">
		{
			"id": "${ job.id }",
			<c:if test="${ job.owner.present }">
				<spring:eval var="jobUrl" expression="job.url.get()" />
				<spring:eval var="jobOwner" expression="job.owner.get()" />
				"url": "${ jobUrl }",
				"owner": "${ jobOwner }",
			</c:if>
			"boards": [
			<c:forEach items="${ job.boards }" var="board" varStatus="boardLoop">
				{
					"triad": {
						"x": ${ board.x },
						"y": ${ board.y },
						"z": ${ board.z }
					},
					"physical": {
						"cabinet": "${ board.cabinet }",
						"frame": "${ board.frame }",
						"board": "${ board.board }"
					},
					<c:if test="${ job.owner.present }">
						"network": {
							"address": "${ board.address }"
						}
					</c:if>
				}<c:if test="${ !boardLoop.last }">,</c:if>
			</c:forEach>
			]
		}<c:if test="${ !loop.last }">,</c:if>
	</c:forEach>],
	"live_boards": [
	<c:forEach items="${ machine.live }" var="board">
		<%-- board is a BoardCoords --%>
		{
			"triad": {
				"x": ${ board.x },
				"y": ${ board.y },
				"z": ${ board.z }
			},
			"physical": {
				"cabinet": "${ board.cabinet }",
				"frame": "${ board.frame }",
				"board": "${ board.board }"
			},
			<sec:authorize access="hasRole('ADMIN')">
				"network": {
					"address": "${ board.address }"
				}
			</sec:authorize>
		}<c:if test="${ !loop.last }">,</c:if>
	</c:forEach>],
	"dead_boards": [
	<c:forEach items="${ machine.dead }" var="board">
		<%-- board is a BoardCoords --%>
		{
			"triad": {
				"x": ${ board.x },
				"y": ${ board.y },
				"z": ${ board.z }
			},
			"physical": {
				"cabinet": "${ board.cabinet }",
				"frame": "${ board.frame }",
				"board": "${ board.board }"
			},
			<sec:authorize access="hasRole('ADMIN')">
				"network": {
					"address": "${ board.address }"
				}
			</sec:authorize>
		}<c:if test="${ !loop.last }">,</c:if>
	</c:forEach>]
}
<c:if test="${ empty param.json }">
);
</c:if>
